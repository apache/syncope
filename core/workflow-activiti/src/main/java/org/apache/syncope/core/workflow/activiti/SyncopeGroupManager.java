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
package org.apache.syncope.core.workflow.activiti;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.impl.GroupQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.GroupIdentityManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.beans.factory.annotation.Autowired;

public class SyncopeGroupManager implements GroupIdentityManager, SyncopeSession {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Override
    public Class<?> getType() {
        return GroupIdentityManager.class;
    }

    @Override
    public Group createNewGroup(final String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GroupQuery createNewGroupQuery() {
        return new SyncopeGroupQueryImpl(groupDAO);
    }

    @Override
    public void deleteGroup(final String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Group> findGroupsByUser(final String userId) {
        List<Group> result = Collections.emptyList();
        User user = userDAO.findByUsername(userId);
        if (user != null) {
            result = new ArrayList<>();
            for (String groupName : userDAO.findAllGroupNames(user)) {
                result.add(new GroupEntity(groupName));
            }
        }

        return result;
    }

    @Override
    public List<Group> findGroupByQueryCriteria(final GroupQueryImpl query, final Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long findGroupCountByQueryCriteria(final GroupQueryImpl query) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Group> findGroupsByNativeQuery(final Map<String, Object> parameterMap, final int firstResult,
            final int maxResults) {

        throw new UnsupportedOperationException();
    }

    @Override
    public long findGroupCountByNativeQuery(final Map<String, Object> parameterMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertGroup(final Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateGroup(final Group updatedGroup) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNewGroup(final Group group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
