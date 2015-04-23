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

import org.apache.syncope.common.rest.api.beans.SubjectListQuery;
import org.apache.syncope.common.rest.api.beans.SubjectSearchQuery;

public class SubjectSearchQueryBuilder extends SubjectListQueryBuilder {

    private final SubjectSearchQuery instance = new SubjectSearchQuery();

    @Override
    public SubjectSearchQueryBuilder realm(final String realm) {
        return SubjectSearchQueryBuilder.class.cast(super.realm(realm));
    }

    @Override
    public SubjectSearchQueryBuilder page(final Integer page) {
        return SubjectSearchQueryBuilder.class.cast(super.page(page));
    }

    @Override
    public SubjectSearchQueryBuilder size(final Integer size) {
        return SubjectSearchQueryBuilder.class.cast(super.size(size));
    }

    @Override
    public SubjectSearchQueryBuilder orderBy(final String orderBy) {
        return SubjectSearchQueryBuilder.class.cast(super.orderBy(orderBy));
    }

    public SubjectSearchQueryBuilder fiql(final String fiql) {
        instance.setFiql(fiql);

        return this;
    }

    @Override
    public SubjectSearchQuery build() {
        SubjectListQuery slq = super.build();
        instance.setRealms(slq.getRealms());
        instance.setPage(slq.getPage());
        instance.setSize(slq.getSize());
        instance.setOrderBy(slq.getOrderBy());

        return instance;
    }
}
