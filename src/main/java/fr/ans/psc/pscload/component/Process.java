package fr.ans.psc.pscload.component;

import com.google.common.collect.MapDifference;
import fr.ans.psc.pscload.component.utils.FilesUtils;
import fr.ans.psc.pscload.component.utils.SSLUtils;
import fr.ans.psc.pscload.exceptions.ConcurrentProcessCallException;
import fr.ans.psc.pscload.mapper.Loader;
import fr.ans.psc.pscload.mapper.Serializer;
import fr.ans.psc.pscload.metrics.CustomMetrics;
import fr.ans.psc.pscload.model.Professionnel;
import fr.ans.psc.pscload.model.Structure;
import fr.ans.psc.pscload.service.PscRestApi;
import io.micrometer.core.instrument.Metrics;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Objects;

/**
 * The type Loader.
 */
@Component
public class Process {

    /**
     * The logger.
     */
    private static final Logger log = LoggerFactory.getLogger(Process.class);

    @Autowired
    private PscRestApi pscRestApi;

    @Autowired
    private Serializer serializer;

    @Autowired
    private Loader loader;

    @Autowired
    private CustomMetrics customMetrics;

    @Value("${cert.path}")
    private String cert;

    @Value("${key.path}")
    private String key;

    @Value("${ca.path}")
    private String ca;

    @Value("${files.directory}")
    private String filesDirectory;

    @Value("${use.ssl}")
    private boolean useCustomSSLContext;

    private final String TOGGLE_FILE_NAME = "Table_de_Correspondance_bascule";

    private File latestExtract;

    private MapDifference<String, Professionnel> psDiff;

    private MapDifference<String, Structure> structureDiff;

    @Value("${pscextract.base.url}")
    private String pscextractBaseUrl;

    /**
     * Download and parse.
     *
     * @param downloadUrl the download url
     * @throws GeneralSecurityException the general security exception
     * @throws IOException              the io exception
     */
    public ProcessStepStatus downloadAndUnzip(String downloadUrl) throws GeneralSecurityException, IOException {
        log.info("cleaning files repository before download");
        FilesUtils.cleanup(filesDirectory);
        ProcessStepStatus currentStepStatus;

        if (useCustomSSLContext) {
            SSLUtils.initSSLContext(cert, key, ca);
        }
        // downloads only if zip doesnt exist in our files directory
        String zipFile = SSLUtils.downloadFile(downloadUrl, filesDirectory);

        // unzipping only if txt file is newer than what we already have
        if (zipFile != null && FilesUtils.unzip(zipFile, true)) {
            // stage 1: download and unzip successful
            customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(ProcessStep.DOWNLOADED.value);
            currentStepStatus = ProcessStepStatus.CONTINUE;
        } else {
            currentStepStatus = zipFile == null ? ProcessStepStatus.ZIP_FILE_ABSENT : ProcessStepStatus.TXT_FILE_ALREADY_EXISTING;
        }
        return currentStepStatus;
    }

    /**
     * Load latest file.
     *
     */
    public ProcessStepStatus loadLatestFile() throws ConcurrentProcessCallException {
        ProcessStepStatus status;
        if (customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).get() == ProcessStep.UPLOAD_CHANGES_STARTED.value) {
            throw new ConcurrentProcessCallException("Cancel loading latest file : upload changes process still running...");
        }
        Map<String, File> latestFiles = FilesUtils.getLatestExtAndSer(filesDirectory);

