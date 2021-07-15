/*
 * Copyright 2021 Raspberry Pi Foundation
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
package uk.ac.cam.cl.dtg.isaac.api;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.DoNotCall;
import com.google.inject.Inject;
import com.opencsv.CSVWriter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentCancelledException;
import uk.ac.cam.cl.dtg.isaac.api.managers.AttemptCompletedException;
import uk.ac.cam.cl.dtg.isaac.api.managers.DueBeforeNowException;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateAssignmentException;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAttemptManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizQuestionManager;
import uk.ac.cam.cl.dtg.isaac.api.services.AssignmentService;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizFeedbackDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizUserFeedbackDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants.SegueServerLogType;
import uk.ac.cam.cl.dtg.segue.api.ErrorResponseWrapper;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.ok;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.QUIZ_ID_FKEY;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.QUIZ_SECTION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ASSIGNMENT_DUEDATE_FK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.GROUP_FK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_HOUR;
import static uk.ac.cam.cl.dtg.segue.api.Constants.QUIZ_ASSIGNMENT_FK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.QUIZ_ATTEMPT_FK;
import static uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager.extractPageIdFromQuestionId;

/**
 * Quiz Facade
 */
@Path("/quiz")
@Api(value = "/quiz")
public class QuizFacade extends AbstractIsaacFacade {
    private final IContentManager contentManager;
    private final QuizManager quizManager;
    private final UserAccountManager userManager;
    private final UserAssociationManager associationManager;
    private final GroupManager groupManager;
    private final QuizAssignmentManager quizAssignmentManager;
    private final AssignmentService assignmentService;
    private final QuizAttemptManager quizAttemptManager;
    private final QuizQuestionManager quizQuestionManager;

    private static final Logger log = LoggerFactory.getLogger(QuizFacade.class);

    /**
     * QuizFacade. For management of quizzes
     * @param properties
     *            - global properties map
     * @param logManager
     *            - for managing logs.
     * @param contentManager
     *            - for the content etag
     * @param quizManager
     *            - for quiz interaction.
     * @param userManager
     *            - for user information.
     * @param associationManager
     *            - So that we can determine what information is allowed to be seen by other users.
     * @param groupManager
     *            - for group information.
     * @param quizAssignmentManager
     *            - for managing the assignment of quizzes.
     * @param assignmentService
     *            - for general assignment-related services.
     * @param quizAttemptManager
     *            - for managing attempts at quizzes.
     * @param quizQuestionManager
     *            - for parsing, validating, and persisting quiz question answers.
     */
    @Inject
    public QuizFacade(final PropertiesLoader properties, final ILogManager logManager,
                      final IContentManager contentManager, final QuizManager quizManager,
                      final UserAccountManager userManager, final UserAssociationManager associationManager,
                      final GroupManager groupManager, final QuizAssignmentManager quizAssignmentManager,
                      final AssignmentService assignmentService, final QuizAttemptManager quizAttemptManager,
                      final QuizQuestionManager quizQuestionManager) {
        super(properties, logManager);

        this.contentManager = contentManager;
        this.quizManager = quizManager;
        this.userManager = userManager;
        this.associationManager = associationManager;
        this.groupManager = groupManager;
        this.quizAssignmentManager = quizAssignmentManager;
        this.assignmentService = assignmentService;
        this.quizAttemptManager = quizAttemptManager;
        this.quizQuestionManager = quizQuestionManager;
    }

    /**
     * Get quizzes visible to this user.
     *
     * Anonymous users can't see quizzes.
     * Students can see quizzes with the visibleToStudents flag set.
     * Teachers and higher can see all quizzes.
     *
     * @return a Response containing a list of ContentSummaryDTO for the visible quizzes.
     */
    @GET
    @Path("/available")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get quizzes visible to this user.")
    public final Response getAvailableQuizzes(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            boolean isStudent = !isUserTeacherOrAbove(userManager, user);

            EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + "");

            ResultsWrapper<ContentSummaryDTO> summary = this.quizManager.getAvailableQuizzes(isStudent, null, null);

