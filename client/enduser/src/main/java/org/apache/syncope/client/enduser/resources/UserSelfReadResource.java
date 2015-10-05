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
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.adapters.UserTOAdapter;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.wicket.request.resource.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mirror REST resource for obtaining user self operations.
 *
 * @see org.apache.syncope.common.rest.api
 */
public class UserSelfReadResource extends AbstractResource {

    private static final long serialVersionUID = -9184809392631523912L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UserSelfReadResource.class);

    private final UserTOAdapter userTOAdapter;

    public UserSelfReadResource() {
        userTOAdapter = new UserTOAdapter();
    }

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {

        ResourceResponse response = new ResourceResponse();
        final String selfTOJson = POJOHelper.serialize(userTOAdapter.toUserTORequest(SyncopeEnduserSession.get().
                getSelfTO()));

        response.setWriteCallback(new WriteCallback() {

            @Override
            public void writeData(final Attributes attributes) throws IOException {
                attributes.getResponse().write(selfTOJson);
            }
        });

        return response;
    }
}
