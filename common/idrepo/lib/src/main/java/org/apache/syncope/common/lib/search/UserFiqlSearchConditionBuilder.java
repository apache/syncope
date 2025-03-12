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
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

/**
 * Extends {@link AbstractFiqlSearchConditionBuilder} by providing some additional facilities for searching
 * users in Syncope.
 */
public class UserFiqlSearchConditionBuilder
        extends AbstractFiqlSearchConditionBuilder<UserProperty, UserPartialCondition, UserCompleteCondition> {

    private static final long serialVersionUID = 3485708634448845774L;

    @Override
    protected Builder newBuilderInstance() {
        return new Builder(properties);
    }

    @Override
    public UserProperty is(final String property) {
        return newBuilderInstance().is(property);
    }

    public UserCompleteCondition inGroups(final String group, final String... moreGroups) {
        return newBuilderInstance().
                is(SpecialAttr.GROUPS.toString()).
                inGroups(group, moreGroups);
    }

    public UserCompleteCondition notInGroups(final String group, final String... moreGroups) {
        return newBuilderInstance().
                is(SpecialAttr.GROUPS.toString()).
                notInGroups(group, moreGroups);
    }

    public UserCompleteCondition inRelationships(final String anyObject, final String... moreAnyObjects) {
        return newBuilderInstance().
                is(SpecialAttr.RELATIONSHIPS.toString()).
                inRelationships(anyObject, moreAnyObjects);
    }

    public UserCompleteCondition notInRelationships(final String anyObject, final String... moreAnyObjects) {
        return newBuilderInstance().
                is(SpecialAttr.RELATIONSHIPS.toString()).
                notInRelationships(anyObject, moreAnyObjects);
    }

    public UserCompleteCondition inRelationshipTypes(final String type, final String... moreTypes) {
        return newBuilderInstance().
                is(SpecialAttr.RELATIONSHIP_TYPES.toString()).
                inRelationshipTypes(type, moreTypes);
    }

    public UserCompleteCondition notInRelationshipTypes(final String type, final String... moreTypes) {
        return newBuilderInstance().
                is(SpecialAttr.RELATIONSHIP_TYPES.toString()).
                notInRelationshipTypes(type, moreTypes);
    }

    public UserCompleteCondition inRoles(final String role, final String... moreRoles) {
        return newBuilderInstance().
                is(SpecialAttr.ROLES.toString()).
                inRoles(role, moreRoles);
    }

    public UserCompleteCondition notInRoles(final String role, final String... moreRoles) {
        return newBuilderInstance().
                is(SpecialAttr.ROLES.toString()).
                notInRoles(role, moreRoles);
    }

    protected static class Builder extends AbstractFiqlSearchConditionBuilder.Builder<
            UserProperty, UserPartialCondition, UserCompleteCondition>
            implements UserProperty, UserPartialCondition, UserCompleteCondition {

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
        public UserCompleteCondition inGroups(final String group, final String... moreGroups) {
            this.result = SpecialAttr.GROUPS.toString();
            return condition(FiqlParser.EQ, group, (Object[]) moreGroups);
        }

        @Override
        public UserCompleteCondition notInGroups(final String group, final String... moreGroups) {
            this.result = SpecialAttr.GROUPS.toString();
            return condition(FiqlParser.NEQ, group, (Object[]) moreGroups);
        }

        @Override
        public UserCompleteCondition inRelationships(final String anyObject, final String... moreAnyObjects) {
            this.result = SpecialAttr.RELATIONSHIPS.toString();
            return condition(FiqlParser.EQ, anyObject, (Object[]) moreAnyObjects);
        }

        @Override
        public UserCompleteCondition notInRelationships(final String anyObject, final String... moreAnyObjects) {
            this.result = SpecialAttr.RELATIONSHIPS.toString();
            return condition(FiqlParser.NEQ, anyObject, (Object[]) moreAnyObjects);
        }

        @Override
        public UserCompleteCondition inRelationshipTypes(final String type, final String... moreTypes) {
            this.result = SpecialAttr.RELATIONSHIP_TYPES.toString();
            return condition(FiqlParser.EQ, type, (Object[]) moreTypes);
        }

        @Override
        public UserCompleteCondition notInRelationshipTypes(final String type, final String... moreTypes) {
            this.result = SpecialAttr.RELATIONSHIP_TYPES.toString();
            return condition(FiqlParser.NEQ, type, (Object[]) moreTypes);
        }

        @Override
        public UserCompleteCondition inRoles(final String role, final String... moreRoles) {
            this.result = SpecialAttr.ROLES.toString();
            return condition(FiqlParser.EQ, role, (Object[]) moreRoles);
        }

        @Override
        public UserCompleteCondition notInRoles(final String role, final String... moreRoles) {
            this.result = SpecialAttr.ROLES.toString();
            return condition(FiqlParser.NEQ, role, (Object[]) moreRoles);
        }
    }
}
