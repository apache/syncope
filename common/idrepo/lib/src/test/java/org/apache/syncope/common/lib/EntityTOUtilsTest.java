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
package org.apache.syncope.common.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.junit.jupiter.api.Test;

class EntityTOUtilsTest {

    @Test
    void buildAttrMap() {
        Map<String, Attr> attrs = EntityTOUtils.buildAttrMap(List.of(
                new Attr.Builder("schema1").value("value1").build(),
                new Attr.Builder("schema2").value("value2").build(),
                new Attr.Builder("schema1").value("value3").build()));

        assertEquals(2, attrs.size());
        assertEquals(List.of("value3"), attrs.get("schema1").getValues());
        assertEquals(List.of("value2"), attrs.get("schema2").getValues());
    }

    @Test
    void buildRelationshipMap() {
        RelationshipTO first = new RelationshipTO.Builder("type").otherEnd("other").build();
        RelationshipTO second = new RelationshipTO.Builder("type").otherEnd("other").build();
        second.getPlainAttrs().add(new Attr.Builder("schema").value("value").build());

        Map<Pair<String, String>, RelationshipTO> relationships =
                EntityTOUtils.buildRelationshipMap(List.of(first, second));

        assertEquals(1, relationships.size());
        assertTrue(relationships.get(Pair.of("type", "other")).getPlainAttrs().contains(
                new Attr.Builder("schema").value("value").build()));
    }

    @Test
    void buildMembershipMap() {
        MembershipTO first = new MembershipTO.Builder("group").build();
        MembershipTO second = new MembershipTO.Builder("group").groupName("name").build();

        Map<String, MembershipTO> memberships = EntityTOUtils.buildMembershipMap(List.of(first, second));

        assertEquals(1, memberships.size());
        assertEquals("name", memberships.get("group").getGroupName());
    }

    @Test
    void buildLinkedAccountMap() {
        LinkedAccountTO first = new LinkedAccountTO.Builder("resource", "key").build();
        LinkedAccountTO second = new LinkedAccountTO.Builder("resource", "key").username("username").build();

        Map<Pair<String, String>, LinkedAccountTO> accounts =
                EntityTOUtils.buildLinkedAccountMap(List.of(first, second));

        assertEquals(1, accounts.size());
        assertEquals("username", accounts.get(Pair.of("resource", "key")).getUsername());
    }

    @Test
    void toUserCR() {
        UserTO userTO = new UserTO();
        userTO.setRealm("/");
        userTO.setuManager("uManager");
        userTO.setgManager("gManager");
        userTO.setUsername("username");
        userTO.setPassword("password");
        userTO.setSecurityQuestion("question");
        userTO.setMustChangePassword(true);
        userTO.getAuxClasses().add("auxClass");
        userTO.getPlainAttrs().add(new Attr.Builder("plain").value("value").build());
        userTO.getResources().add("resource");
        userTO.getRelationships().add(new RelationshipTO.Builder("type").otherEnd("other").build());
        userTO.getMemberships().add(new MembershipTO.Builder("group").build());
        userTO.getRoles().add("role");

        UserCR userCR = new UserCR();
        EntityTOUtils.toAnyCR(userTO, userCR);

        assertCommon(userCR);
        assertEquals("username", userCR.getUsername());
        assertEquals("password", userCR.getPassword());
        assertEquals("question", userCR.getSecurityQuestion());
        assertTrue(userCR.isMustChangePassword());
        assertEquals(1, userCR.getMemberships().size());
        assertEquals(1, userCR.getRoles().size());
    }

    @Test
    void toGroupCR() {
        GroupTO groupTO = new GroupTO();
        groupTO.setRealm("/");
        groupTO.setuManager("uManager");
        groupTO.setgManager("gManager");
        groupTO.setName("name");
        groupTO.getAuxClasses().add("auxClass");
        groupTO.getPlainAttrs().add(new Attr.Builder("plain").value("value").build());
        groupTO.getResources().add("resource");
        groupTO.getRelationships().add(new RelationshipTO.Builder("type").otherEnd("other").build());
        groupTO.getTypeExtensions().add(new TypeExtensionTO());

        GroupCR groupCR = new GroupCR();
        EntityTOUtils.toAnyCR(groupTO, groupCR);

        assertCommon(groupCR);
        assertEquals("name", groupCR.getName());
        assertEquals(1, groupCR.getTypeExtensions().size());
    }

