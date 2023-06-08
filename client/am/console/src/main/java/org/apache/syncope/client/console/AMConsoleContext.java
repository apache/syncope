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
package org.apache.syncope.client.console;

import org.apache.syncope.client.console.commons.AMAccessPolicyConfProvider;
import org.apache.syncope.client.console.commons.AMPolicyTabProvider;
import org.apache.syncope.client.console.commons.AMRealmPolicyProvider;
import org.apache.syncope.client.console.commons.AccessPolicyConfProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.commons.RealmPolicyProvider;
import org.apache.syncope.client.console.init.AMClassPathScanImplementationContributor;
import org.apache.syncope.client.console.init.ClassPathScanImplementationContributor;
import org.apache.syncope.client.console.rest.AttrRepoRestClient;
import org.apache.syncope.client.console.rest.AuthModuleRestClient;
import org.apache.syncope.client.console.rest.AuthProfileRestClient;
import org.apache.syncope.client.console.rest.ClientAppRestClient;
import org.apache.syncope.client.console.rest.OIDCJWKSRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.rest.SAML2IdPEntityRestClient;
import org.apache.syncope.client.console.rest.SAML2SPEntityRestClient;
import org.apache.syncope.client.console.rest.SRARouteRestClient;
import org.apache.syncope.client.console.rest.SRAStatisticsRestClient;
import org.apache.syncope.client.console.rest.WAConfigRestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AMConsoleContext {

    @Bean
    public ClassPathScanImplementationContributor amClassPathScanImplementationContributor() {
        return new AMClassPathScanImplementationContributor();
    }

    @Bean
    public RealmPolicyProvider realmPolicyProvider(final PolicyRestClient policyRestClient) {
        return new AMRealmPolicyProvider(policyRestClient);
    }

    @Bean
    public PolicyTabProvider amPolicyTabProvider(final PolicyRestClient policyRestClient) {
        return new AMPolicyTabProvider(policyRestClient);
    }

    @Bean
    public AccessPolicyConfProvider accessPolicyConfProvider() {
        return new AMAccessPolicyConfProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public AttrRepoRestClient attrRepoRestClient() {
        return new AttrRepoRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthModuleRestClient authModuleRestClient() {
        return new AuthModuleRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthProfileRestClient authProfileRestClient() {
        return new AuthProfileRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public ClientAppRestClient clientAppRestClient() {
        return new ClientAppRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCJWKSRestClient oidcJWKSRestClient() {
        return new OIDCJWKSRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2IdPEntityRestClient saml2IdPEntityRestClient() {
        return new SAML2IdPEntityRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPEntityRestClient saml2SPEntityRestClient() {
        return new SAML2SPEntityRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public SRARouteRestClient sraRouteRestClient() {
        return new SRARouteRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public SRAStatisticsRestClient sRAStatisticsRestClient() {
        return new SRAStatisticsRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public WAConfigRestClient waConfigRestClient() {
        return new WAConfigRestClient();
    }
}
