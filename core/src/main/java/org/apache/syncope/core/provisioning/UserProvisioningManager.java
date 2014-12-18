/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.syncope.core.provisioning;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.sync.SyncResult;

public interface UserProvisioningManager extends ProvisioningManager<UserTO, UserMod>{
    
    public Map.Entry<Long, List<PropagationStatus>> activate(SyncopeUser user, StatusMod statusMod);

    public Map.Entry<Long, List<PropagationStatus>> reactivate(SyncopeUser user, StatusMod statusMod);

    public Map.Entry<Long, List<PropagationStatus>> suspend(SyncopeUser user, StatusMod statusMod);
    
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO, final boolean storePassword);
    
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO, final boolean storePassword, boolean disablePwdPolicyCheck, Boolean enabled,Set<String> excludedResources);
    
    public Map.Entry<Long, List<PropagationStatus>> update(final UserMod userMod, final boolean removeMemberships);
    
    public Map.Entry<Long, List<PropagationStatus>> updateInSync(final UserMod userMod,final Long id, final SyncResult result, Boolean enabled, Set<String> excludedResources);

    public List<PropagationStatus> delete(Long subjectId, Set<String> excludedResources);    
    
    public void innerSuspend(SyncopeUser user, boolean suspend);
    
    public void requestPasswordReset(final Long id);
    
    public void confirmPasswordReset(SyncopeUser user,final String token,final String password);
    
}
