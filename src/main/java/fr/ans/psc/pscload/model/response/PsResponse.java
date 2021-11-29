package fr.ans.psc.pscload.model.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import fr.ans.psc.pscload.model.Professionnel;

import java.io.Serializable;

public class PsResponse implements Serializable {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Professionnel data;

    public Professionnel getData() {
        return data;
    }
}
