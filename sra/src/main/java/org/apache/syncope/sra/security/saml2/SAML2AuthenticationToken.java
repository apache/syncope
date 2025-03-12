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
package org.apache.syncope.sra.security.saml2;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.saml.credentials.SAML2AuthenticationCredentials;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class SAML2AuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 8322987617416135717L;

    private final SAML2AuthenticationCredentials credentials;

    public SAML2AuthenticationToken(final SAML2AuthenticationCredentials credentials) {
        super(Optional.ofNullable(credentials.getUserProfile()).
                map(p -> p.getRoles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet())).
            orElseGet(Set::of));
        this.credentials = credentials;
        this.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return StringUtils.EMPTY;
    }

    @Override
    public SAML2AuthenticationCredentials getPrincipal() {
        return credentials;
    }
}
