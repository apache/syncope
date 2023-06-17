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

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterProperties;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@EnableConfigurationProperties(KeymasterProperties.class)
@Configuration(proxyBeanMethods = false)
public class SelfKeymasterClientContext {

    private static final Pattern HTTP = Pattern.compile("^http.+");

    static class SelfKeymasterCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            String keymasterAddress = context.getEnvironment().getProperty("keymaster.address");
            return new ConditionOutcome(
                    keymasterAddress != null && HTTP.matcher(keymasterAddress).matches(),
                    "Keymaster address not set for Self: " + keymasterAddress);
        }
    }

    @Conditional(SelfKeymasterCondition.class)
    @Bean
    @ConditionalOnMissingBean(name = "selfKeymasterRESTClientFactoryBean")
    public JAXRSClientFactoryBean selfKeymasterRESTClientFactoryBean(final KeymasterProperties props) {
        JAXRSClientFactoryBean restClientFactoryBean = new JAXRSClientFactoryBean();
        restClientFactoryBean.setAddress(props.getAddress());
        restClientFactoryBean.setUsername(props.getUsername());
        restClientFactoryBean.setPassword(props.getPassword());
        restClientFactoryBean.setThreadSafe(true);
        restClientFactoryBean.setInheritHeaders(true);
        restClientFactoryBean.getFeatures().add(new LoggingFeature());
        restClientFactoryBean.setProviders(List.of(
                new JacksonJsonProvider(JsonMapper.builder().findAndAddModules().build()),
                new SelfKeymasterClientExceptionMapper()));
        return restClientFactoryBean;
    }

    @Conditional(SelfKeymasterCondition.class)
    @Bean
    @ConditionalOnMissingBean(name = "selfConfParamOps")
    public ConfParamOps selfConfParamOps(@Qualifier("selfKeymasterRESTClientFactoryBean")
            final JAXRSClientFactoryBean selfKeymasterRESTClientFactoryBean) {
        return new SelfKeymasterConfParamOps(selfKeymasterRESTClientFactoryBean);
    }

    @Conditional(SelfKeymasterCondition.class)
    @Bean
    @ConditionalOnMissingBean(name = "selfServiceOps")
    public ServiceOps selfServiceOps(@Qualifier("selfKeymasterRESTClientFactoryBean")
            final JAXRSClientFactoryBean selfKeymasterRESTClientFactoryBean) {
        return new SelfKeymasterServiceOps(selfKeymasterRESTClientFactoryBean, 5);
    }

    @Conditional(SelfKeymasterCondition.class)
    @Bean
    @ConditionalOnMissingBean(name = "domainOps")
    public DomainOps domainOps(@Qualifier("selfKeymasterRESTClientFactoryBean")
            final JAXRSClientFactoryBean selfKeymasterRESTClientFactoryBean) {
        return new SelfKeymasterDomainOps(selfKeymasterRESTClientFactoryBean);
    }
}
