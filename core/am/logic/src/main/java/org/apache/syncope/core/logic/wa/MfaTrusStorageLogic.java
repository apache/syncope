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
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.core.logic.AbstractAuthProfileLogic;
import org.apache.syncope.core.persistence.api.dao.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;

public class MfaTrusStorageLogic extends AbstractAuthProfileLogic {

    public MfaTrusStorageLogic(
            final AuthProfileDataBinder binder,
            final AuthProfileDAO authProfileDAO,
            final EntityFactory entityFactory) {

        super(binder, authProfileDAO, entityFactory);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public Pair<Integer, List<MfaTrustedDevice>> search(
            final Integer page,
            final Integer itemsPerPage,
            final String principal,
            final Long id,
            final OffsetDateTime recordDate,
            final List<OrderByClause> orderByClauses) {

        List<Comparator<MfaTrustedDevice>> comparatorList = orderByClauses.
                stream().
                map(orderByClause -> {
                    Comparator<MfaTrustedDevice> comparator = null;
                    if (orderByClause.getField().equals("id")) {
                        comparator = (o1, o2) -> new CompareToBuilder().
                                append(o1.getId(), o2.getId()).toComparison();
                    }
                    if (orderByClause.getField().equals("expirationDate")) {
                        comparator = (o1, o2) -> new CompareToBuilder().
                                append(o1.getExpirationDate(), o2.getExpirationDate()).toComparison();
                    }
                    if (orderByClause.getField().equals("recordDate")) {
                        comparator = (o1, o2) -> new CompareToBuilder().
                                append(o1.getRecordDate(), o2.getRecordDate()).toComparison();
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

        List<MfaTrustedDevice> devices = (principal == null
                ? authProfileDAO.findAll(-1, -1).stream().
                        map(AuthProfile::getMfaTrustedDevices).filter(Objects::nonNull).flatMap(List::stream)
                : authProfileDAO.findByOwner(principal).
                        map(AuthProfile::getMfaTrustedDevices).filter(Objects::nonNull).map(List::stream).
                        orElse(Stream.empty())).
                filter(device -> {
                    EqualsBuilder builder = new EqualsBuilder();
                    builder.appendSuper(device.getExpirationDate().isAfter(ZonedDateTime.now()));
                    if (id != null) {
                        builder.append(id, (Long) device.getId());
                    }
                    if (recordDate != null) {
                        builder.appendSuper(device.getRecordDate().isAfter(recordDate.toZonedDateTime()));
                    }
                    return builder.build();
                }).
                filter(Objects::nonNull).
                collect(Collectors.toList());

        List<MfaTrustedDevice> result = devices.stream().
                limit(itemsPerPage).
                skip(itemsPerPage * (page <= 0 ? 0L : page.longValue() - 1L)).
                sorted((o1, o2) -> {
                    int compare;
                    for (Comparator<MfaTrustedDevice> comparator : comparatorList) {
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

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void create(final String owner, final MfaTrustedDevice device) {
        AuthProfile profile = authProfile(owner);

        List<MfaTrustedDevice> devices = profile.getMfaTrustedDevices();
        devices.add(device);
        profile.setMfaTrustedDevices(devices);
        authProfileDAO.save(profile);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final OffsetDateTime expirationDate, final String recordKey) {
        List<AuthProfile> profiles = authProfileDAO.findAll(-1, -1);
        profiles.forEach(profile -> {
            List<MfaTrustedDevice> devices = profile.getMfaTrustedDevices();
            if (devices != null) {
                if (recordKey != null) {
                    devices.removeIf(device -> recordKey.equals(device.getRecordKey()));
                } else if (expirationDate != null) {
                    devices.removeIf(device -> device.getExpirationDate().isBefore(expirationDate.toZonedDateTime()));
                } else {
                    devices = List.of();
                }
                profile.setMfaTrustedDevices(devices);
                authProfileDAO.save(profile);
            }
        });
    }
}
