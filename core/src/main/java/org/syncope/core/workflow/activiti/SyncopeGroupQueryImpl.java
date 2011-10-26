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
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.RoleDAO;

public class SyncopeGroupQueryImpl implements GroupQuery {

    private RoleDAO roleDAO;

    private Long roleId;

    private List<Group> result;

    public SyncopeGroupQueryImpl(final RoleDAO roleDAO) {
        this.roleDAO = roleDAO;
    }

    @Override
    public GroupQuery groupId(final String groupId) {
        try {
            roleId = Long.valueOf(groupId);
        } catch (NumberFormatException e) {
        }

        return this;
    }

    @Override
    public GroupQuery groupName(final String groupName) {
        return this;
    }

    @Override
    public GroupQuery groupNameLike(final String groupNameLike) {
        return this;
    }

    @Override
    public GroupQuery groupType(final String groupType) {
        return this;
    }

    @Override
    public GroupQuery groupMember(final String groupMemberUserId) {
        return this;
    }

    @Override
    public GroupQuery orderByGroupId() {
        return this;
    }

    @Override
    public GroupQuery orderByGroupName() {
        return this;
    }

    @Override
    public GroupQuery orderByGroupType() {
        return this;
    }

    @Override
    public GroupQuery asc() {
        return this;
    }

    @Override
    public GroupQuery desc() {
        return this;
    }

    private Group fromSyncopeRole(SyncopeRole role) {
        return new GroupEntity(role.getId().toString());
    }

    private void execute() {
        if (roleId != null) {
            SyncopeRole role = roleDAO.find(roleId);
            if (role != null) {
                result = Collections.singletonList(fromSyncopeRole(role));
            } else {
                result = Collections.EMPTY_LIST;
            }
        }
        if (result == null) {
            result = new ArrayList<Group>();
            for (SyncopeRole role : roleDAO.findAll()) {
                result.add(fromSyncopeRole(role));
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
    public Group singleResult() {
        if (result == null) {
            execute();
        }
        if (result.isEmpty()) {
            throw new ActivitiException("Empty result");
        }

        return result.get(0);
    }

    @Override
    public List<Group> list() {
        if (result == null) {
            execute();
        }
        return result;
    }

    @Override
    public List<Group> listPage(final int firstResult, final int maxResults) {
        throw new UnsupportedOperationException();
    }
}
