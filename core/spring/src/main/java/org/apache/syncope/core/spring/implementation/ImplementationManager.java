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
package org.apache.syncope.core.spring.implementation;

import groovy.lang.GroovyClassLoader;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.command.CommandArgs;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.policy.PullCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.PushCorrelationRuleConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.PullCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.Reportlet;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

public final class ImplementationManager {

    private static final GroovyClassLoader GROOVY_CLASSLOADER = new GroovyClassLoader();

    private static final Map<String, Class<?>> CLASS_CACHE = Collections.synchronizedMap(new HashMap<>());

    public static Optional<Reportlet> buildReportlet(final Implementation impl) throws ClassNotFoundException {
        switch (impl.getEngine()) {
            case GROOVY:
                return Optional.of(build(impl));

            case JAVA:
            default:
                ReportletConf conf = POJOHelper.deserialize(impl.getBody(), ReportletConf.class);
                Class<? extends Reportlet> clazz = ApplicationContextProvider.getApplicationContext().
                        getBean(ImplementationLookup.class).getReportletClass(conf.getClass());

                if (clazz == null) {
                    return Optional.empty();
                }

                Reportlet reportlet = (Reportlet) ApplicationContextProvider.getBeanFactory().
                        createBean(clazz, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
                reportlet.setConf(conf);
                return Optional.of(reportlet);
        }
    }

    @SuppressWarnings("unchecked")
    public static Optional<AccountRule> buildAccountRule(
            final Implementation impl,
            final Supplier<AccountRule> cacheGetter,
            final Consumer<AccountRule> cachePutter)
            throws ClassNotFoundException {

        switch (impl.getEngine()) {
            case GROOVY:
                return Optional.of(build(impl, cacheGetter, cachePutter));

            case JAVA:
            default:
                AccountRuleConf conf = POJOHelper.deserialize(impl.getBody(), AccountRuleConf.class);
                Class<AccountRule> clazz = (Class<AccountRule>) ApplicationContextProvider.getApplicationContext().
                        getBean(ImplementationLookup.class).getAccountRuleClass(conf.getClass());

                if (clazz == null) {
                    return Optional.empty();
                }

                AccountRule rule = build(clazz, true, cacheGetter, cachePutter);
                rule.setConf(conf);
                return Optional.of(rule);
        }
    }

    @SuppressWarnings("unchecked")
    public static Optional<PasswordRule> buildPasswordRule(
            final Implementation impl,
            final Supplier<PasswordRule> cacheGetter,
            final Consumer<PasswordRule> cachePutter)
            throws ClassNotFoundException {

        switch (impl.getEngine()) {
            case GROOVY:
                return Optional.of(build(impl, cacheGetter, cachePutter));

            case JAVA:
            default:
                PasswordRuleConf conf = POJOHelper.deserialize(impl.getBody(), PasswordRuleConf.class);
                Class<PasswordRule> clazz = (Class<PasswordRule>) ApplicationContextProvider.getApplicationContext().
                        getBean(ImplementationLookup.class).getPasswordRuleClass(conf.getClass());

                if (clazz == null) {
                    return Optional.empty();
                }

                PasswordRule rule = build(clazz, true, cacheGetter, cachePutter);
                rule.setConf(conf);
                return Optional.of(rule);
        }
    }

    @SuppressWarnings("unchecked")
    public static Optional<PullCorrelationRule> buildPullCorrelationRule(
            final Implementation impl,
            final Supplier<PullCorrelationRule> cacheGetter,
            final Consumer<PullCorrelationRule> cachePutter)
            throws ClassNotFoundException {

        switch (impl.getEngine()) {
            case GROOVY:
                return Optional.of(build(impl, cacheGetter, cachePutter));

            case JAVA:
            default:
                PullCorrelationRuleConf conf = POJOHelper.deserialize(impl.getBody(), PullCorrelationRuleConf.class);
                Class<PullCorrelationRule> clazz =
                        (Class<PullCorrelationRule>) ApplicationContextProvider.getApplicationContext().
                                getBean(ImplementationLookup.class).getPullCorrelationRuleClass(conf.getClass());

                if (clazz == null) {
                    return Optional.empty();
                }

                PullCorrelationRule rule = build(clazz, true, cacheGetter, cachePutter);
                rule.setConf(conf);
                return Optional.of(rule);
        }
    }

    @SuppressWarnings("unchecked")
    public static Optional<PushCorrelationRule> buildPushCorrelationRule(
            final Implementation impl,
            final Supplier<PushCorrelationRule> cacheGetter,
            final Consumer<PushCorrelationRule> cachePutter)
            throws ClassNotFoundException {

        switch (impl.getEngine()) {
            case GROOVY:
                return Optional.of(build(impl, cacheGetter, cachePutter));

            case JAVA:
            default:
                PushCorrelationRuleConf conf = POJOHelper.deserialize(impl.getBody(), PushCorrelationRuleConf.class);
                Class<PushCorrelationRule> clazz =
                        (Class<PushCorrelationRule>) ApplicationContextProvider.getApplicationContext().
                                getBean(ImplementationLookup.class).getPushCorrelationRuleClass(conf.getClass());

                if (clazz == null) {
                    return Optional.empty();
                }

                PushCorrelationRule rule = build(clazz, true, cacheGetter, cachePutter);
                rule.setConf(conf);
                return Optional.of(rule);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends CommandArgs> findCommandArgsClass(final Type type) {
        if (type.getTypeName().startsWith(
                ImplementationTypesHolder.getInstance().getValues().get(IdRepoImplementationType.COMMAND) + "<")) {

            return (Class<? extends CommandArgs>) ((ParameterizedType) type).getActualTypeArguments()[0];
        }

        if (type instanceof Class) {
            for (Type i : ((Class) type).getGenericInterfaces()) {
                Class<? extends CommandArgs> r = findCommandArgsClass(i);
                if (r != null) {
                    return r;
                }
            }
        }

        return null;
    }

    public static CommandArgs emptyArgs(final Implementation impl) throws Exception {
        if (!IdRepoImplementationType.COMMAND.equals(impl.getType())) {
            throw new IllegalArgumentException("This method can be only called on implementations");
        }

        Class<Object> commandClass = getClass(impl).getLeft();

        Class<? extends CommandArgs> commandArgsClass = findCommandArgsClass(commandClass);
        if (commandArgsClass != null
                && (commandArgsClass.getEnclosingClass() == null
                || Modifier.isStatic(commandArgsClass.getModifiers()))) {

            return commandArgsClass.getDeclaredConstructor().newInstance();
        }

        throw new IllegalArgumentException(
                CommandArgs.class.getName() + " shall be either declared as independent or nested static");
    }

    @SuppressWarnings("unchecked")
    private static <T> Pair<Class<T>, Boolean> getClass(final Implementation impl) throws ClassNotFoundException {
        if (CLASS_CACHE.containsKey(impl.getKey())) {
            return Pair.of((Class<T>) CLASS_CACHE.get(impl.getKey()), true);
        }

        Class<?> clazz;
        switch (impl.getEngine()) {
            case GROOVY:
                clazz = GROOVY_CLASSLOADER.parseClass(impl.getBody());
                break;

            case JAVA:
            default:
                clazz = Class.forName(impl.getBody());
        }

        CLASS_CACHE.put(impl.getKey(), clazz);
        return Pair.of((Class<T>) clazz, false);
    }

    @SuppressWarnings("unchecked")
    public static <T> T build(final Implementation impl) throws ClassNotFoundException {
        return (T) ApplicationContextProvider.getBeanFactory().
                createBean(getClass(impl).getLeft(), AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);
    }

    @SuppressWarnings("unchecked")
    private static <T> T build(
            final Class<T> clazz,
            final boolean classCached,
            final Supplier<T> cacheGetter,
            final Consumer<T> cachePutter) {

        boolean perContext = Optional.ofNullable(clazz.getAnnotation(SyncopeImplementation.class)).
                map(ann -> ann.scope() == InstanceScope.PER_CONTEXT).
                orElse(true);
        T instance = null;
        if (perContext && classCached) {
            instance = cacheGetter.get();
        }
        if (instance == null) {
            instance = (T) ApplicationContextProvider.getBeanFactory().
                    createBean(clazz, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, false);

            if (perContext) {
                cachePutter.accept(instance);
            }
        }

        return instance;
    }

    public static <T> T build(final Implementation impl, final Supplier<T> cacheGetter, final Consumer<T> cachePutter)
            throws ClassNotFoundException {

        Pair<Class<T>, Boolean> clazz = getClass(impl);

        return build(clazz.getLeft(), clazz.getRight(), cacheGetter, cachePutter);
    }

    public static Class<?> purge(final String implementation) {
        return CLASS_CACHE.remove(implementation);
    }

    private ImplementationManager() {
        // private constructor for static utility class
    }
}
