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
package org.apache.syncope.common.rest.api.service;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.apache.syncope.common.lib.to.ErrorTO;
import org.apache.syncope.common.rest.api.RESTHeaders;

@ApiResponses(
        @ApiResponse(responseCode = "400",
                description = "An error occurred; HTTP status code can vary depending on the actual error: "
                + "400, 403, 404, 409, 412",
                content =
                @Content(schema =
                        @Schema(implementation = ErrorTO.class)),
                headers = {
                    @Header(name = RESTHeaders.ERROR_CODE, schema =
                            @Schema(type = "string"), description = "Error code"),
                    @Header(name = RESTHeaders.ERROR_INFO, schema =
                            @Schema(type = "string"), description = "Error message") })
)
public interface JAXRSService {

    String PARAM_FIQL = "fiql";

    String PARAM_PAGE = "page";

    String PARAM_SIZE = "size";

    String PARAM_ORDERBY = "orderby";

    String PARAM_RESOURCE = "resource";

    String PARAM_NOTIFICATION = "notification";

    String PARAM_ANYTYPE_KIND = "anyTypeKind";

    String PARAM_ENTITY_KEY = "entityKey";

    String PARAM_DETAILS = "details";

    String PARAM_CONNID_PAGED_RESULTS_COOKIE = "connIdPagedResultsCookie";

    String PARAM_MAX = "max";

}
