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
import org.activiti.engine.ActivitiException;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;

public class SyncopeGroupQueryImpl implements GroupQuery {

    private final GroupDAO groupDAO;

    private String groupId;

    private List<Group> result;

    public SyncopeGroupQueryImpl(final GroupDAO groupDAO) {
        this.groupDAO = groupDAO;
    }

    @Override
    public GroupQuery groupId(final String groupId) {
        try {
            this.groupId = groupId;
        } catch (NumberFormatException e) {
            // ignore
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

    private Group fromSyncopeGroup(final org.apache.syncope.core.persistence.api.entity.group.Group group) {
        return new GroupEntity(group.getKey());
    }

    private void execute() {
        if (groupId != null) {
            org.apache.syncope.core.persistence.api.entity.group.Group syncopeGroup = groupDAO.findByName(groupId);
            if (syncopeGroup == null) {
                result = Collections.emptyList();
            } else {
                result = Collections.singletonList(fromSyncopeGroup(syncopeGroup));
            }
        }
        if (result == null) {
            result = CollectionUtils.collect(groupDAO.findAll(),
                    new Transformer<org.apache.syncope.core.persistence.api.entity.group.Group, Group>() {

                @Override
                public Group transform(final org.apache.syncope.core.persistence.api.entity.group.Group user) {
                    return fromSyncopeGroup(user);
                }

            }, new ArrayList<Group>());
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
        return list();
    }

    @Override
    public GroupQuery potentialStarter(final String procDefId) {
        throw new UnsupportedOperationException();
    }
}
