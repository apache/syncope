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

import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.List;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIIdPService;

public class SAML2IdPsRestClient extends BaseRestClient {

    private static final long serialVersionUID = -5084300184764037527L;

    public List<SAML2SP4UIIdPTO> list() {
        return getService(SAML2SP4UIIdPService.class).list();
    }

    public void importIdPs(final InputStream input) {
        SAML2SP4UIIdPService service = getService(SAML2SP4UIIdPService.class);
        Client client = WebClient.client(service);
        client.type(MediaType.APPLICATION_XML);

        service.importFromMetadata(input);

        resetClient(SAML2SP4UIIdPService.class);
    }

    public SAML2SP4UIIdPTO read(final String key) {
        return getService(SAML2SP4UIIdPService.class).read(key);
    }

    public void update(final SAML2SP4UIIdPTO idp) {
        getService(SAML2SP4UIIdPService.class).update(idp);
    }

    public void delete(final String key) {
        getService(SAML2SP4UIIdPService.class).delete(key);
    }
}
