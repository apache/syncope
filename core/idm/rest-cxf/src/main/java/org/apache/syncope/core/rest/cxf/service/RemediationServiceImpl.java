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

import java.time.OffsetDateTime;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.rest.api.beans.RemediationQuery;
import org.apache.syncope.common.rest.api.service.RemediationService;
import org.apache.syncope.core.logic.RemediationLogic;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.springframework.stereotype.Service;

@Service
public class RemediationServiceImpl extends AbstractService implements RemediationService {

    protected final RemediationLogic logic;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    public RemediationServiceImpl(
            final RemediationLogic logic,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO) {

        this.logic = logic;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
    }

    @Override
    public PagedResult<RemediationTO> list(final RemediationQuery query) {
        Pair<Integer, List<RemediationTO>> result = logic.list(
                query.getPage(), query.getSize(), getOrderByClauses(query.getOrderBy()));
        return buildPagedResult(result.getRight(), query.getPage(), query.getSize(), result.getLeft());
    }

    @Override
    public RemediationTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public Response delete(final String key) {
        logic.delete(key);
        return Response.noContent().build();
    }

    @Override
    public Response remedy(final String remediationKey, final AnyCR anyCR) {
        ProvisioningResult<?> created = logic.remedy(remediationKey, anyCR, isNullPriorityAsync());
        return createResponse(created);
    }

    private void check(final String key, final String anyKey) {
        RemediationTO remediation = logic.read(key);

        AnyDAO<?> anyDAO;
        switch (remediation.getAnyType()) {
            case "USER":
                anyDAO = userDAO;
                break;

            case "GROUP":
                anyDAO = groupDAO;
                break;

            default:
                anyDAO = anyObjectDAO;
        }

        OffsetDateTime etag = anyDAO.findLastChange(anyKey);
        if (etag == null) {
            throw new NotFoundException(remediation.getAnyType() + " for " + key);
        }
        checkETag(String.valueOf(etag.toInstant().toEpochMilli()));
    }

    @Override
    public Response remedy(final String remediationKey, final AnyUR anyUR) {
        check(remediationKey, anyUR.getKey());

        ProvisioningResult<?> updated = logic.remedy(remediationKey, anyUR, isNullPriorityAsync());
        return modificationResponse(updated);
    }

    @Override
    public Response remedy(final String remediationKey, final String anyKey) {
        check(remediationKey, anyKey);

        ProvisioningResult<?> deleted = logic.remedy(remediationKey, anyKey, isNullPriorityAsync());
        return modificationResponse(deleted);
    }

}
