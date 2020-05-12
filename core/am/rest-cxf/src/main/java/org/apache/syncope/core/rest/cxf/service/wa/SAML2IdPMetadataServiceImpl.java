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

import java.net.URI;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.SAML2IdPMetadataTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.SAML2IdPMetadataService;
import org.apache.syncope.core.logic.SAML2IdPMetadataLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SAML2IdPMetadataServiceImpl extends AbstractServiceImpl implements SAML2IdPMetadataService {

    @Autowired
    private SAML2IdPMetadataLogic logic;

    @Override
    public SAML2IdPMetadataTO getByOwner(final String appliesTo) {
        return logic.get(appliesTo);
    }

    @Override
    public SAML2IdPMetadataTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public Response set(final SAML2IdPMetadataTO saml2IdPMetadataTO) {
        SAML2IdPMetadataTO saml2IdPMetadata = logic.set(saml2IdPMetadataTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(saml2IdPMetadata.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, saml2IdPMetadata.getKey()).
                build();
    }
}
