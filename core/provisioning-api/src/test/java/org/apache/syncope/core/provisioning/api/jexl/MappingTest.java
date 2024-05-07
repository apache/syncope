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
package org.apache.syncope.core.provisioning.api.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.junit.jupiter.api.Test;

public class MappingTest extends AbstractTest {

    @Test
    public void anyConnObjectLink() {
        Realm realm = mock(Realm.class);
        when(realm.getFullPath()).thenReturn("/even");

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("rossini");
        when(user.getRealm()).thenReturn(realm);
        assertNotNull(user);

        JexlContext jexlContext = new MapContext();
        JexlUtils.addFieldsToContext(user, jexlContext);

        String connObjectLink = "'uid=' + username + ',ou=people,o=isp'";
        assertEquals("uid=rossini,ou=people,o=isp", JexlUtils.evaluateExpr(connObjectLink, jexlContext));

        connObjectLink = "'uid=' + username + realm.replaceAll('/', ',o=') + ',ou=people,o=isp'";
        assertEquals("uid=rossini,o=even,ou=people,o=isp", JexlUtils.evaluateExpr(connObjectLink, jexlContext));
    }

    @Test
    public void realmConnObjectLink() {
        Realm realm = mock(Realm.class);
        when(realm.getFullPath()).thenReturn("/even/two");
        assertNotNull(realm);

        JexlContext jexlContext = new MapContext();
        JexlUtils.addFieldsToContext(realm, jexlContext);

        String connObjectLink = "syncope:fullPath2Dn(fullPath, 'ou') + ',o=isp'";
        assertEquals("ou=two,ou=even,o=isp", JexlUtils.evaluateExpr(connObjectLink, jexlContext));

        when(realm.getFullPath()).thenReturn("/even");
        assertNotNull(realm);

        jexlContext = new MapContext();
        JexlUtils.addFieldsToContext(realm, jexlContext);

        assertEquals("ou=even,o=isp", JexlUtils.evaluateExpr(connObjectLink, jexlContext));
    }

    @Test
    public void datetime() {
        OffsetDateTime now = OffsetDateTime.now();

        JexlContext jexlContext = new MapContext();
        jexlContext.set("value", now);

        String expression = "value.toInstant().toEpochMilli()";
        assertEquals(now.toInstant().toEpochMilli(), JexlUtils.evaluateExpr(expression, jexlContext));
    }
}
