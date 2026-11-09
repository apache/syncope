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
package org.apache.syncope.core.keymaster.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.keymaster.ConfParamDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.keymaster.ConfParam;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class InternalConfParamHelperTest {

    private final ConfParamDAO confParamDAO = mock(ConfParamDAO.class);

    private final EntityFactory entityFactory = mock(EntityFactory.class);

    private final InternalConfParamHelper helper = new InternalConfParamHelper(confParamDAO, entityFactory);

    @Test
    void list() {
        ConfParam param1 = mock(ConfParam.class);
        when(param1.getKey()).thenReturn("key1");
        ObjectNode node1 = new ObjectNode(JsonNodeFactory.instance);
        node1.put("value", 1);
        when(param1.getValue()).thenReturn(node1);

        ConfParam param2 = mock(ConfParam.class);
        when(param2.getKey()).thenReturn("key2");
        ObjectNode node2 = new ObjectNode(JsonNodeFactory.instance);
        node2.put("value", 2);
        when(param2.getValue()).thenReturn(node2);

        doReturn(List.of(param1, param2)).when(confParamDAO).findAll();

        Map<String, Object> result = helper.list();

        assertEquals(2, result.size());
        assertEquals(Map.of("value", 1), result.get("key1"));
        assertEquals(Map.of("value", 2), result.get("key2"));
    }

    @Test
    void listWithInvalidValueSkips() {
        ConfParam param = mock(ConfParam.class);
        when(param.getKey()).thenReturn("key");
        when(param.getValue()).thenReturn(null);

        doReturn(List.of(param)).when(confParamDAO).findAll();

        Map<String, Object> result = helper.list();

        assertEquals(1, result.size());
        assertNull(result.get("key"));
    }

    @Test
    void getWhenNotFoundReturnsNull() {
        doReturn(Optional.<ConfParam>empty()).when(confParamDAO).findById("key");

        assertNull(helper.get("key"));
    }

    @Test
    void getReturnsValue() {
        ObjectNode value = new ObjectNode(JsonNodeFactory.instance);
        value.put("key", "value");
        ConfParam param = mock(ConfParam.class);
        when(param.getValue()).thenReturn(value);
        doReturn(Optional.<ConfParam>of(param)).when(confParamDAO).findById("key");

        assertSame(value, helper.get("key"));
    }

    @Test
    void setCreatesNewWhenNotFound() {
        ObjectNode value = new ObjectNode(JsonNodeFactory.instance);
        value.put("key", "value");
        ConfParam param = mock(ConfParam.class);
        when(entityFactory.newEntity(eq(ConfParam.class))).thenReturn(param);
        doReturn(Optional.<ConfParam>empty()).when(confParamDAO).findById("key");

        helper.set("key", value);

        verify(param).setKey("key");
        verify(param).setValue(value);
        verify(confParamDAO).save(param);
    }

    @Test
    void setUpdatesExistingWhenFound() {
        ObjectNode value = new ObjectNode(JsonNodeFactory.instance);
        value.put("key", "value");
        ConfParam param = mock(ConfParam.class);
        doReturn(Optional.<ConfParam>of(param)).when(confParamDAO).findById("key");

        helper.set("key", value);

        verify(param).setValue(value);
        verify(confParamDAO).save(param);
    }

    @Test
    void setNullThrows() {
        assertThrows(NotFoundException.class, () -> helper.set("key", null));
    }

    @Test
    void remove() {
        helper.remove("key");

        verify(confParamDAO).deleteById("key");
    }
}
