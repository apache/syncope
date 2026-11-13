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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.RelationshipTypeExtension;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.GroupTypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnyCheckerTest {

    private static final String ALLOWED_SCHEMA = "allowed-schema";
    private static final String NOT_ALLOWED_SCHEMA = "not-allowed-schema";
    private static final String NOT_ALLOWED_RELATIONSHIP_SCHEMA = "not-allowed-relationship-schema";
    private static final String MEMBER_NOT_ALLOWED_SCHEMA = "member-not-allowed-schema";

    @Mock
    private PlainSchemaDAO plainSchemaDAO;

    private AnyChecker anyChecker;

    @BeforeEach
    void setUp() {
        anyChecker = new AnyChecker(plainSchemaDAO);
    }

    @Test
    void findAllowedSchemasByType() {
        final User user = mock(User.class);
        final AnyType anyType = mock(AnyType.class);
        final AnyTypeClass typeClass = mock(AnyTypeClass.class);
        final PlainSchema plainSchema = mock(PlainSchema.class);
        final List<PlainSchema> plainSchemas = new ArrayList<>();
        plainSchemas.add(plainSchema);
        final Set<AnyTypeClass> anyTypeClasses = new HashSet<>();
        anyTypeClasses.add(typeClass);
        final List<AnyTypeClass> auxClasses = new ArrayList<>();

        when(user.getType()).thenReturn(anyType);
        doReturn(anyTypeClasses).when(anyType).getClasses();
        doReturn(auxClasses).when(user).getAuxClasses();
        doReturn(plainSchemas).when(typeClass).getPlainSchemas();
        when(plainSchema.getKey()).thenReturn("plain-schema");

        final AllowedSchemas<PlainSchema> allowedSchemas = anyChecker.findAllowedSchemas(user, PlainSchema.class);

        assertTrue(allowedSchemas.selfContains("plain-schema"));
    }

    @Test
    void findAllowedSchemasByMembership() {
        final User user = mock(User.class);
        final AnyType anyType = mock(AnyType.class);
        final AnyTypeClass auxTypeClass = mock(AnyTypeClass.class);
        final PlainSchema plainSchema = mock(PlainSchema.class);
        final Group group = mock(Group.class);
        final GroupTypeExtension typeExtension = mock(GroupTypeExtension.class);
        final UMembership membership = mock(UMembership.class);

        final List<PlainSchema> plainSchemas = new ArrayList<>();
        plainSchemas.add(plainSchema);
        final List<AnyTypeClass> auxClasses = new ArrayList<>();
        final List<AnyTypeClass> auxTypeClasses = new ArrayList<>();
        auxTypeClasses.add(auxTypeClass);
        final List<GroupTypeExtension> groupTypeExtensions = new ArrayList<>();
        groupTypeExtensions.add(typeExtension);
        final List<UMembership> memberships = new ArrayList<>();
        memberships.add(membership);

        when(user.getType()).thenReturn(anyType);
        doReturn(memberships).when(user).getMemberships();
        when(membership.getRightEnd()).thenReturn(group);
        doReturn(groupTypeExtensions).when(group).getTypeExtensions();
        doReturn(auxTypeClasses).when(typeExtension).getAuxClasses();
        doReturn(plainSchemas).when(auxTypeClass).getPlainSchemas();
        doReturn(auxClasses).when(user).getAuxClasses();

        final AllowedSchemas<PlainSchema> allowedSchemas = anyChecker.findAllowedSchemas(user, PlainSchema.class);

        assertTrue(allowedSchemas.memberships().get(group).contains(plainSchema));
    }

    @Test
    void findAllowedSchemasByRelationshipType() {
        final User user = mock(User.class);
        final AnyType anyType = mock(AnyType.class);
        final AnyTypeClass auxTypeClass = mock(AnyTypeClass.class);
        final PlainSchema plainSchema = mock(PlainSchema.class);
        final RelationshipType relationshipType = mock(RelationshipType.class);
        final URelationship relationship = mock(URelationship.class);
        final RelationshipTypeExtension relationshipTypeExtension = mock(RelationshipTypeExtension.class);

        final List<AnyTypeClass> auxClasses = new ArrayList<>();
        final List<AnyTypeClass> auxTypeClasses = new ArrayList<>();
        auxTypeClasses.add(auxTypeClass);
        final List<RelationshipTypeExtension> relationshipTypeExtensions = new ArrayList<>();
        relationshipTypeExtensions.add(relationshipTypeExtension);
        final List<URelationship> relationships = new ArrayList<>();
        relationships.add(relationship);
        final List<PlainSchema> plainSchemas = new ArrayList<>();
        plainSchemas.add(plainSchema);

        when(user.getType()).thenReturn(anyType);
        doReturn(auxClasses).when(user).getAuxClasses();
        doReturn(relationships).when(user).getRelationships();
        when(relationship.getType()).thenReturn(relationshipType);
        doReturn(relationshipTypeExtensions).when(relationshipType).getTypeExtensions();
        doReturn(auxTypeClasses).when(relationshipTypeExtension).getAuxClasses();
        doReturn(plainSchemas).when(auxTypeClass).getPlainSchemas();

        final AllowedSchemas<PlainSchema> allowedSchemas = anyChecker.findAllowedSchemas(user, PlainSchema.class);

        assertTrue(allowedSchemas.relationshipTypes().get(relationshipType).contains(plainSchema));
    }

    @Test
    void checkBeforeSaveWithInvalidManager() {
        final User user = mock(User.class);
        final User manager = mock(User.class);
        final Group managerGroup = mock(Group.class);
        final AnyUtils anyUtils = mock(AnyUtils.class);

        when(user.getuManager()).thenReturn(manager);
        when(user.getgManager()).thenReturn(managerGroup);
        doReturn(User.class).when(anyUtils).anyClass();

        assertThrows(InvalidEntityException.class, () -> anyChecker.checkBeforeSave(user, anyUtils));
    }

    @Test
    void checkBeforeSaveWithAllowedSchema() {
        final User user = mock(User.class);
        final AnyType anyType = mock(AnyType.class);
        final AnyTypeClass typeClass = mock(AnyTypeClass.class);
        final PlainSchema plainSchema = mock(PlainSchema.class);
        final PlainAttr attr = mock(PlainAttr.class);
        final List<PlainSchema> plainSchemas = new ArrayList<>();
        plainSchemas.add(plainSchema);
        final Set<AnyTypeClass> anyTypeClasses = new HashSet<>();
        anyTypeClasses.add(typeClass);
        final List<AnyTypeClass> auxClasses = new ArrayList<>();
        final AnyUtils anyUtils = mock(AnyUtils.class);

        when(user.getuManager()).thenReturn(null);
        when(user.getType()).thenReturn(anyType);
        doReturn(anyTypeClasses).when(anyType).getClasses();
        doReturn(auxClasses).when(user).getAuxClasses();
        doReturn(plainSchemas).when(typeClass).getPlainSchemas();
        when(user.getPlainAttrs()).thenReturn(List.of(attr));
        when(attr.getSchema()).thenReturn(ALLOWED_SCHEMA);
        when(attr.getUniqueValue()).thenReturn(null);
        when(plainSchema.getKey()).thenReturn(ALLOWED_SCHEMA);

        assertDoesNotThrow(() -> anyChecker.checkBeforeSave(user, anyUtils));
        verify(plainSchemaDAO, never()).existsPlainAttrUniqueValue(any(), anyString(), any(), any());
    }

    @Test
    void checkBeforeSaveWithNotAllowedSchema() {
        final User user = mock(User.class);
        final AnyType anyType = mock(AnyType.class);
        final AnyTypeClass typeClass = mock(AnyTypeClass.class);
        final PlainSchema plainSchema = mock(PlainSchema.class);
        final PlainAttr attr = mock(PlainAttr.class);
        final List<PlainSchema> plainSchemas = new ArrayList<>();
        plainSchemas.add(plainSchema);
        final Set<AnyTypeClass> anyTypeClasses = new HashSet<>();
        anyTypeClasses.add(typeClass);
        final List<AnyTypeClass> auxClasses = new ArrayList<>();
        final AnyUtils anyUtils = mock(AnyUtils.class);

        when(user.getuManager()).thenReturn(null);
        when(user.getType()).thenReturn(anyType);
        doReturn(anyTypeClasses).when(anyType).getClasses();
        doReturn(auxClasses).when(user).getAuxClasses();
        doReturn(plainSchemas).when(typeClass).getPlainSchemas();
        when(user.getPlainAttrs()).thenReturn(List.of(attr));
        when(attr.getSchema()).thenReturn(NOT_ALLOWED_SCHEMA);
        when(plainSchema.getKey()).thenReturn(ALLOWED_SCHEMA);
        doReturn(User.class).when(anyUtils).anyClass();

        final InvalidEntityException exception = assertThrows(InvalidEntityException.class,
                () -> anyChecker.checkBeforeSave(user, anyUtils));

        assertTrue(exception.hasViolation(EntityViolationType.InvalidPlainAttr));
    }

    @Test
    void checkBeforeSaveWithNotAllowedMembershipSchema() {
        final User user = mock(User.class);
        final AnyType anyType = mock(AnyType.class);
        final AnyTypeClass auxTypeClass = mock(AnyTypeClass.class);
        final PlainSchema plainSchema = mock(PlainSchema.class);
        final Group group = mock(Group.class);
        final GroupTypeExtension typeExtension = mock(GroupTypeExtension.class);
        final UMembership membership = mock(UMembership.class);
        final PlainAttr attr = mock(PlainAttr.class);
        final AnyUtils anyUtils = mock(AnyUtils.class);

        final List<PlainSchema> plainSchemas = new ArrayList<>();
        plainSchemas.add(plainSchema);
        final List<AnyTypeClass> auxTypeClasses = new ArrayList<>();
        auxTypeClasses.add(auxTypeClass);
        final List<GroupTypeExtension> groupTypeExtensions = new ArrayList<>();
        groupTypeExtensions.add(typeExtension);
        final List<UMembership> memberships = new ArrayList<>();
        memberships.add(membership);
        final List<PlainAttr> userAttrs = new ArrayList<>();
        final List<PlainAttr> membershipAttrs = new ArrayList<>();
        membershipAttrs.add(attr);

        when(user.getuManager()).thenReturn(null);
        when(user.getType()).thenReturn(anyType);
        doReturn(memberships).when(user).getMemberships();
        when(membership.getRightEnd()).thenReturn(group);
        doReturn(groupTypeExtensions).when(group).getTypeExtensions();
        doReturn(auxTypeClasses).when(typeExtension).getAuxClasses();
        doReturn(plainSchemas).when(auxTypeClass).getPlainSchemas();
        doReturn(userAttrs).when(user).getPlainAttrs();
        doReturn(membershipAttrs).when(user).getPlainAttrs(membership);
        when(attr.getSchema()).thenReturn(MEMBER_NOT_ALLOWED_SCHEMA);
        when(plainSchema.getKey()).thenReturn("member-plain-schema");
        doReturn(User.class).when(anyUtils).anyClass();

        final InvalidEntityException exception = assertThrows(InvalidEntityException.class,
                () -> anyChecker.checkBeforeSave(user, anyUtils));

        assertTrue(exception.hasViolation(EntityViolationType.InvalidPlainAttr));
    }

    @Test
    void checkBeforeSaveWithNotAllowedRelationshipSchema() {
        final User user = mock(User.class);
        final AnyType anyType = mock(AnyType.class);
        final RelationshipType relationshipType = mock(RelationshipType.class);
        final URelationship relationship = mock(URelationship.class);
        final RelationshipTypeExtension relationshipTypeExtension = mock(RelationshipTypeExtension.class);
        final AnyTypeClass auxTypeClass = mock(AnyTypeClass.class);
        final PlainAttr attr = mock(PlainAttr.class);
        final AnyUtils anyUtils = mock(AnyUtils.class);

        final List<AnyTypeClass> auxClasses = new ArrayList<>();
        final List<AnyTypeClass> auxTypeClasses = new ArrayList<>();
        auxTypeClasses.add(auxTypeClass);
        final List<RelationshipTypeExtension> relationshipTypeExtensions = new ArrayList<>();
        relationshipTypeExtensions.add(relationshipTypeExtension);
        final List<URelationship> relationships = new ArrayList<>();
        relationships.add(relationship);
        final List<PlainAttr> userAttrs = new ArrayList<>();
        final List<PlainAttr> relationshipAttrs = new ArrayList<>();
        relationshipAttrs.add(attr);

        when(user.getuManager()).thenReturn(null);
        when(user.getType()).thenReturn(anyType);
        doReturn(auxClasses).when(user).getAuxClasses();
        doReturn(relationships).when(user).getRelationships();
        when(relationship.getType()).thenReturn(relationshipType);
        doReturn(relationshipTypeExtensions).when(relationshipType).getTypeExtensions();
        doReturn(auxTypeClasses).when(relationshipTypeExtension).getAuxClasses();
        doReturn(userAttrs).when(user).getPlainAttrs();
        doReturn(relationshipAttrs).when(user).getPlainAttrs(relationship);
        when(attr.getSchema()).thenReturn(NOT_ALLOWED_RELATIONSHIP_SCHEMA);
        doReturn(User.class).when(anyUtils).anyClass();

        final InvalidEntityException exception = assertThrows(InvalidEntityException.class,
                () -> anyChecker.checkBeforeSave(user, anyUtils));

        assertTrue(exception.hasViolation(EntityViolationType.InvalidPlainAttr));
    }

    @Test
    void checkBeforeSaveWithDuplicateUniqueValue() {
        final User user = mock(User.class);
        final AnyType anyType = mock(AnyType.class);
        final AnyTypeClass typeClass = mock(AnyTypeClass.class);
        final PlainSchema plainSchema = mock(PlainSchema.class);
        final PlainAttr attr = mock(PlainAttr.class);
        final PlainAttrValue uniqueValue = mock(PlainAttrValue.class);
        final List<PlainSchema> plainSchemas = new ArrayList<>();
        plainSchemas.add(plainSchema);
        final Set<AnyTypeClass> anyTypeClasses = new HashSet<>();
        anyTypeClasses.add(typeClass);
        final List<AnyTypeClass> auxClasses = new ArrayList<>();
        final AnyUtils anyUtils = mock(AnyUtils.class);

        when(user.getuManager()).thenReturn(null);
        when(user.getType()).thenReturn(anyType);
        doReturn(anyTypeClasses).when(anyType).getClasses();
        doReturn(auxClasses).when(user).getAuxClasses();
        doReturn(plainSchemas).when(typeClass).getPlainSchemas();
        when(user.getKey()).thenReturn("user-key");
        when(user.getPlainAttrs()).thenReturn(List.of(attr));
        when(attr.getSchema()).thenReturn(ALLOWED_SCHEMA);
        when(attr.getUniqueValue()).thenReturn(uniqueValue);
        when(plainSchema.getKey()).thenReturn(ALLOWED_SCHEMA);
        doReturn(Optional.of(plainSchema)).when(plainSchemaDAO).findById(ALLOWED_SCHEMA);
        when(plainSchemaDAO.existsPlainAttrUniqueValue(any(), eq("user-key"), any(), any())).thenReturn(true);

        assertThrows(DuplicateException.class, () -> anyChecker.checkBeforeSave(user, anyUtils));
        verify(plainSchemaDAO).existsPlainAttrUniqueValue(any(), eq("user-key"), any(), any());
    }
}
