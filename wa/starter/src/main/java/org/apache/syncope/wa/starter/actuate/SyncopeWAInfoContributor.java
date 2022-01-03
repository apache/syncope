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
package org.apache.syncope.wa.starter.actuate;

import org.apache.syncope.wa.bootstrap.WAProperties;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.security.access.prepost.PreAuthorize;

public class SyncopeWAInfoContributor implements InfoContributor {

    protected final WAProperties waProperties;

    public SyncopeWAInfoContributor(final WAProperties waProperties) {
        this.waProperties = waProperties;
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    public void contribute(final Info.Builder builder) {
        builder.withDetail("waProperties", waProperties);
    }
}
