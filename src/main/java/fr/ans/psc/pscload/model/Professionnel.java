package fr.ans.psc.pscload.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.*;

/**
 * The type Professionnel.
 */
public class Professionnel implements Serializable {

    @JsonProperty("idType")
    private String idType;

    @JsonProperty("id")
    private String id;

    @JsonProperty("nationalId")
    private String nationalId;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("dateOfBirth")
    private String dateOfBirth;

    @JsonProperty("birthAddressCode")
    private String birthAddressCode;

    @JsonProperty("birthCountryCode")
    private String birthCountryCode;

    @JsonProperty("birthAddress")
    private String birthAddress;

    @JsonProperty("genderCode")
    private String genderCode;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("email")
    private String email;

    @JsonProperty("salutationCode")
    private String salutationCode;

    @JsonProperty("professions")
    private final List<ExerciceProfessionnel> professions = new ArrayList<>();

    public Professionnel() {}

    public Professionnel(String[] items) {
        this(items, true);
    }

    public Professionnel(String[] items, boolean deep) {
        this.idType = items[0];
        this.id = items[1];
        this.nationalId = items[2];
        this.lastName = items[3];
        this.firstName = items[4];
        this.dateOfBirth = items[5];
        this.birthAddressCode = items[6];
        this.birthCountryCode = items[7];
        this.birthAddress = items[8];
        this.genderCode = items[9];
        this.phone = items[10];
        this.email = items[11];
        this.salutationCode = items[12];
        if (deep) {
            this.professions.add(new ExerciceProfessionnel(items));
        }
    }

    public Professionnel(Professionnel ps) {
        this.idType = ps.idType;
        this.id = ps.id;
        this.nationalId = ps.nationalId;
        this.lastName = ps.lastName;
        this.firstName = ps.firstName;
        this.dateOfBirth = ps.dateOfBirth;
        this.birthAddressCode = ps.birthAddressCode;
        this.birthCountryCode = ps.birthCountryCode;
        this.birthAddress = ps.birthAddress;
        this.genderCode = ps.genderCode;
        this.phone = ps.phone;
        this.email = ps.email;
        this.salutationCode = ps.salutationCode;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getIdType() {
        return idType;
    }

    public List<ExerciceProfessionnel> getProfessions() {
        return professions;
    }

    public int nakedHash() {
        return Objects.hash(idType, id, getNationalId(), lastName, firstName, dateOfBirth, birthAddressCode, birthCountryCode, birthAddress, genderCode, phone, email, salutationCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Professionnel)) return false;
        Professionnel that = (Professionnel) o;
        return Objects.equals(idType, that.idType) && Objects.equals(id, that.id) &&
                Objects.equals(getNationalId(), that.getNationalId()) && Objects.equals(lastName, that.lastName) &&
                Objects.equals(firstName, that.firstName) && Objects.equals(dateOfBirth, that.dateOfBirth) &&
                Objects.equals(birthAddressCode, that.birthAddressCode) && Objects.equals(birthCountryCode, that.birthCountryCode) &&
                Objects.equals(birthAddress, that.birthAddress) && Objects.equals(genderCode, that.genderCode) &&
                Objects.equals(phone, that.phone) && Objects.equals(email, that.email) && Objects.equals(salutationCode, that.salutationCode) &&
                Objects.equals(getProfessions().stream().map(ExerciceProfessionnel::hashCode).reduce(0, Integer::sum),
                        that.getProfessions().stream().map(ExerciceProfessionnel::hashCode).reduce(0, Integer::sum));
    }

    @Override
    public int hashCode() {
        return Objects.hash(idType, id, getNationalId(), lastName, firstName, dateOfBirth, birthAddressCode,
                birthCountryCode, birthAddress, genderCode, phone, email, salutationCode,
                getProfessions().stream().map(ExerciceProfessionnel::hashCode).reduce(0, Integer::sum));
    }
}
