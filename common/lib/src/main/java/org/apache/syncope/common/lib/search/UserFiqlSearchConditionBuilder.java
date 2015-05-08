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
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

/**
 * Extends {@link AbstractFiqlSearchConditionBuilder} by providing some additional facilities for searching
 * users in Syncope.
 */
public class UserFiqlSearchConditionBuilder extends AbstractFiqlSearchConditionBuilder {

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

    public CompleteCondition inGroups(final Long group, final Long... moreGroups) {
        return newBuilderInstance().is(SpecialAttr.GROUPS.toString()).inGroups(group, moreGroups);
    }

    public CompleteCondition inRoles(final Long role, final Long... moreRoles) {
        return newBuilderInstance().is(SpecialAttr.ROLES.toString()).inRoles(role, moreRoles);
    }

    public CompleteCondition notInGroups(final Long group, final Long... moreGroups) {
        return newBuilderInstance().is(SpecialAttr.GROUPS.toString()).notInGroups(group, moreGroups);
    }

    @Override
    public CompleteCondition hasResources(final String resource, final String... moreResources) {
        return newBuilderInstance().is(SpecialAttr.RESOURCES.toString()).hasResources(resource, moreResources);
    }

    @Override
    public CompleteCondition hasNotResources(final String resource, final String... moreResources) {
        return newBuilderInstance().is(SpecialAttr.RESOURCES.toString()).hasNotResources(resource, moreResources);
    }

    protected static class Builder extends AbstractFiqlSearchConditionBuilder.Builder
            implements UserProperty, CompleteCondition {

        public Builder(final Map<String, String> properties) {
            super(properties);
        }

        public Builder(final Builder parent) {
            super(parent);
        }

        @Override
        public UserProperty is(final String property) {
            Builder builder = new Builder(this);
            builder.result = property;
            return builder;
        }

        @Override
        public CompleteCondition inGroups(final Long group, final Long... moreGroups) {
            this.result = SpecialAttr.GROUPS.toString();
            return condition(FiqlParser.EQ, group, (Object[]) moreGroups);
        }

        @Override
        public CompleteCondition notInGroups(final Long group, final Long... moreGroups) {
            this.result = SpecialAttr.GROUPS.toString();
            return condition(FiqlParser.NEQ, group, (Object[]) moreGroups);
        }

        @Override
        public CompleteCondition inRoles(final Long role, final Long... moreRoles) {
            this.result = SpecialAttr.ROLES.toString();
            return condition(FiqlParser.EQ, role, (Object[]) moreRoles);
        }

        @Override
        public CompleteCondition notInRoles(final Long role, final Long... moreRoles) {
            this.result = SpecialAttr.ROLES.toString();
            return condition(FiqlParser.NEQ, role, (Object[]) moreRoles);
        }
    }
}
