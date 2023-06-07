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

import org.apache.syncope.client.enduser.resources.EnduserCodeConsumerResource;
import org.apache.syncope.client.enduser.resources.EnduserLogoutResource;
import org.apache.syncope.client.ui.commons.resources.oidcc4ui.BeforeLogoutResource;
import org.apache.syncope.client.ui.commons.resources.oidcc4ui.LoginResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OIDCC4UIEnduserContext {

    @ConditionalOnMissingBean
    @Bean
    public LoginResource oidcc4uiLoginResource() {
        return new LoginResource();
    }

    @ConditionalOnMissingBean
    @Bean
    public EnduserCodeConsumerResource codeConsumerResource() {
        return new EnduserCodeConsumerResource();
    }

    @ConditionalOnMissingBean
    @Bean
    public BeforeLogoutResource beforeLogoutResource() {
        return new BeforeLogoutResource();
    }

    @ConditionalOnMissingBean
    @Bean
    public EnduserLogoutResource oidcc4uiLogoutResource() {
        return new EnduserLogoutResource();
    }
}
