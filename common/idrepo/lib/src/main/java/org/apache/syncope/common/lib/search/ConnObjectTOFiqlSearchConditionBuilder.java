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

/**
 * Extends {@link AbstractFiqlSearchConditionBuilder} by providing some additional facilities for searching
 * connector objects.
 */
public class ConnObjectTOFiqlSearchConditionBuilder extends AbstractFiqlSearchConditionBuilder {

    private static final long serialVersionUID = 4983742159694010935L;

    @Override
    protected Builder newBuilderInstance() {
        return new Builder(properties);
    }

    @Override
    public SyncopeProperty is(final String property) {
        return newBuilderInstance().is(property);
    }

    protected class Builder extends AbstractFiqlSearchConditionBuilder.Builder
            implements SyncopeProperty, CompleteCondition {

        public Builder(final Map<String, String> properties) {
            super(properties);
        }

        public Builder(final Builder parent) {
            super(parent);
        }

        @Override
        public SyncopeProperty is(final String property) {
            Builder b = new Builder(this);
            b.result = property;
            return b;
        }

        @Override
        public CompleteCondition inDynRealms(final String dynRealm, final String... moreDynRealms) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompleteCondition notInDynRealms(final String dynRealm, final String... moreDynRealms) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompleteCondition hasResources(final String resource, final String... moreResources) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompleteCondition hasNotResources(final String resource, final String... moreResources) {
            throw new UnsupportedOperationException();
        }
    }
}
