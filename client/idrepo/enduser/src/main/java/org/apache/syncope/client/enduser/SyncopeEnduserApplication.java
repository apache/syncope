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

import com.giffing.wicket.spring.boot.starter.web.config.WicketWebInitializerAutoConfig.WebSocketWicketWebInitializerAutoConfiguration;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.ui.commons.actuate.SyncopeCoreHealthIndicator;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStart;
import org.apache.syncope.common.keymaster.client.api.startstop.KeymasterStop;
import org.apache.wicket.request.resource.IResource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

@SpringBootApplication(proxyBeanMethods = false)
@EnableConfigurationProperties(EnduserProperties.class)
public class SyncopeEnduserApplication extends SpringBootServletInitializer {

    public static void main(final String[] args) {
        new SpringApplicationBuilder(SyncopeEnduserApplication.class).
                properties("spring.config.name:enduser").
                build().run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder builder) {
        return builder.properties(Map.of(
                WebSocketWicketWebInitializerAutoConfiguration.REGISTER_SERVER_ENDPOINT_ENABLED, false,
                "spring.config.name", "enduser")).
                sources(SyncopeEnduserApplication.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeWebApplication syncopeWebApplication(
            final ResourceLoader resourceLoader,
            final EnduserProperties props,
            final ClassPathScanImplementationLookup lookup,
            final ServiceOps serviceOps,
            final List<IResource> resources) {

        return new SyncopeWebApplication(resourceLoader, props, lookup, serviceOps, resources);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeCoreHealthIndicator syncopeCoreHealthIndicator(
            final ServiceOps serviceOps, final EnduserProperties props) {

        return new SyncopeCoreHealthIndicator(
                serviceOps,
                props.getAnonymousUser(),
                props.getAnonymousKey(),
                props.isUseGZIPCompression());
    }

    @ConditionalOnProperty(
            prefix = "keymaster", name = "enableAutoRegistration", havingValue = "true", matchIfMissing = true)
    @Bean
    public KeymasterStart keymasterStart() {
        return new KeymasterStart(NetworkService.Type.ENDUSER);
    }

    @ConditionalOnProperty(
            prefix = "keymaster", name = "enableAutoRegistration", havingValue = "true", matchIfMissing = true)
    @Bean
    public KeymasterStop keymasterStop() {
        return new KeymasterStop(NetworkService.Type.ENDUSER);
    }
}
