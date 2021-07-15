/**
 * Copyright 2021 Chris Purdy
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
import uk.ac.cam.cl.dtg.isaac.dos.IsaacRegexMatchQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dos.content.RegexPattern;
import uk.ac.cam.cl.dtg.segue.dos.content.StringChoice;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Validator that only provides functionality to validate Regex String Match questions.
 */
public class IsaacRegexMatchValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacRegexMatchValidator.class);
    
    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacRegexMatchQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac Regex Match Questions... (%s is not regex string match)",
                    question.getId()));
        }

        if (!(answer instanceof StringChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected StringChoice for IsaacRegexMatchQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        StringChoice userAnswer = (StringChoice) answer;

        IsaacRegexMatchQuestion regexMatchQuestion = (IsaacRegexMatchQuestion) question;

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        if (null == regexMatchQuestion.getChoices() || regexMatchQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            feedback = new Content("This question does not have any correct answers");
        }

        // STEP 1: Did they provide an answer at all?

        if (null == feedback && (null == userAnswer.getValue() || userAnswer.getValue().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        // STEP 2: If they did, does their answer match a known answer?

        if (null == feedback) {

            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(regexMatchQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are of the RegexPattern type, ...
                if (!(c instanceof RegexPattern)) {
                    log.error("Isaac RegexMatch Validator for questionId: " + regexMatchQuestion.getId()
                            + " expected there to be a RegexPattern. Instead it found a Choice.");
                    continue;
                }
                RegexPattern regexPattern = (RegexPattern) c;

                if (null == regexPattern.getValue() || regexPattern.getValue().isEmpty()) {
                    log.error("Expected a regex pattern to match on, but none found in choice for question id: "
                            + regexMatchQuestion.getId());
                    continue;
                }

                // ... check if they match the pattern, ...
                if (matchesPattern(regexPattern.getValue(), userAnswer.getValue(), regexPattern.isCaseInsensitive(),
                        regexPattern.isMultiLineRegex(), regexPattern.isMatchWholeString())) {
                    // ... and break at the first matched pattern.
                    feedback = (Content) regexPattern.getExplanation();
                    responseCorrect = regexPattern.isCorrect();
                    break;
                }
            }
        }

        return new QuestionValidationResponse(question.getId(), userAnswer, responseCorrect, feedback, new Date());
    }

    private boolean matchesPattern(String trustedRegexPattern, String userValue, final Boolean caseInsensitive,
                                   final Boolean multiLineRegex, final Boolean matchWholeString) {

        if (null == trustedRegexPattern || null == userValue) {
            return false;
        }

        // The pattern is case sensitive and single line by default - the regex flags are combined with bitwise OR
        Pattern answerPattern = Pattern.compile(trustedRegexPattern,
                (null != caseInsensitive && caseInsensitive ? Pattern.CASE_INSENSITIVE : 0)
                        | (null != multiLineRegex && multiLineRegex ? Pattern.MULTILINE : 0));

        // Try to match entire answer by default
        if (null == matchWholeString || !matchWholeString) {
            return answerPattern.matcher(userValue).find();
        } else {
            return answerPattern.matcher(userValue).matches();
        }
    }
}
