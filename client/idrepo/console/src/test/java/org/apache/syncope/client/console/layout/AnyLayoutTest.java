/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.console.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.junit.jupiter.api.Test;

public class AnyLayoutTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    @Test
    public void issueSYNCOPE1554() throws JsonProcessingException {
        AnyLayout defaultObj = new AnyLayout();
        defaultObj.setUser(new UserFormLayoutInfo());
        defaultObj.setGroup(new GroupFormLayoutInfo());
        defaultObj.getAnyObjects().put("PRINTER", new AnyObjectFormLayoutInfo());
        String defaultObjJSON = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(defaultObj);

        String defaultIfEmpty = AnyLayoutUtils.defaultIfEmpty(
                StringUtils.EMPTY, Arrays.asList(AnyTypeKind.USER.name(), AnyTypeKind.GROUP.name(), "PRINTER"));
        assertEquals(defaultObjJSON, defaultIfEmpty);

        AnyLayout deserialized = MAPPER.readValue(defaultIfEmpty, AnyLayout.class);
        assertNotNull(deserialized);
    }
}
