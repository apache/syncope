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
package org.apache.syncope.core.rest.cxf;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.yaml.JacksonJaxbYAMLProvider;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.annotation.Resource;
import javax.servlet.ServletRequestListener;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.ext.search.SearchContextImpl;
import org.apache.cxf.jaxrs.ext.search.SearchContextProvider;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.staxutils.DocumentDepthProperties;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.cxf.validation.BeanValidationProvider;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.search.SyncopeFiqlParser;
import org.apache.syncope.common.rest.api.DateParamConverterProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ComponentScan("org.apache.syncope.core.rest.cxf.service")
@PropertySource("classpath:errorMessages.properties")
@Configuration
public class RESTCXFContext {

    @Autowired
    private Bus bus;

    @Autowired
    private ApplicationContext ctx;

    @Resource(name = "version")
    private String version;

    @Bean
    public Executor batchExecutor() {
        ThreadPoolTaskExecutor batchExecutor = new ThreadPoolTaskExecutor();
        batchExecutor.setCorePoolSize(10);
        batchExecutor.setThreadNamePrefix("Batch-");
        batchExecutor.initialize();
        return batchExecutor;
    }

    @Bean
    public DateParamConverterProvider dateParamConverterProvider() {
        return new DateParamConverterProvider();
    }

    @Bean
    public JAXBElementProvider<?> jaxbProvider() {
        JAXBElementProvider<?> jaxbProvider = new JAXBElementProvider<>();
        jaxbProvider.setNamespacePrefixes(Map.of(SyncopeConstants.NS, SyncopeConstants.NS_PREFIX));

        DocumentDepthProperties documentDepthProperties = new DocumentDepthProperties();
        documentDepthProperties.setInnerElementCountThreshold(500);
        jaxbProvider.setDepthProperties(documentDepthProperties);

        jaxbProvider.setCollectionWrapperMap(
                Map.of("org.apache.syncope.common.lib.policy.PolicyTO", "policies"));

        return jaxbProvider;
    }

    @Bean
    public JacksonJaxbJsonProvider jsonProvider() {
        JacksonJaxbJsonProvider jsonProvider = new JacksonJaxbJsonProvider();
        jsonProvider.setMapper(new UnwrappedObjectMapper());
        return jsonProvider;
    }

    @Bean
    public JacksonJaxbYAMLProvider yamlProvider() {
        return new JacksonJaxbYAMLProvider();
    }

    @Bean
    public BeanValidationProvider validationProvider() {
        return new BeanValidationProvider();
    }

    @Bean
    public JAXRSBeanValidationInInterceptor validationInInterceptor() {
        JAXRSBeanValidationInInterceptor validationInInterceptor = new JAXRSBeanValidationInInterceptor();
        validationInInterceptor.setProvider(validationProvider());
        return validationInInterceptor;
    }

    @Bean
    public GZIPInInterceptor gzipInInterceptor() {
        return new GZIPInInterceptor();
    }

    @Bean
    public GZIPOutInterceptor gzipOutInterceptor() {
        GZIPOutInterceptor gzipOutInterceptor = new GZIPOutInterceptor();
        gzipOutInterceptor.setThreshold(0);
        gzipOutInterceptor.setForce(true);
        return gzipOutInterceptor;
    }

    @Bean
    public RestServiceExceptionMapper restServiceExceptionMapper() {
        return new RestServiceExceptionMapper();
    }

    @Bean
    public SearchContextProvider searchContextProvider() {
        return new SearchContextProvider();
    }

    @Bean
    public CheckDomainFilter checkDomainFilter() {
        return new CheckDomainFilter();
    }

    @Bean
    public AddDomainFilter addDomainFilter() {
        return new AddDomainFilter();
    }

    @Bean
    public AddETagFilter addETagFilter() {
        return new AddETagFilter();
    }

