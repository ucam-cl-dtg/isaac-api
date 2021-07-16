/*
 * Copyright 2014 Stephen Cummins, 2017 James Sharkey
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

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Validator that only provides functionality to validate Numeric questions.
 */
public class IsaacNumericValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacNumericValidator.class);

    private static final String DEFAULT_VALIDATION_RESPONSE = "Check your working.";
    private static final String DEFAULT_WRONG_UNIT_VALIDATION_RESPONSE = "Check your units.";
    /* Many users are getting answers wrong solely because we don't allow their (unambiguous) syntax for 10^x. Be nicer!
       Allow spaces either side of the times and allow * x X × and \times as multiplication!
       Also allow ^ or ** for powers. Allow e or E. Allow optional brackets around the powers of 10.
       Extract exponent as either group <exp1> or <exp2> (the other will become '').

       Inputs of style "1x10^3" and of style "10^3" must be dealt with separately, since for the latter we need
       to add a "1" to the start so both can become "1e3" when replacing the 10 part.
     */
    private static final String PREFIXED_POWER_OF_TEN_REGEX = "[ ]?((\\*|x|X|×|\\\\times)[ ]?10(\\^|\\*\\*)|e|E)([({](?<exp1>-?[0-9]+)[)}]|(?<exp2>-?[0-9]+))";
    private static final String BARE_POWER_OF_TEN_REGEX = "^(10(\\^|\\*\\*))([({](?<exp1>-?[0-9]+)[)}]|(?<exp2>-?[0-9]+))$";
    private static final String INVALID_NEGATIVE_STANDARD_FORM = ".*?10-([0-9]+).*?";

    /**
     * A class to represent the significant figures a number has, noting if it is ambiguous and the range if so.
     */
    private class SigFigResult {
        boolean isAmbiguous;
        int sigFigsMin;
        int sigFigsMax;

        /**
         * Default constructor.
         *
         * @param isAmbiguous - whether the significant figures are ambiguous or not.
         * @param sigFigsMin  - the minimum number of sig figs the number could have
         * @param sigFigsMax  - the maximum number of sig fig the number could have, equal to min if not ambiguous.
         */
        SigFigResult(final boolean isAmbiguous, final int sigFigsMin, final int sigFigsMax) {
            this.isAmbiguous = isAmbiguous;
            this.sigFigsMin = sigFigsMin;
            this.sigFigsMax = sigFigsMax;
        }
    }

    @Override
    public final QuestionValidationResponse validateQuestionResponse(
            final Question question, final Choice answer) {

        if (!(question instanceof IsaacNumericQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac Numeric Questions... (%s is not numeric)", question.getId()));
        }

        if (!(answer instanceof Quantity)) {
            throw new IllegalArgumentException(String.format(
                    "Expected Quantity for IsaacNumericQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        IsaacNumericQuestion isaacNumericQuestion = (IsaacNumericQuestion) question;
        Quantity answerFromUser = (Quantity) answer;

        log.debug("Starting validation of '" + answerFromUser.getValue() + " " + answerFromUser.getUnits() + "' for '"
                + isaacNumericQuestion.getId() + "'");

        if (null == isaacNumericQuestion.getChoices() || isaacNumericQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            return new QuantityValidationResponse(question.getId(), answerFromUser, false,
                            new Content("This question does not have any correct answers"),
                            false, false, new Date());
        }

        if (isaacNumericQuestion.getSignificantFiguresMin() < 1 || isaacNumericQuestion.getSignificantFiguresMax() < 1
                || isaacNumericQuestion.getSignificantFiguresMax() < isaacNumericQuestion.getSignificantFiguresMin()) {
            log.error("Question has broken significant figure rules! " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            return new QuantityValidationResponse(question.getId(), answerFromUser, false,
                            new Content("This question cannot be answered correctly."),
                            false, false, new Date());
        }

        try {
            if (null == answerFromUser.getValue() || answerFromUser.getValue().isEmpty()) {
                return new QuantityValidationResponse(question.getId(), answerFromUser, false, new Content(
                        "You did not provide an answer."), false, false, new Date());
            } else if (null == answerFromUser.getUnits() && (isaacNumericQuestion.getRequireUnits())) {
                return new QuantityValidationResponse(question.getId(), answerFromUser, false, new Content(
                        "You did not provide any units."), null, false, new Date());
            }

            QuantityValidationResponse bestResponse;

            // Step 1 - exact (string based) matching first - handles case where editors enter two mathematically
            // equivalent known answers - won't check for sig figs.
            bestResponse = this.exactStringMatch(isaacNumericQuestion, answerFromUser);

            // Only return this if the answer is incorrect - as we don't know if the correct answers have always been
            // specified in the correct # of sig figs.
            if (bestResponse != null && !bestResponse.isCorrect()) {
                return useDefaultFeedbackIfNecessary(isaacNumericQuestion, bestResponse);
            }

            // Step 2 - then do correct answer numeric equivalence checking.
            if (isaacNumericQuestion.getRequireUnits()) {
                bestResponse = this.validateWithUnits(isaacNumericQuestion, answerFromUser);
            } else {
                bestResponse = this.validateWithoutUnits(isaacNumericQuestion, answerFromUser);
            }

            // If incorrect and we have not used the default validation response then go ahead and return it 
            // - this provides more helpful feedback than sig figs errors.
            if (!bestResponse.isCorrect() && bestResponse.getExplanation() != null
                    && !(DEFAULT_VALIDATION_RESPONSE.equals(bestResponse.getExplanation().getValue())
                    || DEFAULT_WRONG_UNIT_VALIDATION_RESPONSE
                    .equals(bestResponse.getExplanation().getValue()))) {
                return useDefaultFeedbackIfNecessary(isaacNumericQuestion, bestResponse);
            }

            // Step 3 - then do sig fig checking:
            if (tooFewSignificantFigures(answerFromUser.getValue(), isaacNumericQuestion.getSignificantFiguresMin())) {
                // If too few sig figs then give feedback about this.

                // If we have unit information available put it in our response.
                Boolean validUnits = null;
                if (isaacNumericQuestion.getRequireUnits()) {
                    // Whatever the current bestResponse is, it contains all we need to know about the units:
                    validUnits = bestResponse.getCorrectUnits();
                }
                // Our new bestResponse is about incorrect significant figures:
                Content sigFigResponse = new Content(DEFAULT_VALIDATION_RESPONSE);
                sigFigResponse.setTags(new HashSet<>(ImmutableList.of("sig_figs", "sig_figs_too_few")));
                bestResponse = new QuantityValidationResponse(question.getId(), answerFromUser, false, sigFigResponse,
                        false, validUnits, new Date());
            }
            if (tooManySignificantFigures(answerFromUser.getValue(), isaacNumericQuestion.getSignificantFiguresMax())
                    && bestResponse.isCorrect()) {
                // If (and only if) _correct_, but to too many sig figs, give feedback about this.

                // If we have unit information available put it in our response.
                Boolean validUnits = null;
                if (isaacNumericQuestion.getRequireUnits()) {
                    // Whatever the current bestResponse is, it contains all we need to know about the units:
                    validUnits = bestResponse.getCorrectUnits();
                }
                // Our new bestResponse is about incorrect significant figures:
                Content sigFigResponse = new Content(DEFAULT_VALIDATION_RESPONSE);
                sigFigResponse.setTags(new HashSet<>(ImmutableList.of("sig_figs", "sig_figs_too_many")));
                bestResponse = new QuantityValidationResponse(question.getId(), answerFromUser, false, sigFigResponse,
                        false, validUnits, new Date());
            }

            // And then return the bestResponse:
            log.debug("Finished validation: correct=" + bestResponse.isCorrect() + ", correctValue="
                    + bestResponse.getCorrectValue() + ", correctUnits=" + bestResponse.getCorrectUnits());
            return useDefaultFeedbackIfNecessary(isaacNumericQuestion, bestResponse);
        } catch (NumberFormatException e) {
            log.debug("Validation failed for '" + answerFromUser.getValue() + " " + answerFromUser.getUnits() + "': "
                    + "cannot parse as number!");

            String feedback = "The answer you provided is not a valid number.";
            if (answerFromUser.getValue().matches(INVALID_NEGATIVE_STANDARD_FORM)) {
                feedback += "<br>When writing standard form, you must include a `^` between the 10 and the exponent.";
            }
            return new QuantityValidationResponse(question.getId(), answerFromUser, false, new Content(feedback),
                    false, false, new Date());
        }
    }

    /**
     * Numerically validate the students answer ensuring that the correct unit value is specified.
     *
     * @param isaacNumericQuestion - question to validate.
     * @param answerFromUser       - answer from user
     * @return the validation response
     */
    private QuantityValidationResponse validateWithUnits(final IsaacNumericQuestion isaacNumericQuestion,
                                                         final Quantity answerFromUser) {
        log.debug("\t[validateWithUnits]");
        QuantityValidationResponse bestResponse = null;
        int sigFigsToValidateWith = numberOfSignificantFiguresToValidateWith(answerFromUser.getValue(),
                isaacNumericQuestion.getSignificantFiguresMin(), isaacNumericQuestion.getSignificantFiguresMax());

        String unitsFromUser = answerFromUser.getUnits().trim();

        List<Choice> orderedChoices = getOrderedChoices(isaacNumericQuestion.getChoices());
        for (Choice c : orderedChoices) {
            if (c instanceof Quantity) {
                Quantity quantityFromQuestion = (Quantity) c;

                if (quantityFromQuestion.getUnits() == null) {
                    log.error("Expected units and no units can be found for question id: " + isaacNumericQuestion.getId());
                    continue;
                }

                String unitsFromChoice = quantityFromQuestion.getUnits().trim();
                String quantityFromChoice = quantityFromQuestion.getValue().trim();

                boolean numericValuesMatched = numericValuesMatch(quantityFromChoice, answerFromUser.getValue(),
                        sigFigsToValidateWith);

                // What sort of match do we have:
                if (numericValuesMatched && unitsFromUser.equals(unitsFromChoice)) {
                    // Exact match: nothing else can do better.
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            quantityFromQuestion.isCorrect(), (Content) quantityFromQuestion.getExplanation(),
                            quantityFromQuestion.isCorrect(), quantityFromQuestion.isCorrect(), new Date());
                    break;
                } else if (numericValuesMatched && !unitsFromUser.equals(unitsFromChoice) && quantityFromQuestion.isCorrect()) {
                    // Matches value but not units of a correct choice.
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            false, new Content(DEFAULT_WRONG_UNIT_VALIDATION_RESPONSE), true, false, new Date());
                } else if (!numericValuesMatched && unitsFromUser.equals(unitsFromChoice) && quantityFromQuestion.isCorrect()) {
                    // Matches units but not value of a correct choice.
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            false, new Content(DEFAULT_VALIDATION_RESPONSE), false, true, new Date());
                }
            } else {
                log.error("Isaac Numeric Validator for questionId: " + isaacNumericQuestion.getId()
                        + " expected there to be a Quantity. Instead it found a Choice.");
            }
        }

        if (null == bestResponse) {
            // No matches; tell them they got it wrong but we cannot find any more detailed feedback for them.
            return new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser, false, new Content(
                    DEFAULT_VALIDATION_RESPONSE), false, false, new Date());
        } else {
            return bestResponse;
        }
    }

    /**
     * Numerically validate the response without units being considered.
     *
     * @param isaacNumericQuestion - question to validate.
     * @param answerFromUser       - answer from user
     * @return the validation response
     */
    private QuantityValidationResponse validateWithoutUnits(final IsaacNumericQuestion isaacNumericQuestion,
                                                            final Quantity answerFromUser) {
        log.debug("\t[validateWithoutUnits]");
        QuantityValidationResponse bestResponse = null;
        int sigFigsToValidateWith = numberOfSignificantFiguresToValidateWith(answerFromUser.getValue(),
                isaacNumericQuestion.getSignificantFiguresMin(), isaacNumericQuestion.getSignificantFiguresMax());

        List<Choice> orderedChoices = getOrderedChoices(isaacNumericQuestion.getChoices());

        for (Choice c : orderedChoices) {
            if (c instanceof Quantity) {
                Quantity quantityFromQuestion = (Quantity) c;

                // Do we have a match? Since only comparing values, either an exact match or not a match at all.
                if (numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(), sigFigsToValidateWith)) {
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            quantityFromQuestion.isCorrect(), (Content) quantityFromQuestion.getExplanation(),
                            quantityFromQuestion.isCorrect(), null, new Date());
                    break;
                }
            } else {
                log.error("Isaac Numeric Validator expected there to be a Quantity in ("
                        + isaacNumericQuestion.getCanonicalSourceFile() + ") Instead it found a Choice.");
            }
        }

        if (null == bestResponse) {
            // No matches; tell them they got it wrong but we cannot find any more detailed feedback for them.
            return new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                    false, new Content(DEFAULT_VALIDATION_RESPONSE), false, null, new Date());
        } else {
            return bestResponse;
        }
    }

    /**
     * Test whether two quantity values match. Parse the strings as doubles, supporting notation of 3x10^12 to mean
     * 3e12, then test that they match to given number of s.f.
     *
     * @param trustedValue               - first number
     * @param untrustedValue             - second number
     * @param significantFiguresRequired - the number of significant figures to perform comparisons to
     * @return true when the numbers match
     * @throws NumberFormatException - when one of the values cannot be parsed
     */
    private boolean numericValuesMatch(final String trustedValue, final String untrustedValue,
                                       final int significantFiguresRequired) throws NumberFormatException {
        log.debug("\t[numericValuesMatch]");
        double trustedDouble, untrustedDouble;

        String untrustedParsedValue = reformatNumberForParsing(untrustedValue);
        String trustedParsedValue = reformatNumberForParsing(trustedValue);

        // Round to N s.f. for trusted value
        trustedDouble = roundStringValueToSigFigs(trustedParsedValue, significantFiguresRequired);
        untrustedDouble = roundStringValueToSigFigs(untrustedParsedValue, significantFiguresRequired);
        final double epsilon = 1e-50;

        return Math.abs(trustedDouble - untrustedDouble) < max(epsilon * max(trustedDouble, untrustedDouble), epsilon);
    }

    /**
     * Round a double to a given number of significant figures.
     *
     * @param value   - number to round
     * @param sigFigs - number of significant figures required
     * @return the rounded number.
     */
    private double roundStringValueToSigFigs(final String value, final int sigFigs) {
        log.debug("\t[roundStringValueToSigFigs]");

        // To prevent floating point arithmetic errors when rounding the value, use a BigDecimal and pass the string
        // value of the number:
        BigDecimal bigDecimalValue = new BigDecimal(value);
        BigDecimal rounded = bigDecimalValue.round(new MathContext(sigFigs, RoundingMode.HALF_UP));

        return rounded.doubleValue();

    }

    /**
     * Extract from a number in string form how many significant figures it is given to, noting the range if it is
     * ambiguous (as in the case of 1000, for example).
     *
     * @param valueToCheck - the user provided value in string form
     * @return a SigFigResult containing info on the sig figs of the number
     */
    private SigFigResult extractSignificantFigures(final String valueToCheck) {
        log.debug("\t[extractSignificantFigures]");
        String untrustedParsedValue = reformatNumberForParsing(valueToCheck);

        // Parse exactly into a BigDecimal:
        BigDecimal bd = new BigDecimal(untrustedParsedValue);

        if (untrustedParsedValue.contains(".")) {
            // If it contains a decimal point then there is no ambiguity in how many sig figs it has.
            return new SigFigResult(false, bd.precision(), bd.precision());
        } else {
            // If not, we have to be flexible because integer values have undefined significant figure rules.
            char[] unscaledValueToCheck = bd.unscaledValue().toString().toCharArray();

            // Counting trailing zeroes is useful to give bounds on the number of sig figs it could be to:
            int trailingZeroes = 0;
            for (int i = unscaledValueToCheck.length - 1; i >= 0; i--) {
                if (unscaledValueToCheck[i] == '0') {
                    trailingZeroes++;
                } else {
                    break;
                }
            }

            if (trailingZeroes == 0) {
                // This is an integer with no trailing zeroes; there is no ambiguity in how many sig figs it has.
                return new SigFigResult(false, bd.precision(), bd.precision());
            } else {
                // This is an integer with one or more trailing zeroes; it is unclear how many sig figs it may be to.
                int untrustedValueMinSigFigs = bd.precision() - trailingZeroes;
                int untrustedValueMaxSigFigs = bd.precision();
                return new SigFigResult(true, untrustedValueMinSigFigs, untrustedValueMaxSigFigs);
            }
        }
    }

    /**
     * Deduce from the user answer and question data how many sig figs we should use when checking a question. We must
     * pick a value in the allowed range, but it may be informed by the user's answer.
     *
     * @param valueToCheck      - the user provided value in string form
     * @param minAllowedSigFigs - the minimum number of significant figures the question allows
     * @param maxAllowedSigFigs - the maximum number of significant figures the question allows
     * @return the number of significant figures that should be used when checking the question
     */
    private int numberOfSignificantFiguresToValidateWith(final String valueToCheck, final int minAllowedSigFigs,
                                                         final int maxAllowedSigFigs) {
        log.debug("\t[numberOfSignificantFiguresToValidateWith]");
        int untrustedValueSigFigs;
        SigFigResult sigFigsFromUser = extractSignificantFigures(valueToCheck);

        if (!sigFigsFromUser.isAmbiguous) {
            untrustedValueSigFigs = sigFigsFromUser.sigFigsMin;
        } else {
            // Since choosing the least possible number of sig figs gives the loosest comparison, use that.
            // This is kindest to the user, but may need to be revised.
            untrustedValueSigFigs = sigFigsFromUser.sigFigsMin;
        }

        /* The number of significant figures to validate to must be less than or equal to the max allowed, and greater
           than or equal to the minimum allowed. If the ranges intersect, or the untrusted value is unambiguous in the
           acceptable range, choose the least number of sig figs the user answer allows; this is kindest to the user
           in terms of matching known wrong answers.
         */
        return max(min(untrustedValueSigFigs, maxAllowedSigFigs), minAllowedSigFigs);
    }

    /**
     * Helper method to verify if the answer given is to too few significant figures.
     *
     * @param valueToCheck      - the value as a string from the user to check.
     * @param minAllowedSigFigs - the minimum number of significant figures that is expected for the answer to be correct.
     * @return true if too few, false if not.
     */
    private boolean tooFewSignificantFigures(final String valueToCheck, final int minAllowedSigFigs) {
        log.debug("\t[tooFewSignificantFigures]");

        SigFigResult sigFigsFromUser = extractSignificantFigures(valueToCheck);

        return sigFigsFromUser.sigFigsMax < minAllowedSigFigs;
    }

    /**
     * Helper method to verify if the answer given is to too many significant figures.
     *
     * @param valueToCheck      - the value as a string from the user to check.
     * @param maxAllowedSigFigs - the maximum number of significant figures that is expected for the answer to be correct.
     * @return true if too many, false if not.
     */
    private boolean tooManySignificantFigures(final String valueToCheck, final int maxAllowedSigFigs) {
        log.debug("\t[tooManySignificantFigures]");

        SigFigResult sigFigsFromUser = extractSignificantFigures(valueToCheck);

        return sigFigsFromUser.sigFigsMin > maxAllowedSigFigs;
    }

    /**
     * To save validation effort, if we have string equality between the submitted value and an answer then we
     * can be sure this match is the best possible.
     *
     * @param isaacNumericQuestion - question content object
     * @param answerFromUser       - response form the user
     * @return either a QuestionValidationResponse if there is an exact String match or null if no string match.
     */
    private QuantityValidationResponse exactStringMatch(final IsaacNumericQuestion isaacNumericQuestion,
                                                        final Quantity answerFromUser) {

        log.debug("\t[exactStringMatch]");

        for (Choice c : isaacNumericQuestion.getChoices()) {
            if (c instanceof Quantity) {
                Quantity quantityFromQuestion = (Quantity) c;

                StringBuilder userStringForComparison = new StringBuilder();
                userStringForComparison.append(answerFromUser.getValue().trim());
                if (isaacNumericQuestion.getRequireUnits()) {
                    userStringForComparison.append(answerFromUser.getUnits());
                }

                StringBuilder questionAnswerString = new StringBuilder();
                questionAnswerString.append(quantityFromQuestion.getValue().trim());
                if (isaacNumericQuestion.getRequireUnits()) {
                    questionAnswerString.append(quantityFromQuestion.getUnits());
                }

                if (questionAnswerString.toString().trim().equals(userStringForComparison.toString().trim())) {
                    Boolean unitFeedback = null;
                    if (isaacNumericQuestion.getRequireUnits()) {
                        unitFeedback = quantityFromQuestion.getUnits().equals(answerFromUser.getUnits());
                    }

                    return new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            quantityFromQuestion.isCorrect(), (Content) quantityFromQuestion.getExplanation(),
                            quantityFromQuestion.isCorrect(), unitFeedback, new Date());
                }
            }
        }
        return null;
    }

    /**
     * Format a number in string form such that Java BigDecimal can parse it.
     *
     * Replace "x10^(...)" with "e(...)", allowing many common unambiguous cases, and fix uses of Unicode minus signs,
     * and allow bare powers of ten.
     *
     * @param numberToFormat - number in some unambiguous standard form.
     * @return - number in engineering standard form e.g. "3.4e3"
     */
    private String reformatNumberForParsing(final String numberToFormat) {
        String reformattedNumber = numberToFormat.replace("−", "-");
        reformattedNumber = reformattedNumber.replaceFirst(PREFIXED_POWER_OF_TEN_REGEX, "e${exp1}${exp2}");
        reformattedNumber = reformattedNumber.replaceFirst(BARE_POWER_OF_TEN_REGEX, "1e${exp1}${exp2}");
        return reformattedNumber;
    }

    /**
     *  Replace explanation of validation response if question has default feedback and existing feedback blank or generic.
     *
     *  This method could be void, since it modifies the object passed in by reference, but it makes for shorter and
     *  simpler code when it is used if it just returns the same object it is passed.
     *
     * @param question - the question to use the default feedback of
     * @param response - the response to modify if necessary
     * @return the modified response object
     */
    private QuantityValidationResponse useDefaultFeedbackIfNecessary(final IsaacNumericQuestion question,
                                                                     final QuantityValidationResponse response) {
        Content feedback = response.getExplanation();
        boolean feedbackEmptyOrGeneric = feedbackIsNullOrEmpty(feedback) || DEFAULT_VALIDATION_RESPONSE.equals(feedback.getValue())
                || DEFAULT_WRONG_UNIT_VALIDATION_RESPONSE.equals(feedback.getValue());

        if (null != question.getDefaultFeedback() && feedbackEmptyOrGeneric) {
            log.debug("Replacing generic or blank explanation with default feedback from question.");
            response.setExplanation(question.getDefaultFeedback());
            // TODO - should this preserve the 'sig_figs' tag? If so, how to do it without modifying the referenced default feedback?
        }
        return response;
    }
}
