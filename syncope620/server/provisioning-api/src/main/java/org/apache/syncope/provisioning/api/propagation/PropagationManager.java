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
package org.apache.syncope.provisioning.api.propagation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.mod.MembershipMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.persistence.api.entity.Subject;
import org.apache.syncope.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.persistence.api.entity.user.User;
import org.apache.syncope.provisioning.api.WorkflowResult;

public interface PropagationManager {

    /**
     * Create the role on every associated resource.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param vAttrs virtual attributes to be set
     * @return list of propagation tasks
     */
    List<PropagationTask> getRoleCreateTaskIds(WorkflowResult<Long> wfResult, List<AttrTO> vAttrs);

    /**
     * Create the role on every associated resource.
     *
     * @param wfResult role to be propagated (and info associated), as per result from workflow
     * @param vAttrs virtual attributes to be set
     * @param noPropResourceNames external resources performing not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getRoleCreateTaskIds(
            WorkflowResult<Long> wfResult, Collection<AttrTO> vAttrs, Collection<String> noPropResourceNames);

    /**
     * Create the role on every associated resource.
     *
     * @param key role id
     * @param vAttrs virtual attributes to be set
     * @param propByRes operation to be performed per resource
     * @param noPropResourceNames external resources performing not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getRoleCreateTaskIds(Long key, Collection<AttrTO> vAttrs, PropagationByResource propByRes,
            Collection<String> noPropResourceNames);

    /**
     * Perform delete on each resource associated to the role. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param roleId to be deleted
     * @return list of propagation tasks
     */
    List<PropagationTask> getRoleDeleteTaskIds(Long roleId);

    /**
     * Perform delete on each resource associated to the role. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param roleId to be deleted
     * @param noPropResourceName name of external resource not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getRoleDeleteTaskIds(Long roleId, String noPropResourceName);

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param roleId to be deleted
     * @param noPropResourceNames name of external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getRoleDeleteTaskIds(Long roleId, Collection<String> noPropResourceNames);

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param roleId to be deleted
     * @param noPropResourceNames name of external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getRoleDeleteTaskIds(
            Long roleId, Set<String> resourceNames, Collection<String> noPropResourceNames);

    /**
     * Performs update on each resource associated to the role.
     *
     * @param wfResult role to be propagated (and info associated), as per result from workflow
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @return list of propagation tasks
     */
    List<PropagationTask> getRoleUpdateTaskIds(WorkflowResult<Long> wfResult, Set<String> vAttrsToBeRemoved,
            Set<AttrMod> vAttrsToBeUpdated);

    /**
     * Performs update on each resource associated to the role.
     *
     * @param wfResult role to be propagated (and info associated), as per result from workflow
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param noPropResourceNames external resource names not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getRoleUpdateTaskIds(WorkflowResult<Long> wfResult, Set<String> vAttrsToBeRemoved,
            Set<AttrMod> vAttrsToBeUpdated, Set<String> noPropResourceNames);

    List<PropagationTask> getUpdateTaskIds(Subject<?, ?, ?> subject, String password, boolean changePwd,
            Boolean enable, Set<String> vAttrsToBeRemoved, Set<AttrMod> vAttrsToBeUpdated,
            PropagationByResource propByRes, Collection<String> noPropResourceNames,
            Set<MembershipMod> membershipsToAdd);

    /**
     * Create the user on every associated resource.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param password to be set
     * @param vAttrs virtual attributes to be set
     * @param membershipTOs user memberships
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserCreateTaskIds(WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            String password, List<AttrTO> vAttrs, List<MembershipTO> membershipTOs);

    /**
     * Create the user on every associated resource.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param password to be set
     * @param vAttrs virtual attributes to be set
     * @param noPropResourceNames external resources not to be considered for propagation
     * @param membershipTOs user memberships
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserCreateTaskIds(WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            String password, Collection<AttrTO> vAttrs, Set<String> noPropResourceNames,
            List<MembershipTO> membershipTOs);

    List<PropagationTask> getUserCreateTaskIds(Long id, Boolean enabled,
            PropagationByResource propByRes, String password, Collection<AttrTO> vAttrs,
            Collection<MembershipTO> membershipTOs, Collection<String> noPropResourceNames);

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userKey to be deleted
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserDeleteTaskIds(Long userKey);

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userKey to be deleted
     * @param noPropResourceName name of external resource not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserDeleteTaskIds(Long userKey, String noPropResourceName);

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userKey to be deleted
     * @param noPropResourceNames name of external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserDeleteTaskIds(Long userKey, Collection<String> noPropResourceNames);

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userKey to be deleted
     * @param noPropResourceNames name of external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserDeleteTaskIds(
            Long userKey, Set<String> resourceNames, Collection<String> noPropResourceNames);

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserDeleteTaskIds(WorkflowResult<Long> wfResult);

    /**
     * Performs update on each resource associated to the user excluding the specified into 'resourceNames' parameter.
     *
     * @param user to be propagated
     * @param enable whether user must be enabled or not
     * @param noPropResourceNames external resource names not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserUpdateTaskIds(User user, Boolean enable, Set<String> noPropResourceNames);

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param changePwd whether password should be included for propagation attributes or not
     * @param noPropResourceNames external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserUpdateTaskIds(WorkflowResult<Map.Entry<UserMod, Boolean>> wfResult,
            boolean changePwd, Collection<String> noPropResourceNames);

    List<PropagationTask> getUserUpdateTaskIds(WorkflowResult<Map.Entry<UserMod, Boolean>> wfResult);

}
