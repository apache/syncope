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
package org.apache.syncope.sra;

import java.util.Objects;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStart;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import reactor.core.publisher.Flux;

@PropertySource("classpath:sra.properties")
@PropertySource(value = "file:${conf.directory}/sra.properties", ignoreResourceNotFound = true)
@EnableWebFluxSecurity
@SpringBootApplication
public class SyncopeSRAApplication implements EnvironmentAware {

    public static void main(final String[] args) {
        SpringApplication.run(SyncopeSRAApplication.class, args);
    }

    @Autowired
    private RouteProvider provider;

    private Environment env;

    @Override
    public void setEnvironment(final Environment env) {
        this.env = env;
    }

    @Bean
    public KeymasterStart keymasterStart() {
        return new KeymasterStart(NetworkService.Type.SRA);
    }

    @Bean
    public KeymasterStop keymasterStop() {
        return new KeymasterStop(NetworkService.Type.SRA);
    }

    @Bean
    public RouteLocator routes(final RouteLocatorBuilder builder) {
        return () -> Flux.fromIterable(provider.fetch()).map(Route.AbstractBuilder::build);
    }

    @Bean
    public SecurityWebFilterChain actuatorSecurityFilterChain(final ServerHttpSecurity http) {
        ServerWebExchangeMatcher actuatorMatcher = EndpointRequest.toAnyEndpoint();
        return http.securityMatcher(actuatorMatcher).
                authorizeExchange().anyExchange().authenticated().
                and().httpBasic().
                and().csrf().requireCsrfProtectionMatcher(new NegatedServerWebExchangeMatcher(actuatorMatcher)).
                and().build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.builder().
                username(Objects.requireNonNull(env.getProperty("anonymousUser"))).
                password("{noop}" + env.getProperty("anonymousKey")).
                roles(IdRepoEntitlement.ANONYMOUS).
                build();
        return new MapReactiveUserDetailsService(user);
    }
}
