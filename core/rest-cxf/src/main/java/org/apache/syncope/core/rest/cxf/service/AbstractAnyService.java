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

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;

public abstract class AbstractAnyService<TO extends AnyTO, P extends AnyPatch>
        extends AbstractServiceImpl
        implements AnyService<TO, P> {

    protected abstract AnyDAO<?> getAnyDAO();

    protected abstract AbstractAnyLogic<TO, P> getAnyLogic();

    protected abstract P newPatch(String key);

    private String getActualKey(final String key) {
        String actualKey = key;
        if (!SyncopeConstants.UUID_PATTERN.matcher(key).matches()) {
            actualKey = getAnyDAO().findKey(key);
            if (actualKey == null) {
                throw new NotFoundException("User, Group or Any Object for " + key);
            }
        }

        return actualKey;
    }

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
        return getAnyLogic().read(getActualKey(key));
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

    @Override
    public Response create(final TO anyTO) {
        ProvisioningResult<TO> created = getAnyLogic().create(anyTO, isNullPriorityAsync());
        return createResponse(created);
    }

    protected Date findLastChange(final String key) {
        Date lastChange = getAnyDAO().findLastChange(key);
        if (lastChange == null) {
            throw new NotFoundException("User, Group or Any Object for " + key);
        }

        return lastChange;
    }

    @Override
    public Response update(final P anyPatch) {
        anyPatch.setKey(getActualKey(anyPatch.getKey()));
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

        update(patch);
    }

    @Override
    public Response update(final String key, final SchemaType schemaType, final AttrTO attrTO) {
        String actualKey = getActualKey(key);
        addUpdateOrReplaceAttr(actualKey, schemaType, attrTO, PatchOperation.ADD_REPLACE);
        return modificationResponse(read(actualKey, schemaType, attrTO.getSchema()));
    }

    @Override
    public Response update(final TO anyTO) {
        anyTO.setKey(getActualKey(anyTO.getKey()));
        TO before = getAnyLogic().read(anyTO.getKey());

        checkETag(before.getETagValue());

        ProvisioningResult<TO> updated = getAnyLogic().update(AnyOperations.<TO, P>diff(anyTO, before, false),
                isNullPriorityAsync());
        return modificationResponse(updated);
    }

    @Override
    public void delete(final String key, final SchemaType schemaType, final String schema) {
        String actualKey = getActualKey(key);
        addUpdateOrReplaceAttr(
                actualKey, schemaType, new AttrTO.Builder().schema(schema).build(), PatchOperation.DELETE);
    }

    @Override
    public Response delete(final String key) {
        String actualKey = getActualKey(key);

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
                updated = new ProvisioningResult<>();
                updated.setEntity(getAnyLogic().read(patch.getKey()));
        }

        BulkActionResult result = new BulkActionResult();

        if (patch.getAction() == ResourceDeassociationAction.UNLINK) {
            patch.getResources().forEach(resource -> {
                result.getResults().put(
                        resource,
                        updated.getEntity().getResources().contains(resource)
                        ? BulkActionResult.Status.FAILURE
                        : BulkActionResult.Status.SUCCESS);
            });
        } else {
            updated.getPropagationStatuses().forEach(propagationStatusTO
                    -> result.getResults().put(
                            propagationStatusTO.getResource(),
                            BulkActionResult.Status.valueOf(propagationStatusTO.getStatus().toString())));
        }

        return modificationResponse(result);
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
                updated = new ProvisioningResult<>();
                updated.setEntity(getAnyLogic().read(patch.getKey()));
        }

        BulkActionResult result = new BulkActionResult();

        if (patch.getAction() == ResourceAssociationAction.LINK) {
            patch.getResources().forEach(resource -> {
                result.getResults().put(
                        resource,
                        updated.getEntity().getResources().contains(resource)
                        ? BulkActionResult.Status.SUCCESS
                        : BulkActionResult.Status.FAILURE);
            });
        } else {
            updated.getPropagationStatuses().forEach(propagationStatusTO
                    -> result.getResults().put(
                            propagationStatusTO.getResource(),
                            BulkActionResult.Status.valueOf(propagationStatusTO.getStatus().toString())));
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
                    bulkAction.getTargets().forEach(key -> {
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
                    });
                } else {
                    throw new BadRequestException();
                }
                break;

            case DELETE:
                bulkAction.getTargets().forEach(key -> {
                    try {
                        result.getResults().put(
                                logic.delete(key, isNullPriorityAsync()).getEntity().getKey(),
                                BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing delete for user {}", key, e);
                        result.getResults().put(key, BulkActionResult.Status.FAILURE);
                    }
                });
                break;

            case SUSPEND:
                if (logic instanceof UserLogic) {
                    bulkAction.getTargets().forEach(key -> {
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
                    });
                } else {
                    throw new BadRequestException();
                }
                break;

            case REACTIVATE:
                if (logic instanceof UserLogic) {
                    bulkAction.getTargets().forEach(key -> {
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
                    });
                } else {
                    throw new BadRequestException();
                }
                break;

            default:
        }

        return modificationResponse(result);
    }

}
