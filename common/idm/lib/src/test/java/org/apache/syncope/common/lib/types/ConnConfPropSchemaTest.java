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
package org.apache.syncope.common.lib.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConnConfPropSchemaTest {

    @Test
    void schema() {
        ConnConfPropSchema schema = new ConnConfPropSchema();
        schema.setName("name");
        schema.setType("type");
        schema.setRequired(true);
        schema.setConfidential(false);
        List<Object> defaults = new ArrayList<>();
        defaults.add("default");
        schema.getDefaultValues().clear();
        schema.getDefaultValues().addAll(defaults);
        schema.setDisplayName("display");
        schema.setHelpMessage("help");
        schema.setOrder(1);

        assertEquals("name", schema.getName());
        assertEquals("type", schema.getType());
        assertTrue(schema.isRequired());
        assertTrue(!schema.isConfidential());
        assertEquals(1, schema.getDefaultValues().size());
        assertEquals("default", schema.getDefaultValues().get(0));
        assertEquals("display", schema.getDisplayName());
        assertEquals("help", schema.getHelpMessage());
        assertEquals(1, schema.getOrder());

        ConnConfPropSchema same = new ConnConfPropSchema();
        same.setName("name");
        same.setType("type");
        same.setRequired(true);
        same.setConfidential(false);
        List<Object> sameDefaults = new ArrayList<>();
        sameDefaults.add("default");
        same.getDefaultValues().clear();
        same.getDefaultValues().addAll(sameDefaults);
        same.setDisplayName("display");
        same.setHelpMessage("help");
        same.setOrder(1);

        ConnConfPropSchema other = new ConnConfPropSchema();
        other.setName("other");
        other.setOrder(2);

        assertEquals(schema, schema);
        assertTrue(!schema.equals(null));
        assertEquals(schema, same);
        assertEquals(schema.hashCode(), same.hashCode());
        assertTrue(!schema.equals(other));

        assertEquals(0, schema.compareTo(same));
        assertTrue(schema.compareTo(other) < 0);
    }
}
