package fr.ans.psc.pscload.service;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import fr.ans.psc.pscload.component.JsonFormatter;
import fr.ans.psc.pscload.exceptions.PsRefUnavailableException;
import fr.ans.psc.pscload.metrics.CustomMetrics;
import fr.ans.psc.pscload.model.*;
import fr.ans.psc.pscload.service.task.Create;
import fr.ans.psc.pscload.service.task.Delete;
import fr.ans.psc.pscload.service.task.Update;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The type Psc rest api.
 */
@Service
public class PscRestApi {

    private static final Logger log = LoggerFactory.getLogger(PscRestApi.class);

    @Autowired
    private CustomMetrics customMetrics;

    @Autowired
    private JsonFormatter jsonFormatter;

    final Request.Builder requestBuilder = new Request.Builder().header("Connection", "close");

    @Value("${api.base.url}")
    private String apiBaseUrl;

    @Value("${deactivation.excluded.profession.codes:}")
    private String[] excludedProfessions;

    /**
     * Diff PS maps.
     *
     * @param original OG PS map
     * @param revised  the revised PS map
     * @return the map difference
     */
    public MapDifference<String, Professionnel> diffPsMaps(Map<String, Professionnel> original, Map<String, Professionnel> revised) {
        MapDifference<String, Professionnel> psDiff = Maps.difference(original, revised);

        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_ANY_DELETE_SIZE).set(psDiff.entriesOnlyOnLeft().size());
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_ANY_CREATE_SIZE).set(psDiff.entriesOnlyOnRight().size());
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_ANY_UPDATE_SIZE).set(psDiff.entriesDiffering().size());

        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_ADELI_DELETE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnLeft().values().stream().filter(ps -> CustomMetrics.ID_TYPE.ADELI.value.equals(ps.getIdType())).count()));
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_ADELI_CREATE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnRight().values().stream().filter(ps -> CustomMetrics.ID_TYPE.ADELI.value.equals(ps.getIdType())).count()));
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_ADELI_UPDATE_SIZE).set(
                Math.toIntExact(psDiff.entriesDiffering().values().stream().filter(ps -> CustomMetrics.ID_TYPE.ADELI.value.equals(ps.leftValue().getIdType())).count()));

        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_FINESS_DELETE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnLeft().values().stream().filter(ps -> CustomMetrics.ID_TYPE.FINESS.value.equals(ps.getIdType())).count()));
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_FINESS_CREATE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnRight().values().stream().filter(ps -> CustomMetrics.ID_TYPE.FINESS.value.equals(ps.getIdType())).count()));
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_FINESS_UPDATE_SIZE).set(
                Math.toIntExact(psDiff.entriesDiffering().values().stream().filter(ps -> CustomMetrics.ID_TYPE.FINESS.value.equals(ps.leftValue().getIdType())).count()));

        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_SIRET_DELETE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnLeft().values().stream().filter(ps -> CustomMetrics.ID_TYPE.SIRET.value.equals(ps.getIdType())).count()));
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_SIRET_CREATE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnRight().values().stream().filter(ps -> CustomMetrics.ID_TYPE.SIRET.value.equals(ps.getIdType())).count()));
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_SIRET_UPDATE_SIZE).set(
                Math.toIntExact(psDiff.entriesDiffering().values().stream().filter(ps -> CustomMetrics.ID_TYPE.SIRET.value.equals(ps.leftValue().getIdType())).count()));

        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_RPPS_DELETE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnLeft().values().stream().filter(ps -> CustomMetrics.ID_TYPE.RPPS.value.equals(ps.getIdType())).count()));
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_RPPS_CREATE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnRight().values().stream().filter(ps -> CustomMetrics.ID_TYPE.RPPS.value.equals(ps.getIdType())).count()));
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_RPPS_UPDATE_SIZE).set(
                Math.toIntExact(psDiff.entriesDiffering().values().stream().filter(ps -> CustomMetrics.ID_TYPE.RPPS.value.equals(ps.leftValue().getIdType())).count()));

        return psDiff;
    }

    /**
     * Diff structure maps.
     *
     * @param original the original
     * @param revised  the revised
     * @return the map difference
     */
    public MapDifference<String, Structure> diffStructureMaps(Map<String, Structure> original, Map<String, Structure> revised) {
        MapDifference<String, Structure> structureDiff = Maps.difference(original, revised);

        customMetrics.getAppStructureSizeGauges().get(CustomMetrics.StructureCustomMetric.STRUCTURE_DELETE_SIZE).set(structureDiff.entriesOnlyOnLeft().size());
        customMetrics.getAppStructureSizeGauges().get(CustomMetrics.StructureCustomMetric.STRUCTURE_CREATE_SIZE).set(structureDiff.entriesOnlyOnRight().size());
        customMetrics.getAppStructureSizeGauges().get(CustomMetrics.StructureCustomMetric.STRUCTURE_UPDATE_SIZE).set(structureDiff.entriesDiffering().size());

        return structureDiff;
    }

    /**
     * Upload changes.
     *
     * @param psDiff        the ps diff
     * @param structureDiff the structure diff
     */
    public void uploadChanges(MapDifference<String, Professionnel> psDiff,
                              MapDifference<String, Structure> structureDiff) {
        int psChangesCount = psDiff.entriesOnlyOnLeft().size()
                + psDiff.entriesOnlyOnRight().size()
                + psDiff.entriesDiffering().size();
        int structureChangesCount = structureDiff.entriesOnlyOnLeft().size()
                + structureDiff.entriesOnlyOnRight().size()
                + structureDiff.entriesDiffering().size();
        log.info("Ps changes count : " + psChangesCount);
        log.info("Structure changes count : " + structureChangesCount);
        injectPsDiffTasks(psDiff);
        injectStructuresDiffTasks(structureDiff);
    }

    private void injectPsDiffTasks(MapDifference<String, Professionnel> diff) {
        customMetrics.getAppProgressionGauges().get(CustomMetrics.ProgressionCustomMetric.PS_DELETE_PROGRESSION).set(0);
        customMetrics.getAppProgressionGauges().get(CustomMetrics.ProgressionCustomMetric.PS_CREATE_PROGRESSION).set(0);
        customMetrics.getAppProgressionGauges().get(CustomMetrics.ProgressionCustomMetric.PS_UPDATE_PROGRESSION).set(0);

        diff.entriesOnlyOnLeft().values().parallelStream().forEach(ps -> {
            List<ExerciceProfessionnel> psExPros = ps.getProfessions();
            AtomicBoolean deletable = new AtomicBoolean(true);

            psExPros.forEach(exerciceProfessionnel -> {
                if (excludedProfessions != null && Arrays.stream(excludedProfessions)
                        .anyMatch(profession -> exerciceProfessionnel.getCode().equals(profession))) {
                    deletable.set(false);
                }
            });

            if (deletable.get()) {
                new Delete(getPsUrl(ps.getNationalId())).send();
                customMetrics.getAppProgressionGauges().get(CustomMetrics.ProgressionCustomMetric.PS_DELETE_PROGRESSION).incrementAndGet();
            }
        });

        diff.entriesOnlyOnRight().values().parallelStream().forEach(ps -> {
            new Create(getPsUrl() + "/force", jsonFormatter.jsonFromObject(ps)).send();
            customMetrics.getAppProgressionGauges().get(CustomMetrics.ProgressionCustomMetric.PS_CREATE_PROGRESSION).incrementAndGet();
        });
        diff.entriesDiffering().values().parallelStream().forEach(v -> {
            injectPsUpdateTasks(v.leftValue(), v.rightValue());
            customMetrics.getAppProgressionGauges().get(CustomMetrics.ProgressionCustomMetric.PS_UPDATE_PROGRESSION).incrementAndGet();
        });
    }

    private void injectStructuresDiffTasks(MapDifference<String, Structure> diff) {
        customMetrics.getAppProgressionGauges().get(CustomMetrics.ProgressionCustomMetric.STRUCTURE_DELETE_PROGRESSION).set(0);
        customMetrics.getAppProgressionGauges().get(CustomMetrics.ProgressionCustomMetric.STRUCTURE_CREATE_PROGRESSION).set(0);
        customMetrics.getAppProgressionGauges().get(CustomMetrics.ProgressionCustomMetric.STRUCTURE_UPDATE_PROGRESSION).set(0);

        diff.entriesOnlyOnRight().values().parallelStream().forEach(structure -> {
            new Create(getStructureUrl(), jsonFormatter.jsonFromObject(structure)).send();
            customMetrics.getAppProgressionGauges().get(CustomMetrics.ProgressionCustomMetric.STRUCTURE_CREATE_PROGRESSION).incrementAndGet();
        });
        diff.entriesDiffering().values().parallelStream().forEach(v -> {
            new Update(getStructureUrl(v.leftValue().getStructureId()), jsonFormatter.jsonFromObject(v.rightValue())).send();
            customMetrics.getAppProgressionGauges().get(CustomMetrics.ProgressionCustomMetric.STRUCTURE_UPDATE_PROGRESSION).incrementAndGet();
        });
    }

    private void injectPsUpdateTasks(Professionnel left, Professionnel right) {
        String psUrl = getPsUrl(left.getNationalId());

        if (left.nakedHash() != right.nakedHash()) {
            // update Ps basic attributes
            new Update(psUrl, jsonFormatter.nakedPsFromObject(right)).send();
        }

        // diff professions
        Map<String, ExerciceProfessionnel> leftExPro = Maps
                .uniqueIndex(left.getProfessions(), ExerciceProfessionnel::getProfessionId);
        Map<String, ExerciceProfessionnel> rightExPro = Maps
                .uniqueIndex(right.getProfessions(), ExerciceProfessionnel::getProfessionId);
        MapDifference<String, ExerciceProfessionnel> exProDiff = Maps.difference(leftExPro, rightExPro);

        exProDiff.entriesOnlyOnLeft().forEach((k, v) -> new Delete(getExProUrl(psUrl, v.getProfessionId())).send());
        exProDiff.entriesOnlyOnRight().forEach((k, v) -> new Create(getExProUrl(psUrl), jsonFormatter.jsonFromObject(v)).send());
        exProDiff.entriesDiffering().forEach((k, v) -> injectExProUpdateTasks(v.leftValue(), v.rightValue(), psUrl));
    }

    private void injectExProUpdateTasks(ExerciceProfessionnel leftExPro, ExerciceProfessionnel rightExPro, String psUrl) {
        String exProUrl = getExProUrl(psUrl, leftExPro.getProfessionId());

        if (leftExPro.nakedHash() != rightExPro.nakedHash()) {
            // update ExPro basic attributes
            new Update(exProUrl, jsonFormatter.nakedExProFromObject(rightExPro)).send();
        }

        // diff expertises
        Map<String, SavoirFaire> leftExpertises = Maps
                .uniqueIndex(leftExPro.getExpertises(), SavoirFaire::getExpertiseId);
        Map<String, SavoirFaire> rightExpertises = Maps
                .uniqueIndex(rightExPro.getExpertises(), SavoirFaire::getExpertiseId);
        MapDifference<String, SavoirFaire> expertiseDiff = Maps.difference(leftExpertises, rightExpertises);

        expertiseDiff.entriesOnlyOnLeft().forEach((k, v) -> new Delete(getExpertiseUrl(exProUrl, v.getExpertiseId())).send());
        expertiseDiff.entriesOnlyOnRight().forEach((k, v) -> new Create(getExpertiseUrl(exProUrl), jsonFormatter.jsonFromObject(v)).send());
        expertiseDiff.entriesDiffering().forEach((k, v) -> new Update(
                getExpertiseUrl(exProUrl, v.rightValue().getExpertiseId()), jsonFormatter.jsonFromObject(v.rightValue())).send());

        // diff situations
        Map<String, SituationExercice> leftSituations = Maps
                .uniqueIndex(leftExPro.getWorkSituations(), SituationExercice::getSituationId);
        Map<String, SituationExercice> rightSituations = Maps
                .uniqueIndex(rightExPro.getWorkSituations(), SituationExercice::getSituationId);
        MapDifference<String, SituationExercice> situationDiff = Maps.difference(leftSituations, rightSituations);

        situationDiff.entriesOnlyOnLeft().forEach((k, v) -> new Delete(getSituationUrl(exProUrl, v.getSituationId())).send());
        situationDiff.entriesOnlyOnRight().forEach((k, v) -> new Create(getSituationUrl(exProUrl), jsonFormatter.jsonFromObject(v)).send());
        situationDiff.entriesDiffering().forEach((k, v) ->
                new Update(getSituationUrl(exProUrl, v.rightValue().getSituationId()), jsonFormatter.jsonFromObject(v.rightValue())).send());
    }

    public void uploadPsRefs(Map<String, PsRef> psRefCreateMap) {

        psRefCreateMap.values().parallelStream().forEach(psRef -> {
            try {
                togglePsRefIfNeeded(psRef);
            } catch (PsRefUnavailableException e) {
                log.error(e.getMessage());
            }
        });
    }

    private PsRef getStoredPsRef(String nationalIdRef) throws PsRefUnavailableException {
        OkHttpClient client = new OkHttpClient();
        Request request = requestBuilder.url(getPsRefUrl() + "/" + nationalIdRef).get().build();

        PsRef storedPsRef;

        try {
            Call call = client.newCall(request);
            Response response = call.execute();
            String responseBody = Objects.requireNonNull(response.body()).string();

            storedPsRef = jsonFormatter.psRefFromJson(responseBody);
            if (storedPsRef != null) {
                log.debug("idRef : " + storedPsRef.getNationalIdRef() + " idNat : " + storedPsRef.getNationalId());
            } else {
                log.debug("PsRef not found");
                throw new PsRefUnavailableException("PsRef not found ", nationalIdRef);
            }
            response.close();
        } catch (PsRefUnavailableException e) {
            log.debug("Error while querying stored PsRef : " + nationalIdRef, e);
            throw new PsRefUnavailableException("Error while querying stored PsRef : ", nationalIdRef);
        } catch (IOException ioe) {
            log.error("I/O Exception when trying to get PsRef " + nationalIdRef);
            throw new PsRefUnavailableException("Error while querying stored PsRef : ", nationalIdRef);
        }
        return storedPsRef;
    }

    private Professionnel getStoredProfessionnel(String nationalIdRef) throws PsRefUnavailableException {
        OkHttpClient client = new OkHttpClient();
        Request request = requestBuilder.url(getPsUrl() + "/" + nationalIdRef).get().build();

        Professionnel storedProfessionnel;

        try {
            Call call = client.newCall(request);
            Response response = call.execute();
            String responseBody = Objects.requireNonNull(response.body()).string();

            storedProfessionnel = jsonFormatter.psFromJson(responseBody).getData();
            if (storedProfessionnel == null) {
                log.info("Ps not found");
                throw new Exception("PsRef not found");
            }
            response.close();
        } catch (Exception e) {
            log.error("Error while querying stored Ps : " + nationalIdRef, e);
            throw new PsRefUnavailableException("Error while querying stored PsRef : ", nationalIdRef);
        }
        return storedProfessionnel;
    }

    /*
    * method used for toggle operations :
    * @param psref is a PsRef from toggle table loaded in a map
    * it contains an old Ps nationalId as PsRef.nationalIdRef, and a fresh Ps nationalId as PsRef.nationalId
    * we want to make old indexes point on new ones, so then we can destroy old PS and add the PsRef from toggle-table in db
    *
    * before operation, we have in the toggle table : oldPs -> newPs
    * and in Ps table : oldPs, newPS
    * and in PsRef table : oldPs -> oldPs, newPs -> newPs
    *
    * after operation we have in Ps table : newPs
    * and in PsRef table : oldPs -> newPs, newPs -> newPs
    *
    * the toggle table could contains PsRef that have already been processed previously, because this operation could be
    * run several times, monthly or quarterly. It is not ideal, but we don't have the final word on the data source format.
    * So we have to make checks to not replay an operation already done
     */
    private void togglePsRefIfNeeded(PsRef psRef) throws PsRefUnavailableException {
        // first, we get the PsRef stored in db with the same nationalIdRef than in the toggle table
        PsRef storedPsRef = getStoredPsRef(psRef.getNationalIdRef());

        // if we get a response AND the nationalId is not the same as in the toggle table, that means that the toggle hasn't been
        // processed yet for this Ps. So we continue
        if (storedPsRef != null && !psRef.getNationalId().equals(storedPsRef.getNationalId())) {
            // now we want to definitively destroy oldPs and oldPsRef, but only if newPs already exists in db
            Professionnel newIndexedPs = getStoredProfessionnel(psRef.getNationalId());
            if (newIndexedPs != null) {
                new Delete(getPsUrl() + "/force/" + psRef.getNationalIdRef()).send();
                new Create(getPsRefUrl(), jsonFormatter.jsonFromObject(psRef)).send();
            } else {
                log.error("Ps with old index : {} and new index : {} cannot be updated because new Ps does not exist in db",
                        psRef.getNationalIdRef(), psRef.getNationalId());
            }
        }
    }

    public void checkToggleErrors(Map<String, PsRef> psRefMap) {
        psRefMap.values().parallelStream().forEach(this::logErrorIfToggleIsWrong);
    }

    private void logErrorIfToggleIsWrong(PsRef psRef) {
        PsRef storedPsRef = getStoredPsRef(psRef.getNationalIdRef());

        if (storedPsRef == null) {
            log.error("stored PSRef is null");
        }
        else if (!psRef.getNationalId().equals(storedPsRef.getNationalId())) {
            log.error("Ps not toggled : Adeli is {}, toggled RRPS is {}, stored PsRef is {}", psRef.getNationalIdRef(), psRef.getNationalId(), storedPsRef.getNationalId());
        }
    }

    /**
     * Gets ps url.
     *
     * @return the ps url
     */
    public String getPsUrl() {
        return apiBaseUrl + "/ps";
    }

    /**
     * Gets ps url.
     *
     * @param id the id
     * @return the ps url
     */
    public String getPsUrl(String id) {
        return getPsUrl() + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    /**
     * Gets ex pro url.
     *
     * @param psUrl the ps url
     * @return the ex pro url
     */
    public String getExProUrl(String psUrl) {
        return psUrl + "/professions";
    }

    /**
     * Gets ex pro url.
     *
     * @param psUrl the ps url
     * @param id    the id
     * @return the ex pro url
     */
    public String getExProUrl(String psUrl, String id) {
        return getExProUrl(psUrl) + '/' + URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    /**
     * Gets expertise url.
     *
     * @param exProUrl the ex pro url
     * @return the expertise url
     */
    public String getExpertiseUrl(String exProUrl) {
        return exProUrl + "/expertises";
    }

    /**
     * Gets expertise url.
     *
     * @param exProUrl the ex pro url
     * @param id       the id
     * @return the expertise url
     */
    public String getExpertiseUrl(String exProUrl, String id) {
        return getExpertiseUrl(exProUrl) + '/' + URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    /**
     * Gets situation url.
     *
     * @param exProUrl the ex pro url
     * @return the situation url
     */
    public String getSituationUrl(String exProUrl) {
        return exProUrl + "/situations";
    }

    /**
     * Gets situation url.
     *
     * @param exProUrl the ex pro url
     * @param id       the id
     * @return the situation url
     */
    public String getSituationUrl(String exProUrl, String id) {
        return getSituationUrl(exProUrl) + '/' + URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    /**
     * Gets structure url.
     *
     * @return the structure url
     */
    public String getStructureUrl() {
        return apiBaseUrl + "/structures";
    }

    /**
     * Gets structure url.
     *
     * @param id the id
     * @return the structure url
     */
    public String getStructureUrl(String id) {
        return getStructureUrl() + '/' + URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    /**
     * Gets psRef url
     */
    public String getPsRefUrl() {
        return apiBaseUrl + "/psref";
    }

}
