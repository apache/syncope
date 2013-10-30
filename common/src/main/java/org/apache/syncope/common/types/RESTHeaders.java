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
package org.apache.syncope.common.types;

/**
 * Custom HTTP headers in use with REST services.
 */
public enum RESTHeaders {

    /**
     * UserId option key.
     */
    USER_ID("Syncope.UserId"),
    /**
     * Username option key.
     */
    USERNAME("Syncope.Username"),
    /**
     * Option key stating if user request create is allowed or not.
     */
    USERREQUEST_CREATE_ALLOWED("Syncope.UserRequestCreate.Allowed"),
    /**
     * HTTP header key for object ID assigned to an object after its creation.
     */
    RESOURCE_ID("Syncope.Id"),
    /**
     * Declares the type of exception being raised.
     */
    EXCEPTION_TYPE("Syncope.ExceptionType"),
    /**
     * Not (yet) defined in <tt>javax.ws.rs.core.HttpHeaders</tt>.
     *
     * @see javax.ws.rs.core.HttpHeaders
     */
    CONTENT_DISPOSITION("Content-Disposition");

    private final String name;

    private RESTHeaders(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
