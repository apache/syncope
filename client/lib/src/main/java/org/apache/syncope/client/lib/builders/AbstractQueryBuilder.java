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
package org.apache.syncope.client.lib.builders;

import org.apache.syncope.common.rest.api.beans.AbstractQuery;

public abstract class AbstractQueryBuilder<Q extends AbstractQuery, B extends AbstractQueryBuilder<Q, B>> {

    private Q instance;

    protected abstract Q newInstance();

    protected Q getInstance() {
        if (instance == null) {
            instance = newInstance();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public B page(final Integer page) {
        getInstance().setPage(page);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B size(final Integer size) {
        getInstance().setSize(size);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B orderBy(final String orderBy) {
        getInstance().setOrderBy(orderBy);
        return (B) this;
    }

    public Q build() {
        return getInstance();
    }
}
