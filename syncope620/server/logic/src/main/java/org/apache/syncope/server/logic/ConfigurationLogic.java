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
package org.apache.syncope.server.logic;

import java.io.OutputStream;
import java.lang.reflect.Method;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConfTO;
import org.apache.syncope.server.persistence.api.content.ContentExporter;
import org.apache.syncope.server.persistence.api.dao.ConfDAO;
import org.apache.syncope.server.persistence.api.dao.NotFoundException;
import org.apache.syncope.server.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.server.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.server.persistence.api.entity.conf.CPlainSchema;
import org.apache.syncope.server.provisioning.api.data.ConfigurationDataBinder;
import org.apache.syncope.server.logic.init.WorkflowAdapterLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConfigurationLogic extends AbstractTransactionalLogic<ConfTO> {

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private ConfigurationDataBinder binder;

    @Autowired
    private ContentExporter exporter;

    @Autowired
    private WorkflowAdapterLoader wfAdapterLoader;

    @PreAuthorize("hasRole('CONFIGURATION_DELETE')")
    public void delete(final String key) {
        confDAO.delete(key);
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    public ConfTO list() {
        return binder.getConfTO(confDAO.get());
    }

    @PreAuthorize("isAuthenticated()")
    public AttrTO read(final String key) {
        AttrTO result;

        CPlainAttr conf = confDAO.find(key);
        if (conf == null) {
            CPlainSchema schema = plainSchemaDAO.find(key, CPlainSchema.class);
            if (schema == null) {
                throw new NotFoundException("Configuration key " + key);
            }

            result = new AttrTO();
            result.setSchema(key);
        } else {
            result = binder.getAttrTO(conf);
        }

        return result;
    }

    @PreAuthorize("hasRole('CONFIGURATION_SET')")
    public void set(final AttrTO value) {
        confDAO.save(binder.getAttribute(value));
    }

    @PreAuthorize("hasRole('CONFIGURATION_EXPORT')")
    @Transactional(readOnly = true)
    public void export(final OutputStream os) {
        try {
            exporter.export(os, wfAdapterLoader.getPrefix());
            LOG.debug("Database content successfully exported");
        } catch (Exception e) {
            LOG.error("While exporting database content", e);
        }
    }

    @Override
    protected ConfTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
