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

import org.apache.syncope.common.rest.api.service.SAML2SP4UIIdPService;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIService;
import org.apache.syncope.core.logic.SAML2SP4UIIdPLogic;
import org.apache.syncope.core.logic.SAML2SP4UILogic;
import org.apache.syncope.core.rest.cxf.service.SAML2SP4UIIdPServiceImpl;
import org.apache.syncope.core.rest.cxf.service.SAML2SP4UIServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SAML2SP4UIRESTCXFContext {

    @ConditionalOnMissingBean
    @Bean
    public SAML2SP4UIService saml2SP4UIService(final SAML2SP4UILogic saml2SP4UIService) {
        return new SAML2SP4UIServiceImpl(saml2SP4UIService);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SP4UIIdPService saml2SP4UIIdPService(final SAML2SP4UIIdPLogic saml2SP4UIIdPLogic) {
        return new SAML2SP4UIIdPServiceImpl(saml2SP4UIIdPLogic);
    }
}
