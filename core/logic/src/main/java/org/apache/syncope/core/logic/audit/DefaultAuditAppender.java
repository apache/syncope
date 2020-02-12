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

import org.apache.logging.log4j.core.Appender;

/**
 * Default (abstract) implementation of custom audit appender.
 *
 * It is bound to an empty collection of events, i.e. it does not create any logger.
 * This class shall be extended by non-rewrite appenders; for rewrite, extend
 * {@link DefaultRewriteAuditAppender} instead.
 */
public abstract class DefaultAuditAppender implements AuditAppender {

    protected String domain;

    protected Appender targetAppender;

    @Override
    public void init(final String domain) {
        this.domain = domain;
        initTargetAppender();
    }

    protected abstract void initTargetAppender();

    @Override
    public Appender getTargetAppender() {
        return targetAppender;
    }
}
