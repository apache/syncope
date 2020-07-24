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

import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.apache.syncope.core.logic.WAConfigLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Service
public class WAConfigServiceImpl extends AbstractServiceImpl implements WAConfigService {
    @Autowired
    private WAConfigLogic logic;

    @Override
    public List<Attr> list() {
        return logic.list();
    }

    @Override
    public Attr read(final String key) {
        return logic.read(key);
    }

    @Override
    public Response create(final Attr configTO) {
        final Attr config = logic.create(configTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(config.getSchema()).build();
        return Response.created(location).
            header(RESTHeaders.RESOURCE_KEY, config.getSchema()).
            entity(config).
            build();
    }

    @Override
    public void update(final Attr configTO) {
        logic.update(configTO);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

    @Override
    public void pushToWA() {
        logic.pushToWA();
    }
}
