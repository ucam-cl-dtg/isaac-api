/*
 * Copyright 2016 Ian Davies, James Sharkey, Ryan Lau
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
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicChemistryQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.ChemicalFormula;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatorUnavailableException;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Validator that only provides functionality to validate symbolic chemistry questions.
 *
 */
public class IsaacSymbolicChemistryValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacSymbolicChemistryValidator.class);

    /**
     * Describes the level of equivalence between two mhchem expressions.
     */
    private enum MatchType {
        NONE,
        WEAK0,
        WEAK1,
        WEAK2,
        WEAK3,
        EXACT
    }

    private final String hostname;
    private final String port;
    private final String externalValidatorUrl;

    public IsaacSymbolicChemistryValidator(final String hostname, final String port) {
        this.hostname = hostname;
        this.port = port;
        this.externalValidatorUrl = "http://" + this.hostname + ":" + this.port + "/check";
    }

    @Override
    public QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) throws ValidatorUnavailableException {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacSymbolicChemistryQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac Symbolic Chemistry Questions... "
                            + "(%s is not symbolic chemistry)",
                    question.getId()));
        }
        
        if (!(answer instanceof ChemicalFormula)) {
            throw new IllegalArgumentException(String.format(
                    "Expected ChemicalFormula for IsaacSymbolicQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        IsaacSymbolicChemistryQuestion chemistryQuestion = (IsaacSymbolicChemistryQuestion) question;
        ChemicalFormula submittedFormula = (ChemicalFormula) answer;

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        boolean allTypeMismatch = true;                 // Whether type of answer matches one of the correct answers
        boolean allChemistry = true, allNuclear = true;
        boolean allEquation = true, allExpression = true;
        boolean containsError = false;                  // Whether student answer contains any error terms.
        boolean isEquation = false;                     // Whether student answer is equation or not.
        boolean isBalanced = false;                     // Whether student answer has balanced equation.
        boolean isNuclear = false;                      // Whether student answer has nuclear terms.
        boolean isValid = false;                        // Whether student answer has valid atomic numbers.

        String receivedType;                            // Type of student answer.

        // STEP 0: Do we even have any answers for this question? Always do this check, because we know we
        //         won't have feedback yet.

        if (null == chemistryQuestion.getChoices() || chemistryQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            feedback = new Content("This question does not have any correct answers");
        }

        // STEP 1: Did they provide an answer?

        if (null == feedback && (null == submittedFormula.getMhchemExpression()
                || submittedFormula.getMhchemExpression().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        // STEP 2: Otherwise, Does their answer match a choice exactly?

        if (null == feedback) {

            // For all the choices on this question...
            for (Choice c : chemistryQuestion.getChoices()) {

                // ... that are of the ChemicalFormula type, ...
                if (!(c instanceof ChemicalFormula)) {
                    log.error("Isaac Symbolic Chemistry Validator for questionId: " + chemistryQuestion.getId()
                            + " expected there to be a ChemicalFormula. Instead it found a Choice.");
                    continue;
                }

                ChemicalFormula formulaChoice = (ChemicalFormula) c;

                // ... and that have a mhchem expression ...
                if (null == formulaChoice.getMhchemExpression() || formulaChoice.getMhchemExpression().isEmpty()) {
                    log.error("Expected python expression, but none found in choice for question id: "
                            + chemistryQuestion.getId());
                    continue;
                }

                // ... look for an exact string match to the submitted answer (lazy).
                if (formulaChoice.getMhchemExpression().equals(submittedFormula.getMhchemExpression())) {
                    feedback = (Content) formulaChoice.getExplanation();
                    responseCorrect = formulaChoice.isCorrect();
                }
            }
        }

        // STEP 3: Otherwise, use the symbolic checker to analyse their answer

        if (feedback == null) {

            // Go through all choices, keeping track of the best match we've seen so far. A symbolic match terminates
            // this loop immediately.

            ChemicalFormula closestMatch = null;
            HashMap<String, Object> closestResponse = null;
            IsaacSymbolicChemistryValidator.MatchType closestMatchType = IsaacSymbolicChemistryValidator.MatchType.NONE;
            boolean typeKnownFlag = false;
            boolean validityKnownFlag = false;
            boolean balancedKnownFlag = false;

            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(chemistryQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are of the ChemicalFormula type, ...
                if (!(c instanceof ChemicalFormula)) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                ChemicalFormula formulaChoice = (ChemicalFormula) c;

                // ... and that have a mhchem expression ...
                if (null == formulaChoice.getMhchemExpression() || formulaChoice.getMhchemExpression().isEmpty()) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                // ... test their answer against this choice with the symbolic checker.

                IsaacSymbolicChemistryValidator.MatchType matchType;
                HashMap<String, Object> response;

                try {

                    // Pass some JSON to a REST endpoint and get some JSON back.
                    HashMap<String, String> req = Maps.newHashMap();
                    req.put("target", formulaChoice.getMhchemExpression());
                    req.put("test", submittedFormula.getMhchemExpression());
                    req.put("description", chemistryQuestion.getId());

                    response = getResponseFromExternalValidator(externalValidatorUrl, req);

                    if (response.containsKey("error")) {

                        // If it doesn't contain a code, it wasn't a fatal error in the checker; probably only a
                        // problem with the submitted answer.
                        log.warn("Problem checking formula \"" + submittedFormula.getMhchemExpression()
                                + "\" with symbolic chemistry checker: " + response.get("error"));
                        break;

                    } else if (response.get("containsError").equals(true)) {

                        // Contains error term in expression: Cannot be matched with any terms.
                        containsError = true;
                        break;

                    }

                    if (c.isCorrect()) {

                        // Check if type mismatch occurred, when choice is correct answer.
                        allTypeMismatch = allTypeMismatch && response.get("typeMismatch").equals(true);

                        String expectedType = (String) response.get("expectedType");

                        allChemistry = allChemistry && !expectedType.contains("nuclear");
                        allNuclear = allNuclear && expectedType.contains("nuclear");

                        allExpression = allExpression && expectedType.contains("expression");
                        allEquation = allEquation && expectedType.contains("equation");

                    }

                    // Identify the type of student answer.
                    if (!typeKnownFlag) {

                        receivedType = (String) response.get("receivedType");
                        isEquation = receivedType.contains("equation");
                        isNuclear = receivedType.contains("nuclear");
                        typeKnownFlag = true;

                    }

                    // Check if equation is balanced, given that choice is of type equation.
                    if (!balancedKnownFlag && isEquation && response.get("typeMismatch").equals(false)) {

                        // Check if equation (physical/chemical) is balanced.
                        isBalanced = response.get("isBalanced").equals(true);
                        balancedKnownFlag = true;

                    }

                    // Check if equation is valid, given that choice is of type nuclear.
                    if (!validityKnownFlag && isNuclear && response.get("typeMismatch").equals(false)) {

                        // Check if nuclear (equation/expression) has valid atomic numbers.
                        isValid = response.get("validAtomicNumber").equals(true);
                        validityKnownFlag = true;
                    }


                    if (response.get("equal").equals(true)) {

                        // Input is semantically equivalent to correct answer.
                        matchType = MatchType.EXACT;

                    } else if (response.get("expectedType").equals("equation") || response.get("expectedType").equals("expression")) {
                        if (response.get("weaklyEquivalent").equals(false)) {
                            // This is not a match.
                            continue;
                        }
                        // Strength of match, increasing from 0.
                        int counter = 0;
                        if (response.get("sameState").equals(true)) {
                            counter++;
                        }
                        if (response.get("sameCoefficient").equals(true)) {
                            counter++;
                        }
                        if (response.get("expectedType").equals("equation") && response.get("sameArrow").equals(true)) {
                            counter++;
                        }
                        matchType = MatchType.valueOf("WEAK" + counter);
                    } else {

                        // Response & Answer have type NuclearEquation or NuclearExpression.
                        if (response.get("weaklyEquivalent").equals(false)) {
                            // This is not a match
                            continue;
                        }

                        // Measure the 'weakness' level. (0 is the weakest)
                        int counter = 0;
                        // FIXME: Nuclear Equations and Expressions don't have 'sameCoefficient' property?!
                        // So ignore this for now!
                        // if (response.get("sameCoefficient").equals(true)) {
                        //     counter++;
                        // }

                        matchType = MatchType.valueOf("WEAK" + counter);

                    }

                } catch (IOException e) {
                    log.error("Failed to check formula with chemistry checker. Is the server running? Not trying again.");
                    throw new ValidatorUnavailableException("We are having problems marking Chemistry Questions."
                            + " Please try again later!");
                }

                if (matchType == IsaacSymbolicChemistryValidator.MatchType.EXACT) {

                    // Found an exact match with one of the choices!

                    closestMatch = formulaChoice;
                    closestMatchType = IsaacSymbolicChemistryValidator.MatchType.EXACT;
                    break;

                } else if (matchType.compareTo(closestMatchType) > 0) {

                    // Found a better partial match than current match.

                    if (formulaChoice.isCorrect() || closestMatch == null) {

                        // We have no current closest match, or this choice is actually correct.
                        // Have no other choice than accepting this as closest match right now.

                        closestMatch = formulaChoice;
                        closestResponse = response;
                        closestMatchType = matchType;

                    }

                    // Otherwise, input partially matches a wrong choice, or closestMatch is assigned already.
                    // The best thing to do here is to do nothing.
                }
            }

            // End of second choice matching

            // STEP 4: Decide on what response to give to user

            if (containsError) {

                // User input contains error terms.
                feedback = new Content("Your answer contains invalid syntax!");

            } else if (closestMatch != null && closestMatchType == MatchType.EXACT) {

                // There is an exact match to a choice.
                feedback = (Content) closestMatch.getExplanation();
                responseCorrect = closestMatch.isCorrect();

            } else if (isNuclear && allChemistry) {

                // Nuclear/Chemistry mismatch in all correct answers.
                feedback = new Content("This question is about Chemistry.");

            } else if (!isNuclear && allNuclear) {

                // Nuclear/Chemistry mismatch in all correct answers.
                feedback = new Content("This question is about Nuclear Physics.");

            } else if (!isEquation && allEquation) {

                // Equation/Expression mismatch in all correct answers.
                feedback = new Content("Your answer is an expression but we expected an equation.");

            } else if (isEquation && allExpression) {

                // Equation/Expression mismatch in all correct answers.
                feedback = new Content("Your answer is an equation but we expected an expression.");

            } else if (isEquation && balancedKnownFlag && !isBalanced) {

                // Input is an unbalanced equation.
                feedback = new Content("Your equation is unbalanced.");

            } else if (isNuclear && validityKnownFlag && !isValid) {

                // Input is nuclear, but atomic/mass numbers are invalid.
                feedback = new Content("Check your atomic/mass numbers!");

            } else if (closestMatch != null && closestMatch.isCorrect()) {

                // Weak match to a correct answer.

                if (!((String) closestResponse.get("expectedType")).contains("nuclear")
                        && closestResponse.get("sameState").equals(false)) {

                    // Wrong state symbols
                    feedback = new Content("Check your state symbols!");

                } else if (closestResponse.get("sameCoefficient").equals(false)) {

                    // Wrong coefficients
                    feedback = new Content("Check your coefficients!");

                } else {

                    // Wrong arrow
                    feedback = new Content("What type of reaction is this?");

                }
            }
        }

        // STEP 5: If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != chemistryQuestion.getDefaultFeedback()) {
            feedback = chemistryQuestion.getDefaultFeedback();
        }
        return new QuestionValidationResponse(chemistryQuestion.getId(), answer, responseCorrect, feedback, new Date());
    }

}
