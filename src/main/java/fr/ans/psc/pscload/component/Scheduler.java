package fr.ans.psc.pscload.component;

import fr.ans.psc.pscload.component.utils.FilesUtils;
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

    @Value("${extract.download.url}")
    private String extractDownloadUrl;

    @Value("${files.directory}")
    private String filesDirectory;

    /**
     * Download and parse.
     */
    public void run() throws GeneralSecurityException, IOException {
        if (enabled) {
            System.out.println("--------------------------SCHEDULER TIIIIIME --------------------------");
            process.downloadAndUnzip(extractDownloadUrl);
            process.loadLatestFile();
            process.deserializeFileToMaps();
            process.computeDiff();
        }
    }

    @Scheduled(cron = "${schedule.cron.expression}", zone = "${schedule.cron.timeZone}")
    public void scheduleProcess() throws GeneralSecurityException, IOException {
        run();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event)  {
        try {
            run();
        } catch (Exception e) {
            log.info("scheduled task failed on application ready");
        }
    }
}
