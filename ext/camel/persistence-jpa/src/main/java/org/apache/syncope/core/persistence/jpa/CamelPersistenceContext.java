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
package org.apache.syncope.core.persistence.jpa;

import org.apache.syncope.core.persistence.api.dao.CamelRouteDAO;
import org.apache.syncope.core.persistence.api.entity.CamelEntityFactory;
import org.apache.syncope.core.persistence.jpa.dao.JPACamelRouteDAO;
import org.apache.syncope.core.persistence.jpa.entity.JPACamelEntityFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CamelPersistenceContext {

    @ConditionalOnMissingBean
    @Bean
    public CamelEntityFactory camelEntityFactory() {
        return new JPACamelEntityFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    public CamelRouteDAO camelRouteDAO() {
        return new JPACamelRouteDAO();
    }
}
