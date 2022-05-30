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
package org.apache.syncope.core.logic.wa;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.wa.U2FDevice;
import org.apache.syncope.core.logic.AbstractAuthProfileLogic;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;

public class U2FRegistrationLogic extends AbstractAuthProfileLogic {

    protected final EntityFactory entityFactory;

    public U2FRegistrationLogic(
            final EntityFactory entityFactory,
            final AuthProfileDAO authProfileDAO,
            final AuthProfileDataBinder binder) {

        super(authProfileDAO, binder);
        this.entityFactory = entityFactory;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void create(final String owner, final U2FDevice device) {
        AuthProfile profile = authProfileDAO.findByOwner(owner).orElseGet(() -> {
            AuthProfile authProfile = entityFactory.newEntity(AuthProfile.class);
            authProfile.setOwner(owner);
            return authProfile;
        });

        List<U2FDevice> devices = profile.getU2FRegisteredDevices();
        devices.add(device);
        profile.setU2FRegisteredDevices(devices);
        authProfileDAO.save(profile);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final Long id, final OffsetDateTime expirationDate) {
        List<AuthProfile> profiles = authProfileDAO.findAll(-1, -1);
        profiles.forEach(profile -> {
            List<U2FDevice> devices = profile.getU2FRegisteredDevices();
            if (devices != null) {
                if (id != null) {
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

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public Pair<Integer, List<U2FDevice>> search(
            final Integer page,
            final Integer itemsPerPage, final Long id,
            final OffsetDateTime expirationDate,
            final List<OrderByClause> orderByClauses) {

        List<Comparator<U2FDevice>> comparatorList = orderByClauses.
                stream().
                map(orderByClause -> {
                    Comparator<U2FDevice> comparator = null;
                    if (orderByClause.getField().equals("id")) {
                        comparator = (o1, o2) -> new CompareToBuilder().
                                append(o1.getId(), o2.getId()).toComparison();
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

        List<U2FDevice> devices = authProfileDAO.findAll(-1, -1).
                stream().
                map(AuthProfile::getU2FRegisteredDevices).
                filter(Objects::nonNull).
                flatMap(List::stream).
                filter(device -> {
                    EqualsBuilder builder = new EqualsBuilder();
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

        List<U2FDevice> result = devices.stream().
                limit(itemsPerPage).
                skip(itemsPerPage * (page <= 0 ? 0L : page.longValue() - 1L)).
                sorted((o1, o2) -> {
                    int compare;
                    for (Comparator<U2FDevice> comparator : comparatorList) {
                        compare = comparator.compare(o1, o2);
                        if (compare != 0) {
                            return compare;
                        }
                    }
                    return 0;
                })
                .collect(Collectors.toList());
        return Pair.of(devices.size(), result);
    }
}
