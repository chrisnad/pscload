package fr.ans.psc.pscload.controller;

import fr.ans.psc.pscload.component.Process;
import fr.ans.psc.pscload.component.ProcessStepStatus;
import fr.ans.psc.pscload.component.utils.FilesUtils;
import fr.ans.psc.pscload.exceptions.ConcurrentProcessCallException;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;
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
@Controller
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
    @ResponseBody
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
    @ResponseBody
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
    @ResponseBody
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
    @ResponseBody
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
    @ResponseBody
    public String downloadTest(@RequestParam String fileName) throws IOException, GeneralSecurityException {
        log.info("cleaning files repository before download");
        FilesUtils.cleanup(filesDirectory);
        String downloadUrl = testDownloadUrl + fileName + ".zip";
        log.info("downloading from {}", downloadUrl);
        ProcessStepStatus step = process.downloadAndUnzip(downloadUrl);
        if (step == ProcessStepStatus.CONTINUE) {
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
    @PostMapping(value = "/process/download", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ModelAndView download() throws IOException, GeneralSecurityException {
        log.info("cleaning files repository before download");
        FilesUtils.cleanup(filesDirectory);
        log.info("downloading from {}", extractDownloadUrl);
        ModelAndView mav = initializeMAV("Téléchargement de l'archive RASS réussie.");
        ProcessStepStatus step = process.downloadAndUnzip(extractDownloadUrl);
        mav.addObject("step", step);
        log.info(step == ProcessStepStatus.CONTINUE ? "download complete!" : step.message);

        return mav;
    }

    /**
     * Load string.
     *
     * @return the string
     */
    @PostMapping(value = "/process/load/new", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ModelAndView loadNew() {
        ModelAndView mav = initializeMAV("Chargement des nouvelles maps PS et Structure réussie.");
        try {
            ProcessStepStatus step = process.loadLatestFile();
            mav.addObject("step", step);
            log.info(step == ProcessStepStatus.CONTINUE ? "new Ps and Structure maps loaded" : step.message);
            return mav;
        } catch (ConcurrentProcessCallException e) {
            log.info(e.getMessage());
            ModelAndView errorMav = initializeMAV(e.getMessage());
            errorMav.addObject("step", ProcessStepStatus.ABORT);
            errorMav.setStatus(HttpStatus.CONFLICT);
            return errorMav;
        }
    }

    /**
     * Load current string.
     *
     * @return the string
     */
    @PostMapping(value = "/process/load/current", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelAndView loadCurrent() {
        ModelAndView mav = initializeMAV("Chargement des maps courantes PS et Structure réussie.");
        try {
            ProcessStepStatus step = process.deserializeFileToMaps();
            mav.addObject("step", step);
            log.info(step == ProcessStepStatus.CONTINUE ? "current Ps and Structure maps loaded" : step.message);
            return mav;
        } catch (ConcurrentProcessCallException e) {
            log.info(e.getMessage());
            ModelAndView errorMav = initializeMAV(e.getMessage());
            errorMav.addObject("step", ProcessStepStatus.ABORT);
            errorMav.setStatus(HttpStatus.CONFLICT);
            return errorMav;
        }
    }

    /**
     * Serialize ModelAndView.
     *
     * @return the ModelAndView
     */
    @PostMapping(value = "/process/serialize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelAndView serialize() {
        ModelAndView mav = initializeMAV("Sérialisation des maps PS et Structure réussie.");
        ProcessStepStatus step = process.serializeMapsToFile();
        mav.addObject("step", step);
        log.info(step == ProcessStepStatus.CONTINUE ? "new Ps and Structure maps serialized" : step.message);
        return mav;
    }


    /**
     * Diff ModelAndView.
     *
     * @return the ModelAndView
     */
    @PostMapping(value = "/process/diff", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelAndView diff() {
        log.info("computing map differential");
        try {
            process.computeDiff();
            log.info("computing map differential complete");
            ModelAndView mav = initializeMAV("Les différences entre les fichiers ont bien été calculées.");
            mav.addObject("step", ProcessStepStatus.CONTINUE);
            return mav;
        } catch (ConcurrentProcessCallException e) {
            log.info(e.getMessage());
            ModelAndView errorMav = initializeMAV(e.getMessage());
            errorMav.addObject("step", ProcessStepStatus.ABORT);
            errorMav.setStatus(HttpStatus.CONFLICT);
            return errorMav;
        }
    }

    /**
     * Upload ModelAndView.
     *
     * @return ModelAndView
     */
    @PostMapping(value = "/process/upload/diff", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelAndView uploadDiff() {
        log.info("uploading changes");
        ModelAndView mav = initializeMAV("Les changements du jour ont bien été chargés.");

        try {
            ProcessStepStatus step = process.uploadChanges();
            mav.addObject("step", step);
            if (step == ProcessStepStatus.DIFF_NOT_COMPUTED) {
                return mav;
            }
            FilesUtils.cleanup(filesDirectory);
            log.info("uploading changes finished");

            return mav;
        } catch (ConcurrentProcessCallException e) {
            log.info(e.getMessage());
            ModelAndView errorMav = initializeMAV(e.getMessage());
            errorMav.addObject("step", ProcessStepStatus.ABORT);
            errorMav.setStatus(HttpStatus.CONFLICT);
            return errorMav;
        }
    }

    @PostMapping(value = "/process/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelAndView runFullProcess() {
        log.info("running full process");
        ModelAndView mav = initializeMAV("Le process complet s'est déroulé convenablement. Les changements du jour ont bien été chargés.");
        ProcessStepStatus currentStepStatus = process.runFirst();
        if (currentStepStatus != ProcessStepStatus.ABORT) {
            currentStepStatus = process.runContinue();
        }
        mav.addObject("step", currentStepStatus);
        return mav;
    }

    @PostMapping(value = "/process/continue", produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelAndView continueProcess() {
        log.info("resuming process");
        ModelAndView mav;
        ProcessStepStatus currentStepStatus;

        currentStepStatus = process.runContinue();

        if (currentStepStatus != ProcessStepStatus.ABORT) {
            mav = initializeMAV("Les changements du jour ont bien été chargés après reprise du process.");
        } else {
            mav = initializeMAV(currentStepStatus.message);
            mav.setStatus(HttpStatus.CONFLICT);
        }
        mav.addObject("step", currentStepStatus);
        log.info("continue process finished with status : " + currentStepStatus.name());
        return mav;
    }

    private ModelAndView initializeMAV(String message) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("process-step-feedback");
        mav.addObject("message", message);
        return mav;
    }

    @PostMapping(value = "/toggle", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String toggleRegistrySource(@RequestParam("toggleFile") MultipartFile mpFile) throws IOException {

        File toggleFile = process.uploadToggleFile(mpFile);

        ProcessStepStatus step = process.loadToggleMaps(toggleFile);
        if (step != ProcessStepStatus.CONTINUE) {
            return step.message;
        }
        process.uploadPsRefsAfterToggle();
        return ProcessStepStatus.CONTINUE.message;
    }

    public String checkToggleErrors(@RequestParam("toggleFile") MultipartFile mpFile) {
        try {
            File toggleFile = process.uploadToggleFile(mpFile);
            process.checkToggleErrors(toggleFile);
            return ProcessStepStatus.CONTINUE.message;
        } catch (IOException e) {
            log.error("could not check toggle errors", e);
        }
        return ProcessStepStatus.ABORT.message;


    }

}
