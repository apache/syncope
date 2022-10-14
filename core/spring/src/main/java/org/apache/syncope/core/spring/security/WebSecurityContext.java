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

import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration(proxyBeanMethods = false)
public class WebSecurityContext {

    private static final String ANONYMOUS_BEAN_KEY = "doesNotMatter";

    public WebSecurityContext() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(final HttpFirewall allowUrlEncodedSlashHttpFirewall) {
        return web -> web.httpFirewall(allowUrlEncodedSlashHttpFirewall);
    }

    @Bean
    public SecurityFilterChain filterChain(
            final HttpSecurity http,
            final UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider,
            final JWTAuthenticationProvider jwtAuthenticationProvider,
            final SecurityProperties securityProperties,
            final ApplicationContext ctx) throws Exception {

        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManagerBuilder.class).
                authenticationProvider(usernamePasswordAuthenticationProvider).
                authenticationProvider(jwtAuthenticationProvider).
                build();

        SyncopeAuthenticationDetailsSource authenticationDetailsSource =
                new SyncopeAuthenticationDetailsSource();

        AnonymousAuthenticationProvider anonymousAuthenticationProvider =
                new AnonymousAuthenticationProvider(ANONYMOUS_BEAN_KEY);
        AnonymousAuthenticationFilter anonymousAuthenticationFilter =
                new AnonymousAuthenticationFilter(
                        ANONYMOUS_BEAN_KEY,
                        securityProperties.getAnonymousUser(),
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        anonymousAuthenticationFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

        SyncopeBasicAuthenticationEntryPoint basicAuthenticationEntryPoint =
                new SyncopeBasicAuthenticationEntryPoint();
        basicAuthenticationEntryPoint.setRealmName("Apache Syncope authentication");

        JWTAuthenticationFilter jwtAuthenticationFilter = new JWTAuthenticationFilter(
                authenticationManager,
                basicAuthenticationEntryPoint,
                authenticationDetailsSource,
                ctx.getBean(AuthDataAccessor.class),
                ctx.getBean(DefaultCredentialChecker.class));

        MustChangePasswordFilter mustChangePasswordFilter = new MustChangePasswordFilter();

        http.authenticationManager(authenticationManager).
                authorizeRequests().
                antMatchers("/**").permitAll().and().
                sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().
                securityContext().securityContextRepository(new NullSecurityContextRepository()).and().
                anonymous().
                authenticationProvider(anonymousAuthenticationProvider).
                authenticationFilter(anonymousAuthenticationFilter).and().
                httpBasic().authenticationEntryPoint(basicAuthenticationEntryPoint).
                authenticationDetailsSource(authenticationDetailsSource).and().
                exceptionHandling().accessDeniedHandler(accessDeniedHandler()).and().
                addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter.class).
                addFilterBefore(mustChangePasswordFilter, FilterSecurityInterceptor.class).
                headers().disable().
                csrf().disable();

        return http.build();
    }

    @ConditionalOnMissingBean
    @Bean
    public UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider(
            final SecurityProperties securityProperties,
            final DomainOps domainOps,
            final AuthDataAccessor dataAccessor,
            final UserProvisioningManager provisioningManager,
            final DefaultCredentialChecker credentialChecker) {

        return new UsernamePasswordAuthenticationProvider(
                domainOps,
                dataAccessor,
                provisioningManager,
                credentialChecker,
                securityProperties);
    }

    @ConditionalOnMissingBean
    @Bean
    public JWTAuthenticationProvider jwtAuthenticationProvider(final AuthDataAccessor authDataAccessor) {
        return new JWTAuthenticationProvider(authDataAccessor);
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new SyncopeAccessDeniedHandler();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthDataAccessor authDataAccessor(
            final SecurityProperties securityProperties,
            final RealmDAO realmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnySearchDAO anySearchDAO,
            final AccessTokenDAO accessTokenDAO,
            final ConfParamOps confParamOps,
            final RoleDAO roleDAO,
            final DelegationDAO delegationDAO,
            final ConnectorManager connectorManager,
            final AuditManager auditManager,
            final MappingManager mappingManager,
            final ImplementationLookup implementationLookup) {

        return new AuthDataAccessor(
                securityProperties,
                realmDAO,
                userDAO,
                groupDAO,
                anySearchDAO,
                accessTokenDAO,
                confParamOps,
                roleDAO,
                delegationDAO,
                connectorManager,
                auditManager,
                mappingManager,
                implementationLookup);
    }
}
