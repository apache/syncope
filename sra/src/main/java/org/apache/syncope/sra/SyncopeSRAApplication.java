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

import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStart;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.apache.syncope.sra.actuate.SRASessions;
import org.apache.syncope.sra.actuate.SyncopeCoreHealthIndicator;
import org.apache.syncope.sra.actuate.SyncopeSRAInfoContributor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

@SpringBootApplication(proxyBeanMethods = false)
@EnableConfigurationProperties(SRAProperties.class)
public class SyncopeSRAApplication {

    public static void main(final String[] args) {
        new SpringApplicationBuilder(SyncopeSRAApplication.class).
                properties("spring.config.name:sra").
                build().run(args);
    }

    @ConditionalOnMissingBean
    @Bean
    public RouteProvider routeProvider(final ConfigurableApplicationContext ctx,
                                       final ServiceOps serviceOps,
                                       final SRAProperties props) {
        return new RouteProvider(
                serviceOps,
                ctx,
                props.getAnonymousUser(),
                props.getAnonymousKey(),
                props.isUseGZIPCompression());
    }

    @ConditionalOnMissingBean
    @Bean
    public RouteLocator routes(@Qualifier("routeProvider") final RouteProvider routeProvider) {
        return () -> Flux.fromIterable(routeProvider.fetch()).map(Route.AbstractBuilder::build);
    }

    @ConditionalOnMissingBean
    @Bean
    public SRASessions sraSessionsActuatorEndpoint(final CacheManager cacheManager) {
        return new SRASessions(cacheManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeCoreHealthIndicator syncopeCoreHealthIndicator(final ServiceOps serviceOps,
                                                                 final SRAProperties props) {
        return new SyncopeCoreHealthIndicator(
                serviceOps,
                props.getAnonymousUser(),
                props.getAnonymousKey(),
                props.isUseGZIPCompression());
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeSRAInfoContributor syncopeSRAInfoContributor() {
        return new SyncopeSRAInfoContributor();
    }

    @Bean
    public KeymasterStart keymasterStart() {
        return new KeymasterStart(NetworkService.Type.SRA);
    }

    @Bean
    public KeymasterStop keymasterStop() {
        return new KeymasterStop(NetworkService.Type.SRA);
    }
}
