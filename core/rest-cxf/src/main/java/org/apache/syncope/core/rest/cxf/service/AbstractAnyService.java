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

import java.util.Set;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AssociationPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StatusPatchType;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;

public abstract class AbstractAnyService<TO extends AnyTO, P extends AnyPatch>
        extends AbstractServiceImpl
        implements AnyService<TO, P> {

    protected abstract AbstractAnyLogic<TO, P> getAnyLogic();

    protected abstract P newPatch(Long key);

    @Override
    public Set<AttrTO> read(final Long key, final SchemaType schemaType) {
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
    public AttrTO read(final Long key, final SchemaType schemaType, final String schema) {
        TO any = read(key);
        AttrTO result;
        switch (schemaType) {
            case DERIVED:
                result = any.getDerAttrMap().get(schema);
                break;

            case VIRTUAL:
                result = any.getVirAttrMap().get(schema);
                break;

            case PLAIN:
            default:
                result = any.getPlainAttrMap().get(schema);
        }

        if (result == null) {
            throw new NotFoundException("Attribute for type " + schemaType + " and schema " + schema);
        }

        return result;
    }

    @Override
    public TO read(final Long key) {
        return getAnyLogic().read(key);
    }

    @Override
    public PagedResult<TO> list(final AnyListQuery listQuery) {
        CollectionUtils.transform(listQuery.getRealms(), new Transformer<String, String>() {

            @Override
            public String transform(final String input) {
                return StringUtils.prependIfMissing(input, SyncopeConstants.ROOT_REALM);
            }
        });

        return buildPagedResult(
                getAnyLogic().list(
                        listQuery.getPage(),
                        listQuery.getSize(),
                        getOrderByClauses(listQuery.getOrderBy()),
                        listQuery.getRealms(),
                        listQuery.isDetails()),
                listQuery.getPage(),
                listQuery.getSize(),
                getAnyLogic().count(listQuery.getRealms()));
    }

    @Override
    public PagedResult<TO> search(final AnySearchQuery searchQuery) {
        CollectionUtils.transform(searchQuery.getRealms(), new Transformer<String, String>() {

            @Override
            public String transform(final String input) {
                return StringUtils.prependIfMissing(input, SyncopeConstants.ROOT_REALM);
            }
        });

        SearchCond cond = getSearchCond(searchQuery.getFiql());
        return buildPagedResult(
                getAnyLogic().search(
                        cond,
                        searchQuery.getPage(),
                        searchQuery.getSize(),
                        getOrderByClauses(searchQuery.getOrderBy()),
                        searchQuery.getRealms(),
                        searchQuery.isDetails()),
                searchQuery.getPage(),
                searchQuery.getSize(),
                getAnyLogic().searchCount(cond, searchQuery.getRealms()));
    }

    @Override
    public Response create(final TO anyTO) {
        TO created = getAnyLogic().create(anyTO);
        return createResponse(created.getKey(), created);
    }

    @Override
    public Response update(final P anyPatch) {
        TO any = getAnyLogic().read(anyPatch.getKey());

        checkETag(any.getETagValue());

        TO updated = getAnyLogic().update(anyPatch);
        return modificationResponse(updated);
    }

    private void addUpdateOrReplaceAttr(
            final Long key, final SchemaType schemaType, final AttrTO attrTO, final PatchOperation operation) {

        if (attrTO.getSchema() == null) {
            throw new NotFoundException("Must specify schema");
        }

        P patch = newPatch(key);

        Set<AttrPatch> patches;
        switch (schemaType) {
            case DERIVED:
                patches = patch.getDerAttrs();
                break;

            case VIRTUAL:
                patches = patch.getVirAttrs();
                break;

            case PLAIN:
            default:
                patches = patch.getPlainAttrs();
        }

        patches.add(new AttrPatch.Builder().operation(operation).attrTO(attrTO).build());

        update(patch);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Response update(final Long key, final SchemaType schemaType, final AttrTO attrTO) {
        addUpdateOrReplaceAttr(key, schemaType, attrTO, PatchOperation.ADD_REPLACE);
        return modificationResponse(read(key, schemaType, attrTO.getSchema()));
    }

    @Override
    public Response update(final TO anyTO) {
        TO before = getAnyLogic().read(anyTO.getKey());

        checkETag(before.getETagValue());

        @SuppressWarnings("unchecked")
        TO updated = getAnyLogic().update((P) AnyOperations.diff(anyTO, before, false));
        return modificationResponse(updated);
    }

    @Override
    public void delete(final Long key, final SchemaType schemaType, final String schema) {
        addUpdateOrReplaceAttr(key, schemaType, new AttrTO.Builder().schema(schema).build(), PatchOperation.DELETE);
    }

    @Override
    public Response delete(final Long key) {
        TO group = getAnyLogic().read(key);

        checkETag(group.getETagValue());

        TO deleted = getAnyLogic().delete(key);
        return modificationResponse(deleted);
    }

    @Override
    public Response deassociate(final DeassociationPatch patch) {
        TO any = getAnyLogic().read(patch.getKey());

        checkETag(any.getETagValue());

        TO updated;
        switch (patch.getAction()) {
            case UNLINK:
                updated = getAnyLogic().unlink(patch.getKey(), patch.getResources());
                break;

            case UNASSIGN:
                updated = getAnyLogic().unassign(patch.getKey(), patch.getResources());
                break;

            case DEPROVISION:
                updated = getAnyLogic().deprovision(patch.getKey(), patch.getResources());
                break;

            default:
                updated = getAnyLogic().read(patch.getKey());
        }

        BulkActionResult result = new BulkActionResult();

        if (patch.getAction() == ResourceDeassociationAction.UNLINK) {
            for (String resource : patch.getResources()) {
                result.getResults().put(resource,
                        updated.getResources().contains(resource)
                                ? BulkActionResult.Status.FAILURE
                                : BulkActionResult.Status.SUCCESS);
            }
        } else {
            for (PropagationStatus propagationStatusTO : updated.getPropagationStatusTOs()) {
                result.getResults().put(propagationStatusTO.getResource(),
                        BulkActionResult.Status.valueOf(propagationStatusTO.getStatus().toString()));
            }
        }

        return modificationResponse(result);
    }

    @Override
    public Response associate(final AssociationPatch patch) {
        TO any = getAnyLogic().read(patch.getKey());

        checkETag(any.getETagValue());

        TO updated;
        switch (patch.getAction()) {
            case LINK:
                updated = getAnyLogic().link(
                        patch.getKey(),
                        patch.getResources());
                break;

            case ASSIGN:
                updated = getAnyLogic().assign(
                        patch.getKey(),
                        patch.getResources(),
                        patch.getValue() != null,
                        patch.getValue());
                break;

            case PROVISION:
                updated = getAnyLogic().provision(
                        patch.getKey(),
                        patch.getResources(),
                        patch.getValue() != null,
                        patch.getValue());
                break;

            default:
                updated = getAnyLogic().read(patch.getKey());
        }

        BulkActionResult result = new BulkActionResult();

        if (patch.getAction() == ResourceAssociationAction.LINK) {
            for (String resource : patch.getResources()) {
                result.getResults().put(resource,
                        updated.getResources().contains(resource)
                                ? BulkActionResult.Status.FAILURE
                                : BulkActionResult.Status.SUCCESS);
            }
        } else {
            for (PropagationStatus propagationStatusTO : updated.getPropagationStatusTOs()) {
                result.getResults().put(propagationStatusTO.getResource(),
                        BulkActionResult.Status.valueOf(propagationStatusTO.getStatus().toString()));
            }
        }

        return modificationResponse(result);
    }

    @Override
    public BulkActionResult bulk(final BulkAction bulkAction) {
        AbstractAnyLogic<TO, P> logic = getAnyLogic();

        BulkActionResult result = new BulkActionResult();

        switch (bulkAction.getType()) {
            case DELETE:
                for (String key : bulkAction.getTargets()) {
                    try {
                        result.getResults().put(
                                String.valueOf(logic.delete(Long.valueOf(key)).getKey()),
                                BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing delete for user {}", key, e);
                        result.getResults().put(key, BulkActionResult.Status.FAILURE);
                    }
                }
                break;

            case SUSPEND:
                if (logic instanceof UserLogic) {
                    for (String key : bulkAction.getTargets()) {
                        StatusPatch statusPatch = new StatusPatch();
                        statusPatch.setKey(Long.valueOf(key));
                        statusPatch.setType(StatusPatchType.SUSPEND);
                        try {
                            result.getResults().put(
                                    String.valueOf(((UserLogic) logic).status(statusPatch).getKey()),
                                    BulkActionResult.Status.SUCCESS);
                        } catch (Exception e) {
                            LOG.error("Error performing suspend for user {}", key, e);
                            result.getResults().put(key, BulkActionResult.Status.FAILURE);
                        }
                    }
                }
                break;

            case REACTIVATE:
                for (String key : bulkAction.getTargets()) {
                    StatusPatch statusPatch = new StatusPatch();
                    statusPatch.setKey(Long.valueOf(key));
                    statusPatch.setType(StatusPatchType.REACTIVATE);
                    try {
                        result.getResults().put(
                                String.valueOf(((UserLogic) logic).status(statusPatch).getKey()),
                                BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing reactivate for user {}", key, e);
                        result.getResults().put(key, BulkActionResult.Status.FAILURE);
                    }
                }
                break;

            default:
        }

        return result;
    }

}
