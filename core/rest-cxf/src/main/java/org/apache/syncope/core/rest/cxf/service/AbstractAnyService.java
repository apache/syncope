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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AssociationPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.search.SpecialAttr;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
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
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.SecureRandomUtils;

public abstract class AbstractAnyService<TO extends AnyTO, P extends AnyPatch>
        extends AbstractServiceImpl
        implements AnyService<TO> {

    protected abstract AnyDAO<?> getAnyDAO();

    protected abstract AbstractAnyLogic<TO, P> getAnyLogic();

    protected abstract P newPatch(String key);

    @Override
    public Set<AttrTO> read(final String key, final SchemaType schemaType) {
        TO any = read(key);
        Set<AttrTO> result;
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
    public AttrTO read(final String key, final SchemaType schemaType, final String schema) {
        TO any = read(key);
        Optional<AttrTO> result;
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

        if (!result.isPresent()) {
            throw new NotFoundException("Attribute for type " + schemaType + " and schema " + schema);
        }

        return result.get();
    }

    @Override
    public TO read(final String key) {
        return getAnyLogic().read(getActualKey(getAnyDAO(), key));
    }

    @Override
    public PagedResult<TO> search(final AnyQuery anyQuery) {
        String realm = StringUtils.prependIfMissing(anyQuery.getRealm(), SyncopeConstants.ROOT_REALM);

        // if an assignable query is provided in the FIQL string, start anyway from root realm
        boolean isAssignableCond = StringUtils.isBlank(anyQuery.getFiql())
                ? false
                : -1 != anyQuery.getFiql().indexOf(SpecialAttr.ASSIGNABLE.toString());

        SearchCond searchCond = StringUtils.isBlank(anyQuery.getFiql())
                ? null
                : getSearchCond(anyQuery.getFiql(), realm);

        Pair<Integer, List<TO>> result = getAnyLogic().search(
                searchCond,
                anyQuery.getPage(),
                anyQuery.getSize(),
                getOrderByClauses(anyQuery.getOrderBy()),
                isAssignableCond ? SyncopeConstants.ROOT_REALM : realm,
                anyQuery.getDetails());

        return buildPagedResult(result.getRight(), anyQuery.getPage(), anyQuery.getSize(), result.getLeft());
    }

    protected Date findLastChange(final String key) {
        Date lastChange = getAnyDAO().findLastChange(key);
        if (lastChange == null) {
            throw new NotFoundException("User, Group or Any Object for " + key);
        }

        return lastChange;
    }

    protected Response doUpdate(final P anyPatch) {
        anyPatch.setKey(getActualKey(getAnyDAO(), anyPatch.getKey()));
        Date etagDate = findLastChange(anyPatch.getKey());
        checkETag(String.valueOf(etagDate.getTime()));

        ProvisioningResult<TO> updated = getAnyLogic().update(anyPatch, isNullPriorityAsync());
        return modificationResponse(updated);
    }

    private void addUpdateOrReplaceAttr(
            final String key, final SchemaType schemaType, final AttrTO attrTO, final PatchOperation operation) {

        if (attrTO.getSchema() == null) {
            throw new NotFoundException("Must specify schema");
        }

        P patch = newPatch(key);

        switch (schemaType) {
            case VIRTUAL:
                patch.getVirAttrs().add(attrTO);
                break;

            case PLAIN:
                patch.getPlainAttrs().add(new AttrPatch.Builder().operation(operation).attrTO(attrTO).build());
                break;

            case DERIVED:
            default:
        }

        doUpdate(patch);
    }

    @Override
    public Response update(final String key, final SchemaType schemaType, final AttrTO attrTO) {
        String actualKey = getActualKey(getAnyDAO(), key);
        addUpdateOrReplaceAttr(actualKey, schemaType, attrTO, PatchOperation.ADD_REPLACE);
        return modificationResponse(read(actualKey, schemaType, attrTO.getSchema()));
    }

    @Override
    public void delete(final String key, final SchemaType schemaType, final String schema) {
        addUpdateOrReplaceAttr(
                getActualKey(getAnyDAO(), key),
                schemaType,
                new AttrTO.Builder().schema(schema).build(),
                PatchOperation.DELETE);
    }

    @Override
    public Response delete(final String key) {
        String actualKey = getActualKey(getAnyDAO(), key);

        Date etagDate = findLastChange(actualKey);
        checkETag(String.valueOf(etagDate.getTime()));

        ProvisioningResult<TO> deleted = getAnyLogic().delete(actualKey, isNullPriorityAsync());
        return modificationResponse(deleted);
    }

    @Override
    public Response deassociate(final DeassociationPatch patch) {
        Date etagDate = findLastChange(patch.getKey());
        checkETag(String.valueOf(etagDate.getTime()));

        ProvisioningResult<TO> updated;
        switch (patch.getAction()) {
            case UNLINK:
                updated = new ProvisioningResult<>();
                updated.setEntity(getAnyLogic().unlink(patch.getKey(), patch.getResources()));
                break;

            case UNASSIGN:
                updated = getAnyLogic().unassign(patch.getKey(), patch.getResources(), isNullPriorityAsync());
                break;

            case DEPROVISION:
                updated = getAnyLogic().deprovision(patch.getKey(), patch.getResources(), isNullPriorityAsync());
                break;

            default:
                throw new BadRequestException("Missing action");
        }

        List<BatchResponseItem> batchResponseItems;
        if (patch.getAction() == ResourceDeassociationAction.UNLINK) {
            batchResponseItems = patch.getResources().stream().map(resource -> {
                BatchResponseItem item = new BatchResponseItem();

                item.getHeaders().put(RESTHeaders.RESOURCE_KEY, Arrays.asList(resource));

                item.setStatus(updated.getEntity().getResources().contains(resource)
                        ? Response.Status.BAD_REQUEST.getStatusCode()
                        : Response.Status.OK.getStatusCode());

                if (getPreference() == Preference.RETURN_NO_CONTENT) {
                    item.getHeaders().put(
                            RESTHeaders.PREFERENCE_APPLIED,
                            Arrays.asList(Preference.RETURN_NO_CONTENT.toString()));
                } else {
                    item.setContent(POJOHelper.serialize(updated.getEntity()));
                }

                return item;
            }).collect(Collectors.toList());
        } else {
            batchResponseItems = updated.getPropagationStatuses().stream().
                    map(status -> {
                        BatchResponseItem item = new BatchResponseItem();

                        item.getHeaders().put(RESTHeaders.RESOURCE_KEY, Arrays.asList(status.getResource()));

                        item.setStatus(status.getStatus().getHttpStatus());

                        if (status.getFailureReason() != null) {
                            item.getHeaders().put(RESTHeaders.ERROR_INFO, Arrays.asList(status.getFailureReason()));
                        }

                        if (getPreference() == Preference.RETURN_NO_CONTENT) {
                            item.getHeaders().put(
                                    RESTHeaders.PREFERENCE_APPLIED,
                                    Arrays.asList(Preference.RETURN_NO_CONTENT.toString()));
                        } else {
                            item.setContent(POJOHelper.serialize(updated.getEntity()));
                        }

                        return item;
                    }).collect(Collectors.toList());
        }

        String boundary = "deassociate_" + SecureRandomUtils.generateRandomUUID().toString();
        return Response.ok(BatchPayloadGenerator.generate(
                batchResponseItems, SyncopeConstants.DOUBLE_DASH + boundary)).
                type(RESTHeaders.multipartMixedWith(boundary)).
                build();
    }

    @Override
    public Response associate(final AssociationPatch patch) {
        Date etagDate = findLastChange(patch.getKey());
        checkETag(String.valueOf(etagDate.getTime()));

        ProvisioningResult<TO> updated;
        switch (patch.getAction()) {
            case LINK:
                updated = new ProvisioningResult<>();
                updated.setEntity(getAnyLogic().link(
                        patch.getKey(),
                        patch.getResources()));
                break;

            case ASSIGN:
                updated = getAnyLogic().assign(
                        patch.getKey(),
                        patch.getResources(),
                        patch.getValue() != null,
                        patch.getValue(),
                        isNullPriorityAsync());
                break;

            case PROVISION:
                updated = getAnyLogic().provision(
                        patch.getKey(),
                        patch.getResources(),
                        patch.getValue() != null,
                        patch.getValue(),
                        isNullPriorityAsync());
                break;

            default:
                throw new BadRequestException("Missing action");
        }

        List<BatchResponseItem> batchResponseItems;
        if (patch.getAction() == ResourceAssociationAction.LINK) {
            batchResponseItems = patch.getResources().stream().map(resource -> {
                BatchResponseItem item = new BatchResponseItem();

                item.getHeaders().put(RESTHeaders.RESOURCE_KEY, Arrays.asList(resource));

                item.setStatus(updated.getEntity().getResources().contains(resource)
                        ? Response.Status.OK.getStatusCode()
                        : Response.Status.BAD_REQUEST.getStatusCode());

                if (getPreference() == Preference.RETURN_NO_CONTENT) {
                    item.getHeaders().put(
                            RESTHeaders.PREFERENCE_APPLIED,
                            Arrays.asList(Preference.RETURN_NO_CONTENT.toString()));
                } else {
                    item.setContent(POJOHelper.serialize(updated.getEntity()));
                }

                return item;
            }).collect(Collectors.toList());
        } else {
            batchResponseItems = updated.getPropagationStatuses().stream().
                    map(status -> {
                        BatchResponseItem item = new BatchResponseItem();

                        item.getHeaders().put(RESTHeaders.RESOURCE_KEY, Arrays.asList(status.getResource()));

                        item.setStatus(status.getStatus().getHttpStatus());

                        if (status.getFailureReason() != null) {
                            item.getHeaders().put(RESTHeaders.ERROR_INFO, Arrays.asList(status.getFailureReason()));
                        }

                        if (getPreference() == Preference.RETURN_NO_CONTENT) {
                            item.getHeaders().put(
                                    RESTHeaders.PREFERENCE_APPLIED,
                                    Arrays.asList(Preference.RETURN_NO_CONTENT.toString()));
                        } else {
                            item.setContent(POJOHelper.serialize(updated.getEntity()));
                        }

                        return item;
                    }).collect(Collectors.toList());
        }

        String boundary = "associate_" + SecureRandomUtils.generateRandomUUID().toString();
        return Response.ok(BatchPayloadGenerator.generate(
                batchResponseItems, SyncopeConstants.DOUBLE_DASH + boundary)).
                type(RESTHeaders.multipartMixedWith(boundary)).
                build();
    }
}
