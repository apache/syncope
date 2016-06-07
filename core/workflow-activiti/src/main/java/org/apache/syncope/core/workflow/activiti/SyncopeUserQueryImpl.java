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
import org.activiti.engine.ActivitiException;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;

public class SyncopeUserQueryImpl implements UserQuery {

    private final UserDAO userDAO;

    private final GroupDAO groupDAO;

    private String username;

    private String memberOf;

    private List<User> result;

    public SyncopeUserQueryImpl(final UserDAO userDAO, final GroupDAO groupDAO) {
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
    }

    @Override
    public UserQuery userId(final String id) {
        this.username = id;
        return this;
    }

    @Override
    public UserQuery userFirstName(final String firstName) {
        return this;
    }

    @Override
    public UserQuery userFirstNameLike(final String firstNameLike) {
        return this;
    }

    @Override
    public UserQuery userLastName(final String lastName) {
        return this;
    }

    @Override
    public UserQuery userLastNameLike(final String lastNameLike) {
        return this;
    }

    @Override
    public UserQuery userFullNameLike(final String fullNameLike) {
        return this;
    }

    @Override
    public UserQuery userEmail(final String email) {
        return this;
    }

    @Override
    public UserQuery userEmailLike(final String emailLike) {
        return this;
    }

    @Override
    public UserQuery memberOfGroup(final String groupId) {
        memberOf = groupId;
        return this;
    }

    @Override
    public UserQuery orderByUserId() {
        return this;
    }

    @Override
    public UserQuery orderByUserFirstName() {
        return this;
    }

    @Override
    public UserQuery orderByUserLastName() {
        return this;
    }

    @Override
    public UserQuery orderByUserEmail() {
        return this;
    }

    @Override
    public UserQuery asc() {
        return this;
    }

    @Override
    public UserQuery desc() {
        return this;
    }

    private User fromSyncopeUser(final org.apache.syncope.core.persistence.api.entity.user.User user) {
        return new UserEntity(user.getUsername());
    }

    private void execute() {
        if (username != null) {
            org.apache.syncope.core.persistence.api.entity.user.User user = userDAO.findByUsername(username);
            if (user == null) {
                result = Collections.<User>emptyList();
            } else if (memberOf == null || userDAO.findAllGroupNames(user).contains(memberOf)) {
                result = Collections.singletonList(fromSyncopeUser(user));
            }
        }
        if (memberOf != null) {
            Group group = groupDAO.findByName(memberOf);
            if (group == null) {
                result = Collections.<User>emptyList();
            } else {
                result = new ArrayList<>();
                List<UMembership> memberships = groupDAO.findUMemberships(group);
                for (UMembership membership : memberships) {
                    User user = fromSyncopeUser(membership.getLeftEnd());
                    if (!result.contains(user)) {
                        result.add(user);
                    }
                }
            }
        }
        // THIS CAN BE *VERY* DANGEROUS
        if (result == null) {
            result = CollectionUtils.collect(userDAO.findAll(),
                    new Transformer<org.apache.syncope.core.persistence.api.entity.user.User, User>() {

                @Override
                public User transform(final org.apache.syncope.core.persistence.api.entity.user.User user) {
                    return fromSyncopeUser(user);
                }

            }, new ArrayList<User>());
        }
    }

    @Override
    public long count() {
        if (result == null) {
            execute();
        }
        return result.size();
    }

    @Override
    public User singleResult() {
        if (result == null) {
            execute();
        }
        if (result.isEmpty()) {
            throw new ActivitiException("Empty result");
        }

        return result.get(0);
    }

    @Override
    public List<User> list() {
        if (result == null) {
            execute();
        }
        return result;
    }

    @Override
    public List<User> listPage(final int firstResult, final int maxResults) {
        if (result == null) {
            execute();
        }
        return result.subList(firstResult, firstResult + maxResults - 1);
    }

    @Override
    public UserQuery potentialStarter(final String string) {
        throw new UnsupportedOperationException();
    }
}
