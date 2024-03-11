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

import java.lang.reflect.Method;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class LogicInvocationHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(LogicInvocationHandler.class);

    protected final NotificationManager notificationManager;

    protected final AuditManager auditManager;

    public LogicInvocationHandler(final NotificationManager notificationManager, final AuditManager auditManager) {
        this.notificationManager = notificationManager;
        this.auditManager = auditManager;
    }

    @Around("execution(* org.apache.syncope.core.logic.AbstractLogic+.*(..))")
    public Object around(final ProceedingJoinPoint joinPoint) throws Throwable {
        Class<?> clazz = joinPoint.getTarget().getClass();

        Object[] input = joinPoint.getArgs();

        String category = clazz.getSimpleName();

        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method method = ms.getMethod();

        String event = joinPoint.getSignature().getName();

        boolean notificationsAvailable = notificationManager.notificationsAvailable(
                AuthContextUtils.getDomain(),
                OpEvent.CategoryType.LOGIC,
                category,
                null,
                event);
        boolean auditRequested = auditManager.auditRequested(
                AuthContextUtils.getDomain(),
                AuthContextUtils.getUsername(),
                OpEvent.CategoryType.LOGIC,
                category,
                null,
                event);

        OpEvent.Outcome outcome = null;
        Object output = null;
        Object before = null;

        try {
            LOG.debug("Before {}.{}({})", clazz.getSimpleName(), event,
                    input == null || input.length == 0 ? StringUtils.EMPTY : input);

            if (notificationsAvailable || auditRequested) {
                try {
                    before = ((AbstractLogic) joinPoint.getTarget()).resolveBeanReference(method, input);
                } catch (UnresolvedReferenceException ignore) {
                    LOG.debug("Unresolved bean reference ...");
                }
            }

            output = joinPoint.proceed();
            outcome = OpEvent.Outcome.SUCCESS;

            LOG.debug("After returning {}.{}: {}", clazz.getSimpleName(), event, output);
            return output;
        } catch (Throwable t) {
            output = t;
            outcome = OpEvent.Outcome.FAILURE;

            LOG.debug("After throwing {}.{}", clazz.getSimpleName(), event);
            throw t;
        } finally {
            if (notificationsAvailable || auditRequested) {
                AfterHandlingEvent afterHandlingEvent = new AfterHandlingEvent(
                        AuthContextUtils.getDomain(),
                        AuthContextUtils.getWho(),
                        OpEvent.CategoryType.LOGIC,
                        category,
                        null,
                        event,
                        outcome,
                        before,
                        output,
                        input);

                try {
                    notificationManager.createTasks(afterHandlingEvent);
                    auditManager.audit(afterHandlingEvent);
                } catch (Throwable t) {
                    LOG.error("While managing Audit and Notifications", t);
                }
            }
        }
    }
}
