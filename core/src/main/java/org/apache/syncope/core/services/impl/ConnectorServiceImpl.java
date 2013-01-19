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
package org.apache.syncope.core.services.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.syncope.client.to.ConnBundleTO;
import org.apache.syncope.client.to.ConnInstanceTO;
import org.apache.syncope.client.to.SchemaTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.core.rest.controller.ConnInstanceController;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.services.ConnectorService;
import org.apache.syncope.types.ConnConfProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConnectorServiceImpl implements ConnectorService, ContextAware {

    @Autowired
    private ConnInstanceController connectorController;

    private UriInfo uriInfo;

    @Override
    public Response create(final ConnInstanceTO connectorTO) {
        try {
            ConnInstanceTO connector = connectorController.create(new DummyHTTPServletResponse(), connectorTO);
            URI location = uriInfo.getAbsolutePathBuilder().path(connector.getId() + "").build();
            return Response.created(location).entity(connector).build();
        } catch (SyncopeClientCompositeErrorException e) {
            throw new BadRequestException(e);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public void delete(final Long connectorId) {
        try {
            connectorController.delete(connectorId);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public List<ConnBundleTO> getBundles(final String lang) {
        try {
            return connectorController.getBundles(lang);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        } catch (MissingConfKeyException e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public List<ConnConfProperty> getConfigurationProperties(final Long connectorId) {
        try {
            return connectorController.getConfigurationProperties(connectorId);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public List<SchemaTO> getSchemaNames(final Long connectorId, final ConnInstanceTO connectorTO,
            final boolean showall) {
        try {
            List<String> schemaNames = connectorController.getSchemaNames(new DummyHTTPServletResponse(), connectorTO,
                    showall);
            List<SchemaTO> schemas = new ArrayList<SchemaTO>();
            for (String name : schemaNames) {
                SchemaTO schemaTO = new SchemaTO();
                schemaTO.setName(name);
                schemas.add(schemaTO);
            }
            return schemas;
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public List<ConnInstanceTO> list(final String lang) {
        return connectorController.list(lang);
    }

    @Override
    public ConnInstanceTO read(final Long connectorId) {
        try {
            return connectorController.read(connectorId);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public ConnInstanceTO readConnectorBean(final String resourceName) {
        try {
            return connectorController.readConnectorBean(resourceName);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public void update(final Long connectorId, final ConnInstanceTO connectorTO) {
        try {
            connectorController.update(connectorTO);
        } catch (SyncopeClientCompositeErrorException e) {
            throw new BadRequestException(e);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public boolean validate(final ConnInstanceTO connectorTO) {
        try {
            return (Boolean) connectorController.check(new DummyHTTPServletResponse(), connectorTO).getModel().values()
                    .iterator().next();
        } catch (SyncopeClientCompositeErrorException e) {
            throw new BadRequestException(e);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public void setUriInfo(final UriInfo ui) {
        this.uriInfo = ui;
    }
}
