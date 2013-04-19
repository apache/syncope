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
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.ConnBundleTO;
import org.apache.syncope.common.to.ConnIdObjectClassTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.core.rest.controller.ConnInstanceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConnectorServiceImpl implements ConnectorService, ContextAware {

    @Autowired
    private ConnInstanceController connInstanceController;

    private UriInfo uriInfo;

    @Override
    public Response create(final ConnInstanceTO connInstanceTO) {
        ConnInstanceTO connInstance = connInstanceController.create(new DummyHTTPServletResponse(), connInstanceTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(connInstance.getId() + "").build();
        return Response.created(location).header(SyncopeConstants.REST_HEADER_ID, connInstance.getId()).build();
    }

    @Override
    public void delete(final Long connInstanceId) {
        connInstanceController.delete(connInstanceId);
    }

    @Override
    public List<ConnBundleTO> getBundles(final String lang) {
        return connInstanceController.getBundles(lang);
    }

    @Override
    public List<ConnConfProperty> getConfigurationProperties(final Long connInstanceId) {
        return connInstanceController.getConfigurationProperties(connInstanceId);
    }

    @Override
    public List<SchemaTO> getSchemaNames(final Long connInstanceId, final ConnInstanceTO connInstanceTO,
            final boolean includeSpecial) {

        List<String> schemaNames = connInstanceController.getSchemaNames(connInstanceTO, includeSpecial);
        List<SchemaTO> result = new ArrayList<SchemaTO>(schemaNames.size());
        for (String name : schemaNames) {
            SchemaTO schemaTO = new SchemaTO();
            schemaTO.setName(name);
            result.add(schemaTO);
        }
        return result;
    }

    @Override
    public List<ConnIdObjectClassTO> getSupportedObjectClasses(final Long connInstanceId,
            final ConnInstanceTO connInstanceTO) {

        List<String> objectClasses = connInstanceController.getSupportedObjectClasses(connInstanceTO);
        List<ConnIdObjectClassTO> result = new ArrayList<ConnIdObjectClassTO>(objectClasses.size());
        for (String objectClass : objectClasses) {
            result.add(new ConnIdObjectClassTO(objectClass));
        }
        return result;
    }

    @Override
    public List<ConnInstanceTO> list(final String lang) {
        return connInstanceController.list(lang);
    }

    @Override
    public ConnInstanceTO read(final Long connInstanceId) {
        return connInstanceController.read(connInstanceId);
    }

    @Override
    public ConnInstanceTO readByResource(final String resourceName) {
        return connInstanceController.readByResource(resourceName);
    }

    @Override
    public void update(final Long connInstanceId, final ConnInstanceTO connInstanceTO) {
        connInstanceController.update(connInstanceTO);
    }

    @Override
    public boolean check(final ConnInstanceTO connInstanceTO) {
        return (Boolean) connInstanceController.check(connInstanceTO).getModel().values().iterator().next();
    }

    @Override
    public void setUriInfo(final UriInfo ui) {
        this.uriInfo = ui;
    }

    @Override
    public void reload() {
        connInstanceController.reload();
    }

    @Override
    public BulkActionRes bulkAction(final BulkAction bulkAction) {
        return connInstanceController.bulkAction(bulkAction);
    }
}
