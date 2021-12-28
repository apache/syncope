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
import org.apache.syncope.ext.scimv2.api.service.GroupService;
import org.apache.syncope.ext.scimv2.api.service.SCIMService;
import org.apache.syncope.ext.scimv2.api.service.UserService;
import org.apache.syncope.ext.scimv2.cxf.service.GroupServiceImpl;
import org.apache.syncope.ext.scimv2.cxf.service.SCIMServiceImpl;
import org.apache.syncope.ext.scimv2.cxf.service.UserServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SCIMv2RESTCXFContext {

    @ConditionalOnMissingBean
    @Bean
    public SCIMJacksonJsonProvider scimJacksonJsonProvider() {
        return new SCIMJacksonJsonProvider();
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
    public Server scimv2Container(final ApplicationContext ctx, final Bus bus,
                                  final SCIMJacksonJsonProvider scimJacksonJsonProvider,
                                  final SCIMExceptionMapper scimExceptionMapper,
                                  final AddETagFilter scimAddETagFilter) {
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
                scimJacksonJsonProvider,
                scimExceptionMapper,
                scimAddETagFilter));

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
    public GroupService scimv2GroupService(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final SCIMDataBinder binder,
            final SCIMConfManager confManager) {

        return new GroupServiceImpl(userDAO, groupDAO, userLogic, groupLogic, binder, confManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserService scimv2UserService(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final SCIMDataBinder binder,
            final SCIMConfManager confManager) {

        return new UserServiceImpl(userDAO, groupDAO, userLogic, groupLogic, binder, confManager);
    }
}