    @Bean
    public WadlGenerator wadlGenerator() {
        WadlGenerator wadlGenerator = new WadlGenerator();
        wadlGenerator.setApplicationTitle("Apache Syncope " + version);
        wadlGenerator.setNamespacePrefix(SyncopeConstants.NS_PREFIX);
        wadlGenerator.setIncrementNamespacePrefix(false);
        wadlGenerator.setLinkAnyMediaTypeToXmlSchema(true);
        wadlGenerator.setUseJaxbContextForQnames(true);
        wadlGenerator.setAddResourceAndMethodIds(true);
        wadlGenerator.setIgnoreMessageWriters(true);
        wadlGenerator.setUsePathParamsToCompareOperations(false);
        return wadlGenerator;
    }

    @Bean
    public OpenApiFeature openapiFeature() {
        OpenApiFeature openapiFeature = new OpenApiFeature();
        openapiFeature.setTitle("Apache Syncope");
        openapiFeature.setVersion(version);
        openapiFeature.setDescription("Apache Syncope " + version);
        openapiFeature.setContactName("The Apache Syncope community");
        openapiFeature.setContactEmail("dev@syncope.apache.org");
        openapiFeature.setContactUrl("http://syncope.apache.org");
        openapiFeature.setScan(false);
        openapiFeature.setResourcePackages(Set.of("org.apache.syncope.common.rest.api.service"));

        SyncopeOpenApiCustomizer openApiCustomizer = new SyncopeOpenApiCustomizer(ctx.getEnvironment());
        openApiCustomizer.setDynamicBasePath(false);
        openApiCustomizer.setReplaceTags(false);
        openapiFeature.setCustomizer(openApiCustomizer);

        Map<String, SecurityScheme> securityDefinitions = new HashMap<>();
        SecurityScheme basicAuth = new SecurityScheme();
        basicAuth.setType(SecurityScheme.Type.HTTP);
        basicAuth.setScheme("basic");
        securityDefinitions.put("BasicAuthentication", basicAuth);
        SecurityScheme bearer = new SecurityScheme();
        bearer.setType(SecurityScheme.Type.HTTP);
        bearer.setScheme("bearer");
        bearer.setBearerFormat("JWT");
        securityDefinitions.put("Bearer", bearer);
        openapiFeature.setSecurityDefinitions(securityDefinitions);

        return openapiFeature;
    }

    @Bean
    public Server restContainer() {
        SpringJAXRSServerFactoryBean restContainer = new SpringJAXRSServerFactoryBean();
        restContainer.setBus(bus);
        restContainer.setAddress("/");
        restContainer.setStaticSubresourceResolution(true);
        restContainer.setBasePackages(List.of(
                "org.apache.syncope.common.rest.api.service",
                "org.apache.syncope.core.rest.cxf.service"));

        Map<String, Object> properties = new HashMap<>();
        properties.put(SearchContextImpl.CUSTOM_SEARCH_PARSER_CLASS_PROPERTY, SyncopeFiqlParser.class.getName());
        properties.put(SearchUtils.LAX_PROPERTY_MATCH, "true");
        properties.put("convert.wadl.resources.to.dom", "false");
        restContainer.setProperties(properties);

        restContainer.setProviders(List.of(
                dateParamConverterProvider(),
                jaxbProvider(),
                jsonProvider(),
                yamlProvider(),
                restServiceExceptionMapper(),
                searchContextProvider(),
                checkDomainFilter(),
                addDomainFilter(),
                addETagFilter(),
                wadlGenerator()));

        restContainer.setInInterceptors(List.of(
                gzipInInterceptor(),
                validationInInterceptor()));

        restContainer.setOutInterceptors(List.of(gzipOutInterceptor()));

        restContainer.setFeatures(List.of(openapiFeature()));

        restContainer.setApplicationContext(ctx);
        return restContainer.create();
    }

    @Bean
    public ServletListenerRegistrationBean<ServletRequestListener> listenerRegistrationBean() {
        ServletListenerRegistrationBean<ServletRequestListener> bean = new ServletListenerRegistrationBean<>();
        bean.setListener(new ThreadLocalCleanupListener());
        return bean;
    }
}
