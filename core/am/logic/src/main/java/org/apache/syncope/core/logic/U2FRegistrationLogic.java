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

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.U2FRegisteredDevice;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    public void delete(final String entityKey, final Long id, final Date expirationDate) {
        List<AuthProfile> profiles = authProfileDAO.findAll();
        profiles.forEach(profile -> {
            List<U2FRegisteredDevice> devices = profile.getU2FRegisteredDevices();
            if (devices != null) {
                if (StringUtils.isNotBlank(entityKey)) {
                    devices.removeIf(device -> device.getKey().equals(entityKey));
                } else if (id != null) {
                    devices.removeIf(device -> device.getId() == id);
                } else if (expirationDate != null) {
                    devices.removeIf(device -> device.getIssueDate().compareTo(expirationDate) < 0);
                } else {
                    devices = List.of();
                }
                profile.setU2FRegisteredDevices(devices);
                authProfileDAO.save(profile);
            }
        });
    }

    @PreAuthorize("hasRole('" + AMEntitlement.U2F_SEARCH + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public Pair<Integer, List<U2FRegisteredDevice>> search(final String entityKey, final Integer page,
            final Integer itemsPerPage, final Long id,
            final Date expirationDate,
            final List<OrderByClause> orderByClauses) {
        List<Comparator<U2FRegisteredDevice>> comparatorList = orderByClauses.
                stream().
                map(orderByClause -> {
                    Comparator<U2FRegisteredDevice> comparator = null;
                    if (orderByClause.getField().equals("id")) {
                        comparator = (o1, o2) -> new CompareToBuilder().
                                append(o1.getId(), o2.getId()).toComparison();
                    }
                    if (orderByClause.getField().equals("owner")) {
                        comparator = (o1, o2) -> new CompareToBuilder().
                                append(o1.getOwner(), o2.getOwner()).toComparison();
                    }
                    if (orderByClause.getField().equals("key")) {
                        comparator = (o1, o2) -> new CompareToBuilder().
                                append(o1.getKey(), o2.getKey()).toComparison();
                    }
                    if (orderByClause.getField().equals("issueDate")) {
                        comparator = (o1, o2) -> new CompareToBuilder().
                                append(o1.getIssueDate(), o2.getIssueDate()).toComparison();
                    }
                    if (orderByClause.getField().equals("record")) {
                        comparator = (o1, o2) -> new CompareToBuilder().
                                append(o1.getRecord(), o2.getRecord()).toComparison();
                    }
                    if (comparator != null) {
                        if (orderByClause.getDirection() == OrderByClause.Direction.DESC) {
                            return comparator.reversed();
                        }
                        return comparator;
                    }
                    return null;
                }).
                filter(Objects::nonNull).
                collect(Collectors.toList());

        List<U2FRegisteredDevice> devices = authProfileDAO.findAll().
                stream().
                map(AuthProfile::getU2FRegisteredDevices).
                filter(Objects::nonNull).
                flatMap(List::stream).
                filter(device -> {
                    EqualsBuilder builder = new EqualsBuilder();
                    if (StringUtils.isNotBlank(entityKey)) {
                        builder.append(entityKey, device.getKey());
                    }
                    if (id != null) {
                        builder.append(id, (Long) device.getId());
                    }
                    if (expirationDate != null) {
                        builder.appendSuper(device.getIssueDate().compareTo(expirationDate) >= 0);
                    }
                    return true;
                }).
                filter(Objects::nonNull).
                collect(Collectors.toList());

        List<U2FRegisteredDevice> pagedResults = devices.
                stream().
                limit(itemsPerPage).
                skip(itemsPerPage * (page <= 0 ? 0L : page.longValue() - 1L)).
                sorted((o1, o2) -> {
                    int result;
                    for (Comparator<U2FRegisteredDevice> comparator : comparatorList) {
                        result = comparator.compare(o1, o2);
                        if (result != 0) {
                            return result;
                        }
                    }
                    return 0;
                })
                .collect(Collectors.toList());
        return Pair.of(devices.size(), pagedResults);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.U2F_UPDATE_DEVICE + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void update(final U2FRegisteredDevice acct) {
        List<AuthProfile> profiles = authProfileDAO.findAll();
        profiles.forEach(profile -> {
            List<U2FRegisteredDevice> devices = profile.getU2FRegisteredDevices();
            if (devices != null) {
                if (devices.removeIf(device -> device.getKey().equals(acct.getKey()))) {
                    devices.add(acct);
                    profile.setU2FRegisteredDevices(devices);
                    authProfileDAO.save(profile);
                }
            }
        });
    }
}
