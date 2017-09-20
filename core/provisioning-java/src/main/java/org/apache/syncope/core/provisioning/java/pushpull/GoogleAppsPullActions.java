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
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
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
 * <li>the id provided by Google in response to create, which will need to be used for all subsequent operations</li>
 * <li>the e-mail address</li>
 * </ol>
 */
public class GoogleAppsPullActions implements PullActions {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleAppsPullActions.class);

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    private final Map<String, String> googleAppsIds = new HashMap<>();

    protected String getEmailSchema() {
        return "email";
    }

    protected String getGoogleAppsIdSchema() {
        return "GoogleAppsId";
    }

    @Override
    public SyncDelta beforeProvision(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entity) throws JobExecutionException {

        if (!(entity instanceof UserTO)) {
            return delta;
        }

        UserTO userTO = (UserTO) entity;
        if (userTO.getUsername() == null) {
            userTO.setUsername(delta.getObject().getName().getNameValue());
        }

        return delta;
    }

    @Override
    public <P extends AnyPatch> SyncDelta beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entity,
            final P anyPatch) throws JobExecutionException {

        if (!(anyPatch instanceof UserPatch)) {
            return delta;
        }

        UserPatch userPatch = (UserPatch) anyPatch;
        if (userPatch.getUsername() == null) {
            userPatch.setUsername(new StringReplacePatchItem.Builder().
                    value(delta.getObject().getName().getNameValue()).build());
        }

        return delta;
    }

    @Transactional
    @Override
    public void after(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entity,
            final ProvisioningReport result) throws JobExecutionException {

        if (!(entity instanceof UserTO)) {
            return;
        }

        googleAppsIds.put(entity.getKey(), delta.getUid().getUidValue());
    }

    @Transactional
    @Override
    public void afterAll(final ProvisioningProfile<?, ?> profile) throws JobExecutionException {
        googleAppsIds.entrySet().forEach((entry) -> {
            User user = userDAO.find(entry.getKey());
            if (user == null) {
                LOG.error("Could not find user {}, skipping", entry.getKey());
            } else {
                AnyUtils anyUtils = anyUtilsFactory.getInstance(user);

                // 1. stores the __UID__ received by Google
                PlainSchema googleAppsId = plainSchemaDAO.find(getGoogleAppsIdSchema());
                if (googleAppsId == null) {
                    LOG.error("Could not find schema googleAppsId, skipping");
                } else {
                    UPlainAttr attr = user.getPlainAttr(getGoogleAppsIdSchema()).orElse(null);
                    if (attr == null) {
                        attr = entityFactory.newEntity(UPlainAttr.class);
                        attr.setSchema(googleAppsId);
                        attr.setOwner(user);
                        user.add(attr);

                        try {
                            attr.add(entry.getValue(), anyUtils);
                            userDAO.save(user);
                        } catch (InvalidPlainAttrValueException e) {
                            LOG.error("Invalid value for attribute {}: {}",
                                    googleAppsId.getKey(), entry.getValue(), e);
                        }
                    } else {
                        LOG.debug("User {} has already a googleAppsId assigned: {}", user, attr.getValuesAsStrings());
                    }
                }
            }
        });
    }

}
