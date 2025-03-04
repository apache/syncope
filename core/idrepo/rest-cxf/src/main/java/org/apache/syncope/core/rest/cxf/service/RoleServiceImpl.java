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

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.apache.syncope.core.logic.RoleLogic;

public class RoleServiceImpl extends AbstractService implements RoleService {

    protected final RoleLogic logic;

    public RoleServiceImpl(final RoleLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<RoleTO> list() {
        return logic.list();
    }

    @Override
    public RoleTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public Response create(final RoleTO roleTO) {
        RoleTO created = logic.create(roleTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created.getKey()).
                build();
    }

    @Override
    public void update(final RoleTO roleTO) {
        logic.update(roleTO);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

    @Override
    public Response getAnyLayout(final String key) {
        String template = logic.getAnyLayout(key);
        StreamingOutput sout = (os) -> os.write(template.getBytes());

        return Response.ok(sout).
                type(MediaType.APPLICATION_JSON_TYPE).
                build();
    }

    @Override
    public void setAnyLayout(final String key, final InputStream anyLayoutIn) {
        try {
            logic.setAnyLayout(key, IOUtils.toString(anyLayoutIn, StandardCharsets.UTF_8.name()));
        } catch (final IOException e) {
            LOG.error("While setting console layout info for role {}", key, e);
            throw new InternalServerErrorException("Could not read entity", e);
        }
    }

    @Override
    public void removeAnyLayout(final String key) {
        logic.setAnyLayout(key, null);
    }
}
