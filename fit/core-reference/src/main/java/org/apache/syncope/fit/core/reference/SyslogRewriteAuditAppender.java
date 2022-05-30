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
package org.apache.syncope.fit.core.reference;

import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.logic.ConnectorLogic;
import org.apache.syncope.core.logic.audit.DefaultRewriteAuditAppender;
import org.apache.syncope.core.logic.ResourceLogic;

public class SyslogRewriteAuditAppender extends DefaultRewriteAuditAppender {

    @Override
    public Set<AuditLoggerName> getEvents() {
        Set<AuditLoggerName> events = new HashSet<>();
        events.add(new AuditLoggerName(
                AuditElements.EventCategoryType.LOGIC,
                ResourceLogic.class.getSimpleName(),
                null,
                "update",
                AuditElements.Result.SUCCESS));
        events.add(new AuditLoggerName(
                AuditElements.EventCategoryType.LOGIC,
                ConnectorLogic.class.getSimpleName(),
                null,
                "update",
                AuditElements.Result.SUCCESS));
        events.add(new AuditLoggerName(
                AuditElements.EventCategoryType.LOGIC,
                ResourceLogic.class.getSimpleName(),
                null,
                "delete",
                AuditElements.Result.SUCCESS));
        return events;
    }

    @Override
    protected void initTargetAppender() {
        targetAppender = SyslogAppender.newSyslogAppenderBuilder().
                setName(getTargetAppenderName()).
                setHost("localhost").
                setPort(514).
                setProtocol(Protocol.UDP).
                setLayout(PatternLayout.newBuilder().withPattern("%d{ISO8601} %-5level %logger - %msg%n").build()).
                setFacility(Facility.LOCAL1).
                build();
    }

    @Override
    public String getTargetAppenderName() {
        return "audit_for_" + domain + "_syslog";
    }
}
