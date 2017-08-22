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
package org.apache.syncope.core.migration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.java.job.SetUMembershipsJob;
import org.apache.syncope.core.provisioning.java.pushpull.SchedulingPullActions;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class MigrationPullActions extends SchedulingPullActions {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationPullActions.class);

    private static final String CIPHER_ALGORITHM_ATTR = "cipherAlgorithm";

    private static final String RESOURCES_ATTR = "__RESOURCES__";

    private static final String MEMBERSHIPS_ATTR = "__MEMBERSHIPS__";

    @Autowired
    private UserDAO userDAO;

    private final Map<String, Set<String>> memberships = new HashMap<>();

    @Override
    public SyncDelta beforeProvision(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entity) throws JobExecutionException {

        // handles resource assignment, for users and groups
        Attribute resourcesAttr = delta.getObject().getAttributeByName(RESOURCES_ATTR);
        if (entity instanceof AnyTO && resourcesAttr != null
                && resourcesAttr.getValue() != null && !resourcesAttr.getValue().isEmpty()) {

            ((AnyTO) entity).getResources().addAll(
                    resourcesAttr.getValue().stream().map(Object::toString).collect(Collectors.toList()));
        }

        return delta;
    }

    @Transactional
    @Override
    public void after(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final EntityTO entity,
            final ProvisioningReport result)
            throws JobExecutionException {

        if (entity instanceof UserTO) {
            // handles ciphered password import
            CipherAlgorithm cipherAlgorithm = null;
            Attribute cipherAlgorithmAttr = delta.getObject().getAttributeByName(CIPHER_ALGORITHM_ATTR);
            if (cipherAlgorithmAttr != null
                    && cipherAlgorithmAttr.getValue() != null && !cipherAlgorithmAttr.getValue().isEmpty()) {

                cipherAlgorithm = CipherAlgorithm.valueOf(cipherAlgorithmAttr.getValue().get(0).toString());
            }

            GuardedString passwordValue = AttributeUtil.getPasswordValue(delta.getObject().getAttributes());

            if (cipherAlgorithm != null && passwordValue != null) {
                User user = userDAO.find(entity.getKey());
                LOG.debug("Setting encoded password for {}", user);
                user.setEncodedPassword(SecurityUtil.decrypt(passwordValue), cipherAlgorithm);
            }
        } else if (entity instanceof GroupTO) {
            // handles group membership
            Attribute membershipsAttr = delta.getObject().getAttributeByName(MEMBERSHIPS_ATTR);
            if (membershipsAttr != null
                    && membershipsAttr.getValue() != null && !membershipsAttr.getValue().isEmpty()) {

                LOG.debug("Found {} for group {}", MEMBERSHIPS_ATTR, entity.getKey());

                for (Object membership : membershipsAttr.getValue()) {
                    User member = userDAO.findByUsername(membership.toString());
                    if (member == null) {
                        LOG.warn("Could not find member {} for group {}", membership, entity.getKey());
                    } else {
                        Set<String> memb = memberships.get(member.getKey());
                        if (memb == null) {
                            memb = new HashSet<>();
                            memberships.put(member.getKey(), memb);
                        }
                        memb.add(entity.getKey());
                    }
                }
            }
        } else {
            super.after(profile, delta, entity, result);
        }
    }

    @Override
    public void afterAll(final ProvisioningProfile<?, ?> profile) throws JobExecutionException {
        Map<String, Object> jobMap = new HashMap<>();
        jobMap.put(SetUMembershipsJob.MEMBERSHIPS_KEY, memberships);
        schedule(SetUMembershipsJob.class, jobMap);
    }

}
