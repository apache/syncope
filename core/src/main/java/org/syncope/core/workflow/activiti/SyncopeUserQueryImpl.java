/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.workflow.activiti;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.util.EntitlementUtil;

public class SyncopeUserQueryImpl implements UserQuery {

    private UserDAO userDAO;

    private RoleDAO roleDAO;

    private EntitlementDAO entitlementDAO;

    private String username;

    private Long memberOf;

    private List<User> result;

    public SyncopeUserQueryImpl(final UserDAO userDAO,
            final RoleDAO roleDAO, final EntitlementDAO entitlementDAO) {

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

    private User fromSyncopeUser(final SyncopeUser syncopeUser) {
        return new UserEntity(syncopeUser.getUsername());
    }

    private void execute() {
        if (username != null) {
            SyncopeUser user = userDAO.find(username);
            if (user != null) {
                if (memberOf == null || user.getRoleIds().contains(memberOf)) {
                    result = Collections.singletonList(fromSyncopeUser(user));
                }
            } else {
                result = Collections.EMPTY_LIST;
            }
        }
        if (memberOf != null) {
            SyncopeRole role = roleDAO.find(memberOf);
            if (role != null) {
                result = new ArrayList<User>();
                List<Membership> memberships = roleDAO.findMemberships(role);
                User user;
                for (Membership membership : memberships) {
                    user = fromSyncopeUser(membership.getSyncopeUser());
                    if (!result.contains(user)) {
                        result.add(user);
                    }
                }
            } else {
                result = Collections.EMPTY_LIST;
            }
        }
        // THIS CAN BE *VERY* DANGEROUS
        if (result == null) {
            result = new ArrayList<User>();
            for (SyncopeUser user : userDAO.findAll(
                    EntitlementUtil.getRoleIds(entitlementDAO.findAll()))) {

                result.add(fromSyncopeUser(user));
            }
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
    public List<User> listPage(final int firstResult,
            final int maxResults) {
        throw new UnsupportedOperationException();
    }
}
