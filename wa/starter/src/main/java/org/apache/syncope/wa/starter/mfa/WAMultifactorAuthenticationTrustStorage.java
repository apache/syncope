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
package org.apache.syncope.wa.starter.mfa;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.rest.api.beans.MfaTrustedDeviceQuery;
import org.apache.syncope.common.rest.api.service.wa.MfaTrustStorageService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.syncope.wa.starter.services.WAServiceRegistry;
import org.apereo.cas.configuration.model.support.mfa.trusteddevice.TrustedDevicesMultifactorProperties;
import org.apereo.cas.trusted.authentication.api.MultifactorAuthenticationTrustRecord;
import org.apereo.cas.trusted.authentication.api.MultifactorAuthenticationTrustRecordKeyGenerator;
import org.apereo.cas.trusted.authentication.storage.BaseMultifactorAuthenticationTrustStorage;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WAMultifactorAuthenticationTrustStorage extends BaseMultifactorAuthenticationTrustStorage {

    private static final Logger LOG = LoggerFactory.getLogger(WAServiceRegistry.class);

    protected static final int PAGE_SIZE = 500;

    protected final WARestClient waRestClient;

    public WAMultifactorAuthenticationTrustStorage(
            final TrustedDevicesMultifactorProperties trustedDevicesMultifactorProperties,
            final CipherExecutor<Serializable, String> cipherExecutor,
            final MultifactorAuthenticationTrustRecordKeyGenerator keyGenerationStrategy,
            final WARestClient waRestClient) {

        super(trustedDevicesMultifactorProperties, cipherExecutor, keyGenerationStrategy);
        this.waRestClient = waRestClient;
    }

    @Override
    protected MultifactorAuthenticationTrustRecord saveInternal(final MultifactorAuthenticationTrustRecord record) {
        MfaTrustedDevice device = new MfaTrustedDevice();
        device.setRecordKey(record.getRecordKey());
        device.setId(record.getId());
        device.setName(record.getName());
        device.setDeviceFingerprint(record.getDeviceFingerprint());
        Optional.ofNullable(record.getExpirationDate()).
                ifPresent(date -> device.setExpirationDate(date.toInstant().atZone(ZoneId.systemDefault())));
        device.setRecordDate(record.getRecordDate());

        LOG.trace("Saving multifactor authentication trust record [{}]", device);

        waRestClient.getService(MfaTrustStorageService.class).create(record.getPrincipal(), device);

        return record;
    }

    @Override
    public void remove(final ZonedDateTime expirationDate) {
        waRestClient.getService(MfaTrustStorageService.class).delete(
                new MfaTrustedDeviceQuery.Builder().expirationDate(expirationDate.toOffsetDateTime()).build());
    }

    @Override
    public void remove(final String recordKey) {
        waRestClient.getService(MfaTrustStorageService.class).delete(
                new MfaTrustedDeviceQuery.Builder().recordKey(recordKey).build());
    }

    protected MultifactorAuthenticationTrustRecord translate(final MfaTrustedDevice device) {
        MultifactorAuthenticationTrustRecord record = new MultifactorAuthenticationTrustRecord();
        record.setRecordKey(device.getRecordKey());
        record.setId(device.getId());
        record.setName(device.getName());
        record.setDeviceFingerprint(device.getDeviceFingerprint());
        Optional.ofNullable(device.getExpirationDate()).
                ifPresent(date -> record.setExpirationDate(Date.from(date.toInstant())));
        record.setRecordDate(device.getRecordDate());
        return record;
    }

    @Override
    public Set<? extends MultifactorAuthenticationTrustRecord> getAll() {
        if (!waRestClient.isReady()) {
            LOG.debug("Syncope client is not yet ready to fetch MFA trusted device records");
            return Set.of();
        }

        long count = waRestClient.getService(MfaTrustStorageService.class).
                search(new MfaTrustedDeviceQuery.Builder().page(1).size(0).build()).getTotalCount();

        Set<MultifactorAuthenticationTrustRecord> result = new HashSet<>();

        for (int page = 1; page <= (count / PAGE_SIZE) + 1; page++) {
            waRestClient.getService(MfaTrustStorageService.class).
                    search(new MfaTrustedDeviceQuery.Builder().page(page).size(PAGE_SIZE).
                            orderBy("expirationDate").build()).
                    getResult().stream().
                    map(this::translate).
                    forEach(result::add);
        }

        return result;
    }

    @Override
    public Set<? extends MultifactorAuthenticationTrustRecord> get(final ZonedDateTime onOrAfterDate) {
        if (!waRestClient.isReady()) {
            LOG.debug("Syncope client is not yet ready to fetch MFA trusted device records");
            return Set.of();
        }

        return waRestClient.getService(MfaTrustStorageService.class).
                search(new MfaTrustedDeviceQuery.Builder().recordDate(onOrAfterDate.toOffsetDateTime()).build()).
                getResult().stream().
                map(this::translate).
                collect(Collectors.toSet());
    }

    @Override
    public Set<? extends MultifactorAuthenticationTrustRecord> get(final String principal) {
        if (!waRestClient.isReady()) {
            LOG.debug("Syncope client is not yet ready to fetch MFA trusted device records");
            return Set.of();
        }

        return waRestClient.getService(MfaTrustStorageService.class).
                search(new MfaTrustedDeviceQuery.Builder().principal(principal).build()).getResult().stream().
                map(this::translate).
                collect(Collectors.toSet());
    }

    @Override
    public MultifactorAuthenticationTrustRecord get(final long id) {
        if (!waRestClient.isReady()) {
            LOG.debug("Syncope client is not yet ready to fetch MFA trusted device records");
            return null;
        }

        return waRestClient.getService(MfaTrustStorageService.class).
                search(new MfaTrustedDeviceQuery.Builder().id(id).build()).getResult().stream().findFirst().
                map(this::translate).
                orElse(null);
    }
}
