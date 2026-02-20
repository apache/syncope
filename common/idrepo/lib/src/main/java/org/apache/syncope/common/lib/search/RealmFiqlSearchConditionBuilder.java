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

public class RealmFiqlSearchConditionBuilder
        extends AbstractFiqlSearchConditionBuilder<RealmProperty, RealmPartialCondition, RealmCompleteCondition> {

    private static final long serialVersionUID = 324753886224642253L;

    protected static class Builder extends AbstractFiqlSearchConditionBuilder.Builder<
            RealmProperty, RealmPartialCondition, RealmCompleteCondition>
            implements RealmProperty, RealmPartialCondition, RealmCompleteCondition {

        public Builder(final Map<String, String> properties) {
            super(properties);
        }

        public Builder(final RealmFiqlSearchConditionBuilder.Builder parent) {
            super(parent);
        }
    }

    @Override
    protected Builder newBuilderInstance() {
        return new Builder(properties);
    }

    @Override
    public RealmProperty is(final String property) {
        return newBuilderInstance().is(property);
    }
}
