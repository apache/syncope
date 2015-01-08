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
package org.apache.syncope.core.provisioning;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.sync.SyncResult;

public interface UserProvisioningManager extends ProvisioningManager<UserTO, UserMod> {

    Map.Entry<Long, List<PropagationStatus>> activate(SyncopeUser user, StatusMod statusMod);

    Map.Entry<Long, List<PropagationStatus>> reactivate(SyncopeUser user, StatusMod statusMod);

    Map.Entry<Long, List<PropagationStatus>> suspend(SyncopeUser user, StatusMod statusMod);

    void innerSuspend(SyncopeUser user, boolean propagate);

    Map.Entry<Long, List<PropagationStatus>> create(UserTO userTO, boolean storePassword);

    Map.Entry<Long, List<PropagationStatus>> create(UserTO userTO, boolean storePassword,
            boolean disablePwdPolicyCheck, Boolean enabled, Set<String> excludedResources);

    Map.Entry<Long, List<PropagationStatus>> update(UserMod userMod, boolean removeMemberships);

    Map.Entry<Long, List<PropagationStatus>> update(UserMod userMod, Long id,
            SyncResult result, Boolean enabled, Set<String> excludedResources);

    List<PropagationStatus> delete(Long subjectKey, Set<String> excludedResources);

    void requestPasswordReset(Long id);

    void confirmPasswordReset(SyncopeUser user, String token, String password);

}
