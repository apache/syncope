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
package org.apache.syncope.core.rest.cxf;

import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.ReconciliationService;
import org.apache.syncope.common.rest.api.service.RemediationService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.core.logic.ConnectorLogic;
import org.apache.syncope.core.logic.ReconciliationLogic;
import org.apache.syncope.core.logic.RemediationLogic;
import org.apache.syncope.core.logic.ResourceLogic;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.rest.cxf.service.ConnectorServiceImpl;
import org.apache.syncope.core.rest.cxf.service.ReconciliationServiceImpl;
import org.apache.syncope.core.rest.cxf.service.RemediationServiceImpl;
import org.apache.syncope.core.rest.cxf.service.ResourceServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdMRESTCXFContext {

    @ConditionalOnMissingBean
    @Bean
    public ConnectorService connectorService(final ConnectorLogic connectorLogic) {
        return new ConnectorServiceImpl(connectorLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReconciliationService reconciliationService(
            final SearchCondVisitor searchCondVisitor,
            final ReconciliationLogic reconciliationLogic) {

        return new ReconciliationServiceImpl(searchCondVisitor, reconciliationLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public RemediationService remediationService(
            final RemediationLogic remediationLogic,
            final AnyUtilsFactory anyUtilsFactory) {

        return new RemediationServiceImpl(remediationLogic, anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ResourceService resourceService(final ResourceLogic resourceLogic) {
        return new ResourceServiceImpl(resourceLogic);
    }
}
