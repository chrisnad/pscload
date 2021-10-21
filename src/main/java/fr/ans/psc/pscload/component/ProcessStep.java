package fr.ans.psc.pscload.component;

public enum ProcessStep {
    IDLE(0),
    DOWNLOADED (10),
    CURRENT_MAP_LOADED(20),
    PREVIOUS_MAP_LOADED(30),
    COMPUTE_DIFF_STARTED(40),
    COMPUTE_DIFF_FINISHED(50),
    UPLOAD_CHANGES_STARTED(60),
    UPLOAD_CHANGES_FINISHED(70),
    CURRENT_MAP_SERIALIZED(80),
    TOGGLE_RUNNING(-10);

    public int value;

    ProcessStep (int value) {
        this.value = value;
    }
}
