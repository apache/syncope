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

public enum FilterFactory {
    ADD_REQUEST_HEADER,
    ADD_REQUEST_PARAMETER,
    ADD_RESPONSE_HEADER,
    HYSTRIX,
    FALLBACK_HEADERS,
    PREFIX_PATH,
    PRESERVE_HOST_HEADER,
    REDIRECT,
    REMOVE_REQUEST_HEADER,
    REMOVE_RESPONSE_HEADER,
    REQUEST_RATE_LIMITER,
    REWRITE_PATH,
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
    CUSTOM

}
