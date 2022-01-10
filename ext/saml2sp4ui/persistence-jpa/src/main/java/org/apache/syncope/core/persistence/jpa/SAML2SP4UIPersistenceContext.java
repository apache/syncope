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

import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIEntityFactory;
import org.apache.syncope.core.persistence.jpa.dao.JPASAML2SP4UIIdPDAO;
import org.apache.syncope.core.persistence.jpa.entity.JPASAML2SP4UIEntityFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SAML2SP4UIPersistenceContext {

    @ConditionalOnMissingBean
    @Bean
    public SAML2SP4UIEntityFactory saml2SP4UIEntityFactory() {
        return new JPASAML2SP4UIEntityFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SP4UIIdPDAO saml2SP4UIIdPDAO() {
        return new JPASAML2SP4UIIdPDAO();
    }
}
