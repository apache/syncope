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
import org.apache.syncope.common.lib.policy.InboundCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.policy.PushCorrelationRuleConf;
import org.apache.syncope.common.lib.report.ReportConf;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate;
import org.apache.syncope.core.provisioning.api.rules.AccountRule;
import org.apache.syncope.core.provisioning.api.rules.InboundCorrelationRule;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.apache.syncope.core.provisioning.api.rules.PushCorrelationRule;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

public final class ImplementationManager {

    private static final GroovyClassLoader GROOVY_CLASSLOADER = new GroovyClassLoader();

    private static final Map<String, Class<?>> CLASS_CACHE = Collections.synchronizedMap(new HashMap<>());

    @SuppressWarnings("unchecked")
    public static Optional<ReportJobDelegate> buildReportJobDelegate(
            final Implementation impl,
            final Supplier<ReportJobDelegate> cacheGetter,
            final Consumer<ReportJobDelegate> cachePutter)
            throws ClassNotFoundException {

        switch (impl.getEngine()) {
            case GROOVY:
                return Optional.of(build(impl, cacheGetter, cachePutter));

            case JAVA:
            default:
                ReportConf conf = POJOHelper.deserialize(impl.getBody(), ReportConf.class);
                Class<ReportJobDelegate> clazz =
                        (Class<ReportJobDelegate>) ApplicationContextProvider.getApplicationContext().
                                getBean(ImplementationLookup.class).getReportClass(conf.getClass());

                if (clazz == null) {
                    return Optional.empty();
                }

                ReportJobDelegate report = build(clazz, true, cacheGetter, cachePutter);
                report.setConf(conf);
                return Optional.of(report);
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
    public static Optional<InboundCorrelationRule> buildInboundCorrelationRule(
            final Implementation impl,
            final Supplier<InboundCorrelationRule> cacheGetter,
            final Consumer<InboundCorrelationRule> cachePutter)
            throws ClassNotFoundException {

        switch (impl.getEngine()) {
            case GROOVY:
                return Optional.of(build(impl, cacheGetter, cachePutter));

            case JAVA:
            default:
                InboundCorrelationRuleConf conf = POJOHelper.deserialize(
                        impl.getBody(), InboundCorrelationRuleConf.class);
                Class<InboundCorrelationRule> clazz =
                        (Class<InboundCorrelationRule>) ApplicationContextProvider.getApplicationContext().
                                getBean(ImplementationLookup.class).getInboundCorrelationRuleClass(conf.getClass());
                if (clazz == null) {
                    return Optional.empty();
                }

                InboundCorrelationRule rule = build(clazz, true, cacheGetter, cachePutter);
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Class<? extends CommandArgs> findCommandArgsClass(final Type type) {
        if (type.getTypeName().startsWith(
                ImplementationTypesHolder.getInstance().getValues().get(IdRepoImplementationType.COMMAND) + "<")) {

            return (Class<? extends CommandArgs>) ((ParameterizedType) type).getActualTypeArguments()[0];
        }

        if (type instanceof Class aClass) {
            for (Type i : aClass.getGenericInterfaces()) {
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
        return (T) ApplicationContextProvider.getBeanFactory().createBean(getClass(impl).getLeft());
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
            instance = ApplicationContextProvider.getBeanFactory().createBean(clazz);

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
