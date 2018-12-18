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
package org.apache.syncope.client.console.widgets.reconciliation;

import java.io.Serializable;

public class Missing implements Serializable {

    private static final long serialVersionUID = -4779715117027316991L;

    private final String resource;

    private final String connObjectKeyValue;

    public Missing(final String resource, final String connObjectKeyValue) {
        this.resource = resource;
        this.connObjectKeyValue = connObjectKeyValue;
    }

    public String getResource() {
        return resource;
    }

    public String getConnObjectKeyValue() {
        return connObjectKeyValue;
    }
}
