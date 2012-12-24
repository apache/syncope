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
package org.apache.syncope.core.propagation;

import java.util.Set;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.types.ConnConfProperty;
import org.springframework.beans.BeansException;

public interface ConnectorFactory {

    ConnectorFacadeProxy createConnectorBean(ConnInstance connInstance, Set<ConnConfProperty> configuration)
            throws NotFoundException;

    /**
     * Get a live connector bean that is registered with the given resource.
     *
     * @param resource the resource.
     * @return live connector bran for given resource
     * @throws BeansException if there is any problem with Spring
     * @throws NotFoundException if the connector is not registered in the context
     */
    ConnectorFacadeProxy getConnector(ExternalResource resource)
            throws BeansException, NotFoundException;

    void load();
}