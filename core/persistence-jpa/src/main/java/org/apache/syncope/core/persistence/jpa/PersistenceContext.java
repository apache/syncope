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
package org.apache.syncope.core.persistence.jpa;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.ValidationMode;
import javax.validation.Validator;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.LoggerDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.jpa.spring.CommonEntityManagerFactoryConf;
import org.apache.syncope.core.persistence.jpa.spring.DomainTransactionInterceptorInjector;
import org.apache.syncope.core.persistence.jpa.spring.MultiJarAwarePersistenceUnitPostProcessor;
import org.apache.syncope.core.spring.ResourceWithFallbackLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@PropertySource("classpath:persistence.properties")
@PropertySource(value = "file:${conf.directory}/persistence.properties", ignoreResourceNotFound = true)
@ComponentScan("org.apache.syncope.core.persistence.jpa")
@Configuration
public class PersistenceContext implements EnvironmentAware {

    private static final Logger OPENJPA_LOG = LoggerFactory.getLogger("org.apache.openjpa");

    private Environment env;

    @Override
    public void setEnvironment(final Environment env) {
        this.env = env;
    }

    @Bean
    public static BeanFactoryPostProcessor domainTransactionInterceptorInjector() {
        return new DomainTransactionInterceptorInjector();
    }

    @ConditionalOnMissingBean
    @Bean
    public CommonEntityManagerFactoryConf commonEMFConf() {
        CommonEntityManagerFactoryConf commonEMFConf = new CommonEntityManagerFactoryConf();
        commonEMFConf.setPackagesToScan("org.apache.syncope.core.persistence.jpa.entity");
        commonEMFConf.setValidationMode(ValidationMode.NONE);
        commonEMFConf.setPersistenceUnitPostProcessors(new MultiJarAwarePersistenceUnitPostProcessor());
        Map<String, Object> jpaPropertyMap = new HashMap<>();

        jpaPropertyMap.put("openjpa.Log", "slf4j");
        if (OPENJPA_LOG.isDebugEnabled()) {
            jpaPropertyMap.put("openjpa.Log", "SQL=TRACE");
            jpaPropertyMap.put("openjpa.ConnectionFactoryProperties",
                    "PrintParameters=true, PrettyPrint=true, PrettyPrintLineLength=120");
        }

        jpaPropertyMap.put("openjpa.NontransactionalWrite", false);

        jpaPropertyMap.put("openjpa.jdbc.MappingDefaults",
                "ForeignKeyDeleteAction=restrict, JoinForeignKeyDeleteAction=restrict,"
                + "FieldStrategies='"
                + "java.util.Locale=org.apache.syncope.core.persistence.jpa.openjpa.LocaleValueHandler,"
                + "java.lang.Boolean=org.apache.syncope.core.persistence.jpa.openjpa.BooleanValueHandler'");

        jpaPropertyMap.put("openjpa.DataCache", "true");
        jpaPropertyMap.put("openjpa.QueryCache", "true");

        jpaPropertyMap.put("openjpa.RemoteCommitProvider", env.getProperty("openjpa.RemoteCommitProvider", "sjvm"));

        commonEMFConf.setJpaPropertyMap(jpaPropertyMap);
        return commonEMFConf;
    }

    @Bean
    public EntityFactory entityFactory()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (EntityFactory) Class.forName(env.getProperty("entity.factory")).getConstructor().newInstance();
    }

    @ConditionalOnMissingBean(name = "plainSchemaDAO")
    @Bean
    public PlainSchemaDAO plainSchemaDAO()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (PlainSchemaDAO) Class.forName(env.getProperty("plainSchema.dao")).getConstructor().newInstance();
    }

    @ConditionalOnMissingBean(name = "plainAttrDAO")
    @Bean
    public PlainAttrDAO plainAttrDAO()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (PlainAttrDAO) Class.forName(env.getProperty("plainAttr.dao")).getConstructor().newInstance();
    }

    @ConditionalOnMissingBean(name = "plainAttrValueDAO")
    @Bean
    public PlainAttrValueDAO plainAttrValueDAO()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (PlainAttrValueDAO) Class.forName(env.getProperty("plainAttrValue.dao")).getConstructor().newInstance();
    }

    @ConditionalOnMissingBean(name = "anySearchDAO")
    @Bean
    public AnySearchDAO anySearchDAO()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (AnySearchDAO) Class.forName(env.getProperty("any.search.dao")).getConstructor().newInstance();
    }

    @ConditionalOnMissingBean(name = "anySearchVisitor")
    @Bean
    public SearchCondVisitor anySearchVisitor()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (SearchCondVisitor) Class.forName(env.getProperty("any.search.visitor")).getConstructor().newInstance();
    }

    @ConditionalOnMissingBean(name = "userDAO")
    @Bean
    public UserDAO userDAO()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (UserDAO) Class.forName(env.getProperty("user.dao")).getConstructor().newInstance();
    }

    @ConditionalOnMissingBean(name = "groupDAO")
    @Bean
    public GroupDAO groupDAO()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (GroupDAO) Class.forName(env.getProperty("group.dao")).getConstructor().newInstance();
    }

    @ConditionalOnMissingBean(name = "anyObjectDAO")
    @Bean
    public AnyObjectDAO anyObjectDAO()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (AnyObjectDAO) Class.forName(env.getProperty("anyObject.dao")).getConstructor().newInstance();
    }

    @ConditionalOnMissingBean(name = "loggerDAO")
    @Bean
    public LoggerDAO loggerDAO()
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        return (LoggerDAO) Class.forName(env.getProperty("logger.dao")).getConstructor().newInstance();
    }

    @Bean
    public Validator localValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @ConditionalOnMissingBean(name = "viewsXML")
    @Bean
    public ResourceWithFallbackLoader viewsXML() {
        ResourceWithFallbackLoader viewsXML = new ResourceWithFallbackLoader();
        viewsXML.setPrimary("file:" + env.getProperty("content.directory") + "/views.xml");
        viewsXML.setFallback("classpath:views.xml");
        return viewsXML;
    }

    @ConditionalOnMissingBean(name = "indexesXML")
    @Bean
    public ResourceWithFallbackLoader indexesXML() {
        ResourceWithFallbackLoader indexesXML = new ResourceWithFallbackLoader();
        indexesXML.setPrimary("file:" + env.getProperty("content.directory") + "/indexes.xml");
        indexesXML.setFallback("classpath:indexes.xml");
        return indexesXML;
    }
}
