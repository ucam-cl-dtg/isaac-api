/*
 * Copyright 2016 Alistair Stead, James Sharkey, Ian Davies
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

import com.google.api.client.util.Maps;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicLogicQuestion;
import uk.ac.cam.cl.dtg.segue.dos.FormulaValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.LogicFormula;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatorUnavailableException;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Validator that provides functionality to validate symbolic logic questions.
 *
 */
public class IsaacSymbolicLogicValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacSymbolicLogicValidator.class);

    private enum MatchType {
        NONE,
        SYMBOLIC,
        EXACT
    }

    private final String hostname;
    private final String port;
    private final String externalValidatorUrl;

    public IsaacSymbolicLogicValidator(final String hostname, final String port) {
        this.hostname = hostname;
        this.port = port;
        this.externalValidatorUrl = "http://" + this.hostname + ":" + this.port + "/check/logic";
    }

    @Override
    public QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer)
            throws ValidatorUnavailableException {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacSymbolicLogicQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac Symbolic Questions... (%s is not symbolic)",
                    question.getId()));
        }
        
        if (!(answer instanceof LogicFormula)) {
            throw new IllegalArgumentException(String.format(
                    "Expected LogicFormula for IsaacSymbolicLogicQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        IsaacSymbolicLogicQuestion symbolicLogicQuestion = (IsaacSymbolicLogicQuestion) question;
        LogicFormula submittedLogicFormula = (LogicFormula) answer;

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        MatchType responseMatchType = MatchType.NONE;   // The match type we found
        boolean responseCorrect = false;                // Whether we're right or wrong


        // There are several specific responses the user can receive. Each of them will set feedback content, so
        // use that to decide whether to proceed to the next check in each case.

        // STEP 0: Do we even have any answers for this question? Always do this check, because we know we
        //         won't have feedback yet.

        if (null == symbolicLogicQuestion.getChoices() || symbolicLogicQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            feedback = new Content("This question does not have any correct answers");
        }

        // STEP 1: Did they provide an answer?

        if (null == feedback && (null == submittedLogicFormula.getPythonExpression() || submittedLogicFormula.getPythonExpression().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        // STEP 2: Otherwise, Does their answer match a choice exactly?

        if (null == feedback) {

            // For all the choices on this question...
            for (Choice c : symbolicLogicQuestion.getChoices()) {

                // ... that are of the LogicFormula type, ...
                if (!(c instanceof LogicFormula)) {
                    log.error("Validator for questionId: " + symbolicLogicQuestion.getId()
                            + " expected there to be a LogicFormula. Instead it found a Choice.");
                    continue;
                }

                LogicFormula logicFormulaChoice = (LogicFormula) c;

                // ... and that have a python expression ...
                if (null == logicFormulaChoice.getPythonExpression() || logicFormulaChoice.getPythonExpression().isEmpty()) {
                    log.error("Expected python expression, but none found in choice for question id: "
                            + symbolicLogicQuestion.getId());
                    continue;
                }

                // ... look for an exact string match to the submitted answer.
                if (logicFormulaChoice.getPythonExpression().equals(submittedLogicFormula.getPythonExpression())) {
                    feedback = (Content) logicFormulaChoice.getExplanation();
                    responseMatchType = MatchType.EXACT;
                    responseCorrect = logicFormulaChoice.isCorrect();
                }
            }
        }

        // STEP 3: Otherwise, use the symbolic checker to analyse their answer

        if (null == feedback) {

            // Go through all choices, keeping track of the best match we've seen so far. A symbolic match terminates
            // this loop immediately. A numeric match may later be replaced with a symbolic match, but otherwise will suffice.

            LogicFormula closestMatch = null;
            MatchType closestMatchType = MatchType.NONE;

            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(symbolicLogicQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are of the LogicFormula type, ...
                if (!(c instanceof LogicFormula)) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                LogicFormula logicFormulaChoice = (LogicFormula) c;

                // ... and that have a python expression ...
                if (null == logicFormulaChoice.getPythonExpression() || logicFormulaChoice.getPythonExpression().isEmpty()) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                // ... test their answer against this choice with the symbolic checker.

                // We don't do any sanitisation of user input here, we'll leave that to the python.

                MatchType matchType = MatchType.NONE;

                try {
                    HashMap<String, String> req = Maps.newHashMap();
                    req.put("target", logicFormulaChoice.getPythonExpression());
                    req.put("test", submittedLogicFormula.getPythonExpression());
                    req.put("description", symbolicLogicQuestion.getId());
                    if (symbolicLogicQuestion.getAvailableSymbols() != null) {
                        req.put("symbols", String.join(",", symbolicLogicQuestion.getAvailableSymbols()));
                    }

                    HashMap<String, Object> response = getResponseFromExternalValidator(externalValidatorUrl, req);

                    if (response.containsKey("error")) {
                        if (response.containsKey("code")) {
                            log.error("Failed to check logic formula \"" + submittedLogicFormula.getPythonExpression()
                                    + "\" against \"" + logicFormulaChoice.getPythonExpression() + "\": " + response.get("error"));
                        } else if (response.containsKey("syntax_error")) {
                            // There's a syntax error in the "test" expression, no use checking it further:
                            closestMatch = null;
                            feedback = new Content("Your answer does not seem to be valid boolean logic.<br>"
                                        + "Check for things like mismatched brackets or misplaced symbols.");
                            feedback.setTags(new HashSet<>(Collections.singletonList("syntax_error")));
                            responseCorrect = false;
                            break;
                        } else {
                            log.warn("Problem checking logic formula \"" + submittedLogicFormula.getPythonExpression()
                                    + "\" for (" + symbolicLogicQuestion.getId() + ") with symbolic checker: " + response.get("error"));
                        }
                    } else {
                        if (response.get("equal").equals("true")) {
                            matchType = MatchType.valueOf(((String) response.get("equality_type")).toUpperCase());
                        }
                    }

                } catch (IOException e) {
                    log.error("Failed to check formula with symbolic checker. Is the server running? Not trying again.");
                    throw new ValidatorUnavailableException("We are having problems marking Logic Questions."
                            + " Please try again later!");
                }

                if (matchType == MatchType.EXACT) {
                    closestMatch = logicFormulaChoice;
                    closestMatchType = MatchType.EXACT;
                    break;
                } else if (matchType.compareTo(closestMatchType) > 0) {
                    if (logicFormulaChoice.getRequiresExactMatch() && logicFormulaChoice.isCorrect()) {
                        closestMatch = logicFormulaChoice;
                        closestMatchType = matchType;
                    } else {
                        if (closestMatch == null || !closestMatch.getRequiresExactMatch()) {
                            closestMatch = logicFormulaChoice;
                            closestMatchType = matchType;
                        } else {
                            // This is not as good a match as the one we already have.
                        }
                    }
                }
            }

            if (null != closestMatch) {
                // We found a decent match. Of course, it still might be wrong.

                if (closestMatchType != MatchType.EXACT && closestMatch.getRequiresExactMatch()) {
                    if (closestMatch.isCorrect()) {
                        feedback = new Content("Your answer is not in the form we expected. Can you rearrange or simplify it?");
                        feedback.setTags(new HashSet<>(Collections.singletonList("required_exact")));
                        responseCorrect = false;
                        responseMatchType = closestMatchType;

                        log.info("User submitted an answer that was close to an exact match, but not exact "
                                + "for question " + symbolicLogicQuestion.getId() + ". Choice: "
                                + closestMatch.getPythonExpression() + ", submitted: "
                                + submittedLogicFormula.getPythonExpression());
                    } else {
                        // This is weak match to a wrong answer; we can't use the feedback for the choice.
                    }
                } else {
                    feedback = (Content) closestMatch.getExplanation();
                    responseCorrect = closestMatch.isCorrect();
                    responseMatchType = closestMatchType;
                }

            }
        }

        // STEP 4: If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != symbolicLogicQuestion.getDefaultFeedback()) {
            feedback = symbolicLogicQuestion.getDefaultFeedback();
        }

        // If we got this far and feedback is still null, they were wrong. There's no useful feedback we can give at this point.

        return new FormulaValidationResponse(symbolicLogicQuestion.getId(), answer, feedback, responseCorrect, responseMatchType.toString(), new Date());
    }

}
