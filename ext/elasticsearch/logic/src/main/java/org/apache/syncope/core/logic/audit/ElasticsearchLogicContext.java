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

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.core.logic.IdRepoLogicContext;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchIndexManager;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfigureBefore(IdRepoLogicContext.class)
@Configuration(proxyBeanMethods = false)
public class ElasticsearchLogicContext {

    @ConditionalOnMissingBean(name = { "defaultAuditAppenders", "elasticsearchDefaultAuditAppenders" })
    @Bean
    public List<AuditAppender> defaultAuditAppenders(
            final DomainHolder domainHolder,
            final ElasticsearchIndexManager elasticsearchIndexManager) {

        List<AuditAppender> auditAppenders = new ArrayList<>();

        LoggerContext logCtx = (LoggerContext) LogManager.getContext(false);
        domainHolder.getDomains().forEach((domain, dataSource) -> {
            AuditAppender appender = new ElasticsearchAuditAppender(domain, elasticsearchIndexManager);

            LoggerConfig logConf = new LoggerConfig(AuditLoggerName.getAuditLoggerName(domain), null, false);
            logConf.addAppender(appender.getTargetAppender(), Level.DEBUG, null);
            logConf.setLevel(Level.DEBUG);
            logCtx.getConfiguration().addLogger(logConf.getName(), logConf);

            auditAppenders.add(appender);
        });

        return auditAppenders;
    }
}
