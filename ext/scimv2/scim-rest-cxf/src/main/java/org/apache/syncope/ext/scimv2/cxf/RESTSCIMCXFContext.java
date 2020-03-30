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
package org.apache.syncope.ext.scimv2.cxf;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan("org.apache.syncope.ext.scimv2.cxf.service")
@Configuration
public class RESTSCIMCXFContext {

    @Autowired
    private Bus bus;

    @Autowired
    private ApplicationContext ctx;

    @Resource(name = "version")
    private String version;

    @Bean
    public SCIMJacksonJsonProvider scimJacksonJsonProvider() {
        return new SCIMJacksonJsonProvider();
    }

    @Bean
    public SCIMExceptionMapper scimExceptionMapper() {
        return new SCIMExceptionMapper();
    }

    @Bean
    public AddETagFilter scimAddETagFilter() {
        return new AddETagFilter();
    }

    @Bean
    public WadlGenerator scimWADLGenerator() {
        WadlGenerator wadlGenerator = new WadlGenerator();
        wadlGenerator.setApplicationTitle("Apache Syncope SCIMv2 " + version);
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
    public Server scimv2Container() {
        SpringJAXRSServerFactoryBean scimv2Container = new SpringJAXRSServerFactoryBean();
        scimv2Container.setBus(bus);
        scimv2Container.setAddress("/scim");
        scimv2Container.setStaticSubresourceResolution(true);
        scimv2Container.setBasePackages(List.of(
                "org.apache.syncope.ext.scimv2.api.service",
                "org.apache.syncope.ext.scimv2.cxf.service"));
        scimv2Container.setProperties(Map.of("convert.wadl.resources.to.dom", "false"));

        scimv2Container.setInInterceptors(List.of(
                ctx.getBean(GZIPInInterceptor.class),
                ctx.getBean(JAXRSBeanValidationInInterceptor.class)));

        scimv2Container.setOutInterceptors(List.of(
                ctx.getBean(GZIPOutInterceptor.class)));

        scimv2Container.setProviders(List.of(
                scimJacksonJsonProvider(),
                scimExceptionMapper(),
                scimAddETagFilter(),
                scimWADLGenerator()));

        scimv2Container.setApplicationContext(ctx);
        return scimv2Container.create();
    }
}
