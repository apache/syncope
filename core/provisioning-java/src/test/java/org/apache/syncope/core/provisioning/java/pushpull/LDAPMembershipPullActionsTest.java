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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAProvision;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.JobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;

public class LDAPMembershipPullActionsTest extends AbstractTest {

    @Mock
    private AnyTypeDAO anyTypeDAO;

    @Mock
    private GroupDAO groupDAO;

    @Mock
    private InboundMatcher inboundMatcher;

    @InjectMocks
    private LDAPMembershipPullActions ldapMembershipPullActions;

    @Mock
    private SyncDelta syncDelta;

    @Mock
    private ProvisioningProfile<?, ?> profile;

    @Mock
    private ProvisioningReport result;

    private EntityTO entity;

    private AnyPatch anyPatch;

    private Map<String, Set<String>> membershipsBefore;

    private User user;

    @BeforeEach
    public void before() {
        List<UMembership> uMembList = new ArrayList<>();
        UMembership uMembership = new JPAUMembership();
        user = new JPAUser();
        uMembership.setLeftEnd(user);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID().toString());
        uMembList.add(uMembership);

        anyPatch = new UserPatch();
        membershipsBefore = new HashMap<>();
        ReflectionTestUtils.setField(ldapMembershipPullActions, "membershipsBefore", membershipsBefore);

        lenient().when(groupDAO.findUMemberships(groupDAO.find(anyString()))).thenReturn(uMembList);
    }

    @Test
    public void testBeforeUpdateWithGroupTOAndEmptyMemberships() throws JobExecutionException {
        entity = new GroupTO();
        entity.setKey(UUID.randomUUID().toString());
        Set<String> expected = new HashSet<>();
        expected.add(entity.getKey());

        ldapMembershipPullActions.beforeUpdate(profile, syncDelta, entity, anyPatch);

        assertTrue(entity instanceof GroupTO);
        assertEquals(1, membershipsBefore.get(user.getKey()).size());
        assertEquals(expected, membershipsBefore.get(user.getKey()));
    }

    @Test
    public void testBeforeUpdate() throws JobExecutionException {
        entity = new UserTO();
        entity.setKey(UUID.randomUUID().toString());
        Set<String> memb = new HashSet<>();
        memb.add(entity.getKey());
        membershipsBefore.put(user.getKey(), memb);

        ldapMembershipPullActions.beforeUpdate(profile, syncDelta, entity, anyPatch);

        assertTrue(!(entity instanceof GroupTO));
        assertEquals(1, membershipsBefore.get(user.getKey()).size());
    }

    @Test
    public void testAfterWithEmptyAttributes(@Mock Attribute attribute, @Mock Connector connector,
            @Mock ConnectorObject connectorObj, @Mock ConnInstance connInstance,
            @Mock ExternalResource externalResource,
            @Mock ProvisioningTask provisioningTask) throws JobExecutionException {
        ConnConfPropSchema connConfPropSchema = new ConnConfPropSchema();
        connConfPropSchema.setName("testSchemaName");
        ConnConfProperty connConfProperty = new ConnConfProperty();
        connConfProperty.setSchema(connConfPropSchema);
        Set<ConnConfProperty> connConfProperties = new HashSet<>();
        connConfProperties.add(connConfProperty);
        entity = new GroupTO();
        Optional provision = Optional.of(new JPAProvision());

        when(profile.getTask()).thenReturn(provisioningTask);
        when(provisioningTask.getResource()).thenReturn(externalResource);
        when(anyTypeDAO.findUser()).thenReturn(new JPAAnyType());
        when(externalResource.getProvision(any(AnyType.class))).thenReturn(provision);

        when(profile.getConnector()).thenReturn(connector);
        when(syncDelta.getObject()).thenReturn(connectorObj);
        when(connectorObj.getAttributeByName(anyString())).thenReturn(attribute);
        when(connector.getConnInstance()).thenReturn(connInstance);
        when(connInstance.getConf()).thenReturn(connConfProperties);

        ldapMembershipPullActions.after(profile, syncDelta, entity, result);

        assertTrue(entity instanceof GroupTO);
        assertTrue(provision.isPresent());
        assertEquals(new LinkedList<>(), attribute.getValue());
    }

}
