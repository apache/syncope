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
package org.apache.syncope.client.console.rest;

import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.lib.to.ConnInstanceHistoryConfTO;
import org.apache.syncope.common.rest.api.service.ConnectorHistoryService;

/**
 * Console client for invoking Rest Connector configuration history services.
 */
public class ConnectorHistoryRestClient extends BaseRestClient {

    private static final long serialVersionUID = -1917949374689773018L;

    public List<ConnInstanceHistoryConfTO> list(final String key) {
        List<ConnInstanceHistoryConfTO> connHistoryConfs = Collections.<ConnInstanceHistoryConfTO>emptyList();
        try {
            connHistoryConfs = getService(ConnectorHistoryService.class).list(key);
        } catch (Exception e) {
            LOG.error("While reading connector history configuration instances", e);
        }
        return connHistoryConfs;
    }

    public void delete(final String key) {
        getService(ConnectorHistoryService.class).delete(key);
    }

    public void restore(final String key) {
        getService(ConnectorHistoryService.class).restore(key);
    }
}
