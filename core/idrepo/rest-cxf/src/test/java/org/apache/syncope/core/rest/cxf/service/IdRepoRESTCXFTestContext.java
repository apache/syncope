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

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jakarta.rs.xml.JacksonXMLProvider;
import com.fasterxml.jackson.jakarta.rs.yaml.JacksonYAMLProvider;
import org.apache.cxf.jaxrs.ext.search.SearchContextProvider;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.cxf.validation.BeanValidationProvider;
import org.apache.syncope.common.lib.jackson.SyncopeJsonMapper;
import org.apache.syncope.common.lib.jackson.SyncopeXmlMapper;
import org.apache.syncope.common.lib.jackson.SyncopeYAMLMapper;
import org.apache.syncope.common.rest.api.DateParamConverterProvider;
import org.apache.syncope.core.rest.cxf.AddETagFilter;
import org.apache.syncope.core.rest.cxf.RestServiceExceptionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class IdRepoRESTCXFTestContext {

    @Autowired
    private Environment env;

    @Bean
    public DateParamConverterProvider dateParamConverterProvider() {
        return new DateParamConverterProvider();
    }

    @Bean
    public JacksonXMLProvider xmlProvider() {
        return new JacksonXMLProvider(new SyncopeXmlMapper());
    }

    @Bean
    public JacksonJsonProvider jsonProvider() {
        return new JacksonJsonProvider(new SyncopeJsonMapper());
    }

    @Bean
    public JacksonYAMLProvider yamlProvider() {
        return new JacksonYAMLProvider(new SyncopeYAMLMapper());
    }

    @Bean
    public BeanValidationProvider validationProvider() {
        return new BeanValidationProvider();
    }

    @Bean
    public JAXRSBeanValidationInInterceptor validationInInterceptor(final BeanValidationProvider validationProvider) {
        JAXRSBeanValidationInInterceptor validationInInterceptor = new JAXRSBeanValidationInInterceptor();
        validationInInterceptor.setProvider(validationProvider);
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
        return new RestServiceExceptionMapper(env);
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
