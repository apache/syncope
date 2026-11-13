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
package org.apache.syncope.common.lib.scim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SCIMExtensionAnyConfTest {

    private static SCIMItem item(final String intAttrName, final String extAttrName) {
        SCIMItem item = new SCIMItem();
        item.setIntAttrName(intAttrName);
        item.setExtAttrName(extAttrName);
        return item;
    }

    @Test
    void addIgnoresNullAndDuplicates() {
        SCIMExtensionAnyConf conf = new SCIMExtensionAnyConf();

        assertFalse(conf.add(null));
        assertTrue(conf.add(item("realm", "realm")));
        assertTrue(conf.add(item("realm", "realm")));

        assertEquals(1, conf.getAttributes().size());
    }

    @Test
    void asMap() {
        SCIMExtensionAnyConf conf = new SCIMExtensionAnyConf();
        conf.add(item("realm", "urn:syncope:realm"));
        conf.add(item("surname", "urn:syncope:familyName"));

        assertEquals(Map.of(
                "realm", "urn:syncope:realm",
                "surname", "urn:syncope:familyName"),
                conf.asMap());
        assertThrows(UnsupportedOperationException.class, () -> conf.asMap().put("other", "other"));
    }

    @Test
    void scimItemEquality() {
        SCIMItem item = item("realm", "realm");
        SCIMItem same = item("realm", "realm");
        SCIMItem differentType = item("realm", "realm");
        differentType.setReturned(SCIMReturned.ALWAYS);

        assertEquals(item, same);
        assertEquals(item.hashCode(), same.hashCode());
        assertFalse(item.equals(differentType));
    }
}
