/*
 * Copyright 2019 James Sharkey
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

import uk.ac.cam.cl.dtg.segue.dto.content.ItemChoiceDTO;

import java.util.List;

/**
 * Choice for Item Questions, containing a list of Items.
 *
 */
@DTOMapping(ItemChoiceDTO.class)
@JsonContentType("itemChoice")
public class ItemChoice extends Choice {

    private Boolean allowSubsetMatch;
    private List<Item> items;

    /**
     * Default constructor required for mapping.
     */
    public ItemChoice() {
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(final List<Item> items) {
        this.items = items;
    }

    public Boolean isAllowSubsetMatch() {
        return this.allowSubsetMatch;
    }

    public void setAllowSubsetMatch(final boolean allowSubsetMatch) {
        this.allowSubsetMatch = allowSubsetMatch;
    }
}
