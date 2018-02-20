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
package org.apache.syncope.core.rest.cxf.service;

import java.util.List;
import org.apache.syncope.common.lib.to.ConnInstanceHistoryConfTO;
import org.apache.syncope.common.rest.api.service.ConnectorHistoryService;
import org.apache.syncope.core.logic.ConnectorHistoryLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConnectorHistoryServiceImpl extends AbstractServiceImpl implements ConnectorHistoryService {

    @Autowired
    private ConnectorHistoryLogic logic;

    @Override
    public List<ConnInstanceHistoryConfTO> list(final String connectorKey) {
        return logic.list(connectorKey);
    }

    @Override
    public void restore(final String key) {
        logic.restore(key);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

}
