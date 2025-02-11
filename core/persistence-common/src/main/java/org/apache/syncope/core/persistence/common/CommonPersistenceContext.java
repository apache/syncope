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
package org.apache.syncope.core.persistence.common;

import jakarta.validation.Validator;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.ClientAppUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.policy.PolicyUtilsFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.common.attrvalue.DefaultPlainAttrValidationManager;
import org.apache.syncope.core.persistence.common.content.KeymasterConfParamLoader;
import org.apache.syncope.core.persistence.common.entity.DefaultAnyUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration(proxyBeanMethods = false)
public class CommonPersistenceContext {

    @ConditionalOnMissingBean
    @Bean
    public SearchCondVisitor searchCondVisitor() {
        return new SearchCondVisitor();
    }

    @Bean
    public Validator localValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @ConditionalOnMissingBean
    @Bean
    public PlainAttrValidationManager plainAttrValidationManager() {
        return new DefaultPlainAttrValidationManager();
    }

    @ConditionalOnMissingBean
    @Bean
    public PolicyUtilsFactory policyUtilsFactory() {
        return new PolicyUtilsFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    public ClientAppUtilsFactory clientAppUtilsFactory() {
        return new ClientAppUtilsFactory();
    }

    @Bean(name = "userAnyUtils")
    public AnyUtils userAnyUtils(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy EntityFactory entityFactory) {

        return new DefaultAnyUtils(
                userDAO,
                groupDAO,
                anyObjectDAO,
                plainSchemaDAO,
                entityFactory,
                AnyTypeKind.USER);
    }

    @Bean(name = "groupAnyUtils")
    public AnyUtils groupAnyUtils(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy EntityFactory entityFactory) {

        return new DefaultAnyUtils(
                userDAO,
                groupDAO,
                anyObjectDAO,
                plainSchemaDAO,
                entityFactory,
                AnyTypeKind.GROUP);
    }

    @Bean(name = "anyObjectAnyUtils")
    public AnyUtils anyObjectAnyUtils(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy EntityFactory entityFactory) {

        return new DefaultAnyUtils(
                userDAO,
                groupDAO,
                anyObjectDAO,
                plainSchemaDAO,
                entityFactory,
                AnyTypeKind.ANY_OBJECT);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyUtilsFactory anyUtilsFactory(
            @Qualifier("userAnyUtils")
            final AnyUtils userAnyUtils,
            @Qualifier("groupAnyUtils")
            final AnyUtils groupAnyUtils,
            @Qualifier("anyObjectAnyUtils")
            final AnyUtils anyObjectAnyUtils) {

        return new AnyUtilsFactory(userAnyUtils, groupAnyUtils, anyObjectAnyUtils);
    }

    @ConditionalOnMissingBean
    @Bean
    public KeymasterConfParamLoader keymasterConfParamLoader(final ConfParamOps confParamOps) {
        return new KeymasterConfParamLoader(confParamOps);
    }
}
