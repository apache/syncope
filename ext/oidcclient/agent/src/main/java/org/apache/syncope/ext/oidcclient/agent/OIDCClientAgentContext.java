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
package org.apache.syncope.ext.oidcclient.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:oidcclient-agent.properties")
@PropertySource(value = "file:${conf.directory}/oidcclient-agent.properties", ignoreResourceNotFound = true)
@Configuration
public class OIDCClientAgentContext {

    @Value("${anonymousUser}")
    private String anonymousUser;

    @Value("${anonymousKey}")
    private String anonymousKey;

    @Value("${useGZIPCompression}")
    private boolean useGZIPCompression;

    @Autowired
    private ApplicationContext ctx;

    @Bean
    public ServletRegistrationBean<Login> oidcClientLogin() {
        return new ServletRegistrationBean<>(
                new Login(ctx, anonymousUser, anonymousKey, useGZIPCompression), "/oidcclient/login");
    }

    @Bean
    public ServletRegistrationBean<CodeConsumer> oidcClientCodeConsumer() {
        return new ServletRegistrationBean<>(
                new CodeConsumer(ctx, anonymousUser, anonymousKey, useGZIPCompression), "/oidcclient/code-consumer");
    }

    @Bean
    public ServletRegistrationBean<BeforeLogout> oidcClientBeforeLogout() {
        return new ServletRegistrationBean<>(new BeforeLogout(ctx, useGZIPCompression), "/oidcclient/beforelogout");
    }

    @Bean
    public ServletRegistrationBean<Logout> oidcClientLogout() {
        return new ServletRegistrationBean<>(new Logout(), "/oidcclient/logout");
    }
}
