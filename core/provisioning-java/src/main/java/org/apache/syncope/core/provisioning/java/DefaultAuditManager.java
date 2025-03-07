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
package org.apache.syncope.core.provisioning.java;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.AuditEventDAO;
import org.apache.syncope.core.persistence.api.entity.AuditConf;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.utils.ExceptionUtils2;
import org.apache.syncope.core.provisioning.api.AuditEventProcessor;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.annotation.Transactional;

public class DefaultAuditManager implements AuditManager {

    protected static final Logger LOG = LoggerFactory.getLogger(AuditManager.class);

    protected static final String MASKED_VALUE = "<MASKED>";

    protected static Object maskSensitive(final Object object) {
        Object masked;

        if (object instanceof UserTO userTO) {
            masked = SerializationUtils.clone(userTO);
            if (((UserTO) masked).getPassword() != null) {
                ((UserTO) masked).setPassword(MASKED_VALUE);
            }
            if (((UserTO) masked).getSecurityAnswer() != null) {
                ((UserTO) masked).setSecurityAnswer(MASKED_VALUE);
            }
        } else if (object instanceof UserCR userCR) {
            masked = SerializationUtils.clone(userCR);
            if (((UserCR) masked).getPassword() != null) {
                ((UserCR) masked).setPassword(MASKED_VALUE);
            }
            if (((UserCR) masked).getSecurityAnswer() != null) {
                ((UserCR) masked).setSecurityAnswer(MASKED_VALUE);
            }
        } else if (object instanceof final UserUR userUR && userUR.getPassword() != null) {
            masked = SerializationUtils.clone(userUR);
            ((UserUR) masked).getPassword().setValue(MASKED_VALUE);
        } else {
            masked = object;
        }

        return masked;
    }

    protected final AuditConfDAO auditConfDAO;

    protected final AuditEventDAO auditEventDAO;

    protected final EntityFactory entityFactory;

    protected final List<AuditEventProcessor> auditEventProcessors;

    protected final AsyncTaskExecutor taskExecutor;

    public DefaultAuditManager(
            final AuditConfDAO auditConfDAO,
            final AuditEventDAO auditEventDAO,
            final EntityFactory entityFactory,
            final List<AuditEventProcessor> auditEventProcessors,
            final AsyncTaskExecutor taskExecutor) {

        this.auditConfDAO = auditConfDAO;
        this.auditEventDAO = auditEventDAO;
        this.entityFactory = entityFactory;
        this.auditEventProcessors = auditEventProcessors;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public boolean auditRequested(
            final String domain,
            final String who,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op) {

        return AuthContextUtils.callAsAdmin(domain, () -> {
            OpEvent opEvent = new OpEvent(type, category, subcategory, op, OpEvent.Outcome.SUCCESS);
            if (auditConfDAO.findById(opEvent.toString()).map(AuditConf::isActive).orElse(false)) {
                return true;
            }

            opEvent = new OpEvent(type, category, subcategory, op, OpEvent.Outcome.FAILURE);
            return auditConfDAO.findById(opEvent.toString()).map(AuditConf::isActive).orElse(false);
        });
    }

    @Override
    public void audit(final AfterHandlingEvent event) {
        audit(
                event.getDomain(),
                event.getWho(),
                event.getType(),
                event.getCategory(),
                event.getSubcategory(),
                event.getOp(),
                event.getOutcome(),
                event.getBefore(),
                event.getOutput(),
                event.getInput());
    }

    @Override
    public void audit(
            final String domain,
            final String who,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op,
            final OpEvent.Outcome outcome,
            final Object before,
            final Object output,
            final Object... input) {

        taskExecutor.submit(() -> AuthContextUtils.runAsAdmin(domain, new Runnable() {

            @Transactional
            @Override
            public void run() {
                OpEvent opEvent = new OpEvent(type, category, subcategory, op, outcome);

                Optional<? extends AuditConf> auditConf = auditConfDAO.findById(opEvent.toString());
                if (auditConf.isEmpty()) {
                    LOG.debug("No audit conf found for {}, skipping", opEvent);
                    return;
                }
                if (!auditConf.get().isActive()) {
                    LOG.debug("Audit conf found for {} is not active, skipping", opEvent);
                    LOG.debug("Audit conf found for {} is not active, skippping", opEvent);
                    return;
                }

                try {
                    AuditEvent auditEvent = entityFactory.newEntity(AuditEvent.class);
                    auditEvent.setOpEvent(opEvent.toString());
                    auditEvent.setWho(who);
                    auditEvent.setWhen(OffsetDateTime.now());
                    auditEvent.setBefore(POJOHelper.serialize((maskSensitive(before))));

                    Optional.ofNullable(input).ifPresent(in -> auditEvent.setInputs(Arrays.stream(in).
                            map(DefaultAuditManager::maskSensitive).map(POJOHelper::serialize).
                            toList()));

                    if (output instanceof Throwable throwable) {
                        auditEvent.setOutput(throwable.getMessage());
                        auditEvent.setThrowable(ExceptionUtils2.getFullStackTrace(throwable));
                    } else {
                        auditEvent.setOutput(POJOHelper.serialize((maskSensitive(output))));
                    }

                    auditEventDAO.save(auditEvent);

                    auditEventProcessors.stream().
                            filter(p -> p.getEvents(domain).contains(opEvent)).
                            forEach(p -> p.process(domain, auditEvent));
                } catch (Exception e) {
                    LOG.error("While processing audit event for conf {}", opEvent, e);
                }
            }
        }));
    }
}
