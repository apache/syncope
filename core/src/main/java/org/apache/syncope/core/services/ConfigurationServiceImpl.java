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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.to.ConfigurationTO;
import org.apache.syncope.common.to.MailTemplateTO;
import org.apache.syncope.common.to.ValidatorTO;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.persistence.dao.impl.ContentLoader;
import org.apache.syncope.core.rest.controller.ConfigurationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationServiceImpl extends AbstractServiceImpl implements ConfigurationService, ContextAware {

    @Autowired
    private ConfigurationController controller;

    @Override
    public Response create(final ConfigurationTO configurationTO) {
        ConfigurationTO created = controller.create(configurationTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_ID.toString(), created.getKey()).
                build();
    }

    @Override
    public Response export() {
        StreamingOutput sout = new StreamingOutput() {

            @Override
            public void write(final OutputStream os) throws IOException {
                controller.export(os);
            }
        };
        return Response.ok(sout).
                type(MediaType.TEXT_XML).
                header(RESTHeaders.CONTENT_DISPOSITION.toString(), "attachment; filename=" + ContentLoader.CONTENT_XML).
                build();
    }

    @Override
    public void delete(final String key) {
        controller.delete(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MailTemplateTO> getMailTemplates() {
        return CollectionWrapper.wrap(controller.getMailTemplates(), MailTemplateTO.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ValidatorTO> getValidators() {
        return CollectionWrapper.wrap(controller.getValidators(), ValidatorTO.class);
    }

    @Override
    public List<ConfigurationTO> list() {
        return controller.list();
    }

    @Override
    public ConfigurationTO read(final String key) {
        return controller.read(key);

    }

    @Override
    public void update(final String key, final ConfigurationTO configurationTO) {
        controller.update(configurationTO);
    }
}
