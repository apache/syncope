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
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.to.ConfigurationTO;
import org.apache.syncope.common.to.MailTemplateTO;
import org.apache.syncope.common.to.ValidatorTO;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.rest.controller.ConfigurationController;
import org.apache.syncope.core.util.ImportExport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationServiceImpl implements ConfigurationService, ContextAware {

    @Autowired
    private ConfigurationController configurationController;

    private UriInfo uriInfo;

    @Override
    public Response create(final ConfigurationTO configurationTO) {
        ConfigurationTO created = configurationController.create(new DummyHTTPServletResponse(), configurationTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getKey()).build();
        return Response.created(location).
                header(SyncopeConstants.REST_HEADER_ID, created.getKey()).
                build();
    }

    @Override
    public Response dbExport() {
        StreamingOutput sout = new StreamingOutput() {

            @Override
            public void write(final OutputStream os) throws IOException {
                configurationController.dbExportInternal(os);
            }
        };
        return Response.ok(sout)
                .type(MediaType.TEXT_XML)
                .header(SyncopeConstants.CONTENT_DISPOSITION_HEADER,
                "attachment; filename=" + ImportExport.CONTENT_FILE)
                .build();
    }

    @Override
    public void delete(final String key) {
        configurationController.delete(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<MailTemplateTO> getMailTemplates() {
        return CollectionWrapper.wrapMailTemplates(
                (Set<String>) configurationController.getMailTemplates().getModel().values().iterator().next());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<ValidatorTO> getValidators() {
        return CollectionWrapper.wrapValidators(
                (Set<String>) configurationController.getValidators().getModel().values().iterator().next());
    }

    @Override
    public List<ConfigurationTO> list() {
        return configurationController.list(null);
    }

    @Override
    public ConfigurationTO read(final String key) {
        return configurationController.read(null, key);

    }

    @Override
    public void update(final String key, final ConfigurationTO configurationTO) {
        configurationController.update(configurationTO);
    }

    @Override
    public void setUriInfo(final UriInfo ui) {
        this.uriInfo = ui;
    }
}
