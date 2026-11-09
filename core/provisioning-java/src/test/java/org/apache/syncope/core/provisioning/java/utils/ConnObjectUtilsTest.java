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
package org.apache.syncope.core.provisioning.java.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.ConnObject;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.junit.jupiter.api.Test;

class ConnObjectUtilsTest {

    @Test
    void toSyncTokenNull() {
        assertNull(ConnObjectUtils.toSyncToken(null));
    }

    @Test
    void toStringNull() {
        assertNull(ConnObjectUtils.toString(null));
    }

    @Test
    void syncTokenRoundTrip() {
        SyncToken token = new SyncToken("tokenValue");
        String serialized = ConnObjectUtils.toString(token);
        SyncToken deserialized = ConnObjectUtils.toSyncToken(serialized);
        assertEquals(token.getValue(), deserialized.getValue());
    }

    @Test
    void getPasswordString() {
        assertEquals("plain", ConnObjectUtils.getPassword("plain"));
    }

    @Test
    void getPasswordOther() {
        assertEquals("42", ConnObjectUtils.getPassword(Integer.valueOf(42)));
    }

    @Test
    void getPasswordGuardedString() {
        GuardedString guarded = new GuardedString("secret".toCharArray());
        assertEquals("secret", ConnObjectUtils.getPassword(guarded));
    }

    @Test
    void getConnObjectTONull() {
        ConnObject connObject = ConnObjectUtils.getConnObjectTO(null, null);
        assertTrue(connObject != null);
        assertTrue(connObject.getAttrs().isEmpty());
    }

    @Test
    void getConnObjectTOWithAttributes() {
        Attribute attr = AttributeBuilder.build("attrName", "attrValue");
        Set<Attribute> attrs = Set.of(attr);

        ConnObject connObject = ConnObjectUtils.getConnObjectTO("fiqlValue", attrs);

        assertEquals("fiqlValue", connObject.getFiql());
        assertEquals(1, connObject.getAttrs().size());
        Attr attrTO = connObject.getAttrs().stream().findFirst().orElseThrow();
        assertEquals("attrName", attrTO.getSchema());
        assertEquals(1, attrTO.getValues().size());
        assertEquals("attrValue", attrTO.getValues().getFirst());
    }

    @Test
    void getConnObjectTOWithPasswordAttribute() {
        GuardedString guarded = new GuardedString("secret".toCharArray());
        Attribute attr = AttributeBuilder.build("password", guarded);
        Set<Attribute> attrs = Set.of(attr);

        ConnObject connObject = ConnObjectUtils.getConnObjectTO("fiqlValue", attrs);

        Attr attrTO = connObject.getAttrs().stream().findFirst().orElseThrow();
        assertEquals("secret", attrTO.getValues().getFirst());
    }

    @Test
    void getConnObjectTOWithSecurityUtil() {
        // Verify SecurityUtil.decrypt round-trip used by getPassword
        GuardedString guarded = new GuardedString("password1".toCharArray());
        assertEquals("password1", SecurityUtil.decrypt(guarded));
    }
}
