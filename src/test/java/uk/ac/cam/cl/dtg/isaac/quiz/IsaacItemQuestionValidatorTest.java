/*
 * Copyright 2019 James Sharkey
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
package uk.ac.cam.cl.dtg.isaac.quiz;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacItemQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Item;
import uk.ac.cam.cl.dtg.segue.dos.content.ItemChoice;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the Item Question Validator class.
 *
 */
@PowerMockIgnore({"javax.ws.*"})
public class IsaacItemQuestionValidatorTest {
    private IsaacItemQuestionValidator validator;
    private IsaacItemQuestion someItemQuestion;
    private String incorrectExplanation = "EXPLANATION";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Initial configuration of tests.
     */
    @Before
    public final void setUp() {
        validator = new IsaacItemQuestionValidator();

        // Set up the question object:
        someItemQuestion = new IsaacItemQuestion();

        List<Choice> answerList = Lists.newArrayList();
        ItemChoice someIncorrectChoice = new ItemChoice();
        ItemChoice someCorrectAnswer = new ItemChoice();
        Item item1 = new Item("id001", "A");
        Item item2 = new Item("id002", "B");
        Item item3 = new Item("id003", "C");
        someItemQuestion.setItems(ImmutableList.of(item1, item2, item3));

        // Correct and incorrect choices the same:
        someCorrectAnswer.setItems(ImmutableList.of(item1, item3));
        someCorrectAnswer.setCorrect(true);
        someIncorrectChoice.setItems(ImmutableList.of(item1, item2));
        someIncorrectChoice.setCorrect(false);
        someIncorrectChoice.setExplanation(new Content(incorrectExplanation));

        // Add both choices to question, incorrect first:
        answerList.add(someIncorrectChoice);
        answerList.add(someCorrectAnswer);
        someItemQuestion.setChoices(answerList);
    }

