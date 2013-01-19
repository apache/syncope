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
import java.util.List;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.syncope.client.to.ConfigurationTO;
import org.apache.syncope.client.to.MailTemplateTO;
import org.apache.syncope.client.to.ValidatorTO;
import org.apache.syncope.client.util.CollectionWrapper;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.core.rest.controller.ConfigurationController;
import org.apache.syncope.services.ConfigurationService;
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
        return Response.created(location).entity(created).build();
    }

    @Override
    public Response dbExport() {
        configurationController.dbExport(new DummyHTTPServletResponse());
        // TODO catch output-stream and forward it to response
        return null;
    }

    @Override
    public void delete(final String key) {
        try {
            configurationController.delete(key);
        } catch (MissingConfKeyException e) {
            throw new NotFoundException(e);
        }
    }

    @Override
    public Set<MailTemplateTO> getMailTemplates() {
        return CollectionWrapper.wrapMailTemplates(configurationController.getMailTemplates());
    }

    @Override
    public Set<ValidatorTO> getValidators() {
        return CollectionWrapper.wrapValidator(configurationController.getValidators());
    }

    @Override
    public List<ConfigurationTO> list() {
        return configurationController.list(null);
    }

    @Override
    public ConfigurationTO read(String key) {
        try {
            return configurationController.read(null, key);
        } catch (MissingConfKeyException e) {
            throw new NotFoundException(e);
        }
    }

    @Override
    public ConfigurationTO update(String key, ConfigurationTO configurationTO) {
        try {
            return configurationController.update(null, configurationTO);
        } catch (MissingConfKeyException e) {
            throw new NotFoundException(e);
        }
    }

    @Override
    public void setUriInfo(UriInfo ui) {
        this.uriInfo = ui;
    }
}
