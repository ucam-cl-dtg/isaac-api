package uk.ac.cam.cl.dtg.isaac.app;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import uk.ac.cam.cl.dtg.isaac.api.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.IsaacController;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.UserManager;
import uk.ac.cam.cl.dtg.segue.dos.users.User;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Test class for the user manager class.
 * 
 */
public class IsaacControllerTests {

	private SegueApiFacade dummyAPI = null;
	private PropertiesLoader dummyPropertiesLoader = null;
	private GameManager dummyGameManager = null;
	private UserManager dummyUserManager = null;
	private String validLiveVersion = "d600d7af95b3cbceecd6910604fa9ea0c5337219";

	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		this.dummyAPI = createMock(SegueApiFacade.class);
		this.dummyPropertiesLoader = createMock(PropertiesLoader.class);
		this.dummyGameManager = createMock(GameManager.class);
		this.dummyUserManager = createMock(UserManager.class);
	}

	/**
	 * Verify that when an empty gameboard is noticed a 204 is returned.
	 */
	@Test
	public final void isaacEndPoint_checkEmptyGameboardCausesError_SegueErrorResponseShouldBeReturned() {
		IsaacController isaacController = new IsaacController(dummyAPI,
				dummyPropertiesLoader, dummyGameManager);

		HttpServletRequest dummyRequest = createMock(HttpServletRequest.class);
		String subjects = "physics";
		String fields = "mechanics";
		String topics = "dynamics";
		String levels = "2,3,4";
		String concepts = "newtoni";

		expect(dummyAPI.getCurrentUser(dummyRequest)).andReturn(null)
				.atLeastOnce();

		expect(
				dummyGameManager.generateRandomGameboard(
						EasyMock.<List<String>> anyObject(),
						EasyMock.<List<String>> anyObject(),
						EasyMock.<List<String>> anyObject(),
						EasyMock.<List<Integer>> anyObject(),
						EasyMock.<List<String>> anyObject(),
						EasyMock.<User> anyObject())).andReturn(null)
				.atLeastOnce();

		replay(dummyGameManager);
		replay(dummyAPI);

		Response r = isaacController.generateGameboard(dummyRequest, subjects,
				fields, topics, levels, concepts);

		assertTrue(r.getStatus() == Status.NO_CONTENT.getStatusCode());
		verify(dummyAPI, dummyGameManager);
	}
}
