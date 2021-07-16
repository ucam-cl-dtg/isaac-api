/*
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
package uk.ac.cam.cl.dtg.segue.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;

import java.util.List;

/**
 * Base class for all question types.
 * 
 */
public class QuestionDTO extends ContentDTO {
    protected ContentBaseDTO answer;
    protected List<ContentBaseDTO> hints;
    protected ContentDTO defaultFeedback;

    // Set if the user is logged in and we have information.
    protected QuestionValidationResponseDTO bestAttempt;

    /**
     * Default constructor for mappers.
     */
    public QuestionDTO() {

    }

    /**
     * Gets the answer, defaults to not return to the user.
     * This is Manually unignored when the answer is required to
     * be returned to the user e.g. quick questions
     * @return the answer
     */
    @JsonIgnore
    public ContentBaseDTO getAnswer() {
        return answer;
    }

    /**
     * Sets the answer.
     * 
     * @param answer
     *            the answer to set
     */
    public void setAnswer(final ContentBaseDTO answer) {
        this.answer = answer;
    }

    /**
     * Gets the hints.
     * 
     * @return the hints
     */
    public List<ContentBaseDTO> getHints() {
        return hints;
    }

    /**
     * Sets the hints.
     * 
     * @param hints
     *            the hints to set
     */
    public void setHints(final List<ContentBaseDTO> hints) {
        this.hints = hints;
    }

    /**
     * Gets the default feedback to be used when no other feedback is generated..
     *
     * @return the defaultFeedback
     */
    public final ContentDTO getDefaultFeedback() {
        return defaultFeedback;
    }

    /**
     * Sets the default feedback to be used when no other feedback is generated.
     *
     * @param defaultFeedback
     *            the defaultFeedback to set
     */
    public final void setDefaultFeedback(final ContentDTO defaultFeedback) {
        this.defaultFeedback = defaultFeedback;
    }

    /**
     * Gets the bestAttempt.
     * 
     * @return the bestAttempt
     */
    public QuestionValidationResponseDTO getBestAttempt() {
        return bestAttempt;
    }

    /**
     * Sets the bestAttempt.
     * 
     * @param bestAttempt
     *            the bestAttempt to set
     */
    public void setBestAttempt(final QuestionValidationResponseDTO bestAttempt) {
        this.bestAttempt = bestAttempt;
    }

}
