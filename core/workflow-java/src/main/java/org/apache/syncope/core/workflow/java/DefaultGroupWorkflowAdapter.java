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
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;

/**
 * Simple implementation basically not involving any workflow engine.
 */
public class DefaultGroupWorkflowAdapter extends AbstractGroupWorkflowAdapter {

    public DefaultGroupWorkflowAdapter(
            final GroupDataBinder dataBinder,
            final GroupDAO groupDAO,
            final EntityFactory entityFactory) {

        super(dataBinder, groupDAO, entityFactory);
    }

    @Override
    protected WorkflowResult<String> doCreate(final GroupCR groupCR, final String creator, final String context) {
        Group group = entityFactory.newEntity(Group.class);
        dataBinder.create(group, groupCR);
        metadata(group, creator, context);
        group = groupDAO.saveAndRefreshDynMemberships(group);

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.set(ResourceOperation.CREATE, groupDAO.findAllResourceKeys(group.getKey()));

        return new WorkflowResult<>(group.getKey(), propByRes, "create");
    }

    @Override
    protected WorkflowResult<GroupUR> doUpdate(
            final Group group, final GroupUR groupUR, final String updater, final String context) {

        PropagationByResource<String> propByRes = dataBinder.update(group, groupUR);
        metadata(group, updater, context);
        groupDAO.save(group);

        return new WorkflowResult<>(groupUR, propByRes, "update");
    }

    @Override
    protected void doDelete(final Group group) {
        groupDAO.delete(group);
    }
}
