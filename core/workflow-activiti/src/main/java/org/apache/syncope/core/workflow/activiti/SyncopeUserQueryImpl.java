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
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.Group;

public class SyncopeUserQueryImpl implements UserQuery {

    private UserDAO userDAO;

    private GroupDAO groupDAO;

    private String username;

    private Long memberOf;

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
        try {
            memberOf = Long.valueOf(groupId);
        } catch (NumberFormatException e) {
            // ignore
        }
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

    private void execute(final int page, final int itemsPerPage) {
        if (username != null) {
            org.apache.syncope.core.persistence.api.entity.user.User user = userDAO.find(username);
            if (user == null) {
                result = Collections.<User>emptyList();
            } else {
                if (memberOf == null || user.getGroupKeys().contains(memberOf)) {
                    result = Collections.singletonList(fromSyncopeUser(user));
                }
            }
        }
        if (memberOf != null) {
            Group group = groupDAO.find(memberOf);
            if (group == null) {
                result = Collections.<User>emptyList();
            } else {
                result = new ArrayList<>();
                List<Membership> memberships = groupDAO.findMemberships(group);
                User user;
                for (Membership membership : memberships) {
                    user = fromSyncopeUser(membership.getUser());
                    if (!result.contains(user)) {
                        result.add(user);
                    }
                }
            }
        }
        // THIS CAN BE *VERY* DANGEROUS
        if (result == null) {
            result = CollectionUtils.collect(
                    userDAO.findAll(SyncopeConstants.FULL_ADMIN_REALMS, page, itemsPerPage),
                    new Transformer<org.apache.syncope.core.persistence.api.entity.user.User, User>() {

                        @Override
                        public User transform(final org.apache.syncope.core.persistence.api.entity.user.User user) {
                            return fromSyncopeUser(user);
                        }

                    },
                    new ArrayList<User>());
        }
    }

    @Override
    public long count() {
        if (result == null) {
            execute(-1, -1);
        }
        return result.size();
    }

    @Override
    public User singleResult() {
        if (result == null) {
            execute(-1, -1);
        }
        if (result.isEmpty()) {
            throw new ActivitiException("Empty result");
        }

        return result.get(0);
    }

    @Override
    public List<User> list() {
        if (result == null) {
            execute(-1, -1);
        }
        return result;
    }

    @Override
    public List<User> listPage(final int firstResult, final int maxResults) {
        if (result == null) {
            execute((firstResult / maxResults) + 1, maxResults);
        }
        return result;
    }

    @Override
    public UserQuery potentialStarter(final String string) {
        throw new UnsupportedOperationException();
    }
}
