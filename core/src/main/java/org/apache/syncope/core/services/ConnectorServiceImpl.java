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
package org.apache.syncope.core.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.ConnectorService;
import org.apache.syncope.common.to.ConnBundleTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.core.rest.controller.ConnInstanceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConnectorServiceImpl implements ConnectorService, ContextAware {

    @Autowired
    private ConnInstanceController connectorController;

    private UriInfo uriInfo;

    @Override
    public Response create(final ConnInstanceTO connectorTO) {
        ConnInstanceTO connector = connectorController.create(new DummyHTTPServletResponse(), connectorTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(connector.getId() + "").build();
        return Response.created(location).header(SyncopeConstants.REST_HEADER_ID, connector.getId()).build();
    }

    @Override
    public void delete(final Long connectorId) {
        connectorController.delete(connectorId);
    }

    @Override
    public List<ConnBundleTO> getBundles(final String lang) {
        return connectorController.getBundles(lang);
    }

    @Override
    public List<ConnConfProperty> getConfigurationProperties(final Long connectorId) {
        return connectorController.getConfigurationProperties(connectorId);
    }

    @Override
    public List<SchemaTO> getSchemaNames(final Long connectorId, final ConnInstanceTO connectorTO,
            final boolean showall) {
        List<String> schemaNames = connectorController.getSchemaNames(new DummyHTTPServletResponse(), connectorTO,
                showall);
        List<SchemaTO> schemas = new ArrayList<SchemaTO>();
        for (String name : schemaNames) {
            SchemaTO schemaTO = new SchemaTO();
            schemaTO.setName(name);
            schemas.add(schemaTO);
        }
        return schemas;
    }

    @Override
    public List<ConnInstanceTO> list(final String lang) {
        return connectorController.list(lang);
    }

    @Override
    public ConnInstanceTO read(final Long connectorId) {
        return connectorController.read(connectorId);
    }

    @Override
    public ConnInstanceTO readConnectorBean(final String resourceName) {
        return connectorController.readConnectorBean(resourceName);
    }

    @Override
    public void update(final Long connectorId, final ConnInstanceTO connectorTO) {
        connectorController.update(connectorTO);
    }

    @Override
    public boolean validate(final ConnInstanceTO connectorTO) {
        return (Boolean) connectorController.check(new DummyHTTPServletResponse(), connectorTO).getModel().values()
                .iterator().next();
    }

    @Override
    public void setUriInfo(final UriInfo ui) {
        this.uriInfo = ui;
    }
}
