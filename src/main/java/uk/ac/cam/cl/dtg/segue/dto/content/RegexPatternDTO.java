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
package uk.ac.cam.cl.dtg.segue.dto.content;

/**
 * DTO to represent a regex pattern.
 *
 */
public class RegexPatternDTO extends ChoiceDTO {
    private Boolean caseInsensitive;
    private Boolean multiLineRegex;
    private Boolean matchWholeString;

    /**
     * Default constructor required for mapping.
     */
    public RegexPatternDTO() {

    }

    /**
     * @return Whether this regex choice should allow any case to match.
     */
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    /**
     * Whether to match strictly to case or not.
     *
     * @param caseInsensitive Whether to ignore the case when checking.
     */
    public void setCaseInsensitive(final boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * @return Whether this regex pattern should have the multiline flag set.
     */
    public boolean isMultiLineRegex() {
        return multiLineRegex;
    }

    /**
     * Whether this regex pattern should match strings across multiple lines.
     *
     * @param multiLineRegex Whether this regex pattern should have the multiline flag set.
     */
    public void setMultiLineRegex(final boolean multiLineRegex) {
        this.multiLineRegex = multiLineRegex;
    }

    /**
     * @return Whether this regex pattern is partial or exact match.
     */
    public boolean ismatchWholeString() {
        return matchWholeString;
    }

    /**
     * This determines whether to use .find() or .match() when matching strings
     * against this regex. If matchWholeString is true, we use .match().
     *
     * @param matchWholeString  Whether this regex pattern is partial or exact match.
     */
    public void setMatchWholeString(final boolean matchWholeString) {
        this.matchWholeString = matchWholeString;
    }
}
