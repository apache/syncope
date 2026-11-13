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
package org.apache.syncope.core.persistence.common.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({ "unchecked", "rawtypes" })
@ExtendWith(MockitoExtension.class)
class DefaultAnyUtilsTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private GroupDAO groupDAO;

    @Mock
    private AnyObjectDAO anyObjectDAO;

    @Mock
    private PlainSchemaDAO plainSchemaDAO;

    @Mock
    private EntityFactory entityFactory;

    @Mock
    private AnyTypeKind anyTypeKind;

    private DefaultAnyUtils utilsFor(final AnyTypeKind kind) {
        when(entityFactory.userClass()).thenReturn((Class) User.class);
        when(entityFactory.groupClass()).thenReturn((Class) Group.class);
        when(entityFactory.anyObjectClass()).
                thenReturn((Class) org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject.class);

        return new DefaultAnyUtils(
                userDAO, groupDAO, anyObjectDAO, plainSchemaDAO, entityFactory, kind);
    }

    @Test
    void anyTypeKindReturnsConfiguredKind() {
        AnyTypeKind kind = AnyTypeKind.USER;
        DefaultAnyUtils utils = utilsFor(kind);

        assertSame(kind, utils.anyTypeKind());
    }

    @Test
    void anyClassForUser() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.USER);

        assertSame(User.class, utils.anyClass());
    }

    @Test
    void anyClassForGroup() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.GROUP);

        assertSame(Group.class, utils.anyClass());
    }

    @Test
    void anyClassForAnyObject() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.ANY_OBJECT);

        assertSame(
                org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject.class,
                utils.anyClass());
    }

    @Test
    void getFieldReturnsEmptyForInterface() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.USER);

        // User is an interface; all its fields are static, so initFieldNames finds nothing.
        Optional<Field> field = utils.getField("anyField");

        assertFalse(field.isPresent());
    }

    @Test
    void getFieldNonExisting() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.USER);

        Optional<Field> field = utils.getField("nonExistingField");

        assertFalse(field.isPresent());
    }

    @Test
    void newAnyTOForUser() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.USER);

        AnyTO to = utils.newAnyTO();

        assertNotNull(to);
        assertEquals(UserTO.class, to.getClass());
    }

    @Test
    void newAnyTOForGroup() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.GROUP);

        AnyTO to = utils.newAnyTO();

        assertNotNull(to);
        assertEquals(GroupTO.class, to.getClass());
    }

    @Test
    void newAnyTOForAnyObject() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.ANY_OBJECT);

        AnyTO to = utils.newAnyTO();

        assertNotNull(to);
        assertEquals(AnyObjectTO.class, to.getClass());
    }

    @Test
    void newAnyCRForUser() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.USER);

        AnyCR cr = utils.newAnyCR();

        assertNotNull(cr);
        assertEquals(UserCR.class, cr.getClass());
    }

    @Test
    void newAnyCRForGroup() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.GROUP);

        AnyCR cr = utils.newAnyCR();

        assertNotNull(cr);
        assertEquals(GroupCR.class, cr.getClass());
    }

    @Test
    void newAnyCRForAnyObject() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.ANY_OBJECT);

        AnyCR cr = utils.newAnyCR();

        assertNotNull(cr);
        assertEquals(AnyObjectCR.class, cr.getClass());
    }

    @Test
    void newAnyURSetsKey() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.USER);

        AnyUR ur = utils.newAnyUR("test-key");

        assertNotNull(ur);
        assertEquals("test-key", ur.getKey());
    }

    @Test
    void newAnyURNullKey() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.USER);

        AnyUR ur = utils.newAnyUR(null);

        assertNotNull(ur);
        assertNull(ur.getKey());
    }

    @Test
    void daoForUser() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.USER);

        AnyDAO<?> dao = utils.dao();

        assertSame(userDAO, dao);
    }

    @Test
    void daoForGroup() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.GROUP);

        AnyDAO<?> dao = utils.dao();

        assertSame(groupDAO, dao);
    }

    @Test
    void daoForAnyObject() {
        DefaultAnyUtils utils = utilsFor(AnyTypeKind.ANY_OBJECT);

        AnyDAO<?> dao = utils.dao();

        assertSame(anyObjectDAO, dao);
    }

    @Test
    void constructorDoesNotThrow() {
        assertDoesNotThrow(() -> utilsFor(AnyTypeKind.USER));
    }
}
