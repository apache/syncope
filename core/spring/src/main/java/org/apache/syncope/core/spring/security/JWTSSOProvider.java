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
package org.apache.syncope.core.spring.security;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.entity.user.User;

/**
 * Enables a generic mechanism for JWT validation and subject resolution which allows to plug in implementations
 * recognizing JWT produced by third parties.
 */
public interface JWTSSOProvider extends JWSVerifier {

    /**
     * Gives the identifier for the JWT issuer verified by this instance.
     *
     * @return identifier for the JWT issuer verified by this instance
     */
    String getIssuer();

    /**
     * Attempts to resolve the given JWT claims into internal {@link User} and authorities.
     * <strong>IMPORTANT</strong>: this is not invoked for the {@code admin} super-user.
     *
     * @param jwtClaims JWT claims
     * @return internal User, with authorities, matching the provided JWT claims, if found; otherwise null
     */
    Pair<User, Set<SyncopeGrantedAuthority>> resolve(JWTClaimsSet jwtClaims);
}
