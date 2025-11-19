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
package org.apache.syncope.core.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.spring.security.DefaultEncryptorManager;
import org.apache.syncope.core.spring.security.DummyImplementationLookup;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.jenkinsci.plugins.scriptsecurity.sandbox.blacklists.Blacklist;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;

@EnableAspectJAutoProxy(proxyTargetClass = false)
@Configuration(proxyBeanMethods = false)
public class SpringTestConfiguration {

    public static final String AES_SECRET_KEY = "1abcdefghilmnopq";

    @Bean
    public ApplicationContextProvider applicationContextProvider() {
        return new ApplicationContextProvider();
    }

    @Bean
    public EncryptorManager encryptorManager() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setAesSecretKey(AES_SECRET_KEY);
        return new DefaultEncryptorManager(securityProperties);
    }

    @Primary
    @Bean
    public ImplementationLookup implementationLookup() {
        return new DummyImplementationLookup();
    }

    @Bean
    public Blacklist groovyBlackList() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/META-INF/groovy.blacklist");
                Reader reader = new InputStreamReader(is);) {

            return new Blacklist(reader);
        }
    }
}
