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
package org.apache.syncope.core.logic;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthProfileLogic extends AbstractAuthProfileLogic {

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_DELETE + "') ")
    public void delete(final String key) {
        authProfileDAO.delete(key);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_DELETE + "') ")
    public void deleteByOwner(final String owner) {
        authProfileDAO.findByOwner(owner).ifPresent(authProfileDAO::delete);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_READ + "') ")
    @Transactional(readOnly = true)
    public AuthProfileTO readByOwner(final String owner) {
        return authProfileDAO.findByOwner(owner).
                map(binder::getAuthProfileTO).
                orElseThrow(() -> new NotFoundException(owner + " not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_READ + "') ")
    @Transactional(readOnly = true)
    public AuthProfileTO read(final String key) {
        AuthProfile authProfile = authProfileDAO.find(key);
        if (authProfile == null) {
            throw new NotFoundException(key + " not found");
        }
        return binder.getAuthProfileTO(authProfile);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_LIST + "')")
    @Transactional(readOnly = true)
    public List<AuthProfileTO> list() {
        return authProfileDAO.findAll().
                stream().
                map(binder::getAuthProfileTO).
                collect(Collectors.toList());
    }
}
