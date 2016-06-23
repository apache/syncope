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

import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Quartz Job used for setting user memberships asynchronously, after the completion of
 * {@link org.apache.syncope.core.provisioning.api.pushpull.PullActions}.
 */
public class SetUMembershipsJob extends AbstractInterruptableJob {

    private static final Logger LOG = LoggerFactory.getLogger(SetUMembershipsJob.class);

    public static final String MEMBERSHIPS_KEY = "memberships";

    @Autowired
    private UserProvisioningManager userProvisioningManager;

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        super.execute(context);

        try {
            AuthContextUtils.execWithAuthContext(context.getMergedJobDataMap().getString(JobManager.DOMAIN_KEY),
                    new AuthContextUtils.Executable<Void>() {

                @Override
                public Void exec() {
                    @SuppressWarnings("unchecked")
                    Map<String, Set<String>> memberships =
                            (Map<String, Set<String>>) context.getMergedJobDataMap().get(MEMBERSHIPS_KEY);

                    LOG.debug("About to set memberships (User -> Groups) {}", memberships);

                    for (Map.Entry<String, Set<String>> membership : memberships.entrySet()) {
                        UserPatch userPatch = new UserPatch();
                        userPatch.setKey(membership.getKey());

                        for (String groupKey : membership.getValue()) {
                            userPatch.getMemberships().add(
                                    new MembershipPatch.Builder().
                                    operation(PatchOperation.ADD_REPLACE).
                                    group(groupKey).
                                    build());
                        }

                        if (!userPatch.isEmpty()) {
                            LOG.debug("About to update User {}", userPatch.getKey());
                            userProvisioningManager.update(userPatch, true);
                        }
                    }

                    return null;
                }
            });
        } catch (RuntimeException e) {
            LOG.error("While setting memberships", e);
            throw new JobExecutionException("While executing memberships", e);
        }
    }

}
