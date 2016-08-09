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
package org.apache.syncope.core.provisioning.java.pushpull;

import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeResultHandler;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningActions;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractSyncopeResultHandler<T extends ProvisioningTask, A extends ProvisioningActions>
        implements SyncopeResultHandler<T, A> {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeResultHandler.class);

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    /**
     * ConnectorObject utils.
     */
    @Autowired
    protected ConnObjectUtils connObjectUtils;

    /**
     * Notification Manager.
     */
    @Autowired
    protected NotificationManager notificationManager;

    /**
     * Audit Manager.
     */
    @Autowired
    protected AuditManager auditManager;

    /**
     * Propagation manager.
     */
    @Autowired
    protected PropagationManager propagationManager;

    /**
     * Task executor.
     */
    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    protected AnyObjectWorkflowAdapter awfAdapter;

    /**
     * User workflow adapter.
     */
    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    /**
     * Group workflow adapter.
     */
    @Autowired
    protected GroupWorkflowAdapter gwfAdapter;

    @Autowired
    protected AnyObjectDataBinder anyObjectDataBinder;

    @Autowired
    protected UserDataBinder userDataBinder;

    @Autowired
    protected GroupDataBinder groupDataBinder;

    @Autowired
    protected AnyObjectProvisioningManager anyObjectProvisioningManager;

    @Autowired
    protected UserProvisioningManager userProvisioningManager;

    @Autowired
    protected GroupProvisioningManager groupProvisioningManager;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    /**
     * Provisioning profile.
     */
    protected ProvisioningProfile<T, A> profile;

    protected abstract AnyUtils getAnyUtils();

    protected abstract AnyTO getAnyTO(String key);

    protected abstract Any<?> getAny(String key);

    protected abstract AnyPatch newPatch(String key);

    protected abstract WorkflowResult<String> update(AnyPatch patch);

    @Override
    public void setProfile(final ProvisioningProfile<T, A> profile) {
        this.profile = profile;
    }

    @Override
    public ProvisioningProfile<T, A> getProfile() {
        return profile;
    }
}
