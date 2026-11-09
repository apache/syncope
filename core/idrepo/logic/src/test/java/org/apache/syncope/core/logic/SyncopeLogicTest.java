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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.content.ContentExporter;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.RelationshipTypeDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

public class SyncopeLogicTest {

    private RealmSearchDAO realmSearchDAO;

    private AnyTypeDAO anyTypeDAO;

    private GroupDAO groupDAO;

    private RelationshipTypeDAO relationshipTypeDAO;

    private AnySearchDAO anySearchDAO;

    private GroupDataBinder groupDataBinder;

    private RelationshipTypeDataBinder relationshipTypeDataBinder;

    private ContentExporter exporter;

    private SyncopeLogic logic;

    @BeforeEach
    void setUp() {
        realmSearchDAO = mock(RealmSearchDAO.class);
        anyTypeDAO = mock(AnyTypeDAO.class);
        groupDAO = mock(GroupDAO.class);
        relationshipTypeDAO = mock(RelationshipTypeDAO.class);
        anySearchDAO = mock(AnySearchDAO.class);
        groupDataBinder = mock(GroupDataBinder.class);
        relationshipTypeDataBinder = mock(RelationshipTypeDataBinder.class);
        exporter = mock(ContentExporter.class);

        logic = new SyncopeLogic(
                realmSearchDAO,
                anyTypeDAO,
                groupDAO,
                relationshipTypeDAO,
                anySearchDAO,
                groupDataBinder,
                relationshipTypeDataBinder,
                exporter);
    }

    @Test
    void searchAssignableGroupsMissingRealm() {
        doReturn(Optional.empty()).when(realmSearchDAO).findByFullPath("/missing");

        assertThrows(NotFoundException.class,
                () -> logic.searchAssignableGroups("/missing", null, Pageable.unpaged()));
    }

    @Test
    void searchAssignableGroups() {
        Realm realm = mock(Realm.class);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(anySearchDAO.count(eq(realm), eq(true), any(), any(SearchCond.class), eq(AnyTypeKind.GROUP))).
                thenReturn(0L);
        when(anySearchDAO.search(
                eq(realm), eq(true), any(), any(SearchCond.class), any(Pageable.class), eq(AnyTypeKind.GROUP))).
                thenReturn(List.of());

        assertEquals(0, logic.searchAssignableGroups(
                SyncopeConstants.ROOT_REALM, "*", Pageable.unpaged()).getTotalElements());
    }

    @Test
    void readUserGroupTypeExtensionMissingGroup() {
        doReturn(Optional.empty()).when(groupDAO).findByName("missing");

        assertThrows(NotFoundException.class, () -> logic.readUserGroupTypeExtension("missing"));
    }

    @Test
    void readUserRelationshipTypeExtensionMissing() {
        doReturn(Optional.empty()).when(relationshipTypeDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.readUserRelationshipTypeExtension("missing"));
    }

    @Test
    void exportInternalStorageContent() throws Exception {
        OutputStream os = mock(OutputStream.class);
        doThrow(new IllegalStateException("test")).when(exporter).
                export("Master", 1, os, new String[] { "element" });

        logic.exportInternalStorageContent(1, os, List.of("element"));

        verify(exporter).export("Master", 1, os, new String[] { "element" });
    }

    @Test
    void exportInternalStorageContentWithThreshold() throws Exception {
        OutputStream os = mock(OutputStream.class);

        logic.exportInternalStorageContent(0, os, List.of());

        verify(exporter).export(eq("Master"), anyInt(), eq(os), any(String[].class));
    }

    @Test
    void resolveReference() {
        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveBeanReference(
                SyncopeLogic.class.getMethod("searchAssignableGroups", String.class, String.class, Pageable.class),
                SyncopeConstants.ROOT_REALM, null, Pageable.unpaged()));
    }
}
