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

import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;

import org.apache.syncope.common.lib.to.client.CASSPTO;
import org.apache.syncope.common.lib.to.client.ClientAppTO;
import org.springframework.stereotype.Component;

@ClientAppMapFor(clientAppClass = CASSPTO.class)
@Component
public class CASSPTOMapper implements ClientAppMapper {

    @Override
    public RegisteredService build(
        final ClientAppTO clientAppTO,
        final RegisteredServiceAuthenticationPolicy authPolicy,
        final RegisteredServiceAccessStrategy accessStrategy,
        final RegisteredServiceAttributeReleasePolicy attributeReleasePolicy) {

        CASSPTO rp = CASSPTO.class.cast(clientAppTO);

        RegexRegisteredService service = new RegexRegisteredService();

        service.setServiceId(rp.getServiceId());
        service.setId(rp.getClientAppId());
        service.setName(rp.getName());
        service.setDescription(rp.getDescription());
        service.setAccessStrategy(accessStrategy);
        service.setAuthenticationPolicy(authPolicy);
        service.setAttributeReleasePolicy(attributeReleasePolicy);

        return service;
    }

}
