package fr.ans.psc.pscload.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The type Pscload metrics.
 */
@Component
public class CustomMetrics {
    
    private String PS_METRIC_NAME = "ps.metric";
    private String STRUCTURE_METRIC_NAME = "structure.metric";
    private String ID_TYPE_TAG = "idType";
    private String OPERATION_TAG = "operation";
    
    private enum ID_TYPE {
        ANY("any"),
        ADELI("0"),
        FINESS("3"),
        SIRET("5"),
        RPPS("8");
        
        
        private String value;
        
        private ID_TYPE(String value) {
            this.value = value;
        }
    }
    
    
    private enum OPERATION {
        create,
        update,
        delete,
        upload
    }

    private final Map<CustomMetric, AtomicInteger> appGauges = new EnumMap<>(CustomMetric.class);

    public static final String SER_FILE = "ser.file";

    public static final String TIMESTAMP = "timestamp";

    /**
     * The enum Custom metric.
     */
    public enum CustomMetric {
        STAGE,
        PS_UPLOAD_SIZE,
        PS_0_UPLOAD_SIZE,
        PS_3_UPLOAD_SIZE,
        PS_5_UPLOAD_SIZE,
        PS_8_UPLOAD_SIZE,
        PS_UPLOAD_PROGRESSION,
        STRUCTURE_UPLOAD_SIZE,
        STRUCTURE_UPLOAD_PROGRESSION,
        PS_DELETE_SIZE,
        PS_0_DELETE_SIZE,
        PS_3_DELETE_SIZE,
        PS_5_DELETE_SIZE,
        PS_8_DELETE_SIZE,
        PS_DELETE_PROGRESSION,
        PS_CREATE_SIZE,
        PS_0_CREATE_SIZE,
        PS_3_CREATE_SIZE,
        PS_5_CREATE_SIZE,
        PS_8_CREATE_SIZE,
        PS_CREATE_PROGRESSION,
        PS_UPDATE_SIZE,
        PS_0_UPDATE_SIZE,
        PS_3_UPDATE_SIZE,
        PS_5_UPDATE_SIZE,
        PS_8_UPDATE_SIZE,
        PS_UPDATE_PROGRESSION,
        STRUCTURE_DELETE_SIZE,
        STRUCTURE_DELETE_PROGRESSION,
        STRUCTURE_CREATE_SIZE,
        STRUCTURE_CREATE_PROGRESSION,
        STRUCTURE_UPDATE_SIZE,
        STRUCTURE_UPDATE_PROGRESSION,
    }

