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

import java.util.Iterator;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ApplicationTO;
import org.apache.syncope.common.lib.to.PrivilegeTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.entity.Application;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.provisioning.api.data.ApplicationDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationDataBinderImpl implements ApplicationDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(ApplicationDataBinder.class);

    protected final ApplicationDAO applicationDAO;

    protected final EntityFactory entityFactory;

    public ApplicationDataBinderImpl(final ApplicationDAO applicationDAO, final EntityFactory entityFactory) {
        this.applicationDAO = applicationDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    public Application create(final ApplicationTO applicationTO) {
        return update(entityFactory.newEntity(Application.class), applicationTO);
    }

    @Override
    public Application update(final Application toBeUpdated, final ApplicationTO applicationTO) {
        toBeUpdated.setKey(applicationTO.getKey());
        Application application = applicationDAO.save(toBeUpdated);

        application.setDescription(applicationTO.getDescription());

        // 1. add or update all (valid) privileges from TO
        applicationTO.getPrivileges().forEach(privilegeTO -> {
            if (privilegeTO == null) {
                LOG.error("Null {}", PrivilegeTO.class.getSimpleName());
            } else {
                Privilege privilege = applicationDAO.findPrivilege(privilegeTO.getKey());
                if (privilege == null) {
                    privilege = entityFactory.newEntity(Privilege.class);
                    privilege.setKey(privilegeTO.getKey());
                    privilege.setApplication(application);

                    application.add(privilege);
                } else if (!application.equals(privilege.getApplication())) {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPrivilege);
                    sce.getElements().add(
                            "Privilege " + privilege.getKey() + " already owned by " + privilege.getApplication());
                    throw sce;
                }

                privilege.setDescription(privilegeTO.getDescription());
                privilege.setSpec(privilegeTO.getSpec());
            }
        });

        // 2. remove all privileges not contained in the TO
        for (Iterator<? extends Privilege> itor = application.getPrivileges().iterator(); itor.hasNext();) {
            Privilege privilege = itor.next();
            if (!applicationTO.getPrivileges().stream().
                    anyMatch(privilegeTO -> privilege.getKey().equals(privilegeTO.getKey()))) {

                privilege.setApplication(null);
                itor.remove();
            }
        }

        return application;
    }

    @Override
    public PrivilegeTO getPrivilegeTO(final Privilege privilege) {
        PrivilegeTO privilegeTO = new PrivilegeTO();
        privilegeTO.setKey(privilege.getKey());
        privilegeTO.setDescription(privilege.getDescription());
        privilegeTO.setApplication(privilege.getApplication().getKey());
        privilegeTO.setSpec(privilege.getSpec());
        return privilegeTO;
    }

    @Override
    public ApplicationTO getApplicationTO(final Application application) {
        ApplicationTO applicationTO = new ApplicationTO();

        applicationTO.setKey(application.getKey());
        applicationTO.setDescription(application.getDescription());
        applicationTO.getPrivileges().addAll(
                application.getPrivileges().stream().map(this::getPrivilegeTO).collect(Collectors.toList()));

        return applicationTO;
    }
}
