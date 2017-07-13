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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;

public abstract class AbstractAuditAppender implements AuditAppender {

    protected String domainName;

    protected Appender targetAppender;

    protected RewriteAppender rewriteAppender;

    @Override
    public abstract void init();

    public abstract void initTargetAppender();

    public abstract void initRewriteAppender();

    @Override
    public abstract RewritePolicy getRewritePolicy();

    @Override
    public String getDomainName() {
        return domainName;
    }

    @Override
    public void setDomainName(final String domainName) {
        this.domainName = domainName;
    }

    @Override
    public abstract String getTargetAppenderName();

    @Override
    public boolean isRewriteEnabled() {
        return rewriteAppender != null;
    }

    @Override
    public RewriteAppender getRewriteAppender() {
        return rewriteAppender;
    }

    @Override
    public Appender getTargetAppender() {
        return targetAppender;
    }

}
