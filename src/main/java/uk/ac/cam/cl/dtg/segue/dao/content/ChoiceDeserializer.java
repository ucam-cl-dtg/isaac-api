/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao.content;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.ac.cam.cl.dtg.segue.dos.content.ChemicalFormula;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.Formula;
import uk.ac.cam.cl.dtg.segue.dos.content.FreeTextRule;
import uk.ac.cam.cl.dtg.segue.dos.content.GraphChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.Item;
import uk.ac.cam.cl.dtg.segue.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.LogicFormula;
import uk.ac.cam.cl.dtg.segue.dos.content.ParsonsChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dos.content.StringChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.RegexPattern;

import java.io.IOException;

/**
 * Choice deserializer
 * 
 * This class requires the primary content base deserializer as a constructor
 * argument.
 * 
 * It is to allow subclasses of the choices object to be detected correctly.
 */
public class ChoiceDeserializer extends JsonDeserializer<Choice> {
    private ContentBaseDeserializer contentDeserializer;
    private ItemDeserializer itemDeserializer;
    
    private static ObjectMapper choiceMapper;
    
    /**
     * Creates a Choice deserializer that is used by jackson to handle polymorphic types.
     * 
     * @param contentDeserializer
     *            - Instance of a contentBase deserializer needed to deserialize nested content.
     * @param itemDeserializer
     *            - Required to cope with deserialising Items inside Choices when deserialising Choices directly.
     */
    public ChoiceDeserializer(final ContentBaseDeserializer contentDeserializer, final ItemDeserializer itemDeserializer) {
        this.contentDeserializer = contentDeserializer;
        this.itemDeserializer = itemDeserializer;
    }

    @Override
    public Choice deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
            throws IOException {

        ObjectNode root = getSingletonChoiceMapper().readTree(jsonParser);

        if (null == root.get("type")) {
            throw new JsonMappingException(
                    "Error: unable to parse content as there is no type property within the json input.");
        }

        String contentType = root.get("type").textValue();

        switch (contentType) {
            case "quantity":
                return getSingletonChoiceMapper().readValue(root.toString(), Quantity.class);
            case "formula":
                return getSingletonChoiceMapper().readValue(root.toString(), Formula.class);
            case "chemicalFormula":
                return getSingletonChoiceMapper().readValue(root.toString(), ChemicalFormula.class);
            case "logicFormula":
                return getSingletonChoiceMapper().readValue(root.toString(), LogicFormula.class);
            case "graphChoice":
                return getSingletonChoiceMapper().readValue(root.toString(), GraphChoice.class);
            case "stringChoice":
                return getSingletonChoiceMapper().readValue(root.toString(), StringChoice.class);
            case "regexPattern":
                return getSingletonChoiceMapper().readValue(root.toString(), RegexPattern.class);
            case "freeTextRule":
                return getSingletonChoiceMapper().readValue(root.toString(), FreeTextRule.class);
            case "parsonsChoice":
                return getSingletonChoiceMapper().readValue(root.toString(), ParsonsChoice.class);
            case "itemChoice":
                return getSingletonChoiceMapper().readValue(root.toString(), ItemChoice.class);
            default:
                return getSingletonChoiceMapper().readValue(root.toString(), Choice.class);
        }
    }
    
    /**
     * This is to reduce overhead in creating object mappers.
     * @return a preconfigured object mapper.
     */
    private ObjectMapper getSingletonChoiceMapper() {
        if (null == choiceMapper) {
            SimpleModule contentDeserializerModule = new SimpleModule("ContentDeserializerModule");
            contentDeserializerModule.addDeserializer(ContentBase.class, contentDeserializer);
            contentDeserializerModule.addDeserializer(Item.class, itemDeserializer);
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(contentDeserializerModule);
            choiceMapper = mapper;
        }
        
        return choiceMapper;
    }
}