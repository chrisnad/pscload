package fr.ans.psc.pscload.component;

import fr.ans.psc.pscload.component.utils.FilesUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * The type Scheduler.
 */
@Component
public class Scheduler implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * The logger.
     */
    private static final Logger log = LoggerFactory.getLogger(Process.class);

    @Autowired
    private Process process;

    @Value("${enable.scheduler:true}")
    private boolean enabled;

    @Value("${auto.continue.scheduler:false}")
    private boolean autoContinue;

    @Value("${extract.download.url}")
    private String extractDownloadUrl;

    @Value("${files.directory}")
    private String filesDirectory;

    /**
     * Download and parse.
     */
    public void run() throws GeneralSecurityException, IOException {
        if (enabled) {
            log.info("start batch");
            process.downloadAndUnzip(extractDownloadUrl);
            process.loadLatestFile();
            process.deserializeFileToMaps();
            process.computeDiff();
            if (autoContinue) {
                process.serializeMapsToFile();
                process.uploadChanges();
                FilesUtils.cleanup(filesDirectory);
            }
        }
    }

    @Scheduled(cron = "${schedule.cron.expression}", zone = "${schedule.cron.timeZone}")
    public void scheduleProcess() throws GeneralSecurityException, IOException {
        run();
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event)  {
        try {
            run();
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }
}
