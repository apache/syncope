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
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
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

public abstract class AbstractAnyRestClient<T extends AnyTO> extends BaseRestClient {

    private static final long serialVersionUID = 1962529678091410544L;

    public abstract int count(String realm, String type);

    public abstract List<T> list(String realm, int page, int size, final SortParam<String> sort, final String type);

    public abstract int searchCount(String realm, String fiql, final String type);

    public abstract List<T> search(
            String realm, String fiql, int page, int size, final SortParam<String> sort, final String type);

    public abstract T read(final String key);

    public abstract ProvisioningResult<T> delete(String etag, String key);

    protected <E extends AnyService<T, ?>> ProvisioningResult<T> delete(
            final Class<E> serviceClass, final Class<T> objectType, final String etag, final String key) {
        ProvisioningResult<T> result;
        synchronized (this) {
            final E service = getService(etag, serviceClass);
            result = service.delete(key).readEntity(new GenericType<ProvisioningResult<T>>() {
            });
            resetClient(serviceClass);
        }
        return result;
    }

    public abstract BulkActionResult bulkAction(BulkAction action);

    protected abstract Class<? extends AnyService<?, ?>> getAnyServiceClass();

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

}
