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

import java.util.List;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Configuration(proxyBeanMethods = false)
public class WebSecurityContext {

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
            final AccessDeniedHandler accessDeniedHandler,
            final AuthDataAccessor dataAccessor,
            final DefaultCredentialChecker defaultCredentialChecker) throws Exception {

        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManagerBuilder.class).
                parentAuthenticationManager(null).
                authenticationProvider(usernamePasswordAuthenticationProvider).
                build();
        http.authenticationManager(authenticationManager);

        SyncopeAuthenticationDetailsSource authenticationDetailsSource =
                new SyncopeAuthenticationDetailsSource();

        SyncopeBasicAuthenticationEntryPoint basicAuthenticationEntryPoint =
                new SyncopeBasicAuthenticationEntryPoint();
        basicAuthenticationEntryPoint.setRealmName("Apache Syncope authentication");
        http.httpBasic(customizer -> customizer.
                authenticationEntryPoint(basicAuthenticationEntryPoint).
                authenticationDetailsSource(authenticationDetailsSource));

        JWTAuthenticationFilter jwtAuthenticationFilter = new JWTAuthenticationFilter(
                authenticationManager,
                basicAuthenticationEntryPoint,
                authenticationDetailsSource,
                dataAccessor,
                defaultCredentialChecker);
        http.addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter.class);

        MustChangePasswordFilter mustChangePasswordFilter = new MustChangePasswordFilter();
        http.addFilterBefore(mustChangePasswordFilter, AuthorizationFilter.class);

        http.authorizeHttpRequests(customizer -> customizer.
                requestMatchers(AntPathRequestMatcher.antMatcher("/actuator/**")).
                hasAuthority(IdRepoEntitlement.ANONYMOUS).
                requestMatchers(AntPathRequestMatcher.antMatcher("/**")).permitAll());
        http.securityContext(AbstractHttpConfigurer::disable);
        http.sessionManagement(AbstractHttpConfigurer::disable);
        http.headers(AbstractHttpConfigurer::disable);
        http.csrf(AbstractHttpConfigurer::disable);
        http.exceptionHandling(customizer -> customizer.accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    @ConditionalOnMissingBean
    @Bean
    public UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider(
            final SecurityProperties securityProperties,
            final EncryptorManager encryptorManager,
            final DomainOps domainOps,
            final AuthDataAccessor dataAccessor,
            final UserProvisioningManager provisioningManager,
            final DefaultCredentialChecker credentialChecker) {

        return new UsernamePasswordAuthenticationProvider(
                domainOps,
                dataAccessor,
                provisioningManager,
                credentialChecker,
                securityProperties,
                encryptorManager);
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new SyncopeAccessDeniedHandler();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthDataAccessor authDataAccessor(
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
            final List<JWTSSOProvider> jwtSSOProviders) {

        return new AuthDataAccessor(
                securityProperties,
                encryptorManager,
                realmSearchDAO,
                userDAO,
                groupDAO,
                anySearchDAO,
                accessTokenDAO,
                confParamOps,
                roleDAO,
                delegationDAO,
                resourceDAO,
                connectorManager,
                auditManager,
                mappingManager,
                jwtSSOProviders);
    }
}
