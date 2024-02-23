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

import org.apache.syncope.core.logic.init.IdMEntitlementLoader;
import org.apache.syncope.core.logic.init.IdMImplementationTypeLoader;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.provisioning.api.data.RemediationDataBinder;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdMLogicContext {

    @ConditionalOnMissingBean
    @Bean
    public IdMEntitlementLoader idmEntitlementLoader() {
        return new IdMEntitlementLoader();
    }

    @ConditionalOnMissingBean
    @Bean
    public IdMImplementationTypeLoader idmImplementationTypeLoader() {
        return new IdMImplementationTypeLoader();
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnectorLogic connectorLogic(
            final ConnIdBundleManager connIdBundleManager,
            final ExternalResourceDAO resourceDAO,
            final ConnInstanceDAO connInstanceDAO,
            final ConnInstanceDataBinder connInstanceDataBinder,
            final ConnectorManager connectorManager) {
        return new ConnectorLogic(
                connIdBundleManager,
                connectorManager,
                resourceDAO,
                connInstanceDAO,
                connInstanceDataBinder);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReconciliationLogic reconciliationLogic(
            final AnyUtilsFactory anyUtilsFactory,
            final AnyTypeDAO anyTypeDAO,
            final ExternalResourceDAO resourceDAO,
            final RealmSearchDAO realmSearchDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final AnySearchDAO anySearchDAO,
            final VirAttrHandler virAttrHandler,
            final ConnectorManager connectorManager,
            final InboundMatcher inboundMatcher,
            final OutboundMatcher outboundMatcher,
            final MappingManager mappingManager) {

        return new ReconciliationLogic(
                anyUtilsFactory,
                anyTypeDAO,
                resourceDAO,
                realmSearchDAO,
                plainSchemaDAO,
                derSchemaDAO,
                virSchemaDAO,
                anySearchDAO,
                virAttrHandler,
                mappingManager,
                inboundMatcher,
                outboundMatcher,
                connectorManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public RemediationLogic remediationLogic(
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final AnyObjectLogic anyObjectLogic,
            final RemediationDataBinder binder,
            final RemediationDAO remediationDAO) {

        return new RemediationLogic(userLogic, groupLogic, anyObjectLogic, binder, remediationDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public ResourceLogic resourceLogic(
            final ResourceDataBinder resourceDataBinder,
            final AnyUtilsFactory anyUtilsFactory,
            final AnyTypeDAO anyTypeDAO,
            final ExternalResourceDAO resourceDAO,
            final ConnInstanceDAO connInstanceDAO,
            final VirSchemaDAO virSchemaDAO,
            final VirAttrHandler virAttrHandler,
            final ConnInstanceDataBinder connInstanceDataBinder,
            final ConnectorManager connectorManager,
            final OutboundMatcher outboundMatcher,
            final MappingManager mappingManager) {

        return new ResourceLogic(
                resourceDAO,
                anyTypeDAO,
                connInstanceDAO,
                virSchemaDAO,
                virAttrHandler,
                resourceDataBinder,
                connInstanceDataBinder,
                outboundMatcher,
                mappingManager,
                connectorManager,
                anyUtilsFactory);
    }
}
