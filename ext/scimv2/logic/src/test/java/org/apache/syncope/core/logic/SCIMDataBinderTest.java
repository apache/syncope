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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.scim.SCIMUserNameConf;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.ext.scimv2.api.data.SCIMPatchOp;
import org.apache.syncope.ext.scimv2.api.data.SCIMPatchOperation;
import org.apache.syncope.ext.scimv2.api.data.SCIMPatchPath;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;
import org.apache.syncope.ext.scimv2.api.data.SCIMUserName;
import org.apache.syncope.ext.scimv2.api.data.Value;
import org.apache.syncope.ext.scimv2.api.type.PatchOp;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SCIMDataBinderTest {

    private SCIMDataBinder dataBinder;

    private static Stream<String> getValue() {
        return Stream.of("True", "False");
    }

    @BeforeAll
    void setup() {
        SCIMConfManager scimConfManager = mock(SCIMConfManager.class);
        SCIMConf conf = new SCIMConf();
        conf.setUserConf(new SCIMUserConf());
        conf.getUserConf().setName(new SCIMUserNameConf());
        conf.getUserConf().getName().setGivenName("firstname");
        conf.getUserConf().getName().setFamilyName("surname");
        when(scimConfManager.get()).thenReturn(conf);
        UserLogic userLogic = mock(UserLogic.class);
        AuthDataAccessor authDataAccessor = mock(AuthDataAccessor.class);
        GroupDAO  groupDAO = mock(GroupDAO.class);
        dataBinder = new SCIMDataBinder(scimConfManager, userLogic, authDataAccessor,  groupDAO);
    }

    @ParameterizedTest
    @MethodSource("getValue")
    void toUserUpdateActive(final String value) {
        SCIMPatchOp scimPatchOp = new SCIMPatchOp();
        scimPatchOp.setOperations(List.of(getOperation("active", null, PatchOp.add, value)));
        Pair<UserUR, StatusR> result = dataBinder.toUserUpdate(new UserTO(), scimPatchOp);
        assertNotNull(result);
        assertTrue(result.getLeft().isEmpty());
        assertNotNull(result.getRight());
        assertTrue(result.getRight().isOnSyncope());
        assertEquals(
                Boolean.parseBoolean(value) ? StatusRType.REACTIVATE : StatusRType.SUSPEND,
                result.getRight().getType());
    }

    @Test
    void toUserUpdate() {
        SCIMPatchOp scimPatchOp = new SCIMPatchOp();
        List<SCIMPatchOperation> operations = new ArrayList<>();
        operations.add(getOperation("name", "familyName", PatchOp.add, "Rossini"));
        scimPatchOp.setOperations(operations);

        Pair<UserUR, StatusR> result = dataBinder.toUserUpdate(new UserTO(), scimPatchOp);
        assertNotNull(result);
        assertNull(result.getRight());
        assertNotNull(result.getLeft());
        assertEquals(1, result.getLeft().getPlainAttrs().size());
        assertTrue(result.getLeft().getPlainAttrs().stream().anyMatch(attrPatch ->
                PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                        && attrPatch.getAttr().getSchema().equals("surname")
                        && attrPatch.getAttr().getValues().contains("Rossini")));

        operations.clear();
        operations.add(getOperation("name", "givenName", PatchOp.add, "Gioacchino"));
        operations.add(getOperation("name", "familyName", PatchOp.remove, null));
        scimPatchOp.setOperations(operations);
        result = dataBinder.toUserUpdate(new UserTO(), scimPatchOp);
        assertNotNull(result);
        assertNull(result.getRight());
        assertNotNull(result.getLeft());
        assertEquals(2, result.getLeft().getPlainAttrs().size());
        assertTrue(result.getLeft().getPlainAttrs().stream().anyMatch(attrPatch ->
                PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                        && attrPatch.getAttr().getSchema().equals("firstname")
                        && attrPatch.getAttr().getValues().contains("Gioacchino")));
        assertTrue(result.getLeft().getPlainAttrs().stream().anyMatch(attrPatch ->
                PatchOperation.DELETE.equals(attrPatch.getOperation())
                        && attrPatch.getAttr().getSchema().equals("surname")
                        &&  attrPatch.getAttr().getValues().isEmpty()));

        operations.clear();
        operations.add(getOperation("name", "familyName", PatchOp.add, "Verdi"));
        operations.add(getOperation("name", "givenName", PatchOp.replace, "Giuseppe"));
        operations.add(getOperation("userName", null, PatchOp.add, "gverdi"));
        scimPatchOp.setOperations(operations);
        result = dataBinder.toUserUpdate(new UserTO(), scimPatchOp);
        assertNotNull(result);
        assertNull(result.getRight());
        assertNotNull(result.getLeft());
        assertEquals(2, result.getLeft().getPlainAttrs().size());
        assertEquals(PatchOperation.ADD_REPLACE, result.getLeft().getUsername().getOperation());
        assertEquals("gverdi", result.getLeft().getUsername().getValue());
        assertTrue(result.getLeft().getPlainAttrs().stream().anyMatch(attrPatch ->
                PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                        && attrPatch.getAttr().getSchema().equals("surname")
                        && attrPatch.getAttr().getValues().contains("Verdi")));
        assertTrue(result.getLeft().getPlainAttrs().stream().anyMatch(attrPatch ->
                PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                        && attrPatch.getAttr().getSchema().equals("firstname")
                        && attrPatch.getAttr().getValues().contains("Giuseppe")));

        operations.clear();
        operations.add(getOperation("name", "familyName", PatchOp.replace, "Puccini"));
        operations.add(getOperation("name", "givenName", PatchOp.remove, null));
        operations.add(getOperation("active", null, PatchOp.add, "True"));
        scimPatchOp.setOperations(operations);
        result = dataBinder.toUserUpdate(new UserTO(), scimPatchOp);
        assertNotNull(result);
        assertNotNull(result.getRight());
        assertTrue(result.getRight().isOnSyncope());
        assertEquals(StatusRType.REACTIVATE, result.getRight().getType());
        assertNotNull(result.getLeft());
        assertEquals(2, result.getLeft().getPlainAttrs().size());
        assertTrue(result.getLeft().getPlainAttrs().stream().anyMatch(attrPatch ->
                PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                        && attrPatch.getAttr().getSchema().equals("surname")
                        && attrPatch.getAttr().getValues().contains("Puccini")));
        assertTrue(result.getLeft().getPlainAttrs().stream().anyMatch(attrPatch ->
                PatchOperation.DELETE.equals(attrPatch.getOperation())
                        && attrPatch.getAttr().getSchema().equals("firstname")
                        &&  attrPatch.getAttr().getValues().isEmpty()));

        UserTO userTO = new UserTO();
        userTO.setUsername("bellini");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.getPlainAttrs().add(new Attr.Builder("surname").value("Bellini").build());
        SCIMUser scimUser = new SCIMUser(
                UUID.randomUUID().toString(), List.of(Resource.User.schema()), null, "bellini", true);
        scimUser.setName(new SCIMUserName());
        scimUser.getName().setFamilyName("Bellini");
        SCIMPatchOperation operation = new SCIMPatchOperation();
        operation.setOp(PatchOp.add);
        operation.setValue(List.of(scimUser));
        operations.clear();
        operations.add(operation);
        scimPatchOp.setOperations(operations);
        result = dataBinder.toUserUpdate(userTO, scimPatchOp);
        assertNotNull(result);
        assertNull(result.getRight());
        assertTrue(result.getLeft().isEmpty());

        userTO.setUsername("rossini");
        userTO.getPlainAttrs().clear();
        userTO.getPlainAttrs().add(new Attr.Builder("surname").value("Rossini").build());
        result = dataBinder.toUserUpdate(userTO, scimPatchOp);
        assertNotNull(result);
        assertNull(result.getRight());
        assertNotNull(result.getLeft());
        assertEquals(PatchOperation.ADD_REPLACE, result.getLeft().getUsername().getOperation());
        assertEquals("bellini", result.getLeft().getUsername().getValue());
        assertEquals(1, result.getLeft().getPlainAttrs().size());
        assertTrue(result.getLeft().getPlainAttrs().stream().anyMatch(attrPatch ->
                PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                        && attrPatch.getAttr().getSchema().equals("surname")
                        && attrPatch.getAttr().getValues().contains("Bellini")));

        userTO.setUsername("bellini");
        userTO.setSuspended(true);
        userTO.getPlainAttrs().clear();
        userTO.getPlainAttrs().add(new Attr.Builder("surname").value("Bellini").build());
        scimUser.getName().setGivenName("Gioacchino");
        scimUser.getRoles().add(new Value("User reviewer"));
        result = dataBinder.toUserUpdate(userTO, scimPatchOp);
        assertNotNull(result);
        assertNotNull(result.getRight());
        assertTrue(result.getRight().isOnSyncope());
        assertEquals(StatusRType.REACTIVATE, result.getRight().getType());
        assertNotNull(result.getLeft());
        assertNull(result.getLeft().getUsername());
        assertEquals(1, result.getLeft().getPlainAttrs().size());
        assertTrue(result.getLeft().getPlainAttrs().stream().anyMatch(attrPatch ->
                PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                        && attrPatch.getAttr().getSchema().equals("firstname")
                        && attrPatch.getAttr().getValues().contains("Gioacchino")));
        assertEquals(1, result.getLeft().getRoles().size());
        assertTrue(result.getLeft().getRoles().stream().anyMatch(role ->
                PatchOperation.ADD_REPLACE.equals(role.getOperation())
                        && role.getValue().equals("User reviewer")));
    }

    private SCIMPatchOperation getOperation(
            final String attribute, final String sub, final PatchOp op, final String value) {
        SCIMPatchOperation operation = new SCIMPatchOperation();
        SCIMPatchPath scimPatchPath = new SCIMPatchPath();
        scimPatchPath.setAttribute(attribute);
        scimPatchPath.setSub(sub);
        operation.setOp(op);
        operation.setPath(scimPatchPath);
        operation.setValue(value == null ? List.of() : List.of(value));
        return operation;
    }
}
