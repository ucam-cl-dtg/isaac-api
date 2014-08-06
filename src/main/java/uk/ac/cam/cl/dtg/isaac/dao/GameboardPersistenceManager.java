package uk.ac.cam.cl.dtg.isaac.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ma.glasnost.orika.MapperFacade;

import org.apache.commons.lang3.Validate;
import org.elasticsearch.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.api.IsaacController;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.dao.IAppDataManager;
import uk.ac.cam.cl.dtg.segue.dos.users.User;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import static java.util.concurrent.TimeUnit.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static com.google.common.collect.Maps.*;

/**
 * This class is responsible for managing and persisting user data.
 */
public class GameboardPersistenceManager {
	private static final Logger log = LoggerFactory
			.getLogger(GameboardPersistenceManager.class);

	private static final Integer CACHE_TARGET_SIZE = 142;
	private static final Long GAMEBOARD_TTL_HOURS = MILLISECONDS.convert(30,
			MINUTES);

	private static final String USER_ID_FKEY = "ownerUserId";

	private final IAppDataManager<GameboardDO> gameboardDataManager;

	private final MapperFacade mapper;
	private final SegueApiFacade api;

	private final Map<String, GameboardDO> gameboardNonPersistentStorage;

	/**
	 * Creates a new user data manager object.
	 * 
	 * @param database
	 *            - the database reference used for persistence.
	 * @param api
	 *            - handle to segue api so that we can perform queries to
	 *            augment gameboard data before and after persistence.
	 * @param mapper
	 *            - An instance of an automapper that can be used for mapping to
	 *            and from GameboardDOs and DTOs.
	 */
	@Inject
	public GameboardPersistenceManager(
			final IAppDataManager<GameboardDO> database,
			final SegueApiFacade api, final MapperFacade mapper) {
		this.gameboardDataManager = database;
		this.mapper = mapper;
		this.api = api;
		this.gameboardNonPersistentStorage = Maps.newConcurrentMap();
	}

	/**
	 * Save a gameboard.
	 * 
	 * @param gameboard
	 *            - gameboard to save
	 * @return internal database id for the saved gameboard.
	 */
	public final String saveGameboardToPermanentStorage(
			final GameboardDTO gameboard) {
		GameboardDO gameboardToSave = mapper.map(gameboard, GameboardDO.class);
		// the mapping operation won't work for the list so we should just
		// create a new one.
		gameboardToSave.setQuestions(new ArrayList<String>());

		// Map each question into an IsaacQuestionInfo object
		for (GameboardItem c : gameboard.getQuestions()) {
			gameboardToSave.getQuestions().add(c.getId());
		}

		String resultId = gameboardDataManager.save(gameboardToSave);

		log.info("Saving gameboard... Gameboard ID: " + gameboard.getId()
				+ " DB id : " + resultId);

		// make sure that it is not still in temporary storage
		this.gameboardNonPersistentStorage.remove(gameboard.getId());

		return resultId;
	}

	/**
	 * Keep generated gameboard in non-persistent storage.
	 * 
	 * This will be removed if the gameboard is saved to persistent storage.
	 * 
	 * @param gameboard
	 *            to temporarily store.
	 * @return gameboard id
	 */
	public final String temporarilyStoreGameboard(final GameboardDTO gameboard) {
		this.gameboardNonPersistentStorage.put(gameboard.getId(),
				this.convertToGameboardDO(gameboard));

		tidyTemporaryGameboardStorage();

		return gameboard.getId();
	}

	/**
	 * Find a gameboard by id.
	 * 
	 * @param gameboardId
	 *            - the id to search for.
	 * @return the gameboard or null if we can't find it..
	 */
	public final GameboardDTO getGameboardById(final String gameboardId) {
		// first try temporary storage
		if (this.gameboardNonPersistentStorage.containsKey(gameboardId)) {
			return this
					.convertToGameboardDTO(this.gameboardNonPersistentStorage
							.get(gameboardId));
		}

		GameboardDO gameboardFromDb = gameboardDataManager.getById(gameboardId);

		if (null == gameboardFromDb) {
			return null;
		}

		GameboardDTO gameboardDTO = this.convertToGameboardDTO(gameboardFromDb);

		return gameboardDTO;
	}

	/**
	 * Retrieve all gameboards (without underlying Gameboard Items) for a given user.
	 * 
	 * @param user
	 *            - to search for
	 * @return gameboards as a list - note these gameboards will not have the
	 *         questions fully populated as it is expected only summary objects
	 *         are required.
	 */
	public final List<GameboardDTO> getGameboardsByUserId(final User user) {
		// TODO: we probably want to augment the DTOs with completeness information if possible.
		Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatch = Maps
				.newHashMap();

		fieldsToMatch.put(immutableEntry(
				Constants.BooleanOperator.AND, USER_ID_FKEY), Arrays
				.asList(user.getDbId()));

		List<GameboardDO> gameboardsFromDb = this.gameboardDataManager
				.find(fieldsToMatch);

		List<GameboardDTO> gameboardDTOs = this
				.convertToGameboardDTOs(gameboardsFromDb, false);

		return gameboardDTOs;
	}