            return ok(summary).tag(etag)
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, isStudent))
                .build();
        } catch (ContentManagerException e) {
            String message = "ContentManagerException whilst getting available quizzes";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }


    /**
     * Get quizzes assigned to this user.
     *
     * Shows a content summary, so we can track when a user actually attempts a quiz.
     *
     * @return a Response containing a list of QuizAssignmentDTO for the assigned quizzes.
     */
    @GET
    @Path("/assignments")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get quizzes assigned to this user.")
    public final Response getAssignedQuizzes(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            List<QuizAssignmentDTO> assignments = this.quizAssignmentManager.getAssignedQuizzes(user);

            this.assignmentService.augmentAssignerSummaries(assignments);

            quizManager.augmentWithQuizSummary(assignments);

            this.quizAttemptManager.augmentAssignmentsFor(user, assignments);

            return Response.ok(assignments)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst getting available quizzes";
            log.error(message, e);
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Get quizzes freely attempted by this user.
     *
     * Shows a content summary, so we can track when a user actually attempts a quiz.
     *
     * @return a Response containing a list of QuizAttemptDTO for the freely attempted quizzes.
     */
    @GET
    @Path("/free_attempts")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get quizzes freely attempted by this user.")
    public final Response getFreeAttempts(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            List<QuizAttemptDTO> attempts = this.quizAttemptManager.getFreeAttemptsFor(user);

            quizManager.augmentWithQuizSummary(attempts);

            return Response.ok(attempts)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst getting available quizzes";
            log.error(message, e);
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Preview a quiz. Only available to teachers and above.
     *
     * @param request
     *            - so we can deal with caching.
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizId
     *            as a string
     * @return a Response containing a list of ContentSummaryDTO for the visible quizzes.
     */
    @GET
    @Path("/{quizId}/preview")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Preview an individual quiz.")
    public final Response previewQuiz(@Context final Request request,
                                      @Context final HttpServletRequest httpServletRequest,
                                      @PathParam("quizId") final String quizId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            if (null == quizId || quizId.isEmpty()) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz id.").toResponse();
            }

            EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + quizId.hashCode() + "");

            Response cachedResponse = generateCachedResponse(request, etag);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            IsaacQuizDTO quiz = this.quizManager.findQuiz(quizId);

            return ok(quiz)
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, false)).tag(etag).build();
        } catch (ContentManagerException e) {
            log.error("Content error whilst previewing a quiz", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This quiz has become unavailable.");
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Start a quiz attempt for a particular quiz assignment.
     *
     * For a student, indicates the quiz is being attempted and creates a QuizAttemptDO in the DB
     * if they are a member of the group for the assignment.
     * If an attempt has already begun, return the already begun attempt.
     *
     * @see #startFreeQuizAttempt An endpoint to allow a quiz that is visibleToStudents to be taken by students.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAssignmentId
     *            - the ID of the quiz assignment for this user.
     * @return a QuizAttemptDTO
     */
    @POST
    @Path("/assignment/{quizAssignmentId}/attempt")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Start a quiz attempt.")
    public final Response startQuizAttempt(@Context final Request request,
                                           @Context final HttpServletRequest httpServletRequest,
                                           @PathParam("quizAssignmentId") final Long quizAssignmentId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (null == quizAssignmentId) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz assignment id.").toResponse();
            }

            // Get the quiz assignment
            QuizAssignmentDTO quizAssignment = this.quizAssignmentManager.getById(quizAssignmentId);

            // Check the user is an active member of the relevant group
            UserGroupDTO group = quizAssignmentManager.getGroupForAssignment(quizAssignment);
            if (!groupManager.isUserInGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are not a member of a group to which this quiz is assigned.").toResponse();
            }

            // Check the due date hasn't passed
            if (quizAssignment.getDueDate() != null && new Date().after(quizAssignment.getDueDate())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "The due date for this quiz has passed.").toResponse();
            }

            // Create a quiz attempt
            QuizAttemptDTO quizAttempt = this.quizAttemptManager.fetchOrCreate(quizAssignment, user);

            quizAttempt = augmentAttempt(quizAttempt, quizAssignment, false);

            return Response.ok(quizAttempt)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                .build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst starting a quiz attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (AttemptCompletedException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "You have already completed your attempt for this quiz.").toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.GONE, "This quiz assignment has been cancelled.").toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst starting a quiz attempt", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This quiz has become unavailable.");
        }
    }

    /**
     * Start a quiz attempt for a free quiz (one that is visibleToStudents).
     *
     * This checks that quiz has not already been assigned. (When a quiz has been set to a student,
     * they are locked out of all previous feedback for that quiz and prevented from starting the
     * quiz freely in order to prevent some ways of cheating.)
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizId
     *            - the ID of the quiz the user wishes to attempt.
     * @return a QuizAttemptDTO
     */
    @POST
    @Path("/{quizId}/attempt")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Start a free quiz attempt.")
    public final Response startFreeQuizAttempt(@Context final Request request,
                                               @Context final HttpServletRequest httpServletRequest,
                                               @PathParam("quizId") final String quizId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (null == quizId || quizId.isEmpty()) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz id.").toResponse();
            }

            // Get the quiz
            IsaacQuizDTO quiz = quizManager.findQuiz(quizId);

            // Check it is visibleToStudents
            if (!quiz.getVisibleToStudents()) {
                return new SegueErrorResponse(Status.FORBIDDEN, "Free attempts are not available for this quiz.").toResponse();
            }

            // Check if there is an active assignment of this quiz
            List<QuizAssignmentDTO> activeQuizAssignments = this.quizAssignmentManager.getActiveQuizAssignments(quiz, user);

            if (!activeQuizAssignments.isEmpty()) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are currently set this quiz. You must complete your assignment before you can attempt this quiz freely.").toResponse();
            }

            // Create a quiz attempt
            QuizAttemptDTO quizAttempt = this.quizAttemptManager.fetchOrCreateFreeQuiz(quiz, user);

            quizAttempt = augmentAttempt(quizAttempt, null, false);

            return Response.ok(quizAttempt)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                .build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst starting a free quiz attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst starting a free quiz attempt", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This quiz has become unavailable.");
        }
    }

    /**
     * Get the QuizDTO for a quiz attempt.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAttemptId
     *            - the ID of the quiz the user wishes to attempt.
     * @return The QuizDTO augmented with the user answers so far.
     */
    @GET
    @Path("/attempt/{quizAttemptId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get the QuizDTO for a quiz attempt.")
    public final Response getQuizAttempt(@Context final HttpServletRequest httpServletRequest,
                                         @PathParam("quizAttemptId") final Long quizAttemptId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            QuizAttemptDTO quizAttempt = getIncompleteQuizAttemptForUser(quizAttemptId, user);

            QuizAssignmentDTO quizAssignment = checkQuizAssignmentNotCancelledOrOverdue(quizAttempt);

            quizAttempt = augmentAttempt(quizAttempt, quizAssignment, false);

            return Response.ok(quizAttempt)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                .build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst getting quiz attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "This quiz assignment has been cancelled.").toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst getting quiz attempt", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This quiz has become unavailable.");
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        }
    }

    /**
     * Get the feedback for a quiz attempt.
     *
     * The attempt must be completed.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAttemptId
     *            - the ID of the quiz the user wishes to see feedback for.
     * @return The feedback if this user is allowed to see it.
     */
    @GET
    @Path("/attempt/{quizAttemptId}/feedback")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get the feedback for a quiz attempt.")
    public final Response getQuizAttemptFeedback(@Context final HttpServletRequest httpServletRequest,
                                                 @PathParam("quizAttemptId") final Long quizAttemptId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            QuizAttemptDTO quizAttempt = getCompleteQuizAttemptForUser(quizAttemptId, user);

            QuizAssignmentDTO quizAssignment = getQuizAssignment(quizAttempt);

            IsaacQuizDTO quiz = quizManager.findQuiz(quizAttempt.getQuizId());

            QuizFeedbackMode feedbackMode;

            if (quizAssignment != null) {
                feedbackMode = quizAssignment.getQuizFeedbackMode();
            } else {
                feedbackMode = quiz.getDefaultFeedbackMode();
                if (feedbackMode == null) {
                    feedbackMode = QuizFeedbackMode.DETAILED_FEEDBACK;
                }
            }

            quizAttempt = quizQuestionManager.augmentFeedbackFor(quizAttempt, quiz, feedbackMode);

            augmentAttempt(quizAttempt, quizAssignment);

            return Response.ok(quizAttempt)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                .build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst getting quiz attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "This quiz assignment has been cancelled.").toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst getting quiz attempt", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This quiz has become unavailable.");
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        }
    }

    /**
     * Mark a QuizAttempt as complete.
     *
     * Only the user taking the quiz can mark it complete.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAttemptId
     *            - the ID of the quiz the user wishes to attempt.
     * @return The updated quiz attempt or an error.
     */
    @POST
    @Path("/attempt/{quizAttemptId}/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Mark a QuizAttempt as complete.")
    public final Response completeQuizAttempt(@Context final HttpServletRequest httpServletRequest,
                                              @PathParam("quizAttemptId") final Long quizAttemptId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            QuizAttemptDTO quizAttempt = getQuizAttempt(quizAttemptId);

            if (!quizAttempt.getUserId().equals(user.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You cannot complete someone else's quiz.").toResponse();
            }

            if ((quizAttempt.getCompletedDate() != null)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "That quiz is already complete.").toResponse();
            }

            quizAttempt = quizAttemptManager.updateAttemptCompletionStatus(quizAttempt, true);

            return Response.ok(quizAttempt).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst marking quiz attempt complete";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        }
    }

    /**
     * Mark a QuizAttempt as incomplete.
     *
     * Only owner and group managers for the quiz assignment can mark it incomplete.
     * If it is a self-taken quiz, you cannot mark it incomplete, as that would make it easier to tweak individual
     * questions to find the right answers (also there is no assignment so you can't construct the URL anyway).
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAssignmentId
     *            - the ID of the assignment you want to mark an attempt incomplete from.
     * @param userId
     *            - the ID of the user whose attempt you want to mark incomplete.
     * @return The updated attempt or an error.
     */
    @POST
    @Path("/assignment/{quizAssignmentId}/{userId}/incomplete")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Mark a QuizAttempt as incomplete.")
    public final Response markIncompleteQuizAttempt(@Context final HttpServletRequest httpServletRequest,
                                                    @PathParam("quizAssignmentId") final Long quizAssignmentId,
                                                    @PathParam("userId") final Long userId) {
        if (null == quizAssignmentId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Missing quizAssignmentId.").toResponse();
        }
        if (null == userId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Missing userId.").toResponse();
        }
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);


            UserGroupDTO group = quizAssignmentManager.getGroupForAssignment(assignment);

            if (assignment == null || !canManageGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You can only mark assignments incomplete for groups you own or manage.").toResponse();
            }

            if (assignment.getDueDate() != null && assignment.getDueDate().before(new Date())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You cannot mark a quiz attempt as incomplete while it is still due.").toResponse();
            }

            RegisteredUserDTO student = userManager.getUserDTOById(userId);
            if (!groupManager.isUserInGroup(student, group)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "That user is not in this group.").toResponse();
            }

            QuizAttemptDTO quizAttempt = quizAttemptManager.getByQuizAssignmentAndUser(assignment, student);

            if (quizAttempt == null || quizAttempt.getCompletedDate() == null) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "That quiz is already incomplete.").toResponse();
            }

            quizAttemptManager.updateAttemptCompletionStatus(quizAttempt, false);

            QuizUserFeedbackDTO feedback;
            UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(user,
                userManager.convertToUserSummaryObject(student));

            feedback = new QuizUserFeedbackDTO(userSummary,
                userSummary.isAuthorisedFullAccess() ? new QuizFeedbackDTO() : null);

            return Response.ok(feedback).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst marking quiz attempt incomplete";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "This quiz assignment has been cancelled.").toResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "No such user.").toResponse();
        }
    }

    /**
     * Log that a student viewed a particular section of a quiz.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAttemptId
     *            - the ID of the quiz the user wishes to attempt.
     * @param sectionNumber
     *            - the number of the section being viewed.
     * @return Confirmation or an error.
     */
    @POST
    @Path("/attempt/{quizAttemptId}/log")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get the QuizDTO for a quiz attempt.")
    public Response logQuizSectionView(@Context final HttpServletRequest httpServletRequest,
                                       @PathParam("quizAttemptId") final Long quizAttemptId,
                                       @FormParam("sectionNumber") Integer sectionNumber) {
        try {
            if (sectionNumber == null) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "Missing sectionNumber.").toResponse();
            }
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            QuizAttemptDTO quizAttempt = getIncompleteQuizAttemptForUser(quizAttemptId, user);

            QuizAssignmentDTO assignment = checkQuizAssignmentNotCancelledOrOverdue(quizAttempt);

            Map<String, Object> eventDetails = ImmutableMap.of(
                QUIZ_ID_FKEY, quizAttempt.getQuizId(),
                QUIZ_ATTEMPT_FK, quizAttempt.getId(),
                QUIZ_ASSIGNMENT_FK, assignment != null ? assignment.getId().toString() : "FREE_ATTEMPT",
                ASSIGNMENT_DUEDATE_FK, assignment == null || assignment.getDueDate() == null ? "NO_DUE_DATE" : assignment.getDueDate(),
                QUIZ_SECTION, sectionNumber
            );

            getLogManager().logEvent(user, httpServletRequest, Constants.IsaacServerLogType.VIEW_QUIZ_SECTION, eventDetails);

            return Response.noContent().build();
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "This quiz assignment has been cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst logging quiz section view";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Record that a user has answered a question.
     *
     * @param request
     *            - the servlet request so we can find out if it is a known user.
     * @param questionId
     *            that you are attempting to answer.
     * @param jsonAnswer
     *            - answer body which will be parsed as a Choice and then converted to a ChoiceDTO.
     * @return Response containing a QuestionValidationResponse object or containing a SegueErrorResponse .
     */
    @POST
    @Path("/attempt/{quizAttemptId}/answer/{question_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Submit an answer to a question.",
        notes = "The answer must be the correct Choice subclass for the question with the provided ID.")
    public Response answerQuestion(@Context final HttpServletRequest request,
                                   @PathParam("quizAttemptId") final Long quizAttemptId,
                                   @PathParam("question_id") final String questionId,
                                   final String jsonAnswer) {
        if (null == jsonAnswer || jsonAnswer.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "No answer received.").toResponse();
        }
        if (null == questionId || questionId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Missing questionId.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            QuizAttemptDTO quizAttempt = getIncompleteQuizAttemptForUser(quizAttemptId, user);

            checkQuizAssignmentNotCancelledOrOverdue(quizAttempt);

            Content contentBasedOnId;
            try {
                contentBasedOnId = this.contentManager.getContentDOById(this.contentManager.getCurrentContentSHA(), questionId);
            } catch (ContentManagerException e1) {
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
                log.error(error.getErrorMessage(), e1);
                return error.toResponse();
            }

            Question question;
            if (contentBasedOnId instanceof Question) {
                question = (Question) contentBasedOnId;
            } else {
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND,
                    "No question object found for given id: " + questionId);
                log.warn(error.getErrorMessage());
                return error.toResponse();
            }

            String quizId = extractPageIdFromQuestionId(questionId);

            // Check the quiz this question is from is valid for this attempt.
            if (!quizId.equals(quizAttempt.getQuizId())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "This question is part of another quiz.").toResponse();
            }

            ChoiceDTO answerFromClientDTO = quizQuestionManager.convertJsonAnswerToChoice(jsonAnswer);

            QuestionValidationResponseDTO answer = quizQuestionManager.validateAnswer(question, answerFromClientDTO);

            quizQuestionManager.recordQuestionAttempt(quizAttempt, answer);

            this.getLogManager().logEvent(user, request, SegueServerLogType.ANSWER_QUIZ_QUESTION, answer);

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst submitting quiz answer";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "This quiz assignment has been cancelled.").toResponse();
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        }
    }

    /**
     * Abandon a started free quiz attempt.
     *
     * This can only be done on free quiz attempts, as we don't want any startDate shenanigans on assigned quizzes.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAttemptId
     *            - the ID of the quiz the user wishes to attempt.
     * @return Confirmation or an error.
     */
    @DELETE
    @Path("/attempt/{quizAttemptId}")
    @ApiOperation(value = "Abandon a started free quiz attempt.")
    public final Response abandonQuizAttempt(@Context final HttpServletRequest httpServletRequest,
                                             @PathParam("quizAttemptId") final Long quizAttemptId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (null == quizAttemptId) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz attempt id.").toResponse();
            }

            QuizAttemptDTO quizAttempt = this.quizAttemptManager.getById(quizAttemptId);

            if (!quizAttempt.getUserId().equals(user.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You cannot cancel a quiz attempt for someone else.").toResponse();
            }

            if (quizAttempt.getQuizAssignmentId() != null) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You can only cancel attempts on quizzes you chose to take.").toResponse();
            }

            if (quizAttempt.getCompletedDate() != null) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You cannot cancel completed quiz attempts.").toResponse();
            }

            this.quizAttemptManager.deleteAttempt(quizAttempt);

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst deleting a free quiz attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    /**
     * Allows a teacher to set a quiz to a group.
     *
     * Sends emails to the members of the group informing them of the quiz.
     * Takes a quiz id, a group id, an optional end datetime for completion, and the feedbackMode.
     *
     * @param request so that we can extract user information.
     * @param clientQuizAssignment the partially constructed quiz assignment
     *
     * @return the quiz assignment that has been created, or an error.
     */
    @POST
    @Path("/assignment")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Set a quiz to a group, with an optional due date.")
    public final Response createQuizAssignment(@Context final HttpServletRequest request,
                                               final QuizAssignmentDTO clientQuizAssignment) {

        if (   clientQuizAssignment.getQuizId() == null
            || clientQuizAssignment.getGroupId() == null
            || clientQuizAssignment.getQuizFeedbackMode() == null) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "A required field was missing. Must provide group and quiz ids and a quiz feedback mode.").toResponse();
        }

        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            if (!(isUserTeacherOrAbove(userManager, currentlyLoggedInUser))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            if (!canManageAssignment(clientQuizAssignment, currentlyLoggedInUser))
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You can only set assignments to groups you own or manage.").toResponse();

            IsaacQuizDTO quiz = this.quizManager.findQuiz(clientQuizAssignment.getQuizId());
            if (null == quiz) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The quiz id specified does not exist.")
                    .toResponse();
            }

            clientQuizAssignment.setOwnerUserId(currentlyLoggedInUser.getId());
            clientQuizAssignment.setCreationDate(null);
            clientQuizAssignment.setId(null);

            // modifies assignment passed in to include an id.
            QuizAssignmentDTO assignmentWithID = this.quizAssignmentManager.createAssignment(clientQuizAssignment);

            Map<String, Object> eventDetails = ImmutableMap.of(
                QUIZ_ID_FKEY, assignmentWithID.getQuizId(),
                GROUP_FK, assignmentWithID.getGroupId(),
                QUIZ_ASSIGNMENT_FK, assignmentWithID.getId(),
                ASSIGNMENT_DUEDATE_FK, assignmentWithID.getDueDate() == null ? "NO_DUE_DATE" : assignmentWithID.getDueDate()
            );

            this.getLogManager().logEvent(currentlyLoggedInUser, request, Constants.IsaacServerLogType.SET_NEW_QUIZ_ASSIGNMENT, eventDetails);

            List<QuizAssignmentDTO> assignments = Collections.singletonList(assignmentWithID);
            quizManager.augmentWithQuizSummary(assignments);

            return Response.ok(assignmentWithID).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (DuplicateAssignmentException|DueBeforeNowException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst setting quiz";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst setting quiz", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This quiz has become unavailable.");
        }
    }

    /**
     * Get quizzes assigned by this user.
     *
     * Optionally filtered by a particular group.
     *
     * @param groupIdOfInterest An optional ID of a group to look up.
     * @return a Response containing a list of QuizAssignmentDTO for the quizzes they have assigned.
     */
    @GET
    @Path("/assigned")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get quizzes assigned by this user.")
    public Response getQuizAssignments(@Context HttpServletRequest request,
                                       @QueryParam("groupId") Long groupIdOfInterest) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            List<UserGroupDTO> groups;
            if (groupIdOfInterest != null) {
                UserGroupDTO group = this.groupManager.getGroupById(groupIdOfInterest);

                if (null == group) {
                    return new SegueErrorResponse(Status.NOT_FOUND, "The group specified cannot be located.")
                        .toResponse();
                }

                if (!canManageGroup(user, group)) {
                    return new SegueErrorResponse(Status.FORBIDDEN, "You are not the owner or manager of that group").toResponse();
                }

                groups = Collections.singletonList(group);
            } else {
                groups = this.groupManager.getAllGroupsOwnedAndManagedByUser(user, false);
            }
            List<QuizAssignmentDTO> assignments = this.quizAssignmentManager.getAssignmentsForGroups(groups);

            quizManager.augmentWithQuizSummary(assignments);

            return Response.ok(assignments)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst getting assigned quizzes";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    /**
     * Exports a CSV of quiz results.
     */
    @GET
    @Path("/assignment/{quizAssignmentId}/download")
    @Produces(MediaType.WILDCARD)
    @GZIP
    @ApiOperation(value = "Export quiz results as CSV.")
    public final Response getQuizAssignmentCSV(@Context final HttpServletRequest httpServletRequest,
                                               @PathParam("quizAssignmentId") Long quizAssignmentId,
                                               @QueryParam("format") final String formatMode) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);

            UserGroupDTO group = quizAssignmentManager.getGroupForAssignment(assignment);

            if (!canManageGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You can only view assignments to groups you own or manage.").toResponse();
            }

            this.assignmentService.augmentAssignerSummaries(Collections.singletonList(assignment));

            IsaacQuizDTO quiz = quizManager.findQuiz(assignment.getQuizId());

            List<RegisteredUserDTO> groupMembers = this.groupManager.getUsersInGroup(group);

            StringWriter stringWriter = new StringWriter();
            List<String[]> rows = Lists.newArrayList();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            StringBuilder headerBuilder = new StringBuilder();
            if (null != formatMode && formatMode.toLowerCase().equals("excel")) {
                headerBuilder.append("\uFEFF");  // UTF-8 Byte Order Marker
            }
            headerBuilder.append(String.format("Quiz (%s) Results: Downloaded on %s \nGenerated by: %s %s \n\n",
                    quiz.getId(), new Date(), user.getGivenName(), user.getFamilyName()));

            List<QuestionDTO> questions = GameManager.getAllMarkableQuestionPartsDFSOrder(quiz);
            List<String> headerRow = Lists.newArrayList(Arrays.asList("", ""));
            for (QuestionDTO question : questions) {
                headerRow.add(question.getTitle());
            }
            rows.add(headerRow.toArray(new String[0]));

            for (RegisteredUserDTO groupMember : groupMembers) {
                if (!associationManager.hasPermission(user, groupMember)) continue; // Maybe add a row anyway?

                QuizAttemptDTO quizAttempt = quizAttemptManager.getByQuizAssignmentAndUser(assignment, groupMember);
                if (null == quizAttempt) continue; // Maybe add a row anyway?

                List<String> row = Lists.newArrayList();
                row.add(groupMember.getFamilyName());
                row.add(groupMember.getGivenName());

                Map<QuestionDTO, QuestionValidationResponse> answersMap = quizQuestionManager.getAnswerMap(quiz, quizAttempt, true);
                for (QuestionDTO question : questions) {
                    // TODO: This needs a special serialization method that produces a concise version of the answer.
                    row.add(answersMap.get(question).getAnswer().toString());
                }
                Collections.addAll(rows, row.toArray(new String[0]));
            }

            csvWriter.writeAll(rows);
            csvWriter.close();
            headerBuilder.append(stringWriter);

            return Response.ok(headerBuilder.toString())
                    .header("Content-Disposition", "attachment; filename=quiz_results.csv")
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "This assignment has been cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst viewing quiz assignment";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst viewing quiz assignment", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This quiz has become unavailable.");
        } catch (IOException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while building the CSV file.").toResponse();
        }
    }

    /**
     * Allows a teacher to see results of an assignment
     *
     * @param httpServletRequest so that we can extract user information.
     * @param quizAssignmentId the id of the assignment to view.
     *
     * @return Details about the assignment.
     */
    @GET
    @Path("/assignment/{quizAssignmentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "View a quiz assignment.")
    public final Response getQuizAssignment(@Context final HttpServletRequest httpServletRequest,
                                            @PathParam("quizAssignmentId") Long quizAssignmentId) {

        if (null == quizAssignmentId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz assignment id.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);

            UserGroupDTO group = quizAssignmentManager.getGroupForAssignment(assignment);

            if (!canManageGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You can only view assignments to groups you own or manage.").toResponse();
            }

            this.assignmentService.augmentAssignerSummaries(Collections.singletonList(assignment));

            IsaacQuizDTO quiz = quizManager.findQuiz(assignment.getQuizId());

            List<RegisteredUserDTO> groupMembers = this.groupManager.getUsersInGroup(group);

            Map<RegisteredUserDTO, QuizFeedbackDTO> feedbackMap = quizQuestionManager.getAssignmentTeacherFeedback(quiz, assignment, groupMembers);

            List<QuizUserFeedbackDTO> userFeedback = new ArrayList<>();

            for (Map.Entry<RegisteredUserDTO, QuizFeedbackDTO> feedback : feedbackMap.entrySet()) {
                UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(user,
                    userManager.convertToUserSummaryObject(feedback.getKey()));

                userFeedback.add(new QuizUserFeedbackDTO(userSummary,
                    userSummary.isAuthorisedFullAccess() ? feedback.getValue() : null));
            }

            assignment.setUserFeedback(userFeedback);
            assignment.setQuiz(quiz);

            return Response.ok(assignment)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "This assignment has been cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst viewing quiz assignment";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst viewing quiz assignment", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This quiz has become unavailable.");
        }
    }

    /**
     * Allows a teacher to see a specific user's attempt to an assignment.
     *
     * @param httpServletRequest so that we can extract user information.
     * @param quizAssignmentId the id of the assignment.
     * @param userId the id of the user to get the answers for.
     *
     * @return Details about the user's attempt.
     */
    @GET
    @Path("/assignment/{quizAssignmentId}/attempt/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "View a quiz assignment attempt.")
    public final Response getQuizAssignmentAttempt(@Context final HttpServletRequest httpServletRequest,
                                                   @PathParam("quizAssignmentId") Long quizAssignmentId,
                                                   @PathParam("userId") Long userId) {

        if (null == quizAssignmentId || null == userId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz assignment and user id id.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);

            UserGroupDTO group = quizAssignmentManager.getGroupForAssignment(assignment);

            if (!canManageGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You can only view assignments to groups you own or manage.").toResponse();
            }

            RegisteredUserDTO student = this.userManager.getUserDTOById(userId);

            if (!groupManager.isUserInGroup(student, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "That student is not in the group that was assigned this quiz.").toResponse();
            }

            if (!associationManager.hasPermission(user, student)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You do not have access to that student's data.").toResponse();
            }

            QuizAttemptDTO quizAttempt = quizAttemptManager.getByQuizAssignmentAndUser(assignment, student);

            if (quizAttempt == null || quizAttempt.getCompletedDate() == null) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "That student has not completed this quiz assignment.").toResponse();
            }

            quizAttempt = augmentAttempt(quizAttempt, assignment, true);

            return Response.ok(quizAttempt)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                .build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "This assignment has been cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst viewing quiz assignment attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst viewing quiz assignment attempt", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This quiz has become unavailable.");
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "That user does not exist.").toResponse();
        }
    }

    /**
     * Allows a teacher to update a quiz assignment.
     *
     * Only changes to the feedbackMode field are accepted. A bad request error will be thrown if any other fields are set.
     *
     * @param httpServletRequest so that we can extract user information.
     * @param quizAssignmentId the id of the assignment to update.
     * @param clientQuizAssignment a partial object with new values for the quiz assignment (only feedbackMode may be set.)
     *
     * @return Confirmation or error.
     */
    @POST
    @Path("/assignment/{quizAssignmentId}")
    @ApiOperation(value = "Update a quiz assignment (only feedbackMode may be updated).")
    public final Response updateQuizAssignment(@Context final HttpServletRequest httpServletRequest,
                                               @PathParam("quizAssignmentId") Long quizAssignmentId,
                                               final QuizAssignmentDTO clientQuizAssignment) {

        if (null == quizAssignmentId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz assignment id.").toResponse();
        }

        if (   clientQuizAssignment.getId() != null
            || clientQuizAssignment.getQuizId() != null
            || clientQuizAssignment.getGroupId() != null
            || clientQuizAssignment.getOwnerUserId() != null
            || clientQuizAssignment.getCreationDate() != null
            || clientQuizAssignment.getDueDate() != null)
        {
            log.warn("Attempt to change fields for quiz assignment id {} that aren't feedbackMode: {}", quizAssignmentId, clientQuizAssignment);
            return new SegueErrorResponse(Status.BAD_REQUEST, "Those fields are not editable.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);

            if (!canManageAssignment(assignment, user)) return new SegueErrorResponse(Status.FORBIDDEN,
                "You can only updates assignments to groups you own or manage.").toResponse();

            quizAssignmentManager.updateAssignment(assignment, clientQuizAssignment);

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "This assignment is already cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst updating quiz assignment";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    /**
     * Allows a teacher to cancel a quiz they have set.
     *
     * @param httpServletRequest so that we can extract user information.
     * @param quizAssignmentId the id of the assignment to cancel.
     *
     * @return Confirmation or error.
     */
    @DELETE
    @Path("/assignment/{quizAssignmentId}")
    @ApiOperation(value = "Cancel a quiz assignment.")
    public final Response cancelQuizAssignment(@Context final HttpServletRequest httpServletRequest,
                                               @PathParam("quizAssignmentId") Long quizAssignmentId) {

        if (null == quizAssignmentId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz assignment id.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);

            if (!canManageAssignment(assignment, user)) return new SegueErrorResponse(Status.FORBIDDEN,
                "You can only cancel assignments to groups you own or manage.").toResponse();

            quizAssignmentManager.cancelAssignment(assignment);

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "This assignment is already cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst cancelling quiz assignment";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    @Nullable
    private QuizAssignmentDTO checkQuizAssignmentNotCancelledOrOverdue(QuizAttemptDTO quizAttempt) throws SegueDatabaseException, AssignmentCancelledException, ErrorResponseWrapper {
        // Relying on the side-effects of getting the assignment.
        QuizAssignmentDTO quizAssignment = getQuizAssignment(quizAttempt);

        if (quizAssignment != null && quizAssignment.getDueDate() != null && quizAssignment.getDueDate().before(new Date())) {
            throw new ErrorResponseWrapper(new SegueErrorResponse(Status.FORBIDDEN, "The due date for this quiz has passed."));
        }

        return quizAssignment;
    }

    @Nullable
    private QuizAssignmentDTO getQuizAssignment(QuizAttemptDTO quizAttempt) throws SegueDatabaseException, AssignmentCancelledException {
        if (quizAttempt.getQuizAssignmentId() != null) {
            return quizAssignmentManager.getById(quizAttempt.getQuizAssignmentId());
        }
        return null;
    }

    @SuppressWarnings( "deprecation" )
    private QuizAttemptDTO getIncompleteQuizAttemptForUser(Long quizAttemptId, RegisteredUserDTO user) throws SegueDatabaseException, ErrorResponseWrapper {
        QuizAttemptDTO quizAttempt = getQuizAttemptForUser(quizAttemptId, user);

        if (quizAttempt.getCompletedDate() != null) {
            throw new ErrorResponseWrapper(new SegueErrorResponse(Status.FORBIDDEN, "You have completed this quiz."));
        }
        return quizAttempt;
    }

    @SuppressWarnings( "deprecation" )
    private QuizAttemptDTO getCompleteQuizAttemptForUser(Long quizAttemptId, RegisteredUserDTO user) throws SegueDatabaseException, ErrorResponseWrapper {
        QuizAttemptDTO quizAttempt = getQuizAttemptForUser(quizAttemptId, user);

        if (quizAttempt.getCompletedDate() == null) {
            throw new ErrorResponseWrapper(new SegueErrorResponse(Status.FORBIDDEN, "You have not completed this quiz."));
        }
        return quizAttempt;
    }

    @DoNotCall
    @Deprecated
    private QuizAttemptDTO getQuizAttemptForUser(Long quizAttemptId, RegisteredUserDTO user) throws ErrorResponseWrapper, SegueDatabaseException {
        QuizAttemptDTO quizAttempt = this.getQuizAttempt(quizAttemptId);

        if (!quizAttempt.getUserId().equals(user.getId())) {
            throw new ErrorResponseWrapper(new SegueErrorResponse(Status.FORBIDDEN, "This is not your quiz attempt."));
        }
        return quizAttempt;
    }

    private QuizAttemptDTO getQuizAttempt(Long quizAttemptId) throws ErrorResponseWrapper, SegueDatabaseException {
        if (null == quizAttemptId) {
            throw new ErrorResponseWrapper(new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid quiz attempt id."));
        }

        return this.quizAttemptManager.getById(quizAttemptId);
    }

    private boolean canManageAssignment(QuizAssignmentDTO clientQuizAssignment, RegisteredUserDTO currentlyLoggedInUser) throws SegueDatabaseException, NoUserLoggedInException {
        UserGroupDTO assigneeGroup = quizAssignmentManager.getGroupForAssignment(clientQuizAssignment);

        return canManageGroup(currentlyLoggedInUser, assigneeGroup);
    }

    private boolean canManageGroup(RegisteredUserDTO currentlyLoggedInUser, UserGroupDTO assigneeGroup) throws NoUserLoggedInException {
        return GroupManager.isOwnerOrAdditionalManager(assigneeGroup, currentlyLoggedInUser.getId())
            || isUserAnAdmin(userManager, currentlyLoggedInUser);
    }

    private QuizAttemptDTO augmentAttempt(QuizAttemptDTO quizAttempt, @Nullable QuizAssignmentDTO quizAssignment, boolean includeCorrect) throws ContentManagerException, SegueDatabaseException {
        IsaacQuizDTO quiz = quizManager.findQuiz(quizAttempt.getQuizId());
        quiz = quizQuestionManager.augmentQuestionsForUser(quiz, quizAttempt, includeCorrect);
        quizAttempt.setQuiz(quiz);

        return augmentAttempt(quizAttempt, quizAssignment);
    }

    private QuizAttemptDTO augmentAttempt(QuizAttemptDTO quizAttempt, @Nullable QuizAssignmentDTO quizAssignment) throws SegueDatabaseException {
        if (quizAssignment != null) {
            this.assignmentService.augmentAssignerSummaries(Collections.singletonList(quizAssignment));
            quizAttempt.setQuizAssignment(quizAssignment);
        }

        return quizAttempt;
    }
}
