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
package org.apache.syncope.core.provisioning.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.syncope.core.spring.ResourceWithFallbackLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan("org.apache.syncope.core.provisioning.camel")
@Configuration
public class ProvisioningCamelContext extends CamelConfiguration {

    @Value("${camel.directory}")
    private String camelDirectory;

    @Override
    protected void setupCamelContext(final CamelContext camelContext) throws Exception {
        camelContext.setStreamCaching(false);
        camelContext.setAllowUseOriginalMessage(false);
    }

    @Bean
    public ResourceWithFallbackLoader userRoutes() {
        ResourceWithFallbackLoader routes = new ResourceWithFallbackLoader();
        routes.setPrimary("file:" + camelDirectory + "/userRoutes.xml");
        routes.setFallback("classpath:userRoutes.xml");
        return routes;
    }

    @Bean
    public ResourceWithFallbackLoader groupRoutes() {
        ResourceWithFallbackLoader routes = new ResourceWithFallbackLoader();
        routes.setPrimary("file:" + camelDirectory + "/groupRoutes.xml");
        routes.setFallback("classpath:groupRoutes.xml");
        return routes;
    }

    @Bean
    public ResourceWithFallbackLoader anyObjectRoutes() {
        ResourceWithFallbackLoader routes = new ResourceWithFallbackLoader();
        routes.setPrimary("file:" + camelDirectory + "/anyObjectRoutes.xml");
        routes.setFallback("classpath:anyObjectRoutes.xml");
        return routes;
    }
}
