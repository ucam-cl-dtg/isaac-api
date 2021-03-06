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
package uk.ac.cam.cl.dtg.segue.dos.content;

import java.util.List;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;

/**
 * Choice object The choice object is a specialized form of content and allows the storage of data relating to possible
 * answers to questions.
 * 
 */
@DTOMapping(QuestionDTO.class)
@JsonContentType("question")
public class Question extends Content {

    protected ContentBase answer;
    protected List<ContentBase> hints;


    public Question() {

    }

    /**
     * Gets the answer.
     * 
     * @return the answer
     */
    public final ContentBase getAnswer() {
        return answer;
    }

    /**
     * Sets the answer.
     * 
     * @param answer
     *            the answer to set
     */
    public final void setAnswer(final ContentBase answer) {
        this.answer = answer;
    }

    /**
     * Gets the hints.
     * 
     * @return the hints
     */
    public final List<ContentBase> getHints() {
        return hints;
    }

    /**
     * Sets the hints.
     * 
     * @param hints
     *            the hints to set
     */
    public final void setHints(final List<ContentBase> hints) {
        this.hints = hints;
    }

}
