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
package org.apache.syncope.common.keymaster.client.self;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import java.util.List;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:keymaster.properties")
@PropertySource(value = "file:${conf.directory}/keymaster.properties", ignoreResourceNotFound = true)
@Configuration
public class SelfKeymasterClientContext {

    @Value("${keymaster.address}")
    private String address;

    @Value("${keymaster.username}")
    private String username;

    @Value("${keymaster.password}")
    private String password;

    @ConditionalOnExpression("#{'${keymaster.address}' matches '^http.+'}")
    @Bean
    @ConditionalOnMissingBean(name = "selfKeymasterRESTClientFactoryBean")
    public JAXRSClientFactoryBean selfKeymasterRESTClientFactoryBean() {
        JAXRSClientFactoryBean restClientFactoryBean = new JAXRSClientFactoryBean();
        restClientFactoryBean.setAddress(address);
        restClientFactoryBean.setUsername(username);
        restClientFactoryBean.setPassword(password);
        restClientFactoryBean.setThreadSafe(true);
        restClientFactoryBean.setInheritHeaders(true);
        restClientFactoryBean.setFeatures(List.of(new LoggingFeature()));
        restClientFactoryBean.setProviders(
            List.of(new JacksonJsonProvider(), new SelfKeymasterClientExceptionMapper()));
        return restClientFactoryBean;
    }

    @ConditionalOnExpression("#{'${keymaster.address}' matches '^http.+'}")
    @Bean
    @ConditionalOnMissingBean(name = "selfConfParamOps")
    public ConfParamOps selfConfParamOps() {
        return new SelfKeymasterConfParamOps(selfKeymasterRESTClientFactoryBean());
    }

    @ConditionalOnExpression("#{'${keymaster.address}' matches '^http.+'}")
    @Bean
    @ConditionalOnMissingBean(name = "selfServiceOps")
    public ServiceOps selfServiceOps() {
        return new SelfKeymasterServiceOps(selfKeymasterRESTClientFactoryBean(), 5);
    }

    @ConditionalOnExpression("#{'${keymaster.address}' matches '^http.+'}")
    @Bean
    @ConditionalOnMissingBean(name = "domainOps")
    public DomainOps domainOps() {
        return new SelfKeymasterDomainOps(selfKeymasterRESTClientFactoryBean());
    }
}