	/**
	 * Update a gameboard. 
	 * The gameboard must already exist and have the same id.
	 * @param gameboard - to update. Note the ID should already exist.
	 * @return gameboardDTO
	 */
	public GameboardDTO updateGameboard(final GameboardDTO gameboard) {
		// TODO: write this.
		return null;
	}

	/**
	 * Helper method to tidy temporary gameboard cache.
	 */
	private void tidyTemporaryGameboardStorage() {
		if (this.gameboardNonPersistentStorage.size() >= CACHE_TARGET_SIZE) {
			log.debug("Running gameboard temporary cache eviction as it is of size  "
					+ this.gameboardNonPersistentStorage.size());

			for (GameboardDO board : this.gameboardNonPersistentStorage
					.values()) {
				long duration = new Date().getTime()
						- board.getCreationDate().getTime();

				if (duration >= GAMEBOARD_TTL_HOURS) {
					this.gameboardNonPersistentStorage.remove(board.getId());
					log.debug("Deleting temporary board from cache "
							+ board.getId());
				}
			}
		}
	}

	/**
	 * Convert form a list of gameboard DOs to a list of Gameboard DTOs.
	 * 
	 * @param gameboardDOs
	 *            to convert
	 * @param populateGameboardItems
	 *            - true if we should fully populate the gameboard DTO with gameboard items 
	 *            false if a summary is ok do? 
	 * @return gameboard DTO
	 */
	private List<GameboardDTO> convertToGameboardDTOs(
			final List<GameboardDO> gameboardDOs, final boolean populateGameboardItems) {
		Validate.notNull(gameboardDOs);

		List<GameboardDTO> gameboardDTOs = Lists.newArrayList();

		for (GameboardDO gameboardDO : gameboardDOs) {
			gameboardDTOs.add(this.convertToGameboardDTO(gameboardDO, populateGameboardItems));
		}

		return gameboardDTOs;
	}

	/**
	 * Convert form a gameboard DO to a Gameboard DTO.
	 * 
	 * This method relies on the api to fully resolve questions.
	 * 
	 * @param gameboardDO
	 *            - to convert
	 * @return gameboard DTO
	 */
	private GameboardDTO convertToGameboardDTO(final GameboardDO gameboardDO) { 
		return this.convertToGameboardDTO(gameboardDO, true);
	}
	
	/**
	 * Convert form a gameboard DO to a Gameboard DTO.
	 * 
	 * This method relies on the api to fully resolve questions.
	 * 
	 * @param gameboardDO
	 *            - to convert
	 * @param populateGameboardItems
	 *            - true if we should fully populate the gameboard DTO with gameboard items 
	 *            false if a summary is ok do? 
	 * @return gameboard DTO
	 */
	private GameboardDTO convertToGameboardDTO(final GameboardDO gameboardDO, final boolean populateGameboardItems) {
		GameboardDTO gameboardDTO = mapper.map(gameboardDO, GameboardDTO.class);
		
		if (!populateGameboardItems) {
			gameboardDTO.setQuestions(null);
			return gameboardDTO;
		}
		
		// build query the db to get full question information
		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMap 
			= Maps.newHashMap();

		fieldsToMap.put(immutableEntry(
				Constants.BooleanOperator.OR, Constants.ID_FIELDNAME + '.'
						+ Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX),
				gameboardDO.getQuestions());

		fieldsToMap.put(immutableEntry(
				Constants.BooleanOperator.OR, Constants.TYPE_FIELDNAME), Arrays
				.asList(QUESTION_TYPE));

		// Search for questions that match the ids.
		ResultsWrapper<ContentDTO> results = api.findMatchingContent(api
				.getLiveVersion(), fieldsToMap, 0, gameboardDO.getQuestions()
				.size());

		List<ContentDTO> questionsForGameboard = results.getResults();

		// Map each Content object into an GameboardItem object
		Map<String, GameboardItem> gameboardReadyQuestions = new HashMap<String, GameboardItem>();

		for (ContentDTO c : questionsForGameboard) {
			GameboardItem questionInfo = mapper.map(c, GameboardItem.class);
			questionInfo.setUri(IsaacController.generateApiUrl(c));
			gameboardReadyQuestions.put(c.getId(), questionInfo);
		}

		// empty and repopulate the gameboard dto.
		gameboardDTO.setQuestions(new ArrayList<GameboardItem>());
		for (String questionid : gameboardDO.getQuestions()) {
			gameboardDTO.getQuestions().add(
					gameboardReadyQuestions.get(questionid));
		}
		return gameboardDTO;
	}

	/**
	 * Convert from a gameboard DTO to a gameboard DO.
	 * 
	 * @param gameboardDTO
	 *            - DTO to convert.
	 * @return GameboardDO.
	 */
	private GameboardDO convertToGameboardDO(final GameboardDTO gameboardDTO) {

		GameboardDO gameboardDO = mapper.map(gameboardDTO, GameboardDO.class);
		// the mapping operation won't work for the list so we should just
		// create a new one.
		gameboardDO.setQuestions(new ArrayList<String>());

		// Map each question into an IsaacQuestionInfo object
		for (GameboardItem c : gameboardDTO.getQuestions()) {
			gameboardDO.getQuestions().add(c.getId());
		}

		return gameboardDO;
	}
}
