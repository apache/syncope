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
package org.apache.syncope.core.logic;

import org.apache.syncope.core.logic.init.CamelRouteLoader;
import org.apache.syncope.core.persistence.api.dao.CamelRouteDAO;
import org.apache.syncope.core.provisioning.api.data.CamelRouteDataBinder;
import org.apache.syncope.core.provisioning.camel.SyncopeCamelContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration(proxyBeanMethods = false)
public class CamelLogicContext {

    @javax.annotation.Resource(name = "userRoutes")
    private Resource userRoutes;

    @javax.annotation.Resource(name = "groupRoutes")
    private Resource groupRoutes;

    @javax.annotation.Resource(name = "anyObjectRoutes")
    private Resource anyObjectRoutes;

    @ConditionalOnMissingBean
    @Bean
    public CamelRouteLoader camelRouteLoader() {
        return new CamelRouteLoader(userRoutes, groupRoutes, anyObjectRoutes);
    }

    @ConditionalOnMissingBean
    @Bean
    public CamelRouteLogic camelRouteLogic(
            final CamelRouteDAO routeDAO,
            final CamelRouteDataBinder binder,
            final SyncopeCamelContext context) {

        return new CamelRouteLogic(routeDAO, binder, context);
    }
}
