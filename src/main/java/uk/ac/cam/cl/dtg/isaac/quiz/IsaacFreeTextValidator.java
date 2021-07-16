/**
 * Copyright 2018 Meurig Thomas
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

import com.google.common.collect.ImmutableMap;
import org.isaacphysics.thirdparty.openmark.marker.PMatch;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacFreeTextQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.FreeTextRule;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dos.content.StringChoice;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

import java.util.Date;
import java.util.Map;

public class IsaacFreeTextValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacFreeTextValidator.class);

    // Map of wildcards from our RegEx based syntax to the PMatch's custom syntax
    private static final String NON_ALPHANUMERIC_CHARS = "!\"#£$%&'/\\-+<=>@^`{}~.,()[]:;";
    private static final Map<String, String> WILDCARD_CONVERSION_MAP = ImmutableMap.of(
            "*", "&",
            ".", "#"
    );
    private static final String ESCAPE_CHARACTER = "\\";
    private static final String TEMPORARY_OBSCURE_CHARACTER = "\uBAD1"; // Same character as is used in PMatch library

    private static String convertToPMatchWildcardNotation(final String ruleValue) {
        String ouSyntaxRuleValue = ruleValue;
        for (Map.Entry<String, String> wildcardMap : WILDCARD_CONVERSION_MAP.entrySet()) {
            String isaacWildCard = wildcardMap.getKey();
            String openMarkWildcard = wildcardMap.getValue();

            // Escape unsupported OpenMark wildcard
            ouSyntaxRuleValue = ouSyntaxRuleValue.replace(openMarkWildcard, ESCAPE_CHARACTER + openMarkWildcard);

            // Temporarily replace escaped Isaac wildcards with an obscure character
            ouSyntaxRuleValue = ouSyntaxRuleValue.replace(ESCAPE_CHARACTER + isaacWildCard, TEMPORARY_OBSCURE_CHARACTER);
            // Convert Isaac wildcard to the library supported OpenMark wildcard
            ouSyntaxRuleValue = ouSyntaxRuleValue.replace(isaacWildCard, openMarkWildcard);
            // Replace our escaped Isaac characters with the wildcard character for matching
            ouSyntaxRuleValue = ouSyntaxRuleValue.replace(TEMPORARY_OBSCURE_CHARACTER, isaacWildCard);
        }
        return ouSyntaxRuleValue;
    }

    private String removeNonAlphanumericChars(final String answer, final String rule) {
        String strippedAnswer = answer;
        for (char nonAlphanumericChar : NON_ALPHANUMERIC_CHARS.toCharArray()) {
            String nonAlphanumericCharStr = String.valueOf(nonAlphanumericChar);
            if (!rule.contains(nonAlphanumericCharStr)) {
                strippedAnswer = strippedAnswer.replace(nonAlphanumericCharStr, " ");
            }
        }
        return strippedAnswer;
    }

    private static String extractAnswerValue(Choice answer, boolean caseInsensitive) {
        return caseInsensitive ? answer.getValue().toLowerCase() : answer.getValue();
    }

    private static String extractRuleValue(FreeTextRule rule) {
        String ruleInCorrectCase = rule.isCaseInsensitive() ? rule.getValue().toLowerCase() : rule.getValue();
        return convertToPMatchWildcardNotation(ruleInCorrectCase);
    }

    private static void validateInputs(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacFreeTextQuestion)) {
            throw new IllegalArgumentException(question.getId() + " is not free-text question");
        }

        if (!(answer instanceof StringChoice)) {
            throw new IllegalArgumentException(
                    answer.getClass() + " is not of expected type StringChoice for (" + question.getId() + ")");
        }
    }

    private static String evaluateMatchingOptions(final FreeTextRule rule) {
        StringBuilder result = new StringBuilder();
        if (rule.getAllowsMisspelling()) { result.append("m"); }
        if (rule.getAllowsAnyOrder()) { result.append("o"); }
        if (rule.getAllowsExtraWords()) { result.append("w"); }
        if (rule.getWordProximity() != null) { result.append("p").append(rule.getWordProximity()); }
        return result.toString();
    }

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        validateInputs(question, answer);
        IsaacFreeTextQuestion freeTextQuestion = (IsaacFreeTextQuestion)question;

        boolean isCorrectResponse = false;
        Content feedback = null;
        for (Choice rule : freeTextQuestion.getChoices()) {
            if (rule instanceof FreeTextRule) {
                FreeTextRule freeTextRule = (FreeTextRule) rule;
                String answerString = extractAnswerValue(answer, freeTextRule.isCaseInsensitive());
                answerString = removeNonAlphanumericChars(answerString, freeTextRule.getValue());
                PMatch questionAnswerMatcher = new PMatch(answerString);
                String matchingParameters = evaluateMatchingOptions(freeTextRule);
                if (questionAnswerMatcher.match(matchingParameters, extractRuleValue(freeTextRule))) {
                    isCorrectResponse = rule.isCorrect();
                    feedback = (Content) rule.getExplanation();
                    break; // on first matching rule
                }
            } else {
                log.error("QuestionId: " + question.getId() + " contains a choice which is not a FreeTextRule.");
            }
        }

        // If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != freeTextQuestion.getDefaultFeedback()) {
            feedback = freeTextQuestion.getDefaultFeedback();
        }

        return new QuestionValidationResponse(question.getId(), answer, isCorrectResponse, feedback, new Date());
    }
}
