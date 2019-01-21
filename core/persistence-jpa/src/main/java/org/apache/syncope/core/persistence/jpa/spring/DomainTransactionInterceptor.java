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
package org.apache.syncope.core.persistence.jpa.spring;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Extends the standard {@link TransactionInterceptor} by dynamically setting the appropriate
 * {@link TransactionAttributeSource} qualifier according to the authentication domain of the caller - retrieved via
 * {@link AuthContextUtils#getDomain()}.
 *
 * @see DomainTransactionAnnotationParser
 */
public class DomainTransactionInterceptor extends TransactionInterceptor {

    private static final long serialVersionUID = 5113728988680448551L;

    private static final Logger LOG = LoggerFactory.getLogger(DomainTransactionInterceptor.class);

    @Override
    public TransactionAttributeSource getTransactionAttributeSource() {
        return new AnnotationTransactionAttributeSource(new DomainTransactionAnnotationParser());
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        try {
            return super.invoke(invocation);
        } catch (Throwable e) {
            LOG.debug("Error during {} invocation", invocation.getMethod(), e);
            throw e;
        }
    }
}
