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
package org.apache.syncope.client.console.rest;

import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AssociationPatch;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

public abstract class AbstractAnyRestClient<TO extends AnyTO, P extends AnyPatch> extends BaseRestClient {

    private static final long serialVersionUID = 1962529678091410544L;

    protected abstract Class<? extends AnyService<TO, P>> getAnyServiceClass();

    public abstract int searchCount(String realm, String fiql, String type);

    public abstract List<TO> search(String realm, String fiql, int page, int size, SortParam<String> sort, String type);

    public TO read(final String key) {
        return getService(getAnyServiceClass()).read(key);
    }

    public ProvisioningResult<TO> create(final TO to) {
        Response response = getService(getAnyServiceClass()).create(to);
        return response.readEntity(new GenericType<ProvisioningResult<TO>>() {
        });
    }

    public ProvisioningResult<TO> update(final String etag, final P patch) {
        ProvisioningResult<TO> result;
        synchronized (this) {
            result = getService(etag, getAnyServiceClass()).update(patch).
                    readEntity(new GenericType<ProvisioningResult<TO>>() {
                    });
            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public ProvisioningResult<TO> delete(final String etag, final String key) {
        ProvisioningResult<TO> result;
        synchronized (this) {
            result = getService(etag, getAnyServiceClass()).delete(key).
                    readEntity(new GenericType<ProvisioningResult<TO>>() {
                    });
            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public BulkActionResult unlink(final String etag, final String key, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            DeassociationPatch deassociationPatch = new DeassociationPatch();
            deassociationPatch.setKey(key);
            deassociationPatch.setAction(ResourceDeassociationAction.UNLINK);
            deassociationPatch.getResources().addAll(StatusUtils.buildStatusPatch(statuses).getResources());

            result = service.deassociate(deassociationPatch).readEntity(BulkActionResult.class);

            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public BulkActionResult link(final String etag, final String key, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            StatusPatch statusPatch = StatusUtils.buildStatusPatch(statuses);

            AssociationPatch associationPatch = new AssociationPatch();
            associationPatch.setKey(key);
            associationPatch.setAction(ResourceAssociationAction.LINK);
            associationPatch.setOnSyncope(statusPatch.isOnSyncope());
            associationPatch.getResources().addAll(statusPatch.getResources());

            result = service.associate(associationPatch).readEntity(BulkActionResult.class);

            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public BulkActionResult deprovision(final String etag, final String key, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            DeassociationPatch deassociationPatch = new DeassociationPatch();
            deassociationPatch.setKey(key);
            deassociationPatch.setAction(ResourceDeassociationAction.DEPROVISION);
            deassociationPatch.getResources().addAll(StatusUtils.buildStatusPatch(statuses).getResources());

            result = service.deassociate(deassociationPatch).readEntity(BulkActionResult.class);

            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public BulkActionResult provision(final String etag, final String key, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            StatusPatch statusPatch = StatusUtils.buildStatusPatch(statuses);

            AssociationPatch associationPatch = new AssociationPatch();
            associationPatch.setKey(key);
            associationPatch.setAction(ResourceAssociationAction.PROVISION);
            associationPatch.setOnSyncope(statusPatch.isOnSyncope());
            associationPatch.getResources().addAll(statusPatch.getResources());

            result = service.associate(associationPatch).readEntity(BulkActionResult.class);

            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public BulkActionResult unassign(final String etag, final String key, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            DeassociationPatch deassociationPatch = new DeassociationPatch();
            deassociationPatch.setKey(key);
            deassociationPatch.setAction(ResourceDeassociationAction.UNASSIGN);
            deassociationPatch.getResources().addAll(StatusUtils.buildStatusPatch(statuses).getResources());

            result = service.deassociate(deassociationPatch).readEntity(BulkActionResult.class);

            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public BulkActionResult assign(final String etag, final String key, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            AnyService<?, ?> service = getService(etag, getAnyServiceClass());

            StatusPatch statusPatch = StatusUtils.buildStatusPatch(statuses);

            AssociationPatch associationPatch = new AssociationPatch();
            associationPatch.setKey(key);
            associationPatch.setAction(ResourceAssociationAction.ASSIGN);
            associationPatch.setOnSyncope(statusPatch.isOnSyncope());
            associationPatch.getResources().addAll(statusPatch.getResources());

            result = service.associate(associationPatch).readEntity(BulkActionResult.class);

            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(getAnyServiceClass()).bulk(action).readEntity(BulkActionResult.class);
    }
}
