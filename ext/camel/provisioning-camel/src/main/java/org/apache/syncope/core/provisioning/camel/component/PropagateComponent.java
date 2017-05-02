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
package org.apache.syncope.core.provisioning.camel.component;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;

public class PropagateComponent extends UriEndpointComponent {

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Autowired
    protected GroupDataBinder groupDataBinder;

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    public PropagateComponent() {
        super(PropagateEndpoint.class);
    }

    @Override
    protected Endpoint createEndpoint(final String uri, final String remaining,
            final Map<String, Object> parameters) throws Exception {
        PropagateType type = PropagateType.valueOf(remaining);
        PropagateEndpoint endpoint = new PropagateEndpoint(uri, this);
        endpoint.setPropagateType(type);
        endpoint.setPropagationManager(propagationManager);
        endpoint.setPropagationTaskExecutor(taskExecutor);
        endpoint.setUserDAO(userDAO);
        endpoint.setGroupDAO(groupDAO);
        endpoint.setAnyObjectDAO(anyObjectDAO);
        endpoint.setGroupDataBinder(groupDataBinder);
        endpoint.setUwfAdapter(uwfAdapter);

        setProperties(endpoint, parameters);
        return endpoint;
    }

}
