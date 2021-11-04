package fr.ans.psc.pscload.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The type Pscload metrics.
 */
@Component
public class CustomMetrics {

    private static final String PS_METRIC_NAME = "ps.metric";
    private static final String STRUCTURE_METRIC_NAME = "structure.metric";
    private static final String PROGRESSION_METRIC_NAME = "progression.metric";
    private static final String ID_TYPE_TAG = "idType";
    private static final String OPERATION_TAG = "operation";
    private static final String ENTITY_TAG = "entity";
    private static final String REFERENCE_TAG = "reference";
    private static final String COUNT_TAG = "count";

    public static final String SER_FILE_TAG = "ser.file";
    public static final String TIMESTAMP_TAG = "timestamp";

    public enum ID_TYPE {
        ANY("any"),
        ADELI("0"),
        FINESS("3"),
        SIRET("5"),
        RPPS("8");

        public String value;

        ID_TYPE(String value) {
            this.value = value;
        }
    }

    public enum OPERATION {
        CREATE,
        UPDATE,
        DELETE,
        UPLOAD
    }

    public enum ENTITY_TYPE {
        PS,
        STRUCTURE
    }

    private final Map<PsCustomMetric, AtomicInteger> appPsSizeGauges = new EnumMap<>(PsCustomMetric.class);
    private final Map<StructureCustomMetric, AtomicInteger> appStructureSizeGauges = new EnumMap<>(StructureCustomMetric.class);
    private final Map<ProgressionCustomMetric, AtomicInteger> appProgressionGauges = new EnumMap<>(ProgressionCustomMetric.class);
    private final Map<MiscCustomMetric, AtomicInteger> appMiscGauges = new EnumMap<>(MiscCustomMetric.class);


    /**
     * The enums Custom metric.
     */
    public enum PsCustomMetric {
        PS_ANY_UPLOAD_SIZE,
        PS_ADELI_UPLOAD_SIZE,
        PS_FINESS_UPLOAD_SIZE,
        PS_SIRET_UPLOAD_SIZE,
        PS_RPPS_UPLOAD_SIZE,

        PS_ANY_DELETE_SIZE,
        PS_ADELI_DELETE_SIZE,
        PS_FINESS_DELETE_SIZE,
        PS_SIRET_DELETE_SIZE,
        PS_RPPS_DELETE_SIZE,

        PS_ANY_CREATE_SIZE,
        PS_ADELI_CREATE_SIZE,
        PS_FINESS_CREATE_SIZE,
        PS_SIRET_CREATE_SIZE,
        PS_RPPS_CREATE_SIZE,

        PS_ANY_UPDATE_SIZE,
        PS_ADELI_UPDATE_SIZE,
        PS_FINESS_UPDATE_SIZE,
        PS_SIRET_UPDATE_SIZE,
        PS_RPPS_UPDATE_SIZE,

        PS_ANY_REFERENCE_SIZE,
        PS_ADELI_REFERENCE_SIZE,
        PS_FINESS_REFERENCE_SIZE,
        PS_SIRET_REFERENCE_SIZE,
        PS_RPPS_REFERENCE_SIZE,

    }

    public enum StructureCustomMetric {
        STRUCTURE_UPLOAD_SIZE,
        STRUCTURE_DELETE_SIZE,
        STRUCTURE_CREATE_SIZE,
        STRUCTURE_UPDATE_SIZE,
    }

    public enum ProgressionCustomMetric {
        PS_UPLOAD_PROGRESSION,
        PS_DELETE_PROGRESSION,
        PS_CREATE_PROGRESSION,
        PS_UPDATE_PROGRESSION,

        STRUCTURE_UPLOAD_PROGRESSION,
        STRUCTURE_DELETE_PROGRESSION,
        STRUCTURE_CREATE_PROGRESSION,
        STRUCTURE_UPDATE_PROGRESSION,
    }

    public enum MiscCustomMetric {
        STAGE
    }

