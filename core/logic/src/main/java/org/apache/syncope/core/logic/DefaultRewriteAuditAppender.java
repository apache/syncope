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

import java.util.Collections;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.syncope.common.lib.types.AuditLoggerName;

/**
 * Default (abstract) implementation of custom rewriting audit appender; it provides rewrite appender definition and 
 * a default "pass-through" policy. It is bound to an empty collection of events, i.e. it does not create any logger.
 * This class has to be extended by rewrite appenders.
 *
 * @see org.apache.syncope.fit.core.reference.TestFileRewriteAuditAppender
 */
public abstract class DefaultRewriteAuditAppender extends AbstractAuditAppender {

    @Override
    public void init() {
        initTargetAppender();
        initRewriteAppender();
    }

    @Override
    public void initRewriteAppender() {
        rewriteAppender = RewriteAppender.createAppender(getTargetAppenderName() + "_rewrite",
                "true",
                new AppenderRef[] { AppenderRef.createAppenderRef(getTargetAppenderName(), Level.DEBUG, null) },
                ((LoggerContext) LogManager.getContext(false)).getConfiguration(), getRewritePolicy(), null);
    }

    @Override
    public Set<AuditLoggerName> getEvents() {
        return Collections.emptySet();
    }

    @Override
    public RewritePolicy getRewritePolicy() {
        return PassThroughRewritePolicy.createPolicy();
    }

}
