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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    public WebSecurityConfigurerAdapter enduserSecurityAdapter(final EnduserProperties props) {
        return new WebSecurityConfigurerAdapter() {
            @Override
            protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
                auth.inMemoryAuthentication().
                    withUser(props.getAnonymousUser()).
                    password("{noop}" + props.getAnonymousKey()).
                    roles(IdRepoEntitlement.ANONYMOUS);
            }

            @Override
            protected void configure(final HttpSecurity http) throws Exception {
                http.csrf().disable().
                    authorizeRequests().
                    requestMatchers(EndpointRequest.toAnyEndpoint()).
                    authenticated().
                    and().
                    httpBasic();
            }
        };
    }
}
