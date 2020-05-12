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

import org.apache.syncope.common.lib.to.SAML2SPKeystoreTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.SAML2SPKeystoreService;
import org.apache.syncope.core.logic.SAML2SPKeystoreLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import java.net.URI;

@Service
public class SAML2SPKeystoreServiceImpl extends AbstractServiceImpl implements SAML2SPKeystoreService {

    @Autowired
    private SAML2SPKeystoreLogic logic;

    @Override
    public SAML2SPKeystoreTO getByOwner(final String name) {
        return logic.get(name);
    }

    @Override
    public SAML2SPKeystoreTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public Response set(final SAML2SPKeystoreTO keystoreTO) {
        final SAML2SPKeystoreTO keystore = logic.set(keystoreTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(keystore.getKey()).build();
        return Response.created(location).
            header(RESTHeaders.RESOURCE_KEY, keystore.getKey()).
            build();
    }
}
