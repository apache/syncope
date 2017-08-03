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

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.SAML2IdPService;
import org.apache.syncope.core.logic.SAML2IdPLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SAML2IdPServiceImpl extends AbstractServiceImpl implements SAML2IdPService {

    @Autowired
    private SAML2IdPLogic logic;

    @Override
    public Set<String> getActionsClasses() {
        return logic.getActionsClasses();
    }

    @Override
    public List<SAML2IdPTO> list() {
        return logic.list();
    }

    @Override
    public SAML2IdPTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public Response importFromMetadata(final InputStream input) {
        List<String> imported = logic.importFromMetadata(input);
        if (imported.isEmpty()) {
            return Response.ok().build();
        } else {
            return Response.ok().header(RESTHeaders.RESOURCE_KEY, imported).build();
        }
    }

    @Override
    public void update(final SAML2IdPTO saml2IdpTO) {
        logic.update(saml2IdpTO);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

}
