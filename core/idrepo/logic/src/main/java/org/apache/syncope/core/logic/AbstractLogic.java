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
import org.apache.syncope.common.lib.to.EntityTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Superclass for all business logic modules.
 *
 * @param <T> transfer object used for input / output
 */
public abstract class AbstractLogic<T extends EntityTO> {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractLogic.class);

    /**
     * Resolves stored bean (if existing) referred by the given CUD method.
     * Read-only methods will be unresolved for performance reasons.
     *
     * @param method method.
     * @param args method arguments.
     * @return referred stored bean.
     * @throws UnresolvedReferenceException in case of failures, read-only methods and unresolved bean.
     */
    public T resolveBeanReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        final Transactional transactional = method.getAnnotation(Transactional.class);
        if (transactional != null && transactional.readOnly()) {
            throw new UnresolvedReferenceException();
        }
        return resolveReference(method, args);
    }

    protected abstract T resolveReference(Method method, Object... args) throws UnresolvedReferenceException;
}
