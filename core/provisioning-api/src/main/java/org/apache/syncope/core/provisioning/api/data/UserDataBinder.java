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
package org.apache.syncope.core.provisioning.api.data;

import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.persistence.api.entity.user.User;

public interface UserDataBinder {

    UserTO returnUserTO(UserTO userTO);

    UserTO getAuthenticatedUserTO();

    UserTO getUserTO(String key);

    UserTO getUserTO(User user, boolean details);

    void create(User user, UserTO userTO, boolean storePassword);

    /**
     * Update user, given {@link UserPatch}.
     *
     * @param toBeUpdated user to be updated
     * @param userPatch bean containing update request
     * @return updated user + propagation by resource
     * @see PropagationByResource
     */
    PropagationByResource update(User toBeUpdated, UserPatch userPatch);

    boolean verifyPassword(String username, String password);

    boolean verifyPassword(User user, String password);
}
