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
import org.apache.syncope.wa.bootstrap.WARestClient;

import java.time.LocalDate;
import java.util.Collection;

public class SyncopeWAU2FDeviceRepository extends BaseU2FDeviceRepository {
    private final WARestClient waRestClient;
    private final LocalDate expirationDate;

    public SyncopeWAU2FDeviceRepository(final LoadingCache<String, String> requestStorage,
                                        final WARestClient waRestClient,
                                        final LocalDate expirationDate) {
        super(requestStorage);
        this.waRestClient = waRestClient;
        this.expirationDate = expirationDate;
    }

    @Override
    public Collection<? extends DeviceRegistration> getRegisteredDevices(final String username) {
        return null;
    }

    @Override
    public void registerDevice(final String username, final DeviceRegistration registration) {

    }

    @Override
    public boolean isDeviceRegisteredFor(final String username) {
        return false;
    }

    @Override
    public void clean() {

    }

    @Override
    public void removeAll() throws Exception {

    }
}
