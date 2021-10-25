package fr.ans.psc.pscload.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import fr.ans.psc.pscload.model.PsRef;
import java.io.Serializable;

public class PsRefResponse implements Serializable {

    @JsonProperty("status")
    @SerializedName("status")
    private String status;

    @JsonProperty("message")
    @SerializedName("message")
    private String message;

    @JsonProperty("data")
    @SerializedName("data")
    private PsRef psRef;

    public PsRef getPsRef() {
        return psRef;
    }

    public void setPsRef(PsRef psRef) {
        this.psRef = psRef;
    }
}
