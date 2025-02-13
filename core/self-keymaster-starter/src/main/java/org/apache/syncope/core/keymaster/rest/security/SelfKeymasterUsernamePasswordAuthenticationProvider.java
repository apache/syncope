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
package org.apache.syncope.core.keymaster.rest.security;

import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterProperties;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.core.spring.security.DefaultCredentialChecker;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.UsernamePasswordAuthenticationProvider;
import org.springframework.security.core.Authentication;

public class SelfKeymasterUsernamePasswordAuthenticationProvider extends UsernamePasswordAuthenticationProvider {

    protected final KeymasterProperties keymasterProperties;

    public SelfKeymasterUsernamePasswordAuthenticationProvider(
            final DomainOps domainOps,
            final AuthDataAccessor dataAccessor,
            final UserProvisioningManager provisioningManager,
            final DefaultCredentialChecker credentialChecker,
            final SecurityProperties securityProperties,
            final KeymasterProperties keymasterProperties,
            final EncryptorManager encryptorManager) {

        super(domainOps, dataAccessor, provisioningManager, credentialChecker, securityProperties, encryptorManager);
        this.keymasterProperties = keymasterProperties;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) {
        if (keymasterProperties.getUsername().equals(authentication.getName())) {
            return finalizeAuthentication(
                    authentication.getCredentials().toString().equals(keymasterProperties.getPassword()),
                    SyncopeAuthenticationDetails.class.cast(authentication.getDetails()).getDomain(),
                    keymasterProperties.getUsername(),
                    null,
                    authentication);
        }

        return super.authenticate(authentication);
    }
}
