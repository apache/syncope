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
package org.apache.syncope.core.rest.cxf.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.search.AnyObjectFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.AnyObjectLogic;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnyObjectServiceImpl extends AbstractAnyService<AnyObjectTO, AnyObjectPatch> implements AnyObjectService {

    @Autowired
    private AnyObjectLogic logic;

    @Override
    protected AbstractAnyLogic<AnyObjectTO, AnyObjectPatch> getAnyLogic() {
        return logic;
    }

    @Override
    protected AnyObjectPatch newPatch(final String key) {
        AnyObjectPatch patch = new AnyObjectPatch();
        patch.setKey(key);
        return patch;
    }

    @Override
    public PagedResult<AnyObjectTO> list(final AnyListQuery listQuery) {
        throw new UnsupportedOperationException("Need to specify " + AnyType.class.getSimpleName());
    }

    @Override
    public PagedResult<AnyObjectTO> list(final String type, final AnyListQuery listQuery) {
        if (StringUtils.isBlank(type)) {
            return super.list(listQuery);
        }

        AnySearchQuery searchQuery = new AnySearchQuery();
        searchQuery.setFiql(new AnyObjectFiqlSearchConditionBuilder(type).query());
        searchQuery.setDetails(listQuery.getDetails());
        searchQuery.setOrderBy(listQuery.getOrderBy());
        searchQuery.setPage(listQuery.getPage());
        searchQuery.setSize(listQuery.getSize());
        searchQuery.setRealm(listQuery.getRealm());

        return search(searchQuery);
    }

}
