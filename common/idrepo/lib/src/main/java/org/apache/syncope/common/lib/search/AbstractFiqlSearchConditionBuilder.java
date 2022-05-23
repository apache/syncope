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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.cxf.jaxrs.ext.search.client.FiqlSearchConditionBuilder;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.syncope.common.lib.BaseBean;

public abstract class AbstractFiqlSearchConditionBuilder<
        P extends SyncopeProperty<C>,
        PA extends SyncopePartialCondition<P, C>, 
        C extends SyncopeCompleteCondition<PA, P>>
        extends FiqlSearchConditionBuilder implements BaseBean {

    private static final long serialVersionUID = 9043884238032703381L;

    public static final Map<String, String> CONTEXTUAL_PROPERTIES = new HashMap<>() {

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
    protected Builder<P, PA, C> newBuilderInstance() {
        return new Builder<>(properties);
    }

    @Override
    public P is(final String property) {
        return newBuilderInstance().is(property);
    }

    public C isNull(final String property) {
        return newBuilderInstance().is(property).nullValue();
    }

    public C isNotNull(final String property) {
        return newBuilderInstance().is(property).notNullValue();
    }

    public C inDynRealms(final String dynRealm, final String... moreDynRealms) {
        return newBuilderInstance().
                is(SpecialAttr.DYNREALMS.toString()).
                inDynRealms(dynRealm, moreDynRealms);
    }

    public C notInDynRealms(final String dynRealm, final String... moreDynRealms) {
        return newBuilderInstance().
                is(SpecialAttr.DYNREALMS.toString()).
                notInDynRealms(dynRealm, moreDynRealms);
    }

    public C hasAuxClasses(final String auxClass, final String... moreAuxClasses) {
        return newBuilderInstance().is(SpecialAttr.AUX_CLASSES.toString()).hasAuxClasses(auxClass, moreAuxClasses);
    }

    public C hasNotAuxClasses(final String auxClass, final String... moreAuxClasses) {
        return newBuilderInstance().is(SpecialAttr.AUX_CLASSES.toString()).hasNotAuxClasses(auxClass, moreAuxClasses);
    }

    public C hasResources(final String resource, final String... moreResources) {
        return newBuilderInstance().is(SpecialAttr.RESOURCES.toString()).hasResources(resource, moreResources);
    }

    public C hasNotResources(final String resource, final String... moreResources) {
        return newBuilderInstance().is(SpecialAttr.RESOURCES.toString()).hasNotResources(resource, moreResources);
    }

    @SuppressWarnings("unchecked")
    protected static class Builder<
            P extends SyncopeProperty<C>,
            PA extends SyncopePartialCondition<P, C>,
            C extends SyncopeCompleteCondition<PA, P>>
            extends FiqlSearchConditionBuilder.Builder
            implements SyncopeProperty<C>, SyncopeCompleteCondition<PA, P>, SyncopePartialCondition<P, C> {

        protected Builder(final Map<String, String> properties) {
            super(properties);
        }

        protected Builder(final Builder<P, PA, C> parent) {
            super(parent);
        }

        @Override
        public P is(final String property) {
            Builder<P, PA, C> b = new Builder<>(this);
            b.result = property;
            return (P) b;
        }

        @Override
        protected C condition(
                final String operator, final Object value, final Object... moreValues) {

            super.condition(operator, value, moreValues);
            return (C) this;
        }

        @Override
        public C equalToIgnoreCase(final String value, final String... moreValues) {
            return condition(SyncopeFiqlParser.IEQ, value, (Object[]) moreValues);
        }

        @Override
        public C notEqualTolIgnoreCase(final String literalOrPattern) {
            return condition(SyncopeFiqlParser.NIEQ, literalOrPattern);
        }

        @Override
        public C nullValue() {
            return condition(FiqlParser.EQ, SpecialAttr.NULL);
        }

        @Override
        public C notNullValue() {
            return condition(FiqlParser.NEQ, SpecialAttr.NULL);
        }

        @Override
        public C hasAuxClasses(final String auxClass, final String... moreAuxClasses) {
            this.result = SpecialAttr.AUX_CLASSES.toString();
            return condition(FiqlParser.EQ, auxClass, (Object[]) moreAuxClasses);
        }

        @Override
        public C hasNotAuxClasses(final String auxClass, final String... moreAuxClasses) {
            this.result = SpecialAttr.AUX_CLASSES.toString();
            return condition(FiqlParser.NEQ, auxClass, (Object[]) moreAuxClasses);
        }

        @Override
        public C hasResources(final String resource, final String... moreResources) {
            this.result = SpecialAttr.RESOURCES.toString();
            return condition(FiqlParser.EQ, resource, (Object[]) moreResources);
        }

        @Override
        public C hasNotResources(final String resource, final String... moreResources) {
            this.result = SpecialAttr.RESOURCES.toString();
            return condition(FiqlParser.NEQ, resource, (Object[]) moreResources);
        }

        @Override
        public C inDynRealms(final String dynRealm, final String... moreDynRealms) {
            this.result = SpecialAttr.DYNREALMS.toString();
            return condition(FiqlParser.EQ, dynRealm, (Object[]) moreDynRealms);
        }

        @Override
        public C notInDynRealms(final String dynRealm, final String... moreDynRealms) {
            this.result = SpecialAttr.DYNREALMS.toString();
            return condition(FiqlParser.NEQ, dynRealm, (Object[]) moreDynRealms);
        }

        @Override
        public PA and() {
            super.and();
            return (PA) this;
        }

        @Override
        public P and(final String name) {
            return and().is(name);
        }

        @Override
        public PA or() {
            super.or();
            return (PA) this;
        }

        @Override
        public P or(final String name) {
            return or().is(name);
        }

        @Override
        public C and(final CompleteCondition cc, final CompleteCondition cc1, final CompleteCondition... cn) {
            super.and(cc1, cc, cn);
            return (C) this;
        }

        @Override
        public C or(final CompleteCondition cc, final CompleteCondition cc1, final CompleteCondition... cn) {
            super.or(cc1, cc, cn);
            return (C) this;
        }

        @Override
        public C and(final List<CompleteCondition> conditions) {
            super.and(conditions);
            return (C) this;
        }

        @Override
        public C or(final List<CompleteCondition> conditions) {
            super.or(conditions);
            return (C) this;
        }
    }
}
