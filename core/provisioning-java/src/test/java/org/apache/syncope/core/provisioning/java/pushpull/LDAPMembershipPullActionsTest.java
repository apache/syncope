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
import static org.mockito.Mockito.verify;
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
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PullMatch;
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
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
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

    @Mock
    private Map<String, Set<String>> membershipsAfter;

    @Mock
    private ProvisioningTask provisioningTask;

    @Mock
    private ExternalResource externalResource;

    @Mock
    private Connector connector;

    @Mock
    private ConnectorObject connectorObj;

    @Mock
    private ConnInstance connInstance;

    private EntityTO entity;

    private AnyPatch anyPatch;

    private Map<String, Set<String>> membershipsBefore;

    private User user;

    Set<ConnConfProperty> connConfProperties;

    @BeforeEach
    public void initTest() {
        List<UMembership> uMembList = new ArrayList<>();
        UMembership uMembership = new JPAUMembership();
        user = new JPAUser();
        uMembership.setLeftEnd(user);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID().toString());
        uMembList.add(uMembership);

        anyPatch = new UserPatch();
        membershipsBefore = new HashMap<>();
        ReflectionTestUtils.setField(ldapMembershipPullActions, "membershipsBefore", membershipsBefore);
        ReflectionTestUtils.setField(ldapMembershipPullActions, "membershipsAfter", membershipsAfter);

        lenient().when(groupDAO.findUMemberships(groupDAO.find(anyString()))).thenReturn(uMembList);

        ConnConfPropSchema connConfPropSchema = new ConnConfPropSchema();
        connConfPropSchema.setName("testSchemaName");
        ConnConfProperty connConfProperty = new ConnConfProperty();
        connConfProperty.setSchema(connConfPropSchema);
        connConfProperties = new HashSet<>();
        connConfProperties.add(connConfProperty);

        lenient().when(profile.getTask()).thenReturn(provisioningTask);
        lenient().when(provisioningTask.getResource()).thenReturn(externalResource);
        lenient().when(anyTypeDAO.findUser()).thenReturn(new JPAAnyType());

        lenient().when(profile.getConnector()).thenReturn(connector);
        lenient().when(syncDelta.getObject()).thenReturn(connectorObj);
        lenient().when(connector.getConnInstance()).thenReturn(connInstance);
        lenient().when(connInstance.getConf()).thenReturn(connConfProperties);
    }

    @Test
    public void beforeUpdateWithGroupTOAndEmptyMemberships() throws JobExecutionException {
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
    public void beforeUpdate() throws JobExecutionException {
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
    public void afterWithEmptyAttributes(@Mock Attribute attribute) throws JobExecutionException {
        entity = new GroupTO();
        Optional provision = Optional.of(new JPAProvision());

        when(connectorObj.getAttributeByName(anyString())).thenReturn(attribute);
        when(externalResource.getProvision(any(AnyType.class))).thenReturn(provision);

        ldapMembershipPullActions.after(profile, syncDelta, entity, result);

        assertTrue(entity instanceof GroupTO);
        assertTrue(provision.isPresent());
        assertEquals(new LinkedList<>(), attribute.getValue());
    }

    @Test
    public void after() throws JobExecutionException {
        entity = new UserTO();
        Optional provision = Optional.empty();
        Optional match = Optional.of(new PullMatch(MatchType.ANY, user));
        String expectedUid = UUID.randomUUID().toString();
        Attribute attribute = new Uid(expectedUid);
        List<Object> expected = new LinkedList<>();
        expected.add(expectedUid);

        when(connectorObj.getAttributeByName(anyString())).thenReturn(attribute);
        when(externalResource.getProvision(any(AnyType.class))).thenReturn(provision);
        when(inboundMatcher.match(any(AnyType.class), anyString(), any(ExternalResource.class), any(Connector.class))).
                thenReturn(match);

        ldapMembershipPullActions.after(profile, syncDelta, entity, result);

        verify(membershipsAfter).get(anyString());
        verify(membershipsAfter).put(anyString(), any());
        assertTrue(!(entity instanceof GroupTO));
        assertTrue(!provision.isPresent());
        assertEquals(expected, attribute.getValue());
        assertTrue(match.isPresent());
    }

    @Test
    public void afterAll(
            @Mock Map<String, Object> jobMap,
            @Mock SchedulerFactoryBean schedulerFactoryBean,
            @Mock Scheduler scheduler) throws JobExecutionException, SchedulerException {
        ReflectionTestUtils.setField(ldapMembershipPullActions, "scheduler", schedulerFactoryBean);
        when(schedulerFactoryBean.getScheduler()).thenReturn(scheduler);
        
        ldapMembershipPullActions.afterAll(profile);

        verify(scheduler).scheduleJob(any(), any());
    }

}
