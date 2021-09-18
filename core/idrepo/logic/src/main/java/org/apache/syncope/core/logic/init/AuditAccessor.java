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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.entity.AuditConf;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;

/**
 * Domain-sensible (via {@code @Transactional} access to audit data.
 *
 * @see AuditLoader
 */
public class AuditAccessor {

    protected final AuditConfDAO auditConfDAO;

    public AuditAccessor(final AuditConfDAO auditConfDAO) {
        this.auditConfDAO = auditConfDAO;
    }

    @Transactional
    public void synchronizeLoggingWithAudit(final LoggerContext ctx) {
        Map<String, AuditConf> audits = auditConfDAO.findAll().stream().
                collect(Collectors.toMap(
                        audit -> AuditLoggerName.getAuditEventLoggerName(AuthContextUtils.getDomain(), audit.getKey()),
                        Function.identity()));

        audits.forEach((logger, audit) -> {
            LoggerConfig logConf = ctx.getConfiguration().getLoggerConfig(logger);
            logConf.setLevel(audit.isActive() ? Level.DEBUG : Level.OFF);
        });

        ctx.updateLoggers();
    }
}
