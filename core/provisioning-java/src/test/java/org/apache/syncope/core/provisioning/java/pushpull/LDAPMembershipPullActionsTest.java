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
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.JobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;

public class LDAPMembershipPullActionsTest extends AbstractTest {

    private LDAPMembershipPullActions ldapMembershipPullActions;

    @Mock
    protected AnyTypeDAO anyTypeDAO;

    @Mock
    protected GroupDAO groupDAO;

    @Mock
    private ProvisioningProfile<?, ?> profile;

    @Mock
    private EntityTO entity;

    @Mock
    private GroupTO groupEntity;

    @Mock
    private AnyPatch anyPatch;

    @Mock
    private SyncDelta syncDelta;

    @Mock
    private Map<String, Set<String>> membershipsBefore;

    @Mock
    private UMembership uMembership;

    @Mock
    private User user;

    @BeforeEach
    public void before() {
        ldapMembershipPullActions = new LDAPMembershipPullActions();

        List<UMembership> uMembList = new ArrayList<>();
        uMembList.add(uMembership);

        Mockito.when(groupDAO.findUMemberships(groupDAO.find(entity.getKey()))).thenReturn(uMembList);
        Mockito.when(uMembership.getLeftEnd()).thenReturn(user);
        Mockito.when(user.getKey()).thenReturn("userTestKey");

        ReflectionTestUtils.setField(ldapMembershipPullActions, "groupDAO", groupDAO);
        ReflectionTestUtils.setField(ldapMembershipPullActions, "membershipsBefore", membershipsBefore);
    }

    @Test
    public void testBeforeUpdateWithGroupTOAndEmptyMemberships() throws JobExecutionException {
        Set<String> memb = new HashSet<>();

        Mockito.when(membershipsBefore.get(uMembership.getLeftEnd().getKey())).thenReturn(memb);
        ldapMembershipPullActions.beforeUpdate(profile, syncDelta, groupEntity, anyPatch);

        Assertions.assertTrue(groupEntity instanceof GroupTO);
        Assertions.assertEquals(1, memb.size());
    }

    @Test
    public void testBeforeUpdate() throws JobExecutionException {
        Set<String> memb = new HashSet<>();
        memb.add("testMemb");

        Mockito.when(membershipsBefore.get(uMembership.getLeftEnd().getKey())).thenReturn(memb);
        ldapMembershipPullActions.beforeUpdate(profile, syncDelta, entity, anyPatch);

        Assertions.assertTrue(!(entity instanceof GroupTO));
        Assertions.assertEquals(2, memb.size());
    }

}
