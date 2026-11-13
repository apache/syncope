/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the License.  You may not use this file except in compliance
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
package org.apache.syncope.core.persistence.api.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RealmCheckerTest {

    private static final String ALLOWED_SCHEMA = "allowed-schema";
    private static final String NOT_ALLOWED_SCHEMA = "not-allowed-schema";

    @Mock
    private PlainSchemaDAO plainSchemaDAO;

    private RealmChecker realmChecker;

    @BeforeEach
    void setUp() {
        realmChecker = new RealmChecker(plainSchemaDAO);
    }

    @Test
    void checkBeforeSaveWithNoAttrs() {
        final Realm realm = mock(Realm.class);
        final Set<AnyTypeClass> noAnyTypeClasses = new HashSet<>();
        final List<PlainAttr> noAttrs = new ArrayList<>();
        doReturn(noAnyTypeClasses).when(realm).getAnyTypeClasses();
        doReturn(noAttrs).when(realm).getPlainAttrs();

        assertDoesNotThrow(() -> realmChecker.checkBeforeSave(realm));
        verify(plainSchemaDAO, never()).existsPlainAttrUniqueValue(anyString(), any(), any());
    }

    @Test
    void checkBeforeSaveWithNullSchema() {
        final Realm realm = mock(Realm.class);
        final PlainAttr attr = mock(PlainAttr.class);
        final List<PlainAttr> attrs = new ArrayList<>();
        attrs.add(attr);
        doReturn(new HashSet<AnyTypeClass>()).when(realm).getAnyTypeClasses();
        when(realm.getPlainAttrs()).thenReturn(attrs);
        when(attr.getSchema()).thenReturn(null);
        when(attr.getUniqueValue()).thenReturn(null);

        assertDoesNotThrow(() -> realmChecker.checkBeforeSave(realm));
        verify(plainSchemaDAO, never()).existsPlainAttrUniqueValue(anyString(), any(), any());
    }

    @Test
    void checkBeforeSaveWithAllowedSchema() {
        final Realm realm = mock(Realm.class);
        final AnyTypeClass anyTypeClass = mock(AnyTypeClass.class);
        final PlainSchema plainSchema = mock(PlainSchema.class);
        final PlainAttr attr = mock(PlainAttr.class);
        final PlainAttrValue uniqueValue = mock(PlainAttrValue.class);
        final List<PlainSchema> plainSchemas = new ArrayList<>();
        plainSchemas.add(plainSchema);
        final Set<AnyTypeClass> anyTypeClasses = new HashSet<>();
        anyTypeClasses.add(anyTypeClass);

        when(plainSchema.getKey()).thenReturn(ALLOWED_SCHEMA);
        doReturn(plainSchemas).when(anyTypeClass).getPlainSchemas();
        doReturn(anyTypeClasses).when(realm).getAnyTypeClasses();
        when(realm.getPlainAttrs()).thenReturn(List.of(attr));
        when(realm.getKey()).thenReturn("realm-key");
        when(attr.getSchema()).thenReturn(ALLOWED_SCHEMA);
        when(attr.getUniqueValue()).thenReturn(uniqueValue);
        doReturn(Optional.of(plainSchema)).when(plainSchemaDAO).findById(ALLOWED_SCHEMA);
        when(plainSchemaDAO.existsPlainAttrUniqueValue("realm-key", plainSchema, uniqueValue)).thenReturn(false);

        assertDoesNotThrow(() -> realmChecker.checkBeforeSave(realm));
        verify(plainSchemaDAO).existsPlainAttrUniqueValue("realm-key", plainSchema, uniqueValue);
    }

    @Test
    void checkBeforeSaveWithNotAllowedSchema() {
        final Realm realm = mock(Realm.class);
        final PlainAttr attr = mock(PlainAttr.class);
        final List<PlainAttr> attrs = new ArrayList<>();
        attrs.add(attr);
        doReturn(new HashSet<AnyTypeClass>()).when(realm).getAnyTypeClasses();
        when(realm.getPlainAttrs()).thenReturn(attrs);
        when(attr.getSchema()).thenReturn(NOT_ALLOWED_SCHEMA);

        final InvalidEntityException exception = assertThrows(InvalidEntityException.class,
                () -> realmChecker.checkBeforeSave(realm));

        assertTrue(exception.hasViolation(EntityViolationType.InvalidPlainAttr));
        assertEquals(1, exception.getViolations().size());
    }

    @Test
    void checkBeforeSaveWithDuplicateUniqueValue() {
        final Realm realm = mock(Realm.class);
        final AnyTypeClass anyTypeClass = mock(AnyTypeClass.class);
        final PlainSchema plainSchema = mock(PlainSchema.class);
        final PlainAttr attr = mock(PlainAttr.class);
        final PlainAttrValue uniqueValue = mock(PlainAttrValue.class);
        final List<PlainSchema> plainSchemas = new ArrayList<>();
        plainSchemas.add(plainSchema);
        final Set<AnyTypeClass> anyTypeClasses = new HashSet<>();
        anyTypeClasses.add(anyTypeClass);

        when(plainSchema.getKey()).thenReturn(ALLOWED_SCHEMA);
        doReturn(plainSchemas).when(anyTypeClass).getPlainSchemas();
        doReturn(anyTypeClasses).when(realm).getAnyTypeClasses();
        when(realm.getPlainAttrs()).thenReturn(List.of(attr));
        when(realm.getKey()).thenReturn("realm-key");
        when(attr.getSchema()).thenReturn(ALLOWED_SCHEMA);
        when(attr.getUniqueValue()).thenReturn(uniqueValue);
        doReturn(Optional.of(plainSchema)).when(plainSchemaDAO).findById(ALLOWED_SCHEMA);
        when(plainSchemaDAO.existsPlainAttrUniqueValue("realm-key", plainSchema, uniqueValue)).thenReturn(true);

        assertThrows(DuplicateException.class, () -> realmChecker.checkBeforeSave(realm));
        verify(plainSchemaDAO).existsPlainAttrUniqueValue("realm-key", plainSchema, uniqueValue);
    }
}
