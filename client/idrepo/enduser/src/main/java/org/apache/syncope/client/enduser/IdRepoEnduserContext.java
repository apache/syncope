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

import org.apache.syncope.client.enduser.commons.PreviewUtils;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.rest.AnyTypeRestClient;
import org.apache.syncope.client.enduser.rest.GroupRestClient;
import org.apache.syncope.client.enduser.rest.SchemaRestClient;
import org.apache.syncope.client.enduser.rest.SecurityQuestionRestClient;
import org.apache.syncope.client.enduser.rest.SyncopeRestClient;
import org.apache.syncope.client.enduser.rest.UserSelfRestClient;
import org.apache.syncope.client.ui.commons.MIMETypesLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdRepoEnduserContext {

    @ConditionalOnMissingBean
    @Bean
    public ClassPathScanImplementationLookup classPathScanImplementationLookup() {
        ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup();
        lookup.load();
        return lookup;
    }

    @ConditionalOnMissingBean
    @Bean
    public MIMETypesLoader mimeTypesLoader() {
        MIMETypesLoader mimeTypesLoader = new MIMETypesLoader();
        mimeTypesLoader.load();
        return mimeTypesLoader;
    }

    @ConditionalOnMissingBean
    @Bean
    public PreviewUtils previewUtils(final ClassPathScanImplementationLookup lookup) {
        return new PreviewUtils(lookup);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeRestClient anyTypeRestClient() {
        return new AnyTypeRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupRestClient groupRestClient() {
        return new GroupRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public SchemaRestClient schemaRestClient() {
        return new SchemaRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionRestClient securityQuestionRestClient() {
        return new SecurityQuestionRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeRestClient syncopeRestClient() {
        return new SyncopeRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public UserSelfRestClient userSelfRestClient() {
        return new UserSelfRestClient();
    }
}
