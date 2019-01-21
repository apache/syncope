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

import java.io.IOException;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.DefaultPasswordGenerator;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@PropertySource("classpath:security.properties")
@Import(PersistenceContext.class)
@Configuration
public class PersistenceTestContext {

    @Value("${adminUser}")
    private String adminUser;

    @Value("${anonymousUser}")
    private String anonymousUser;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() throws IOException {
        PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
        pspc.setIgnoreResourceNotFound(true);
        pspc.setIgnoreUnresolvablePlaceholders(true);
        return pspc;
    }

    @Bean
    public String adminUser() {
        return adminUser;
    }

    @Bean
    public String anonymousUser() {
        return anonymousUser;
    }

    @Bean
    public ApplicationContextProvider applicationContextProvider() {
        return new ApplicationContextProvider();
    }

    @Bean
    public PasswordGenerator passwordGenerator() {
        return new DefaultPasswordGenerator();
    }
}
