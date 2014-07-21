package uk.ac.cam.cl.dtg.segue.dao;

import java.util.List;

import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.mongodb.DB;
import com.mongodb.MongoException;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dos.users.LinkedAccount;
import uk.ac.cam.cl.dtg.segue.dos.users.QuestionAttempt;
import uk.ac.cam.cl.dtg.segue.dos.users.User;

/**
 * This class is responsible for managing and persisting user data.
 * 
 * @author Stephen Cummins
 */
public class MongoUserDataManager implements IUserDataManager {

	private static final Logger log = LoggerFactory
			.getLogger(MongoUserDataManager.class);

	private final DB database;
	private static final String USER_COLLECTION_NAME = "users";
	private static final String LINKED_ACCOUNT_COLLECTION_NAME = "linkedAccounts";

	/**
	 * Creates a new user data maanger object.
	 * 
	 * @param database
	 *            - the database reference used for persistence.
	 */
	@Inject
	public MongoUserDataManager(final DB database) {
		this.database = database;
	}

	@Override
	public final String register(final User user,
			final AuthenticationProvider provider, final String providerUserId) {
		JacksonDBCollection<User, String> jc = JacksonDBCollection.wrap(
				database.getCollection(USER_COLLECTION_NAME), User.class,
				String.class);

		// ensure userId is empty as if this is a registration then it should
		// get a new id.
		user.setDbId(null);
		WriteResult<User, String> r = jc.save(user);

		User localUser = r.getSavedObject();
		String localUserId = r.getDbObject().get("_id").toString();

		// link the provider account to the newly created account.
		this.linkAuthProviderToAccount(localUser, provider, providerUserId);

		return localUserId;
	}

	@Override
	public final User getById(final String id) {
		if (null == id) {
			return null;
		}

		JacksonDBCollection<User, String> jc = JacksonDBCollection.wrap(
				database.getCollection(USER_COLLECTION_NAME), User.class,
				String.class);

		// Do database query using plain mongodb so we only have to read from
		// the database once.
		User user = jc.findOneById(id);

		return user;
	}

	@Override
	public final void updateUser(final User user) {
		JacksonDBCollection<User, String> jc = JacksonDBCollection.wrap(
				database.getCollection(USER_COLLECTION_NAME), User.class,
				String.class);

		WriteResult<User, String> r = jc.save(user);

		if (r.getError() != null) {
			log.error("Error during database update " + r.getError());
		}
	}

	@Override
	public void registerQuestionAttempt(final User user,
			final String questionPageId, final String fullQuestionId,
			final QuestionAttempt questionAttempt) {
		JacksonDBCollection<User, String> jc = JacksonDBCollection.wrap(
				database.getCollection(USER_COLLECTION_NAME), User.class,
				String.class);

		try {
			WriteResult<User, String> r = jc.updateById(
					user.getDbId(),
					DBUpdate.set(Constants.QUESTION_ATTEMPTS_FIELDNAME + "."
							+ questionPageId + "." + fullQuestionId, questionAttempt));

			if (r.getError() != null) {
				log.error("Error during database update " + r.getError());
			}
		} catch (MongoException e) {
			log.error("MongoDB Database Exception. ", e);
		}
	}

	@Override
	public final void addItemToListField(final User user,
			final String fieldName, final List value) {
		JacksonDBCollection<User, String> jc = JacksonDBCollection.wrap(
				database.getCollection(USER_COLLECTION_NAME), User.class,
				String.class);

		WriteResult<User, String> r = jc.updateById(user.getDbId(),
				DBUpdate.addToSet(fieldName, value));

		if (r.getError() != null) {
			log.error("Error during database update " + r.getError());
		}
	}

	@Override
	public final User getByLinkedAccount(final AuthenticationProvider provider,
			final String providerUserId) {
		if (null == provider || null == providerUserId) {
			return null;
		}

		JacksonDBCollection<LinkedAccount, String> jc = JacksonDBCollection
				.wrap(database.getCollection(LINKED_ACCOUNT_COLLECTION_NAME),
						LinkedAccount.class, String.class);

		LinkedAccount linkAccount = jc.findOne(DBQuery.and(DBQuery.is(
				Constants.LINKED_ACCOUNT_PROVIDER_FIELDNAME, provider), DBQuery
				.is(Constants.LINKED_ACCOUNT_PROVIDER_USER_ID_FIELDNAME,
						providerUserId)));

		if (null == linkAccount) {
			return null;
		}

		return this.getById(linkAccount.getLocalUserId());
	}

	/**
	 * Creates a link record, connecting a local user to an external provider
	 * for authentication purposes.
	 * 
	 * @param user
	 *            - the local user object
	 * @param provider
	 *            - the provider that authenticated the user.
	 * @param providerUserId
	 *            - the providers unique id for the user.
	 * @return true if success false if failure.
	 */
	private boolean linkAuthProviderToAccount(final User user,
			final AuthenticationProvider provider, final String providerUserId) {
		JacksonDBCollection<LinkedAccount, String> jc = JacksonDBCollection
				.wrap(database.getCollection(LINKED_ACCOUNT_COLLECTION_NAME),
						LinkedAccount.class, String.class);

		WriteResult<LinkedAccount, String> r = jc.save(new LinkedAccount(null,
				user.getDbId(), provider, providerUserId));

		return null == r.getError();
	}

}
