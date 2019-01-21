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

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SAML2SPAgentContext {

    @Bean
    public ServletListenerRegistrationBean<SAML2SPAgentSetup> saml2SPAgentSetup() {
        return new ServletListenerRegistrationBean<>(new SAML2SPAgentSetup());
    }

    @Bean
    public ServletRegistrationBean<Metadata> saml2SPMetadata() {
        return new ServletRegistrationBean<>(new Metadata(), "/saml2sp/metadata");
    }

    @Bean
    public ServletRegistrationBean<Login> saml2SPLogin() {
        return new ServletRegistrationBean<>(new Login(), "/saml2sp/login");
    }

    @Bean
    public ServletRegistrationBean<AssertionConsumer> saml2SPAssertionConsumer() {
        return new ServletRegistrationBean<>(new AssertionConsumer(), "/saml2sp/assertion-consumer");
    }

    @Bean
    public ServletRegistrationBean<Logout> saml2SPLogout() {
        return new ServletRegistrationBean<>(new Logout(), "/saml2sp/logout");
    }
}
