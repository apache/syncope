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
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdMLogicContext {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private VirAttrHandler virAttrHandler;

    @Autowired
    private ConnInstanceDataBinder connInstanceDataBinder;

    @Autowired
    private ConnectorManager connectorManager;

    @Autowired
    private InboundMatcher inboundMatcher;

    @Autowired
    private OutboundMatcher outboundMatcher;

    @Autowired
    private MappingManager mappingManager;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

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
    @Autowired
    public ConnectorLogic connectorLogic(final ConnIdBundleManager connIdBundleManager) {
        return new ConnectorLogic(
                connIdBundleManager,
                connectorManager,
                resourceDAO,
                connInstanceDAO,
                connInstanceDataBinder);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public ReconciliationLogic reconciliationLogic(
            final RealmDAO realmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final AnySearchDAO anySearchDAO) {

        return new ReconciliationLogic(
                anyUtilsFactory,
                anyTypeDAO,
                resourceDAO,
                realmDAO,
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
    @Autowired
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
    @Autowired
    public ResourceLogic resourceLogic(final ResourceDataBinder resourceDataBinder) {
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
