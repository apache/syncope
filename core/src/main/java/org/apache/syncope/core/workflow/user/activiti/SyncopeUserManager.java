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
import java.util.Map;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.UserQueryImpl;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.IdentityInfoEntity;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.UserIdentityManager;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class SyncopeUserManager implements UserIdentityManager, SyncopeSession {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Override
    public Class<?> getType() {
        return UserIdentityManager.class;
    }

    @Override
    public Boolean checkPassword(final String userId, final String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public User createNewUser(final String userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserQuery createNewUserQuery() {
        return new SyncopeUserQueryImpl(userDAO, roleDAO, entitlementDAO);
    }

    @Override
    public void deleteUser(final String userId) {
        throw new UnsupportedOperationException();
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
    public UserEntity findUserById(final String userId) {
        UserEntity result = null;
        SyncopeUser user = userDAO.find(userId);
        if (user != null) {
            result = new UserEntity(userId);
        }

        return result;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    public void insertUser(final User user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateUser(final UserEntity updatedUser) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<User> findUserByQueryCriteria(final UserQueryImpl query, final Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long findUserCountByQueryCriteria(final UserQueryImpl query) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentityInfoEntity findUserInfoByUserIdAndKey(final String userId, final String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> findUserInfoKeysByUserIdAndType(final String userId, final String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<User> findPotentialStarterUsers(final String proceDefId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<User> findUsersByNativeQuery(final Map<String, Object> parameterMap,
            final int firstResult, final int maxResults) {

        throw new UnsupportedOperationException();
    }

    @Override
    public long findUserCountByNativeQuery(final Map<String, Object> parameterMap) {
        throw new UnsupportedOperationException();
    }
}
