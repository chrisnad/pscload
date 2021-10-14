package fr.ans.psc.pscload.component;

import fr.ans.psc.pscload.component.utils.FilesUtils;
import fr.ans.psc.pscload.exceptions.ConcurrentProcessCallException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    @Scheduled(cron = "${schedule.cron.expression}", zone = "${schedule.cron.timeZone}")
    public ProcessStepStatus run() throws GeneralSecurityException, IOException {
        ProcessStepStatus currentStep = ProcessStepStatus.INIT_STEP;
        if (enabled) {
            log.info("start batch");

            currentStep = process.downloadAndUnzip(extractDownloadUrl);
            if (currentStep == ProcessStepStatus.CONTINUE) {
                currentStep = process.runFirst();

                if (autoContinue && currentStep == ProcessStepStatus.CONTINUE) {
                    currentStep = process.runContinue();
                }
            }
        }
        return currentStep;
    }
}
