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

import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;

public interface UserProvisioningManager extends ProvisioningManager<UserTO, UserMod> {

    Pair<Long, List<PropagationStatus>> activate(User user, StatusMod statusMod);

    Pair<Long, List<PropagationStatus>> reactivate(User user, StatusMod statusMod);

    Pair<Long, List<PropagationStatus>> suspend(User user, StatusMod statusMod);

    void innerSuspend(User user, boolean propagate);

    Pair<Long, List<PropagationStatus>> create(UserTO userTO, boolean storePassword);

    Pair<Long, List<PropagationStatus>> create(UserTO userTO, boolean storePassword,
            boolean disablePwdPolicyCheck, Boolean enabled, Set<String> excludedResources);

    Pair<Long, List<PropagationStatus>> update(UserMod userMod, boolean removeMemberships);

    Pair<Long, List<PropagationStatus>> update(UserMod userMod, Long key,
            ProvisioningResult result, Boolean enabled, Set<String> excludedResources);

    List<PropagationStatus> delete(Long subjectKey, Set<String> excludedResources);

    void requestPasswordReset(Long key);

    void confirmPasswordReset(User user, String token, String password);

}
