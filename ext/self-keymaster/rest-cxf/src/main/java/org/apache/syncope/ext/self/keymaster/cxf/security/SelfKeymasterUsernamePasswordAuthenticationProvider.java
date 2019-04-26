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
package org.apache.syncope.ext.self.keymaster.cxf.security;

import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.UsernamePasswordAuthenticationProvider;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;

@Configurable
public class SelfKeymasterUsernamePasswordAuthenticationProvider extends UsernamePasswordAuthenticationProvider {

    @Value("${keymaster.username}")
    private String keymasterUsername;

    @Value("${keymaster.password}")
    private String keymasterPassword;

    @Override
    public Authentication authenticate(final Authentication authentication) {
        if (keymasterUsername.equals(authentication.getName())) {
            return finalizeAuthentication(
                    authentication.getCredentials().toString().equals(keymasterPassword),
                    SyncopeAuthenticationDetails.class.cast(authentication.getDetails()).getDomain(),
                    keymasterUsername,
                    authentication);
        }

        return super.authenticate(authentication);
    }
}
