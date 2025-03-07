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

import java.util.Collections;
import java.util.Map;
import org.apache.cxf.jaxrs.ext.search.client.FiqlSearchConditionBuilder;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

/**
 * Extends {@link AbstractFiqlSearchConditionBuilder} by providing some additional facilities for searching
 * any objects in Syncope.
 */
public class AnyObjectFiqlSearchConditionBuilder extends AbstractFiqlSearchConditionBuilder<
        AnyObjectProperty, AnyObjectPartialCondition, AnyObjectCompleteCondition> {

    private static final long serialVersionUID = -3248603004632741935L;

    private final String type;

    public AnyObjectFiqlSearchConditionBuilder(final String type) {
        super();
        this.type = type;
    }

    @Override
    protected Builder newBuilderInstance() {
        return new Builder(properties);
    }

    @Override
    public String query() {
        return new FiqlSearchConditionBuilder.Builder(Collections.emptyMap()).
                is(SpecialAttr.TYPE.toString()).equalTo(type).query();
    }

    @Override
    public AnyObjectProperty is(final String property) {
        return newBuilderInstance().is(property);
    }

    public AnyObjectCompleteCondition inGroups(final String group, final String... moreGroups) {
        return newBuilderInstance().
                is(SpecialAttr.GROUPS.toString()).
                inGroups(group, moreGroups);
    }

    public AnyObjectCompleteCondition notInGroups(final String group, final String... moreGroups) {
        return newBuilderInstance().
                is(SpecialAttr.GROUPS.toString()).
                notInGroups(group, moreGroups);
    }

    public AnyObjectCompleteCondition inRelationships(final String anyObject, final String... moreAnyObjects) {
        return newBuilderInstance().
                is(SpecialAttr.RELATIONSHIPS.toString()).
                inRelationships(anyObject, moreAnyObjects);
    }

    public AnyObjectCompleteCondition notInRelationships(final String anyObject, final String... moreAnyObjects) {
        return newBuilderInstance().
                is(SpecialAttr.RELATIONSHIPS.toString()).
                notInRelationships(anyObject, moreAnyObjects);
    }

    public AnyObjectCompleteCondition inRelationshipTypes(final String type, final String... moreTypes) {
        return newBuilderInstance().
                is(SpecialAttr.RELATIONSHIP_TYPES.toString()).
                inRelationshipTypes(type, moreTypes);
    }

    public AnyObjectCompleteCondition notInRelationshipTypes(final String type, final String... moreTypes) {
        return newBuilderInstance().
                is(SpecialAttr.RELATIONSHIP_TYPES.toString()).
                notInRelationshipTypes(type, moreTypes);
    }

    protected class Builder extends AbstractFiqlSearchConditionBuilder.Builder<
            AnyObjectProperty, AnyObjectPartialCondition, AnyObjectCompleteCondition>
            implements AnyObjectProperty, AnyObjectPartialCondition, AnyObjectCompleteCondition {

        public Builder(final Map<String, String> properties) {
            super(properties);
        }

        public Builder(final Builder parent) {
            super(parent);
        }

        @Override
        public String query() {
            FiqlSearchConditionBuilder.Builder b = new FiqlSearchConditionBuilder.Builder(this);
            return b.and(SpecialAttr.TYPE.toString()).equalTo(type).query();
        }

        @Override
        public AnyObjectProperty is(final String property) {
            Builder b = new Builder(this);
            b.result = property;
            return b;
        }

        @Override
        public AnyObjectCompleteCondition inGroups(final String group, final String... moreGroups) {
            this.result = SpecialAttr.GROUPS.toString();
            return condition(FiqlParser.EQ, group, (Object[]) moreGroups);
        }

        @Override
        public AnyObjectCompleteCondition notInGroups(final String group, final String... moreGroups) {
            this.result = SpecialAttr.GROUPS.toString();
            return condition(FiqlParser.NEQ, group, (Object[]) moreGroups);
        }

        @Override
        public AnyObjectCompleteCondition inRelationships(final String anyObject, final String... moreAnyObjects) {
            this.result = SpecialAttr.RELATIONSHIPS.toString();
            return condition(FiqlParser.EQ, anyObject, (Object[]) moreAnyObjects);
        }

        @Override
        public AnyObjectCompleteCondition notInRelationships(final String group, final String... moreRelationships) {
            this.result = SpecialAttr.RELATIONSHIPS.toString();
            return condition(FiqlParser.NEQ, group, (Object[]) moreRelationships);
        }

        @Override
        public AnyObjectCompleteCondition inRelationshipTypes(final String type, final String... moreTypes) {
            this.result = SpecialAttr.RELATIONSHIP_TYPES.toString();
            return condition(FiqlParser.EQ, type, (Object[]) moreTypes);
        }

        @Override
        public AnyObjectCompleteCondition notInRelationshipTypes(final String type, final String... moreTypes) {
            this.result = SpecialAttr.RELATIONSHIP_TYPES.toString();
            return condition(FiqlParser.NEQ, type, (Object[]) moreTypes);
        }
    }
}
