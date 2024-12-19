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

import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeResultHandler;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractRealmResultHandler<T extends ProvisioningTask<?>, A extends ProvisioningActions>
        implements SyncopeResultHandler<T, A> {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeResultHandler.class);

    @Autowired
    protected RealmDAO realmDAO;

    @Autowired
    protected RealmSearchDAO realmSearchDAO;

    @Autowired
    protected RealmDataBinder binder;

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

    @Autowired
    protected SecurityProperties securityProperties;

    /**
     * Provisioning profile.
     */
    protected ProvisioningProfile<T, A> profile;

    protected volatile boolean stopRequested = false;

    @Override
    public void setProfile(final ProvisioningProfile<T, A> profile) {
        this.profile = profile;
    }

    @Override
    public void stop() {
        stopRequested = true;
    }
}