    /**
     * Instantiates a new Custom metrics.
     *
     * @param meterRegistry the meter registry
     */
    public CustomMetrics(MeterRegistry meterRegistry) {
        appMiscGauges.put(MiscCustomMetric.STAGE, meterRegistry.gauge("pscload.stage", new AtomicInteger(0)));

        // Initialization of metrics

        // PS size metrics :
        // initialize metric for each type and operation for a PS

        Arrays.stream(ID_TYPE.values()).forEach(id_type ->
                Arrays.stream(OPERATION.values()).forEach(operation -> {
                    String metricKey = String.join("_", ENTITY_TYPE.PS.name(), id_type.name(), operation.name(), "SIZE");
                    appPsSizeGauges.put(
                            PsCustomMetric.valueOf(metricKey),
                            meterRegistry.gauge(
                                    PS_METRIC_NAME,
                                    Tags.of(ID_TYPE_TAG, id_type.toString(), OPERATION_TAG, operation.name().toLowerCase()),
                                    // set to -1 to discriminate no changes on this operation & entity (-> 0) and not the correct stage (-> -1)
                                    // a -1 value means that we're not at a stage between diff & upload
                                    // a 0 value means that there are no changes (delete, update, etc) for this particular entity after diff
                                    new AtomicInteger(-1)
                            )
                    );
                })
        );

        // Structure size metrics :
        // initialize metric for each operation for a Structure

        Arrays.stream(OPERATION.values()).forEach(operation -> {
            String metricKey = String.join("_", ENTITY_TYPE.STRUCTURE.name(), operation.name(), "SIZE");
                appStructureSizeGauges.put(
                        StructureCustomMetric.valueOf(metricKey),
                        meterRegistry.gauge(
                                STRUCTURE_METRIC_NAME,
                                Tags.of(OPERATION_TAG, operation.name().toLowerCase()),
                                // set to -1 to discriminate no changes on this operation & entity (-> 0) and not the correct stage (-> -1)
                                // a -1 value means that we're not at a stage between diff & upload
                                // a 0 value means that there are no changes (delete, update, etc) for this particular entity after diff
                                new AtomicInteger(-1)
                        )
                );
        });

        // Progression metrics :
        // Initialize metric for each entity type and operation

        Arrays.stream(OPERATION.values()).forEach(operation ->
                Arrays.stream(ENTITY_TYPE.values()).forEach(entity_type -> {
                        String metricKey = String.join("_", entity_type.name(), operation.name(), "PROGRESSION");
                        appProgressionGauges.put(
                                ProgressionCustomMetric.valueOf(metricKey),
                                meterRegistry.gauge(
                                        PROGRESSION_METRIC_NAME,
                                        Tags.of(ENTITY_TAG, entity_type.name(), OPERATION_TAG, operation.name().toLowerCase()),
                                        new AtomicInteger(0)
                                )
                        );
                })
        );

        // Reference Ps metrics :
        // Initialize metrics for each id_type and counts of last upload changes as reference for computing changes %
        Arrays.stream(ID_TYPE.values()).forEach(id_type -> {
            String metricKey = String.join("_", ENTITY_TYPE.PS.name(), id_type.name(), REFERENCE_TAG, "SIZE");
            appPsSizeGauges.put(
                    PsCustomMetric.valueOf(metricKey),
                    meterRegistry.gauge(
                            PS_METRIC_NAME,
                            Tags.of(ID_TYPE_TAG, id_type.toString(), REFERENCE_TAG, COUNT_TAG),
                            new AtomicInteger(-1)
                    )
            );
        });

        Counter.builder(SER_FILE_TAG)
                .tags(TIMESTAMP_TAG, "")
                .register(meterRegistry);
    }

    /**
     * Gets app gauges.
     *
     * @return the app gauges
     */
    public Map<PsCustomMetric, AtomicInteger> getPsSizeGauges() {
        return appPsSizeGauges;
    }
    public Map<StructureCustomMetric, AtomicInteger> getAppStructureSizeGauges() { return appStructureSizeGauges; }
    public Map<ProgressionCustomMetric, AtomicInteger> getAppProgressionGauges() { return appProgressionGauges; }
    public Map<MiscCustomMetric, AtomicInteger> getAppMiscGauges() { return appMiscGauges; }
}
