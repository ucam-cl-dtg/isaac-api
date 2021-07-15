/**
 * Copyright 2014 Stephen Cummins and Nick Rogers
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
package uk.ac.cam.cl.dtg.segue.api.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.codec.binary.Base64;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.reflect.Whitebox;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.FacebookAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuth2Authenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISecondFactorAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CrossSiteRequestForgeryException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IAnonymousUserDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for the user manager class.
 * 
 */
@PowerMockIgnore({ "javax.ws.*" })
public class UserManagerTest {
    private QuestionManager dummyQuestionDatabase;
    private IUserDataManager dummyDatabase;
    private String dummyHMACSalt;
    private Map<AuthenticationProvider, IAuthenticator> dummyProvidersMap;
    private String dummyHostName;
    private PropertiesLoader dummyPropertiesLoader;
    private static final String CSRF_TEST_VALUE = "CSRFTESTVALUE";

    private MapperFacade dummyMapper;
    private EmailManager dummyQueue;
    private SimpleDateFormat sdf;

    private IAnonymousUserDataManager dummyUserCache;
    private ILogManager dummyLogManager;
    private SegueLocalAuthenticator dummyLocalAuth;

    private ISecondFactorAuthenticator dummySecondFactorAuthenticator;

    /**
     * Initial configuration of tests.
     * 
     * @throws Exception
     *             - test exception
     */
    @Before
    public final void setUp() throws Exception {
        this.dummyQuestionDatabase = createMock(QuestionManager.class);
        this.dummyDatabase = createMock(IUserDataManager.class);
        this.dummyHMACSalt = "BOB";
        this.dummyProvidersMap = new HashMap<>();

        this.dummyLocalAuth = createMock(SegueLocalAuthenticator.class);
        this.dummyProvidersMap.put(AuthenticationProvider.SEGUE, dummyLocalAuth);

        this.dummyHostName = "bob";
        this.dummyMapper = createMock(MapperFacade.class);
        this.dummyQueue = createMock(EmailManager.class);
        this.dummyPropertiesLoader = createMock(PropertiesLoader.class);
        this.sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");

        this.dummyUserCache = createMock(IAnonymousUserDataManager.class);

        this.dummyLogManager = createMock(ILogManager.class);

        this.dummySecondFactorAuthenticator = createMock(ISecondFactorAuthenticator.class);

        expect(this.dummyPropertiesLoader.getProperty(Constants.HMAC_SALT)).andReturn(dummyHMACSalt).anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(Constants.HOST_NAME)).andReturn(dummyHostName).anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(Constants.SESSION_EXPIRY_SECONDS_DEFAULT)).andReturn("60")
                .anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(Constants.SESSION_EXPIRY_SECONDS_REMEMBERED)).andReturn("360")
                .anyTimes();
        expect(this.dummyPropertiesLoader.getProperty(Constants.SEGUE_APP_ENVIRONMENT)).andReturn("DEV").anyTimes();
        replay(this.dummyPropertiesLoader);
    }

    /**
     * Test that the get current user method behaves correctly when not logged in.
     */
    @Test
    public final void getCurrentUser_isNotLoggedIn_NoUserLoggedInExceptionThrown() {
        UserAccountManager userManager = buildTestUserManager();

        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        Cookie[] emptyCookies = {};
        expect(request.getCookies()).andReturn(emptyCookies).anyTimes();

        replay(dummySession);
        replay(request);
        replay(dummyQuestionDatabase);

        // Act
        try {
            userManager.getCurrentRegisteredUser(request);

            // Assert
            fail("Expected NoUserLoggedInException");
        } catch (NoUserLoggedInException e) {
            // fine
        }

        verify(dummyQuestionDatabase, dummySession, request);
    }

    /**
     * Test that get current user with valid HMAC works correctly.
     * 
     * @throws Exception
     */
    @Test
    public final void getCurrentUser_IsAuthenticatedWithValidHMAC_userIsReturned() throws Exception {
        UserAccountManager userManager = buildTestUserManager();
        UserAuthenticationManager authManager = buildTestAuthenticationManager();
        HttpServletRequest request = createMock(HttpServletRequest.class);

        Long validUserId = 533L;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 500);
        String validDateString = sdf.format(calendar.getTime());

        RegisteredUser returnUser = new RegisteredUser(validUserId, "TestFirstName", "TestLastName", "", Role.STUDENT,
                new Date(), Gender.MALE, new Date(), null, null, null,null);
        returnUser.setId(validUserId);
        returnUser.setSessionToken(0);

        Map<String, String> sessionInformation = getSessionInformationAsAMap(authManager, validUserId.toString(), validDateString, returnUser.getSessionToken());
        Cookie[] cookieWithSessionInfo = getCookieArray(sessionInformation);
       
        dummyDatabase.updateUserLastSeen(returnUser);
        expectLastCall();

        expect(request.getCookies()).andReturn(cookieWithSessionInfo).anyTimes();
        replay(request);

        expect(dummyDatabase.getById(validUserId)).andReturn(returnUser);
        expect(dummyDatabase.getAuthenticationProvidersByUsers(Collections.singletonList(returnUser)))
                .andReturn(ImmutableMap.of(returnUser, Lists.newArrayList(AuthenticationProvider.GOOGLE))).once();
        expect(dummyDatabase.getSegueAccountExistenceByUsers(Collections.singletonList(returnUser)))
                .andReturn(ImmutableMap.of(returnUser, false)).atLeastOnce();
        replay(dummyQuestionDatabase);

        expect(dummyMapper.map(returnUser, RegisteredUserDTO.class)).andReturn(new RegisteredUserDTO()).atLeastOnce();
        replay(dummyMapper, dummyDatabase, dummyLocalAuth);

        // Act
        RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

        // Assert
        assertTrue(user != null);

        verify(dummyQuestionDatabase, request, dummyMapper);
    }

    /**
     * Test that requesting authentication with a bad provider behaves as expected.
     * 
     * @throws Exception
     */
    @Test
    public final void authenticate_badProviderGiven_authenticationProviderException() throws Exception {
        UserAccountManager userManager = buildTestUserManager();

        HttpServletRequest request = createMock(HttpServletRequest.class);

        String someInvalidProvider = "BAD_PROVIDER!!";

        replay(request);
        replay(dummyQuestionDatabase);

        // Act
        try {
            userManager.authenticate(request, someInvalidProvider);
            fail("Exception expected");
        } catch (AuthenticationProviderMappingException e) {
            // pass
        }
       
        verify(dummyQuestionDatabase, request);
    }

    /**
     * Test that a valid OAuth provider (Facebook) provides a redirect response.
     * 
     * @throws IOException
     *             - test exception
     * @throws AuthenticationProviderMappingException 
     */
    @Test
    public final void authenticate_selectedValidOAuthProvider_providesRedirectResponseForAuthorization()
            throws IOException, AuthenticationProviderMappingException {
        // Arrange
        IOAuth2Authenticator dummyAuth = createMock(IOAuth2Authenticator.class);
        UserAccountManager userManager = buildTestUserManager(AuthenticationProvider.TEST, dummyAuth);

        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        String exampleRedirectUrl = "https://accounts.google.com/o/oauth2/auth?"
                + "client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&"
                + "redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&"
                + "response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20"
                + "https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms";
        String someValidProviderString = "test";

        // for CSRF state information
        expect(request.getSession()).andReturn(dummySession).atLeastOnce();
        dummySession.setAttribute(EasyMock.<String> anyObject(), EasyMock.<String> anyObject());
        expectLastCall().atLeastOnce();

        replay(dummySession);
        replay(request);
        replay(dummyQuestionDatabase);

        String someAntiForgeryToken = "someAntiForgeryToken";
        expect(dummyAuth.getAntiForgeryStateToken()).andReturn(someAntiForgeryToken).once();
        expect(dummyAuth.getAuthorizationUrl(someAntiForgeryToken)).andReturn(exampleRedirectUrl).once();
        replay(dummyAuth);

        // Act
        URI redirectURI = userManager.authenticate(request, someValidProviderString);

        // Assert
        verify(dummyQuestionDatabase, request);

        assertTrue(redirectURI.toString().equals(exampleRedirectUrl));
    }

    /**
     * Check that a new (unseen) user is registered when seen with 3rd party authenticator.
     * 
     * @throws Exception
     *             -
     */
    @Test
    public final void authenticateCallback_checkNewUserIsAuthenticated_createInternalUserAccount() throws Exception {
        IOAuth2Authenticator dummyAuth = createMock(FacebookAuthenticator.class);
        UserAccountManager userManager = buildTestUserManager(AuthenticationProvider.TEST, dummyAuth);
        UserAuthenticationManager authManager = buildTestAuthenticationManager(AuthenticationProvider.TEST, dummyAuth);
        
        // method param setup for method under test
        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);

        String someDomain = "http://www.somedomain.com/";
        String someClientId = "someClientId";
        String someAuthCode = "someAuthCode";
        String someState = "someState";

        StringBuffer sb = new StringBuffer(someDomain + "?state=" + someState + "&code=" + someAuthCode);
        String validQueryStringFromProvider = "client_id=" + someClientId + "&redirect_uri=" + someDomain;
        String fullResponseUrlFromProvider = someDomain + "?state=" + someState + "&code=" + someAuthCode
                + "?client_id=" + someClientId + "&redirect_uri=" + someDomain;
        String someProviderGeneratedLookupValue = "MYPROVIDERREF";
        String someProviderUniqueUserId = "USER-1";

        Long someSegueUserId = 533L;
        String someSegueAnonymousUserId = "9284723987anonymous83924923";

        AnonymousUser au = new AnonymousUser();
        au.setSessionId(someSegueAnonymousUserId);
        expect(this.dummyUserCache.storeAnonymousUser(au)).andReturn(au).atLeastOnce();
        expect(this.dummyUserCache.getById(au.getSessionId())).andReturn(au).atLeastOnce();

        AnonymousUserDTO someAnonymousUserDTO = new AnonymousUserDTO();
        someAnonymousUserDTO.setSessionId(someSegueAnonymousUserId);

        String validOAuthProvider = "test";
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 500);
        String validDateString = sdf.format(calendar.getTime());

        expect(request.getSession()).andReturn(dummySession).atLeastOnce();

        Cookie[] cookieWithoutSessionInfo = {}; // empty as not logged in.
        expect(request.getCookies()).andReturn(cookieWithoutSessionInfo).times(2);

        expect(dummySession.getAttribute(Constants.ANONYMOUS_USER)).andReturn(someSegueAnonymousUserId).atLeastOnce(); // session
                                                                                                                       // id

        // Mock CSRF checks
        expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME)).andReturn(CSRF_TEST_VALUE).atLeastOnce();
        expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(CSRF_TEST_VALUE).atLeastOnce();

        // Mock URL params extract stuff
        expect(request.getQueryString()).andReturn(validQueryStringFromProvider).atLeastOnce();
        expect(request.getRequestURL()).andReturn(sb);

        // Mock extract auth code call
        expect(dummyAuth.extractAuthCode(fullResponseUrlFromProvider)).andReturn(someAuthCode);

        // Mock exchange code for token call
        expect(dummyAuth.exchangeCode(someAuthCode)).andReturn(someProviderGeneratedLookupValue).atLeastOnce();

        expect(((IFederatedAuthenticator) dummyAuth).getAuthenticationProvider())
                .andReturn(AuthenticationProvider.TEST).atLeastOnce();

        // User object back from provider
        UserFromAuthProvider providerUser = new UserFromAuthProvider(someProviderUniqueUserId, "TestFirstName",
                "TestLastName", "test@test.com", EmailVerificationStatus.VERIFIED, Role.STUDENT, new Date(), Gender.MALE);

        // Mock get User Information from provider call
        expect(((IFederatedAuthenticator) dummyAuth).getUserInfo(someProviderGeneratedLookupValue)).andReturn(
                providerUser).atLeastOnce();

        // Expect this to be a new user and to register them (i.e. return null
        // from database)
        expect(dummyDatabase.getByLinkedAccount(AuthenticationProvider.TEST, someProviderUniqueUserId)).andReturn(null)
                .atLeastOnce();

        RegisteredUser mappedUser = new RegisteredUser(null, "TestFirstName", "testLastName", "test@test.com", Role.STUDENT,
                new Date(), Gender.MALE, new Date(), null, null,null, null);
        mappedUser.setSessionToken(0);

        expect(dummyDatabase.getAuthenticationProvidersByUsers(Collections.singletonList(mappedUser)))
                .andReturn(new HashMap<RegisteredUser, List<AuthenticationProvider>>() {{
                    put(mappedUser, Lists.newArrayList(AuthenticationProvider.GOOGLE));
                }}).atLeastOnce();
        expect(dummyDatabase.getSegueAccountExistenceByUsers(Collections.singletonList(mappedUser)))
                .andReturn(ImmutableMap.of(mappedUser, false)).atLeastOnce();

        RegisteredUserDTO mappedUserDTO = new RegisteredUserDTO();

        expect(dummyMapper.map(providerUser, RegisteredUser.class)).andReturn(mappedUser).atLeastOnce();
        expect(dummyMapper.map(mappedUser, RegisteredUserDTO.class)).andReturn(mappedUserDTO).atLeastOnce();
        expect(dummyMapper.map(au, AnonymousUserDTO.class)).andReturn(someAnonymousUserDTO).anyTimes();

        // handle duplicate account check.
        expect(dummyDatabase.getByEmail(providerUser.getEmail())).andReturn(null).once();

        // A main part of the test is to check the below call happens
        expect(
                dummyDatabase.registerNewUserWithProvider(mappedUser, AuthenticationProvider.TEST,
                        someProviderUniqueUserId)).andReturn(mappedUser).atLeastOnce();

        mappedUser.setId(someSegueUserId);

        expect(dummyDatabase.getById(someSegueUserId)).andReturn(mappedUser);

        Map<String, String> sessionInformation = getSessionInformationAsAMap(authManager, someSegueUserId.toString(),
                validDateString, mappedUser.getSessionToken());
        Cookie[] cookieWithSessionInfo = getCookieArray(sessionInformation);

        // Expect a session to be created
        response.addCookie(cookieWithSessionInfo[0]);
        expectLastCall().once();
        expect(request.getCookies()).andReturn(cookieWithSessionInfo).anyTimes();

        dummyQuestionDatabase.mergeAnonymousQuestionAttemptsIntoRegisteredUser(someAnonymousUserDTO, mappedUserDTO);
        expectLastCall().once();

        expect(dummyQueue.getEmailTemplateDTO("email-template-registration-confirmation-federated")).andReturn(new EmailTemplateDTO()).once();
        dummyQueue.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), anyObject());
        expectLastCall().once();

        replay(dummySession, request, dummyAuth, dummyQuestionDatabase, dummyMapper, dummyDatabase, dummyLocalAuth, dummyQueue, dummyUserCache);

        // Act
        RegisteredUserDTO u = userManager.authenticateCallback(request, response, validOAuthProvider, false);

        // Assert
        verify(dummySession, request, dummyAuth, dummyQuestionDatabase);
        assertTrue(u instanceof RegisteredUserDTO);
    }

    /**
     * Verify that a bad CSRF response from the authentication provider causes an error response.
     *
     */
    @Test
    public final void authenticateCallback_checkInvalidCSRF_throwsCSRFException() {
        UserAccountManager userManager = buildTestUserManager();

        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);

        String someInvalidCSRFValue = "FRAUDHASHAPPENED";
        String validOAuthProvider = "test";

        expect(request.getSession()).andReturn(dummySession).atLeastOnce();
        expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(null).anyTimes();

        // Mock URL params extract stuff
        // Return any non-null string
        String queryString = Constants.STATE_PARAM_NAME + "=" + someInvalidCSRFValue;
        expect(request.getQueryString()).andReturn(queryString).once();

        // Mock CSRF checks
        expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME)).andReturn(CSRF_TEST_VALUE).atLeastOnce();

        expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(someInvalidCSRFValue).atLeastOnce();

        replay(dummySession, request, dummyQuestionDatabase);

        // Act
        try {
            userManager.authenticateCallback(request, response, validOAuthProvider, false);
            fail("Exception should have been thrown");
        } catch (CrossSiteRequestForgeryException e) {
            // success
        } catch (Exception e) {
            // not interested in these cases.
        }
        
        // Assert
        verify(dummyQuestionDatabase, dummySession, request);
    }

    /**
     * Verify that a bad (null) CSRF response from the authentication provider causes an error response.
     *
     */
    @Test
    public final void authenticateCallback_checkWhenNoCSRFProvided_throwsCSRFException() {
        UserAccountManager userManager = buildTestUserManager();

        // method param setup for method under test
        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);
        HttpServletResponse response = createMock(HttpServletResponse.class);

        String validOAuthProvider = "test";

        expect(request.getSession()).andReturn(dummySession).atLeastOnce();
        expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(null).anyTimes();

        // Mock URL params extract stuff
        expect(request.getQueryString()).andReturn("").atLeastOnce();

        // Mock CSRF checks
        expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME)).andReturn(null).atLeastOnce();
        expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(CSRF_TEST_VALUE).atLeastOnce();

        replay(dummySession);
        replay(request);
        replay(dummyQuestionDatabase);

        // Act
        try {
            userManager.authenticateCallback(request, response, validOAuthProvider, false);
            fail("Exception should have been thrown");
        } catch (CrossSiteRequestForgeryException e) {
            // pass
        } catch (Exception e){
            // not interested in this case.
        }
        
        // Assert
        verify(dummyQuestionDatabase, dummySession, request);
        
    }

    /**
     * Verify that a correct HMAC response works correctly.
     * 
     * This method is dependent on the crypto algorithm used.
     * 
     * @throws Exception
     */
    @Test
    public final void validateUsersSession_checkForValidHMAC_shouldReturnAsCorrect() throws Exception {
        UserAuthenticationManager authManager = buildTestAuthenticationManager();
        
        // method param setup for method under test
        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        RegisteredUser mappedUser = new RegisteredUser(null, "TestFirstName", "testLastName", "test@test.com", Role.STUDENT,
                new Date(), Gender.MALE, new Date(), null, null,null, null);
        mappedUser.setSessionToken(0);

        String validUserId = "123";
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 500);
        String validDateString = sdf.format(calendar.getTime());

        Map<String, String> sessionInformation = getSessionInformationAsAMap(authManager, validUserId, validDateString, mappedUser.getSessionToken());

        replay(dummySession);
        replay(request);
        replay(dummyQuestionDatabase);

        // Act
        boolean valid = Whitebox.<Boolean> invokeMethod(authManager, "isValidUsersSession", sessionInformation, mappedUser);

        // Assert
        verify(dummyQuestionDatabase, dummySession, request);
        assertTrue(valid);
    }

    /**
     * Verify that a user session which has been tampered with is detected as invalid.
     * 
     * @throws Exception
     */
    @Test
    public final void validateUsersSession_badUsersSession_shouldReturnAsIncorrect() throws Exception {
        UserAuthenticationManager authManager = buildTestAuthenticationManager();
        
        // method param setup for method under test
        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        String validUserId = "123";
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 500);
        String validDateString = sdf.format(calendar.getTime());

        RegisteredUser mappedUser = new RegisteredUser(null, "TestFirstName", "testLastName", "test@test.com", Role.STUDENT,
                new Date(), Gender.MALE, new Date(), null, null,null, null);
        mappedUser.setSessionToken(0);

        Map<String, String> validSessionInformation = getSessionInformationAsAMap(authManager, validUserId,
                validDateString, mappedUser.getSessionToken());

        Map<String, String> tamperedSessionInformation = ImmutableMap.of(
                Constants.SESSION_USER_ID, validUserId,
                Constants.SESSION_TOKEN, mappedUser.getSessionToken().toString(),
                Constants.DATE_EXPIRES, validDateString + "1",
                Constants.HMAC, validSessionInformation.get(Constants.HMAC)
        );

        replay(dummySession);
        replay(request);
        replay(dummyQuestionDatabase);

        // Act
        boolean valid = Whitebox.<Boolean> invokeMethod(authManager, "isValidUsersSession", tamperedSessionInformation, mappedUser);

        // Assert
        verify(dummyQuestionDatabase, dummySession, request);
        assertFalse(valid);
    }

    /**
     * Verify that an expired user session is detected as invalid.
     * 
     * @throws Exception
     */
    @Test
    public final void validateUsersSession_expiredUsersSession_shouldReturnAsIncorrect() throws Exception {
        UserAuthenticationManager authManager = buildTestAuthenticationManager();
        
        // method param setup for method under test
        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        String validUserId = "123";
        RegisteredUser mappedUser = new RegisteredUser(null, "TestFirstName", "testLastName", "test@test.com", Role.STUDENT,
                new Date(), Gender.MALE, new Date(), null, null,null, null);
        mappedUser.setSessionToken(0);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -60); // Expired 60 seconds ago
        String expiredDateString = sdf.format(calendar.getTime());

        Map<String, String> validSessionInformation = getSessionInformationAsAMap(authManager, validUserId,
                expiredDateString, mappedUser.getSessionToken());

        replay(dummySession);
        replay(request);
        replay(dummyQuestionDatabase);

        // Act
        boolean valid = Whitebox.<Boolean> invokeMethod(authManager, "isValidUsersSession", validSessionInformation, mappedUser);

        // Assert
        verify(dummyQuestionDatabase, dummySession, request);
        assertTrue(!valid);
    }

    /**
     * Verify that a changed session token is detected.
     *
     * @throws Exception
     */
    @Test
    public final void validateUsersSession_incorrectSessionToken_shouldReturnAsIncorrect() throws Exception {
        UserAuthenticationManager authManager = buildTestAuthenticationManager();

        // method param setup for method under test
        HttpSession dummySession = createMock(HttpSession.class);
        HttpServletRequest request = createMock(HttpServletRequest.class);

        String validUserId = "123";
        RegisteredUser mappedUser = new RegisteredUser(null, "TestFirstName", "testLastName", "test@test.com", Role.STUDENT,
                new Date(), Gender.MALE, new Date(), null, null,null, null);
        mappedUser.setSessionToken(1);
        Integer incorrectSessionToken = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 500);
        String validDateString = sdf.format(calendar.getTime());

        Map<String, String> sessionInformationWithTokenMismatch = getSessionInformationAsAMap(authManager, validUserId,
                validDateString, incorrectSessionToken);

        replay(dummySession);
        replay(request);
        replay(dummyQuestionDatabase);

        // Act
        boolean valid = Whitebox.<Boolean> invokeMethod(authManager, "isValidUsersSession", sessionInformationWithTokenMismatch, mappedUser);

        // Assert
        verify(dummyQuestionDatabase, dummySession, request);
        assertTrue(!valid);
    }

    /**
     * Helper method to construct a UserManager with the default TEST provider.
     * 
     * @return A new UserManager instance
     */
    private UserAccountManager buildTestUserManager() {
        return buildTestUserManager(AuthenticationProvider.TEST, createMock(IOAuth2Authenticator.class));
    }

    /**
     * Helper method to construct a UserManager with the specified providers.
     * 
     * @param provider
     *            - The provider to register
     * @param authenticator
     *            - The associated authenticating engine
     * @return A new UserManager instance
     */
    private UserAccountManager buildTestUserManager(final AuthenticationProvider provider,
            final IFederatedAuthenticator authenticator) {
        HashMap<AuthenticationProvider, IAuthenticator> providerMap = new HashMap<AuthenticationProvider, IAuthenticator>();
        providerMap.put(provider, authenticator);
        return new UserAccountManager(dummyDatabase, this.dummyQuestionDatabase, this.dummyPropertiesLoader,
                providerMap, this.dummyMapper, this.dummyQueue, this.dummyUserCache, this.dummyLogManager,
                buildTestAuthenticationManager(provider, authenticator), dummySecondFactorAuthenticator);
    }
    
    private UserAuthenticationManager buildTestAuthenticationManager() {
        return new UserAuthenticationManager(dummyDatabase, dummyPropertiesLoader, dummyProvidersMap, dummyQueue);
    }
    
    private UserAuthenticationManager buildTestAuthenticationManager(AuthenticationProvider provider, IAuthenticator authenticator) {
        HashMap<AuthenticationProvider, IAuthenticator> providerMap = new HashMap<AuthenticationProvider, IAuthenticator>();
        providerMap.put(provider, authenticator);
        providerMap.put(AuthenticationProvider.SEGUE, dummyLocalAuth);
        return new UserAuthenticationManager(dummyDatabase, dummyPropertiesLoader, providerMap, dummyQueue);
    }

    private Map<String, String> getSessionInformationAsAMap(UserAuthenticationManager userAuthManager, String userId, String dateExpires, Integer sessionToken)
            throws Exception {
        String validHMAC = Whitebox.<String> invokeMethod(userAuthManager, "calculateSessionHMAC", dummyHMACSalt, userId,
                dateExpires, sessionToken.toString(), null);
        return ImmutableMap.of(Constants.SESSION_USER_ID, userId, Constants.DATE_EXPIRES, dateExpires, Constants.HMAC,
                validHMAC, Constants.SESSION_TOKEN, sessionToken.toString());
    }

    private Cookie[] getCookieArray(Map<String, String> sessionInformation) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        Cookie[] cookieWithSessionInfo = { new Cookie(Constants.SEGUE_AUTH_COOKIE,
                Base64.encodeBase64String(om.writeValueAsString(sessionInformation).getBytes())) };
        return cookieWithSessionInfo;
    }
}
