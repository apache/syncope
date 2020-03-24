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

import javax.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityContext extends WebSecurityConfigurerAdapter {

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    public WebSecurityContext() {
        super(true);
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Override
    public void configure(final WebSecurity web) {
        web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
    }

    @ConditionalOnMissingBean
    @Bean
    public UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider() {
        return new UsernamePasswordAuthenticationProvider();
    }

    @Bean
    public JWTAuthenticationProvider jwtAuthenticationProvider() {
        return new JWTAuthenticationProvider();
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder builder) throws Exception {
        builder.
                authenticationProvider(usernamePasswordAuthenticationProvider()).
                authenticationProvider(jwtAuthenticationProvider());
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new NullSecurityContextRepository();
    }

    @Bean
    public SecurityContextPersistenceFilter securityContextPersistenceFilter() {
        return new SecurityContextPersistenceFilter(securityContextRepository());
    }

    @Bean
    public AuthenticationEntryPoint basicAuthenticationEntryPoint() {
        SyncopeBasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new SyncopeBasicAuthenticationEntryPoint();
        basicAuthenticationEntryPoint.setRealmName("Apache Syncope authentication");
        return basicAuthenticationEntryPoint;
    }

    @Bean
    public SyncopeAuthenticationDetailsSource authenticationDetailsSource() {
        return new SyncopeAuthenticationDetailsSource();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new SyncopeAccessDeniedHandler();
    }

    @Bean
    public JWTAuthenticationFilter jwtAuthenticationFilter() throws Exception {
        return new JWTAuthenticationFilter(authenticationManager());
    }

    @Bean
    public MustChangePasswordFilter mustChangePasswordFilter() {
        return new MustChangePasswordFilter();
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http.authorizeRequests().
                antMatchers("/**").permitAll().and().
                sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().
                securityContext().securityContextRepository(securityContextRepository()).and().
                anonymous().principal(anonymousUser).and().
                httpBasic().authenticationEntryPoint(basicAuthenticationEntryPoint()).
                authenticationDetailsSource(authenticationDetailsSource()).and().
                exceptionHandling().accessDeniedHandler(accessDeniedHandler()).and().
                addFilterBefore(jwtAuthenticationFilter(), BasicAuthenticationFilter.class).
                addFilterBefore(mustChangePasswordFilter(), FilterSecurityInterceptor.class).
                headers().disable().
                csrf().disable();
    }

    @Bean
    public AuthDataAccessor authDataAccessor() {
        return new AuthDataAccessor();
    }
}
