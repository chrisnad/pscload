package fr.ans.psc.pscload.mapper;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import fr.ans.psc.pscload.metrics.CustomMetrics;
import fr.ans.psc.pscload.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class Serializer {

    /**
     * The logger.
     */
    private static final Logger log = LoggerFactory.getLogger(Serializer.class);

    private static final Kryo kryo = new Kryo();

    static {
        kryo.register(HashMap.class, 9);
        kryo.register(ArrayList.class, 10);
        kryo.register(Professionnel.class, 11);
        kryo.register(ExerciceProfessionnel.class, 12);
        kryo.register(SavoirFaire.class, 13);
        kryo.register(SituationExercice.class, 14);
        kryo.register(StructureRef.class, 15);
        kryo.register(Structure.class, 16);
    }

    private Map<String, Professionnel> psMap = new HashMap<>();

    private Map<String, Structure> structureMap = new HashMap<>();

    @Autowired
    private CustomMetrics customMetrics;

    public Map<String, Professionnel> getPsMap() {
        return psMap;
    }

    public Map<String, Structure> getStructureMap() {
        return structureMap;
    }

    public void serialiseMapsToFile(Map<String, Professionnel> psMap, Map<String, Structure> structureMap, String fileName) throws FileNotFoundException {
        log.info("serializing Ps map to {}", fileName);

        Output output = new Output(new FileOutputStream(fileName));
        kryo.writeClassAndObject(output, psMap);
        kryo.writeClassAndObject(output, structureMap);
        output.close();

        log.info("serialization complete!");
    }

    public void deserialiseFileToMaps(File file) throws FileNotFoundException {
        log.info("deserializing {} to Ps map", file.getName());

        Input input = new Input(new FileInputStream(file));
        psMap = (Map<String, Professionnel>) kryo.readClassAndObject(input);
        structureMap = (Map<String, Structure>) kryo.readClassAndObject(input);
        input.close();

        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_ADELI_UPLOAD_SIZE).set(
                Math.toIntExact(psMap.values().stream().filter(professionnel ->
                        CustomMetrics.ID_TYPE.ADELI.value.equals(professionnel.getIdType())).count())
        );
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_FINESS_UPLOAD_SIZE).set(
                Math.toIntExact(psMap.values().stream().filter(professionnel ->
                        CustomMetrics.ID_TYPE.FINESS.value.equals(professionnel.getIdType())).count())
        );
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_SIRET_UPLOAD_SIZE).set(
                Math.toIntExact(psMap.values().stream().filter(professionnel ->
                        CustomMetrics.ID_TYPE.SIRET.value.equals(professionnel.getIdType())).count())
        );
        customMetrics.getPsSizeGauges().get(CustomMetrics.PsCustomMetric.PS_RPPS_UPLOAD_SIZE).set(
                Math.toIntExact(psMap.values().stream().filter(professionnel ->
                        CustomMetrics.ID_TYPE.RPPS.value.equals(professionnel.getIdType())).count())
        );


        log.info("deserialization complete!");
    }

}
