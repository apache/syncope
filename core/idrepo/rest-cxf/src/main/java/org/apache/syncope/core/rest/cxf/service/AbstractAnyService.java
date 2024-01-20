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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.ResourceAR;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchPayloadGenerator;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.springframework.data.domain.Page;

public abstract class AbstractAnyService<TO extends AnyTO, CR extends AnyCR, UR extends AnyUR>
        extends AbstractSearchService implements AnyService<TO> {

    public AbstractAnyService(final SearchCondVisitor searchCondVisitor) {
        super(searchCondVisitor);
    }

    protected abstract AnyDAO<?> getAnyDAO();

    protected abstract AbstractAnyLogic<TO, CR, UR> getAnyLogic();

    protected abstract UR newUpdateReq(String key);

    @Override
    public Set<Attr> read(final String key, final SchemaType schemaType) {
        TO any = read(key);
        Set<Attr> result;
        switch (schemaType) {
            case DERIVED:
                result = any.getDerAttrs();
                break;

            case VIRTUAL:
                result = any.getVirAttrs();
                break;

            case PLAIN:
            default:
                result = any.getPlainAttrs();
        }

        return result;
    }

    @Override
    public Attr read(final String key, final SchemaType schemaType, final String schema) {
        TO any = read(key);
        Optional<Attr> result;
        switch (schemaType) {
            case DERIVED:
                result = any.getDerAttr(schema);
                break;

            case VIRTUAL:
                result = any.getVirAttr(schema);
                break;

            case PLAIN:
            default:
                result = any.getPlainAttr(schema);
        }

        return result.
                orElseThrow(() -> new NotFoundException("Attribute for type " + schemaType + " and schema " + schema));
    }

    @Override
    public TO read(final String key) {
        return getAnyLogic().read(findActualKey(getAnyDAO(), key));
    }

    @Override
    public PagedResult<TO> search(final AnyQuery anyQuery) {
        String realm = StringUtils.prependIfMissing(anyQuery.getRealm(), SyncopeConstants.ROOT_REALM);
        SearchCond searchCond = StringUtils.isBlank(anyQuery.getFiql())
                ? null
                : getSearchCond(anyQuery.getFiql(), realm);
        try {
            Page<TO> result = getAnyLogic().search(
                    searchCond,
                    pageable(anyQuery),
                    realm,
                    anyQuery.getRecursive(),
                    anyQuery.getDetails());
            return buildPagedResult(result);
        } catch (IllegalArgumentException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
            sce.getElements().add(anyQuery.getFiql());
            sce.getElements().add(ExceptionUtils.getRootCauseMessage(e));
            throw sce;
        }
    }

    protected OffsetDateTime findLastChange(final String key) {
        return getAnyDAO().findLastChange(key).
                orElseThrow(() -> new NotFoundException("User, Group or Any Object for " + key));
    }

    protected Response doUpdate(final UR updateReq) {
        updateReq.setKey(findActualKey(getAnyDAO(), updateReq.getKey()));
        OffsetDateTime etag = findLastChange(updateReq.getKey());
        checkETag(String.valueOf(etag.toInstant().toEpochMilli()));

        ProvisioningResult<TO> updated = getAnyLogic().update(updateReq, isNullPriorityAsync());
        return modificationResponse(updated);
    }

    protected void addUpdateOrReplaceAttr(
            final String key, final SchemaType schemaType, final Attr attrTO, final PatchOperation operation) {

        if (attrTO.getSchema() == null) {
            throw new NotFoundException("Must specify schema");
        }

        UR updateReq = newUpdateReq(key);

        switch (schemaType) {
            case VIRTUAL:
                updateReq.getVirAttrs().add(attrTO);
                break;

            case PLAIN:
                updateReq.getPlainAttrs().add(new AttrPatch.Builder(attrTO).operation(operation).build());
                break;

            case DERIVED:
            default:
        }

        doUpdate(updateReq);
    }

    @Override
    public Response update(final String key, final SchemaType schemaType, final Attr attrTO) {
        String actualKey = findActualKey(getAnyDAO(), key);
        addUpdateOrReplaceAttr(actualKey, schemaType, attrTO, PatchOperation.ADD_REPLACE);
        return modificationResponse(read(actualKey, schemaType, attrTO.getSchema()));
    }

    @Override
    public void delete(final String key, final SchemaType schemaType, final String schema) {
        addUpdateOrReplaceAttr(findActualKey(getAnyDAO(), key),
                schemaType,
                new Attr.Builder(schema).build(),
                PatchOperation.DELETE);
    }

    @Override
    public Response delete(final String key) {
        String actualKey = findActualKey(getAnyDAO(), key);

        OffsetDateTime etag = findLastChange(actualKey);
        checkETag(String.valueOf(etag.toInstant().toEpochMilli()));

        ProvisioningResult<TO> deleted = getAnyLogic().delete(actualKey, isNullPriorityAsync());
        return modificationResponse(deleted);
    }

    @Override
    public Response deassociate(final ResourceDR req) {
        OffsetDateTime etag = findLastChange(req.getKey());
        checkETag(String.valueOf(etag.toInstant().toEpochMilli()));

        ProvisioningResult<TO> updated;
        switch (req.getAction()) {
            case UNLINK:
                updated = new ProvisioningResult<>();
                updated.setEntity(getAnyLogic().unlink(req.getKey(), req.getResources()));
                break;

            case UNASSIGN:
                updated = getAnyLogic().unassign(req.getKey(), req.getResources(), isNullPriorityAsync());
                break;

            case DEPROVISION:
                updated = getAnyLogic().deprovision(req.getKey(), req.getResources(), isNullPriorityAsync());
                break;

            default:
                throw new BadRequestException("Missing action");
        }

        List<BatchResponseItem> batchResponseItems;
        if (req.getAction() == ResourceDeassociationAction.UNLINK) {
            batchResponseItems = req.getResources().stream().map(resource -> {
                BatchResponseItem item = new BatchResponseItem();

                item.getHeaders().put(RESTHeaders.RESOURCE_KEY, List.of(resource));

                item.setStatus(updated.getEntity().getResources().contains(resource)
                        ? Response.Status.BAD_REQUEST.getStatusCode()
                        : Response.Status.OK.getStatusCode());

                if (getPreference() == Preference.RETURN_NO_CONTENT) {
                    item.getHeaders().put(
                            RESTHeaders.PREFERENCE_APPLIED,
                            List.of(Preference.RETURN_NO_CONTENT.toString()));
                } else {
                    item.setContent(POJOHelper.serialize(updated.getEntity()));
                }

                return item;
            }).toList();
        } else {
            batchResponseItems = updated.getPropagationStatuses().stream().
                    map(status -> {
                        BatchResponseItem item = new BatchResponseItem();

                        item.getHeaders().put(RESTHeaders.RESOURCE_KEY, List.of(status.getResource()));

                        item.setStatus(status.getStatus().getHttpStatus());

                        if (status.getFailureReason() != null) {
                            item.getHeaders().put(RESTHeaders.ERROR_INFO, List.of(status.getFailureReason()));
                        }

                        if (getPreference() == Preference.RETURN_NO_CONTENT) {
                            item.getHeaders().put(
                                    RESTHeaders.PREFERENCE_APPLIED,
                                    List.of(Preference.RETURN_NO_CONTENT.toString()));
                        } else {
                            item.setContent(POJOHelper.serialize(updated.getEntity()));
                        }

                        return item;
                    }).toList();
        }

        String boundary = "deassociate_" + SecureRandomUtils.generateRandomUUID().toString();
        return Response.ok(BatchPayloadGenerator.generate(
                batchResponseItems, JAXRSService.DOUBLE_DASH + boundary)).
                type(RESTHeaders.multipartMixedWith(boundary)).
                build();
    }

    @Override
    public Response associate(final ResourceAR req) {
        OffsetDateTime etag = findLastChange(req.getKey());
        checkETag(String.valueOf(etag.toInstant().toEpochMilli()));

        ProvisioningResult<TO> updated;
        switch (req.getAction()) {
            case LINK:
                updated = new ProvisioningResult<>();
                updated.setEntity(getAnyLogic().link(
                        req.getKey(),
                        req.getResources()));
                break;

            case ASSIGN:
                updated = getAnyLogic().assign(
                        req.getKey(),
                        req.getResources(),
                        req.getValue() != null,
                        req.getValue(),
                        isNullPriorityAsync());
                break;

            case PROVISION:
                updated = getAnyLogic().provision(
                        req.getKey(),
                        req.getResources(),
                        req.getValue() != null,
                        req.getValue(),
                        isNullPriorityAsync());
                break;

            default:
                throw new BadRequestException("Missing action");
        }

        List<BatchResponseItem> batchResponseItems;
        if (req.getAction() == ResourceAssociationAction.LINK) {
            batchResponseItems = req.getResources().stream().map(resource -> {
                BatchResponseItem item = new BatchResponseItem();

                item.getHeaders().put(RESTHeaders.RESOURCE_KEY, List.of(resource));

                item.setStatus(updated.getEntity().getResources().contains(resource)
                        ? Response.Status.OK.getStatusCode()
                        : Response.Status.BAD_REQUEST.getStatusCode());

                if (getPreference() == Preference.RETURN_NO_CONTENT) {
                    item.getHeaders().put(
                            RESTHeaders.PREFERENCE_APPLIED,
                            List.of(Preference.RETURN_NO_CONTENT.toString()));
                } else {
                    item.setContent(POJOHelper.serialize(updated.getEntity()));
                }

                return item;
            }).toList();
        } else {
            batchResponseItems = updated.getPropagationStatuses().stream().
                    map(status -> {
                        BatchResponseItem item = new BatchResponseItem();

                        item.getHeaders().put(RESTHeaders.RESOURCE_KEY, List.of(status.getResource()));

                        item.setStatus(status.getStatus().getHttpStatus());

                        if (status.getFailureReason() != null) {
                            item.getHeaders().put(RESTHeaders.ERROR_INFO, List.of(status.getFailureReason()));
                        }

                        if (getPreference() == Preference.RETURN_NO_CONTENT) {
                            item.getHeaders().put(
                                    RESTHeaders.PREFERENCE_APPLIED,
                                    List.of(Preference.RETURN_NO_CONTENT.toString()));
                        } else {
                            item.setContent(POJOHelper.serialize(updated.getEntity()));
                        }

                        return item;
                    }).toList();
        }

        String boundary = "associate_" + SecureRandomUtils.generateRandomUUID().toString();
        return Response.ok(BatchPayloadGenerator.generate(
                batchResponseItems, JAXRSService.DOUBLE_DASH + boundary)).
                type(RESTHeaders.multipartMixedWith(boundary)).
                build();
    }
}
