package fr.ans.psc.pscload;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.ObjectRowProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import fr.ans.psc.pscload.component.JsonFormatter;
import fr.ans.psc.pscload.model.object.*;
import fr.ans.psc.pscload.model.object.response.PsListResponse;
import fr.ans.psc.pscload.service.PscRestApi;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SpringBootTest
class PscloadApplicationTests {

	@Test
	@Disabled
	void postMessageTest() {
		String url = "http://localhost:8000/api/ps";
		//String message = "0|496112012|0496112012|CHOLOT|Florence'Renée'Georgette|20/05/1964|||||||MME|60|C||MESFIOUI RAJA|Florence|||S|||||||||||||||||||||||||||||";
		String message = "3|190000042/021721|3190000042/021721|PROS|JEAN LOUIS''||||||0555926000||M||||PROS|JEAN LOUIS|||S||||||||F190000042||||||||||||||0555926000||0555926080|||||";
		PscRestApi pscRestApi = new PscRestApi();
		JsonFormatter jsonFormatter = new JsonFormatter();

		String jsonPs = jsonFormatter.nakedPsFromMessage(message);
		pscRestApi.put(url, jsonPs);
	}

	@Test
	@Disabled
	void restServiceTest() {
		String url = "http://localhost:8000/api/ps";
		PscRestApi pscRestApi = new PscRestApi();
		PsListResponse psListResponse = pscRestApi.getPsList(url);
		System.out.println("fae");
	}

	@Test
	@Disabled
	void deserializeAndDiff() {
		String url = "http://localhost:8000/api/ps";
		long startTime = System.currentTimeMillis();

		Map<String, Professionnel> original = getPsHashMap("Extraction_PSC.txt");
		System.out.println(System.currentTimeMillis()-startTime);

/*		Map<String, Professionnel> revised = getPsHashMap("Extraction_PSC_20002.txt");
		System.out.println(System.currentTimeMillis()-startTime);

		MapDifference<String, Professionnel> diff = Maps.difference(original, revised);

		PscRestApi pscRestApi = new PscRestApi();
		JsonFormatter jsonFormatter = new JsonFormatter();

//		diff.entriesOnlyOnLeft().forEach((k, v) -> pscRestApi.delete(url + '/' + v.getNationalId()));
//		diff.entriesOnlyOnRight().forEach((k, v) -> pscRestApi.put(url, jsonFormatter.psFromObject(v)));
//		diff.entriesDiffering().forEach((k, v) -> pscRestApi.diffUpdate(v.leftValue(), v.rightValue()));

		System.out.println(System.currentTimeMillis()-startTime);*/
	}

	@Test
	@Disabled
	void firstParseAndSerializeAndDeserialize() throws FileNotFoundException {
		long startTime = System.currentTimeMillis();

		//build simple lists of the lines of the two testfiles
		Map<String, Professionnel> original = getPsHashMap("Extraction_PSC_100000.txt");
		System.out.println(System.currentTimeMillis()-startTime);
		serialisePsHashMapToFile(original, "Extraction_PSC_100000.ser");
		System.out.println(System.currentTimeMillis()-startTime);

		original.clear();
		Map<String, Professionnel> des = deserialiseFileToPsHashMap("Extraction_PSC_100000.ser");
		System.out.println(System.currentTimeMillis()-startTime);
	}

	private Map<String, Professionnel> getPsHashMap(String fileName) {
		Map<String, Professionnel> psMap = new HashMap<>();
		// ObjectRowProcessor converts the parsed values and gives you the resulting row.
		ObjectRowProcessor rowProcessor = new ObjectRowProcessor() {
			@Override
			public void rowProcessed(Object[] objects, ParsingContext parsingContext) {
				Professionnel newPs = new Professionnel(Arrays.asList(objects).toArray(new String[objects.length]));
				Professionnel mappedPs = psMap.get(newPs.getNationalId());
				if (mappedPs != null) {
					Optional<Map.Entry<String, ExerciceProfessionnel>> exPro = newPs.getProfessions().entrySet().stream().findAny();
					if (exPro.isPresent()) {
						ExerciceProfessionnel mappedExPro = mappedPs.getProfessions().get(exPro.get().getKey());
						if (mappedExPro != null) {
							Optional<Map.Entry<String, SavoirFaire>> expertise = exPro.get().getValue().getExpertises().entrySet().stream().findAny();
							if (expertise.isPresent()) {
								if (!mappedExPro.getExpertises().containsKey(expertise.get().getKey())) {
									mappedExPro.getExpertises().put(expertise.get().getKey(), expertise.get().getValue());
								}
							} // else do nothing
							Optional<Map.Entry<String, SituationExercice>> situation = exPro.get().getValue().getWorkSituations().entrySet().stream().findAny();
							if (situation.isPresent()) {
								if (!mappedExPro.getWorkSituations().containsKey(situation.get().getKey())) {
									mappedExPro.getWorkSituations().put(situation.get().getKey(), situation.get().getValue());
								}
							} // else do nothing
						} else {
							mappedPs.getProfessions().put(exPro.get().getKey(), exPro.get().getValue());
						}
					} // else do nothing
				} else {
					psMap.put(newPs.getNationalId(), newPs);
				}
			}
		};

		CsvParserSettings parserSettings = new CsvParserSettings();
		parserSettings.getFormat().setLineSeparator("\n");
		parserSettings.getFormat().setDelimiter('|');
		parserSettings.setProcessor(rowProcessor);
		parserSettings.setHeaderExtractionEnabled(true);

		CsvParser parser = new CsvParser(parserSettings);
		parser.parse(new InputStreamReader(Objects.requireNonNull(this.getClass().getClassLoader()
				.getResourceAsStream(fileName)), StandardCharsets.UTF_8));

		return psMap;
	}

	private void serialisePsHashMapToFile(Object psList, String fileName) throws FileNotFoundException {
		Kryo kryo = new Kryo();
		kryo.register(HashMap.class, 9);
		kryo.register(ArrayList.class, 10);
		kryo.register(Professionnel.class, 11);
		kryo.register(ExerciceProfessionnel.class, 12);
		kryo.register(SavoirFaire.class, 13);
		kryo.register(SituationExercice.class, 14);
		kryo.register(Structure.class, 15);

		Output output = new Output(new FileOutputStream(fileName));
		kryo.writeObjectOrNull(output, psList, HashMap.class);
		output.close();
	}

	private Map<String, Professionnel> deserialiseFileToPsHashMap(String fileName) throws FileNotFoundException {
		Kryo kryo = new Kryo();
		kryo.register(HashMap.class, 9);
		kryo.register(ArrayList.class, 10);
		kryo.register(Professionnel.class, 11);
		kryo.register(ExerciceProfessionnel.class, 12);
		kryo.register(SavoirFaire.class, 13);
		kryo.register(SituationExercice.class, 14);
		kryo.register(Structure.class, 15);

		Input input = new Input(new FileInputStream(fileName));
		Map<String, Professionnel> psList = kryo.readObjectOrNull(input, HashMap.class);
		input.close();

		return psList;
	}

}
