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
package org.apache.syncope.common.rest.api;

import jakarta.ws.rs.core.MediaType;

/**
 * Custom HTTP headers in use with REST services.
 */
public final class RESTHeaders {

    public static final String DOMAIN = "X-Syncope-Domain";

    public static final String TOKEN = "X-Syncope-Token";

    public static final String TOKEN_EXPIRE = "X-Syncope-Token-Expire";

    public static final String OWNED_ENTITLEMENTS = "X-Syncope-Entitlements";

    public static final String DELEGATED_BY = "X-Syncope-Delegated-By";

    public static final String DELEGATIONS = "X-Syncope-Delegations";

    public static final String RESOURCE_KEY = "X-Syncope-Key";

    public static final String CONNOBJECT_KEY = "X-Syncope-ConnObject-Key";

    /**
     * Asks for asynchronous propagation towards external resources with null priority.
     */
    public static final String NULL_PRIORITY_ASYNC = "X-Syncope-Null-Priority-Async";

    /**
     * Declares the type of exception being raised.
     *
     * @see org.apache.syncope.common.lib.types.ClientExceptionType
     */
    public static final String ERROR_CODE = "X-Application-Error-Code";

    /**
     * Declares additional information for the exception being raised.
     */
    public static final String ERROR_INFO = "X-Application-Error-Info";

    /**
     * Mediatype for PNG images, not defined in {@link jakarta.ws.rs.core.MediaType}.
     */
    public static final String MEDIATYPE_IMAGE_PNG = "image/png";

    /**
     * Mediatype for YAML, not defined in {@link jakarta.ws.rs.core.MediaType}.
     */
    public static final String APPLICATION_YAML = "application/yaml";

    /**
     * Mediatype for YAML, not defined in {@link jakarta.ws.rs.core.MediaType}.
     */
    public static final MediaType APPLICATION_YAML_TYPE = new MediaType("application", "yaml");

    /**
     * Mediatype for text/csv, not defined in {@link jakarta.ws.rs.core.MediaType}.
     */
    public static final String TEXT_CSV = "text/csv";

    /**
     * Mediatype for text/csv, not defined in {@link jakarta.ws.rs.core.MediaType}.
     */
    public static final MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");

    /**
     * Mediatype for multipart/mixed, not defined in {@link jakarta.ws.rs.core.MediaType}.
     */
    public static final String MULTIPART_MIXED = "multipart/mixed";

    /**
     * The boundary parameter name for multipart, not defined in {@link jakarta.ws.rs.core.MediaType}.
     */
    public static final String BOUNDARY_PARAMETER = "boundary";

    /**
     * Builds Content-Type string for multipart/mixed and the given boundary.
     *
     * @param boundary multipart boundary value
     * @return multipart/mixed Content-Type string, with given boundary
     */
    public static String multipartMixedWith(final String boundary) {
        return MULTIPART_MIXED + ';' + BOUNDARY_PARAMETER + '=' + boundary;
    }

    /**
     * Allows the client to specify a preference for the result to be returned from the server.
     * <a href="http://msdn.microsoft.com/en-us/library/hh537533.aspx">More information</a>.
     *
     * @see Preference
     */
    public static final String PREFER = "Prefer";

    /**
     * Allows the server to inform the client about the fact that a specified preference was applied.
     * <a href="http://msdn.microsoft.com/en-us/library/hh554623.aspx">More information</a>.
     *
     * @see Preference
     */
    public static final String PREFERENCE_APPLIED = "Preference-Applied";

    private RESTHeaders() {
        // Empty constructor for static utility class.
    }
}
