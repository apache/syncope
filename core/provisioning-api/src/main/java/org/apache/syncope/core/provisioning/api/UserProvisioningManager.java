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
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;

public interface UserProvisioningManager extends ProvisioningManager<UserTO, UserPatch> {

    Pair<String, List<PropagationStatus>> activate(StatusPatch statusPatch, boolean nullPriorityAsync);

    Pair<String, List<PropagationStatus>> reactivate(StatusPatch statusPatch, boolean nullPriorityAsync);

    Pair<String, List<PropagationStatus>> suspend(StatusPatch statusPatch, boolean nullPriorityAsync);

    void internalSuspend(String key);

    Pair<String, List<PropagationStatus>> create(UserTO userTO, boolean storePassword, boolean nullPriorityAsync);

    Pair<String, List<PropagationStatus>> create(
            UserTO userTO,
            boolean storePassword,
            boolean disablePwdPolicyCheck,
            Boolean enabled,
            Set<String> excludedResources,
            boolean nullPriorityAsync);

    Pair<UserPatch, List<PropagationStatus>> update(
            UserPatch userPatch,
            ProvisioningReport result,
            Boolean enabled,
            Set<String> excludedResources,
            boolean nullPriorityAsync);

    void requestPasswordReset(String key);

    void confirmPasswordReset(String key, String token, String password);

    List<PropagationStatus> provision(
            String key, boolean changePwd, String password, Collection<String> resources, boolean nullPriorityAsync);

}
