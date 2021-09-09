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
package org.apache.syncope.core.rest.cxf.service.wa;

import java.util.List;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;
import org.apache.syncope.core.logic.wa.WAClientAppLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractService;
import org.springframework.stereotype.Service;

@Service
public class WAClientAppServiceImpl extends AbstractService implements WAClientAppService {

    protected final WAClientAppLogic logic;

    public WAClientAppServiceImpl(final WAClientAppLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<WAClientApp> list() {
        return logic.list();
    }

    @Override
    public WAClientApp read(final Long clientAppId, final ClientAppType type) {
        return logic.read(clientAppId, type);
    }

    @Override
    public WAClientApp read(final String name, final ClientAppType type) {
        return logic.read(name, type);
    }
}
