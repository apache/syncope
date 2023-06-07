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

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.syncope.common.rest.api.service.DynRealmService;
import org.apache.syncope.common.rest.api.service.RealmService;

/**
 * Console client for invoking REST Realm's services.
 */
public class RealmRestClient extends BaseRestClient {

    private static final long serialVersionUID = -8549081557283519638L;

    public PagedResult<RealmTO> search(final RealmQuery query) {
        return getService(RealmService.class).search(query);
    }

    public List<DynRealmTO> listDynRealms() {
        return getService(DynRealmService.class).list();
    }

    public DynRealmTO readDynRealm(final String key) {
        return getService(DynRealmService.class).read(key);
    }

    public ProvisioningResult<RealmTO> create(final String parentPath, final RealmTO realmTO) {
        final Response response = getService(RealmService.class).create(parentPath, realmTO);
        return response.readEntity(new GenericType<>() {
        });
    }

    public ProvisioningResult<RealmTO> update(final RealmTO realmTO) {
        final Response response = getService(RealmService.class).update(realmTO);
        return response.readEntity(new GenericType<>() {
        });
    }

    public void delete(final String fullPath) {
        getService(RealmService.class).delete(fullPath);
    }
}
