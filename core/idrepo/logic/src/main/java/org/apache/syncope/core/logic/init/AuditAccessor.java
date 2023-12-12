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

import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain-sensible (via {@code @Transactional} access to audit data.
 *
 * @see AuditLoader
 */
public class AuditAccessor {

    protected final AuditConfDAO auditConfDAO;

    protected final LoggingSystem loggingSystem;

    public AuditAccessor(final AuditConfDAO auditConfDAO, final LoggingSystem loggingSystem) {
        this.auditConfDAO = auditConfDAO;
        this.loggingSystem = loggingSystem;
    }

    @Transactional
    public void synchronizeLoggingWithAudit() {
        auditConfDAO.findAll().forEach(auditConf -> loggingSystem.setLogLevel(
                AuditLoggerName.getAuditEventLoggerName(AuthContextUtils.getDomain(), auditConf.getKey()),
                auditConf.isActive() ? LogLevel.DEBUG : LogLevel.OFF));
    }
}
