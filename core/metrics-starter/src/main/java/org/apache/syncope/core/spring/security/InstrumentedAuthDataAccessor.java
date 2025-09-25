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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;

public class InstrumentedAuthDataAccessor extends AuthDataAccessor {

    protected static final String SUCCESS_TYPE = ".success";

    protected static final String FAILURE_TYPE = ".failure";

    protected static final String MUST_CHANGE_PASSWORD_TYPE = ".failure";

    protected static final String NOT_FOUND_TYPE = ".notfound";

    protected static final String DISABLED_TYPE = ".disabled";

    protected static final Function<String, String> USERNAME = suffix -> "syncope.auth.username" + suffix + ".count";

    protected static final Function<String, String> JWT = suffix -> "syncope.auth.jwt" + suffix + ".count";

    protected static final Function<String, String> SUCCESS_DESC =
            type -> "The total number of succeeded " + type + " logins";

    protected static final Function<String, String> FAILURE_DESC =
            type -> "The total number of failed " + type + " logins";

    protected static final Function<String, String> MUST_CHANGE_PASSWORD_DESC =
            type -> "The total number of mustChangePassword users attempting to perform " + type + " login";

    protected static final Function<String, String> NOT_FOUND_DESC =
            type -> "The total number of not found users attempting to perform " + type + " login";

    protected static final Function<String, String> DISABLED_DESC =
            type -> "The total number of disabled users attempting to perform " + type + " login";

    protected final MeterRegistry meterRegistry;

    public InstrumentedAuthDataAccessor(
            final SecurityProperties securityProperties,
            final EncryptorManager encryptorManager,
            final RealmSearchDAO realmSearchDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnySearchDAO anySearchDAO,
            final AccessTokenDAO accessTokenDAO,
            final ConfParamOps confParamOps,
            final RoleDAO roleDAO,
            final DelegationDAO delegationDAO,
            final ExternalResourceDAO resourceDAO,
            final ConnectorManager connectorManager,
            final AuditManager auditManager,
            final MappingManager mappingManager,
            final List<JWTSSOProvider> jwtSSOProviders,
            final MeterRegistry meterRegistry) {

        super(securityProperties, encryptorManager, realmSearchDAO, userDAO, groupDAO, anySearchDAO, accessTokenDAO,
                confParamOps, roleDAO, delegationDAO, resourceDAO, connectorManager, auditManager, mappingManager,
                jwtSSOProviders);
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Triple<User, Boolean, String> authenticate(final String domain, final Authentication authentication) {
        try {
            Triple<User, Boolean, String> auth = super.authenticate(domain, authentication);

            Optional.ofNullable(auth.getLeft()).ifPresentOrElse(
                    user -> {
                        Counter.builder(auth.getMiddle() ? USERNAME.apply(SUCCESS_TYPE) : USERNAME.apply(FAILURE_TYPE)).
                                description(auth.getMiddle()
                                        ? SUCCESS_DESC.apply("username") : FAILURE_DESC.apply("username")).
                                tag("realm", user.getRealm().getFullPath()).
                                register(meterRegistry).
                                increment();
                        if (user.isMustChangePassword()) {
                            Counter.builder(USERNAME.apply(MUST_CHANGE_PASSWORD_TYPE)).
                                    description(MUST_CHANGE_PASSWORD_DESC.apply("username")).
                                    register(meterRegistry).
                                    increment();
                        }
                    },
                    () -> Counter.builder(USERNAME.apply(NOT_FOUND_TYPE)).
                            description(NOT_FOUND_DESC.apply("username")).
                            register(meterRegistry).
                            increment());

            return auth;
        } catch (DisabledException e) {
            Counter.builder(USERNAME.apply(DISABLED_TYPE)).
                    description(DISABLED_DESC.apply("JWT")).
                    register(meterRegistry).
                    increment();
            throw e;
        }
    }

    @Override
    public Pair<String, Set<SyncopeGrantedAuthority>> authenticate(final JWTAuthentication authentication) {
        try {
            Pair<String, Set<SyncopeGrantedAuthority>> auth = super.authenticate(authentication);

            if (MUST_CHANGE_PASSWORD_AUTHORITIES.equals(auth.getRight())) {
                Counter.builder(JWT.apply(MUST_CHANGE_PASSWORD_TYPE)).
                        description(MUST_CHANGE_PASSWORD_DESC.apply("JWT")).
                        register(meterRegistry).
                        increment();
            } else {
                Counter.builder(JWT.apply(SUCCESS_TYPE)).
                        description(SUCCESS_DESC.apply("JWT")).
                        register(meterRegistry).
                        increment();
            }

            return auth;
        } catch (AuthenticationCredentialsNotFoundException e) {
            Counter.builder(JWT.apply(NOT_FOUND_TYPE)).
                    description(NOT_FOUND_DESC.apply("JWT")).
                    register(meterRegistry).
                    increment();
            throw e;
        } catch (DisabledException e) {
            Counter.builder(JWT.apply(DISABLED_TYPE)).
                    description(DISABLED_DESC.apply("JWT")).
                    register(meterRegistry).
                    increment();
            throw e;
        }
    }
}
