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
package org.apache.syncope.ext.scimv2.cxf.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.core.logic.AnyObjectLogic;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.SCIMDataBinder;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.SCIMAnyObject;
import org.apache.syncope.ext.scimv2.api.data.SCIMPatchOp;
import org.apache.syncope.ext.scimv2.api.data.SCIMResource;
import org.apache.syncope.ext.scimv2.api.data.SCIMSearchRequest;
import org.apache.syncope.ext.scimv2.api.service.SCIMAnyObjectService;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;

public class SCIMAnyObjectServiceImpl extends AbstractSCIMService<SCIMAnyObject> implements SCIMAnyObjectService {

    public SCIMAnyObjectServiceImpl(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final AnyObjectLogic anyObjectLogic,
            final SCIMDataBinder binder,
            final SCIMConfManager confManager) {

        super(userDAO, groupDAO, anyObjectDAO, userLogic, groupLogic, anyObjectLogic, binder, confManager);
    }

    @Override
    public SCIMAnyObject get(final String id, final String attributes, final String excludedAttributes) {
        return binder.toSCIMAnyObject(
                anyObjectLogic.read(id),
                uriInfo.getAbsolutePathBuilder().build().toASCIIString(),
                List.of(ArrayUtils.nullToEmpty(StringUtils.split(attributes, ','))),
                List.of(ArrayUtils.nullToEmpty(StringUtils.split(excludedAttributes, ','))));
    }

    @Override
    public ListResponse<SCIMAnyObject> search(
            final String attributes, final String excludedAttributes, final String filter, final String sortBy,
            final SortOrder sortOrder, final Integer startIndex, final Integer count) {
        
        SCIMSearchRequest request = new SCIMSearchRequest(filter, sortBy, sortOrder, startIndex, count);
        if (attributes != null) {
            request.getAttributes().addAll(
                    List.of(ArrayUtils.nullToEmpty(StringUtils.split(attributes, ','))));
        }
        if (excludedAttributes != null) {
            request.getExcludedAttributes().addAll(
                    List.of(ArrayUtils.nullToEmpty(StringUtils.split(excludedAttributes, ','))));
        }

        Matcher matcher = Pattern.compile("type\\s+eq\\s+\"(.*?)\"").matcher(filter);
        if (matcher.find()) {
            return doSearch(matcher.group(1), request);
        } else {
            throw new UnsupportedOperationException("Need to specify type");
        }
    }

    @Override
    public ListResponse<SCIMAnyObject> search(final SCIMSearchRequest request) {
        Matcher matcher = Pattern.compile("type\\s+eq\\s+\"(.*?)\"").matcher(request.getFilter());
        if (matcher.find()) {
            return doSearch(matcher.group(1), request);
        } else {
            throw new UnsupportedOperationException("Need to specify type");
        }
    }

    @Override
    public Response create(final SCIMAnyObject anyObject) {
        ProvisioningResult<AnyObjectTO> result = anyObjectLogic.create(binder.toAnyObjectCR(anyObject), false);
        return createResponse(
                result.getEntity().getKey(),
                binder.toSCIMAnyObject(
                        result.getEntity(),
                        uriInfo.getAbsolutePathBuilder().path(result.getEntity().getKey()).build().toASCIIString(),
                        List.of(),
                        List.of()));
    }

    @Override
    public Response update(final String id, final SCIMPatchOp patch) {
        SCIMResource resource = getResource(id);
        Response.ResponseBuilder builder = checkETag(
                "urn:ietf:params:scim:schemas:extension:syncope:2.0:" + resource.getSchemas().get(0), id);
        if (builder != null) {
            return builder.build();
        }

        patch.getOperations().forEach(op -> {
            AnyObjectUR update = binder.toAnyObjectUR(anyObjectLogic.read(id), op);
            anyObjectLogic.update(update, false);
        });

        return updateResponse(
                id,
                null,
                true);
    }

    @Override
    public Response replace(final String id, final SCIMAnyObject anyObject) {
        if (!id.equals(anyObject.getId())) {
            throw new BadRequestException(ErrorType.invalidPath, "Expected " + id + ", found " + anyObject.getId());
        }

        SCIMResource resource = getResource(id);
        Response.ResponseBuilder builder = checkETag(
                "urn:ietf:params:scim:schemas:extension:syncope:2.0:" + resource.getSchemas().get(0), id);
        if (builder != null) {
            return builder.build();
        }

        AnyObjectTO before = anyObjectLogic.read(id);

        AnyObjectUR req = AnyOperations.diff(binder.toAnyObjectTO(anyObject, true), before, false);
        req.getResources().clear();
        req.getAuxClasses().clear();
        req.getRelationships().clear();
        ProvisioningResult<AnyObjectTO> result = anyObjectLogic.update(req, false);

        return updateResponse(
                result.getEntity().getKey(),
                binder.toSCIMAnyObject(
                        result.getEntity(),
                        uriInfo.getAbsolutePathBuilder().path(result.getEntity().getKey()).build().toASCIIString(),
                        List.of(),
                        List.of()),
                false);
    }

    @Override
    public Response delete(final String id) {
        SCIMResource resource = getResource(id);
        Response.ResponseBuilder builder = checkETag(
                "urn:ietf:params:scim:schemas:extension:syncope:2.0:" + resource.getSchemas().get(0), id);
        if (builder != null) {
            return builder.build();
        }

        anyObjectLogic.delete(id, false);
        return Response.noContent().build();
    }

    @Override
    protected SCIMResource getResource(final String key) {
        return binder.toSCIMAnyObject(
                anyObjectLogic.read(key),
                uriInfo.getAbsolutePathBuilder().path(key).build().toASCIIString(),
                List.of(),
                List.of());
    }
}
