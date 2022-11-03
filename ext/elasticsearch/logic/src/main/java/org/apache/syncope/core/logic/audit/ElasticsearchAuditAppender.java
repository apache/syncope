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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchIndexManager;

public class ElasticsearchAuditAppender extends DefaultAuditAppender {

    public ElasticsearchAuditAppender(final String domain, final ElasticsearchIndexManager elasticsearchIndexManager) {
        super(domain);

        LoggerContext logCtx = (LoggerContext) LogManager.getContext(false);

        targetAppender = Optional.ofNullable(logCtx.getConfiguration().<Appender>getAppender(getTargetAppenderName())).
                orElseGet(() -> {
                    ElasticsearchAppender a = ElasticsearchAppender.newBuilder().
                            setName(getTargetAppenderName()).
                            setIgnoreExceptions(false).
                            setDomain(domain).
                            setIndexManager(elasticsearchIndexManager).
                            build();
                    a.start();
                    logCtx.getConfiguration().addAppender(a);
                    return a;
                });
    }

    @Override
    public String getTargetAppenderName() {
        return "audit_for_" + domain;
    }
}
