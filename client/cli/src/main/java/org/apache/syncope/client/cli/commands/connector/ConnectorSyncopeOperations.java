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
package org.apache.syncope.client.cli.commands.connector;

import java.util.List;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.rest.api.service.ConnectorService;

public class ConnectorSyncopeOperations {

    private final ConnectorService connectorService = SyncopeServices.get(ConnectorService.class);

    public ConnInstanceTO readByResource(final String resourceName) {
        return connectorService.readByResource(resourceName, null);
    }

    public ConnInstanceTO read(final String resourceKey) {
        return connectorService.read(resourceKey, null);
    }

    public List<ConnBundleTO> getBundles() {
        return connectorService.getBundles(null);
    }

    public List<ConnInstanceTO> list() {
        return connectorService.list(null);
    }

    public void delete(final String resourceKey) {
        connectorService.delete(resourceKey);
    }
}
