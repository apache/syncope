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

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;

public class MustChangePasswordFilter implements Filter {

    private static final String[] ALLOWED = new String[] {
        "/users/self", "/users/self/changePassword"
    };

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // not used
    }

    @Override
    public void destroy() {
        // not used
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof SecurityContextHolderAwareRequestWrapper) {
            boolean isMustChangePassword = IterableUtils.matchesAny(
                    SecurityContextHolder.getContext().getAuthentication().getAuthorities(),
                    new Predicate<GrantedAuthority>() {

                @Override
                public boolean evaluate(final GrantedAuthority authority) {
                    return StandardEntitlement.MUST_CHANGE_PASSWORD.equals(authority.getAuthority());
                }
            });

            SecurityContextHolderAwareRequestWrapper wrapper =
                    SecurityContextHolderAwareRequestWrapper.class.cast(request);
            if (isMustChangePassword && "GET".equalsIgnoreCase(wrapper.getMethod())
                    && !ArrayUtils.contains(ALLOWED, wrapper.getPathInfo())) {

                throw new AccessDeniedException("Please change your password first");
            }
        }

        chain.doFilter(request, response);
    }

}
