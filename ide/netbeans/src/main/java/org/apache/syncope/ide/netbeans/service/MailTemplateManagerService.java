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
package org.apache.syncope.ide.netbeans.service;

import java.io.InputStream;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.rest.api.service.MailTemplateService;

public class MailTemplateManagerService {

    private final MailTemplateService service;

    public MailTemplateManagerService(final String url, final String userName, final String password) {
        SyncopeClient syncopeClient = new SyncopeClientFactoryBean().setAddress(url).create(userName, password);
        service = syncopeClient.getService(MailTemplateService.class);
    }

    public List<MailTemplateTO> list() {
        return service.list();
    }

    public boolean create(final MailTemplateTO mailTemplateTO) {
        return Response.Status.CREATED.getStatusCode() == service.create(mailTemplateTO).getStatus();
    }

    public MailTemplateTO read(final String key) {
        return service.read(key);
    }

    public boolean delete(final String key) {
        service.delete(key);
        return true;
    }

    public Object getFormat(final String key, final MailTemplateFormat format) {
        return service.getFormat(key, format).getEntity();
    }

    public void setFormat(final String key, final MailTemplateFormat format, final InputStream templateIn) {
        service.setFormat(key, format, templateIn);
    }

    public boolean removeFormat(final String key, final MailTemplateFormat format) {
        return false;
    }

}
