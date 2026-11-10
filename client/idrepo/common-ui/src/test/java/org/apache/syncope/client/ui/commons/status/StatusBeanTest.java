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
package org.apache.syncope.client.ui.commons.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.junit.jupiter.api.Test;

class StatusBeanTest {

    private static final String KEY = UUID.randomUUID().toString();
    private static final String NAME = "testUser";
    private static final String RESOURCE = "ldap";

    @Test
    void userConstructor() {
        UserTO user = new UserTO();
        user.setKey(KEY);
        user.setUsername(NAME);

        StatusBean bean = new StatusBean(user, RESOURCE);

        assertNotNull(bean);
        assertEquals(KEY, bean.getKey());
        assertEquals(NAME, bean.getName());
        assertEquals(RESOURCE, bean.getResource());
        assertEquals(Status.OBJECT_NOT_FOUND, bean.getStatus());
        assertTrue(bean.isLinked());
        assertEquals(bean, bean);
    }

    @Test
    void groupConstructor() {
        GroupTO group = new GroupTO();
        group.setKey(KEY);
        group.setName(NAME);

        StatusBean bean = new StatusBean(group, RESOURCE);

        assertNotNull(bean);
        assertEquals(KEY, bean.getKey());
        assertEquals(NAME, bean.getName());
        assertEquals(RESOURCE, bean.getResource());
    }

    @Test
    void settersAndGetters() {
        StatusBean bean = new StatusBean(new UserTO(), RESOURCE);

        bean.setConnObjectLink("connLink");
        assertEquals("connLink", bean.getConnObjectLink());

        bean.setStatus(Status.ACTIVE);
        assertEquals(Status.ACTIVE, bean.getStatus());

        bean.setLinked(false);
        assertFalse(bean.isLinked());
    }

    @Test
    void equalsAndHashCode() {
        UserTO user1 = new UserTO();
        user1.setKey(KEY);
        user1.setUsername(NAME);

        UserTO user2 = new UserTO();
        user2.setKey(KEY);
        user2.setUsername(NAME);

        StatusBean bean1 = new StatusBean(user1, RESOURCE);
        StatusBean bean2 = new StatusBean(user2, RESOURCE);

        assertEquals(bean1, bean2);
        assertEquals(bean1.hashCode(), bean2.hashCode());
        assertNotEquals(bean1, new StatusBean(new UserTO(), RESOURCE));
    }

    @Test
    void toStringNotNull() {
        StatusBean bean = new StatusBean(new UserTO(), RESOURCE);
        assertNotNull(bean.toString());
    }
}
