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
package org.apache.syncope.core.workflow.java;

import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = { Throwable.class })
public abstract class AbstractGroupWorkflowAdapter extends AbstractWorkflowAdapter implements GroupWorkflowAdapter {

    protected final GroupDataBinder dataBinder;

    public AbstractGroupWorkflowAdapter(
            final GroupDataBinder dataBinder,
            final GroupDAO groupDAO,
            final EntityFactory entityFactory,
            final ApplicationEventPublisher publisher) {

        super(groupDAO, entityFactory, publisher);

        this.dataBinder = dataBinder;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    protected abstract WorkflowResult<String> doCreate(GroupCR groupCR, String creator, String context);

    @Override
    public WorkflowResult<String> create(final GroupCR groupCR, final String creator, final String context) {
        return doCreate(groupCR, creator, context);
    }

    protected abstract WorkflowResult<GroupUR> doUpdate(Group group, GroupUR groupUR, String updater, String context);

    @Override
    public WorkflowResult<GroupUR> update(final GroupUR groupUR, final String updater, final String context) {
        WorkflowResult<GroupUR> result = doUpdate(groupDAO.authFind(groupUR.getKey()), groupUR, updater, context);

        // re-read to ensure that requester's administration rights are still valid
        groupDAO.authFind(groupUR.getKey());

        return result;
    }

    protected abstract void doDelete(Group group, String eraser, String context);

    @Override
    public void delete(final String groupKey, final String eraser, final String context) {
        doDelete(groupDAO.authFind(groupKey), eraser, context);
    }
}
