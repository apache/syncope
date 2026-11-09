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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GroupDataBinderTest extends AbstractDataBinderTest {

    private static final String CITIZEN_KEY = "29f96485-729e-4d31-88a1-6fc60e4677f3";

    private static final String VERDI_KEY = "74cd8ece-715a-44a4-a736-e17b46c4e7e6";

    @Autowired
    private GroupDataBinder binder;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Test
    void get() {
        GroupTO group = binder.getGroupTO(CITIZEN_KEY);
        assertEquals(CITIZEN_KEY, group.getKey());
        assertEquals("citizen", group.getName());
        assertEquals(SyncopeConstants.ROOT_REALM, group.getRealm());

        GroupTO root = binder.getGroupTO("37d15e4c-cdc1-460b-a591-8505c8133806");
        assertEquals("root", root.getName());
        assertEquals("niceIcon", root.getPlainAttr("icon").orElseThrow().getValues().getFirst());
    }

    @Test
    void createFull() {
        TypeExtensionTO userTypeExtension = new TypeExtensionTO();
        userTypeExtension.setAnyType(AnyTypeKind.USER.name());
        userTypeExtension.getAuxClasses().addAll(List.of("other", "not-existing"));

        TypeExtensionTO printerTypeExtension = new TypeExtensionTO();
        printerTypeExtension.setAnyType("PRINTER");
        printerTypeExtension.getAuxClasses().addAll(List.of("csv", "not-existing"));

        GroupCR groupCR = new GroupCR.Builder("/even", "fullGroup").
                uManager(VERDI_KEY).
                auxClasses("other", "csv", "not-existing").
                plainAttr(new Attr.Builder("icon").value("fullIcon").build()).
                plainAttr(new Attr.Builder("show").value("true").build()).
                plainAttr(new Attr.Builder("title").values(List.of("title1", "title2")).build()).
                plainAttr(new Attr.Builder("originalName").value("originalFullGroup").build()).
                plainAttr(new Attr.Builder("rderived_sx").value("sx").build()).
                plainAttr(new Attr.Builder("rderived_dx").value("dx").build()).
                plainAttr(new Attr.Builder("ctype").value("G").build()).
                plainAttr(new Attr.Builder("aLong").value("42").build()).
                plainAttr(new Attr.Builder("cool").value("true").build()).
                plainAttr(new Attr.Builder("gender").value("F").build()).
                plainAttr(new Attr.Builder("loginDate").values(List.of("2020-01-01", "2021-02-02")).build()).
                plainAttr(new Attr.Builder("uselessReadonly").value("ignored").build()).
                plainAttr(new Attr.Builder("not-existing").value("ignored").build()).
                resource("ws-target-resource-1").
                resource("not-existing").
                typeExtension(userTypeExtension).
                typeExtension(printerTypeExtension).
                build();

        Group group = entityFactory.newEntity(Group.class);
        binder.create(group, groupCR);
        group = groupDAO.save(group);
        entityManager.flush();

        Group actual = groupDAO.findById(group.getKey()).orElseThrow();
        assertEquals("fullGroup", actual.getName());
        assertEquals("/even", actual.getRealm().getFullPath());
        assertEquals(VERDI_KEY, actual.getuManager().getKey());
        assertNull(actual.getgManager());

        assertEquals(2, actual.getAuxClasses().size());
        assertTrue(actual.getAuxClasses().stream().anyMatch(auxClass -> "other".equals(auxClass.getKey())));
        assertTrue(actual.getAuxClasses().stream().anyMatch(auxClass -> "csv".equals(auxClass.getKey())));

        assertEquals("fullIcon", actual.getPlainAttr("icon").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("true", actual.getPlainAttr("show").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals(2, actual.getPlainAttr("title").orElseThrow().getValuesAsStrings().size());
        assertEquals("originalFullGroup",
                actual.getPlainAttr("originalName").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("G", actual.getPlainAttr("ctype").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals(2, actual.getPlainAttr("loginDate").orElseThrow().getValuesAsStrings().size());
        assertTrue(actual.getPlainAttr("uselessReadonly").isEmpty());
        assertTrue(actual.getPlainAttr("not-existing").isEmpty());

        assertEquals(1, actual.getResources().size());
        assertEquals("ws-target-resource-1", actual.getResources().getFirst().getKey());

        GroupTO created = binder.getGroupTO(actual, true);
        assertEquals(List.of("other"), created.getTypeExtension(AnyTypeKind.USER.name()).orElseThrow().getAuxClasses());
        assertEquals(List.of("csv"), created.getTypeExtension("PRINTER").orElseThrow().getAuxClasses());
    }

    @Test
    void updateFull() {
        Group group = groupDAO.findById("37d15e4c-cdc1-460b-a591-8505c8133806").orElseThrow();

        TypeExtensionTO userTypeExtension = new TypeExtensionTO();
        userTypeExtension.setAnyType(AnyTypeKind.USER.name());
        userTypeExtension.getAuxClasses().addAll(List.of("csv", "not-existing"));

        GroupUR groupUR = new GroupUR.Builder(group.getKey()).
                name(new StringReplacePatchItem.Builder().value("root2").build()).
                realm(new StringReplacePatchItem.Builder().value("/even/two").build()).
                uManager(new StringReplacePatchItem.Builder().value(VERDI_KEY).build()).
                auxClass(new StringPatchItem.Builder().value("csv").operation(PatchOperation.ADD_REPLACE).build()).
                auxClass(new StringPatchItem.Builder().value("other").operation(PatchOperation.ADD_REPLACE).build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("icon").value("updatedIcon").build()).build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("originalName").build()).
                        operation(PatchOperation.DELETE).
                        build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("aLong").value("42").build()).build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("cool").value("false").build()).build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("not-existing").value("ignored").build()).build()).
                resource(new StringPatchItem.Builder().
                        value("ws-target-resource-1").
                        operation(PatchOperation.ADD_REPLACE).
                        build()).
                resource(new StringPatchItem.Builder().
                        value("not-existing").
                        operation(PatchOperation.ADD_REPLACE).
                        build()).
                typeExtension(userTypeExtension).
                build();

        PropagationByResource<String> propByRes = binder.update(group, groupUR);
        assertNotNull(propByRes);
        entityManager.flush();

        Group actual = groupDAO.findById(group.getKey()).orElseThrow();
        assertEquals("root2", actual.getName());
        assertEquals("/even/two", actual.getRealm().getFullPath());
        assertEquals(VERDI_KEY, actual.getuManager().getKey());
        assertNull(actual.getgManager());
        assertEquals("updatedIcon", actual.getPlainAttr("icon").orElseThrow().getValuesAsStrings().getFirst());
        assertTrue(actual.getPlainAttr("originalName").isEmpty());
        assertEquals("42", actual.getPlainAttr("aLong").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("false", actual.getPlainAttr("cool").orElseThrow().getValuesAsStrings().getFirst());
        assertTrue(actual.getResources().stream().
                anyMatch(resource -> "ws-target-resource-1".equals(resource.getKey())));

        GroupTO updated = binder.getGroupTO(actual, true);
        assertEquals("root2", updated.getName());
        assertEquals("/even/two", updated.getRealm());
        assertEquals(List.of("csv"), updated.getTypeExtension(AnyTypeKind.USER.name()).orElseThrow().getAuxClasses());
    }

    @Test
    void updateAuxClassRemoval() {
        Group group = groupDAO.findById("37d15e4c-cdc1-460b-a591-8505c8133806").orElseThrow();

        GroupUR addAuxClass = new GroupUR.Builder(group.getKey()).
                auxClass(new StringPatchItem.Builder().
                        value("other").
                        operation(PatchOperation.ADD_REPLACE).
                        build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("ctype").value("G").build()).build()).
                build();
        binder.update(group, addAuxClass);
        entityManager.flush();

        Group withAuxClass = groupDAO.findById(group.getKey()).orElseThrow();
        assertTrue(withAuxClass.getAuxClasses().stream().
                anyMatch(auxClass -> "other".equals(auxClass.getKey())));
        assertEquals("G", withAuxClass.getPlainAttr("ctype").orElseThrow().getValuesAsStrings().getFirst());

        GroupUR removeAuxClass = new GroupUR.Builder(group.getKey()).
                auxClass(new StringPatchItem.Builder().
                        value("other").
                        operation(PatchOperation.DELETE).
                        build()).
                build();
        binder.update(withAuxClass, removeAuxClass);
        entityManager.flush();

        Group actual = groupDAO.findById(group.getKey()).orElseThrow();
        assertTrue(actual.getAuxClasses().isEmpty());
        assertTrue(actual.getPlainAttr("ctype").isEmpty());
    }

    @Test
    void invalidCreate() {
        assertThrows(SyncopeClientException.class, () -> binder.create(
                entityFactory.newEntity(Group.class), new GroupCR.Builder("/even/three", "testGroup").build()));
    }

    @Test
    void invalidUpdate() {
        Group group = groupDAO.findById(CITIZEN_KEY).orElseThrow();

        GroupUR groupUR = new GroupUR.Builder(group.getKey()).
                realm(new StringReplacePatchItem.Builder().value("/even/three").build()).
                build();

        PropagationByResource<String> propByRes = binder.update(group, groupUR);
        assertNotNull(propByRes);

        Group actual = groupDAO.findById(CITIZEN_KEY).orElseThrow();
        assertEquals(SyncopeConstants.ROOT_REALM, actual.getRealm().getFullPath());
    }
}
