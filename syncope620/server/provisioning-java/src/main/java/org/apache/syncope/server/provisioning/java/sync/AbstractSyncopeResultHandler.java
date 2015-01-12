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
package org.apache.syncope.server.provisioning.java.sync;

import org.apache.syncope.server.persistence.api.dao.RoleDAO;
import org.apache.syncope.server.persistence.api.dao.UserDAO;
import org.apache.syncope.server.persistence.api.entity.AttributableUtilFactory;
import org.apache.syncope.server.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.server.provisioning.api.RoleProvisioningManager;
import org.apache.syncope.server.provisioning.api.data.RoleDataBinder;
import org.apache.syncope.server.provisioning.api.UserProvisioningManager;
import org.apache.syncope.server.provisioning.api.data.UserDataBinder;
import org.apache.syncope.server.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.server.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.server.provisioning.api.sync.ProvisioningActions;
import org.apache.syncope.server.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.server.provisioning.api.sync.SyncopeResultHandler;
import org.apache.syncope.server.misc.AuditManager;
import org.apache.syncope.server.provisioning.java.notification.NotificationManager;
import org.apache.syncope.server.misc.ConnObjectUtil;
import org.apache.syncope.server.workflow.api.RoleWorkflowAdapter;
import org.apache.syncope.server.workflow.api.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractSyncopeResultHandler<T extends ProvisioningTask, A extends ProvisioningActions>
        implements SyncopeResultHandler<T, A> {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSyncopeResultHandler.class);

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected RoleDAO roleDAO;

    /**
     * ConnectorObject util.
     */
    @Autowired
    protected ConnObjectUtil connObjectUtil;

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

    /**
     * User workflow adapter.
     */
    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    /**
     * Role workflow adapter.
     */
    @Autowired
    protected RoleWorkflowAdapter rwfAdapter;

    @Autowired
    protected UserDataBinder userTransfer;

    @Autowired
    protected RoleDataBinder roleTransfer;

    @Autowired
    protected UserProvisioningManager userProvisioningManager;

    @Autowired
    protected RoleProvisioningManager roleProvisioningManager;

    @Autowired
    protected AttributableUtilFactory attrUtilFactory;

    /**
     * Sync profile.
     */
    protected ProvisioningProfile<T, A> profile;

    public void setProfile(final ProvisioningProfile<T, A> profile) {
        this.profile = profile;
    }

    public ProvisioningProfile<T, A> getProfile() {
        return profile;
    }
}
