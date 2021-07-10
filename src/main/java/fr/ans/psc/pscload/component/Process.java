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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

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

    private File latestExtract;

    private MapDifference<String, Professionnel> psDiff;

    private MapDifference<String, Structure> structureDiff;

    /**
     * Download and parse.
     *
     * @param downloadUrl the download url
     * @throws GeneralSecurityException the general security exception
     * @throws IOException              the io exception
     */
    public void downloadAndUnzip(String downloadUrl) throws GeneralSecurityException, IOException {
        if (useSSL) {
            SSLUtils.initSSLContext(cert, key, ca);
        }
        // downloads only if zip doesnt exist in our files directory
        String zipFile = SSLUtils.downloadFile(downloadUrl, filesDirectory);

        // unzipping only if txt file is newer than what we already have
        if (zipFile != null && FilesUtils.unzip(zipFile)) {
            // stage 1: download and unzip successful
            customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STAGE).set(1);
        }
    }

    /**
     * Load latest file.
     *
     * @throws IOException the io exception
     */
    public void loadLatestFile() throws IOException {
        Map<String, File> latestFiles = FilesUtils.getLatestExtAndSer(filesDirectory);

        latestExtract = latestFiles.get("txt");
        log.info("loading file: {}", latestExtract.getName());

        loader.loadMapFromFile(latestExtract);
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STAGE).set(2);
    }

    /**
     * Deserialize file to maps.
     *
     * @throws IOException the io exception
     */
    public void deserializeFileToMaps() throws IOException {
        Map<String, File> latestFiles = FilesUtils.getLatestExtAndSer(filesDirectory);

        File ogFile = latestFiles.get("ser");

        if(ogFile != null) {
            serializer.deserialiseFileToMaps(ogFile);
        }
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STAGE).set(3);
    }

    /**
     * Serialize maps to file.
     *
     * @throws FileNotFoundException the file not found exception
     */
    public void serializeMapsToFile() throws FileNotFoundException {
        // serialise latest extract. This step should be done right here otherwise deserializing this file will fail
        String latestExtractDate = FilesUtils.getDateStringFromFileName(latestExtract);
        serializer.serialiseMapsToFile(loader.getPsMap(), loader.getStructureMap(),
                filesDirectory + "/" + latestExtractDate.concat(".ser"));
        customMetrics.getLatestSerDate().set(Long.parseLong(latestExtractDate));
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STAGE).set(4);
    }

    /**
     * Compute diff.
     */
    public void computeDiff() {
        psDiff = pscRestApi.diffPsMaps(serializer.getPsMap(), loader.getPsMap());
        structureDiff = pscRestApi.diffStructureMaps(serializer.getStructureMap(), loader.getStructureMap());
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STAGE).set(5);
    }

    /**
     * Load changes.
     */
    public void uploadChanges() throws InterruptedException {
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STAGE).set(6);
        pscRestApi.uploadChanges(psDiff, structureDiff);
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STAGE).set(0);
    }

    /**
     * Upload full.
     */
    public void uploadFull() {
        pscRestApi.uploadPsMap(loader.getPsMap());
        pscRestApi.uploadStructureMap(loader.getStructureMap());
    }

}
