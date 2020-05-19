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
package org.apache.syncope.core.rest.cxf.service;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.yaml.JacksonJaxbYAMLProvider;
import java.util.Map;
import org.apache.cxf.jaxrs.ext.search.SearchContextProvider;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.staxutils.DocumentDepthProperties;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.cxf.validation.BeanValidationProvider;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.rest.api.DateParamConverterProvider;
import org.apache.syncope.core.rest.cxf.AddETagFilter;
import org.apache.syncope.core.rest.cxf.RestServiceExceptionMapper;
import org.apache.syncope.core.rest.cxf.SyncopeObjectMapper;
import org.apache.syncope.core.rest.cxf.SyncopeYAMLMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RESTCXFTestContext {

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

        jaxbProvider.setCollectionWrapperMap(Map.of("org.apache.syncope.common.lib.policy.PolicyTO", "policies"));

        return jaxbProvider;
    }

    @Bean
    public JacksonJaxbJsonProvider jsonProvider() {
        JacksonJaxbJsonProvider jsonProvider = new JacksonJaxbJsonProvider();
        jsonProvider.setMapper(new SyncopeObjectMapper());
        return jsonProvider;
    }

    @Bean
    public JacksonJaxbYAMLProvider yamlProvider() {
        JacksonJaxbYAMLProvider yamlProvider = new JacksonJaxbYAMLProvider();
        yamlProvider.setMapper(new SyncopeYAMLMapper());
        return yamlProvider;
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
    public AddETagFilter addETagFilter() {
        return new AddETagFilter();
    }
}
