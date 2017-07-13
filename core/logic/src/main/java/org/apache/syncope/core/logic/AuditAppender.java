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
package org.apache.syncope.core.logic;

import java.util.Set;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.syncope.common.lib.types.AuditLoggerName;

/**
 * Basic interface to implement to define a custom audit appender
 *
 * @see org.apache.syncope.core.logic.DefaultRewriteAuditAppender or org.apache.syncope.core.logic.DefaultAuditAppender
 */
public interface AuditAppender {

    void init();

    Set<AuditLoggerName> getEvents();

    Appender getTargetAppender();

    RewritePolicy getRewritePolicy();

    String getTargetAppenderName();
    
    void setDomainName(String name);

    String getDomainName();

    boolean isRewriteEnabled();

    RewriteAppender getRewriteAppender();
}
