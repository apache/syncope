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

import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.transaction.annotation.SpringTransactionAnnotationParser;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

public class DomainTransactionAnnotationParser extends SpringTransactionAnnotationParser {

    private static final long serialVersionUID = -1490842839439224837L;

    @Override
    protected TransactionAttribute parseTransactionAnnotation(final AnnotationAttributes attributes) {
        RuleBasedTransactionAttribute rbta =
                (RuleBasedTransactionAttribute) super.parseTransactionAnnotation(attributes);
        rbta.setQualifier(AuthContextUtils.getDomain());
        return rbta;
    }
}
