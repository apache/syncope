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
import org.apache.syncope.common.lib.wa.U2FDevice;
import org.apache.syncope.common.rest.api.beans.U2FDeviceQuery;
import org.apache.syncope.common.rest.api.service.wa.U2FRegistrationService;
import org.apache.syncope.core.logic.wa.U2FRegistrationLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractService;
import org.springframework.stereotype.Service;

@Service
public class U2FRegistrationServiceImpl extends AbstractService implements U2FRegistrationService {

    protected final U2FRegistrationLogic logic;

    public U2FRegistrationServiceImpl(final U2FRegistrationLogic logic) {
        this.logic = logic;
    }

    @Override
    public void delete(final U2FDeviceQuery query) {
        logic.delete(query.getId(), query.getExpirationDate());
    }

    @Override
    public void create(final String owner, final U2FDevice device) {
        logic.create(owner, device);
    }

    @Override
    public PagedResult<U2FDevice> search(final U2FDeviceQuery query) {
        Pair<Integer, List<U2FDevice>> result = logic.search(
                query.getPage(),
                query.getSize(),
                query.getId(),
                query.getExpirationDate(),
                getOrderByClauses(query.getOrderBy()));
        return buildPagedResult(result.getRight(), query.getPage(), query.getSize(), result.getLeft());
    }
}
