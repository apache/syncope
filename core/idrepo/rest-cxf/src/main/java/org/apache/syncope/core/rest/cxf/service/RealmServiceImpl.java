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

import java.net.URI;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.core.logic.RealmLogic;
import org.springframework.stereotype.Service;

@Service
public class RealmServiceImpl extends AbstractService implements RealmService {

    protected final RealmLogic logic;

    public RealmServiceImpl(final RealmLogic logic) {
        this.logic = logic;
    }

    @Override
    public PagedResult<RealmTO> search(final RealmQuery query) {
        String keyword = query.getKeyword() == null ? null : query.getKeyword().replace('*', '%');

        Pair<Integer, List<RealmTO>> result = logic.search(keyword, query.getBase());
        return buildPagedResult(result.getRight(), 1, result.getRight().size(), result.getLeft());
    }

    @Override
    public List<RealmTO> list(final String fullPath) {
        return logic.list(StringUtils.prependIfMissing(fullPath, SyncopeConstants.ROOT_REALM));
    }

    @Override
    public Response create(final String parentPath, final RealmTO realmTO) {
        ProvisioningResult<RealmTO> created =
                logic.create(StringUtils.prependIfMissing(parentPath, SyncopeConstants.ROOT_REALM), realmTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getEntity().getName()).build();
        Response.ResponseBuilder builder = Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created.getEntity().getFullPath());

        return applyPreference(created, builder).build();
    }

    @Override
    public Response update(final RealmTO realmTO) {
        realmTO.setFullPath(StringUtils.prependIfMissing(realmTO.getFullPath(), SyncopeConstants.ROOT_REALM));
        ProvisioningResult<RealmTO> updated = logic.update(realmTO);
        return modificationResponse(updated);
    }

    @Override
    public Response delete(final String fullPath) {
        ProvisioningResult<RealmTO> deleted =
                logic.delete(StringUtils.prependIfMissing(fullPath, SyncopeConstants.ROOT_REALM));
        return modificationResponse(deleted);
    }
}
