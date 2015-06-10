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

import java.util.ArrayList;
import org.apache.syncope.common.rest.api.beans.ListQuery;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;

public class AnyListQueryBuilder extends ListQueryBuilder {

    private final AnyListQuery instance = new AnyListQuery();

    @Override
    public AnyListQueryBuilder page(final Integer page) {
        return AnyListQueryBuilder.class.cast(super.page(page));
    }

    @Override
    public AnyListQueryBuilder size(final Integer size) {
        return AnyListQueryBuilder.class.cast(super.size(size));
    }

    @Override
    public AnyListQueryBuilder orderBy(final String orderBy) {
        return AnyListQueryBuilder.class.cast(super.orderBy(orderBy));
    }

    public AnyListQueryBuilder realm(final String realm) {
        if (instance.getRealms() == null) {
            instance.setRealms(new ArrayList<String>());
        }
        instance.getRealms().add(realm);

        return this;
    }

    @Override
    public AnyListQuery build() {
        ListQuery lq = super.build();
        instance.setPage(lq.getPage());
        instance.setSize(lq.getSize());
        instance.setOrderBy(lq.getOrderBy());

        return instance;
    }
}
