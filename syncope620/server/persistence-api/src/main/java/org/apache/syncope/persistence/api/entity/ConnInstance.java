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
package org.apache.syncope.persistence.api.entity;

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;

public interface ConnInstance extends Entity<Long> {

    boolean addCapability(ConnectorCapability capabitily);

    boolean addResource(ExternalResource resource);

    String getBundleName();

    Set<ConnectorCapability> getCapabilities();

    Set<ConnConfProperty> getConfiguration();

    Integer getConnRequestTimeout();

    String getConnectorName();

    String getDisplayName();

    String getLocation();

    ConnPoolConf getPoolConf();

    List<ExternalResource> getResources();

    String getVersion();

    boolean removeCapability(ConnectorCapability capabitily);

    boolean removeResource(ExternalResource resource);

    void setBundleName(String bundleName);

    void setCapabilities(Set<ConnectorCapability> capabilities);

    void setConfiguration(Set<ConnConfProperty> configuration);

    void setConnRequestTimeout(Integer connRequestTimeout);

    void setConnectorName(String connectorName);

    void setDisplayName(String displayName);

    void setLocation(String location);

    void setPoolConf(ConnPoolConf poolConf);

    void setResources(List<ExternalResource> resources);

    void setVersion(String version);
}
