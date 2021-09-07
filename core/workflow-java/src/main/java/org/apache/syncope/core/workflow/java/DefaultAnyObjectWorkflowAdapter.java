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

import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;

/**
 * Simple implementation basically not involving any workflow engine.
 */
public class DefaultAnyObjectWorkflowAdapter extends AbstractAnyObjectWorkflowAdapter {

    public DefaultAnyObjectWorkflowAdapter(
            final AnyObjectDataBinder dataBinder,
            final AnyObjectDAO anyObjectDAO,
            final EntityFactory entityFactory) {

        super(dataBinder, anyObjectDAO, entityFactory);
    }

    @Override
    protected WorkflowResult<String> doCreate(
            final AnyObjectCR anyObjectCR, final String creator, final String context) {

        AnyObject anyObject = entityFactory.newEntity(AnyObject.class);
        dataBinder.create(anyObject, anyObjectCR);
        metadata(anyObject, creator, context);
        anyObject = anyObjectDAO.save(anyObject);

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.set(ResourceOperation.CREATE, anyObjectDAO.findAllResourceKeys(anyObject.getKey()));

        return new WorkflowResult<>(anyObject.getKey(), propByRes, "create");
    }

    @Override
    protected WorkflowResult<AnyObjectUR> doUpdate(
            final AnyObject anyObject, final AnyObjectUR anyObjectUR, final String updater, final String context) {

        PropagationByResource<String> propByRes = dataBinder.update(anyObject, anyObjectUR);
        metadata(anyObject, updater, context);
        anyObjectDAO.save(anyObject);

        return new WorkflowResult<>(anyObjectUR, propByRes, "update");
    }

    @Override
    protected void doDelete(final AnyObject anyObject) {
        anyObjectDAO.delete(anyObject);
    }
}
