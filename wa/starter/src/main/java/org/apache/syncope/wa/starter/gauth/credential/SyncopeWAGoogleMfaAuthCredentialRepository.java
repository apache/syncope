/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.wa.starter.gauth.credential;

import org.apereo.cas.authentication.OneTimeTokenAccount;
import org.apereo.cas.otp.repository.credentials.BaseOneTimeTokenCredentialRepository;
import org.apereo.cas.util.crypto.CipherExecutor;

import java.util.Collection;
import java.util.List;

public class SyncopeWAGoogleMfaAuthCredentialRepository extends BaseOneTimeTokenCredentialRepository {
    protected SyncopeWAGoogleMfaAuthCredentialRepository() {
        super(CipherExecutor.noOpOfStringToString());
    }

    @Override
    public OneTimeTokenAccount get(final String username) {
        return null;
    }

    @Override
    public Collection<? extends OneTimeTokenAccount> load() {
        return null;
    }

    @Override
    public void save(final String userName, final String secretKey, final int validationCode, final List<Integer> scratchCodes) {

    }

    @Override
    public OneTimeTokenAccount create(final String username) {
        return null;
    }

    @Override
    public OneTimeTokenAccount update(final OneTimeTokenAccount account) {
        return null;
    }

    @Override
    public void deleteAll() {

    }

    @Override
    public void delete(final String username) {

    }

    @Override
    public long count() {
        return 0;
    }
}
