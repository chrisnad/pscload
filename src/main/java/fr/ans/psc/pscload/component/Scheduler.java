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
    public ProcessStep run() throws GeneralSecurityException, IOException {
        if (enabled) {
            log.info("start batch");
            ProcessStep currentStep;

            currentStep = process.downloadAndUnzip(extractDownloadUrl);
            if (currentStep != ProcessStep.CONTINUE) {
                return currentStep;
            }

            currentStep = process.downloadAndUnzip(extractDownloadUrl);
            if (currentStep != ProcessStep.CONTINUE) {
                return currentStep;
            }

            currentStep = process.loadLatestFile();
            if (currentStep != ProcessStep.CONTINUE) {
                return currentStep;
            }

            currentStep = process.deserializeFileToMaps();
            if (currentStep != ProcessStep.CONTINUE) {
                return currentStep;
            }

            process.computeDiff();

            if (autoContinue) {
                currentStep = process.uploadChanges();
                if (currentStep != ProcessStep.CONTINUE) {
                    return currentStep;
                }
                currentStep = process.serializeMapsToFile();
                if (currentStep != ProcessStep.CONTINUE) {
                    return currentStep;
                }
                currentStep = process.triggerExtract();
                if (currentStep != ProcessStep.CONTINUE) {
                    return currentStep;
                }

                FilesUtils.cleanup(filesDirectory);
            }

            return currentStep;
        }
        return null;
    }
}
