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
package org.apache.syncope.client.console.rest;

import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.AttrLayoutType;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConfTO;
import org.apache.syncope.common.rest.api.service.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationRestClient extends BaseRestClient {

    private static final long serialVersionUID = 7692363064029538722L;

    @Autowired
    private SchemaRestClient schemaRestClient;

    public ConfTO list() {
        ConfTO conf = getService(ConfigurationService.class).list();

        for (Iterator<AttrTO> it = conf.getPlainAttrs().iterator(); it.hasNext();) {
            AttrTO attr = it.next();
            for (AttrLayoutType type : AttrLayoutType.values()) {
                if (type.getConfKey().equals(attr.getSchema())) {
                    it.remove();
                }
            }
        }

        return conf;
    }

    public AttrTO get(final String key) {
        try {
            return getService(ConfigurationService.class).get(key);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a configuration schema", e);
        }
        return null;
    }

    public AttrTO readAttrLayout(final AttrLayoutType type) {
        if (type == null) {
            return null;
        }

        AttrTO attrLayout = get(type.getConfKey());
        if (attrLayout == null) {
            attrLayout = new AttrTO();
            attrLayout.setSchema(type.getConfKey());
        }
        if (attrLayout.getValues().isEmpty()) {
            attrLayout.getValues().addAll(schemaRestClient.getPlainSchemaNames());
        }

        return attrLayout;
    }

    public void set(final AttrTO attrTO) {
        getService(ConfigurationService.class).set(attrTO);
    }

    public void delete(final String key) {
        getService(ConfigurationService.class).delete(key);
    }

    public List<String> getMailTemplates() {
        return SyncopeConsoleSession.get().getSyncopeTO().getMailTemplates();
    }

    public Response dbExport() {
        return getService(ConfigurationService.class).export();
    }
}
