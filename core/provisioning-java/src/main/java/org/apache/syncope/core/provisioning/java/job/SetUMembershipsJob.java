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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
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

    public static final String MEMBERSHIPS_BEFORE_KEY = "membershipsBefore";

    public static final String MEMBERSHIPS_AFTER_KEY = "membershipsAfter";

    public static final String CONTEXT = "context";

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private UserProvisioningManager userProvisioningManager;

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        String executor = Optional.ofNullable(context.getMergedJobDataMap().getString(JobManager.EXECUTOR_KEY)).
                orElse(securityProperties.getAdminUser());

        try {
            AuthContextUtils.callAsAdmin(context.getMergedJobDataMap().getString(JobManager.DOMAIN_KEY), () -> {

                @SuppressWarnings("unchecked")
                Map<String, Set<String>> membershipsBefore =
                        (Map<String, Set<String>>) context.getMergedJobDataMap().get(MEMBERSHIPS_BEFORE_KEY);
                LOG.debug("Memberships before pull (User -> Groups) {}", membershipsBefore);

                @SuppressWarnings("unchecked")
                Map<String, Set<String>> membershipsAfter =
                        (Map<String, Set<String>>) context.getMergedJobDataMap().get(MEMBERSHIPS_AFTER_KEY);
                LOG.debug("Memberships after pull (User -> Groups) {}", membershipsAfter);

                List<UserUR> updateReqs = new ArrayList<>();

                membershipsAfter.forEach((user, groups) -> {
                    UserUR userUR = new UserUR();
                    userUR.setKey(user);
                    updateReqs.add(userUR);

                    groups.forEach(group -> {
                        Set<String> before = membershipsBefore.get(user);
                        if (before == null || !before.contains(group)) {
                            userUR.getMemberships().add(new MembershipUR.Builder(group).
                                    operation(PatchOperation.ADD_REPLACE).
                                    build());
                        }
                    });
                });

                membershipsBefore.forEach((user, groups) -> {
                    UserUR userUR = updateReqs.stream().
                            filter(req -> user.equals(req.getKey())).findFirst().
                            orElseGet(() -> {
                                UserUR req = new UserUR.Builder(user).build();
                                updateReqs.add(req);
                                return req;
                            });

                    groups.forEach(group -> {
                        Set<String> after = membershipsAfter.get(user);
                        if (after == null || !after.contains(group)) {
                            userUR.getMemberships().add(new MembershipUR.Builder(group).
                                    operation(PatchOperation.DELETE).
                                    build());
                        }
                    });
                });

                updateReqs.stream().filter(req -> !req.isEmpty()).forEach(req -> {
                    LOG.debug("About to update User {}", req);
                    userProvisioningManager.update(
                            req, true, executor, context.getMergedJobDataMap().getString(CONTEXT));
                });

                return null;
            });
        } catch (RuntimeException e) {
            LOG.error("While setting memberships", e);
            throw new JobExecutionException("While executing memberships", e);
        } finally {
            ApplicationContextProvider.getBeanFactory().destroySingleton(context.getJobDetail().getKey().getName());
        }
    }
}
