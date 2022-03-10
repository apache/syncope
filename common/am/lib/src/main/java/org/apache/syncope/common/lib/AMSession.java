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
package org.apache.syncope.common.lib;

import java.time.OffsetDateTime;
import org.apache.syncope.common.lib.to.EntityTO;

public class AMSession implements EntityTO {

    private static final long serialVersionUID = 18201657700802L;

    private String key;

    private OffsetDateTime authenticationDate;

    private String principal;

    private String json;

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public OffsetDateTime getAuthenticationDate() {
        return authenticationDate;
    }

    public void setAuthenticationDate(final OffsetDateTime authenticationDate) {
        this.authenticationDate = authenticationDate;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(final String authenticatedPrincipal) {
        this.principal = authenticatedPrincipal;
    }

    public String getJson() {
        return json;
    }

    public void setJson(final String json) {
        this.json = json;
    }
}
