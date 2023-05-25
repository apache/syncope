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
package org.apache.syncope.client.enduser;

import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @ConditionalOnMissingBean
    @Bean
    public SecurityFilterChain actuatorFilterChain(final HttpSecurity http) throws Exception {
        EndpointRequest.EndpointRequestMatcher actuatorEndpoints = EndpointRequest.toAnyEndpoint();
        http.authorizeHttpRequests(customizer -> customizer.
                requestMatchers(new NegatedRequestMatcher(actuatorEndpoints)).permitAll().
                requestMatchers(actuatorEndpoints).authenticated());

        http.httpBasic(Customizer.withDefaults());
        http.csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @ConditionalOnMissingBean
    @Bean
    public UserDetailsService actuatorUserDetailsService(final EnduserProperties props) {
        UserDetails user = User.withUsername(props.getAnonymousUser()).
                password("{noop}" + props.getAnonymousKey()).
                roles(IdRepoEntitlement.ANONYMOUS).
                build();
        return new InMemoryUserDetailsManager(user);
    }
}
