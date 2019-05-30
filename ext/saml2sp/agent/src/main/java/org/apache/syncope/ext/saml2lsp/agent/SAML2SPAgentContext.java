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
package org.apache.syncope.ext.saml2lsp.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:saml2sp-agent.properties")
@PropertySource(value = "file:${conf.directory}/saml2sp-agent.properties", ignoreResourceNotFound = true)
@Configuration
public class SAML2SPAgentContext {

    @Value("${anonymousUser}")
    private String anonymousUser;

    @Value("${anonymousKey}")
    private String anonymousKey;

    @Value("${useGZIPCompression}")
    private boolean useGZIPCompression;

    @Autowired
    private ApplicationContext ctx;

    @Bean
    public ServletRegistrationBean<Metadata> saml2SPMetadata() {
        return new ServletRegistrationBean<>(
                new Metadata(ctx, anonymousUser, anonymousKey, useGZIPCompression), "/saml2sp/metadata");
    }

    @Bean
    public ServletRegistrationBean<Login> saml2SPLogin() {
        ServletRegistrationBean<Login> bean = new ServletRegistrationBean<>(
                new Login(ctx, anonymousUser, anonymousKey, useGZIPCompression), "/saml2sp/login");
        bean.setName("saml2SPLogin");
        return bean;
    }

    @Bean
    public ServletRegistrationBean<AssertionConsumer> saml2SPAssertionConsumer() {
        ServletRegistrationBean<AssertionConsumer> bean = new ServletRegistrationBean<>(
                new AssertionConsumer(ctx, anonymousUser, anonymousKey, useGZIPCompression),
                "/saml2sp/assertion-consumer");
        bean.setName("saml2SPAssertionConsumer");
        return bean;
    }

    @Bean
    public ServletRegistrationBean<Logout> saml2SPLogout() {
        ServletRegistrationBean<Logout> bean =
                new ServletRegistrationBean<>(new Logout(ctx, useGZIPCompression), "/saml2sp/logout");
        bean.setName("saml2SPLogout");
        return bean;
    }
}
