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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.rules.InboundMatch;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class LDAPMembershipPullActionsTest extends AbstractTest {

    @Autowired
    private EntityFactory entityFactory;

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
    private ProvisioningTask<?> pullTask;

    @Mock
    private ExternalResource resource;

    @Mock
    private Provision provision;

    @Mock
    private Connector connector;

    @Mock
    private ConnectorObject connectorObj;

    @Mock
    private ConnInstance connInstance;

    private EntityTO entity;

    private AnyUR anyReq;

    private Map<String, Set<String>> membershipsAfter;

    private Map<String, Set<String>> membershipsBefore;

    private User user;

    private List<ConnConfProperty> connConfProperties;

    @BeforeEach
    public void initTest() {
        user = entityFactory.newEntity(User.class);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID().toString());

        UMembership uMembership = entityFactory.newEntity(UMembership.class);
        uMembership.setLeftEnd(user);

        anyReq = new UserUR();

        membershipsBefore = new HashMap<>();
        ReflectionTestUtils.setField(ldapMembershipPullActions, "membershipsBefore", membershipsBefore);
        membershipsAfter = new HashMap<>();
        ReflectionTestUtils.setField(ldapMembershipPullActions, "membershipsAfter", membershipsAfter);

        lenient().when(groupDAO.findById(anyString())).thenAnswer(ic -> Optional.of(mock(Group.class)));
        lenient().when(groupDAO.findUMemberships(any(Group.class))).thenReturn(List.of(uMembership));

        ConnConfPropSchema connConfPropSchema = new ConnConfPropSchema();
        connConfPropSchema.setName("testSchemaName");
        ConnConfProperty connConfProperty = new ConnConfProperty();
        connConfProperty.setSchema(connConfPropSchema);
        connConfProperties = List.of(connConfProperty);

        lenient().when(profile.getTask()).thenAnswer(ic -> pullTask);
        lenient().when(pullTask.getResource()).thenReturn(resource);
        lenient().when(resource.getProvisionByAnyType(anyString())).thenReturn(Optional.of(provision));
        lenient().when(provision.getMapping()).thenReturn(new Mapping());
        lenient().when(anyTypeDAO.getUser()).thenAnswer(ic -> {
            AnyType userAnyType = mock(AnyType.class);
            lenient().when(userAnyType.getKey()).thenReturn(AnyTypeKind.USER.name());
            return userAnyType;
        });

        lenient().when(profile.getConnector()).thenReturn(connector);
        lenient().when(syncDelta.getObject()).thenReturn(connectorObj);
        lenient().when(connector.getConnInstance()).thenReturn(connInstance);
        lenient().when(connInstance.getConf()).thenReturn(connConfProperties);
    }

    @Test
    public void beforeUpdateWithGroupTOAndEmptyMemberships() throws JobExecutionException {
        entity = new GroupTO();
        entity.setKey(UUID.randomUUID().toString());

        ldapMembershipPullActions.beforeUpdate(profile, syncDelta, entity, anyReq);

        assertTrue(entity instanceof GroupTO);
        assertEquals(1, membershipsBefore.get(user.getKey()).size());
        assertEquals(Set.of(entity.getKey()), membershipsBefore.get(user.getKey()));
    }

    @Test
    public void beforeUpdate() throws JobExecutionException {
        entity = new UserTO();
        entity.setKey(UUID.randomUUID().toString());

        membershipsBefore.put(user.getKey(), Set.of(entity.getKey()));

        ldapMembershipPullActions.beforeUpdate(profile, syncDelta, entity, anyReq);

        assertFalse(entity instanceof GroupTO);
        assertEquals(1, membershipsBefore.get(user.getKey()).size());
    }

    @Test
    public void after() throws JobExecutionException {
        entity = new GroupTO();
        entity.setKey(UUID.randomUUID().toString());

        when(connectorObj.getAttributeByName(anyString())).thenReturn(new Uid(UUID.randomUUID().toString()));
        when(inboundMatcher.match(any(AnyType.class), anyString(), any(ExternalResource.class), any(Connector.class))).
                thenReturn(Optional.of(new InboundMatch(MatchType.ANY, user)));

        ldapMembershipPullActions.after(profile, syncDelta, entity, result);

        assertEquals(1, membershipsAfter.get(user.getKey()).size());
        assertEquals(entity.getKey(), membershipsAfter.get(user.getKey()).iterator().next());
    }
}
