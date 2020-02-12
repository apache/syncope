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
package org.apache.syncope.core.logic.audit;

import java.util.Optional;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.AppenderRef;

/**
 * Default (abstract) implementation of rewrite audit appender, allowing the logging event to be manipulated
 * before it is processed.
 * It provides rewrite appender definition and a default "pass-through" policy.
 * It is bound to an empty collection of events, i.e. it does not create any logger.
 * This class shall be extended by rewrite appenders; for non-rewrite, extend {@link DefaultAuditAppender} instead.
 *
 * @see RewriteAppender
 */
public abstract class DefaultRewriteAuditAppender extends DefaultAuditAppender {

    protected RewriteAppender rewriteAppender;

    @Override
    public void init(final String domain) {
        super.init(domain);

        rewriteAppender = RewriteAppender.createAppender(
                getTargetAppenderName() + "_rewrite",
                "true",
                new AppenderRef[] { AppenderRef.createAppenderRef(getTargetAppenderName(), Level.DEBUG, null) },
                ((LoggerContext) LogManager.getContext(false)).getConfiguration(), getRewritePolicy(), null);
    }

    protected RewritePolicy getRewritePolicy() {
        return PassThroughAuditRewritePolicy.createPolicy();
    }

    @Override
    public Optional<RewriteAppender> getRewriteAppender() {
        return Optional.of(rewriteAppender);
    }
}
