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

import org.apache.syncope.common.lib.to.WAConfigTO;
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
    public List<WAConfigTO> list() {
        return logic.list();
    }

    @Override
    public WAConfigTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public WAConfigTO readByName(final String name) {
        return logic.get(name);
    }

    @Override
    public Response create(final WAConfigTO configTO) {
        final WAConfigTO config = logic.create(configTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(config.getKey()).build();
        return Response.created(location).
            header(RESTHeaders.RESOURCE_KEY, config.getKey()).
            entity(config).
            build();
    }

    @Override
    public void update(final WAConfigTO configTO) {
        logic.update(configTO);
    }

    @Override
    public Response delete(final String key) {
        logic.delete(key);
        return Response.noContent().build();
    }
}
