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
package uk.ac.cam.cl.dtg.segue.dao.content;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import uk.ac.cam.cl.dtg.segue.dos.content.Item;
import uk.ac.cam.cl.dtg.segue.dos.content.ParsonsItem;
import uk.ac.cam.cl.dtg.segue.dto.content.ItemDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ParsonsItemDTO;

/**
 * Converts Item objects to and from their DTO equivalents.
 *
 */
public class ItemOrikaConverter extends BidirectionalConverter<Item, ItemDTO> {

    /**
     * Constructs an Orika Converter specialises in selecting the correct subclass for choice objects.
     *
     */
    public ItemOrikaConverter() {

    }

    @Override
    public ItemDTO convertTo(final Item source, final Type<ItemDTO> destinationType, MappingContext _context) {
        if (null == source) {
            return null;
        }

        if (source instanceof ParsonsItem) {
            return super.mapperFacade.map(source, ParsonsItemDTO.class);
        } else {
            // This looks like it should cause an infinite loop / stack overflow but apparently it does not.
            ItemDTO itemDTO = new ItemDTO();
            super.mapperFacade.map(source, itemDTO);
            return itemDTO;
        }
    }

    @Override
    public Item convertFrom(final ItemDTO source, final Type<Item> destinationType, MappingContext _context) {
        if (null == source) {
            return null;
        }

        if (source instanceof ParsonsItemDTO) {
            return super.mapperFacade.map(source, ParsonsItem.class);
        } else {
            // This looks like it should cause an infinite loop / stack overflow but apparently it does not.
            Item item = new Item();
            super.mapperFacade.map(source, item);
            return item;
        }
    }
}
