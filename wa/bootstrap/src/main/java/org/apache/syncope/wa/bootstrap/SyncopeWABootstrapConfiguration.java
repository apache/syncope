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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:wa.properties")
@PropertySource(value = "file:${conf.directory}/wa.properties", ignoreResourceNotFound = true)
public class SyncopeWABootstrapConfiguration {

    @Configuration(proxyBeanMethods = false)
    public static class WAClientConfiguration {
        @Value("${wa.anonymousUser}")
        private String anonymousUser;

        @Value("${wa.anonymousKey}")
        private String anonymousKey;

        @Value("${wa.useGZIPCompression:true}")
        private boolean useGZIPCompression;

        @Bean
        public WARestClient waRestClient() {
            return new WARestClient(anonymousUser, anonymousKey, useGZIPCompression);
        }
    }

    @Configuration(proxyBeanMethods = false)
    public static class PropertySourceConfiguration {
        @Bean
        public PropertySourceLocator configPropertySourceLocator(final WARestClient waRestClient) {
            return new SyncopeWAPropertySourceLocator(waRestClient);
        }
    }
}
