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

        appGauges.put(CustomMetric.PS_UPLOAD_SIZE, meterRegistry.gauge("ps.metric", Tags.of("group", "total", "operation", "upload"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_0_UPLOAD_SIZE, meterRegistry.gauge("ps.metric", Tags.of("group", "0", "operation", "upload"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_3_UPLOAD_SIZE, meterRegistry.gauge("ps.metric", Tags.of("group", "3", "operation", "upload"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_5_UPLOAD_SIZE, meterRegistry.gauge("ps.metric", Tags.of("group", "5", "operation", "upload"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_8_UPLOAD_SIZE, meterRegistry.gauge("ps.metric", Tags.of("group", "8", "operation", "upload"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_UPLOAD_PROGRESSION, meterRegistry.gauge("ps.metric", Tags.of("progression", "true", "operation", "upload"), new AtomicInteger(0)));

        appGauges.put(CustomMetric.STRUCTURE_UPLOAD_SIZE, meterRegistry.gauge("structure.metric", Tags.of("operation", "upload"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_UPLOAD_PROGRESSION,meterRegistry.gauge("structure.metric", Tags.of("operation", "upload"), new AtomicInteger(0)));

        appGauges.put(CustomMetric.PS_DELETE_SIZE ,meterRegistry.gauge("ps.metric", Tags.of("group", "total", "operation", "delete"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_0_DELETE_SIZE ,meterRegistry.gauge("ps.metric", Tags.of("group", "0", "operation", "delete"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_3_DELETE_SIZE ,meterRegistry.gauge("ps.metric", Tags.of("group", "3", "operation", "delete"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_5_DELETE_SIZE ,meterRegistry.gauge("ps.metric", Tags.of("group", "5", "operation", "delete"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_8_DELETE_SIZE ,meterRegistry.gauge("ps.metric", Tags.of("group", "8", "operation", "delete"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_DELETE_PROGRESSION ,meterRegistry.gauge("ps.metric", Tags.of("progression", "true", "operation", "delete"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_CREATE_SIZE ,meterRegistry.gauge("ps.metric", Tags.of("group", "total", "operation", "create"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_0_CREATE_SIZE ,meterRegistry.gauge("ps.metric", Tags.of("group", "0", "operation", "create"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_3_CREATE_SIZE ,meterRegistry.gauge("ps.metric", Tags.of("group", "3", "operation", "create"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_5_CREATE_SIZE ,meterRegistry.gauge("ps.metric", Tags.of("group", "5", "operation", "create"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_8_CREATE_SIZE ,meterRegistry.gauge("ps.metric", Tags.of("group", "8", "operation", "create"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_CREATE_PROGRESSION ,meterRegistry.gauge("ps.metric", Tags.of("progression", "true", "operation", "create"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_UPDATE_SIZE,meterRegistry.gauge("ps.metric", Tags.of("group", "total", "operation", "update"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_0_UPDATE_SIZE,meterRegistry.gauge("ps.metric", Tags.of("group", "0", "operation", "update"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_3_UPDATE_SIZE,meterRegistry.gauge("ps.metric", Tags.of("group", "3", "operation", "update"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_5_UPDATE_SIZE,meterRegistry.gauge("ps.metric", Tags.of("group", "5", "operation", "update"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_8_UPDATE_SIZE,meterRegistry.gauge("ps.metric", Tags.of("group", "8", "operation", "update"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.PS_UPDATE_PROGRESSION,meterRegistry.gauge("ps.metric", Tags.of("progression", "true", "operation", "update"), new AtomicInteger(0)));

        appGauges.put(CustomMetric.STRUCTURE_DELETE_SIZE ,meterRegistry.gauge("structure.metric", Tags.of("operation", "delete"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_DELETE_PROGRESSION ,meterRegistry.gauge("structure.metric", Tags.of("progression", "true", "operation", "delete"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_CREATE_SIZE ,meterRegistry.gauge("structure.metric", Tags.of("operation", "create"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_CREATE_PROGRESSION ,meterRegistry.gauge("structure.metric", Tags.of("progression", "true", "operation", "create"),new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_UPDATE_SIZE,meterRegistry.gauge("structure.metric", Tags.of("operation", "update"), new AtomicInteger(0)));
        appGauges.put(CustomMetric.STRUCTURE_UPDATE_PROGRESSION,meterRegistry.gauge("structure.metric", Tags.of("progression", "true", "operation", "update"), new AtomicInteger(0)));

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
