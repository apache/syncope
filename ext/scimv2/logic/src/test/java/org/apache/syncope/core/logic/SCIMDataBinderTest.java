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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.scim.SCIMUserNameConf;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.provisioning.api.jexl.EmptyClassLoader;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.ext.scimv2.api.data.Group;
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

    private GroupDAO groupDAO;

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
        groupDAO = mock(GroupDAO.class);

        JexlEngine jexlEngine = new JexlBuilder().
                loader(new EmptyClassLoader()).
                permissions(JexlPermissions.RESTRICTED.compose("java.time.*", "org.apache.syncope.*")).
                cache(512).
                silent(false).
                strict(false).
                create();
        JexlTools jexlTools = new JexlTools(jexlEngine);

        dataBinder = new SCIMDataBinder(scimConfManager, userLogic, authDataAccessor, groupDAO, jexlTools);
    }

    private static SCIMPatchOperation operation(
            final String attribute,
            final String sub,
            final PatchOp op,
            final String value) {

        SCIMPatchOperation operation = new SCIMPatchOperation();
        SCIMPatchPath scimPatchPath = new SCIMPatchPath();
        scimPatchPath.setAttribute(attribute);
        scimPatchPath.setSub(sub);
        operation.setOp(op);
        operation.setPath(scimPatchPath);
        Optional.ofNullable(value).ifPresent(v -> operation.getValue().add(v));
        return operation;
    }

    @ParameterizedTest
    @MethodSource("getValue")
    void toUserUpdateActive(final String value) {
        SCIMPatchOp scimPatchOp = new SCIMPatchOp();
        scimPatchOp.getOperations().add(operation("active", null, PatchOp.add, value));
        Pair<List<UserUR>, StatusR> result = dataBinder.toUserUpdate(new UserTO(), scimPatchOp);
        assertNotNull(result);
        assertEquals(1, result.getLeft().size());
        assertTrue(result.getLeft().getFirst().isEmpty());
        assertNotNull(result.getRight());
        assertTrue(result.getRight().isOnSyncope());
        assertEquals(
                Boolean.parseBoolean(value) ? StatusRType.REACTIVATE : StatusRType.SUSPEND,
                result.getRight().getType());
    }

    @Test
    void toUserUpdate() {
        SCIMPatchOp scimPatchOp = new SCIMPatchOp();
        scimPatchOp.getOperations().add(operation("name", "familyName", PatchOp.add, "Rossini"));

        Pair<List<UserUR>, StatusR> result = dataBinder.toUserUpdate(new UserTO(), scimPatchOp);
        assertNotNull(result);
        assertNull(result.getRight());
        assertEquals(1, result.getLeft().size());
        assertEquals(1, result.getLeft().getFirst().getPlainAttrs().size());
        assertTrue(result.getLeft().getFirst().getPlainAttrs().stream().anyMatch(attrPatch
                -> PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                && attrPatch.getAttr().getSchema().equals("surname")
                && attrPatch.getAttr().getValues().contains("Rossini")));

        scimPatchOp.getOperations().clear();
        scimPatchOp.getOperations().add(operation("name", "givenName", PatchOp.add, "Gioacchino"));
        scimPatchOp.getOperations().add(operation("name", "familyName", PatchOp.remove, null));
        result = dataBinder.toUserUpdate(new UserTO(), scimPatchOp);
        assertNotNull(result);
        assertNull(result.getRight());
        assertEquals(1, result.getLeft().size());
        assertEquals(2, result.getLeft().getFirst().getPlainAttrs().size());
        assertTrue(result.getLeft().getFirst().getPlainAttrs().stream().anyMatch(attrPatch
                -> PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                && attrPatch.getAttr().getSchema().equals("firstname")
                && attrPatch.getAttr().getValues().contains("Gioacchino")));
        assertTrue(result.getLeft().getFirst().getPlainAttrs().stream().anyMatch(attrPatch
                -> PatchOperation.DELETE.equals(attrPatch.getOperation())
                && attrPatch.getAttr().getSchema().equals("surname")
                && attrPatch.getAttr().getValues().isEmpty()));

        scimPatchOp.getOperations().clear();
        scimPatchOp.getOperations().add(operation("name", "familyName", PatchOp.add, "Verdi"));
        scimPatchOp.getOperations().add(operation("name", "givenName", PatchOp.replace, "Giuseppe"));
        scimPatchOp.getOperations().add(operation("userName", null, PatchOp.add, "gverdi"));
        result = dataBinder.toUserUpdate(new UserTO(), scimPatchOp);
        assertNotNull(result);
        assertNull(result.getRight());
        assertEquals(1, result.getLeft().size());
        assertEquals(2, result.getLeft().getFirst().getPlainAttrs().size());
        assertEquals(PatchOperation.ADD_REPLACE, result.getLeft().getFirst().getUsername().getOperation());
        assertEquals("gverdi", result.getLeft().getFirst().getUsername().getValue());
        assertTrue(result.getLeft().getFirst().getPlainAttrs().stream().anyMatch(attrPatch
                -> PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                && attrPatch.getAttr().getSchema().equals("surname")
                && attrPatch.getAttr().getValues().contains("Verdi")));
        assertTrue(result.getLeft().getFirst().getPlainAttrs().stream().anyMatch(attrPatch
                -> PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                && attrPatch.getAttr().getSchema().equals("firstname")
                && attrPatch.getAttr().getValues().contains("Giuseppe")));

        scimPatchOp.getOperations().clear();
        scimPatchOp.getOperations().add(operation("name", "familyName", PatchOp.replace, "Puccini"));
        scimPatchOp.getOperations().add(operation("name", "givenName", PatchOp.remove, null));
        scimPatchOp.getOperations().add(operation("active", null, PatchOp.add, "True"));
        result = dataBinder.toUserUpdate(new UserTO(), scimPatchOp);
        assertNotNull(result);
        assertNotNull(result.getRight());
        assertTrue(result.getRight().isOnSyncope());
        assertEquals(StatusRType.REACTIVATE, result.getRight().getType());
        assertEquals(1, result.getLeft().size());
        assertEquals(2, result.getLeft().getFirst().getPlainAttrs().size());
        assertTrue(result.getLeft().getFirst().getPlainAttrs().stream().anyMatch(
                attrPatch -> PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                && attrPatch.getAttr().getSchema().equals("surname")
                && attrPatch.getAttr().getValues().contains("Puccini")));
        assertTrue(result.getLeft().getFirst().getPlainAttrs().stream().anyMatch(
                attrPatch -> PatchOperation.DELETE.equals(attrPatch.getOperation())
                && attrPatch.getAttr().getSchema().equals("firstname")
                && attrPatch.getAttr().getValues().isEmpty()));

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
        operation.getValue().add(scimUser);
        scimPatchOp.getOperations().clear();
        scimPatchOp.getOperations().add(operation);
        result = dataBinder.toUserUpdate(userTO, scimPatchOp);
        assertNotNull(result);
        assertNull(result.getRight());
        assertEquals(1, result.getLeft().size());
        assertTrue(result.getLeft().getFirst().isEmpty());

        userTO.setUsername("rossini");
        userTO.getPlainAttrs().clear();
        userTO.getPlainAttrs().add(new Attr.Builder("surname").value("Rossini").build());
        result = dataBinder.toUserUpdate(userTO, scimPatchOp);
        assertNotNull(result);
        assertNull(result.getRight());
        assertEquals(1, result.getLeft().size());
        assertEquals(PatchOperation.ADD_REPLACE, result.getLeft().getFirst().getUsername().getOperation());
        assertEquals("bellini", result.getLeft().getFirst().getUsername().getValue());
        assertEquals(1, result.getLeft().getFirst().getPlainAttrs().size());
        assertTrue(result.getLeft().getFirst().getPlainAttrs().stream().
                anyMatch(attrPatch -> PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
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
        assertEquals(1, result.getLeft().size());
        assertNull(result.getLeft().getFirst().getUsername());
        assertEquals(1, result.getLeft().getFirst().getPlainAttrs().size());
        assertTrue(result.getLeft().getFirst().getPlainAttrs().stream().anyMatch(
                attrPatch -> PatchOperation.ADD_REPLACE.equals(attrPatch.getOperation())
                && attrPatch.getAttr().getSchema().equals("firstname")
                && attrPatch.getAttr().getValues().contains("Gioacchino")));
        assertEquals(1, result.getLeft().getFirst().getRoles().size());
        assertTrue(result.getLeft().getFirst().getRoles().stream().anyMatch(
                role -> PatchOperation.ADD_REPLACE.equals(role.getOperation())
                && role.getValue().equals("User reviewer")));
    }

    @Test
    void groups() {
        when(groupDAO.findById("37d15e4c-cdc1-460b-a591-8505c8133806")).thenAnswer(ic -> {
            org.apache.syncope.core.persistence.api.entity.group.Group syncopeGroup =
                    mock(org.apache.syncope.core.persistence.api.entity.group.Group.class);
            ExternalResource resource = mock(ExternalResource.class);
            Provision provision = mock(Provision.class);
            when(provision.getAnyType()).thenReturn(AnyTypeKind.USER.name());
            when(resource.getKey()).thenReturn("resource-ldap");
            when(resource.getProvisions()).thenAnswer(invocation -> List.of(provision));
            when(syncopeGroup.getResources()).thenAnswer(invocation -> List.of(resource));
            return Optional.of(syncopeGroup);
        });
        when(groupDAO.findById("29f96485-729e-4d31-88a1-6fc60e4677f3")).thenAnswer(ic -> {
            org.apache.syncope.core.persistence.api.entity.group.Group syncopeGroup =
                    mock(org.apache.syncope.core.persistence.api.entity.group.Group.class);
            ExternalResource resource = mock(ExternalResource.class);
            Provision provision = mock(Provision.class);
            when(provision.getAnyType()).thenReturn(AnyTypeKind.USER.name());
            when(resource.getKey()).thenReturn("resource-testdb");
            when(resource.getProvisions()).thenAnswer(invocation -> List.of(provision));
            when(syncopeGroup.getResources()).thenAnswer(invocation -> List.of(resource));
            return Optional.of(syncopeGroup);
        });
        when(groupDAO.findById("f779c0d4-633b-4be5-8f57-32eb478a3ca5")).thenAnswer(ic -> {
            org.apache.syncope.core.persistence.api.entity.group.Group syncopeGroup =
                    mock(org.apache.syncope.core.persistence.api.entity.group.Group.class);
            ExternalResource resource = mock(ExternalResource.class);
            Provision provision = mock(Provision.class);
            when(provision.getAnyType()).thenReturn(AnyTypeKind.USER.name());
            when(resource.getKey()).thenReturn("ws-target-resource-list-mappings-1");
            when(resource.getProvisions()).thenAnswer(invocation -> List.of(provision));

            ExternalResource resource2 = mock(ExternalResource.class);
            when(resource2.getKey()).thenReturn("ws-target-resource-list-mappings-2");
            when(resource2.getProvisions()).thenAnswer(invocation -> List.of(provision));
            when(syncopeGroup.getResources()).thenAnswer(invocation -> List.of(resource, resource2));
            return Optional.of(syncopeGroup);
        });

        UserTO userTO = new UserTO();

        Group group = new Group("37d15e4c-cdc1-460b-a591-8505c8133806", null, "root", null);
        SCIMUser scimUser = new SCIMUser(
                UUID.randomUUID().toString(), List.of(Resource.User.schema()), null, "bellini", true);
        scimUser.getGroups().add(group);
        group = new Group("29f96485-729e-4d31-88a1-6fc60e4677f3", null, "citizen", null);
        scimUser.getGroups().add(group);

        SCIMPatchOperation operation = new SCIMPatchOperation();
        operation.setOp(PatchOp.add);
        operation.getValue().clear();
        operation.getValue().add(scimUser);
        SCIMPatchOp scimPatchOp = new SCIMPatchOp();
        scimPatchOp.getOperations().add(operation);

        group = new Group("f779c0d4-633b-4be5-8f57-32eb478a3ca5", null, "otherchild", null);
        SCIMUser scimUser2 = new SCIMUser(
                UUID.randomUUID().toString(), List.of(Resource.User.schema()), null, "bellini", true);
        scimUser2.getGroups().add(group);
        SCIMPatchOperation operation2 = new SCIMPatchOperation();
        operation2.setOp(PatchOp.add);
        operation2.getValue().add(scimUser2);
        scimPatchOp.getOperations().add(operation2);

        Pair<List<UserUR>, StatusR> result = dataBinder.toUserUpdate(userTO, scimPatchOp);
        assertNotNull(result);
        assertNull(result.getRight());
        assertEquals(3, result.getLeft().size());
        assertTrue(result.getLeft().get(0).isEmpty());
        assertEquals(2, result.getLeft().get(1).getMemberships().size());
        assertTrue(result.getLeft().get(1).getMemberships().stream().anyMatch(
                membershipUR -> PatchOperation.ADD_REPLACE.equals(membershipUR.getOperation())
                && membershipUR.getGroup().equals("37d15e4c-cdc1-460b-a591-8505c8133806")));
        assertTrue(result.getLeft().get(1).getMemberships().stream().anyMatch(
                membershipUR -> PatchOperation.ADD_REPLACE.equals(membershipUR.getOperation())
                && membershipUR.getGroup().equals("29f96485-729e-4d31-88a1-6fc60e4677f3")));
        assertEquals(1, result.getLeft().get(2).getMemberships().size());
        assertTrue(result.getLeft().get(2).getMemberships().stream().anyMatch(
                membershipUR -> PatchOperation.ADD_REPLACE.equals(membershipUR.getOperation())
                && membershipUR.getGroup().equals("f779c0d4-633b-4be5-8f57-32eb478a3ca5")));
    }
}
