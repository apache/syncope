/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.core.services;

import java.lang.reflect.Method;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.rest.controller.AbstractController;
import org.apache.syncope.core.rest.controller.UnresolvedReferenceException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Aspect
public class ControllerHandler {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ControllerHandler.class);

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private AuditManager auditManager;

    @Around("execution(* org.apache.syncope.core.rest.controller.AbstractController+.*(..))")
    public Object around(final ProceedingJoinPoint joinPoint) throws Throwable {
        final Class<?> clazz = joinPoint.getTarget().getClass();

        final Object[] input = joinPoint.getArgs();

        final String category = clazz.getSimpleName();

        final MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method method = ms.getMethod();

        final String event = joinPoint.getSignature().getName();

        AuditElements.Result result = null;
        Object output = null;
        Object before = null;

        try {
            LOG.debug("Before {}.{}({})",
                    new Object[] {clazz.getSimpleName(), event, input == null || input.length == 0 ? "" : "..."});

            try {
                before = ((AbstractController) joinPoint.getTarget()).resolveBeanReference(method, input);
            } catch (UnresolvedReferenceException ignore) {
                LOG.debug("Unresolved bean reference ...");
            }

            output = joinPoint.proceed();
            result = AuditElements.Result.SUCCESS;

            LOG.debug("After returning {}.{}", clazz.getSimpleName(), event);
            return output;
        } catch (Throwable t) {
            output = t;
            result = AuditElements.Result.FAILURE;

            LOG.debug("After throwing {}.{}", clazz.getSimpleName(), event);
            throw t;
        } finally {
            notificationManager.createTasks(
                    AuditElements.EventCategoryType.REST,
                    category,
                    null,
                    event,
                    result,
                    before,
                    output,
                    input);

            auditManager.audit(
                    AuditElements.EventCategoryType.REST,
                    category,
                    null,
                    event,
                    result,
                    before,
                    output,
                    input);
        }
    }
}
