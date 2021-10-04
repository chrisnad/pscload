package fr.ans.psc.pscload.component;

import com.google.common.collect.MapDifference;
import fr.ans.psc.pscload.component.utils.FilesUtils;
import fr.ans.psc.pscload.component.utils.SSLUtils;
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
    private boolean useSSL;

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
    public ProcessStep downloadAndUnzip(String downloadUrl) throws GeneralSecurityException, IOException {
        if (useSSL) {
            SSLUtils.initSSLContext(cert, key, ca);
        }
        // downloads only if zip doesnt exist in our files directory
        String zipFile = SSLUtils.downloadFile(downloadUrl, filesDirectory);

        // unzipping only if txt file is newer than what we already have
        if (zipFile != null && FilesUtils.unzip(zipFile, true)) {
            // stage 1: download and unzip successful
            customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(1);
            return ProcessStep.CONTINUE;
        }
        return zipFile == null ? ProcessStep.ZIP_FILE_ABSENT : ProcessStep.TXT_FILE_ALREADY_EXISTING;
    }

    /**
     * Load latest file.
     *
     * @throws IOException the io exception
     */
    public ProcessStep loadLatestFile() throws IOException {
        Map<String, File> latestFiles = FilesUtils.getLatestExtAndSer(filesDirectory);

        latestExtract = latestFiles.get("txt");
        if (latestExtract == null) {
            return ProcessStep.TXT_FILE_ABSENT;
        }
        log.info("loading file: {}", latestExtract.getName());

        loader.loadMapsFromFile(latestExtract);
        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(2);
        return ProcessStep.CONTINUE;
    }

    /**
     * Deserialize file to maps.
     *
     * @throws IOException the io exception
     */
    public ProcessStep deserializeFileToMaps() throws IOException {
        Map<String, File> latestFiles = FilesUtils.getLatestExtAndSer(filesDirectory);

        File ogFile = latestFiles.get("ser");

        if(ogFile == null) {
            return ProcessStep.SER_FILE_ABSENT;
        }
        else {
            serializer.deserialiseFileToMaps(ogFile);
            customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(3);
            return ProcessStep.CONTINUE;
        }
    }

    /**
     * Compute diff.
     */
    public void computeDiff() {
        psDiff = pscRestApi.diffPsMaps(serializer.getPsMap(), loader.getPsMap());
        structureDiff = pscRestApi.diffStructureMaps(serializer.getStructureMap(), loader.getStructureMap());

        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(4);
    }

    /**
     * Serialize maps to file.
     *
     * @throws FileNotFoundException the file not found exception
     */
    public ProcessStep serializeMapsToFile() throws FileNotFoundException {
        // serialise latest extract
        if (latestExtract == null) {
            return ProcessStep.TXT_FILE_ABSENT;
        }
        else {
            String latestExtractDate = FilesUtils.getDateStringFromFileName(latestExtract);
            serializer.serialiseMapsToFile(loader.getPsMap(), loader.getStructureMap(),
                    filesDirectory + "/" + latestExtractDate.concat(".ser"));

            Metrics.counter(CustomMetrics.SER_FILE_TAG, CustomMetrics.TIMESTAMP_TAG, latestExtractDate).increment();
            customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(5);
            return ProcessStep.CONTINUE;
        }

    }

    /**
     * Load changes.
     *
     */
    public ProcessStep uploadChanges() throws IOException {
        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(6);

        if (psDiff == null || structureDiff == null) {
           return ProcessStep.DIFF_NOT_COMPUTED;
        }
        pscRestApi.uploadChanges(psDiff, structureDiff);

        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(0);
        return ProcessStep.CONTINUE;
    }

    public ProcessStep triggerExtract() throws IOException {
        log.info("prepare trigger RASS extract");
        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder();
        RequestBody body = RequestBody.create("{}", MediaType.parse("application/json"));
        Request request = requestBuilder.url(pscextractBaseUrl + "/generate-extract")
                .post(body).build();

        Call call = client.newCall(request);
        Response response = call.execute();
        log.info("extract response", response);
        String responseBody = Objects.requireNonNull(response.body()).string();
        log.info("response body: {}", responseBody);
        response.close();

        return ProcessStep.CONTINUE;
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
    public ProcessStep loadToggleMaps(File toggleFile) throws IOException {
        loader.loadPSRefMapFromFile(toggleFile);
        return ProcessStep.CONTINUE;
    }

    public void uploadPsRefsAfterToggle() {
        pscRestApi.uploadPsRefs(loader.getPsRefCreateMap(), loader.getPsRefUpdateMap());
    }

}
