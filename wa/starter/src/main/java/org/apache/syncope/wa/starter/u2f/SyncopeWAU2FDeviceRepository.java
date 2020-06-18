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

import org.apereo.cas.adaptors.u2f.storage.BaseU2FDeviceRepository;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.yubico.u2f.data.DeviceRegistration;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.U2FRegistration;
import org.apache.syncope.common.rest.api.service.wa.U2FRegistrationService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class SyncopeWAU2FDeviceRepository extends BaseU2FDeviceRepository {
    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWAU2FDeviceRepository.class);

    private final WARestClient waRestClient;

    private final LocalDate expirationDate;

    public SyncopeWAU2FDeviceRepository(final LoadingCache<String, String> requestStorage,
                                        final WARestClient waRestClient,
                                        final LocalDate expirationDate) {
        super(requestStorage);
        this.waRestClient = waRestClient;
        this.expirationDate = expirationDate;
    }

    private static DeviceRegistration parseRegistrationRecord(final U2FRegistration record) {
        try {
            return DeviceRegistration.fromJson(record.getRecord());
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Collection<? extends DeviceRegistration> getRegisteredDevices(final String owner) {
        final PagedResult<U2FRegistration> records = getU2FService().findRegistrationFor(owner);
        return records.getResult().
            stream().
            map(SyncopeWAU2FDeviceRepository::parseRegistrationRecord).
            filter(Objects::nonNull).
            collect(Collectors.toList());
    }

    @Override
    public void registerDevice(final String username, final DeviceRegistration registration) {

    }

    @Override
    public boolean isDeviceRegisteredFor(final String owner) {
        try {
            return getU2FService().findRegistrationFor(owner) != null;
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

    }

    @Override
    public void removeAll() throws Exception {
        getU2FService().deleteAll();
    }

    private U2FRegistrationService getU2FService() {
        if (!WARestClient.isReady()) {
            throw new RuntimeException("Syncope core is not yet ready");
        }
        return waRestClient.getSyncopeClient().getService(U2FRegistrationService.class);
    }
}
