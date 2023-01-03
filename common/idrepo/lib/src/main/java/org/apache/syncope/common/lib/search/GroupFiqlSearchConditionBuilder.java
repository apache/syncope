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
 * groups in Syncope.
 */
public class GroupFiqlSearchConditionBuilder
        extends AbstractFiqlSearchConditionBuilder<GroupProperty, GroupPartialCondition, GroupCompleteCondition> {

    private static final long serialVersionUID = 6275686371606165706L;

    @Override
    protected Builder newBuilderInstance() {
        return new Builder(properties);
    }

    @Override
    public GroupProperty is(final String property) {
        return newBuilderInstance().is(property);
    }

    public GroupCompleteCondition withMembers(final String member, final String... moreMembers) {
        return newBuilderInstance().
                is(SpecialAttr.MEMBER.toString()).
                withMembers(member, moreMembers);
    }

    public GroupCompleteCondition withoutMembers(final String member, final String... moreMembers) {
        return newBuilderInstance().
                is(SpecialAttr.MEMBER.toString()).
                withoutMembers(member, moreMembers);
    }

    protected static class Builder extends AbstractFiqlSearchConditionBuilder.Builder<
            GroupProperty, GroupPartialCondition, GroupCompleteCondition>
            implements GroupProperty, GroupCompleteCondition, GroupPartialCondition {

        public Builder(final Map<String, String> properties) {
            super(properties);
        }

        public Builder(final Builder parent) {
            super(parent);
        }

        @Override
        public GroupProperty is(final String property) {
            Builder b = new Builder(this);
            b.result = property;
            return b;
        }

        @Override
        public GroupCompleteCondition withMembers(final String member, final String... moreMembers) {
            this.result = SpecialAttr.MEMBER.toString();
            return condition(FiqlParser.EQ, member, (Object[]) moreMembers);
        }

        @Override
        public GroupCompleteCondition withoutMembers(final String member, final String... moreMembers) {
            this.result = SpecialAttr.MEMBER.toString();
            return condition(FiqlParser.NEQ, member, (Object[]) moreMembers);
        }
    }
}
