package fr.ans.psc.pscload.controller;

import fr.ans.psc.pscload.component.Loader;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
class LoadController {

    /**
     * The logger.
     */
    private static final Logger log = LoggerFactory.getLogger(LoadController.class);

    @Autowired
    private final Loader loader;

    @Value("${files.directory}")
    private String filesDirectory;

    @Value("${test.download.url}")
    private String testDownloadUrl;

    @Value("${extract.download.url}")
    private String extractDownloadUrl;

    LoadController(Loader loader) {
        this.loader = loader;
    }

    @GetMapping(value = "/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public String index() {
        return "health check OK";
    }

    @GetMapping(value = "/clean-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public String cleanAll() throws IOException {
        FileUtils.cleanDirectory(new File(filesDirectory));
        log.info("all files in {} were deleted!", filesDirectory);
        return "all files in storage were deleted";
    }

    @GetMapping(value = "/clean", produces = MediaType.APPLICATION_JSON_VALUE)
    public String clean() throws IOException {
        String[] fileList = Stream.of(Objects.requireNonNull(new File(filesDirectory).listFiles()))
                .map(File::getAbsolutePath).distinct().toArray(String[]::new);
        for (String file : fileList)
        {
            if (!file.endsWith(".ser")) {
                boolean isDeleted = Files.deleteIfExists(Path.of(file));
                log.info("file: {} is deleted: {}", file, isDeleted);
            }
        }
        log.info("cleanup complete!");
        return "cleanup complete";
    }

    @GetMapping(value = "/load", produces = MediaType.APPLICATION_JSON_VALUE)
    public String load() throws IOException, GeneralSecurityException {
        log.info("loading from {}", extractDownloadUrl);
        loader.downloadAndParse(extractDownloadUrl);
        log.info("loading complete!");
        return "loading complete";
    }

    @GetMapping(value = "/load-test", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String loadTest(@RequestParam String fileName) throws GeneralSecurityException, IOException {
        String downloadUrl = testDownloadUrl + fileName + ".zip";
        log.info("loading from {}", downloadUrl);
        loader.downloadAndParse(downloadUrl);
        return "loading complete";
    }

    @GetMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String listFiles() {
        return Stream.of(Objects.requireNonNull(new File(filesDirectory).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet()).toString();
    }

    @GetMapping(value = "/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public String run() throws IOException {
        log.info("running");
        loader.diffOrLoad();
        log.info("run complete!");
        return "run complete";
    }

}