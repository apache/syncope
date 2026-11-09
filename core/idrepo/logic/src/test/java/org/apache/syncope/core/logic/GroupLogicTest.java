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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

public class GroupLogicTest {

    private RealmSearchDAO realmSearchDAO;

    private AnyTypeDAO anyTypeDAO;

    private UserDAO userDAO;

    private GroupDAO groupDAO;

    private SecurityProperties securityProperties;

    private AnySearchDAO searchDAO;

    private GroupDataBinder binder;

    private GroupProvisioningManager provisioningManager;

    private GroupLogic logic;

    @BeforeEach
    void setUp() {
        realmSearchDAO = mock(RealmSearchDAO.class);
        anyTypeDAO = mock(AnyTypeDAO.class);
        userDAO = mock(UserDAO.class);
        groupDAO = mock(GroupDAO.class);
        searchDAO = mock(AnySearchDAO.class);
        binder = mock(GroupDataBinder.class);
        provisioningManager = mock(GroupProvisioningManager.class);

        securityProperties = new SecurityProperties();
        securityProperties.setAdminUser("admin");

        logic = new GroupLogic(
                realmSearchDAO,
                anyTypeDAO,
                mock(TemplateUtils.class),
                userDAO,
                groupDAO,
                securityProperties,
                searchDAO,
                mock(ImplementationDAO.class),
                mock(TaskDAO.class),
                binder,
                provisioningManager,
                mock(TaskDataBinder.class),
                mock(JobManager.class),
                mock(SyncopeTaskScheduler.class),
                mock(EntityFactory.class));
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void read() {
        GroupTO to = new GroupTO();
        to.setKey("key");
        when(binder.getGroupTO("key")).thenReturn(to);

        assertSame(to, logic.read("key"));
    }

    @Test
    void ownForAdmin() {
        LogicTestAuth.authenticate("admin");

        assertEquals(List.of(), logic.own());
    }

    @Test
    void ownWithMissingUser() {
        LogicTestAuth.authenticate("user");
        doReturn(Optional.empty()).when(userDAO).findByUsername("user");

        assertThrows(NotFoundException.class, () -> logic.own());
    }

    @Test
    void own() {
        LogicTestAuth.authenticate("user");

        User user = mock(User.class);
        Group group = mock(Group.class);
        GroupTO to = new GroupTO();
        to.setKey("key");

        doReturn(Optional.of(user)).when(userDAO).findByUsername("user");
        when(userDAO.findAllGroups(user)).thenReturn(List.of(group));
        when(binder.getGroupTO(group, true)).thenReturn(to);

        assertEquals(List.of(to), logic.own());
    }

    @Test
    void searchMissingRealm() {
        doReturn(Optional.empty()).when(realmSearchDAO).findByFullPath("/missing");

        assertThrows(NotFoundException.class,
                () -> logic.search(null, Pageable.unpaged(), "/missing", true, false));
    }

    @Test
    void search() {
        Realm realm = mock(Realm.class);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(searchDAO.getAllMatchingCond()).thenReturn(mock(SearchCond.class));
        when(searchDAO.count(eq(realm), eq(true), any(), any(SearchCond.class), any())).thenReturn(0L);
        when(searchDAO.search(
                eq(realm), eq(true), any(), any(SearchCond.class), any(Pageable.class), any())).
                thenReturn(List.of());

        assertEquals(0, logic.search(null, Pageable.unpaged(),
                SyncopeConstants.ROOT_REALM, true, false).getTotalElements());
    }

    @Test
    void create() {
        GroupCR createReq = new GroupCR.Builder(SyncopeConstants.ROOT_REALM, "group").build();
        Realm realm = mock(Realm.class);
        GroupTO to = new GroupTO();
        to.setKey("key");

        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(anyTypeDAO.getGroup()).thenReturn(mock(org.apache.syncope.core.persistence.api.entity.AnyType.class));
        when(provisioningManager.create(createReq, false, "unauthenticated", "REST")).
                thenReturn(new ProvisioningManager.ProvisioningResult<>("key", List.of()));
        when(binder.getGroupTO("key")).thenReturn(to);

        assertSame(to, logic.create(createReq, false).getEntity());
    }

    @Test
    void update() {
        GroupUR updateReq = new GroupUR.Builder("key").build();
        Realm realm = mock(Realm.class);
        GroupTO groupTO = new GroupTO();
        groupTO.setKey("key");
        groupTO.setRealm(SyncopeConstants.ROOT_REALM);

        when(binder.getGroupTO("key")).thenReturn(groupTO);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(provisioningManager.update(updateReq, Set.of(), false, "unauthenticated", "REST")).
                thenReturn(new ProvisioningManager.ProvisioningResult<>(updateReq, List.of()));

        assertSame(groupTO, logic.update(updateReq, false).getEntity());
    }

    @Test
    void deleteManager() {
        GroupTO groupTO = new GroupTO();
        groupTO.setKey("key");
        groupTO.setRealm(SyncopeConstants.ROOT_REALM);
        Realm realm = mock(Realm.class);

        when(binder.getGroupTO("key")).thenReturn(groupTO);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(groupDAO.isManager("key")).thenReturn(true);

        assertThrows(SyncopeClientException.class, () -> logic.delete("key", false));
    }

    @Test
    void unlink() {
        GroupTO groupTO = new GroupTO();
        groupTO.setKey("key");
        groupTO.setRealm(SyncopeConstants.ROOT_REALM);
        GroupTO unlinked = new GroupTO();
        unlinked.setKey("key");
        Realm realm = mock(Realm.class);

        when(binder.getGroupTO("key")).thenReturn(groupTO, unlinked);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(provisioningManager.unlink(any(GroupUR.class), eq("unauthenticated"), eq("REST"))).thenReturn("key");

        assertSame(unlinked, logic.unlink("key", List.of("resource")));
    }

    @Test
    void deprovision() {
        GroupTO groupTO = new GroupTO();
        groupTO.setKey("key");
        groupTO.setRealm(SyncopeConstants.ROOT_REALM);
        Realm realm = mock(Realm.class);
        PropagationStatus status = mock(PropagationStatus.class);
        when(status.getResource()).thenReturn("resource");

        when(binder.getGroupTO("key")).thenReturn(groupTO);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(provisioningManager.deprovision("key", List.of("resource"), false, "unauthenticated")).
                thenReturn(List.of(status));

        assertEquals(List.of(status), logic.deprovision("key", List.of("resource"), false).getPropagationStatuses());
    }
}
