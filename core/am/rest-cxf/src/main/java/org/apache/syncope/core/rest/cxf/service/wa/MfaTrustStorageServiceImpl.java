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
package org.apache.syncope.core.rest.cxf.service.wa;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.rest.api.beans.MfaTrustedDeviceQuery;
import org.apache.syncope.common.rest.api.service.wa.MfaTrustStorageService;
import org.apache.syncope.core.logic.wa.MfaTrusStorageLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractService;
import org.springframework.stereotype.Service;

@Service
public class MfaTrustStorageServiceImpl extends AbstractService implements MfaTrustStorageService {

    protected final MfaTrusStorageLogic logic;

    public MfaTrustStorageServiceImpl(final MfaTrusStorageLogic logic) {
        this.logic = logic;
    }

    @Override
    public PagedResult<MfaTrustedDevice> search(final MfaTrustedDeviceQuery query) {
        Pair<Integer, List<MfaTrustedDevice>> result = logic.search(
                query.getPage(),
                query.getSize(),
                query.getPrincipal(),
                query.getId(),
                query.getRecordDate(),
                getOrderByClauses(query.getOrderBy()));
        return buildPagedResult(result.getRight(), query.getPage(), query.getSize(), result.getLeft());
    }

    @Override
    public void create(final String owner, final MfaTrustedDevice device) {
        logic.create(owner, device);
    }

    @Override
    public void delete(final MfaTrustedDeviceQuery query) {
        logic.delete(query.getExpirationDate(), query.getRecordKey());
    }
}
