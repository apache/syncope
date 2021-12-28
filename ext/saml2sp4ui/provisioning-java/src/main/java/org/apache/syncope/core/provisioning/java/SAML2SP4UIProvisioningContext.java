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
package org.apache.syncope.core.provisioning.java;

import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIEntityFactory;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.data.SAML2SP4UIIdPDataBinder;
import org.apache.syncope.core.provisioning.java.data.SAML2SP4UIIdPDataBinderImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SAML2SP4UIProvisioningContext {

    @ConditionalOnMissingBean
    @Bean
    public SAML2SP4UIIdPDataBinder saml2SP4UIIdPDataBinder(
            final AnyTypeDAO anyTypeDAO,
            final SAML2SP4UIIdPDAO idapDAO,
            final ImplementationDAO implementationDAO,
            final SAML2SP4UIEntityFactory entityFactory,
            final IntAttrNameParser intAttrNameParser) {

        return new SAML2SP4UIIdPDataBinderImpl(
                anyTypeDAO,
                idapDAO,
                implementationDAO,
                entityFactory,
                intAttrNameParser);
    }
}