    @Test
    void toAnyObjectCR() {
        AnyObjectTO anyObjectTO = new AnyObjectTO();
        anyObjectTO.setRealm("/");
        anyObjectTO.setuManager("uManager");
        anyObjectTO.setgManager("gManager");
        anyObjectTO.setType("type");
        anyObjectTO.setName("name");
        anyObjectTO.getAuxClasses().add("auxClass");
        anyObjectTO.getPlainAttrs().add(new Attr.Builder("plain").value("value").build());
        anyObjectTO.getResources().add("resource");
        anyObjectTO.getRelationships().add(new RelationshipTO.Builder("type").otherEnd("other").build());
        anyObjectTO.getMemberships().add(new MembershipTO.Builder("group").build());

        AnyObjectCR anyObjectCR = new AnyObjectCR();
        EntityTOUtils.toAnyCR(anyObjectTO, anyObjectCR);

        assertCommon(anyObjectCR);
        assertEquals("type", anyObjectCR.getType());
        assertEquals("name", anyObjectCR.getName());
        assertEquals(1, anyObjectCR.getMemberships().size());
    }

    @Test
    void toUserTO() {
        UserCR userCR = new UserCR();
        userCR.setRealm("/");
        userCR.setuManager("uManager");
        userCR.setgManager("gManager");
        userCR.setUsername("username");
        userCR.setPassword("password");
        userCR.setSecurityQuestion("question");
        userCR.setMustChangePassword(true);
        userCR.getAuxClasses().add("auxClass");
        userCR.getPlainAttrs().add(new Attr.Builder("plain").value("value").build());
        userCR.getResources().add("resource");
        userCR.getRelationships().add(new RelationshipTO.Builder("type").otherEnd("other").build());
        userCR.getMemberships().add(new MembershipTO.Builder("group").build());
        userCR.getRoles().add("role");

        UserTO userTO = new UserTO();
        EntityTOUtils.toAnyTO(userCR, userTO);

        assertCommon(userTO);
        assertEquals("username", userTO.getUsername());
        assertEquals("password", userTO.getPassword());
        assertEquals("question", userTO.getSecurityQuestion());
        assertTrue(userTO.isMustChangePassword());
        assertEquals(1, userTO.getMemberships().size());
        assertEquals(1, userTO.getRoles().size());
    }

    @Test
    void toGroupTO() {
        GroupCR groupCR = new GroupCR();
        groupCR.setRealm("/");
        groupCR.setuManager("uManager");
        groupCR.setgManager("gManager");
        groupCR.setName("name");
        groupCR.getAuxClasses().add("auxClass");
        groupCR.getPlainAttrs().add(new Attr.Builder("plain").value("value").build());
        groupCR.getResources().add("resource");
        groupCR.getRelationships().add(new RelationshipTO.Builder("type").otherEnd("other").build());
        groupCR.getTypeExtensions().add(new TypeExtensionTO());

        GroupTO groupTO = new GroupTO();
        EntityTOUtils.toAnyTO(groupCR, groupTO);

        assertCommon(groupTO);
        assertEquals("name", groupTO.getName());
        assertEquals(1, groupTO.getTypeExtensions().size());
    }

    @Test
    void toAnyObjectTO() {
        AnyObjectCR anyObjectCR = new AnyObjectCR();
        anyObjectCR.setRealm("/");
        anyObjectCR.setuManager("uManager");
        anyObjectCR.setgManager("gManager");
        anyObjectCR.setType("type");
        anyObjectCR.setName("name");
        anyObjectCR.getAuxClasses().add("auxClass");
        anyObjectCR.getPlainAttrs().add(new Attr.Builder("plain").value("value").build());
        anyObjectCR.getResources().add("resource");
        anyObjectCR.getRelationships().add(new RelationshipTO.Builder("type").otherEnd("other").build());
        anyObjectCR.getMemberships().add(new MembershipTO.Builder("group").build());

        AnyObjectTO anyObjectTO = new AnyObjectTO();
        EntityTOUtils.toAnyTO(anyObjectCR, anyObjectTO);

        assertCommon(anyObjectTO);
        assertEquals("type", anyObjectTO.getType());
        assertEquals("name", anyObjectTO.getName());
        assertEquals(1, anyObjectTO.getMemberships().size());
    }

    private static void assertCommon(final AnyCR anyCR) {
        assertEquals("/", anyCR.getRealm());
        assertEquals("uManager", anyCR.getuManager());
        assertEquals("gManager", anyCR.getgManager());
        assertEquals(1, anyCR.getAuxClasses().size());
        assertEquals(1, anyCR.getPlainAttrs().size());
        assertEquals(1, anyCR.getResources().size());
        assertEquals(1, anyCR.getRelationships().size());
    }

    private static void assertCommon(final AnyTO anyTO) {
        assertEquals("/", anyTO.getRealm());
        assertEquals("uManager", anyTO.getuManager());
        assertEquals("gManager", anyTO.getgManager());
        assertEquals(1, anyTO.getAuxClasses().size());
        assertEquals(1, anyTO.getPlainAttrs().size());
        assertEquals(1, anyTO.getResources().size());
        assertEquals(1, anyTO.getRelationships().size());
    }
}
