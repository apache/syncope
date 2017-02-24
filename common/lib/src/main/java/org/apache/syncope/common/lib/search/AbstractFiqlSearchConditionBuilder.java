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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.cxf.jaxrs.ext.search.client.FiqlSearchConditionBuilder;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

public abstract class AbstractFiqlSearchConditionBuilder extends FiqlSearchConditionBuilder implements Serializable {

    private static final long serialVersionUID = 9043884238032703381L;

    public static final Map<String, String> CONTEXTUAL_PROPERTIES = new HashMap<String, String>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(SearchUtils.LAX_PROPERTY_MATCH, "true");
        }
    };

    protected AbstractFiqlSearchConditionBuilder() {
        super();
    }

    protected AbstractFiqlSearchConditionBuilder(final Map<String, String> properties) {
        super(properties);
    }

    @Override
    protected Builder newBuilderInstance() {
        return new Builder(properties);
    }

    @Override
    public SyncopeProperty is(final String property) {
        return newBuilderInstance().is(property);
    }

    public CompleteCondition isNull(final String property) {
        return newBuilderInstance().is(property).nullValue();
    }

    public CompleteCondition isNotNull(final String property) {
        return newBuilderInstance().is(property).notNullValue();
    }

    public CompleteCondition hasResources(final String resource, final String... moreResources) {
        return newBuilderInstance().is(SpecialAttr.RESOURCES.toString()).hasResources(resource, moreResources);
    }

    public CompleteCondition hasNotResources(final String resource, final String... moreResources) {
        return newBuilderInstance().is(SpecialAttr.RESOURCES.toString()).hasNotResources(resource, moreResources);
    }

    protected static class Builder extends FiqlSearchConditionBuilder.Builder
            implements SyncopeProperty, CompleteCondition {

        protected Builder(final Map<String, String> properties) {
            super(properties);
        }

        protected Builder(final Builder parent) {
            super(parent);
        }

        @Override
        public SyncopeProperty is(final String property) {
            Builder b = new Builder(this);
            b.result = property;
            return b;
        }

        @Override
        public CompleteCondition nullValue() {
            return condition(FiqlParser.EQ, SpecialAttr.NULL);
        }

        @Override
        public CompleteCondition notNullValue() {
            return condition(FiqlParser.NEQ, SpecialAttr.NULL);
        }

        @Override
        public CompleteCondition hasResources(final String resource, final String... moreResources) {
            this.result = SpecialAttr.RESOURCES.toString();
            return condition(FiqlParser.EQ, resource, (Object[]) moreResources);
        }

        @Override
        public CompleteCondition hasNotResources(final String resource, final String... moreResources) {
            this.result = SpecialAttr.RESOURCES.toString();
            return condition(FiqlParser.NEQ, resource, (Object[]) moreResources);
        }

        @Override
        public CompleteCondition equalToIgnoreCase(final String value, final String... moreValues) {
            return condition(SyncopeFiqlParser.IEQ, value, (Object[]) moreValues);
        }

        @Override
        public CompleteCondition notEqualTolIgnoreCase(final String literalOrPattern) {
            return condition(SyncopeFiqlParser.NIEQ, literalOrPattern);
        }
    }
}
