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

import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.common.lib.to.OIDCOpEntityTO;
import org.apache.syncope.common.rest.api.service.OIDCOpEntityService;

public class OIDCOpEntityRestClient extends BaseRestClient {

    private static final long serialVersionUID = -1392090291817187902L;

    public OIDCOpEntityTO generate(final String jwksKeyId, final String jwksType, final int jwksKeySize) {
        Response response = getService(OIDCOpEntityService.class).generate(jwksKeyId, jwksType, jwksKeySize);
        return response.readEntity(OIDCOpEntityTO.class);
    }

    public Mutable<OIDCOpEntityTO> get() {
        MutableObject<OIDCOpEntityTO> result = new MutableObject<>();
        try {
            result.setValue(getService(OIDCOpEntityService.class).get());
        } catch (Exception e) {
            LOG.debug("While getting OIDC JKS", e);
        }
        return result;
    }

    public void set(final OIDCOpEntityTO oidcOpEntity) {
        getService(OIDCOpEntityService.class).set(oidcOpEntity);
    }

    public void delete() {
        getService(OIDCOpEntityService.class).delete();
    }
}
