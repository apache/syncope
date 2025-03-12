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
import java.io.InputStream;
import java.util.ArrayList;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.common.rest.api.beans.CSVPushSpec;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.service.ReconciliationService;

public class ReconciliationRestClient extends BaseRestClient {

    private static final long serialVersionUID = -3161863874876938094L;

    public ReconStatus status(final ReconQuery reconQuery) {
        return getService(ReconciliationService.class).status(reconQuery);
    }

    public void push(final ReconQuery reconQuery, final PushTaskTO pushTask) {
        getService(ReconciliationService.class).push(reconQuery, pushTask);
    }

    public void pull(final ReconQuery reconQuery, final PullTaskTO pullTask) {
        getService(ReconciliationService.class).pull(reconQuery, pullTask);
    }

    public Response push(final AnyQuery anyQuery, final CSVPushSpec spec) {
        ReconciliationService service = getService(ReconciliationService.class);
        Client client = WebClient.client(service);
        client.accept(RESTHeaders.TEXT_CSV);

        Response response = service.push(anyQuery, spec);

        resetClient(ReconciliationService.class);

        return response;
    }

    public ArrayList<ProvisioningReport> pull(final CSVPullSpec spec, final InputStream csv) {
        ReconciliationService service = getService(ReconciliationService.class);
        Client client = WebClient.client(service);
        client.type(RESTHeaders.TEXT_CSV);

        ArrayList<ProvisioningReport> result = new ArrayList<>(service.pull(spec, csv));

        resetClient(ReconciliationService.class);

        return result;
    }
}
