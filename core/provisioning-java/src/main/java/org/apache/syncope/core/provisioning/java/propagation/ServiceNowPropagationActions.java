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
package org.apache.syncope.core.provisioning.java.propagation;

import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is required during setup of an External Resource based on the ConnId
 * <a href="https://github.com/Tirasa/ConnIdServiceNowBundle">ServiceNow connector</a>.
 *
 * It manages:
 * <ol>
 * <li>the User id provided by ServiceNow, which will need to be used for all subsequent operations</li>
 * </ol>
 */
public class ServiceNowPropagationActions extends DefaultPropagationActions {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceNowPropagationActions.class);

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    protected String getServiceNowIdSchema() {
        return "ServiceNowUserId";
    }

    @Transactional
    @Override
    public void after(final PropagationTask task, final TaskExec execution, final ConnectorObject afterObj) {
        if (task.getOperation() == ResourceOperation.DELETE || task.getOperation() == ResourceOperation.NONE) {
            return;
        }

        if (AnyTypeKind.USER.equals(task.getAnyTypeKind())) {

            User user = userDAO.find(task.getEntityKey());
            if (user == null) {
                LOG.error("Could not find user {}, skipping", task.getEntityKey());
            } else {
                boolean modified = false;
                AnyUtils anyUtils = anyUtilsFactory.getInstance(user);

                // ServiceNow v1.1 User ID
                PlainSchema userId = plainSchemaDAO.find(getServiceNowIdSchema());
                if (userId == null) {
                    LOG.error("Could not find schema {}, skipping", getServiceNowIdSchema());
                } else {
                    // set back the __UID__ received by ServiceNow service
                    UPlainAttr attr = user.getPlainAttr(getServiceNowIdSchema());
                    if (attr == null) {
                        attr = entityFactory.newEntity(UPlainAttr.class);
                        attr.setSchema(userId);
                        attr.setOwner(user);
                        user.add(attr);

                        try {
                            attr.add(afterObj.getUid().getUidValue(), anyUtils);
                            modified = true;
                        } catch (InvalidPlainAttrValueException e) {
                            LOG.error("Invalid value for attribute {}: {}",
                                    userId.getKey(), afterObj.getUid().getUidValue(), e);
                        }
                    } else {
                        LOG.debug("User {} has already {} assigned: {}",
                                user, userId.getKey(), attr.getValuesAsStrings());
                    }
                }

                if (modified) {
                    userDAO.save(user);
                }
            }
        }
    }

}
