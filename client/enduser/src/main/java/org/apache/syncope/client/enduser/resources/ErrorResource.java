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

import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mirror REST resource for obtaining user self operations.
 *
 * @see org.apache.syncope.common.rest.api
 */
public class ErrorResource extends AbstractBaseResource {

    private static final long serialVersionUID = -9184809392631523912L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ErrorResource.class);

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {

        ResourceResponse response = new ResourceResponse();
        response.disableCaching();
        response.setContentType(MediaType.APPLICATION_JSON);

        response.setStatusCode(403);

        return response;
    }
}
