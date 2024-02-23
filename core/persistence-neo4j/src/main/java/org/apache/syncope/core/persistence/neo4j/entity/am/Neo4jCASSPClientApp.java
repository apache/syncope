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
package org.apache.syncope.core.persistence.neo4j.entity.am;

import jakarta.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.am.CASSPClientApp;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jCASSPClientApp.NODE)
public class Neo4jCASSPClientApp extends AbstractClientApp implements CASSPClientApp {

    public static final String NODE = "CASSPClientApp";

    private static final long serialVersionUID = 6422422526695279794L;

    @NotNull
    private String serviceId;

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public void setServiceId(final String serviceId) {
        this.serviceId = serviceId;
    }
}
