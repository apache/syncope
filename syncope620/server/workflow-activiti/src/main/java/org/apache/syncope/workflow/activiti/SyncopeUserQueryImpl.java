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
package org.apache.syncope.workflow.activiti;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.apache.syncope.server.persistence.api.RoleEntitlementUtil;
import org.apache.syncope.server.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.server.persistence.api.dao.RoleDAO;
import org.apache.syncope.server.persistence.api.dao.UserDAO;
import org.apache.syncope.server.persistence.api.entity.membership.Membership;
import org.apache.syncope.server.persistence.api.entity.role.Role;

public class SyncopeUserQueryImpl implements UserQuery {

    private UserDAO userDAO;

    private RoleDAO roleDAO;

    private EntitlementDAO entitlementDAO;

    private String username;

    private Long memberOf;

    private List<User> result;

    public SyncopeUserQueryImpl(final UserDAO userDAO, final RoleDAO roleDAO, final EntitlementDAO entitlementDAO) {
        this.userDAO = userDAO;
        this.roleDAO = roleDAO;
        this.entitlementDAO = entitlementDAO;
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

    private User fromSyncopeUser(final org.apache.syncope.server.persistence.api.entity.user.User user) {
        return new UserEntity(user.getUsername());
    }

    private void execute(final int page, final int itemsPerPage) {
        if (username != null) {
            org.apache.syncope.server.persistence.api.entity.user.User user = userDAO.find(username);
            if (user == null) {
                result = Collections.<User>emptyList();
            } else {
                if (memberOf == null || user.getRoleKeys().contains(memberOf)) {
                    result = Collections.singletonList(fromSyncopeUser(user));
                }
            }
        }
        if (memberOf != null) {
            Role role = roleDAO.find(memberOf);
            if (role == null) {
                result = Collections.<User>emptyList();
            } else {
                result = new ArrayList<>();
                List<Membership> memberships = roleDAO.findMemberships(role);
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
            result = new ArrayList<>();

            List<org.apache.syncope.server.persistence.api.entity.user.User> users =
                    userDAO.findAll(RoleEntitlementUtil.getRoleKeys(entitlementDAO.findAll()), page, itemsPerPage);
            for (org.apache.syncope.server.persistence.api.entity.user.User user : users) {
                result.add(fromSyncopeUser(user));
            }
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
