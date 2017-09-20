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

import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
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
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is required during setup of an External Resource based on the ConnId
 * <a href="https://github.com/Tirasa/ConnIdGoogleAppsBundle">GoogleApps connector</a>.
 *
 * It manages:
 * <ol>
 * <li>the id provided by Google, which will need to be used for all subsequent operations</li>
 * <li>the e-mail address</li>
 * </ol>
 */
public class GoogleAppsPropagationActions implements PropagationActions {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleAppsPropagationActions.class);

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    protected PlainAttrValueDAO plainAttrValueDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    protected String getEmailSchema() {
        return "email";
    }

    protected String getGoogleAppsIdSchema() {
        return "GoogleAppsId";
    }

    @Transactional
    @Override
    public void before(final PropagationTask task, final ConnectorObject beforeObj) {
        if (task.getOperation() == ResourceOperation.DELETE || task.getOperation() == ResourceOperation.NONE) {
            return;
        }
        if (AnyTypeKind.USER != task.getAnyTypeKind()) {
            return;
        }

        Set<Attribute> attrs = new HashSet<>(task.getAttributes());

        // ensure to set __NAME__ value to user's email (e.g. primary e-mail address)
        User user = userDAO.find(task.getEntityKey());
        if (user == null) {
            LOG.error("Could not find user {}, skipping", task.getEntityKey());
        } else {
            Name name = AttributeUtil.getNameFromAttributes(attrs);
            if (name != null) {
                attrs.remove(name);
            }
            attrs.add(new Name(user.getPlainAttr(getEmailSchema()).get().getValuesAsStrings().get(0)));
        }

        task.setAttributes(attrs);
    }

    @Transactional
    @Override
    public void after(final PropagationTask task, final TaskExec execution, final ConnectorObject afterObj) {
        if (task.getOperation() == ResourceOperation.DELETE || task.getOperation() == ResourceOperation.NONE) {
            return;
        }
        if (AnyTypeKind.USER != task.getAnyTypeKind()) {
            return;
        }

        User user = userDAO.find(task.getEntityKey());
        if (user == null) {
            LOG.error("Could not find user {}, skipping", task.getEntityKey());
        } else {
            boolean modified = false;
            AnyUtils anyUtils = anyUtilsFactory.getInstance(user);

            PlainSchema googleAppsId = plainSchemaDAO.find(getGoogleAppsIdSchema());
            if (googleAppsId == null) {
                LOG.error("Could not find schema {}, skipping", getGoogleAppsIdSchema());
            } else {
                // set back the __UID__ received by Google
                UPlainAttr attr = user.getPlainAttr(getGoogleAppsIdSchema()).orElse(null);
                if (attr == null) {
                    attr = entityFactory.newEntity(UPlainAttr.class);
                    attr.setSchema(googleAppsId);
                    attr.setOwner(user);
                    user.add(attr);

                    try {
                        attr.add(afterObj.getUid().getUidValue(), anyUtils);
                        modified = true;
                    } catch (InvalidPlainAttrValueException e) {
                        LOG.error("Invalid value for attribute {}: {}",
                                googleAppsId.getKey(), afterObj.getUid().getUidValue(), e);
                    }
                } else {
                    LOG.debug("User {} has already {} assigned: {}",
                            user, googleAppsId.getKey(), attr.getValuesAsStrings());
                }
            }

            if (modified) {
                userDAO.save(user);
            }
        }
    }

}
