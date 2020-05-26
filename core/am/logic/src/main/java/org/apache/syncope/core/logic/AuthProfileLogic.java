/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.core.logic;

import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional(rollbackFor = {Throwable.class})
public class AuthProfileLogic {
    @Autowired
    private AuthProfileDAO authProfileDAO;

    @Autowired
    private AuthProfileDataBinder authProfileDataBinder;

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_DELETE + "') ")
    public void deleteByKey(final String key) {
        authProfileDAO.deleteByKey(key);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_DELETE + "') ")
    public void deleteByOwner(final String owner) {
        authProfileDAO.deleteByOwner(owner);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_READ + "') ")
    public AuthProfileTO findByOwner(final String owner) {
        AuthProfile authProfile = authProfileDAO.findByOwner(owner).orElse(null);
        if (authProfile == null) {
            throw new NotFoundException(owner + " not found");
        }
        return authProfileDataBinder.getAuthProfileTO(authProfile);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_READ + "') ")
    public AuthProfileTO findByKey(final String key) {
        AuthProfile authProfile = authProfileDAO.findByKey(key).orElse(null);
        if (authProfile == null) {
            throw new NotFoundException(key + " not found");
        }
        return authProfileDataBinder.getAuthProfileTO(authProfile);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_LIST + "')")
    public List<AuthProfileTO> list() {
        return authProfileDAO.findAll().
            stream().
            map(authProfileDataBinder::getAuthProfileTO).
            collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_CREATE + "')")
    public AuthProfileTO create(final AuthProfileTO authProfileTO) {
        return authProfileDataBinder.getAuthProfileTO(authProfileDAO.save(authProfileDataBinder.create(authProfileTO)));
    }
}
