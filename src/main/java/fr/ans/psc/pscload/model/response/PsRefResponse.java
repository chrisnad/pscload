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
    private PsRef[] psRefs;

    public PsRef[] getPsRefs() {
        return psRefs;
    }

    public void setPsRefs(PsRef[] psRefs) {
        this.psRefs = psRefs;
    }
}
