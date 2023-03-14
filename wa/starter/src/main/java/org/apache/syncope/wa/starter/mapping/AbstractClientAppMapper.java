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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.clientapps.AnonymousUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.clientapps.DefaultUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.clientapps.GroovyUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.clientapps.PairwiseOidcUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.clientapps.PrincipalAttributeUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.clientapps.UsernameAttributeProviderConf;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.types.PersistentIdGenerator;
import org.apereo.cas.authentication.principal.OidcPairwisePersistentIdGenerator;
import org.apereo.cas.authentication.principal.ShibbolethCompatiblePersistentIdGenerator;
import org.apereo.cas.services.AnonymousRegisteredServiceUsernameAttributeProvider;
import org.apereo.cas.services.BaseWebBasedRegisteredService;
import org.apereo.cas.services.DefaultRegisteredServiceProperty;
import org.apereo.cas.services.DefaultRegisteredServiceUsernameProvider;
import org.apereo.cas.services.GroovyRegisteredServiceUsernameProvider;
import org.apereo.cas.services.PairwiseOidcRegisteredServiceUsernameAttributeProvider;
import org.apereo.cas.services.PrincipalAttributeRegisteredServiceUsernameProvider;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.apereo.cas.services.RegisteredServiceMultifactorPolicy;
import org.apereo.cas.services.RegisteredServiceProperty;
import org.apereo.cas.util.RandomUtils;

abstract class AbstractClientAppMapper implements ClientAppMapper {

    protected Optional<org.apereo.cas.authentication.principal.PersistentIdGenerator> toPersistentIdGenerator(
            final PersistentIdGenerator persistentIdGenerator) {

        if (persistentIdGenerator == null) {
            return Optional.empty();
        }

        org.apereo.cas.authentication.principal.PersistentIdGenerator result = null;
        switch (persistentIdGenerator) {
            case SHIBBOLETH:
                result = new ShibbolethCompatiblePersistentIdGenerator(RandomUtils.randomAlphanumeric(16));
                break;

            case OIDC:
                result = new OidcPairwisePersistentIdGenerator();
                break;

            default:
        }

        return Optional.ofNullable(result);
    }

    protected void setUsernameAttributeProvider(
            final BaseWebBasedRegisteredService service,
            final UsernameAttributeProviderConf conf) {

        if (conf instanceof DefaultUsernameAttributeProviderConf) {
            service.setUsernameAttributeProvider(
                    new DefaultRegisteredServiceUsernameProvider());
        } else if (conf instanceof PrincipalAttributeUsernameAttributeProviderConf) {
            service.setUsernameAttributeProvider(
                    new PrincipalAttributeRegisteredServiceUsernameProvider(
                            ((PrincipalAttributeUsernameAttributeProviderConf) conf).getUsernameAttribute()));
        } else if (conf instanceof GroovyUsernameAttributeProviderConf) {
            service.setUsernameAttributeProvider(
                    new GroovyRegisteredServiceUsernameProvider(
                            ((GroovyUsernameAttributeProviderConf) conf).getGroovyScript()));
        } else if (conf instanceof AnonymousUsernameAttributeProviderConf) {
            AnonymousRegisteredServiceUsernameAttributeProvider arsuap =
                    new AnonymousRegisteredServiceUsernameAttributeProvider();
            toPersistentIdGenerator(((AnonymousUsernameAttributeProviderConf) conf).getPersistentIdGenerator()).
                    ifPresent(arsuap::setPersistentIdGenerator);
            service.setUsernameAttributeProvider(arsuap);
        } else if (conf instanceof PairwiseOidcUsernameAttributeProviderConf) {
            PairwiseOidcRegisteredServiceUsernameAttributeProvider porsuap =
                    new PairwiseOidcRegisteredServiceUsernameAttributeProvider();
            toPersistentIdGenerator(((PairwiseOidcUsernameAttributeProviderConf) conf).getPersistentIdGenerator()).
                    ifPresent(porsuap::setPersistentIdGenerator);
            service.setUsernameAttributeProvider(porsuap);
        }
    }

    protected void setCommon(final BaseWebBasedRegisteredService service, final ClientAppTO clientApp) {
        service.setId(clientApp.getClientAppId());
        service.setName(clientApp.getName());
        service.setDescription(clientApp.getDescription());
        service.setLogo(clientApp.getLogo());
        setUsernameAttributeProvider(service, clientApp.getUsernameAttributeProviderConf());

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
            final BaseWebBasedRegisteredService service,
            final RegisteredServiceAuthenticationPolicy authPolicy,
            final RegisteredServiceMultifactorPolicy mfaPolicy,
            final RegisteredServiceAccessStrategy accessStrategy,
            final RegisteredServiceAttributeReleasePolicy attributeReleasePolicy) {

        if (authPolicy != null) {
            service.setAuthenticationPolicy(authPolicy);
        }
        if (mfaPolicy != null) {
            service.setMultifactorAuthenticationPolicy(mfaPolicy);
        }
        if (accessStrategy != null) {
            service.setAccessStrategy(accessStrategy);
        }
        if (attributeReleasePolicy != null) {
            service.setAttributeReleasePolicy(attributeReleasePolicy);
        }
    }
}
