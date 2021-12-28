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

import org.apache.syncope.core.persistence.api.dao.CamelRouteDAO;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.CamelRouteDataBinder;
import org.apache.syncope.core.provisioning.camel.data.CamelRouteDataBinderImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Configuration(proxyBeanMethods = false)
public class CamelProvisioningContext {

    @Bean
    public Resource userRoutes() {
        return new ClassPathResource("userRoutes.xml");
    }

    @Bean
    public Resource groupRoutes() {
        return new ClassPathResource("groupRoutes.xml");
    }

    @Bean
    public Resource anyObjectRoutes() {
        return new ClassPathResource("anyObjectRoutes.xml");
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeCamelContext syncopeCamelContext(final CamelRouteDAO routeDAO) {
        return new SyncopeCamelContext(routeDAO);
    }

    @Bean
    public UserProvisioningManager userProvisioningManager(
            final CamelRouteDAO routeDAO,
            final SyncopeCamelContext contextFactory) {

        return new CamelUserProvisioningManager(routeDAO, contextFactory);
    }

    @Bean
    public GroupProvisioningManager groupProvisioningManager(
            final CamelRouteDAO routeDAO,
            final SyncopeCamelContext contextFactory) {

        return new CamelGroupProvisioningManager(routeDAO, contextFactory);
    }

    @Bean
    public AnyObjectProvisioningManager anyObjectProvisioningManager(
            final CamelRouteDAO routeDAO,
            final SyncopeCamelContext contextFactory) {

        return new CamelAnyObjectProvisioningManager(routeDAO, contextFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public CamelRouteDataBinder camelRouteDataBinder(final CamelRouteDAO routeDAO) {
        return new CamelRouteDataBinderImpl(routeDAO);
    }
}
