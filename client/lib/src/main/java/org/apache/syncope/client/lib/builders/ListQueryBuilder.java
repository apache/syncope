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

import org.apache.syncope.common.rest.api.beans.ListQuery;

public class ListQueryBuilder {

    private final ListQuery instance = new ListQuery();

    public ListQueryBuilder page(final Integer page) {
        instance.setPage(page);

        return this;
    }

    public ListQueryBuilder size(final Integer size) {
        instance.setSize(size);

        return this;
    }

    public ListQueryBuilder orderBy(final String orderBy) {
        instance.setOrderBy(orderBy);

        return this;
    }

    public ListQueryBuilder details(final boolean details) {
        instance.setDetails(details);

        return this;
    }

    public ListQuery build() {
        return instance;
    }
}
