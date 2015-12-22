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
package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionSummaryPageDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.dos.content.SeguePage;

import java.util.List;

/**
 * IsaacQuestion Summary Page DO.
 * A page that will provide an augmented view of the content referenced.
 */
@DTOMapping(IsaacQuestionSummaryPageDTO.class)
@JsonContentType("isaacQuestionSummaryPage")
public class IsaacQuestionSummaryPage extends SeguePage {

    private List<GameboardItem> featuredQuestions;
    private List<String> topBoards;
    private List<ExternalReference> extraordinaryQuestions;
    
    /**
     * Default constructor required for Jackson.
     */
    public IsaacQuestionSummaryPage() {
        
    }

    /**
     * Gets the featuredQuestions.
     * @return the featuredQuestions
     */
    public List<GameboardItem> getFeaturedQuestions() {
        return featuredQuestions;
    }

    /**
     * Sets the featuredQuestions.
     * @param featuredQuestions the featuredQuestions to set
     */
    public void setFeaturedQuestions(final List<GameboardItem> featuredQuestions) {
        this.featuredQuestions = featuredQuestions;
    }

    /**
     * Gets the topBoards.
     * @return the topBoards
     */
    public List<String> getTopBoards() {
        return topBoards;
    }

    /**
     * Sets the topBoards.
     * @param topBoards the topBoards to set
     */
    public void setTopBoards(final List<String> topBoards) {
        this.topBoards = topBoards;
    }

    /**
     * Gets the extraordinaryQuestions.
     * @return the extraordinaryQuestions
     */
    public List<ExternalReference> getExtraordinaryQuestions() {
        return extraordinaryQuestions;
    }

    /**
     * Sets the extraordinaryQuestions.
     * @param extraordinaryQuestions the extraordinaryQuestions to set
     */
    public void setExtraordinaryQuestions(final List<ExternalReference> extraordinaryQuestions) {
        this.extraordinaryQuestions = extraordinaryQuestions;
    }
}