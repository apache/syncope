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

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import java.util.List;
import java.util.Map;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.SCIMDataBinder;
import org.apache.syncope.core.logic.SCIMLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.ext.scimv2.api.service.SCIMGroupService;
import org.apache.syncope.ext.scimv2.api.service.SCIMService;
import org.apache.syncope.ext.scimv2.api.service.SCIMUserService;
import org.apache.syncope.ext.scimv2.cxf.service.SCIMGroupServiceImpl;
import org.apache.syncope.ext.scimv2.cxf.service.SCIMServiceImpl;
import org.apache.syncope.ext.scimv2.cxf.service.SCIMUserServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SCIMv2RESTCXFContext {

    @ConditionalOnMissingBean
    @Bean
    public JacksonJsonProvider scimJacksonJsonProvider() {
        return new JacksonJsonProvider(JsonMapper.builder().
                findAndAddModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build());
    }

    @ConditionalOnMissingBean
    @Bean
    public SCIMExceptionMapper scimExceptionMapper() {
        return new SCIMExceptionMapper();
    }

    @ConditionalOnMissingBean(name = "scimAddETagFilter")
    @Bean
    public AddETagFilter scimAddETagFilter() {
        return new AddETagFilter();
    }

    @ConditionalOnMissingBean(name = "scimv2Container")
    @Bean
    public Server scimv2Container(
            final SCIMService scimService,
            final SCIMGroupService scimv2GroupService,
            final SCIMUserService scimv2UserService,
            final GZIPInInterceptor gzipInInterceptor,
            final GZIPOutInterceptor gzipOutInterceptor,
            final JAXRSBeanValidationInInterceptor validationInInterceptor,
            final JacksonJsonProvider scimJacksonJsonProvider,
            final SCIMExceptionMapper scimExceptionMapper,
            final AddETagFilter scimAddETagFilter,
            final Bus bus,
            final ApplicationContext ctx) {

        SpringJAXRSServerFactoryBean scimv2Container = new SpringJAXRSServerFactoryBean();
        scimv2Container.setBus(bus);
        scimv2Container.setAddress("/scim");
        scimv2Container.setStaticSubresourceResolution(true);

        scimv2Container.setProperties(Map.of("convert.wadl.resources.to.dom", "false"));

        scimv2Container.setServiceBeans(List.of(scimService, scimv2GroupService, scimv2UserService));

        scimv2Container.setInInterceptors(List.of(gzipInInterceptor, validationInInterceptor));

        scimv2Container.setOutInterceptors(List.of(gzipOutInterceptor));

        scimv2Container.setProviders(List.of(scimJacksonJsonProvider, scimExceptionMapper, scimAddETagFilter));

        scimv2Container.setApplicationContext(ctx);
        return scimv2Container.create();
    }

    @ConditionalOnMissingBean
    @Bean
    public SCIMService scimService(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final SCIMDataBinder binder,
            final SCIMConfManager confManager,
            final SCIMLogic scimLogic) {

        return new SCIMServiceImpl(userDAO, groupDAO, userLogic, groupLogic, binder, confManager, scimLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public SCIMGroupService scimv2GroupService(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final SCIMDataBinder binder,
            final SCIMConfManager confManager) {

        return new SCIMGroupServiceImpl(userDAO, groupDAO, userLogic, groupLogic, binder, confManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public SCIMUserService scimv2UserService(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final SCIMDataBinder binder,
            final SCIMConfManager confManager) {

        return new SCIMUserServiceImpl(userDAO, groupDAO, userLogic, groupLogic, binder, confManager);
    }
}
