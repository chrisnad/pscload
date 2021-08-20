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
        if (zipFile != null && FilesUtils.unzip(zipFile, true)) {
            // stage 1: download and unzip successful
            customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(1);
        }
    }

    /**
     * Load latest file.
     *
     * @throws IOException the io exception
     */
    public boolean loadLatestFile() throws IOException {
        Map<String, File> latestFiles = FilesUtils.getLatestExtAndSer(filesDirectory);

        latestExtract = latestFiles.get("txt");
        if (latestExtract == null) {
            return false;
        }
        log.info("loading file: {}", latestExtract.getName());

        loader.loadMapsFromFile(latestExtract);
        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(2);
        return true;
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
        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(3);
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
    public void serializeMapsToFile() throws FileNotFoundException {
        // serialise latest extract. This step should be done right here otherwise deserializing this file will fail
        String latestExtractDate = FilesUtils.getDateStringFromFileName(latestExtract);
        serializer.serialiseMapsToFile(loader.getPsMap(), loader.getStructureMap(),
                filesDirectory + "/" + latestExtractDate.concat(".ser"));

        Metrics.counter(CustomMetrics.SER_FILE_TAG, CustomMetrics.TIMESTAMP_TAG, latestExtractDate).increment();
        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(5);
    }

    /**
     * Load changes.
     *
     */
    public boolean uploadChanges() throws IOException {
        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(6);

        if (psDiff == null || structureDiff == null) {
           return false;
        }
        pscRestApi.uploadChanges(psDiff, structureDiff);

        customMetrics.getAppMiscGauges().get(CustomMetrics.MiscCustomMetric.STAGE).set(0);
        return true;
    }

}
