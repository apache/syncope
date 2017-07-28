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

import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowDefinitionAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = { Throwable.class })
public abstract class AbstractGroupWorkflowAdapter implements GroupWorkflowAdapter, GroupWorkflowDefinitionAdapter {

    @Autowired
    protected GroupDataBinder dataBinder;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected EntityFactory entityFactory;

    @Override
    public String getPrefix() {
        return null;
    }

    protected abstract WorkflowResult<String> doCreate(GroupTO groupTO);

    @Override
    public WorkflowResult<String> create(final GroupTO groupTO) {
        return doCreate(groupTO);
    }

    protected abstract WorkflowResult<String> doUpdate(Group group, GroupPatch groupPatch);

    @Override
    public WorkflowResult<String> update(final GroupPatch groupPatch) {
        WorkflowResult<String> result = doUpdate(groupDAO.authFind(groupPatch.getKey()), groupPatch);

        // re-read to ensure that requester's administration rights are still valid
        groupDAO.authFind(groupPatch.getKey());

        return result;
    }

    protected abstract void doDelete(Group group);

    @Override
    public void delete(final String groupKey) {
        doDelete(groupDAO.authFind(groupKey));
    }
}
