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
package org.apache.syncope.core.provisioning.api;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningReport;

public interface UserProvisioningManager extends ProvisioningManager<UserCR, UserUR> {

    Pair<String, List<PropagationStatus>> create(
            UserCR userCR,
            boolean disablePwdPolicyCheck,
            Boolean enabled,
            Set<String> excludedResources,
            boolean nullPriorityAsync,
            String creator,
            String context);

    Pair<UserUR, List<PropagationStatus>> update(
            UserUR userUR,
            boolean nullPriorityAsync,
            String updater,
            String context);

    Pair<UserUR, List<PropagationStatus>> update(
            UserUR userUR,
            ProvisioningReport result,
            Boolean enabled,
            Set<String> excludedResources,
            boolean nullPriorityAsync,
            String updater,
            String context);

    Pair<String, List<PropagationStatus>> activate(
            StatusR statusR, boolean nullPriorityAsync, String updater, String context);

    Pair<String, List<PropagationStatus>> reactivate(
            StatusR statusR, boolean nullPriorityAsync, String updater, String context);

    Pair<String, List<PropagationStatus>> suspend(
            StatusR statusR, boolean nullPriorityAsync, String updater, String context);

    void internalSuspend(String key, String updater, String context);

    void requestPasswordReset(String key, String updater, String context);

    void confirmPasswordReset(String key, String token, String password, String updater, String context);

    List<PropagationStatus> provision(
            String key,
            boolean changePwd,
            String password,
            Collection<String> resources,
            boolean nullPriorityAsync,
            String executor);
}
