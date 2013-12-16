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
 * roles in Syncope.
 */
public class RoleFiqlSearchConditionBuilder extends SyncopeFiqlSearchConditionBuilder {

    public RoleFiqlSearchConditionBuilder() {
        super();
    }

    public RoleFiqlSearchConditionBuilder(final Map<String, String> properties) {
        super(properties);
    }

    @Override
    protected Builder newBuilderInstance() {
        return new Builder(properties);
    }

    @Override
    public RoleProperty is(final String property) {
        return newBuilderInstance().is(property);
    }

    public CompleteCondition hasEntitlements(final String entitlement, final String... moreEntitlements) {
        return newBuilderInstance().is(SpecialAttr.ENTITLEMENTS.toString()).
                hasEntitlements(entitlement, moreEntitlements);
    }

    public CompleteCondition hasNotEntitlements(final String entitlement, final String... moreEntitlements) {
        return newBuilderInstance().is(SpecialAttr.ENTITLEMENTS.toString()).
                hasNotEntitlements(entitlement, moreEntitlements);
    }

    protected static class Builder extends SyncopeFiqlSearchConditionBuilder.Builder
            implements RoleProperty, CompleteCondition {

        public Builder(final Map<String, String> properties) {
            super(properties);
        }

        public Builder(final Builder parent) {
            super(parent);
        }

        @Override
        public RoleProperty is(final String property) {
            Builder b = new Builder(this);
            b.result = property;
            return b;
        }

        @Override
        public CompleteCondition hasEntitlements(final String entitlement, final String... moreEntitlements) {
            this.result = SpecialAttr.ENTITLEMENTS.toString();
            return condition(FiqlParser.EQ, entitlement, (Object[]) moreEntitlements);
        }

        @Override
        public CompleteCondition hasNotEntitlements(final String entitlement, final String... moreEntitlements) {
            this.result = SpecialAttr.ENTITLEMENTS.toString();
            return condition(FiqlParser.NEQ, entitlement, (Object[]) moreEntitlements);
        }

    }
}
