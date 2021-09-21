package fr.ans.psc.pscload.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class PsRef implements Serializable {

    @JsonProperty
    @SerializedName("nationalIdRef")
    private String nationalIdRef;

    @JsonProperty
    @SerializedName("nationalId")
    private String nationalId;

    public PsRef(String nationalIdRef, String nationalId) {
        this.nationalIdRef = nationalIdRef;
        this.nationalId = nationalId;
    }

    public PsRef(String[] items) {
        this.nationalIdRef = "0" + items[0];
        this.nationalId = "8" + items[1];
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
