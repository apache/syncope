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

public interface JAXRSService {

    String PARAM_FIQL = "fiql";

    String PARAM_PAGE = "page";

    String PARAM_SIZE = "size";

    String PARAM_ORDERBY = "orderby";

    String PARAM_KEYWORD = "keyword";

    String PARAM_RESOURCE = "resource";

    String PARAM_NOTIFICATION = "notification";

    String PARAM_ANYTYPE_KIND = "anyTypeKind";

    String PARAM_ENTITY_KEY = "entityKey";

    String PARAM_USER = "user";

    String PARAM_REALM = "realm";

    String PARAM_RECURSIVE = "recursive";

    String PARAM_DETAILS = "details";

    String PARAM_CONNID_PAGED_RESULTS_COOKIE = "connIdPagedResultsCookie";

    String PARAM_MAX = "max";

    String PARAM_ANYTYPEKEY = "anyTypeKey";

    String DOUBLE_DASH = "--";

    String CRLF = "\r\n";

}
