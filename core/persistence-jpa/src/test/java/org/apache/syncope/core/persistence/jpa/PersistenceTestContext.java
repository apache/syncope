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

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.core.spring.security.DefaultPasswordGenerator;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

@Import(PersistenceContext.class)
@Configuration
public class PersistenceTestContext {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();

        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

        List<Resource> locations = new ArrayList<>();
        for (String location : System.getProperty("CORE_PROPERTIES").split(",")) {
            locations.add(resourceLoader.getResource(location));
        }
        ppc.setLocations(locations.toArray(new Resource[0]));

        return ppc;
    }

    @Value("${security.adminUser}")
    private String adminUser;

    @Value("${security.anonymousUser}")
    private String anonymousUser;

    @Bean
    public String adminUser() {
        return adminUser;
    }

    @Bean
    public String anonymousUser() {
        return anonymousUser;
    }

    @Bean
    public SecurityProperties securityProperties() {
        return new SecurityProperties();
    }

    @Bean
    public PasswordGenerator passwordGenerator() {
        return new DefaultPasswordGenerator();
    }
}
