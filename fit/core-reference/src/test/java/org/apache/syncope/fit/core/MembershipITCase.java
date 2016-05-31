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
package org.apache.syncope.fit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class MembershipITCase extends AbstractITCase {

    @Test
    public void misc() {
        UserTO user = UserITCase.getUniqueSampleTO("memb@apache.org");
        user.setRealm("/even/two");
        user.getPlainAttrs().add(new AttrTO.Builder().schema("aLong").value("1976").build());
        user.getPlainAttrs().remove(user.getPlainAttrMap().get("ctype"));

        // the group 034740a9-fa10-453b-af37-dc7897e98fb1 has USER type extensions for 'csv' and 'other' 
        // any type classes
        MembershipTO membership = new MembershipTO.Builder().group("034740a9-fa10-453b-af37-dc7897e98fb1").build();
        membership.getPlainAttrs().add(new AttrTO.Builder().schema("aLong").value("1977").build());

        // 'fullname' is in 'minimal user', so it is not allowed for this membership
        membership.getPlainAttrs().add(new AttrTO.Builder().schema("fullname").value("discarded").build());

        user.getMemberships().add(membership);

        // user creation fails because of fullname
        try {
            createUser(user);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUser, e.getType());
            assertTrue(e.getMessage().contains("InvalidPlainAttr: fullname not allowed for membership of group"));
        }

        // remove fullname and try again
        CollectionUtils.filterInverse(membership.getPlainAttrs(), new Predicate<AttrTO>() {

            @Override
            public boolean evaluate(final AttrTO object) {
                return "fullname".equals(object.getSchema());
            }
        });
        try {
            user = createUser(user).getAny();

            // 1. verify that 'aLong' is correctly populated for user
            assertEquals(1, user.getPlainAttrMap().get("aLong").getValues().size());
            assertEquals("1976", user.getPlainAttrMap().get("aLong").getValues().get(0));

            // 2. verify that 'aLong' is correctly populated for user's membership
            assertEquals(1, user.getMemberships().size());
            membership = user.getMembershipMap().get("034740a9-fa10-453b-af37-dc7897e98fb1");
            assertNotNull(membership);
            assertEquals(1, membership.getPlainAttrMap().get("aLong").getValues().size());
            assertEquals("1977", membership.getPlainAttrMap().get("aLong").getValues().get(0));

            // 3. verify that derived attrbutes from 'csv' and 'other' are also populated for user's membership
            assertFalse(membership.getDerAttrMap().get("csvuserid").getValues().isEmpty());
            assertFalse(membership.getDerAttrMap().get("noschema").getValues().isEmpty());

            // update user - change some values and add new membership attribute
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(user.getKey());

            userPatch.getPlainAttrs().add(new AttrPatch.Builder().
                    attrTO(new AttrTO.Builder().schema("aLong").value("1977").build()).build());

            MembershipPatch membershipPatch = new MembershipPatch.Builder().group(membership.getGroupKey()).build();
            membershipPatch.getPlainAttrs().add(new AttrPatch.Builder().
                    attrTO(new AttrTO.Builder().schema("aLong").value("1976").build()).build());
            membershipPatch.getPlainAttrs().add(new AttrPatch.Builder().
                    attrTO(new AttrTO.Builder().schema("ctype").value("membership type").build()).build());
            userPatch.getMemberships().add(membershipPatch);

            user = updateUser(userPatch).getAny();

            // 4. verify that 'aLong' is correctly populated for user
            assertEquals(1, user.getPlainAttrMap().get("aLong").getValues().size());
            assertEquals("1977", user.getPlainAttrMap().get("aLong").getValues().get(0));
            assertFalse(user.getPlainAttrMap().containsKey("ctype"));

            // 5. verify that 'aLong' is correctly populated for user's membership
            assertEquals(1, user.getMemberships().size());
            membership = user.getMembershipMap().get("034740a9-fa10-453b-af37-dc7897e98fb1");
            assertNotNull(membership);
            assertEquals(1, membership.getPlainAttrMap().get("aLong").getValues().size());
            assertEquals("1976", membership.getPlainAttrMap().get("aLong").getValues().get(0));

            // 6. verify that 'ctype' is correctly populated for user's membership
            assertEquals("membership type", membership.getPlainAttrMap().get("ctype").getValues().get(0));

            // finally remove membership
            userPatch = new UserPatch();
            userPatch.setKey(user.getKey());

            membershipPatch = new MembershipPatch.Builder().group(membership.getGroupKey()).
                    operation(PatchOperation.DELETE).build();
            userPatch.getMemberships().add(membershipPatch);

            user = updateUser(userPatch).getAny();

            assertTrue(user.getMemberships().isEmpty());
        } finally {
            if (user.getKey() != null) {
                userService.delete(user.getKey());
            }
        }
    }

    @Test
    public void deleteUserWithMembership() {
        UserTO user = UserITCase.getUniqueSampleTO("memb@apache.org");
        user.setRealm("/even/two");
        user.getPlainAttrs().add(new AttrTO.Builder().schema("aLong").value("1976").build());

        MembershipTO membership = new MembershipTO.Builder().group("034740a9-fa10-453b-af37-dc7897e98fb1").build();
        membership.getPlainAttrs().add(new AttrTO.Builder().schema("aLong").value("1977").build());
        user.getMemberships().add(membership);

        user = createUser(user).getAny();
        assertNotNull(user.getKey());

        userService.delete(user.getKey());
    }

    @Test
    public void onGroupDelete() {
        // pre: create group with type extension
        TypeExtensionTO typeExtension = new TypeExtensionTO();
        typeExtension.setAnyType(AnyTypeKind.USER.name());
        typeExtension.getAuxClasses().add("csv");
        typeExtension.getAuxClasses().add("other");

        GroupTO groupTO = GroupITCase.getBasicSampleTO("typeExt");
        groupTO.getTypeExtensions().add(typeExtension);
        groupTO = createGroup(groupTO).getAny();
        assertNotNull(groupTO);

        // pre: create user with membership to such group
        UserTO user = UserITCase.getUniqueSampleTO("typeExt@apache.org");

        MembershipTO membership = new MembershipTO.Builder().group(groupTO.getKey()).build();
        membership.getPlainAttrs().add(new AttrTO.Builder().schema("aLong").value("1454").build());
        user.getMemberships().add(membership);

        user = createUser(user).getAny();

        // verify that 'aLong' is correctly populated for user's membership
        assertEquals(1, user.getMemberships().size());
        membership = user.getMembershipMap().get(groupTO.getKey());
        assertNotNull(membership);
        assertEquals(1, membership.getPlainAttrMap().get("aLong").getValues().size());
        assertEquals("1454", membership.getPlainAttrMap().get("aLong").getValues().get(0));

        // verify that derived attrbutes from 'csv' and 'other' are also populated for user's membership
        assertFalse(membership.getDerAttrMap().get("csvuserid").getValues().isEmpty());
        assertFalse(membership.getDerAttrMap().get("noschema").getValues().isEmpty());

        // now remove the group -> all related memberships should have been removed as well
        groupService.delete(groupTO.getKey());

        // re-read user and verify that no memberships are available any more
        user = userService.read(user.getKey());
        assertTrue(user.getMemberships().isEmpty());
    }
}
