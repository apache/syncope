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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;

public class SinglePurposeEntitlementFilter implements Filter {

    protected record Setting(String entitlement, String httpMethod, String httpPathInfo, String errorMessage) {

    }

    protected static final List<Setting> SETTINGS = List.of(
            new Setting(IdRepoEntitlement.MUST_CHANGE_PASSWORD,
                    HttpMethod.POST, "/users/self/mustChangePassword",
                    "Please change your password first"),
            new Setting(IdRepoEntitlement.MFA_ENROLL,
                    HttpMethod.PUT, "/mfa",
                    "Please enroll your MFA first"));

    @Override
    public void init(final FilterConfig filterConfig) {
        // not used
    }

    @Override
    public void destroy() {
        // not used
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof SecurityContextHolderAwareRequestWrapper wrappedRequest) {
            Optional<Setting> match = SETTINGS.stream().
                    filter(setting -> SecurityContextHolder.getContext().getAuthentication().getAuthorities().
                    stream().anyMatch(granted -> setting.entitlement().equals(granted.getAuthority()))).
                    findFirst();

            match.filter(setting -> !setting.httpMethod().equalsIgnoreCase(wrappedRequest.getMethod())
                    || !setting.httpPathInfo().equals(wrappedRequest.getPathInfo())).
                    ifPresent(setting -> {
                        throw new AccessDeniedException(
                                setting.errorMessage(),
                                new SingleEntitlementException(setting.entitlement()));
                    });
        }

        chain.doFilter(request, response);
    }
}