        latestExtract = latestFiles.get("txt");
        if (latestExtract != null) {
            log.info("loading file: {}", latestExtract.getName());

            try {
                loader.loadMapsFromFile(latestExtract);
                customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(ProcessStep.CURRENT_MAP_LOADED.value);
                status = ProcessStepStatus.CONTINUE;

            } catch (IOException e) {
                log.error("error during file reading", e);
                status = ProcessStepStatus.FILE_READING_ERROR;
            }
        } else {
            status = ProcessStepStatus.TXT_FILE_ABSENT;
        }
        return status;
    }

    /**
     * Deserialize file to maps.
     *
     */
    public ProcessStepStatus deserializeFileToMaps() throws ConcurrentProcessCallException {
        ProcessStepStatus status;
        if (customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).get() == ProcessStep.UPLOAD_CHANGES_STARTED.value) {
            throw new ConcurrentProcessCallException("Cancel deserializing file : upload changes process still running...");
        }
        Map<String, File> latestFiles = FilesUtils.getLatestExtAndSer(filesDirectory);

        File ogFile = latestFiles.get("ser");

        if(ogFile == null) {
            log.info("no ser file has been found");
            // if no ser file is present we should not try to desrialize it but continue with an empty map (map is already initialized)
            customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(ProcessStep.PREVIOUS_MAP_LOADED.value);
            status = ProcessStepStatus.CONTINUE;
        }
        else {
            try {
                serializer.deserialiseFileToMaps(ogFile);
                customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(ProcessStep.PREVIOUS_MAP_LOADED.value);
                status = ProcessStepStatus.CONTINUE;
            } catch (FileNotFoundException e) {
                log.error("Error during deserialization", e);
                status = ProcessStepStatus.INVALID_SER_FILE_PATH;
            }
        }
        return status;
    }

    /**
     * Compute diff.
     */
    public void computeDiff() throws ConcurrentProcessCallException {

        if (customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).get() == ProcessStep.UPLOAD_CHANGES_STARTED.value
        || customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).get() == ProcessStep.COMPUTE_DIFF_STARTED.value) {
            throw new ConcurrentProcessCallException("Cancel computing diff : upload changes process still running...");
        }
        log.info("starting diff");
        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(ProcessStep.COMPUTE_DIFF_STARTED.value);
        psDiff = pscRestApi.diffPsMaps(serializer.getPsMap(), loader.getPsMap());
        structureDiff = pscRestApi.diffStructureMaps(serializer.getStructureMap(), loader.getStructureMap());

        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(ProcessStep.COMPUTE_DIFF_FINISHED.value);
    }

    /**
     * Load changes.
     *
     */
    public ProcessStepStatus uploadChanges() throws ConcurrentProcessCallException {
        if (customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).get() == ProcessStep.UPLOAD_CHANGES_STARTED.value) {
            throw new ConcurrentProcessCallException("Cancel new upload changes : previous upload changes process still running...");
        }
        if (psDiff == null || structureDiff == null) {
           return ProcessStepStatus.DIFF_NOT_COMPUTED;
        }
        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(ProcessStep.UPLOAD_CHANGES_STARTED.value);
        pscRestApi.uploadChanges(psDiff, structureDiff);

        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(ProcessStep.UPLOAD_CHANGES_FINISHED.value);
        return ProcessStepStatus.CONTINUE;
    }

    /**
     * Serialize maps to file.
     *
     */
    public ProcessStepStatus serializeMapsToFile() {
        ProcessStepStatus status;
        // serialise latest extract
        if (latestExtract == null) {
            status = ProcessStepStatus.TXT_FILE_ABSENT;
        }
        else {
            String latestExtractDate = FilesUtils.getDateStringFromFileName(latestExtract);
            try {
                serializer.serialiseMapsToFile(loader.getPsMap(), loader.getStructureMap(),
                        filesDirectory + "/" + latestExtractDate.concat(".ser"));

                Metrics.counter(CustomMetrics.SER_FILE_TAG, CustomMetrics.TIMESTAMP_TAG, latestExtractDate).increment();
                customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(ProcessStep.IDLE.value);
            } catch (FileNotFoundException e) {
                log.error("Invalid path", e);
                return ProcessStepStatus.INVALID_SER_FILE_PATH;
            }

            status = ProcessStepStatus.CONTINUE;
        }
        return status;
    }

    public ProcessStepStatus triggerExtract()  {
        ProcessStepStatus processStepStatus;
        log.info("prepare trigger RASS extract");
        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder();
        RequestBody body = RequestBody.create("{}", MediaType.parse("application/json"));
        Request request = requestBuilder.url(pscextractBaseUrl + "/generate-extract")
                .post(body).build();

        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            log.info("extract response : " + response);
            String responseBody = Objects.requireNonNull(response.body()).string();
            log.info("response body: {}", responseBody);
            response.close();
            processStepStatus = ProcessStepStatus.CONTINUE;
        } catch (IOException e) {
            log.error("Error during pscextract endpoint call", e);
            processStepStatus = ProcessStepStatus.PSCEXTRACT_ENDPOINT_FAILURE;
        }
        return processStepStatus;
    }

    /**
     * upload toggle file
     */
    public File uploadToggleFile(MultipartFile mpFile) throws IOException {
        InputStream initialStream = mpFile.getInputStream();
        byte[] buffer = new byte[initialStream.available()];
        initialStream.read(buffer);

        File toggleFile = File.createTempFile(TOGGLE_FILE_NAME, "tmp");

        try (OutputStream outStream = new FileOutputStream(toggleFile)) {
            outStream.write(buffer);
        }
        return toggleFile;
    }

    /**
     * Load toggle file.
     *
     * @throws IOException the io exception
     */
    public ProcessStepStatus loadToggleMaps(File toggleFile) throws IOException {
        loader.loadPSRefMapFromFile(toggleFile);
        return ProcessStepStatus.CONTINUE;
    }

    public void uploadPsRefsAfterToggle() {
        pscRestApi.uploadPsRefs(loader.getPsRefCreateMap(), loader.getPsRefUpdateMap());
    }

    public ProcessStepStatus runFirst() {
        ProcessStepStatus currentStepStatus;

        try {
            currentStepStatus = loadLatestFile();

            if (currentStepStatus == ProcessStepStatus.CONTINUE) {
                currentStepStatus = deserializeFileToMaps();

                computeDiff();
            }
        } catch (ConcurrentProcessCallException e) {
            log.warn(e.getMessage(), e);
            currentStepStatus = ProcessStepStatus.ABORT;
        }

        return currentStepStatus;
    }

    public ProcessStepStatus runContinue() {
        ProcessStepStatus currentStepStatus;

        try {
            currentStepStatus = uploadChanges();

            if (currentStepStatus == ProcessStepStatus.CONTINUE) {
                currentStepStatus = serializeMapsToFile();

                currentStepStatus = triggerExtract();

                if (currentStepStatus == ProcessStepStatus.CONTINUE) {
                    FilesUtils.cleanup(filesDirectory);
                    log.info("full upload finished");
                }
            }
        } catch (ConcurrentProcessCallException e) {
            log.warn(e.getMessage(), e);
            currentStepStatus = ProcessStepStatus.ABORT;
        }
        return currentStepStatus;
    }

}
