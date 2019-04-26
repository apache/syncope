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
package org.apache.syncope.ext.self.keymaster.cxf;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.util.Arrays;
import java.util.Collections;
import javax.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationOutInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.core.spring.security.UsernamePasswordAuthenticationProvider;
import org.apache.syncope.core.spring.security.WebSecurityContext;
import org.apache.syncope.ext.self.keymaster.cxf.client.SelfKeymasterInternalConfParamOps;
import org.apache.syncope.ext.self.keymaster.cxf.security.SelfKeymasterUsernamePasswordAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:keymaster.properties")
@PropertySource(value = "file:${conf.directory}/keymaster.properties", ignoreResourceNotFound = true)
@ComponentScan("org.apache.syncope.ext.self.keymaster.cxf.service")
@Configuration
@AutoConfigureBefore(WebSecurityContext.class)
@ConditionalOnExpression("'${keymaster.address}' matches '^http.+'")
public class SelfKeymasterContext {

    @Autowired
    private Bus bus;

    @Autowired
    private ApplicationContext ctx;

    @Resource(name = "version")
    private String version;

    @Bean
    public JacksonJsonProvider selfKeymasterJacksonJsonProvider() {
        return new JacksonJsonProvider();
    }

    @Bean
    public SelfKeymasterExceptionMapper selfKeymasterExceptionMapper() {
        return new SelfKeymasterExceptionMapper();
    }

    @Bean
    public WadlGenerator selfKeymasterWADLGenerator() {
        WadlGenerator wadlGenerator = new WadlGenerator();
        wadlGenerator.setApplicationTitle("Apache Syncope Self Keymaster " + version);
        wadlGenerator.setNamespacePrefix("syncope30");
        wadlGenerator.setIncrementNamespacePrefix(false);
        wadlGenerator.setLinkAnyMediaTypeToXmlSchema(true);
        wadlGenerator.setUseJaxbContextForQnames(true);
        wadlGenerator.setAddResourceAndMethodIds(true);
        wadlGenerator.setIgnoreMessageWriters(true);
        wadlGenerator.setUsePathParamsToCompareOperations(false);
        return wadlGenerator;
    }

    @Bean
    public Server selfKeymasterContainer() {
        SpringJAXRSServerFactoryBean selfKeymasterContainer = new SpringJAXRSServerFactoryBean();
        selfKeymasterContainer.setBus(bus);
        selfKeymasterContainer.setAddress("/keymaster");
        selfKeymasterContainer.setStaticSubresourceResolution(true);
        selfKeymasterContainer.setBasePackages(Arrays.asList(
                "org.apache.syncope.ext.self.keymaster.api.service",
                "org.apache.syncope.ext.self.keymaster.cxf.service"));
        selfKeymasterContainer.setProperties(Collections.singletonMap("convert.wadl.resources.to.dom", "false"));

        selfKeymasterContainer.setInInterceptors(Arrays.asList(
                ctx.getBean(GZIPInInterceptor.class),
                ctx.getBean(JAXRSBeanValidationInInterceptor.class)));

        selfKeymasterContainer.setOutInterceptors(Arrays.asList(
                ctx.getBean(GZIPOutInterceptor.class),
                ctx.getBean(JAXRSBeanValidationOutInterceptor.class)));

        selfKeymasterContainer.setProviders(Arrays.asList(
                selfKeymasterJacksonJsonProvider(),
                selfKeymasterExceptionMapper(),
                selfKeymasterWADLGenerator()));

        selfKeymasterContainer.setApplicationContext(ctx);
        return selfKeymasterContainer.create();
    }

    @Bean
    public UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider() {
        return new SelfKeymasterUsernamePasswordAuthenticationProvider();
    }

    @Bean
    public ConfParamOps internalConfParamOps() {
        return new SelfKeymasterInternalConfParamOps();
    }
}
