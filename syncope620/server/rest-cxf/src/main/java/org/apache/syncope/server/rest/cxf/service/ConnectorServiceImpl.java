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
package org.apache.syncope.server.rest.cxf.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnIdObjectClassTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.server.logic.ConnectorLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConnectorServiceImpl extends AbstractServiceImpl implements ConnectorService {

    @Autowired
    private ConnectorLogic logic;

    @Override
    public Response create(final ConnInstanceTO connInstanceTO) {
        ConnInstanceTO connInstance = logic.create(connInstanceTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(connInstance.getKey())).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_ID, connInstance.getKey()).
                build();
    }

    @Override
    public void delete(final Long connInstanceKey) {
        logic.delete(connInstanceKey);
    }

    @Override
    public List<ConnBundleTO> getBundles(final String lang) {
        return logic.getBundles(lang);
    }

    @Override
    public List<ConnConfProperty> getConfigurationProperties(final Long connInstanceKey) {
        return logic.getConfigurationProperties(connInstanceKey);
    }

    @Override
    public List<PlainSchemaTO> getSchemaNames(final Long connInstanceKey, final ConnInstanceTO connInstanceTO,
            final boolean includeSpecial) {

        connInstanceTO.setKey(connInstanceKey);

        List<String> schemaNames = logic.getSchemaNames(connInstanceTO, includeSpecial);
        List<PlainSchemaTO> result = new ArrayList<>(schemaNames.size());
        for (String name : schemaNames) {
            PlainSchemaTO schemaTO = new PlainSchemaTO();
            schemaTO.setKey(name);
            result.add(schemaTO);
        }
        return result;
    }

    @Override
    public List<ConnIdObjectClassTO> getSupportedObjectClasses(final Long connInstanceKey,
            final ConnInstanceTO connInstanceTO) {

        connInstanceTO.setKey(connInstanceKey);

        List<String> objectClasses = logic.getSupportedObjectClasses(connInstanceTO);
        List<ConnIdObjectClassTO> result = new ArrayList<>(objectClasses.size());
        for (String objectClass : objectClasses) {
            result.add(new ConnIdObjectClassTO(objectClass));
        }
        return result;
    }

    @Override
    public List<ConnInstanceTO> list(final String lang) {
        return logic.list(lang);
    }

    @Override
    public ConnInstanceTO read(final Long connInstanceKey) {
        return logic.read(connInstanceKey);
    }

    @Override
    public ConnInstanceTO readByResource(final String resourceName) {
        return logic.readByResource(resourceName);
    }

    @Override
    public void update(final Long connInstanceKey, final ConnInstanceTO connInstanceTO) {
        connInstanceTO.setKey(connInstanceKey);
        logic.update(connInstanceTO);
    }

    @Override
    public boolean check(final ConnInstanceTO connInstanceTO) {
        return logic.check(connInstanceTO);
    }

    @Override
    public void reload() {
        logic.reload();
    }

    @Override
    public BulkActionResult bulk(final BulkAction bulkAction) {
        return logic.bulk(bulkAction);
    }
}
