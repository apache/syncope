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
package org.apache.syncope.core.provisioning.api.propagation;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.WorkflowResult;

public interface PropagationManager {

    /**
     * Create the group on every associated resource.
     *
     * @param wfResult group to be propagated (and info associated), as per result from workflow
     * @param vAttrs virtual attributes to be set
     * @param noPropResourceNames external resources performing not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getGroupCreateTasks(
            WorkflowResult<Long> wfResult, Collection<AttrTO> vAttrs, Collection<String> noPropResourceNames);

    /**
     * Create the group on every associated resource.
     *
     * @param key group id
     * @param vAttrs virtual attributes to be set
     * @param propByRes operation to be performed per resource
     * @param noPropResourceNames external resources performing not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getGroupCreateTasks(Long key, Collection<AttrTO> vAttrs, PropagationByResource propByRes,
            Collection<String> noPropResourceNames);

    /**
     * Performs update on each resource associated to the group.
     *
     * @param wfResult group to be propagated (and info associated), as per result from workflow
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param noPropResourceNames external resource names not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getGroupUpdateTasks(WorkflowResult<Long> wfResult, Set<String> vAttrsToBeRemoved,
            Set<AttrMod> vAttrsToBeUpdated, Set<String> noPropResourceNames);

    /**
     * Perform delete on each resource associated to the group. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param groupKey to be deleted
     * @return list of propagation tasks
     */
    List<PropagationTask> getGroupDeleteTasks(Long groupKey);

    /**
     * Perform delete on each resource associated to the group. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param groupKey to be deleted
     * @param noPropResourceName name of external resource not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getGroupDeleteTasks(Long groupKey, String noPropResourceName);

    /**
     * Perform delete on each resource associated to the group. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param groupKey to be deleted
     * @param noPropResourceNames name of external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getGroupDeleteTasks(Long groupKey, Collection<String> noPropResourceNames);

    /**
     * Perform delete on each resource associated to the group. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param groupKey to be deleted
     * @param resourceNames resource from which group is to be deleted
     * @param noPropResourceNames name of external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getGroupDeleteTasks(
            Long groupKey, Set<String> resourceNames, Collection<String> noPropResourceNames);

    List<PropagationTask> getAnyObjectCreateTasks(Long anyObjectKey, Collection<AttrTO> vAttrs,
            PropagationByResource propByRes, List<String> noPropResourceNames);

    List<PropagationTask> getAnyObjectDeleteTasks(Long anyObjectKey, String noPropResourceName);

    List<PropagationTask> getAnyObjectDeleteTasks(Long anyObjectKey, Collection<String> noPropResourceNames);

    /**
     * Create the user on every associated resource.
     *
     * @param key to be propagated
     * @param enable whether user must be enabled or not
     * @param propByRes operation to be performed per resource
     * @param password to be set
     * @param vAttrs virtual attributes to be set
     * @param noPropResourceNames external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserCreateTasks(Long key, Boolean enable,
            PropagationByResource propByRes, String password, Collection<AttrTO> vAttrs,
            Collection<String> noPropResourceNames);

    /**
     * Performs update on each resource associated to the user excluding the specified into 'resourceNames' parameter.
     *
     * @param user to be propagated
     * @param enable whether user must be enabled or not
     * @param noPropResourceNames external resource names not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserUpdateTasks(User user, Boolean enable, Collection<String> noPropResourceNames);

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param changePwd whether password should be included for propagation attributes or not
     * @param noPropResourceNames external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserUpdateTasks(WorkflowResult<Pair<UserMod, Boolean>> wfResult,
            boolean changePwd, Collection<String> noPropResourceNames);

    List<PropagationTask> getUserUpdateTasks(WorkflowResult<Pair<UserMod, Boolean>> wfResult);

    List<PropagationTask> getUpdateTasks(Any<?, ?, ?> any, String password, boolean changePwd,
            Boolean enable, Set<String> vAttrsToBeRemoved, Set<AttrMod> vAttrsToBeUpdated,
            PropagationByResource propByRes, Collection<String> noPropResourceNames);

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userKey to be deleted
     * @param noPropResourceNames name of external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserDeleteTasks(Long userKey, Collection<String> noPropResourceNames);

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userKey to be deleted
     * @param resourceNames resources
     * @param noPropResourceNames name of external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserDeleteTasks(
            Long userKey, Set<String> resourceNames, Collection<String> noPropResourceNames);

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @return list of propagation tasks
     */
    List<PropagationTask> getUserDeleteTasks(WorkflowResult<Long> wfResult);

}
