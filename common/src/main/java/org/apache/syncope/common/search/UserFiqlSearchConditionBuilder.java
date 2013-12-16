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
package org.apache.syncope.common.search;

import java.util.Map;
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

/**
 * Extends <tt>SyncopeFiqlSearchConditionBuilder</tt> by providing some additional facilities for searching
 * users in Syncope.
 */
public class UserFiqlSearchConditionBuilder extends SyncopeFiqlSearchConditionBuilder {

    public UserFiqlSearchConditionBuilder() {
        super();
    }

    public UserFiqlSearchConditionBuilder(final Map<String, String> properties) {
        super(properties);
    }

    @Override
    protected Builder newBuilderInstance() {
        return new Builder(properties);
    }

    @Override
    public UserProperty is(final String property) {
        return newBuilderInstance().is(property);
    }

    public CompleteCondition hasRoles(final Long role, final Long... moreRoles) {
        return newBuilderInstance().is(SpecialAttr.ROLES.toString()).hasRoles(role, moreRoles);
    }

    public CompleteCondition hasNotRoles(final Long role, final Long... moreRoles) {
        return newBuilderInstance().is(SpecialAttr.ROLES.toString()).hasNotRoles(role, moreRoles);
    }

    public CompleteCondition hasResources(final String resource, final String... moreResources) {
        return newBuilderInstance().is(SpecialAttr.RESOURCES.toString()).hasResources(resource, moreResources);
    }

    public CompleteCondition hasNotResources(final String resource, final String... moreResources) {
        return newBuilderInstance().is(SpecialAttr.RESOURCES.toString()).hasNotResources(resource, moreResources);
    }

    protected static class Builder extends SyncopeFiqlSearchConditionBuilder.Builder
            implements UserProperty, CompleteCondition {

        public Builder(final Map<String, String> properties) {
            super(properties);
        }

        public Builder(final Builder parent) {
            super(parent);
        }

        @Override
        public UserProperty is(final String property) {
            Builder b = new Builder(this);
            b.result = property;
            return b;
        }

        @Override
        public CompleteCondition hasRoles(final Long role, final Long... moreRoles) {
            this.result = SpecialAttr.ROLES.toString();
            return condition(FiqlParser.EQ, role, (Object[]) moreRoles);
        }

        @Override
        public CompleteCondition hasNotRoles(final Long role, final Long... moreRoles) {
            this.result = SpecialAttr.ROLES.toString();
            return condition(FiqlParser.NEQ, role, (Object[]) moreRoles);
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

    }

}
