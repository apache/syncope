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
package org.apache.syncope.core.spring;

import groovy.lang.GroovyClassLoader;
import java.util.Optional;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.Reportlet;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

public final class ImplementationManager {

    private static final Logger LOG = LoggerFactory.getLogger(ImplementationManager.class);

    private static final GroovyClassLoader GROOVY_CLASSLOADER = new GroovyClassLoader();

    public static Optional<Reportlet> buildReportlet(final Implementation impl)
            throws InstantiationException, IllegalAccessException {

        switch (impl.getEngine()) {
            case GROOVY:
                return Optional.of(ImplementationManager.<Reportlet>buildGroovy(impl.getBody()));

            case JAVA:
            default:
                Reportlet reportlet = null;

                ReportletConf reportletConf = POJOHelper.deserialize(impl.getBody(), ReportletConf.class);
                Class<? extends Reportlet> reportletClass = ApplicationContextProvider.getApplicationContext().
                        getBean(ImplementationLookup.class).getReportletClass(reportletConf.getClass());
                if (reportletClass == null) {
                    LOG.warn("Could not find matching reportlet for {}", reportletConf.getClass());
                } else {
                    // fetch (or create) reportlet
                    if (ApplicationContextProvider.getBeanFactory().containsSingleton(reportletClass.getName())) {
                        reportlet = (Reportlet) ApplicationContextProvider.getBeanFactory().
                                getSingleton(reportletClass.getName());
                    } else {
                        reportlet = (Reportlet) ApplicationContextProvider.getBeanFactory().
                                createBean(reportletClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
                        ApplicationContextProvider.getBeanFactory().
                                registerSingleton(reportletClass.getName(), reportlet);
                    }
                    reportlet.setConf(reportletConf);
                }

                return Optional.ofNullable(reportlet);
        }
    }

    public static Optional<AccountRule> buildAccountRule(final Implementation impl)
            throws InstantiationException, IllegalAccessException {

        switch (impl.getEngine()) {
            case GROOVY:
                return Optional.of(ImplementationManager.<AccountRule>buildGroovy(impl.getBody()));

            case JAVA:
            default:
                AccountRule rule = null;

                AccountRuleConf ruleConf = POJOHelper.deserialize(impl.getBody(), AccountRuleConf.class);
                Class<? extends AccountRule> ruleClass = ApplicationContextProvider.getApplicationContext().
                        getBean(ImplementationLookup.class).getAccountRuleClass(ruleConf.getClass());
                if (ruleClass == null) {
                    LOG.warn("Could not find matching password rule for {}", impl.getClass());
                } else {
                    // fetch (or create) rule
                    if (ApplicationContextProvider.getBeanFactory().containsSingleton(ruleClass.getName())) {
                        rule = (AccountRule) ApplicationContextProvider.getBeanFactory().
                                getSingleton(ruleClass.getName());
                    } else {
                        rule = (AccountRule) ApplicationContextProvider.getBeanFactory().
                                createBean(ruleClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
                        ApplicationContextProvider.getBeanFactory().
                                registerSingleton(ruleClass.getName(), rule);
                    }
                    rule.setConf(ruleConf);
                }

                return Optional.ofNullable(rule);
        }
    }

    public static Optional<PasswordRule> buildPasswordRule(final Implementation impl)
            throws InstantiationException, IllegalAccessException {

        switch (impl.getEngine()) {
            case GROOVY:
                return Optional.of(ImplementationManager.<PasswordRule>buildGroovy(impl.getBody()));

            case JAVA:
            default:
                PasswordRule rule = null;

                PasswordRuleConf ruleConf = POJOHelper.deserialize(impl.getBody(), PasswordRuleConf.class);
                Class<? extends PasswordRule> ruleClass = ApplicationContextProvider.getApplicationContext().
                        getBean(ImplementationLookup.class).getPasswordRuleClass(ruleConf.getClass());
                if (ruleClass == null) {
                    LOG.warn("Could not find matching password rule for {}", impl.getClass());
                } else {
                    // fetch (or create) rule
                    if (ApplicationContextProvider.getBeanFactory().containsSingleton(ruleClass.getName())) {
                        rule = (PasswordRule) ApplicationContextProvider.getBeanFactory().
                                getSingleton(ruleClass.getName());
                    } else {
                        rule = (PasswordRule) ApplicationContextProvider.getBeanFactory().
                                createBean(ruleClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
                        ApplicationContextProvider.getBeanFactory().
                                registerSingleton(ruleClass.getName(), rule);
                    }
                    rule.setConf(ruleConf);
                }

                return Optional.ofNullable(rule);
        }
    }

    public static <T> T build(final Implementation impl)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        switch (impl.getEngine()) {
            case GROOVY:
                return ImplementationManager.<T>buildGroovy(impl.getBody());

            case JAVA:
            default:
                return ImplementationManager.<T>buildJava(impl.getBody());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T buildGroovy(final String classBody) throws InstantiationException, IllegalAccessException {
        Class<?> clazz = GROOVY_CLASSLOADER.parseClass(classBody);
        return (T) ApplicationContextProvider.getBeanFactory().
                createBean(clazz, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
    }

    @SuppressWarnings("unchecked")
    private static <T> T buildJava(final String className) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        return (T) ApplicationContextProvider.getBeanFactory().
                createBean(clazz, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
    }

    private ImplementationManager() {
        // private constructor for static utility class
    }
}
