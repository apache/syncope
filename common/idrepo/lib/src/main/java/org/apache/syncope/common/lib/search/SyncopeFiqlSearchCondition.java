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
package org.apache.syncope.common.lib.search;

import java.util.Map;
import org.apache.cxf.jaxrs.ext.search.Beanspector;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SimpleSearchCondition;

/**
 * Adds support for custom FIQL operators to {@link SimpleSearchCondition}.
 *
 * @param <T> type of search condition.
 */
public class SyncopeFiqlSearchCondition<T> extends SimpleSearchCondition<T> {

    static {
        SUPPORTED_TYPES.add(ConditionType.CUSTOM);
    }

    private String operator;

    public SyncopeFiqlSearchCondition(final ConditionType cType, final T condition) {
        super(cType, condition);
    }

    public SyncopeFiqlSearchCondition(
            final Map<String, ConditionType> getters2operators,
            final Map<String, String> realGetters,
            final Map<String, Beanspector.TypeInfo> propertyTypeInfo,
            final T condition,
            final String operator) {

        super(getters2operators, realGetters, propertyTypeInfo, condition);
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }
}
