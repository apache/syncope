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
package org.apache.syncope.client.enduser.resources;

import java.io.IOException;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.adapters.SyncopeTOAdapter;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.wicket.request.resource.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfoResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(InfoResource.class);

    private static final long serialVersionUID = 6453101466981543020L;

    private final SyncopeTOAdapter syncopeTOAdapter;

    public InfoResource() {
        syncopeTOAdapter = new SyncopeTOAdapter();
    }

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {

        ResourceResponse response = new ResourceResponse();

        try {
            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final IResource.Attributes attributes) throws IOException {
                    attributes.getResponse().write(POJOHelper.serialize(syncopeTOAdapter.toSyncopeTORequest(
                            SyncopeEnduserSession.get().getSyncopeTO())));
                }
            });
        response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (Exception e) {
            LOG.error("Error retrieving syncope info", e);
            response.setError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), new StringBuilder()
                    .append("ErrorMessage{{ ")
                    .append(e.getMessage())
                    .append(" }}")
                    .toString());
        }

        return response;
    }
}
