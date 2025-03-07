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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.search.SpecialAttr;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.AnyObjectLogic;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;

public class AnyObjectServiceImpl extends AbstractAnyService<AnyObjectTO, AnyObjectCR, AnyObjectUR>
        implements AnyObjectService {

    protected final AnyObjectDAO anyObjectDAO;

    protected final AnyObjectLogic logic;

    public AnyObjectServiceImpl(
            final SearchCondVisitor searchCondVisitor,
            final AnyObjectDAO anyObjectDAO,
            final AnyObjectLogic logic) {

        super(searchCondVisitor);
        this.anyObjectDAO = anyObjectDAO;
        this.logic = logic;
    }

    @Override
    protected AnyDAO<?> getAnyDAO() {
        return anyObjectDAO;
    }

    @Override
    protected AbstractAnyLogic<AnyObjectTO, AnyObjectCR, AnyObjectUR> getAnyLogic() {
        return logic;
    }

    @Override
    public AnyObjectTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public AnyObjectTO read(final String type, final String name) {
        return logic.read(type, name);
    }

    @Override
    protected AnyObjectUR newUpdateReq(final String key) {
        return new AnyObjectUR.Builder(key).build();
    }

    @Override
    public Response create(final AnyObjectCR createReq) {
        ProvisioningResult<AnyObjectTO> created = logic.create(createReq, isNullPriorityAsync());
        return createResponse(created);
    }

    @Override
    public Response update(final AnyObjectUR updateReq) {
        return doUpdate(updateReq);
    }

    @Override
    public PagedResult<AnyObjectTO> search(final AnyQuery anyQuery) {
        if (StringUtils.isBlank(anyQuery.getFiql()) || -1 == anyQuery.getFiql().indexOf(SpecialAttr.TYPE.toString())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
            sce.getElements().add(SpecialAttr.TYPE + " is required in the FIQL string");
            throw sce;
        }

        return super.search(anyQuery);
    }
}
