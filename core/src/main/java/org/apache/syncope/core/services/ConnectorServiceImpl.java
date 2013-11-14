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
import org.apache.syncope.common.services.ConnectorService;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.ConnBundleTO;
import org.apache.syncope.common.to.ConnIdObjectClassTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.core.rest.controller.ConnectorController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConnectorServiceImpl extends AbstractServiceImpl implements ConnectorService, ContextAware {

    @Autowired
    private ConnectorController controller;

    @Override
    public Response create(final ConnInstanceTO connInstanceTO) {
        ConnInstanceTO connInstance = controller.create(connInstanceTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(connInstance.getId())).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_ID, connInstance.getId()).
                build();
    }

    @Override
    public void delete(final Long connInstanceId) {
        controller.delete(connInstanceId);
    }

    @Override
    public List<ConnBundleTO> getBundles(final String lang) {
        return controller.getBundles(lang);
    }

    @Override
    public List<ConnConfProperty> getConfigurationProperties(final Long connInstanceId) {
        return controller.getConfigurationProperties(connInstanceId);
    }

    @Override
    public List<SchemaTO> getSchemaNames(final Long connInstanceId, final ConnInstanceTO connInstanceTO,
            final boolean includeSpecial) {

        List<String> schemaNames = controller.getSchemaNames(connInstanceTO, includeSpecial);
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

        List<String> objectClasses = controller.getSupportedObjectClasses(connInstanceTO);
        List<ConnIdObjectClassTO> result = new ArrayList<ConnIdObjectClassTO>(objectClasses.size());
        for (String objectClass : objectClasses) {
            result.add(new ConnIdObjectClassTO(objectClass));
        }
        return result;
    }

    @Override
    public List<ConnInstanceTO> list(final String lang) {
        return controller.list(lang);
    }

    @Override
    public ConnInstanceTO read(final Long connInstanceId) {
        return controller.read(connInstanceId);
    }

    @Override
    public ConnInstanceTO readByResource(final String resourceName) {
        return controller.readByResource(resourceName);
    }

    @Override
    public void update(final Long connInstanceId, final ConnInstanceTO connInstanceTO) {
        controller.update(connInstanceTO);
    }

    @Override
    public boolean check(final ConnInstanceTO connInstanceTO) {
        return controller.check(connInstanceTO);
    }

    @Override
    public void reload() {
        controller.reload();
    }

    @Override
    public BulkActionRes bulk(final BulkAction bulkAction) {
        return controller.bulk(bulkAction);
    }
}
