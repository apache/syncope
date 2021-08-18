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
package org.apache.syncope.core.starter;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.DomainWatcher;
import org.apache.syncope.common.keymaster.client.api.KeymasterProperties;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.core.keymaster.internal.SelfKeymasterInternalConfParamOps;
import org.apache.syncope.core.keymaster.internal.SelfKeymasterInternalDomainOps;
import org.apache.syncope.core.keymaster.internal.SelfKeymasterInternalServiceOps;
import org.apache.syncope.core.logic.ConfParamLogic;
import org.apache.syncope.core.logic.DomainLogic;
import org.apache.syncope.core.logic.NetworkServiceLogic;
import org.apache.syncope.core.persistence.api.dao.ConfParamDAO;
import org.apache.syncope.core.persistence.api.dao.DomainDAO;
import org.apache.syncope.core.persistence.api.dao.NetworkServiceDAO;
import org.apache.syncope.core.persistence.api.entity.SelfKeymasterEntityFactory;
import org.apache.syncope.core.rest.cxf.RestServiceExceptionMapper;
import org.apache.syncope.core.rest.security.SelfKeymasterUsernamePasswordAuthenticationProvider;
import org.apache.syncope.core.spring.security.UsernamePasswordAuthenticationProvider;
import org.apache.syncope.core.spring.security.WebSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@EnableConfigurationProperties(KeymasterProperties.class)
@ComponentScan("org.apache.syncope.core.keymaster.rest.cxf.service")
@Configuration
@AutoConfigureBefore(WebSecurityContext.class)
public class SelfKeymasterContext {

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

    @Autowired
    private SelfKeymasterEntityFactory entityFactory;

    @Autowired
    private Bus bus;

    @Autowired
    private ApplicationContext ctx;

    @Conditional(SelfKeymasterCondition.class)
    @Bean
    public Server selfKeymasterContainer(final JacksonJsonProvider jsonProvider) {
        SpringJAXRSServerFactoryBean selfKeymasterContainer = new SpringJAXRSServerFactoryBean();
        selfKeymasterContainer.setBus(bus);
        selfKeymasterContainer.setAddress("/keymaster");
        selfKeymasterContainer.setStaticSubresourceResolution(true);
        selfKeymasterContainer.setBasePackages(List.of(
                "org.apache.syncope.common.keymaster.rest.api.service",
                "org.apache.syncope.core.keymaster.rest.cxf.service"));
        selfKeymasterContainer.setProperties(Map.of("convert.wadl.resources.to.dom", "false"));

        selfKeymasterContainer.setInInterceptors(List.of(
                ctx.getBean(GZIPInInterceptor.class),
                ctx.getBean(JAXRSBeanValidationInInterceptor.class)));

        selfKeymasterContainer.setOutInterceptors(List.of(
                ctx.getBean(GZIPOutInterceptor.class)));

        selfKeymasterContainer.setProviders(List.of(
                ctx.getBean(RestServiceExceptionMapper.class),
                jsonProvider));

        selfKeymasterContainer.setApplicationContext(ctx);
        return selfKeymasterContainer.create();
    }

    @Conditional(SelfKeymasterCondition.class)
    @Bean
    public UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider() {
        return new SelfKeymasterUsernamePasswordAuthenticationProvider();
    }

    @Conditional(SelfKeymasterCondition.class)
    @Bean
    public ConfParamOps internalConfParamOps() {
        return new SelfKeymasterInternalConfParamOps();
    }

    @Conditional(SelfKeymasterCondition.class)
    @Bean
    public ServiceOps internalServiceOps() {
        return new SelfKeymasterInternalServiceOps();
    }

    @Conditional(SelfKeymasterCondition.class)
    @Bean
    public DomainOps domainOps() {
        return new SelfKeymasterInternalDomainOps();
    }

    @Conditional(SelfKeymasterCondition.class)
    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public ConfParamLogic confParamLogic(final ConfParamDAO confParamDAO) {
        return new ConfParamLogic(confParamDAO, entityFactory);
    }

    @Conditional(SelfKeymasterCondition.class)
    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public DomainLogic domainLogic(
            final DomainDAO domainDAO,
            final DomainWatcher domainWatcher) {

        return new DomainLogic(domainDAO, entityFactory, domainWatcher);
    }

    @Conditional(SelfKeymasterCondition.class)
    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public NetworkServiceLogic networkServiceLogic(final NetworkServiceDAO serviceDAO) {
        return new NetworkServiceLogic(serviceDAO, entityFactory);
    }
}
