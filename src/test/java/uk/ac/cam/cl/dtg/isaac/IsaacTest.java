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
package uk.ac.cam.cl.dtg.isaac;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizManager;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.GroupMembershipDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryWithEmailAddressDTO;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createPartialMockForAllMethodsExcept;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;
import static org.powermock.api.easymock.PowerMock.verify;

public class IsaacTest {
    protected static Date somePastDate = new Date(System.currentTimeMillis() - 7*24*60*60*1000);
    protected static Date someFurtherPastDate = new Date(System.currentTimeMillis() - 14*24*60*60*1000);
    protected static Date someFutureDate = new Date(System.currentTimeMillis() + 7*24*60*60*1000);
    protected IsaacQuizDTO studentQuiz;
    protected IsaacQuiz studentQuizDO;
    protected IsaacQuizDTO teacherQuiz;
    protected IsaacQuizDTO otherQuiz;
    protected IsaacQuizSectionDTO quizSection1;
    protected IsaacQuizSectionDTO quizSection2;

    protected ContentSummaryDTO studentQuizSummary;
    protected ContentSummaryDTO teacherQuizSummary;
    protected RegisteredUserDTO student;
    protected RegisteredUserDTO teacher;
    protected RegisteredUserDTO secondTeacher;
    protected RegisteredUserDTO otherTeacher;
    protected RegisteredUserDTO noone;
    protected RegisteredUserDTO secondStudent;
    protected RegisteredUserDTO otherStudent;
    protected RegisteredUserDTO adminUser;
    protected List<RegisteredUserDTO> everyone;
    protected UserGroupDTO studentGroup;
    protected UserGroupDTO studentInactiveGroup;

    protected QuizAssignmentDTO studentAssignment;
    protected QuizAssignmentDTO overdueAssignment;
    private QuizAssignmentDTO completedAssignment;
    protected ImmutableList<Long> studentGroups;
    protected List<QuizAssignmentDTO> teacherAssignmentsToTheirGroups;
    private QuizAssignmentDTO otherAssignment;
    protected QuizAssignmentDTO studentInactiveIgnoredAssignment;
    protected QuizAssignmentDTO studentInactiveAssignment;
    protected List<QuizAssignmentDTO> studentAssignments;
    protected QuizAttemptDTO studentAttempt;
    protected QuizAttemptDTO overdueAttempt;
    protected QuizAttemptDTO completedAttempt;
    protected QuizAttemptDTO overdueCompletedAttempt;
    protected QuizAttemptDTO otherAttempt;
    protected QuizAttemptDTO ownAttempt;
    protected QuizAttemptDTO ownCompletedAttempt;
    protected QuizAttemptDTO attemptOnNullFeedbackModeQuiz;
    protected List<QuizAttemptDTO> studentAttempts;
    protected IsaacQuestionBaseDTO question;
    protected IsaacQuestionBaseDTO question2;
    protected IsaacQuestionBaseDTO question3;
    protected IsaacQuestionBase questionDO;
    protected IsaacQuestionBaseDTO questionPageQuestion;
    protected IsaacQuestionBase questionPageQuestionDO;

    protected GroupManager groupManager;
    protected QuizManager quizManager;

    protected Map<Object, MockConfigurer> defaultsMap = new HashMap<>();

    @Before
    public final void initializeIsaacTest() throws SegueDatabaseException, ContentManagerException {
        initializeIsaacObjects();
        initializeMocks();
    }

