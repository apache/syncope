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

import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.beans.RemediationQuery;
import org.apache.syncope.common.rest.api.service.RemediationService;
import org.apache.syncope.core.logic.RemediationLogic;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.springframework.data.domain.Page;

public class RemediationServiceImpl extends AbstractService implements RemediationService {

    protected final RemediationLogic logic;

    protected final AnyUtilsFactory anyUtilsFactory;

    public RemediationServiceImpl(
            final RemediationLogic logic,
            final AnyUtilsFactory anyUtilsFactory) {

        this.logic = logic;
        this.anyUtilsFactory = anyUtilsFactory;
    }

    @Override
    public PagedResult<RemediationTO> list(final RemediationQuery query) {
        Page<RemediationTO> result = logic.list(
                query.getBefore(),
                query.getAfter(),
                pageable(query));
        return buildPagedResult(result);
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

        AnyTypeKind anyTypeKind = AnyTypeKind.USER.name().equals(remediation.getAnyType())
                ? AnyTypeKind.USER
                : AnyTypeKind.GROUP.name().equals(remediation.getAnyType())
                ? AnyTypeKind.GROUP
                : AnyTypeKind.ANY_OBJECT;

        OffsetDateTime etag = anyUtilsFactory.getInstance(anyTypeKind).dao().findLastChange(anyKey).
                orElseThrow(() -> new NotFoundException(remediation.getAnyType() + " for " + key));
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
