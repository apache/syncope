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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class AuthProfileLogic extends AbstractAuthProfileLogic {

    public AuthProfileLogic(final AuthProfileDAO authProfileDAO, final AuthProfileDataBinder binder) {
        super(authProfileDAO, binder);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_DELETE + "') ")
    public void delete(final String key) {
        authProfileDAO.delete(key);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_READ + "') ")
    @Transactional(readOnly = true)
    public AuthProfileTO read(final String key) {
        return Optional.ofNullable(authProfileDAO.find(key)).
                map(binder::getAuthProfileTO).
                orElseThrow(() -> new NotFoundException(key + " not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_CREATE + "') ")
    public AuthProfileTO create(final AuthProfileTO authProfileTO) {
        return binder.getAuthProfileTO(authProfileDAO.save(binder.create(authProfileTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_UPDATE + "') ")
    public void update(final AuthProfileTO authProfileTO) {
        AuthProfile authProfile = Optional.ofNullable(authProfileDAO.find(authProfileTO.getKey())).
                orElseThrow(() -> new NotFoundException(authProfileTO.getKey() + " not found"));
        binder.update(authProfile, authProfileTO);
        authProfileDAO.save(authProfile);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_PROFILE_LIST + "')")
    @Transactional(readOnly = true)
    public Pair<Integer, List<AuthProfileTO>> list(final int page, final int size) {
        int count = authProfileDAO.count();

        List<AuthProfileTO> result = authProfileDAO.findAll(page, size).
                stream().
                map(binder::getAuthProfileTO).
                collect(Collectors.toList());

        return Pair.of(count, result);
    }
}
