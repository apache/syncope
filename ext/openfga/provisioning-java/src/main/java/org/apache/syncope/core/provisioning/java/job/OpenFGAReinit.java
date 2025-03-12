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
package org.apache.syncope.core.provisioning.java.job;

import java.util.List;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.openfga.client.OpenFGAClient;
import org.apache.syncope.ext.openfga.client.OpenFGAClientFactory;
import org.apache.syncope.ext.openfga.client.OpenFGAStoreManager;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Replace the authorization model and creates new tuples with information from existing
 * any types, relationship types, users, groups and any objects.
 */
public class OpenFGAReinit extends AbstractSchedTaskJobDelegate<SchedTask> {

    @Autowired
    protected OpenFGAStoreManager storeManager;

    @Autowired
    protected OpenFGAClientFactory clientFactory;

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Override
    protected String doExecute(final JobExecutionContext context) throws JobExecutionException {
        if (!context.isDryRun()) {
            setStatus("Start rebuilding OpenFGA authorization model and tuples");

            try {
                OpenFGAClient client = clientFactory.get(AuthContextUtils.getDomain());

                // replace the current authorization model
                clientFactory.initAuthorizationModel(client, AuthContextUtils.getDomain(), true);

                List<? extends AnyType> anyTypes = anyTypeDAO.findAll().stream().
                        filter(anyType -> anyType.getKind() == AnyTypeKind.ANY_OBJECT).toList();
                for (AnyType anyType : anyTypes) {
                    storeManager.handle(client, SyncDeltaType.CREATE, anyType);
                }

                List<? extends RelationshipType> relationshipTypes = relationshipTypeDAO.findAll();
                for (RelationshipType relationshipType : relationshipTypes) {
                    storeManager.handle(client, SyncDeltaType.CREATE, relationshipType);
                }

                // replace tuples
                long users = userDAO.count();
                for (int page = 0; page <= (users / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                    Pageable pageable = PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT);
                    for (User user : userDAO.findAll(pageable)) {
                        storeManager.handle(client, SyncDeltaType.CREATE, user);
                    }
                }

                long groups = groupDAO.count();
                for (int page = 0; page <= (groups / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                    Pageable pageable = PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT);
                    for (Group group : groupDAO.findAll(pageable)) {
                        storeManager.handle(client, SyncDeltaType.CREATE, group);
                    }
                }

                long anyObjects = anyObjectDAO.count();
                for (int page = 0; page <= (anyObjects / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                    Pageable pageable = PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT);
                    for (AnyObject anyObject : anyObjectDAO.findAll(pageable)) {
                        storeManager.handle(client, SyncDeltaType.CREATE, anyObject);
                    }
                }

                setStatus("Rebuild for domain " + AuthContextUtils.getDomain() + " successfully completed");

                return "Authorization model replaced with USER, GROUP, " + anyTypes.size() + " AnyTypes, Memberships "
                        + "and " + relationshipTypes.size() + " RelationshipTypes\n"
                        + "Tuples:\n"
                        + " * " + users + " Users\n"
                        + " * " + groups + " Groups\n"
                        + " * " + anyObjects + " AnyObjects";
            } catch (Exception e) {
                throw new JobExecutionException("While rebuilding for domain " + AuthContextUtils.getDomain(), e);
            }
        }

        return "SUCCESS";
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec<?> execution) {
        return true;
    }
}
