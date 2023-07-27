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
package org.apache.syncope.wa.bootstrap;

import org.apache.syncope.wa.bootstrap.mapping.AttrReleaseMapper;
import org.apache.syncope.wa.bootstrap.mapping.AttrRepoPropertySourceMapper;
import org.apache.syncope.wa.bootstrap.mapping.AuthModulePropertySourceMapper;
import org.apache.syncope.wa.bootstrap.mapping.DefaultAttrReleaseMapper;
import org.apereo.cas.configuration.support.CasConfigurationJasyptCipherExecutor;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:wa.properties")
@PropertySource(value = "file:${syncope.conf.dir}/wa.properties", ignoreResourceNotFound = true)
public class WABootstrapConfiguration {

    @Configuration(proxyBeanMethods = false)
    public static class WAClientConfiguration {

        @Value("${wa.anonymousUser}")
        private String anonymousUser;

        @Value("${wa.anonymousKey}")
        private String anonymousKey;

        @Value("${wa.useGZIPCompression:true}")
        private boolean useGZIPCompression;

        @Value("${service.discovery.address}")
        private String serviceDiscoveryAddress;

        @Bean
        public WARestClient waRestClient() {
            return new WARestClient(anonymousUser, anonymousKey, useGZIPCompression, serviceDiscoveryAddress);
        }
    }

    @Configuration(proxyBeanMethods = false)
    public static class PropertySourceConfiguration {

        @ConditionalOnMissingBean(name = "waConfigurationCipher")
        @Bean
        public CipherExecutor<String, String> waConfigurationCipher(final Environment environment) {
            return new CasConfigurationJasyptCipherExecutor(environment);
        }

        @ConditionalOnMissingBean
        @Bean
        public AuthModulePropertySourceMapper authModulePropertySourceMapper(final WARestClient waRestClient) {
            return new AuthModulePropertySourceMapper(waRestClient);
        }

        @ConditionalOnMissingBean
        @Bean
        public AttrRepoPropertySourceMapper attrRepoPropertySourceMapper(final WARestClient waRestClient) {
            return new AttrRepoPropertySourceMapper(waRestClient);
        }

        @ConditionalOnMissingBean
        @Bean
        public AttrReleaseMapper attrReleaseMapper() {
            return new DefaultAttrReleaseMapper();
        }

        @Bean
        public PropertySourceLocator configPropertySourceLocator(
                @Qualifier("waConfigurationCipher")
                final CipherExecutor<String, String> waConfigurationCipher,
                final WARestClient waRestClient,
                final AuthModulePropertySourceMapper authModulePropertySourceMapper,
                final AttrRepoPropertySourceMapper attrRepoPropertySourceMapper,
                final AttrReleaseMapper attrReleaseMapper) {

            return new WAPropertySourceLocator(
                    waRestClient,
                    authModulePropertySourceMapper,
                    attrRepoPropertySourceMapper,
                    attrReleaseMapper,
                    waConfigurationCipher);
        }
    }
}
