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
package org.apache.syncope.wa.starter.u2f;

import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.wa.U2FDevice;
import org.apache.syncope.common.rest.api.beans.U2FDeviceQuery;
import org.apache.syncope.common.rest.api.service.wa.U2FRegistrationService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.adaptors.u2f.storage.BaseU2FDeviceRepository;
import org.apereo.cas.adaptors.u2f.storage.U2FDeviceRegistration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public class SyncopeWAU2FDeviceRepository extends BaseU2FDeviceRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWAU2FDeviceRepository.class);

    private final WARestClient waRestClient;

    private final OffsetDateTime expirationDate;

    public SyncopeWAU2FDeviceRepository(
            final CasConfigurationProperties casProperties,
            final LoadingCache<String, String> requestStorage,
            final WARestClient waRestClient,
            final OffsetDateTime expirationDate) {

        super(casProperties, requestStorage, CipherExecutor.noOpOfSerializableToString());
        this.waRestClient = waRestClient;
        this.expirationDate = expirationDate;
    }

    private static U2FDeviceRegistration parseRegistrationRecord(final String owner, final U2FDevice device) {
        try {
            return U2FDeviceRegistration.builder().
                    id(device.getId()).
                    username(owner).
                    record(device.getRecord()).
                    createdDate(device.getIssueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).
                    build();
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Collection<? extends U2FDeviceRegistration> getRegisteredDevices(final String owner) {
        return getU2FService().
                search(new U2FDeviceQuery.Builder().owner(owner).expirationDate(expirationDate).build()).getResult().
                stream().
                map(device -> parseRegistrationRecord(owner, device)).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    @Override
    public Collection<? extends U2FDeviceRegistration> getRegisteredDevices() {
        return getU2FService().search(new U2FDeviceQuery.Builder().expirationDate(expirationDate).build()).getResult().
                stream().
                map(device -> parseRegistrationRecord("", device)).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    @Override
    public U2FDeviceRegistration registerDevice(final U2FDeviceRegistration registration) {
        U2FDevice record = new U2FDevice.Builder().
                issueDate(OffsetDateTime.of(
                        registration.getCreatedDate().atStartOfDay(), OffsetDateTime.now().getOffset())).
                record(registration.getRecord()).
                id(registration.getId()).
                build();
        getU2FService().create(registration.getUsername(), record);
        return parseRegistrationRecord(registration.getUsername(), record);
    }

    @Override
    public void deleteRegisteredDevice(final U2FDeviceRegistration registration) {
        getU2FService().delete(new U2FDeviceQuery.Builder().id(registration.getId()).build());
    }

    @Override
    public boolean isDeviceRegisteredFor(final String owner) {
        try {
            Collection<? extends U2FDeviceRegistration> devices = getRegisteredDevices(owner);
            return !CollectionUtils.isEmpty(devices);
        } catch (final SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound) {
                LOG.info("Could not locate account for owner {}", owner);
            } else {
                LOG.error(e.getMessage(), e);
            }
        }
        return false;
    }

    @Override
    public void clean() {
        getU2FService().delete(new U2FDeviceQuery.Builder().expirationDate(expirationDate).build());
    }

    @Override
    public void removeAll() {
        getU2FService().delete(new U2FDeviceQuery.Builder().build());
    }

    private U2FRegistrationService getU2FService() {
        if (!WARestClient.isReady()) {
            throw new IllegalStateException("Syncope core is not yet ready");
        }
        return waRestClient.getSyncopeClient().getService(U2FRegistrationService.class);
    }
}
