/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.validation.entity;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AOP proxy intercepting DAO calls.
 */
@Component
@Aspect
public class EntityValidationInterceptor {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            EntityValidationInterceptor.class);

    @Autowired
    private Validator validator;

    /**
     * Validate bean prior saving to DB.
     *
     * @param pjp Aspect's ProceedingJoinPoint
     * @return DAO method's return value
     * @throws Throwable if anything goes wrong
     */
    @Around("execution(* org.syncope.core.persistence.dao..*.save(..))")
    public final Object save(final ProceedingJoinPoint pjp)
            throws Throwable {

        Set<ConstraintViolation<Object>> violations =
                validator.validate(pjp.getArgs()[0]);
        if (!violations.isEmpty()) {
            LOG.error("Bean validation errors found: {}", violations);
            throw new InvalidEntityException(violations);
        }

        return pjp.proceed();
    }
}
