package fr.ans.psc.pscload.service;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import fr.ans.psc.pscload.component.JsonFormatter;
import fr.ans.psc.pscload.metrics.CustomMetrics;
import fr.ans.psc.pscload.model.*;
import fr.ans.psc.pscload.service.task.Create;
import fr.ans.psc.pscload.service.task.Delete;
import fr.ans.psc.pscload.service.task.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The type Psc rest api.
 */
@Service
public class PscRestApi {

    @Autowired
    private CustomMetrics customMetrics;

    @Autowired
    private JsonFormatter jsonFormatter;

    @Value("${api.base.url}")
    private String apiBaseUrl;

    /**
     * Diff PS maps.
     *
     * @param original OG PS map
     * @param revised  the revised PS map
     * @return the map difference
     */
    public MapDifference<String, Professionnel> diffPsMaps(Map<String, Professionnel> original, Map<String, Professionnel> revised) {
        MapDifference<String, Professionnel> psDiff = Maps.difference(original, revised);

        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_DELETE_SIZE).set(psDiff.entriesOnlyOnLeft().size());
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_CREATE_SIZE).set(psDiff.entriesOnlyOnRight().size());
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_UPDATE_SIZE).set(psDiff.entriesDiffering().size());

        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_0_DELETE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnLeft().values().stream().filter(ps -> "0".equals(ps.getIdType())).count()));
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_0_CREATE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnRight().values().stream().filter(ps -> "0".equals(ps.getIdType())).count()));
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_0_UPDATE_SIZE).set(
                Math.toIntExact(psDiff.entriesDiffering().values().stream().filter(ps -> "0".equals(ps.leftValue().getIdType())).count()));

        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_3_DELETE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnLeft().values().stream().filter(ps -> "3".equals(ps.getIdType())).count()));
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_3_CREATE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnRight().values().stream().filter(ps -> "3".equals(ps.getIdType())).count()));
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_3_UPDATE_SIZE).set(
                Math.toIntExact(psDiff.entriesDiffering().values().stream().filter(ps -> "3".equals(ps.leftValue().getIdType())).count()));

        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_5_DELETE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnLeft().values().stream().filter(ps -> "5".equals(ps.getIdType())).count()));
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_5_CREATE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnRight().values().stream().filter(ps -> "5".equals(ps.getIdType())).count()));
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_5_UPDATE_SIZE).set(
                Math.toIntExact(psDiff.entriesDiffering().values().stream().filter(ps -> "5".equals(ps.leftValue().getIdType())).count()));

        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_8_DELETE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnLeft().values().stream().filter(ps -> "8".equals(ps.getIdType())).count()));
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_8_CREATE_SIZE).set(
                Math.toIntExact(psDiff.entriesOnlyOnRight().values().stream().filter(ps -> "8".equals(ps.getIdType())).count()));
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_8_UPDATE_SIZE).set(
                Math.toIntExact(psDiff.entriesDiffering().values().stream().filter(ps -> "8".equals(ps.leftValue().getIdType())).count()));

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

        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STRUCTURE_DELETE_SIZE).set(structureDiff.entriesOnlyOnLeft().size());
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STRUCTURE_CREATE_SIZE).set(structureDiff.entriesOnlyOnRight().size());
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STRUCTURE_UPDATE_SIZE).set(structureDiff.entriesDiffering().size());

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
        injectPsDiffTasks(psDiff);
        injectStructuresDiffTasks(structureDiff);
    }

    private void injectPsDiffTasks(MapDifference<String, Professionnel> diff) {
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_DELETE_PROGRESSION).set(0);
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_CREATE_PROGRESSION).set(0);
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_UPDATE_PROGRESSION).set(0);

        diff.entriesOnlyOnLeft().values().parallelStream().forEach(ps -> {
            new Delete(getPsUrl(ps.getNationalId())).send();
            customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_DELETE_PROGRESSION).incrementAndGet();
        });
        diff.entriesOnlyOnRight().values().parallelStream().forEach(ps -> {
            new Create(getPsUrl(), jsonFormatter.jsonFromObject(ps)).send();
            customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_CREATE_PROGRESSION).incrementAndGet();
        });
        diff.entriesDiffering().values().parallelStream().forEach(v -> {
            injectPsUpdateTasks(v.leftValue(), v.rightValue());
            customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.PS_UPDATE_PROGRESSION).incrementAndGet();
        });
    }

    private void injectStructuresDiffTasks(MapDifference<String, Structure> diff) {
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STRUCTURE_DELETE_PROGRESSION).set(0);
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STRUCTURE_CREATE_PROGRESSION).set(0);
        customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STRUCTURE_UPDATE_PROGRESSION).set(0);

        diff.entriesOnlyOnLeft().values().parallelStream().forEach(structure -> {
            new Delete(getStructureUrl(structure.getStructureId())).send();
            customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STRUCTURE_DELETE_PROGRESSION).incrementAndGet();
        });
        diff.entriesOnlyOnRight().values().parallelStream().forEach(structure -> {
            new Create(getStructureUrl(), jsonFormatter.jsonFromObject(structure)).send();
            customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STRUCTURE_CREATE_PROGRESSION).incrementAndGet();
        });
        diff.entriesDiffering().values().parallelStream().forEach(v -> {
            new Update(getStructureUrl(v.leftValue().getStructureId()), jsonFormatter.jsonFromObject(v.rightValue())).send();
            customMetrics.getAppGauges().get(CustomMetrics.CustomMetric.STRUCTURE_UPDATE_PROGRESSION).incrementAndGet();
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
        return  exProUrl + "/expertises";
    }

    /**
     * Gets expertise url.
     *
     * @param exProUrl the ex pro url
     * @param id       the id
     * @return the expertise url
     */
    public String getExpertiseUrl(String exProUrl, String id) {
        return  getExpertiseUrl(exProUrl) + '/' + URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    /**
     * Gets situation url.
     *
     * @param exProUrl the ex pro url
     * @return the situation url
     */
    public String getSituationUrl(String exProUrl) {
        return  exProUrl + "/situations";
    }

    /**
     * Gets situation url.
     *
     * @param exProUrl the ex pro url
     * @param id       the id
     * @return the situation url
     */
    public String getSituationUrl(String exProUrl, String id) {
        return  getSituationUrl(exProUrl) + '/' + URLEncoder.encode(id, StandardCharsets.UTF_8);
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

}
