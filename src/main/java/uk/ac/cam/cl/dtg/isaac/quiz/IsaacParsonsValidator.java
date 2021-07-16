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

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacParsonsQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Item;
import uk.ac.cam.cl.dtg.segue.dos.content.ParsonsChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.ParsonsItem;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator that only provides functionality to validate Parsons questions.
 */
public class IsaacParsonsValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacParsonsValidator.class);

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacParsonsQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with IsaacParsonsQuestions (%s is not ParsonsQuestion)", question.getId()));
        }

        if (!(answer instanceof ParsonsChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected ParsonsChoice for IsaacParsonsQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        IsaacParsonsQuestion parsonsQuestion = (IsaacParsonsQuestion) question;
        ParsonsChoice submittedChoice = (ParsonsChoice) answer;

        // STEP 0: Is it even possible to answer this question?

        if (null == parsonsQuestion.getChoices() || parsonsQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());
            feedback = new Content("This question does not have any correct answers");
        }

        if (null == parsonsQuestion.getItems() || parsonsQuestion.getItems().isEmpty()) {
            log.error("ItemQuestion does not have any items. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());
            feedback = new Content("This question does not have any items to choose from!");
        }

        // STEP 1: Did they provide a valid answer?

        if (null == feedback && (null == submittedChoice.getItems() || submittedChoice.getItems().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        Set<String> submittedItemIdSet = null;
        Set<String> allowedItemIds;
        if (null != submittedChoice.getItems() && null != parsonsQuestion.getItems()) {
            submittedItemIdSet = submittedChoice.getItems().stream().map(Item::getId).collect(Collectors.toSet());
            allowedItemIds = parsonsQuestion.getItems().stream().map(Item::getId).collect(Collectors.toSet());
            if (!allowedItemIds.containsAll(submittedItemIdSet)) {
                feedback = new Content("You did not provide a valid answer; it contained unrecognised items");
            }
        }

        // STEP 2: If they did, does their answer match a known answer?

        if (null == feedback && null != submittedItemIdSet) {
            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(parsonsQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are of the Formula type, ...
                if (!(c instanceof ParsonsChoice)) {
                    log.error(String.format(
                            "Validator for question (%s) expected there to be an ParsonsChoice. Instead it found a %s.",
                            parsonsQuestion.getId(), c.getClass().toString()));
                    continue;
                }

                ParsonsChoice parsonsChoice = (ParsonsChoice) c;

                // ... and that have a python expression ...
                if (null == parsonsChoice.getItems() || parsonsChoice.getItems().isEmpty()) {
                    log.error("Expected list of ParsonsItems, but none found in choice for question id: "
                            + parsonsQuestion.getId());
                    continue;
                }

                // ... look for a match to the submitted answer.
                if (parsonsChoice.getItems().size() != submittedChoice.getItems().size()) {
                    continue;
                }

                boolean itemTypeMismatch = false;
                List<String> submittedItemIds = new ArrayList<>();
                List<Integer> submittedItemIndentations = new ArrayList<>();
                List<String> choiceItemIds = new ArrayList<>();
                List<Integer> choiceItemIndentations = new ArrayList<>();

                // Run through the submitted items:
                for (Item item : submittedChoice.getItems()) {
                    if (!(item instanceof ParsonsItem)) {
                        throw new IllegalArgumentException("Expected ParsonsChoice to contain ParsonsItems!");
                    }
                    ParsonsItem parsonsItem = (ParsonsItem) item;
                    submittedItemIds.add(parsonsItem.getId());
                    submittedItemIndentations.add(parsonsItem.getIndentation());
                }
                // Run through the items in the question:
                for (Item item : parsonsChoice.getItems()) {
                    if (!(item instanceof ParsonsItem)) {
                        log.error("ParsonsChoice contained non-ParsonsItem. Skipping!");
                        itemTypeMismatch = true;
                        break;
                    }
                    ParsonsItem parsonsItem = (ParsonsItem) item;
                    choiceItemIds.add(parsonsItem.getId());
                    choiceItemIndentations.add(parsonsItem.getIndentation());
                }
                if (itemTypeMismatch) {
                    // A problem with the choice itself. Maybe another one will be a better match?
                    continue;
                }

                if (choiceItemIds.equals(submittedItemIds) && choiceItemIndentations.equals(submittedItemIndentations)) {
                    responseCorrect = parsonsChoice.isCorrect();
                    feedback = (Content) parsonsChoice.getExplanation();
                    break;
                }
            }
        }

        // STEP 3: If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != parsonsQuestion.getDefaultFeedback()) {
            feedback = parsonsQuestion.getDefaultFeedback();
        }

        return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
    }

}
