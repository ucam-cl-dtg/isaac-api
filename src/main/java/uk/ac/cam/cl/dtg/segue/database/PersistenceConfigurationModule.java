package uk.ac.cam.cl.dtg.segue.database;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.IRegistrationManager;
import uk.ac.cam.cl.dtg.segue.dao.LogManager;
import uk.ac.cam.cl.dtg.segue.dao.RegistrationManager;
import uk.ac.cam.cl.dtg.segue.dto.Choice;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.Question;
import uk.ac.cam.cl.dtg.segue.dto.ChoiceQuestion;

import com.google.inject.AbstractModule;
import com.mongodb.DB;

/**
 * This class is responsible for injecting configuration values for persistence related classes
 */
public class PersistenceConfigurationModule extends AbstractModule {

	private ContentMapper mapper = new ContentMapper(buildDefaultJsonTypeMap());
	private static final Logger log = LoggerFactory.getLogger(PersistenceConfigurationModule.class);
	private static final String gitDbUri = "c:\\rutherford-test\\.git";
	
	@Override
	protected void configure() {
		// Setup different persistence bindings

		try {
			// MongoDB
			bind(DB.class).toInstance(Mongo.getDB());

			// GitDb			
			bind(GitDb.class).toInstance(new GitDb(gitDbUri));
			
			//bind(IContentManager.class).to(MongoContentManager.class); //Allows Mongo take over Content Management
			bind(IContentManager.class).to(GitContentManager.class); //Allows GitDb take over Content Management
		} catch (IOException e) {
			e.printStackTrace();
			log.error("Error instantiating the Git database for the given path: " + gitDbUri);
		}
		
		bind(ILogManager.class).to(LogManager.class);
		bind(IRegistrationManager.class).to(RegistrationManager.class);
		bind(ContentMapper.class).toInstance(mapper);
	}
	
	/**
	 * This method will return you a populated Map which enables mapping to and from content objects.
	 * 
	 * It requires that the class definition has the JsonType("XYZ") annotation
	 * 
	 * @return 
	 */
	private Map<String, Class<? extends Content>> buildDefaultJsonTypeMap() {
		HashMap<String, Class<? extends Content>> map = new HashMap<String, Class<? extends Content>>();

		// We need to pre-register different content objects here for the automapping to work
		map.put("choice", Choice.class);
		map.put("question", Question.class);
		map.put("choiceQuestion", ChoiceQuestion.class);
		return map;
	}
}
