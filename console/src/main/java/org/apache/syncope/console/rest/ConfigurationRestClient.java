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
package org.apache.syncope.console.rest;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConfTO;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.wrap.MailTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationRestClient extends BaseRestClient {

    private static final long serialVersionUID = 7692363064029538722L;

    public ConfTO list() {
        return getService(ConfigurationService.class).list();
    }

    public AttributeTO read(final String key) {
        try {
            return getService(ConfigurationService.class).read(key);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a configuration schema", e);
        }
        return null;
    }

    public void set(final AttributeTO attributeTO) {
        getService(ConfigurationService.class).set(attributeTO.getSchema(), attributeTO);
    }

    public void delete(final String key) {
        getService(ConfigurationService.class).delete(key);
    }

    public List<String> getMailTemplates() {
        return CollectionWrapper.unwrap(
                new ArrayList<MailTemplate>(getService(ConfigurationService.class).getMailTemplates()));
    }

    public Response dbExport() {
        return getService(ConfigurationService.class).export();
    }
}
