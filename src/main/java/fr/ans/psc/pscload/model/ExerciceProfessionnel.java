package fr.ans.psc.pscload.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.*;

public class ExerciceProfessionnel implements Serializable {

    @JsonProperty("code")
    private String code;

    @JsonProperty("categoryCode")
    private String categoryCode;

    @JsonProperty("salutationCode")
    private String salutationCode;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("expertises")
    private final List<SavoirFaire> expertises = new ArrayList<>();

    @JsonProperty("workSituations")
    private final List<SituationExercice> workSituations = new ArrayList<>();

    public ExerciceProfessionnel() {}

    public ExerciceProfessionnel(String[] items) {
        this.code = items[13];
        this.categoryCode = items[14];
        this.salutationCode = items[15];
        this.lastName = items[16];
        this.firstName = items[17];
        this.expertises.add(new SavoirFaire(items));
        this.workSituations.add(new SituationExercice(items));
    }

    public ExerciceProfessionnel(ExerciceProfessionnel exPro) {
        this.code = exPro.code;
        this.categoryCode = exPro.categoryCode;
        this.salutationCode = exPro.salutationCode;
        this.lastName = exPro.lastName;
        this.firstName = exPro.firstName;
    }

    public String getProfessionId() {
        String key = Objects.toString(code, "") +
                Objects.toString(categoryCode, "");
        if ("".equals(key)) {
            return "ND";
        }
        return key;
    }

    public String getCode() {
        String key = Objects.toString(code, "");
        if ("".equals(key)) {
            return "ND";
        }
        return key;
    }

    public List<SavoirFaire> getExpertises() {
        return expertises;
    }

    public List<SituationExercice> getWorkSituations() {
        return workSituations;
    }

    public int nakedHash() {
        return Objects.hash(code, categoryCode, salutationCode, lastName, firstName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExerciceProfessionnel)) return false;
        ExerciceProfessionnel that = (ExerciceProfessionnel) o;
        return Objects.equals(code, that.code) && Objects.equals(categoryCode, that.categoryCode) &&
                Objects.equals(salutationCode, that.salutationCode) && Objects.equals(lastName, that.lastName) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(getExpertises().stream().map(SavoirFaire::hashCode).reduce(0, Integer::sum),
                        that.getExpertises().stream().map(SavoirFaire::hashCode).reduce(0, Integer::sum)) &&
                Objects.equals(getWorkSituations().stream().map(SituationExercice::hashCode).reduce(0, Integer::sum),
                        that.getWorkSituations().stream().map(SituationExercice::hashCode).reduce(0, Integer::sum));
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, categoryCode, salutationCode, lastName, firstName,
                getExpertises().stream().map(SavoirFaire::hashCode).reduce(0, Integer::sum),
                getWorkSituations().stream().map(SituationExercice::hashCode).reduce(0, Integer::sum));
    }

    @Override
    public String toString() {
        return code + '|' + categoryCode + '|' + salutationCode + '|' + lastName + '|' + firstName;
    }
}
