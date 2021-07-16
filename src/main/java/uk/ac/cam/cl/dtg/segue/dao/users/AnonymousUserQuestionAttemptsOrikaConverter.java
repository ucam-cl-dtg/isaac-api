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
package uk.ac.cam.cl.dtg.segue.dao.users;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;

import java.util.List;
import java.util.Map;

/**
 * AnonymousQuestionAttemptsOrikaConverter A specialist converter class to work with the Orika automapper library.
 * 
 * Responsible for converting question attempt maps from their DO state to their DTO state. It seems ORIKA is not good
 * at converting between highly nested data structures.
 */
public class AnonymousUserQuestionAttemptsOrikaConverter
        extends
        CustomConverter<Map<String, Map<String, List<QuestionValidationResponse>>>, 
        Map<String, Map<String, List<QuestionValidationResponseDTO>>>> {

    /**
     * Constructs an Orika Converter specialises in selecting the correct subclass for choice objects.
     * 
     */
    public AnonymousUserQuestionAttemptsOrikaConverter() {

    }

    @Override
    public Map<String, Map<String, List<QuestionValidationResponseDTO>>> convert(
            final Map<String, Map<String, List<QuestionValidationResponse>>> source,
            final Type<? extends Map<String, Map<String, List<QuestionValidationResponseDTO>>>> destinationType,
            MappingContext _context) {
        // convert in one direction
        if (null == source) {
            return null;
        }

        // now map the hard question attempts stuff.
        Map<String, Map<String, List<QuestionValidationResponseDTO>>> newMap = Maps.newHashMap();
        for (Map.Entry<String, Map<String, List<QuestionValidationResponse>>> page : source.entrySet()) {
            Map<String, List<QuestionValidationResponseDTO>> attemptsMap = Maps.newHashMap();
            for (Map.Entry<String, List<QuestionValidationResponse>> questionEntry : page.getValue().entrySet()) {
                List<QuestionValidationResponseDTO> dtoList = Lists.newArrayList();
                for (QuestionValidationResponse dto : questionEntry.getValue()) {
                    dtoList.add(this.mapperFacade.map(dto, QuestionValidationResponseDTO.class));
                }
                attemptsMap.put(questionEntry.getKey(), dtoList);
            }
            newMap.put(page.getKey(), attemptsMap);
        }
        return newMap;
    }
}