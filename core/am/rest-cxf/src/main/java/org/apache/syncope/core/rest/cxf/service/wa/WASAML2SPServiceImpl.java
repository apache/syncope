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
package org.apache.syncope.core.rest.cxf.service.wa;

import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Optional;
import org.apache.syncope.common.rest.api.service.wa.WASAML2SPService;
import org.apache.syncope.core.logic.AuthModuleLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractService;

public class WASAML2SPServiceImpl extends AbstractService implements WASAML2SPService {

    protected final AuthModuleLogic logic;

    public WASAML2SPServiceImpl(final AuthModuleLogic logic) {
        this.logic = logic;
    }

    @Override
    public Response getSAML2SPKeystore(final String clientName) {
        return Optional.ofNullable(logic.readSAML2SPConf(clientName).getKeystore()).
                map(Response::ok).
                orElseGet(() -> Response.status(Response.Status.NOT_FOUND)).
                build();
    }

    @Override
    public void setSAML2SPKeystore(final String clientName, final InputStream keystore) {
        logic.setSAML2SPKeystore(clientName, keystore);
    }

    @Override
    public Response getSAML2SPMetadata(final String clientName) {
        return Optional.ofNullable(logic.readSAML2SPConf(clientName).getServiceProviderMetadata()).
                map(Response::ok).
                orElseGet(() -> Response.status(Response.Status.NOT_FOUND)).
                build();
    }

    @Override
    public void setSAML2SPMetadata(final String clientName, final InputStream metadata) {
        logic.setSAML2SPMetadata(clientName, metadata);
    }
}
