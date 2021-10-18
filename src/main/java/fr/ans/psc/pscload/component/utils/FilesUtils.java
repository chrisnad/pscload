package fr.ans.psc.pscload.component.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The type Files utils.
 */
public class FilesUtils {

    /**
     * The logger.
     */
    private static final Logger log = LoggerFactory.getLogger(FilesUtils.class);

    /**
     * Instantiates a new Files utils.
     */
    FilesUtils() {}

    /**
     * Unzip.
     *
     * @param zipFilePath the zip file path
     * @return true if a new file is found and unzipped successfully
     * @throws IOException io exception
     */
    public static boolean unzip(String zipFilePath) throws IOException {
        return unzip(zipFilePath, false);
    }

    public static String getDateStringFromFileName(File file) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddhhmm");

        String regex = ".*(\\d{12}).*";
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(file.getName());
        if (m.find()) {
            return m.group(1);
        }
        return dateFormatter.format(new Date(0));
    }

    /**
     * Unzip.
     *
     * @param zipFilePath the zip file path
     * @param clean      set to true to delete the zip file after unzipping
     * @return true if a new file is found and unzipped successfully
     * @throws IOException io exception
     */
    public static boolean unzip(String zipFilePath, boolean clean) throws IOException {
        File zip = new File(zipFilePath);
        File destDir = zip.getParentFile();
        File[] existingFiles = zipsTextsNSers(destDir.listFiles()).get("txts").toArray(new File[0]);

        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry zipEntry = zis.getNextEntry();

        boolean goAhead = false;
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            // check only entries that are files
            if (!zipEntry.isDirectory()) {
                // check if newer than what exists, otherwise go to next entry
                if (isNew(newFile, existingFiles)) {
                    goAhead = true;
                } else {
                    log.info("{} is not new, will not be extracted", newFile.getName());
                    zipEntry = zis.getNextEntry();
                    continue;
                }
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }
                // write file content
                log.info("unzipping into {}", newFile.getName());
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                log.info("unzip complete!");
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        if (clean) {
            log.info("clean set to true, deleting {}", zip.getName());
            zip.delete();
        }
        return goAhead;
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    /**
     * Deletes all except latest files.
     *
     * @param filesDirectory the files directory
     */
    public static void cleanup(String filesDirectory) {
        log.info("Cleaning files repository, removing all but latest files");
        Map<String, List<File>> filesMap = zipsTextsNSers(new File(filesDirectory).listFiles());

        List<File> listOfZips = filesMap.get("zips");
        List<File> listOfExtracts = filesMap.get("txts");
        List<File> listOfSers = filesMap.get("sers");

        // Order files lists from oldest to newest by comparing parsed dates,
        // but honestly same result if we had used file name String to compare
        listOfZips.sort(FilesUtils::compare);
        listOfExtracts.sort(FilesUtils::compare);
        listOfSers.sort(FilesUtils::compare);

        if (listOfZips.size() > 0) {
            listOfZips.remove(listOfZips.size() -1);
        }
        if (listOfExtracts.size() > 0) {
            listOfExtracts.remove(listOfExtracts.size() -1);
        }
        if (listOfSers.size() > 0) {
            listOfSers.remove(listOfSers.size() -1);
        }

        for (File file : listOfZips) {
            file.delete();
        }
        for (File file : listOfExtracts) {
            file.delete();
        }
        for (File file : listOfSers) {
            file.delete();
        }
    }

    /**
     * Gets latest extract and ser.
     *
     * @param filesDirectory the files directory
     * @return the latest ext and ser files as map, null value if file doesnt exist
     */
    public static Map<String, File> getLatestExtAndSer(String filesDirectory) {
        Map<String, List<File>> filesMap = zipsTextsNSers(new File(filesDirectory).listFiles());

        List<File> listOfExtracts = filesMap.get("txts");
        List<File> listOfSers = filesMap.get("sers");

        // Order files lists from oldest to newest by comparing parsed dates,
        // but honestly same result if we had used file name String to compare
        listOfExtracts.sort(FilesUtils::compare);
        listOfSers.sort(FilesUtils::compare);

        Map<String, File> latestFiles = new HashMap<>();

        if (listOfExtracts.isEmpty()) {
            latestFiles.put("txt", null);
        } else {
            latestFiles.put("txt", listOfExtracts.get(listOfExtracts.size() -1));
        }

        if (listOfSers.isEmpty()) {
            latestFiles.put("ser", null);
        } else {
            latestFiles.put("ser", listOfSers.get(listOfSers.size() -1));
        }

        return latestFiles;
    }

    /**
     * Zips and texts map.
     *
     * @param listOfFiles the list of files
     * @return the map
     */
    private static Map<String, List<File>> zipsTextsNSers(File[] listOfFiles) {
        Map<String, List<File>> filesMap = new HashMap<>();
        filesMap.put("zips", new ArrayList<>());
        filesMap.put("txts", new ArrayList<>());
        filesMap.put("sers", new ArrayList<>());

        for (File file : listOfFiles != null ? listOfFiles : new File[0]) {
            if (file.getName().endsWith(".ser")) {
                filesMap.get("sers").add(file);
            } else if (file.getName().endsWith(".zip")) {
                filesMap.get("zips").add(file);
            } else if (file.getName().endsWith(".txt")) {
                filesMap.get("txts").add(file);
            }
        }
        return filesMap;
    }

    private static int compare(File f1, File f2) {
        try {
            return getDateFromFileName(f1).compareTo(getDateFromFileName(f2));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static boolean isNew(File f1, File[] listF2) {
        if (listF2.length == 0) {
            return true;
        }
        for (File f2 : listF2) {
            if (compare(f1, f2) > 0) {
                return true;
            }
        }
        return false;
    }

    private static Date getDateFromFileName(File file) throws ParseException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddhhmm");

        String regex = ".*(\\d{12}).*";
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(file.getName());
        if (m.find()) {
            return dateFormatter.parse(m.group(1));
        }
        return new Date(0);
    }

}
