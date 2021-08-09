package fr.ans.psc.pscload.component;

import fr.ans.psc.pscload.component.utils.FilesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * The type Scheduler.
 */
@Component
public class Scheduler {

    @Autowired
    private Process process;

    @Value("${enable.scheduler:true}")
    private boolean enabled;

    @Value("${extract.download.url}")
    private String extractDownloadUrl;

    @Value("${files.directory}")
    private String filesDirectory;

    /**
     * Download and parse.
     */
    @Scheduled(fixedDelayString = "${schedule.rate.ms}")
    public void run() throws GeneralSecurityException, IOException {
        if (enabled) {
            process.downloadAndUnzip(extractDownloadUrl);
            process.loadLatestFile();
            process.deserializeFileToMaps();
            process.computeDiff();
            process.serializeMapsToFile();
            process.uploadChanges();
            FilesUtils.cleanup(filesDirectory);
        }
    }
}
