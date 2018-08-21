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

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.common.rest.api.service.ImplementationService;

public class ImplementationManagerService {

    private final ImplementationService service ;

    public ImplementationManagerService(final String url, final String userName, final String password) {
        SyncopeClient syncopeClient = new SyncopeClientFactoryBean().setAddress(url).create(userName, password);
        service = syncopeClient.getService(ImplementationService.class);
    }
 
    public List<ImplementationTO> list(final ImplementationType type) {
        return service.list(type);
    } 

    public ImplementationTO read(final ImplementationType type , final String key) {
        return service.read(type, key);
    }

    public boolean create(final ImplementationTO implementationTO) {
        return Response.Status.CREATED.getStatusCode() == service.create(implementationTO).getStatus();
    }

    public boolean delete(final ImplementationType type , final String key) {
        return Response.Status.NO_CONTENT.getStatusCode() == service.delete(type, key).getStatus();
    }

    public boolean update(final ImplementationTO implementationTO) {
       return Response.Status.NO_CONTENT.getStatusCode() == service.update(implementationTO).getStatus();
    }
}
