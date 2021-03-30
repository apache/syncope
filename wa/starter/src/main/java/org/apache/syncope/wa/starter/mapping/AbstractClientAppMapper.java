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
package org.apache.syncope.wa.starter.mapping;

import java.util.Map;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apereo.cas.services.DefaultRegisteredServiceProperty;
import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.apereo.cas.services.RegisteredServiceProperty;

abstract class AbstractClientAppMapper implements ClientAppMapper {

    protected void setCommon(final RegexRegisteredService service, final ClientAppTO clientApp) {
        service.setId(clientApp.getClientAppId());
        service.setName(clientApp.getName());
        service.setDescription(clientApp.getDescription());

        if (!clientApp.getProperties().isEmpty()) {
            Map<String, RegisteredServiceProperty> properties = clientApp.getProperties().stream().
                    collect(Collectors.toMap(
                            Attr::getSchema,
                            attr -> new DefaultRegisteredServiceProperty(attr.getValues()),
                            (existing, replacement) -> existing));
            service.setProperties(properties);
        }
    }

    protected void setPolicies(
            final RegexRegisteredService service,
            final RegisteredServiceAuthenticationPolicy authenticationPolicy,
            final RegisteredServiceAccessStrategy accessStrategy,
            final RegisteredServiceAttributeReleasePolicy attributeReleasePolicy) {

        if (authenticationPolicy != null) {
            service.setAuthenticationPolicy(authenticationPolicy);
        }
        if (accessStrategy != null) {
            service.setAccessStrategy(accessStrategy);
        }
        if (attributeReleasePolicy != null) {
            service.setAttributeReleasePolicy(attributeReleasePolicy);
        }
    }
}
