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

public enum OIDCResponseType {
    CODE("code"),
    TOKEN("token"),
    ID_TOKEN("id_token"),
    ID_TOKEN_TOKEN("id_token token"),
    DEVICE_CODE("device_code");

    private final String externalForm;

    OIDCResponseType(final String external) {
        this.externalForm = external;
    }

    public String getExternalForm() {
        return externalForm;
    }
}
