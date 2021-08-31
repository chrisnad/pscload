package fr.ans.psc.pscload.controller;

import fr.ans.psc.pscload.component.Process;
import fr.ans.psc.pscload.component.ProcessStep;
import fr.ans.psc.pscload.component.utils.FilesUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The type Load controller.
 */
@RestController
class ProcessController {

    /**
     * The logger.
     */
    private static final Logger log = LoggerFactory.getLogger(ProcessController.class);

    @Autowired
    private final Process process;

    @Value("${files.directory}")
    private String filesDirectory;

    @Value("${test.download.url}")
    private String testDownloadUrl;

    @Value("${extract.download.url}")
    private String extractDownloadUrl;

    /**
     * Instantiates a new Load controller.
     *
     * @param process the process
     */
    ProcessController(Process process) {
        this.process = process;
    }

    /**
     * Index string.
     *
     * @return the string
     */
    @GetMapping(value = "/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public String index() {
        return "health check OK";
    }

    /**
     * Clean all string.
     *
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/clean-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public String cleanAll() throws IOException {
        FileUtils.cleanDirectory(new File(filesDirectory));
        log.info("all files in {} were deleted!", filesDirectory);
        return "all files in storage were deleted";
    }

    /**
     * Clean string.
     *
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/clean", produces = MediaType.APPLICATION_JSON_VALUE)
    public String clean() throws IOException {
        String[] fileList = Stream.of(Objects.requireNonNull(new File(filesDirectory).listFiles()))
                .map(File::getAbsolutePath).distinct().toArray(String[]::new);
        for (String file : fileList) {
            if (!file.endsWith(".ser")) {
                boolean isDeleted = Files.deleteIfExists(Path.of(file));
                log.info("file: {} is deleted: {}", file, isDeleted);
            }
        }
        log.info("cleanup complete!");
        return "cleanup complete";
    }

    /**
     * List files string.
     *
     * @return the string
     */
    @GetMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String listFiles() {
        return Stream.of(Objects.requireNonNull(new File(filesDirectory).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet()).toString();
    }

    /**
     * Delete file string.
     *
     * @param payload the payload
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/deleteFile",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String deleteFile(@RequestBody Map<String, Object> payload) throws IOException {
        String fileName = (String) payload.get("fileName");
        FileUtils.forceDelete(new File(filesDirectory, fileName));
        log.info("deleted {}", fileName);
        return "deleted " + fileName;
    }

    /**
     * Download test string.
     *
     * @param fileName the file name
     * @return the string
     * @throws IOException              the io exception
     * @throws GeneralSecurityException the general security exception
     */
    @PostMapping(value = "/process/download/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public String downloadTest(@RequestParam String fileName) throws IOException, GeneralSecurityException {
        String downloadUrl = testDownloadUrl + fileName + ".zip";
        log.info("downloading from {}", downloadUrl);
        ProcessStep step = process.downloadAndUnzip(downloadUrl);
        if (step == ProcessStep.CONTINUE) {
            log.info("download complete");
            return "download complete!";
        }
        log.info(step.message);
        return step.message;
    }

    /**
     * Download string.
     *
     * @return the string
     * @throws IOException              the io exception
     * @throws GeneralSecurityException the general security exception
     */
    @PostMapping(value = "/process/download/prod", produces = MediaType.APPLICATION_JSON_VALUE)
    public String download() throws IOException, GeneralSecurityException {
        log.info("downloading from {}", extractDownloadUrl);
        ProcessStep step = process.downloadAndUnzip(extractDownloadUrl);
        if (step == ProcessStep.CONTINUE) {
            log.info("download complete");
            return "download complete!";
        }
        log.info(step.message);
        return step.message;
    }

    /**
     * Load string.
     *
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/process/load/new", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> loadNew() throws IOException {
        ProcessStep step = process.loadLatestFile();
        if (step == ProcessStep.TXT_FILE_ABSENT) {
            return new ResponseEntity<>(step.message, HttpStatus.PRECONDITION_FAILED);
        }
        log.info("new Ps and Structure maps loaded");
        return new ResponseEntity<>("new maps loading complete", HttpStatus.OK);
    }

    /**
     * Load current string.
     *
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/process/load/current", produces = MediaType.APPLICATION_JSON_VALUE)
    public String loadCurrent() throws IOException {
        ProcessStep step = process.deserializeFileToMaps();
        if (step != ProcessStep.SER_FILE_ABSENT) {
            log.info("current Ps and Structure maps loaded");
            return "current maps loading complete!";
        }
        log.info(step.message);
        return step.message;

    }

    /**
     * Serialize string.
     *
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/process/serialize", produces = MediaType.APPLICATION_JSON_VALUE)
    public String serialize() throws IOException {
        ProcessStep step = process.serializeMapsToFile();
        if (step == ProcessStep.CONTINUE) {
            log.info("new Ps and Structure maps serialized");
            return "new Ps and Structure maps serialization complete!";
        }
        log.info(step.message);
        return step.message;
    }


    /**
     * Diff string.
     *
     * @return the string
     */
    @PostMapping(value = "/process/diff", produces = MediaType.APPLICATION_JSON_VALUE)
    public String diff() {
        log.info("computing map differential");
        process.computeDiff();
        log.info("computing map differential complete");
        return "computing map differential complete!";
    }

    /**
     * Upload string.
     *
     * @return the string
     */
    @PostMapping(value = "/process/upload/diff", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> uploadDiff() throws IOException {
        log.info("uploading changes");
        ProcessStep step = process.uploadChanges();
        if (step == ProcessStep.DIFF_NOT_COMPUTED) {
            return new ResponseEntity<>(step.message, HttpStatus.PRECONDITION_FAILED);
        }
        FilesUtils.cleanup(filesDirectory);
        log.info("uploading changes finished");
        return new ResponseEntity<>("uploading changes complete!", HttpStatus.OK);
    }

    @PostMapping(value = "/process/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public String runFullProcess() throws IOException {
        log.info("running full process");
        ProcessStep currentStep;

        currentStep = process.loadLatestFile();
        if (currentStep != ProcessStep.CONTINUE) {
            return currentStep.message;
        }

        currentStep = process.deserializeFileToMaps();
        if (currentStep != ProcessStep.CONTINUE) {
            return currentStep.message;
        }
        process.computeDiff();
        currentStep = process.serializeMapsToFile();
        if (currentStep != ProcessStep.CONTINUE) {
            return currentStep.message;
        }
        currentStep = process.uploadChanges();
        if (currentStep != ProcessStep.CONTINUE) {
            return currentStep.message;
        }
        log.info("full upload finished");
        return "full upload complete!";
    }

    // TODO : webhook callback
    // #1 if (continue)

    @PostMapping(value="/process/continue", produces = MediaType.APPLICATION_JSON_VALUE)
    public String continueProcess() throws IOException {
        log.info("resuming process");
        ProcessStep currentStep;

        currentStep = process.serializeMapsToFile();if (currentStep != ProcessStep.CONTINUE) {
            return currentStep.message;
        }
        currentStep = process.uploadChanges();
        if (currentStep != ProcessStep.CONTINUE) {
            return currentStep.message;
        }
        FilesUtils.cleanup(filesDirectory);
        log.info("full upload finished after resume");
        return "full upload after resume !";
    }

    // #2 if not
    @PostMapping(value="/process/abort", produces = MediaType.APPLICATION_JSON_VALUE)
    public String abortProcess() throws IOException {
        log.info("aborting process");
        // TBD
        return "process aborted";
    }

}
