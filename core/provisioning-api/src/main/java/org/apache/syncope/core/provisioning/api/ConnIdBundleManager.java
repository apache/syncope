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
package org.apache.syncope.core.provisioning.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;

/**
 * Manage information about ConnId connector bundles.
 */
public interface ConnIdBundleManager {

    ConfigurationProperties getConfigurationProperties(ConnectorInfo info);

    Map<URI, ConnectorInfoManager> getConnManagers();

    Pair<URI, ConnectorInfo> getConnectorInfo(ConnInstance connInstance);

    Map<URI, ConnectorInfoManager> getConnInfoManagers();

    void resetConnManagers();

    List<URI> getLocations();
}
