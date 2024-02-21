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

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.logic.audit.AuditAppender;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.spring.security.AuthContextUtils;

public class AuditLoader implements SyncopeCoreLoader {

    protected static final String ROOT_LOGGER = "ROOT";

    public static void addAppenderToLoggerContext(
            final LoggerContext ctx,
            final AuditAppender auditAppender,
            final LoggerConfig eventLogConf) {

        Appender targetAppender = Optional.ofNullable(
                ctx.getConfiguration().getAppender(auditAppender.getTargetAppenderName())).map(Appender.class::cast).
                orElseGet(() -> auditAppender.getTargetAppender());
        targetAppender.start();
        ctx.getConfiguration().addAppender(targetAppender);

        auditAppender.getRewriteAppender().ifPresentOrElse(
                rewriteAppender -> {
                    rewriteAppender.start();
                    eventLogConf.addAppender(rewriteAppender, Level.DEBUG, null);
                },
                () -> eventLogConf.addAppender(targetAppender, Level.DEBUG, null));

        ctx.updateLoggers();
    }

    protected final AuditAccessor auditAccessor;

    protected final List<AuditAppender> auditAppenders;

    public AuditLoader(final AuditAccessor auditAccessor, final List<AuditAppender> auditAppenders) {
        this.auditAccessor = auditAccessor;
        this.auditAppenders = auditAppenders;
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public void load(final String domain) {
        LoggerContext logCtx = (LoggerContext) LogManager.getContext(false);

        auditAppenders.forEach(auditAppender -> auditAppender.getEvents().stream().
                map(event -> AuditLoggerName.getAuditEventLoggerName(domain, event.toAuditKey())).
                forEach(domainAuditLoggerName -> {
                    LoggerConfig eventLogConf = logCtx.getConfiguration().getLoggerConfig(domainAuditLoggerName);
                    boolean isRootLogConf = LogManager.ROOT_LOGGER_NAME.equals(eventLogConf.getName());

                    if (isRootLogConf) {
                        eventLogConf = new LoggerConfig(domainAuditLoggerName, null, false);
                    }
                    eventLogConf.setLevel(Level.DEBUG);

                    addAppenderToLoggerContext(logCtx, auditAppender, eventLogConf);

                    if (isRootLogConf) {
                        logCtx.getConfiguration().addLogger(domainAuditLoggerName, eventLogConf);
                    }
                }));

        AuthContextUtils.runAsAdmin(domain, () -> auditAccessor.synchronizeLoggingWithAudit());
    }
}
