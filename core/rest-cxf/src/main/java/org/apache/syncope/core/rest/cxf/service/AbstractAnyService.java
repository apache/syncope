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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AssociationPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.search.SpecialAttr;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StatusPatchType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;

public abstract class AbstractAnyService<TO extends AnyTO, P extends AnyPatch>
        extends AbstractServiceImpl
        implements AnyService<TO, P> {

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
    public TO read(final String key) {
        return getAnyLogic().read(key);
    }

    @Override
    public PagedResult<TO> search(final AnyQuery anyQuery) {
        String realm = StringUtils.prependIfMissing(anyQuery.getRealm(), SyncopeConstants.ROOT_REALM);

        if (StringUtils.isBlank(anyQuery.getFiql())) {
            return buildPagedResult(
                    getAnyLogic().list(
                            anyQuery.getPage(),
                            anyQuery.getSize(),
                            getOrderByClauses(anyQuery.getOrderBy()),
                            realm,
                            anyQuery.getDetails()),
                    anyQuery.getPage(),
                    anyQuery.getSize(),
                    getAnyLogic().count(realm));
        } else {
            // if an assignable query is provided in the FIQL string, start anyway from root realm
            boolean isAssignableCond = -1 != anyQuery.getFiql().indexOf(SpecialAttr.ASSIGNABLE.toString());

            SearchCond cond = getSearchCond(anyQuery.getFiql(), realm);
            return buildPagedResult(
                    getAnyLogic().search(
                            cond,
                            anyQuery.getPage(),
                            anyQuery.getSize(),
                            getOrderByClauses(anyQuery.getOrderBy()),
                            isAssignableCond ? SyncopeConstants.ROOT_REALM : realm,
                            anyQuery.getDetails()),
                    anyQuery.getPage(),
                    anyQuery.getSize(),
                    getAnyLogic().searchCount(cond, isAssignableCond ? SyncopeConstants.ROOT_REALM : realm));
        }
    }

    @Override
    public Response create(final TO anyTO) {
        ProvisioningResult<TO> created = getAnyLogic().create(anyTO, isNullPriorityAsync());
        return createResponse(created);
    }

    @Override
    public Response update(final P anyPatch) {
        TO any = getAnyLogic().read(anyPatch.getKey());

        checkETag(any.getETagValue());

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

        update(patch);
    }

    @Override
    public Response update(final String key, final SchemaType schemaType, final AttrTO attrTO) {
        addUpdateOrReplaceAttr(key, schemaType, attrTO, PatchOperation.ADD_REPLACE);
        return modificationResponse(read(key, schemaType, attrTO.getSchema()));
    }

    @Override
    public Response update(final TO anyTO) {
        TO before = getAnyLogic().read(anyTO.getKey());

        checkETag(before.getETagValue());

        ProvisioningResult<TO> updated = getAnyLogic().update(AnyOperations.<TO, P>diff(anyTO, before, false),
                isNullPriorityAsync());
        return modificationResponse(updated);
    }

    @Override
    public void delete(final String key, final SchemaType schemaType, final String schema) {
        addUpdateOrReplaceAttr(key, schemaType, new AttrTO.Builder().schema(schema).build(), PatchOperation.DELETE);
    }

    @Override
    public Response delete(final String key) {
        TO group = getAnyLogic().read(key);

        checkETag(group.getETagValue());

        ProvisioningResult<TO> deleted = getAnyLogic().delete(key, isNullPriorityAsync());
        return modificationResponse(deleted);
    }

    @Override
    public Response deassociate(final DeassociationPatch patch) {
        TO any = getAnyLogic().read(patch.getKey());

        checkETag(any.getETagValue());

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
                updated = new ProvisioningResult<>();
                updated.setEntity(getAnyLogic().read(patch.getKey()));
        }

        BulkActionResult result = new BulkActionResult();

        if (patch.getAction() == ResourceDeassociationAction.UNLINK) {
            for (String resource : patch.getResources()) {
                result.getResults().put(resource,
                        updated.getEntity().getResources().contains(resource)
                        ? BulkActionResult.Status.FAILURE
                        : BulkActionResult.Status.SUCCESS);
            }
        } else {
            for (PropagationStatus propagationStatusTO : updated.getPropagationStatuses()) {
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
                updated = new ProvisioningResult<>();
                updated.setEntity(getAnyLogic().read(patch.getKey()));
        }

        BulkActionResult result = new BulkActionResult();

        if (patch.getAction() == ResourceAssociationAction.LINK) {
            for (String resource : patch.getResources()) {
                result.getResults().put(resource,
                        updated.getEntity().getResources().contains(resource)
                        ? BulkActionResult.Status.SUCCESS
                        : BulkActionResult.Status.FAILURE);
            }
        } else {
            for (PropagationStatus propagationStatusTO : updated.getPropagationStatuses()) {
                result.getResults().put(propagationStatusTO.getResource(),
                        BulkActionResult.Status.valueOf(propagationStatusTO.getStatus().toString()));
            }
        }

        return modificationResponse(result);
    }

    @Override
    public Response bulk(final BulkAction bulkAction) {
        AbstractAnyLogic<TO, P> logic = getAnyLogic();

        BulkActionResult result = new BulkActionResult();

        switch (bulkAction.getType()) {
            case MUSTCHANGEPASSWORD:
                if (logic instanceof UserLogic) {
                    for (String key : bulkAction.getTargets()) {
                        try {
                            final UserPatch userPatch = new UserPatch();
                            userPatch.setKey(key);
                            userPatch.setMustChangePassword(new BooleanReplacePatchItem.Builder().value(true).build());

                            result.getResults().put(
                                    ((UserLogic) logic).update(userPatch, false).getEntity().getKey(),
                                    BulkActionResult.Status.SUCCESS);
                        } catch (Exception e) {
                            LOG.error("Error performing delete for user {}", key, e);
                            result.getResults().put(key, BulkActionResult.Status.FAILURE);
                        }
                    }
                } else {
                    throw new BadRequestException();
                }
                break;

            case DELETE:
                for (String key : bulkAction.getTargets()) {
                    try {
                        result.getResults().put(
                                logic.delete(key, isNullPriorityAsync()).getEntity().getKey(),
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
                        statusPatch.setKey(key);
                        statusPatch.setType(StatusPatchType.SUSPEND);
                        statusPatch.setOnSyncope(true);

                        try {
                            result.getResults().put(
                                    ((UserLogic) logic).
                                    status(statusPatch, isNullPriorityAsync()).getEntity().getKey(),
                                    BulkActionResult.Status.SUCCESS);
                        } catch (Exception e) {
                            LOG.error("Error performing suspend for user {}", key, e);
                            result.getResults().put(key, BulkActionResult.Status.FAILURE);
                        }
                    }
                } else {
                    throw new BadRequestException();
                }
                break;

            case REACTIVATE:
                if (logic instanceof UserLogic) {
                    for (String key : bulkAction.getTargets()) {
                        StatusPatch statusPatch = new StatusPatch();
                        statusPatch.setKey(key);
                        statusPatch.setType(StatusPatchType.REACTIVATE);
                        statusPatch.setOnSyncope(true);

                        try {
                            result.getResults().put(
                                    ((UserLogic) logic).
                                    status(statusPatch, isNullPriorityAsync()).getEntity().getKey(),
                                    BulkActionResult.Status.SUCCESS);
                        } catch (Exception e) {
                            LOG.error("Error performing reactivate for user {}", key, e);
                            result.getResults().put(key, BulkActionResult.Status.FAILURE);
                        }
                    }
                } else {
                    throw new BadRequestException();
                }
                break;

            default:
        }

        return modificationResponse(result);
    }

}
