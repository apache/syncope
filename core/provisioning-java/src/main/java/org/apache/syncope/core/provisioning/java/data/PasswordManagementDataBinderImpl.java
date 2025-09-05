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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.provisioning.api.data.PasswordManagementDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordManagementDataBinderImpl implements PasswordManagementDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(PasswordManagementDataBinder.class);

    protected final EntityFactory entityFactory;

    public PasswordManagementDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    @Override
    public PasswordManagement create(final PasswordManagementTO passwordManagementTO) {
        PasswordManagement passwordManagement = entityFactory.newEntity(PasswordManagement.class);
        passwordManagement.setKey(passwordManagementTO.getKey());
        return update(passwordManagement, passwordManagementTO);
    }

    @Override
    public PasswordManagement update(final PasswordManagement passwordManagement,
            final PasswordManagementTO passwordManagementTO) {
        passwordManagement.setDescription(passwordManagementTO.getDescription());
        passwordManagement.setEnabled(passwordManagementTO.isEnabled());
        passwordManagement.setConf(passwordManagementTO.getConf());

        return passwordManagement;
    }

    @Override
    public PasswordManagementTO getPasswordManagementTO(final PasswordManagement passwordManagement) {
        PasswordManagementTO passwordManagementTO = new PasswordManagementTO();

        passwordManagementTO.setKey(passwordManagement.getKey());
        passwordManagementTO.setDescription(passwordManagement.getDescription());
        passwordManagementTO.setEnabled(passwordManagement.isEnabled());
        passwordManagementTO.setConf(passwordManagement.getConf());

        return passwordManagementTO;
    }
}
