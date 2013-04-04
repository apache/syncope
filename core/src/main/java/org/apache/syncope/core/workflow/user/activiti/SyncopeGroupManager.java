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
package org.apache.syncope.core.workflow.user.activiti;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.GroupEntityManager;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class SyncopeGroupManager extends GroupEntityManager implements SyncopeSession {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Override
    public Class<? extends Session> getType() {
        return GroupEntityManager.class;
    }

    @Override
    public Group createNewGroup(final String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GroupQuery createNewGroupQuery() {
        return new SyncopeGroupQueryImpl(roleDAO);
    }

    @Override
    public void deleteGroup(final String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GroupEntity findGroupById(final String groupId) {
        GroupEntity result = null;

        SyncopeRole role = null;
        try {
            role = roleDAO.find(Long.valueOf(groupId));
        } catch (NumberFormatException e) {
        }
        if (role != null) {
            result = new GroupEntity(groupId);
        }

        return result;
    }

    @Override
    public List<Group> findGroupsByUser(final String userId) {
        List<Group> result = Collections.emptyList();
        SyncopeUser user = userDAO.find(userId);
        if (user != null) {
            result = new ArrayList<Group>();
            for (Long roleId : user.getRoleIds()) {
                result.add(new GroupEntity(roleId.toString()));
            }
        }

        return result;
    }

    @Override
    public void insertGroup(final Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateGroup(final GroupEntity updatedGroup) {
        throw new UnsupportedOperationException();
    }
}
