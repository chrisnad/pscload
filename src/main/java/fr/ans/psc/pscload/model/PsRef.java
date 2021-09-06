package fr.ans.psc.pscload.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PsRef {

    @JsonProperty
    private String nationalIdRef;

    @JsonProperty
    private String nationalId;

    public PsRef(String nationalIdRef, String nationalId) {
        this.nationalIdRef = nationalIdRef;
        this.nationalId = nationalId;
    }

    public PsRef(String[] items) {
        this.nationalIdRef = "8" + items[1];
        this.nationalId = "0" + items[0];
    }

    public String getNationalIdRef() {
        return nationalIdRef;
    }

    public void setNationalIdRef(String nationalIdRef) {
        this.nationalIdRef = nationalIdRef;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }
}
