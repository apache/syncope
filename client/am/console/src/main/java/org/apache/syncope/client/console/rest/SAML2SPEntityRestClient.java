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

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.common.rest.api.service.SAML2SPEntityService;

public class SAML2SPEntityRestClient extends BaseRestClient {

    private static final long serialVersionUID = -1392090291817187902L;

    public List<SAML2SPEntityTO> list() {
        return new ArrayList<>(getService(SAML2SPEntityService.class).list());
    }

    public SAML2SPEntityTO get(final String key) {
        return getService(SAML2SPEntityService.class).get(key);
    }

    public void set(final SAML2SPEntityTO entityTO) {
        getService(SAML2SPEntityService.class).set(entityTO);
    }

    public void delete(final String key) {
        getService(SAML2SPEntityService.class).delete(key);
    }
}
