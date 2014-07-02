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
package org.apache.syncope.core.sync.impl;

import org.apache.syncope.core.sync.SyncProfile;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.AbstractSyncTask;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.sync.AbstractSyncActions;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractSyncopeResultHandler<T extends AbstractSyncTask, A extends AbstractSyncActions<?>> {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSyncopeResultHandler.class);

    /**
     * User data binder.
     */
    @Autowired
    protected UserDataBinder userDataBinder;

    /**
     * Role data binder.
     */
    @Autowired
    protected RoleDataBinder roleDataBinder;

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
     * task executor.
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

    /**
     * Sync profile.
     */
    protected SyncProfile<T, A> profile;

    public void setProfile(final SyncProfile<T, A> profile) {
        this.profile = profile;
    }

    public SyncProfile<T, A> getProfile() {
        return profile;
    }
}
