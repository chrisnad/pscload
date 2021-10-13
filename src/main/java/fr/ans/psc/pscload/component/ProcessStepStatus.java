package fr.ans.psc.pscload.component;

public enum ProcessStepStatus {
    INIT_STEP("Process started"),
    CONTINUE("Process step OK"),
    ZIP_FILE_ABSENT("No zip has been found"),
    TXT_FILE_ALREADY_EXISTING("Unzipped file is not different from existing txt file"),
    TXT_FILE_ABSENT("No txt file has been found"),
    SER_FILE_ABSENT("No serialized file has been found"),
    DIFF_NOT_COMPUTED("Diffs have not been computed yet"),
    INVALID_SER_FILE_PATH("Invalid ser file path"),
    PSCEXTRACT_ENDPOINT_FAILURE("Error during pscextract endpoint call"),
    FILE_READING_ERROR("Error during file reading"),
    ABORT("Upload changes process still running, launch aborted");

    public String message;

    ProcessStepStatus(String message) {this.message = message;}
}
