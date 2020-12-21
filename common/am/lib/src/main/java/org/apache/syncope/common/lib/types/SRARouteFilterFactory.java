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
package org.apache.syncope.common.lib.types;

public enum SRARouteFilterFactory {
    ADD_REQUEST_HEADER,
    ADD_REQUEST_PARAMETER,
    ADD_RESPONSE_HEADER,
    DEDUPE_RESPONSE_HEADER,
    FALLBACK_HEADERS,
    MAP_REQUEST_HEADER,
    PREFIX_PATH,
    PRESERVE_HOST_HEADER,
    REDIRECT_TO,
    REMOVE_REQUEST_HEADER,
    REMOVE_RESPONSE_HEADER,
    REQUEST_RATE_LIMITER,
    REWRITE_PATH,
    REWRITE_LOCATION,
    RETRY,
    SECURE_HEADERS,
    SET_PATH,
    SET_REQUEST_HEADER,
    SET_RESPONSE_HEADER,
    REWRITE_RESPONSE_HEADER,
    SET_STATUS,
    SAVE_SESSION,
    STRIP_PREFIX,
    REQUEST_HEADER_TO_REQUEST_URI,
    SET_REQUEST_SIZE,
    SET_REQUEST_HOST,
    LINK_REWRITE,
    CLIENT_CERTS_TO_REQUEST_HEADER,
    QUERY_PARAM_TO_REQUEST_HEADER,
    PRINCIPAL_TO_REQUEST_HEADER,
    CUSTOM

}
