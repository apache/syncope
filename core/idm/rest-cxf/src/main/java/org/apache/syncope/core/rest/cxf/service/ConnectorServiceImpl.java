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

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.apache.syncope.common.lib.to.ConnIdBundle;
import org.apache.syncope.common.lib.to.ConnIdObjectClass;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.core.logic.ConnectorLogic;

public class ConnectorServiceImpl extends AbstractService implements ConnectorService {

    protected final ConnectorLogic logic;

    public ConnectorServiceImpl(final ConnectorLogic logic) {
        this.logic = logic;
    }

    @Override
    public Response create(final ConnInstanceTO connInstanceTO) {
        ConnInstanceTO connInstance = logic.create(connInstanceTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(connInstance.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, connInstance.getKey()).
                build();
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

    @Override
    public List<ConnIdBundle> getBundles(final String lang) {
        return logic.getBundles(lang);
    }

    @Override
    public List<ConnIdObjectClass> buildObjectClassInfo(
            final ConnInstanceTO connInstanceTO, final boolean includeSpecial) {

        return logic.buildObjectClassInfo(connInstanceTO, includeSpecial);
    }

    @Override
    public List<ConnInstanceTO> list(final String lang) {
        return logic.list(lang);
    }

    @Override
    public ConnInstanceTO read(final String key, final String lang) {
        return logic.read(key, lang);
    }

    @Override
    public ConnInstanceTO readByResource(final String resourceName, final String lang) {
        return logic.readByResource(resourceName, lang);
    }

    @Override
    public void update(final ConnInstanceTO connInstanceTO) {
        logic.update(connInstanceTO);
    }

    @Override
    public void check(final ConnInstanceTO connInstanceTO) {
        logic.check(connInstanceTO);
    }

    @Override
    public void reload() {
        logic.reload();
    }
}
