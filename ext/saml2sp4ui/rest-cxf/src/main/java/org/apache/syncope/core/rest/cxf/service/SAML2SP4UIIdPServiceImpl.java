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

import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIIdPService;
import org.apache.syncope.core.logic.SAML2SP4UIIdPLogic;

public class SAML2SP4UIIdPServiceImpl extends AbstractService implements SAML2SP4UIIdPService {

    protected final SAML2SP4UIIdPLogic logic;

    public SAML2SP4UIIdPServiceImpl(final SAML2SP4UIIdPLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<SAML2SP4UIIdPTO> list() {
        return logic.list();
    }

    @Override
    public SAML2SP4UIIdPTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public Response importFromMetadata(final InputStream input) {
        return Response.status(Response.Status.CREATED).
                header(RESTHeaders.RESOURCE_KEY, logic.importFromMetadata(input)).build();
    }

    @Override
    public void update(final SAML2SP4UIIdPTO saml2IdpTO) {
        logic.update(saml2IdpTO);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }
}
