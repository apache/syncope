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
package org.apache.syncope.ext.elasticsearch.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.GRelationship;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class ElasticsearchUtilsTest {

    @Mock
    private PlainSchemaDAO plainSchemaDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private GroupDAO groupDAO;

    @Mock
    private AnyObjectDAO anyObjectDAO;

    private ElasticsearchUtils utils;

    @BeforeEach
    public void setupUtils() {
        utils = new ElasticsearchUtils(plainSchemaDAO, userDAO, groupDAO, anyObjectDAO);
    }

    private static PlainAttr plainAttr(final String schema, final Object... values) {
        PlainAttr plainAttr = new PlainAttr();
        plainAttr.setSchema(schema);
        for (Object value : values) {
            PlainAttrValue plainAttrValue = new PlainAttrValue();
            switch (value) {
                case Long l ->
                    plainAttrValue.setLongValue(l);
                case byte[] b ->
                    plainAttrValue.setBinaryValue(b);
                default ->
                    plainAttrValue.setStringValue(value.toString());
            }
            plainAttr.add(plainAttrValue);
        }
        return plainAttr;
    }

    private void plainSchema(final String key, final AttrSchemaType type) {
        PlainSchema plainSchema = mock(PlainSchema.class);
        when(plainSchema.getType()).thenReturn(type);
        doReturn(Optional.of(plainSchema)).when(plainSchemaDAO).findById(key);
    }

    private static AnyTypeClass anyTypeClass(final String key) {
        AnyTypeClass anyTypeClass = mock(AnyTypeClass.class);
        when(anyTypeClass.getKey()).thenReturn(key);
        return anyTypeClass;
    }

    @Test
    public void indexNames() {
        assertThat(ElasticsearchUtils.getAnyIndex("Master", AnyTypeKind.USER)).isEqualTo("master_user");
        assertThat(ElasticsearchUtils.getAnyIndex("TWO", AnyTypeKind.GROUP)).isEqualTo("two_group");
        assertThat(ElasticsearchUtils.getAnyIndex("Master", AnyTypeKind.ANY_OBJECT)).
                isEqualTo("master_any_object");
        assertThat(ElasticsearchUtils.getRealmIndex("Master")).isEqualTo("master_realm");
        assertThat(ElasticsearchUtils.getAuditIndex("Master")).isEqualTo("master_audit");
    }

    @Test
    public void documentUser() {
        plainSchema("aLong", AttrSchemaType.Long);
        plainSchema("aString", AttrSchemaType.String);
        plainSchema("binary", AttrSchemaType.Binary);
        plainSchema("encrypted", AttrSchemaType.Encrypted);

        AnyType anyType = mock(AnyType.class);
        when(anyType.getKey()).thenReturn("USER");
        Realm realm = mock(Realm.class);
        when(realm.getKey()).thenReturn("realm-key");
        User manager = mock(User.class);
        when(manager.getKey()).thenReturn("manager-key");
        AnyTypeClass auxClass = anyTypeClass("aux");

        User user = mock(User.class);
        when(user.getKey()).thenReturn("user-key");
        when(user.getRealm()).thenReturn(realm);
        when(user.getType()).thenReturn(anyType);
        doReturn(List.of(auxClass)).when(user).getAuxClasses();
        when(user.getuManager()).thenReturn(manager);
        when(user.getUsername()).thenReturn("rossi");
        when(user.getToken()).thenReturn("token");
        when(user.getFailedLogins()).thenReturn(3);
        when(user.isSuspended()).thenReturn(true);
        when(user.isMustChangePassword()).thenReturn(false);

        Role role = mock(Role.class);
        when(role.getKey()).thenReturn("role-key");
        when(userDAO.findAllRoles(user)).thenReturn(List.of(role));
        when(userDAO.findAllGroupKeys(user)).thenReturn(Set.of("group-key"));
        when(userDAO.findAllResourceKeys("user-key")).thenReturn(Set.of("resource-key"));

        AnyObject rightEnd = mock(AnyObject.class);
        when(rightEnd.getKey()).thenReturn("right-end-key");
        RelationshipType relationshipType = mock(RelationshipType.class);
        when(relationshipType.getKey()).thenReturn("relationship-type-key");
        URelationship relationship = mock(URelationship.class);
        when(relationship.getRightEnd()).thenReturn(rightEnd);
        when(relationship.getType()).thenReturn(relationshipType);
        doReturn(List.of(relationship)).when(user).getRelationships();

        when(user.getPlainAttrs()).thenReturn(List.of(
                plainAttr("aLong", 42L),
                plainAttr("aString", "one", "two"),
                plainAttr("binary", "not-indexed".getBytes()),
                plainAttr("encrypted", "not-indexed"),
                plainAttr("unknown", "not-indexed")));

        UMembership membership = mock(UMembership.class);
        doReturn(List.of(membership)).when(user).getMemberships();
        when(user.getPlainAttrs(membership)).thenReturn(List.of(plainAttr("aString", "membership")));

        Map<String, Object> document = utils.document(user);

        assertThat(document).
                containsEntry("realm", "realm-key").
                containsEntry("anyType", "USER").
                containsEntry("username", "rossi").
                containsEntry("token", "token").
                containsEntry("failedLogins", 3).
                containsEntry("suspended", true).
                containsEntry("mustChangePassword", false).
                containsEntry("uManager", "manager-key").
                containsEntry("auxClasses", List.of("aux")).
                containsEntry("roles", List.of("role-key")).
                containsEntry("memberships", Set.of("group-key")).
                containsEntry("relationships", List.of("right-end-key")).
                containsEntry("relationshipTypes", List.of("relationship-type-key")).
                containsEntry("resources", Set.of("resource-key")).
                containsEntry("aLong", 42L).
                containsEntry("aString", List.of("one", "two", "membership")).
                doesNotContainKeys("gManager", "binary", "encrypted", "unknown");
    }

    @Test
    public void documentGroup() {
        AnyType anyType = mock(AnyType.class);
        when(anyType.getKey()).thenReturn("GROUP");
        Realm realm = mock(Realm.class);
        when(realm.getKey()).thenReturn("realm-key");

        Group group = mock(Group.class);
        when(group.getKey()).thenReturn("group-key");
        when(group.getRealm()).thenReturn(realm);
        when(group.getType()).thenReturn(anyType);
        when(group.getAuxClasses()).thenReturn(List.of());
        when(group.getName()).thenReturn("citizens");

        User userMember = mock(User.class);
        when(userMember.getKey()).thenReturn("user-key");
        UMembership uMembership = mock(UMembership.class);
        when(uMembership.getLeftEnd()).thenReturn(userMember);
        when(groupDAO.findUMemberships(eq(group), any(Pageable.class))).thenReturn(List.of(uMembership));

        AnyObject anyObjectMember = mock(AnyObject.class);
        when(anyObjectMember.getKey()).thenReturn("any-object-key");
        AMembership aMembership = mock(AMembership.class);
        when(aMembership.getLeftEnd()).thenReturn(anyObjectMember);
        when(groupDAO.findAMemberships(group)).thenReturn(List.of(aMembership));

        AnyObject rightEnd = mock(AnyObject.class);
        when(rightEnd.getKey()).thenReturn("right-end-key");
        RelationshipType relationshipType = mock(RelationshipType.class);
        when(relationshipType.getKey()).thenReturn("relationship-type-key");
        GRelationship relationship = mock(GRelationship.class);
        when(relationship.getRightEnd()).thenReturn(rightEnd);
        when(relationship.getType()).thenReturn(relationshipType);
        doReturn(List.of(relationship)).when(group).getRelationships();

        when(groupDAO.findAllResourceKeys("group-key")).thenReturn(Set.of("resource-key"));
        when(group.getPlainAttrs()).thenReturn(List.of());

        Map<String, Object> document = utils.document(group);

        assertThat(document).
                containsEntry("realm", "realm-key").
                containsEntry("anyType", "GROUP").
                containsEntry("name", "citizens").
                containsEntry("relationships", List.of("right-end-key")).
                containsEntry("relationshipTypes", List.of("relationship-type-key")).
                containsEntry("resources", Set.of("resource-key")).
                containsEntry("members", Set.of("user-key", "any-object-key")).
                doesNotContainKeys("uManager", "gManager");
    }

    @Test
    public void documentAnyObject() {
        AnyType anyType = mock(AnyType.class);
        when(anyType.getKey()).thenReturn("DEVICE");
        Realm realm = mock(Realm.class);
        when(realm.getKey()).thenReturn("realm-key");

        AnyObject anyObject = mock(AnyObject.class);
        when(anyObject.getKey()).thenReturn("any-object-key");
        when(anyObject.getRealm()).thenReturn(realm);
        when(anyObject.getType()).thenReturn(anyType);
        when(anyObject.getAuxClasses()).thenReturn(List.of());
        when(anyObject.getName()).thenReturn("printer");

        when(anyObjectDAO.findAllGroupKeys(anyObject)).thenReturn(Set.of("group-key"));
        when(anyObjectDAO.findAllRelationships(anyObject)).thenReturn(List.of());
        when(anyObjectDAO.findAllResourceKeys("any-object-key")).thenReturn(Set.of("resource-key"));
        when(anyObject.getPlainAttrs()).thenReturn(List.of());

        Map<String, Object> document = utils.document(anyObject);

        assertThat(document).
                containsEntry("realm", "realm-key").
                containsEntry("anyType", "DEVICE").
                containsEntry("name", "printer").
                containsEntry("memberships", Set.of("group-key")).
                containsEntry("relationships", List.of()).
                containsEntry("relationshipTypes", List.of()).
                containsEntry("resources", Set.of("resource-key")).
                doesNotContainKeys("uManager", "gManager");
    }

    @Test
    public void documentRealm() {
        plainSchema("aString", AttrSchemaType.String);

        Realm realm = mock(Realm.class);
        when(realm.getName()).thenReturn("citizens");
        when(realm.getFullPath()).thenReturn("/citizens");
        doReturn(Set.of(anyTypeClass("aux"))).when(realm).getAnyTypeClasses();
        ExternalResource resource = mock(ExternalResource.class);
        when(resource.getKey()).thenReturn("resource-key");
        doReturn(List.of(resource)).when(realm).getResources();
        when(realm.getPlainAttrs()).thenReturn(List.of(plainAttr("aString", "value")));

        Map<String, Object> document = utils.document(realm);

        assertThat(document).
                containsEntry("name", "citizens").
                containsEntry("parent_id", null).
                containsEntry("fullPath", "/citizens").
                containsEntry("auxClasses", List.of("aux")).
                containsEntry("resources", List.of("resource-key")).
                containsEntry("aString", "value");
    }

    @Test
    public void documentAuditEvent() {
        OffsetDateTime when = OffsetDateTime.now();
        AuditEvent auditEvent = mock(AuditEvent.class);
        when(auditEvent.getKey()).thenReturn("audit-key");
        when(auditEvent.getOpEvent()).thenReturn("LOGIC/UserLogic/create[SUCCESS]");
        when(auditEvent.getWho()).thenReturn("admin");
        when(auditEvent.getWhen()).thenReturn(when);
        when(auditEvent.getBefore()).thenReturn("before");
        when(auditEvent.getInputs()).thenReturn(List.of("input"));
        when(auditEvent.getOutput()).thenReturn("output");
        when(auditEvent.getThrowable()).thenReturn("throwable");

        Map<String, Object> document = utils.document(auditEvent);

        assertThat(document).
                containsEntry("key", "audit-key").
                containsEntry("opEvent", "LOGIC/UserLogic/create[SUCCESS]").
                containsEntry("who", "admin").
                containsEntry("when", when).
                containsEntry("before", "before").
                containsEntry("inputs", List.of("input")).
                containsEntry("output", "output").
                containsEntry("throwable", "throwable");
    }
}
