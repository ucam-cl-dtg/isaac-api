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
package uk.ac.cam.cl.dtg.segue.quiz;

import java.util.Date;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;

/**
 * Default quiz validator for ChoiceQuestions.
 * 
 * This relies on the annotation {@link ValidatesWith} being used.
 */
public class ChoiceQuestionValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(ChoiceQuestionValidator.class);

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, 
            final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        ChoiceQuestion choiceQuestion;
        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong
        // check that the question is of type ChoiceQuestion before we go ahead
        if (question instanceof ChoiceQuestion) {
            choiceQuestion = (ChoiceQuestion) question;

            if (null == choiceQuestion.getChoices() || choiceQuestion.getChoices().isEmpty()) {
                log.warn("Question does not have any answers. " + question.getId() + " src: "
                        + question.getCanonicalSourceFile());
                return new QuestionValidationResponse(question.getId(), answer, false, null, new Date());
            }

            for (Choice choice : choiceQuestion.getChoices()) {
                if (choice.getValue().equals(answer.getValue())) {
                    responseCorrect = choice.isCorrect();
                    feedback = (Content) choice.getExplanation();
                    break;
                }
            }

            log.info("Unable to find choice for question ( " + question.getId() + " ) matching the answer supplied ("
                    + answer + "). Returning that it is incorrect without an explanation.");

            // If we still have no feedback to give, use the question's default feedback if any to use:
            if (feedbackIsNullOrEmpty(feedback) && null != choiceQuestion.getDefaultFeedback()) {
                feedback = choiceQuestion.getDefaultFeedback();
            }

            return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
        } else {
            log.error("Expected to be able to cast the question as a ChoiceQuestion " + "but this cast failed.");
            throw new ClassCastException("Incorrect type of question received. Unable to validate.");
        }
    }
}
