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
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

/**
 * Console client for invoking Rest any type class services.
 */
public class AnyObjectRestClient extends AbstractAnyRestClient<AnyObjectTO> {

    private static final long serialVersionUID = -8874495991295283249L;

    @Override
    protected Class<? extends AnyService<?, ?>> getAnyServiceClass() {
        return AnyObjectService.class;
    }

    @Override
    public int count(final String realm, final String type) {
        return getService(AnyObjectService.class).list(
                type, new AnyListQuery.Builder().realm(realm).page(1).size(1).build()).getTotalCount();
    }

    @Override
    public List<AnyObjectTO> list(
            final String realm, final int page, final int size, final SortParam<String> sort, final String type) {

        return list(type, realm).getResult();
    }

    public PagedResult<AnyObjectTO> list(final String type, final String realm) {
        return getService(AnyObjectService.class).
                list(type, new AnyListQuery.Builder().realm(realm).build());
    }

    @Override
    public int searchCount(final String realm, final String fiql, final String type) {
        return getService(AnyObjectService.class).
                search(new AnySearchQuery.Builder().realm(realm).fiql(fiql).page(1).size(1).build()).
                getTotalCount();
    }

    @Override
    public List<AnyObjectTO> search(final String realm, final String fiql, final int page, final int size,
            final SortParam<String> sort,
            final String type) {

        return getService(AnyObjectService.class).search(
                new AnySearchQuery.Builder().realm(realm).fiql(fiql).page(page).size(size).
                orderBy(toOrderBy(sort)).details(false).build()).getResult();
    }

    @Override
    public AnyObjectTO read(final Long key) {
        return getService(AnyObjectService.class).read(key);
    }

    public ProvisioningResult<AnyObjectTO> create(final AnyObjectTO anyObjectTO) {
        Response response = getService(AnyObjectService.class).create(anyObjectTO);
        return response.readEntity(new GenericType<ProvisioningResult<AnyObjectTO>>() {
        });
    }

    public ProvisioningResult<AnyObjectTO> update(final String etag, final AnyObjectPatch anyObjectPatch) {
        ProvisioningResult<AnyObjectTO> result;
        synchronized (this) {
            AnyObjectService service = getService(etag, AnyObjectService.class);
            result = service.update(anyObjectPatch).readEntity(new GenericType<ProvisioningResult<AnyObjectTO>>() {
            });
            resetClient(AnyObjectService.class);
        }
        return result;
    }

    @Override
    public ProvisioningResult<AnyObjectTO> delete(final String etag, final Long key) {
        return delete(AnyObjectService.class, AnyObjectTO.class, etag, key);
    }

    @Override
    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(AnyObjectService.class).bulk(action);
    }
}
