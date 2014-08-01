package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.cam.cl.dtg.segue.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;

/**
 * Choice deserializer
 * 
 * This class requires the primary content bas deserializer as a constructor
 * arguement.
 * 
 * It is to allow subclasses of the choices object to be detected correctly.
 */
public class QuestionValidationResponseDeserializer extends JsonDeserializer<QuestionValidationResponse> {
	private ContentBaseDeserializer contentDeserializer;
	private ChoiceDeserializer choiceDeserializer;
	
	public QuestionValidationResponseDeserializer(ContentBaseDeserializer contentDeserializer, ChoiceDeserializer choiceDeserializer) {
		this.contentDeserializer = contentDeserializer;
		this.choiceDeserializer = choiceDeserializer;
	}

	@Override
	public QuestionValidationResponse deserialize(JsonParser jsonParser,
			DeserializationContext deserializationContext) throws IOException,
			JsonProcessingException, JsonMappingException {

		SimpleModule contentDeserializerModule = new SimpleModule(
				"ContentDeserializerModule");
		contentDeserializerModule.addDeserializer(ContentBase.class,
				contentDeserializer);
		contentDeserializerModule.addDeserializer(Choice.class, choiceDeserializer);

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(contentDeserializerModule);

		ObjectNode root = (ObjectNode) mapper.readTree(jsonParser);

		if (null == root.get("answer")) {
			throw new JsonMappingException(
					"Error: unable to parse content as there is no answer property within the json input.");			
		}

		String QuestionResponseType = root.get("answer").get("type").textValue();

		if (QuestionResponseType.equals("quantity")) {
			return mapper.readValue(root.toString(), QuantityValidationResponse.class);
		} else {
			return mapper.readValue(root.toString(), QuestionValidationResponse.class);
		}
	}
}