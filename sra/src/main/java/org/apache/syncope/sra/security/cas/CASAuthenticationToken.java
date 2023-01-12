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
package org.apache.syncope.sra.security.cas;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.client.validation.Assertion;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class CASAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 6333776590644298469L;

    private final Assertion assertion;

    public CASAuthenticationToken(final Assertion assertion) {
        super(null);
        this.assertion = assertion;
        this.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return StringUtils.EMPTY;
    }

    @Override
    public Assertion getPrincipal() {
        return assertion;
    }
}