    /*
       Test that correct answers are recognised.
    */
    @Test
    public final void isaacItemQuestionValidator_CorrectItems_CorrectResponseShouldBeReturned() {
        // Set up user answer:
        ItemChoice c = new ItemChoice();
        Item submittedItem1 = new Item("id001", null);
        Item submittedItem3 = new Item("id003", null);
        c.setItems(ImmutableList.of(submittedItem1, submittedItem3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someItemQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that exact correct answers are recognised even if the choice is subset match.
    */
    @Test
    public final void isaacItemQuestionValidator_CorrectItemsSubsetMatchEnabled_CorrectResponseShouldBeReturned() {

        // Enable subset matching on the second choice in the list (the correct one)
        ((ItemChoice) someItemQuestion.getChoices().get(1)).setAllowSubsetMatch(true);

        // Set up user answer:
        ItemChoice c = new ItemChoice();
        Item submittedItem1 = new Item("id001", null);
        Item submittedItem3 = new Item("id003", null);
        c.setItems(ImmutableList.of(submittedItem1, submittedItem3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someItemQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that correct answers are recognised in any order.
    */
    @Test
    public final void isaacItemQuestionValidator_CorrectItemsUnordered_CorrectResponseShouldBeReturned() {
        // Set up user answer:
        ItemChoice c = new ItemChoice();
        Item submittedItem1 = new Item("id001", null);
        Item submittedItem3 = new Item("id003", null);
        c.setItems(ImmutableList.of(submittedItem3, submittedItem1));  // Reverse order, shouldn't matter!

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someItemQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that correct choices take precedence over incorrect choices.
    */
    @Test
    public final void isaacItemQuestionValidator_CorrectChoicePrecedence_CorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacItemQuestion itemQuestion = new IsaacItemQuestion();

        List<Choice> answerList = Lists.newArrayList();
        ItemChoice someIncorrectChoice = new ItemChoice();
        ItemChoice someCorrectAnswer = new ItemChoice();
        Item item1 = new Item("id001", "A");
        Item item2 = new Item("id002", "B");
        Item item3 = new Item("id003", "C");
        itemQuestion.setItems(ImmutableList.of(item1, item2, item3));
        List<Item> itemsForChoices = ImmutableList.of(item1, item3);

        // Correct and incorrect choices the same:
        someCorrectAnswer.setItems(itemsForChoices);
        someCorrectAnswer.setCorrect(true);
        someIncorrectChoice.setItems(itemsForChoices);
        someIncorrectChoice.setCorrect(false);

        // Add both choices to question, incorrect first:
        answerList.add(someIncorrectChoice);
        answerList.add(someCorrectAnswer);
        itemQuestion.setChoices(answerList);

        // Set up user answer:
        ItemChoice c = new ItemChoice();
        Item submittedItem1 = new Item("id001", null);
        Item submittedItem3 = new Item("id003", null);
        c.setItems(ImmutableList.of(submittedItem1, submittedItem3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(itemQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that correct subset choices take precedence over incorrect subset ones.
    */
    @Test
    public final void isaacItemQuestionValidator_CorrectChoiceSubsetPrecedence_CorrectResponseShouldBeReturned() {

        // Enable subset matching on both of the choices in the list
        ((ItemChoice) someItemQuestion.getChoices().get(0)).setAllowSubsetMatch(true);
        ((ItemChoice) someItemQuestion.getChoices().get(1)).setAllowSubsetMatch(true);

        // Set up user answer that is a subset of both the correct and incorrect choice
        ItemChoice c = new ItemChoice();
        Item submittedItem1 = new Item("id001", null);
        Item submittedItem2 = new Item("id002", null);
        Item submittedItem3 = new Item("id003", null);
        c.setItems(ImmutableList.of(submittedItem1, submittedItem2, submittedItem3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someItemQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that if the correct choice is a strict subset of the submitted answer, it is marked as correct,
       if the correct choice is marked as subset match.
    */
    @Test
    public final void isaacItemQuestionValidator_CorrectChoiceSubsetOfSubmitted_CorrectResponseShouldBeReturned() {

        // Enable subset matching on the correct choice
        ((ItemChoice) someItemQuestion.getChoices().get(1)).setAllowSubsetMatch(true);

        // Set up user answer that is a strict superset of the correct choice
        ItemChoice c = new ItemChoice();
        Item submittedItem1 = new Item("id001", null);
        Item submittedItem2 = new Item("id002", null);
        Item submittedItem3 = new Item("id003", null);
        c.setItems(ImmutableList.of(submittedItem1, submittedItem2, submittedItem3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someItemQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that incorrect strict choices take precedence over correct subset ones.
    */
    @Test
    public final void isaacItemQuestionValidator_IncorrectOverCorrectSubsetPrecedence_IncorrectResponseShouldBeReturned() {

        IsaacItemQuestion itemQuestion = new IsaacItemQuestion();

        List<Choice> answerList = Lists.newArrayList();
        ItemChoice someIncorrectChoice = new ItemChoice();
        ItemChoice someCorrectAnswer = new ItemChoice();
        Item item1 = new Item("id001", "A");
        Item item2 = new Item("id002", "B");
        Item item3 = new Item("id003", "C");
        itemQuestion.setItems(ImmutableList.of(item1, item2, item3));

        // Correct choice is subset match, incorrect one is not
        someCorrectAnswer.setItems(ImmutableList.of(item1, item2, item3));
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setAllowSubsetMatch(true);
        someIncorrectChoice.setItems(ImmutableList.of(item1, item3));
        someIncorrectChoice.setCorrect(false);
        someCorrectAnswer.setAllowSubsetMatch(false);

        // Add both choices to question, correct (with subset match enabled) first:
        answerList.add(someCorrectAnswer);
        answerList.add(someIncorrectChoice);
        itemQuestion.setChoices(answerList);

        // Set up user answer that is the same as the incorrect answer, and is a subset of the correct answer
        ItemChoice c = new ItemChoice();
        Item submittedItem1 = new Item("id001", null);
        Item submittedItem3 = new Item("id003", null);
        c.setItems(ImmutableList.of(submittedItem1, submittedItem3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(itemQuestion, c);
        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    /*
       Test that if the submitted answer is a strict subset of the correct choice, it is marked as incorrect,
       if the correct choice is marked as subset match.
    */
    @Test
    public final void isaacItemQuestionValidator_SubmittedSubsetOfCorrectChoice_CorrectResponseShouldBeReturned() {

        // Set up the question object:
        IsaacItemQuestion itemQuestion = new IsaacItemQuestion();

        ItemChoice subsetCorrectAnswer = new ItemChoice();
        Item item1 = new Item("id001", "A");
        Item item2 = new Item("id002", "B");
        Item item3 = new Item("id003", "C");
        itemQuestion.setItems(ImmutableList.of(item1, item2, item3));

        // Set up the subset match choice
        subsetCorrectAnswer.setItems(ImmutableList.of(item1, item2));
        subsetCorrectAnswer.setAllowSubsetMatch(true);
        subsetCorrectAnswer.setCorrect(true);

        itemQuestion.setChoices(ImmutableList.of(subsetCorrectAnswer));

        // Set up user answer as a subset of the correct choice items
        ItemChoice userAnswer = new ItemChoice();
        Item submittedItem1 = new Item("id001", null);
        userAnswer.setItems(ImmutableList.of(submittedItem1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(itemQuestion, userAnswer);
        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    /*
     Test that invalid answers are recognised.
    */
    @Test
    public final void isaacItemQuestionValidator_InvalidItems_ErrorResponseShouldBeReturned() {
        // Set up invalid user answer:
        ItemChoice c = new ItemChoice();
        Item submittedItem1 = new Item("id005", null);
        c.setItems(ImmutableList.of(submittedItem1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someItemQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("unrecognised items"));
    }

    /*
     Test that no items being submitted gets custom feedback.
    */
    @Test
    public final void isaacItemQuestionValidator_NoItemsSubmitted_IncorrectResponseShouldBeReturned() {
        // Set up invalid user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(null);

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someItemQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("did not provide an answer"));
    }

    /*
     Test that incorrect answers are incorrect.
    */
    @Test
    public final void isaacItemQuestionValidator_IncorrectAnswer_IncorrectResponseShouldBeReturned() {
        // Set up user answer:
        ItemChoice c = new ItemChoice();
        Item submittedItem1 = new Item("id001", null);
        c.setItems(ImmutableList.of(submittedItem1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someItemQuestion, c);
        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    /*
       Test that incorrect choices can be matched.
    */
    @Test
    public final void isaacItemQuestionValidator_IncorrectChoiceMatch_MatchedResponseShouldBeReturned() {
        // Set up user answer:
        ItemChoice c = new ItemChoice();
        Item submittedItem1 = new Item("id001", null);
        Item submittedItem2 = new Item("id002", null);
        c.setItems(ImmutableList.of(submittedItem1, submittedItem2));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someItemQuestion, c);
        assertFalse(response.isCorrect());
        assertEquals(response.getExplanation().getValue(), incorrectExplanation);
    }

    //  ---------- Tests from here test invalid questions themselves ----------

    /*
     Test that missing choices are detected.
    */
    @Test
    public final void isaacItemQuestionValidator_NoChoices_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacItemQuestion itemQuestion = new IsaacItemQuestion();

        Item item1 = new Item("id001", "A");
        Item item2 = new Item("id002", "B");
        Item item3 = new Item("id003", "C");
        itemQuestion.setItems(ImmutableList.of(item1, item2, item3));

        itemQuestion.setChoices(Lists.newArrayList());

        // Set up user answer:
        ItemChoice c = new ItemChoice();
        Item submittedItem1 = new Item("id001", null);
        c.setItems(ImmutableList.of(submittedItem1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(itemQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("not have any correct answers"));
    }

    /*
     Test that missing items are detected.
    */
    @Test
    public final void isaacItemQuestionValidator_NoItems_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacItemQuestion itemQuestion = new IsaacItemQuestion();

        List<Choice> answerList = Lists.newArrayList();
        Item item1 = new Item("id001", "A");
        Item item2 = new Item("id002", "B");
        Item item3 = new Item("id003", "C");
        // Don't set items on the question object itself.

        ItemChoice someCorrectAnswer = new ItemChoice();
        someCorrectAnswer.setItems(ImmutableList.of(item1, item2, item3));
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);
        itemQuestion.setChoices(answerList);

        // Set up correct user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1, item2, item3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(itemQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("not have any items to choose from"));
    }

    /*
     Test that choices missing items are detected.
    */
    @Test
    public final void isaacItemQuestionValidator_NoItemsInChoice_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacItemQuestion itemQuestion = new IsaacItemQuestion();

        List<Choice> answerList = Lists.newArrayList();
        ItemChoice someCorrectAnswer = new ItemChoice();
        Item item1 = new Item("id001", "A");
        Item item2 = new Item("id002", "B");
        Item item3 = new Item("id003", "C");
        itemQuestion.setItems(ImmutableList.of(item1, item2, item3));

        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);
        itemQuestion.setChoices(answerList);

        // Set up correct user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1, item2, item3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(itemQuestion, c);
        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    /*
     Test that incorrect choice types in question are detected.
    */
    @Test
    public final void isaacItemQuestionValidator_WrongChoiceType_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacItemQuestion itemQuestion = new IsaacItemQuestion();

        List<Choice> answerList = Lists.newArrayList();
        Item item1 = new Item("id001", "A");
        Item item2 = new Item("id002", "B");
        Item item3 = new Item("id003", "C");
        itemQuestion.setItems(ImmutableList.of(item1, item2, item3));

        Choice someCorrectAnswer = new Choice();
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);
        itemQuestion.setChoices(answerList);

        // Set up correct user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1, item2, item3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(itemQuestion, c);
        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    /*
     Test that incorrect question types are detected.
    */
    @Test
    public final void isaacItemQuestionValidator_WrongQuestionType_ExceptionShouldBeThrown() {
        IsaacQuickQuestion invalidQuestionType = new IsaacQuickQuestion();
        invalidQuestionType.setId("invalidQuestionType");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("only works with IsaacItemQuestions");

        // This should throw an exception:
        validator.validateQuestionResponse(invalidQuestionType, new ItemChoice());
    }

    /*
     Test that incorrect submitted choice types are detected.
    */
    @Test
    public final void isaacItemQuestionValidator_WrongChoiceType_ExceptionShouldBeThrown() {
        IsaacItemQuestion itemQuestion = new IsaacItemQuestion();
        itemQuestion.setId("invalidQuestionType");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Expected ItemChoice for IsaacItemQuestion");

        // This should throw an exception:
        validator.validateQuestionResponse(itemQuestion, new Choice());
    }
}