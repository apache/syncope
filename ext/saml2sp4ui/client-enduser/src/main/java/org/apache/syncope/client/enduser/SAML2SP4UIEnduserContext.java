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
package org.apache.syncope.client.enduser;

import org.apache.syncope.client.enduser.resources.saml2sp4ui.EnduserAssertionConsumerResource;
import org.apache.syncope.client.enduser.resources.saml2sp4ui.EnduserLogoutResource;
import org.apache.syncope.client.ui.commons.resources.saml2sp4ui.LoginResource;
import org.apache.syncope.client.ui.commons.resources.saml2sp4ui.MetadataResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SAML2SP4UIEnduserContext {

    @ConditionalOnMissingBean
    @Bean
    public MetadataResource metadataResource() {
        return new MetadataResource();
    }

    @ConditionalOnMissingBean
    @Bean
    public LoginResource saml2sp4uiLoginResource() {
        return new LoginResource();
    }

    @ConditionalOnMissingBean
    @Bean
    public EnduserAssertionConsumerResource assertionConsumerResource() {
        return new EnduserAssertionConsumerResource();
    }

    @ConditionalOnMissingBean
    @Bean
    public EnduserLogoutResource saml2sp4uiLogoutResource() {
        return new EnduserLogoutResource();
    }
}
