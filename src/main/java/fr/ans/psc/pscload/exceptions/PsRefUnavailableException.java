package fr.ans.psc.pscload.exceptions;

public class PsRefUnavailableException extends RuntimeException {

    private final String psRefNationalIdRef;

    public PsRefUnavailableException(String message, String psRefNationalIdRef) {
        super(message);
        this.psRefNationalIdRef = psRefNationalIdRef;
    }

    public String getPsRefNationalIdRef() {
        return psRefNationalIdRef;
    }
}
