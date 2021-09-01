package fr.ans.psc.pscload.component;

public enum ProcessStep {
    CONTINUE("Process step OK"),
    ZIP_FILE_ABSENT("No zip has been found"),
    TXT_FILE_ALREADY_EXISTING("Unzipped file is not different from existing txt file"),
    TXT_FILE_ABSENT("No txt file has been found"),
    SER_FILE_ABSENT("No serialized file has been found"),
    DIFF_NOT_COMPUTED("Diffs have not been computed yet");

    public String message;

    ProcessStep(String message) {this.message = message;}
}