    protected void initializeIsaacObjects() {
        long id = 0L;
        studentQuizSummary = new ContentSummaryDTO();
        teacherQuizSummary = new ContentSummaryDTO();

        questionPageQuestion = new IsaacQuestionBaseDTO();
        questionPageQuestion.setId("questionPage|question1");
        questionPageQuestionDO = new IsaacQuestionBase();
        questionPageQuestionDO.setId(questionPageQuestion.getId());

        question = new IsaacQuestionBaseDTO();
        question.setId("studentQuiz|section1|question1");

        question2 = new IsaacQuestionBaseDTO();
        question2.setId("studentQuiz|section2|question2");

        question3 = new IsaacQuestionBaseDTO();
        question3.setId("studentQuiz|section2|question3");

        questionDO = new IsaacQuestionBase();
        questionDO.setId(question.getId());

        quizSection1 = new IsaacQuizSectionDTO();
        quizSection1.setId("studentQuiz|section1");
        quizSection1.setChildren(Collections.singletonList(question));

        quizSection2 = new IsaacQuizSectionDTO();
        quizSection2.setId("studentQuiz|section2");
        quizSection2.setChildren(ImmutableList.of(question2, question3));

        studentQuiz = new IsaacQuizDTO("studentQuiz", null, null, null, null, null, null, null, ImmutableList.of(quizSection1, quizSection2), null, null, null, false, null, null, true, QuizFeedbackMode.OVERALL_MARK, null);
        teacherQuiz = new IsaacQuizDTO("teacherQuiz", null, null, null, null, null, null, null, null, null, null, null, false, null, null, false, null, null);
        otherQuiz = new IsaacQuizDTO("otherQuiz", null, null, null, null, null, null, null, Collections.singletonList(quizSection1), null, null, null, false, null, null, true, QuizFeedbackMode.DETAILED_FEEDBACK, null);

        // A bit scrappy, but hopefully sufficient.
        studentQuizDO = new IsaacQuiz("studentQuiz", null, null, null, null, null, null, null, null, null, null, null, false, null, null, true, null);

        student = new RegisteredUserDTO("Some", "Student", "test-student@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "");
        student.setRole(Role.STUDENT);
        student.setId(++id);

        teacher = new RegisteredUserDTO("Some", "Teacher", "test-teacher@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE, somePastDate, "");
        teacher.setRole(Role.TEACHER);
        teacher.setId(++id);

        secondTeacher = new RegisteredUserDTO("Second", "Teacher", "second-teacher@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.PREFER_NOT_TO_SAY, somePastDate, "");
        secondTeacher.setRole(Role.TEACHER);
        secondTeacher.setId(++id);

        otherTeacher = new RegisteredUserDTO("Other", "Teacher", "other-teacher@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.OTHER, somePastDate, "");
        otherTeacher.setRole(Role.TEACHER);
        otherTeacher.setId(++id);

        noone = null;

        secondStudent = new RegisteredUserDTO("Second", "Student", "second-student@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE, somePastDate, "");
        secondStudent.setRole(Role.STUDENT);
        secondStudent.setId(++id);

        otherStudent = new RegisteredUserDTO("Other", "Student", "other-student@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "");
        otherStudent.setRole(Role.STUDENT);
        otherStudent.setId(++id);

        adminUser = new RegisteredUserDTO("Test", "Admin", "test-admin@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.UNKNOWN, somePastDate, "");
        adminUser.setRole(Role.ADMIN);
        adminUser.setId(++id);

        everyone = anyOf(student, teacher, secondTeacher, otherTeacher, secondStudent, otherStudent, adminUser);

        studentGroup = new UserGroupDTO();
        studentGroup.setId(++id);
        studentGroup.setOwnerId(teacher.getId());
        UserSummaryWithEmailAddressDTO secondTeacherSummary = new UserSummaryWithEmailAddressDTO();
        secondTeacherSummary.setId(secondTeacher.getId());
        secondTeacherSummary.setEmail(secondTeacher.getEmail());
        secondTeacherSummary.setEmailVerificationStatus(secondTeacher.getEmailVerificationStatus());
        secondTeacherSummary.setRole(secondTeacher.getRole());
        secondTeacherSummary.setFamilyName(secondTeacher.getFamilyName());
        secondTeacherSummary.setGivenName(secondTeacher.getGivenName());

        studentGroup.setAdditionalManagers(Collections.singleton(secondTeacherSummary));

        studentInactiveGroup = new UserGroupDTO(++id, "studentInactiveGroup", teacher.getId(), somePastDate, somePastDate, false);

        studentGroups = ImmutableList.of(studentGroup.getId(), studentInactiveGroup.getId());

        completedAssignment = new QuizAssignmentDTO(++id, studentQuiz.getId(), teacher.getId(), studentGroup.getId(), someFurtherPastDate, somePastDate, QuizFeedbackMode.OVERALL_MARK);
        studentAssignment = new QuizAssignmentDTO(++id, studentQuiz.getId(), teacher.getId(), studentGroup.getId(), somePastDate, someFutureDate, QuizFeedbackMode.DETAILED_FEEDBACK);
        overdueAssignment = new QuizAssignmentDTO(++id, studentQuiz.getId(), teacher.getId(), studentGroup.getId(), someFurtherPastDate, somePastDate, QuizFeedbackMode.SECTION_MARKS);
        otherAssignment = new QuizAssignmentDTO(++id, teacherQuiz.getId(), teacher.getId(), studentGroup.getId(), somePastDate, someFutureDate, QuizFeedbackMode.OVERALL_MARK);

        studentInactiveIgnoredAssignment = new QuizAssignmentDTO(++id, teacherQuiz.getId(), teacher.getId(), studentInactiveGroup.getId(), somePastDate, someFutureDate, QuizFeedbackMode.OVERALL_MARK);
        studentInactiveAssignment = new QuizAssignmentDTO(++id, teacherQuiz.getId(), teacher.getId(), studentInactiveGroup.getId(), someFurtherPastDate, someFutureDate, QuizFeedbackMode.OVERALL_MARK);

        studentAssignments = ImmutableList.of(completedAssignment, studentAssignment, overdueAssignment, otherAssignment, studentInactiveAssignment);

        teacherAssignmentsToTheirGroups = ImmutableList.<QuizAssignmentDTO>builder().addAll(studentAssignments).add(studentInactiveIgnoredAssignment).build();

        studentAttempt = new QuizAttemptDTO(++id, student.getId(), studentQuiz.getId(), studentAssignment.getId(), somePastDate, null);
        overdueAttempt = new QuizAttemptDTO(++id, student.getId(), studentQuiz.getId(), overdueAssignment.getId(), somePastDate, null);
        completedAttempt = new QuizAttemptDTO(++id, student.getId(), studentQuiz.getId(), studentAssignment.getId(), somePastDate, new Date());
        overdueCompletedAttempt = new QuizAttemptDTO(++id, student.getId(), studentQuiz.getId(), overdueAssignment.getId(), somePastDate, new Date());
        otherAttempt = new QuizAttemptDTO(++id, student.getId(), teacherQuiz.getId(), otherAssignment.getId(), somePastDate, null);

        ownCompletedAttempt = new QuizAttemptDTO(++id, student.getId(), otherQuiz.getId(), null, somePastDate, somePastDate);
        ownAttempt = new QuizAttemptDTO(++id, student.getId(), otherQuiz.getId(), null, somePastDate, null);
        attemptOnNullFeedbackModeQuiz = new QuizAttemptDTO(101L, student.getId(), teacherQuiz.getId(), null, somePastDate, somePastDate);

        studentAttempts = ImmutableList.of(studentAttempt, overdueAttempt, completedAttempt, overdueCompletedAttempt, otherAttempt, ownAttempt, ownCompletedAttempt, attemptOnNullFeedbackModeQuiz);
    }

    protected void initializeMocks() throws ContentManagerException, SegueDatabaseException {
        quizManager = createMock(QuizManager.class);

        registerDefaultsFor(quizManager, m -> {
            expect(m.getAvailableQuizzes(true, null, null)).andStubReturn(wrap(studentQuizSummary));
            expect(m.getAvailableQuizzes(false, null, null)).andStubReturn(wrap(studentQuizSummary, teacherQuizSummary));
            expect(m.findQuiz(studentQuiz.getId())).andStubReturn(studentQuiz);
            expect(m.findQuiz(teacherQuiz.getId())).andStubReturn(teacherQuiz);
            expect(m.findQuiz(otherQuiz.getId())).andStubReturn(otherQuiz);
            expect(m.extractSectionObjects(studentQuiz)).andStubReturn(ImmutableList.of(quizSection1, quizSection2));
        });

        groupManager = createPartialMockForAllMethodsExcept(GroupManager.class, "filterItemsBasedOnMembershipContext");
        expect(groupManager.getGroupById(anyLong())).andStubAnswer(() -> {
            Object[] arguments = getCurrentArguments();
            if (arguments[0] == studentGroup.getId()) {
                return studentGroup;
            } else {
                throw new SegueDatabaseException("No such group.");
            }
        });
        expect(groupManager.isUserInGroup(anyObject(), anyObject())).andStubAnswer(() -> {
            Object[] arguments = getCurrentArguments();
            if ((arguments[0] == student) && (arguments[1] == studentGroup || arguments[1] == studentInactiveGroup)) {
                return true;
            } else if (arguments[0] == secondStudent && arguments[1] == studentGroup) {
                return true;
            } else {
                return false;
            }
        });
        expect(groupManager.getGroupMembershipList(student, false)).andStubReturn(ImmutableList.of(studentGroup, studentInactiveGroup));
        expect(groupManager.getGroupMembershipList(secondStudent, false)).andStubReturn(ImmutableList.of(studentGroup));
        expect(groupManager.getUserMembershipMapForGroup(studentGroup.getId())).andStubReturn(
            ImmutableMap.of(
                student.getId(), new GroupMembershipDTO(studentGroup.getId(), student.getId(), GroupMembershipStatus.ACTIVE, null, somePastDate),
                secondStudent.getId(), new GroupMembershipDTO(studentGroup.getId(), secondStudent.getId(), GroupMembershipStatus.ACTIVE, null, somePastDate)
            )
        );
        Date beforeSomePastDate = new Date(somePastDate.getTime() - 1000L);
        expect(groupManager.getUserMembershipMapForGroup(studentInactiveGroup.getId())).andStubReturn(
            Collections.singletonMap(student.getId(), new GroupMembershipDTO(studentGroup.getId(), student.getId(), GroupMembershipStatus.INACTIVE, null, beforeSomePastDate))
        );
        expect(groupManager.getUsersInGroup(studentGroup)).andStubReturn(ImmutableList.of(student, secondStudent));

        replay(quizManager, groupManager);
    }


    protected List<RegisteredUserDTO> anyOf(RegisteredUserDTO... users) {
        return Arrays.asList(users);
    }

    protected List<RegisteredUserDTO> studentsTeachersOrAdmin() {
        return anyOf(teacher, secondTeacher, adminUser);
    }

    @SafeVarargs
    protected final <T> ResultsWrapper<T> wrap(T... items) {
        return new ResultsWrapper<>(Arrays.asList(items), (long) items.length);
    }

    /**
     * If a mock needs to have some expectations as well as stub returns, put the stub return setup in a call to this function.
     */
    protected <T> void registerDefaultsFor(T mock, MockConfigurer<T> defaults) {
        defaultsMap.put(mock, defaults);
        defaults.configure(mock);
    }

    @SafeVarargs
    protected final <T> void withMock(T mock, MockConfigurer<T>... setups) {
        verify(mock);
        reset(mock);
        if (defaultsMap.containsKey(mock)) {
            ((MockConfigurer<T>) defaultsMap.get(mock)).configure(mock);
        }
        for (MockConfigurer<T> setup: setups) {
            setup.configure(mock);
        }
        replay(mock);
    }

    /**
     * MockConfigurer
     *
     * Basically a Consumer that can throw.
     *
     * @param <M> What it configures.
     */
    @FunctionalInterface
    public interface MockConfigurer<M> {
        void accept(M mock) throws Exception;

        default void configure(M mock) {
            try {
                accept(mock);
            } catch (Exception e) {
                throw new RuntimeException("Error configuring defaults for mock: " + mock.toString(), e);
            }
        }
    }
}
