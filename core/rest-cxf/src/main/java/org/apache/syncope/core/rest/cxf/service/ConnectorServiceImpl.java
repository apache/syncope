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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnIdObjectClassTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.wrap.BooleanWrap;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.core.logic.ConnectorLogic;
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
                header(RESTHeaders.RESOURCE_KEY, connInstance.getKey()).
                build();
    }

    @Override
    public void delete(final Long key) {
        logic.delete(key);
    }

    @Override
    public List<ConnBundleTO> getBundles(final String lang) {
        return logic.getBundles(lang);
    }

    @Override
    public List<ConnConfProperty> getConfigurationProperties(final Long key) {
        return logic.getConfigurationProperties(key);
    }

    @Override
    public List<PlainSchemaTO> getSchemaNames(final Long key, final ConnInstanceTO connInstanceTO,
            final boolean includeSpecial) {

        connInstanceTO.setKey(key);

        return CollectionUtils.collect(logic.getSchemaNames(connInstanceTO, includeSpecial),
                new Transformer<String, PlainSchemaTO>() {

                    @Override
                    public PlainSchemaTO transform(final String name) {
                        PlainSchemaTO schemaTO = new PlainSchemaTO();
                        schemaTO.setKey(name);
                        return schemaTO;
                    }
                }, new ArrayList<PlainSchemaTO>());
    }

    @Override
    public List<ConnIdObjectClassTO> getSupportedObjectClasses(final Long key,
            final ConnInstanceTO connInstanceTO) {

        connInstanceTO.setKey(key);

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
    public ConnInstanceTO read(final Long key) {
        return logic.read(key);
    }

    @Override
    public ConnInstanceTO readByResource(final String resourceName) {
        return logic.readByResource(resourceName);
    }

    @Override
    public void update(final Long key, final ConnInstanceTO connInstanceTO) {
        connInstanceTO.setKey(key);
        logic.update(connInstanceTO);
    }

    @Override
    public BooleanWrap check(final ConnInstanceTO connInstanceTO) {
        BooleanWrap result = new BooleanWrap();
        result.setElement(logic.check(connInstanceTO));
        return result;
    }

    @Override
    public void reload() {
        logic.reload();
    }

    @Override
    public BulkActionResult bulk(final BulkAction bulkAction) {
        BulkActionResult result = new BulkActionResult();

        if (bulkAction.getOperation() == BulkAction.Type.DELETE) {
            for (String key : bulkAction.getTargets()) {
                try {
                    result.getResults().put(
                            String.valueOf(logic.delete(Long.valueOf(key)).getKey()), BulkActionResult.Status.SUCCESS);
                } catch (Exception e) {
                    LOG.error("Error performing delete for connector {}", key, e);
                    result.getResults().put(key, BulkActionResult.Status.FAILURE);
                }
            }
        }

        return result;
    }
}
