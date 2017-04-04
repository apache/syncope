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
package org.apache.syncope.core.workflow.flowable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.Picture;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.UserQueryImpl;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.IdentityInfoEntity;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.UserIdentityManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class SyncopeUserManager implements UserIdentityManager, SyncopeSession {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Override
    public Class<?> getType() {
        return UserIdentityManager.class;
    }

    @Override
    public Boolean checkPassword(final String userKey, final String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public User createNewUser(final String userKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserQuery createNewUserQuery() {
        return new SyncopeUserQueryImpl(userDAO, groupDAO);
    }

    @Override
    public void deleteUser(final String userKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Group> findGroupsByUser(final String username) {
        List<Group> result = Collections.emptyList();
        org.apache.syncope.core.persistence.api.entity.user.User user = userDAO.findByUsername(username);
        if (user != null) {
            result = new ArrayList<>();
            for (String groupName : userDAO.findAllGroupNames(user)) {
                result.add(new GroupEntity(groupName));
            }
        }

        return result;
    }

    @Override
    public UserEntity findUserById(final String username) {
        UserEntity result = null;
        org.apache.syncope.core.persistence.api.entity.user.User user = userDAO.findByUsername(username);
        if (user != null) {
            result = new UserEntity(username);
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
    public boolean isNewUser(final User user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateUser(final User updatedUser) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Picture getUserPicture(final String string) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserPicture(final String string, final Picture pctr) {
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
    public IdentityInfoEntity findUserInfoByUserIdAndKey(final String userKey, final String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> findUserInfoKeysByUserIdAndType(final String userKey, final String type) {
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
