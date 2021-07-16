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

import java.util.Collections;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.logic.audit.DefaultRewriteAuditAppender;
import org.apache.syncope.core.logic.ResourceLogic;

public class TestFileRewriteAuditAppender extends DefaultRewriteAuditAppender {

    @Override
    public Set<AuditLoggerName> getEvents() {
        return Collections.singleton(new AuditLoggerName(
                AuditElements.EventCategoryType.LOGIC,
                ResourceLogic.class.getSimpleName(),
                null,
                "update",
                AuditElements.Result.SUCCESS));
    }

    @Override
    protected void initTargetAppender() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

        // get log file path from existing file appender
        RollingRandomAccessFileAppender main =
                (RollingRandomAccessFileAppender) ctx.getConfiguration().getAppender("main");
        String pathPrefix = StringUtils.replace(main.getFileName(), "core.log", StringUtils.EMPTY);

        targetAppender = FileAppender.newBuilder().
                setName(getTargetAppenderName()).
                withAppend(true).
                withFileName(pathPrefix + getTargetAppenderName() + ".log").
                setLayout(PatternLayout.newBuilder().
                        withPattern("%d{HH:mm:ss.SSS} %-5level %logger - %msg%n").
                        build()).
                build();
    }

    @Override
    public String getTargetAppenderName() {
        return "audit_for_" + domain + "_file";
    }

    @Override
    protected RewritePolicy getRewritePolicy() {
        return TestRewritePolicy.createPolicy();
    }
}
