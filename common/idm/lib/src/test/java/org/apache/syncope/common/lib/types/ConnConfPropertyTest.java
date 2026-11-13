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

class ConnConfPropertyTest {

    @Test
    void property() {
        ConnConfProperty property = new ConnConfProperty();
        ConnConfPropSchema schema = new ConnConfPropSchema();
        property.setSchema(schema);
        List<Object> values = new ArrayList<>();
        values.add("value");
        property.getValues().clear();
        property.getValues().addAll(values);
        property.setOverridable(true);

        assertEquals("value", property.getValues().get(0));
        assertTrue(property.isOverridable());

        ConnConfProperty same = new ConnConfProperty();
        same.setSchema(schema);
        List<Object> sameValues = new ArrayList<>();
        sameValues.add("value");
        same.getValues().clear();
        same.getValues().addAll(sameValues);
        same.setOverridable(true);

        ConnConfProperty other = new ConnConfProperty();
        List<Object> otherValues = new ArrayList<>();
        otherValues.add("other");
        other.getValues().clear();
        other.getValues().addAll(otherValues);

        assertEquals(property, property);
        assertTrue(!property.equals(null));
        assertEquals(property, same);
        assertEquals(property.hashCode(), same.hashCode());
        assertTrue(!property.equals(other));
    }
}
