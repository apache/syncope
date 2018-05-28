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

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is required during setup of an External Resource based on the ConnId
 * <a href="https://github.com/Tirasa/ConnIdAzureBundle">Azure connector</a>.
 *
 * It manages:
 * <ol>
 * <li>the id provided by Azure in response to create, which will need to be used for all subsequent operations</li>
 * <li>the e-mail address</li>
 * </ol>
 */
public class AzurePullActions extends DefaultPullActions {

    private static final Logger LOG = LoggerFactory.getLogger(AzurePullActions.class);

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    private final Map<String, String> azureIds = new HashMap<>();

    private AnyTypeKind entityType;

    protected String getEmailAttrName() {
        return "mailNickname";
    }

    protected String getAzureUserIdSchema() {
        return "AzureUserId";
    }

    protected String getAzureGroupIdSchema() {
        return "AzureGroupId";
    }

    @Override
    public void beforeProvision(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entity) throws JobExecutionException {

        if (entity instanceof UserTO) {
            UserTO userTO = (UserTO) entity;
            if (userTO.getUsername() == null) {
                userTO.setUsername(delta.getObject().getName().getNameValue());
            }
        } else if (entity instanceof GroupTO) {
            GroupTO groupTO = (GroupTO) entity;
            if (groupTO.getName() == null) {
                groupTO.setName(delta.getObject().getName().getNameValue());
            }
        }
    }

    @Override
    public <P extends AnyPatch> void beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entity,
            final P anyPatch) throws JobExecutionException {

        if (anyPatch instanceof UserPatch) {
            UserPatch userPatch = (UserPatch) anyPatch;
            if (userPatch.getUsername() == null) {
                userPatch.setUsername(new StringReplacePatchItem.Builder().
                        value(delta.getObject().getName().getNameValue()).build());
            }
        } else if (entity instanceof GroupPatch) {
            GroupPatch groupPatch = (GroupPatch) entity;
            if (groupPatch.getName() == null) {
                groupPatch.setName(new StringReplacePatchItem.Builder().
                        value(delta.getObject().getName().getNameValue()).build());
            }
        }
    }

    @Transactional
    @Override
    public void after(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entity,
            final ProvisioningReport result) throws JobExecutionException {

        if (!(entity instanceof UserTO) && !(entity instanceof GroupTO)) {
            return;
        }

        if (entity instanceof UserTO) {
            entityType = AnyTypeKind.USER;
        } else if (entity instanceof GroupTO) {
            entityType = AnyTypeKind.GROUP;
        }

        azureIds.put(entity.getKey(), delta.getUid().getUidValue());
    }

    @Transactional
    @Override
    public void afterAll(final ProvisioningProfile<?, ?> profile) throws JobExecutionException {
        for (Map.Entry<String, String> entry : azureIds.entrySet()) {

            if (AnyTypeKind.USER.equals(entityType)) {
                User user = userDAO.find(entry.getKey());
                if (user == null) {
                    LOG.error("Could not find user {}, skipping", entry.getKey());
                } else {
                    AnyUtils anyUtils = anyUtilsFactory.getInstance(user);

                    // 1. stores the __UID__ received by Azure
                    PlainSchema azureId = plainSchemaDAO.find(getAzureUserIdSchema());
                    if (azureId == null) {
                        LOG.error("Could not find schema {}, skipping", getAzureUserIdSchema());
                    } else {
                        UPlainAttr attr = user.getPlainAttr(getAzureUserIdSchema());
                        if (attr == null) {
                            attr = entityFactory.newEntity(UPlainAttr.class);
                            attr.setSchema(azureId);
                            attr.setOwner(user);
                            user.add(attr);

                            try {
                                attr.add(entry.getValue(), anyUtils);
                                userDAO.save(user);
                            } catch (InvalidPlainAttrValueException e) {
                                LOG.error("Invalid value for attribute {}: {}",
                                        azureId.getKey(), entry.getValue(), e);
                            }
                        } else {
                            LOG.debug("User {} has already a {} assigned: {}", user, getAzureUserIdSchema(),
                                    attr.getValuesAsStrings());
                        }
                    }
                }
            } else if (AnyTypeKind.GROUP.equals(entityType)) {
                Group group = groupDAO.find(entry.getKey());
                if (group == null) {
                    LOG.error("Could not find group {}, skipping", entry.getKey());
                } else {
                    AnyUtils anyUtils = anyUtilsFactory.getInstance(group);

                    // 1. stores the __UID__ received by Azure
                    PlainSchema azureId = plainSchemaDAO.find(getAzureGroupIdSchema());
                    if (azureId == null) {
                        LOG.error("Could not find schema {}, skipping", getAzureGroupIdSchema());
                    } else {
                        GPlainAttr attr = group.getPlainAttr(getAzureGroupIdSchema());
                        if (attr == null) {
                            attr = entityFactory.newEntity(GPlainAttr.class);
                            attr.setSchema(azureId);
                            attr.setOwner(group);
                            group.add(attr);

                            try {
                                attr.add(entry.getValue(), anyUtils);
                                groupDAO.save(group);
                            } catch (InvalidPlainAttrValueException e) {
                                LOG.error("Invalid value for attribute {}: {}",
                                        azureId.getKey(), entry.getValue(), e);
                            }
                        } else {
                            LOG.debug("Group {} has already a {} assigned: {}", group, getAzureGroupIdSchema(),
                                    attr.getValuesAsStrings());
                        }
                    }
                }
            }

        }
    }

}
