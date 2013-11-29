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
package org.apache.syncope.core.services;

import java.net.URI;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.syncope.common.types.Preference;
import org.apache.syncope.common.types.RESTHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractServiceImpl {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractServiceImpl.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected MessageContext context;

    /**
     * Reads <tt>Prefer</tt> header from request and parses into a <tt>Preference</tt> instance.
     *
     * @return a <tt>Preference</tt> instance matching the passed <tt>Prefer</tt> header,
     * or <tt>Preference.NONE</tt> if missing.
     */
    protected Preference getPreference() {
        return Preference.fromLiteral(context.getHttpHeaders().getHeaderString(RESTHeaders.PREFER));
    }

    /**
     * Builds response to successful <tt>create</tt> request, taking into account any <tt>Prefer</tt> header.
     *
     * @param id identifier of the created entity
     * @param entity the entity just created
     * @return response to successful <tt>create</tt> request
     */
    protected Response.ResponseBuilder createResponse(final Object id, final Object entity) {
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(id)).build();

        Response.ResponseBuilder builder = Response.
                created(location).
                header(RESTHeaders.RESOURCE_ID, id);

        switch (getPreference()) {
            case RETURN_NO_CONTENT:
                break;

            case RETURN_CONTENT:
            case NONE:
            default:
                builder = builder.entity(entity);
                break;

        }
        if (getPreference() == Preference.RETURN_CONTENT || getPreference() == Preference.RETURN_NO_CONTENT) {
            builder = builder.header(RESTHeaders.PREFERENCE_APPLIED, getPreference().literal());
        }

        return builder;
    }

    /**
     * Builds response to successful modification request, taking into account any <tt>Prefer</tt> header.
     *
     * @param entity the entity just modified
     * @return response to successful modification request
     */
    protected Response.ResponseBuilder updateResponse(final Object entity) {
        Response.ResponseBuilder builder;
        switch (getPreference()) {
            case RETURN_NO_CONTENT:
                builder = Response.noContent();
                break;

            case RETURN_CONTENT:
            case NONE:
            default:
                builder = Response.ok(entity);
                break;
        }
        if (getPreference() == Preference.RETURN_CONTENT || getPreference() == Preference.RETURN_NO_CONTENT) {
            builder = builder.header(RESTHeaders.PREFERENCE_APPLIED, getPreference().literal());
        }

        return builder;
    }
}
