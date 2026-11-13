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
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.RelationshipUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnyObjectDataBinderTest extends AbstractDataBinderTest {

    private static final String PRINTER_KEY = "fc6dbc3a-6c07-4965-8781-921e7401a4a5";

    private static final String OTHER_PRINTER_KEY = "8559d14d-58c2-46eb-a2d4-a7d35161e8f8";

    private static final String THIRD_PRINTER_KEY = "9e1d130c-d6a3-48b1-98b3-182477ed0688";

    private static final String VERDI_KEY = "74cd8ece-715a-44a4-a736-e17b46c4e7e6";

    private static final String GROUP_KEY = "f779c0d4-633b-4be5-8f57-32eb478a3ca5";

    @Autowired
    private AnyObjectDataBinder binder;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private EncryptorManager encryptorManager;

    @Autowired
    private EntityFactory entityFactory;

    @Test
    void get() {
        AnyObjectTO printer = binder.getAnyObjectTO(PRINTER_KEY);
        assertEquals(PRINTER_KEY, printer.getKey());
        assertEquals("HP LJ 1300n", printer.getName());
        assertEquals("PRINTER", printer.getType());
        assertEquals(SyncopeConstants.ROOT_REALM, printer.getRealm());
        assertEquals("Canon MFC8030", printer.getPlainAttr("model").orElseThrow().getValues().getFirst());

        AnyObjectTO noDetails = binder.getAnyObjectTO(anyObjectDAO.findById(PRINTER_KEY).orElseThrow(), false);
        assertEquals(PRINTER_KEY, noDetails.getKey());
    }

    @Test
    void createFull() {
        AnyObjectCR anyObjectCR = new AnyObjectCR.Builder("/even/two", "PRINTER", "fullPrinter").
                uManager(VERDI_KEY).
                auxClasses("other", "csv", "not-existing").
                plainAttr(new Attr.Builder("model").value("Full model").build()).
                plainAttr(new Attr.Builder("location").value("3rd floor").build()).
                plainAttr(new Attr.Builder("ctype").value("G").build()).
                plainAttr(new Attr.Builder("aLong").value("42").build()).
                plainAttr(new Attr.Builder("cool").value("true").build()).
                plainAttr(new Attr.Builder("gender").value("M").build()).
                plainAttr(new Attr.Builder("loginDate").values(List.of("2020-01-01", "2021-02-02")).build()).
                plainAttr(new Attr.Builder("uselessReadonly").value("ignored").build()).
                plainAttr(new Attr.Builder("not-existing").value("ignored").build()).
                resource("ws-target-resource-1").
                resource("not-existing").
                relationship(new RelationshipTO.Builder("inclusion").
                        otherEnd(OTHER_PRINTER_KEY).
                        build()).
                membership(new MembershipTO.Builder(GROUP_KEY).
                        plainAttr(new Attr.Builder("obscure").value("membership secret").build()).
                        build()).
                build();

        AnyObject anyObject = entityFactory.newEntity(AnyObject.class);
        binder.create(anyObject, anyObjectCR);
        anyObject = anyObjectDAO.save(anyObject);
        entityManager.flush();

        AnyObject actual = anyObjectDAO.findById(anyObject.getKey()).orElseThrow();
        assertEquals("fullPrinter", actual.getName());
        assertEquals("PRINTER", actual.getType().getKey());
        assertEquals("/even/two", actual.getRealm().getFullPath());
        assertEquals(VERDI_KEY, actual.getuManager().getKey());
        assertNull(actual.getgManager());

        assertEquals(2, actual.getAuxClasses().size());
        assertTrue(actual.getAuxClasses().stream().anyMatch(auxClass -> "other".equals(auxClass.getKey())));
        assertTrue(actual.getAuxClasses().stream().anyMatch(auxClass -> "csv".equals(auxClass.getKey())));

        assertEquals("Full model", actual.getPlainAttr("model").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("3rd floor", actual.getPlainAttr("location").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("G", actual.getPlainAttr("ctype").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("42", actual.getPlainAttr("aLong").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("true", actual.getPlainAttr("cool").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("M", actual.getPlainAttr("gender").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals(2, actual.getPlainAttr("loginDate").orElseThrow().getValuesAsStrings().size());
        assertTrue(actual.getPlainAttr("uselessReadonly").isEmpty());
        assertTrue(actual.getPlainAttr("not-existing").isEmpty());

        assertEquals(1, actual.getResources().size());
        assertEquals("ws-target-resource-1", actual.getResources().getFirst().getKey());

        assertNotNull(actual.getRelationship(
                relationshipTypeDAO.findById("inclusion").orElseThrow(), OTHER_PRINTER_KEY).orElseThrow());

        assertNotNull(actual.getMembership(GROUP_KEY).orElseThrow());
        assertTrue(encryptorManager.getInstance().verify(
                "membership secret",
                plainSchemaDAO.findById("obscure").orElseThrow().getCipherAlgorithm(),
                actual.getPlainAttr("obscure", actual.getMembership(GROUP_KEY).orElseThrow()).
                        orElseThrow().getValuesAsStrings().getFirst()));
    }

    @Test
    void updateFull() {
        AnyObjectUR anyObjectUR = new AnyObjectUR.Builder(PRINTER_KEY).
                name(new StringReplacePatchItem.Builder().value("HP LJ 1300n updated").build()).
                realm(new StringReplacePatchItem.Builder().value("/even/two").build()).
                auxClass(new StringPatchItem.Builder().value("other").operation(PatchOperation.ADD_REPLACE).build()).
                auxClass(new StringPatchItem.Builder().value("csv").operation(PatchOperation.ADD_REPLACE).build()).
                auxClass(new StringPatchItem.Builder().value("not-existing").
                        operation(PatchOperation.DELETE).
                        build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("model").value("Updated model").build()).build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("location").build()).
                        operation(PatchOperation.DELETE).
                        build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("ctype").value("F").build()).build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("aLong").value("42").build()).build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("cool").value("false").build()).build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("gender").value("F").build()).build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("not-existing").value("ignored").build()).build()).
                resource(new StringPatchItem.Builder().
                        value("ws-target-resource-2").
                        operation(PatchOperation.ADD_REPLACE).
                        build()).
                resource(new StringPatchItem.Builder().
                        value("not-existing").
                        operation(PatchOperation.ADD_REPLACE).
                        build()).
                build();

        PropagationByResource<String> propByRes = binder.update(
                anyObjectDAO.findById(PRINTER_KEY).orElseThrow(), anyObjectUR);
        assertNotNull(propByRes);
        entityManager.flush();

        AnyObject actual = anyObjectDAO.findById(PRINTER_KEY).orElseThrow();
        assertEquals("HP LJ 1300n updated", actual.getName());
        assertEquals("/even/two", actual.getRealm().getFullPath());
        assertTrue(actual.getAuxClasses().stream().anyMatch(auxClass -> "other".equals(auxClass.getKey())));
        assertTrue(actual.getAuxClasses().stream().anyMatch(auxClass -> "csv".equals(auxClass.getKey())));
        assertEquals("Updated model", actual.getPlainAttr("model").orElseThrow().getValuesAsStrings().getFirst());
        assertTrue(actual.getPlainAttr("location").isEmpty());
        assertEquals("F", actual.getPlainAttr("ctype").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("42", actual.getPlainAttr("aLong").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("false", actual.getPlainAttr("cool").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("F", actual.getPlainAttr("gender").orElseThrow().getValuesAsStrings().getFirst());
        assertTrue(actual.getResources().stream().
                anyMatch(resource -> "ws-target-resource-2".equals(resource.getKey())));

        AnyObjectTO updated = binder.getAnyObjectTO(actual, true);
        assertEquals("HP LJ 1300n updated", updated.getName());
        assertEquals("/even/two", updated.getRealm());
    }

    @Test
    void updateMembershipsAndRelationships() {
        AnyObjectUR addMembership = new AnyObjectUR.Builder(PRINTER_KEY).
                relationship(new RelationshipUR.Builder("inclusion").
                        otherEnd(THIRD_PRINTER_KEY).
                        build()).
                relationship(new RelationshipUR.Builder("inclusion").
                        otherEnd(OTHER_PRINTER_KEY).
                        operation(PatchOperation.DELETE).
                        build()).
                membership(new MembershipUR.Builder(GROUP_KEY).
                        plainAttr(new Attr.Builder("obscure").value("membership secret").build()).
                        build()).
                build();

        binder.update(anyObjectDAO.findById(PRINTER_KEY).orElseThrow(), addMembership);
        entityManager.flush();

        AnyObject updated = anyObjectDAO.findById(PRINTER_KEY).orElseThrow();
        assertNotNull(updated.getRelationship(
                relationshipTypeDAO.findById("inclusion").orElseThrow(), THIRD_PRINTER_KEY).orElseThrow());
        assertTrue(updated.getRelationship(
                relationshipTypeDAO.findById("inclusion").orElseThrow(), OTHER_PRINTER_KEY).isEmpty());
        assertNotNull(updated.getMembership(GROUP_KEY).orElseThrow());

        AnyObjectUR removeMembership = new AnyObjectUR.Builder(PRINTER_KEY).
                membership(new MembershipUR.Builder(GROUP_KEY).
                        operation(PatchOperation.DELETE).
                        build()).
                build();
        binder.update(anyObjectDAO.findById(PRINTER_KEY).orElseThrow(), removeMembership);
        entityManager.flush();

        AnyObject cleared = anyObjectDAO.findById(PRINTER_KEY).orElseThrow();
        assertTrue(cleared.getMembership(GROUP_KEY).isEmpty());
    }

    @Test
    void updateManagers() {
        AnyObjectUR setUserManager = new AnyObjectUR.Builder(PRINTER_KEY).
                uManager(new StringReplacePatchItem.Builder().value(VERDI_KEY).build()).
                build();
        binder.update(anyObjectDAO.findById(PRINTER_KEY).orElseThrow(), setUserManager);
        entityManager.flush();

        AnyObject managedByUser = anyObjectDAO.findById(PRINTER_KEY).orElseThrow();
        assertEquals(VERDI_KEY, managedByUser.getuManager().getKey());
        assertNull(managedByUser.getgManager());

        AnyObjectUR setGroupManager = new AnyObjectUR.Builder(PRINTER_KEY).
                uManager(new StringReplacePatchItem.Builder().value(null).build()).
                gManager(new StringReplacePatchItem.Builder().value(GROUP_KEY).build()).
                build();
        binder.update(anyObjectDAO.findById(PRINTER_KEY).orElseThrow(), setGroupManager);
        entityManager.flush();

        AnyObject managedByGroup = anyObjectDAO.findById(PRINTER_KEY).orElseThrow();
        assertNull(managedByGroup.getuManager());
        assertEquals(GROUP_KEY, managedByGroup.getgManager().getKey());
    }

    @Test
    void invalidCreate() {
        assertThrows(SyncopeClientException.class, () -> binder.create(
                entityFactory.newEntity(AnyObject.class),
                new AnyObjectCR.Builder("/even/two", "not-existing", "test").build()));
        assertThrows(SyncopeClientException.class, () -> binder.create(
                entityFactory.newEntity(AnyObject.class),
                new AnyObjectCR.Builder("/even/three", "PRINTER", "test").build()));
    }
}
