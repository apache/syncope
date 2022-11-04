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
package org.apache.syncope.core.logic.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.logic.audit.AuditAppender;
import org.apache.syncope.core.logic.MemoryAppender;
import org.apache.syncope.core.logic.audit.DefaultAuditAppender;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class LoggerLoader implements SyncopeLoader {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerLoader.class);

    @Autowired
    private DomainsHolder domainsHolder;

    @Autowired
    private LoggerAccessor loggerAccessor;

    @Autowired
    private ImplementationLookup implementationLookup;

    @Value("${default.audit.appender:org.apache.syncope.core.logic.audit.JdbcAuditAppender}")
    private String defaultAuditAppender;

    private final Map<String, MemoryAppender> memoryAppenders = new HashMap<>();

    @Override
    public Integer getPriority() {
        return 300;
    }

    @Override
    public void load() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

        ctx.getConfiguration().getAppenders().entrySet().stream().
                filter(entry -> entry.getValue() instanceof MemoryAppender).
                forEach(entry -> memoryAppenders.put(entry.getKey(), (MemoryAppender) entry.getValue()));

        domainsHolder.getDomains().keySet().forEach(domain -> {
            try {
                DefaultAuditAppender dfaa = (DefaultAuditAppender) ApplicationContextProvider.getBeanFactory().
                        createBean(
                                ClassUtils.forName(defaultAuditAppender, ClassUtils.getDefaultClassLoader()),
                                AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
                dfaa.init(domain);

                LoggerConfig logConf = new LoggerConfig(AuditLoggerName.getAuditLoggerName(domain), null, false);
                logConf.addAppender(dfaa.getTargetAppender(), Level.DEBUG, null);
                logConf.setLevel(Level.DEBUG);
                ctx.getConfiguration().addLogger(logConf.getName(), logConf);
            } catch (Exception e) {
                LOG.error("While creating instance of DefaultAuditAppender {}", defaultAuditAppender, e);
            }

            // SYNCOPE-1144 For each custom audit appender class add related appenders to log4j logger
            auditAppenders(domain).forEach(auditAppender -> auditAppender.getEvents().stream().
                    map(event -> AuditLoggerName.getAuditEventLoggerName(domain, event.toLoggerName())).
                    forEach(domainAuditLoggerName -> {
                        LoggerConfig eventLogConf = ctx.getConfiguration().getLoggerConfig(domainAuditLoggerName);
                        boolean isRootLogConf = LogManager.ROOT_LOGGER_NAME.equals(eventLogConf.getName());
                        if (isRootLogConf) {
                            eventLogConf = new LoggerConfig(domainAuditLoggerName, null, false);
                        }
                        addAppenderToContext(ctx, auditAppender, eventLogConf);
                        eventLogConf.setLevel(Level.DEBUG);
                        if (isRootLogConf) {
                            ctx.getConfiguration().addLogger(domainAuditLoggerName, eventLogConf);
                        }
                    }));

            AuthContextUtils.execWithAuthContext(domain, () -> {
                loggerAccessor.synchronizeLog4J(ctx);
                return null;
            });
        });

        ctx.updateLoggers();
    }

    public Map<String, MemoryAppender> getMemoryAppenders() {
        return memoryAppenders;
    }

    public List<AuditAppender> auditAppenders(final String domain) throws BeansException {
        return implementationLookup.getAuditAppenderClasses().stream().map(clazz -> {
            AuditAppender auditAppender;
            if (ApplicationContextProvider.getBeanFactory().containsSingleton(clazz.getName())) {
                auditAppender = (AuditAppender) ApplicationContextProvider.getBeanFactory().
                        getSingleton(clazz.getName());
            } else {
                auditAppender = (AuditAppender) ApplicationContextProvider.getBeanFactory().
                        createBean(clazz, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
                auditAppender.init(domain);
            }
            return auditAppender;
        }).collect(Collectors.toList());
    }

    public void addAppenderToContext(
            final LoggerContext ctx,
            final AuditAppender auditAppender,
            final LoggerConfig eventLogConf) {

        Appender targetAppender = ctx.getConfiguration().getAppender(auditAppender.getTargetAppenderName());
        if (targetAppender == null) {
            targetAppender = auditAppender.getTargetAppender();
        }
        targetAppender.start();
        ctx.getConfiguration().addAppender(targetAppender);

        Optional<RewriteAppender> rewriteAppender = auditAppender.getRewriteAppender();
        if (rewriteAppender.isPresent()) {
            rewriteAppender.get().start();
            eventLogConf.addAppender(rewriteAppender.get(), Level.DEBUG, null);
        } else {
            eventLogConf.addAppender(targetAppender, Level.DEBUG, null);
        }
    }
}
