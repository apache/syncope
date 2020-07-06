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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.U2FRegisteredDevice;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class U2FRegistrationLogic extends AbstractTransactionalLogic<AuthProfileTO> {
    @Autowired
    private AuthProfileDAO authProfileDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AuthProfileDataBinder authProfileDataBinder;

    @Override
    protected AuthProfileTO resolveReference(final Method method, final Object... args)
        throws UnresolvedReferenceException {
        String key = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof AuthProfileTO) {
                    key = ((AuthProfileTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return authProfileDAO.findByKey(key).
                    map(authProfileDataBinder::getAuthProfileTO).
                    orElseThrow();
            } catch (final Throwable e) {
                LOG.debug("Unresolved reference", e);
                throw new UnresolvedReferenceException(e);
            }
        }
        throw new UnresolvedReferenceException();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.U2F_DELETE_DEVICE + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteAll() {
        authProfileDAO.findAll().
            forEach(profile -> {
                profile.setU2FRegisteredDevices(List.of());
                authProfileDAO.save(profile);
            });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.U2F_DELETE_DEVICE + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteExpiredDevices(final Date expirationDate) {
        final List<AuthProfile> profiles = authProfileDAO.findAll();
        profiles.forEach(profile -> {
            List<U2FRegisteredDevice> records = profile.getU2FRegisteredDevices();
            if (records.removeIf(record -> record.getIssueDate().compareTo(expirationDate) < 0)) {
                profile.setU2FRegisteredDevices(records);
                authProfileDAO.save(profile);
            }
        });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.U2F_SAVE_DEVICE + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public U2FRegisteredDevice save(final U2FRegisteredDevice acct) {
        AuthProfile profile = authProfileDAO.findByOwner(acct.getOwner()).
            orElseGet(() -> {
                final AuthProfile authProfile = entityFactory.newEntity(AuthProfile.class);
                authProfile.setOwner(acct.getOwner());
                return authProfile;
            });

        if (acct.getKey() == null) {
            acct.setKey(SecureRandomUtils.generateRandomUUID().toString());
        }
        profile.add(acct);
        profile = authProfileDAO.save(profile);
        return profile.getU2FRegisteredDevices().
            stream().
            filter(Objects::nonNull).
            filter(t -> t.getKey().equals(acct.getKey())).
            findFirst().
            orElse(null);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.U2F_LIST_DEVICE + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public Collection<? extends U2FRegisteredDevice> list(final Date expirationDate) {
        return authProfileDAO.findAll().
            stream().
            map(AuthProfile::getU2FRegisteredDevices).
            filter(Objects::nonNull).
            flatMap(List::stream).
            filter(device -> expirationDate == null || device.getIssueDate().compareTo(expirationDate) >= 0).
            filter(Objects::nonNull).
            collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.U2F_READ_DEVICE + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public Collection<? extends U2FRegisteredDevice> findRegistrationFor(final String owner,
                                                                         final Date expirationDate) {
        return authProfileDAO.findByOwner(owner).
            map(profile -> new ArrayList<>(profile.getU2FRegisteredDevices())).
            orElse(new ArrayList<>(0)).
            stream().
            filter(record -> record.getIssueDate().compareTo(expirationDate) >= 0).
            collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.U2F_READ_DEVICE + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public U2FRegisteredDevice read(final String key) {
        return authProfileDAO.findAll().
            stream().
            map(AuthProfile::getU2FRegisteredDevices).
            filter(Objects::nonNull).
            flatMap(List::stream).
            filter(record -> record.getKey().equals(key)).
            findFirst().
            orElse(null);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.U2F_DELETE_DEVICE + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final long id) {
        List<AuthProfile> profiles = authProfileDAO.findAll();
        profiles.forEach(profile -> {
            List<U2FRegisteredDevice> devices = profile.getU2FRegisteredDevices();
            if (devices != null) {
                if (devices.removeIf(device -> device.getId() == id)) {
                    profile.setU2FRegisteredDevices(devices);
                    authProfileDAO.save(profile);
                }
            }
        });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.U2F_DELETE_DEVICE + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String key) {
        List<AuthProfile> profiles = authProfileDAO.findAll();
        profiles.forEach(profile -> {
            List<U2FRegisteredDevice> devices = profile.getU2FRegisteredDevices();
            if (devices != null) {
                if (devices.removeIf(device -> device.getKey().equals(key))) {
                    profile.setU2FRegisteredDevices(devices);
                    authProfileDAO.save(profile);
                }
            }
        });
    }
}
