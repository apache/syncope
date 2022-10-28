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

import java.util.Optional;
import javax.sql.DataSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.logic.audit.AuditAppender;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.context.ApplicationContext;

public class AuditLoader implements SyncopeCoreLoader {

    protected static final String ROOT_LOGGER = "ROOT";

    public static void addAppenderToLoggerContext(
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

    protected final AuditAccessor auditAccessor;

    protected final ApplicationContext ctx;

    public AuditLoader(final AuditAccessor auditAccessor, final ApplicationContext ctx) {
        this.auditAccessor = auditAccessor;
        this.ctx = ctx;
    }

    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public void load(final String domain, final DataSource datasource) {
        LoggerContext logCtx = (LoggerContext) LogManager.getContext(false);

        // SYNCOPE-1144 For each custom audit appender class add related appenders to log4j logger
        ctx.getBeansOfType(AuditAppender.class).values().forEach(auditAppender -> auditAppender.getEvents().stream().
                map(event -> AuditLoggerName.getAuditEventLoggerName(domain, event.toAuditKey())).
                forEach(domainAuditLoggerName -> {
                    LoggerConfig eventLogConf = logCtx.getConfiguration().getLoggerConfig(domainAuditLoggerName);
                    boolean isRootLogConf = LogManager.ROOT_LOGGER_NAME.equals(eventLogConf.getName());

                    if (isRootLogConf) {
                        eventLogConf = new LoggerConfig(domainAuditLoggerName, null, false);
                    }

                    addAppenderToLoggerContext(logCtx, auditAppender, eventLogConf);
                    eventLogConf.setLevel(Level.DEBUG);

                    if (isRootLogConf) {
                        logCtx.getConfiguration().addLogger(domainAuditLoggerName, eventLogConf);
                    }
                }));

        AuthContextUtils.callAsAdmin(domain, () -> {
            auditAccessor.synchronizeLoggingWithAudit(logCtx);
            return null;
        });

        logCtx.updateLoggers();
    }
}