    /**
     * Instantiates a new Custom metrics.
     *
     * @param meterRegistry the meter registry
     */
    public CustomMetrics(MeterRegistry meterRegistry) {
        appGauges.put(CustomMetric.STAGE, meterRegistry.gauge("pscload.stage", new AtomicInteger(0)));

        appGauges.put(CustomMetric.PS_UPLOAD_SIZE, meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.ANY.value, OPERATION_TAG, OPERATION.upload.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_0_UPLOAD_SIZE, meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.ADELI.value, OPERATION_TAG, OPERATION.upload.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_3_UPLOAD_SIZE, meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.FINESS.value, OPERATION_TAG, OPERATION.upload.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_5_UPLOAD_SIZE, meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.SIRET.value, OPERATION_TAG, OPERATION.upload.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_8_UPLOAD_SIZE, meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.RPPS.value, OPERATION_TAG, OPERATION.upload.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_UPLOAD_PROGRESSION, meterRegistry.gauge(PS_METRIC_NAME, Tags.of("progression", "true", OPERATION_TAG, OPERATION.upload.name()), new AtomicInteger(0)));

        appGauges.put(CustomMetric.STRUCTURE_UPLOAD_SIZE, meterRegistry.gauge(STRUCTURE_METRIC_NAME, Tags.of(OPERATION_TAG, OPERATION.upload.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_UPLOAD_PROGRESSION,meterRegistry.gauge(STRUCTURE_METRIC_NAME, Tags.of(OPERATION_TAG, OPERATION.upload.name()), new AtomicInteger(0)));

        appGauges.put(CustomMetric.PS_DELETE_SIZE ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.ANY.value, OPERATION_TAG, OPERATION.delete.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_0_DELETE_SIZE ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.ADELI.value, OPERATION_TAG, OPERATION.delete.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_3_DELETE_SIZE ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.FINESS.value, OPERATION_TAG, OPERATION.delete.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_5_DELETE_SIZE ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.SIRET.value, OPERATION_TAG, OPERATION.delete.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_8_DELETE_SIZE ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.RPPS.value, OPERATION_TAG, OPERATION.delete.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_DELETE_PROGRESSION ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of("progression", "true", OPERATION_TAG, OPERATION.delete.name()), new AtomicInteger(0)));

        appGauges.put(CustomMetric.PS_CREATE_SIZE ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.ANY.value, OPERATION_TAG, OPERATION.create.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_0_CREATE_SIZE ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.ADELI.value, OPERATION_TAG, OPERATION.create.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_3_CREATE_SIZE ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.FINESS.value, OPERATION_TAG, OPERATION.create.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_5_CREATE_SIZE ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.SIRET.value, OPERATION_TAG, OPERATION.create.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_8_CREATE_SIZE ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.RPPS.value, OPERATION_TAG, OPERATION.create.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_CREATE_PROGRESSION ,meterRegistry.gauge(PS_METRIC_NAME, Tags.of("progression", "true", OPERATION_TAG, OPERATION.create.name()), new AtomicInteger(0)));

        appGauges.put(CustomMetric.PS_UPDATE_SIZE,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.ANY.value, OPERATION_TAG, OPERATION.update.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_0_UPDATE_SIZE,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.ADELI.value, OPERATION_TAG, OPERATION.update.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_3_UPDATE_SIZE,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.FINESS.value, OPERATION_TAG, OPERATION.update.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_5_UPDATE_SIZE,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.SIRET.value, OPERATION_TAG, OPERATION.update.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_8_UPDATE_SIZE,meterRegistry.gauge(PS_METRIC_NAME, Tags.of(ID_TYPE_TAG, ID_TYPE.RPPS.value, OPERATION_TAG, OPERATION.update.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_UPDATE_PROGRESSION,meterRegistry.gauge(PS_METRIC_NAME, Tags.of("progression", "true", OPERATION_TAG, OPERATION.update.name()), new AtomicInteger(0)));

        appGauges.put(CustomMetric.STRUCTURE_DELETE_SIZE ,meterRegistry.gauge(STRUCTURE_METRIC_NAME, Tags.of(OPERATION_TAG, OPERATION.delete.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_DELETE_PROGRESSION ,meterRegistry.gauge(STRUCTURE_METRIC_NAME, Tags.of("progression", "true", OPERATION_TAG, OPERATION.delete.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_CREATE_SIZE ,meterRegistry.gauge(STRUCTURE_METRIC_NAME, Tags.of(OPERATION_TAG, OPERATION.create.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_CREATE_PROGRESSION ,meterRegistry.gauge(STRUCTURE_METRIC_NAME, Tags.of("progression", "true", OPERATION_TAG, OPERATION.create.name()),new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_UPDATE_SIZE,meterRegistry.gauge(STRUCTURE_METRIC_NAME, Tags.of(OPERATION_TAG, OPERATION.update.name()), new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_UPDATE_PROGRESSION,meterRegistry.gauge(STRUCTURE_METRIC_NAME, Tags.of("progression", "true", OPERATION_TAG, OPERATION.update.name()), new AtomicInteger(0)));

        Counter.builder(SER_FILE)
                .tags(TIMESTAMP, "")
                .register(meterRegistry);
    }

    /**
     * Gets app gauges.
     *
     * @return the app gauges
     */
    public Map<CustomMetric, AtomicInteger> getAppGauges() {
        return appGauges;
    }

}
