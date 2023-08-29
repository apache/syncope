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

import java.util.Optional;
import org.apache.syncope.common.lib.clientapps.AnonymousUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.clientapps.DefaultUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.clientapps.GroovyUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.clientapps.PairwiseOidcUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.clientapps.PrincipalAttributeUsernameAttributeProviderConf;
import org.apache.syncope.common.lib.clientapps.UsernameAttributeProviderConf;
import org.apache.syncope.common.lib.types.PersistentIdGenerator;
import org.apereo.cas.authentication.principal.OidcPairwisePersistentIdGenerator;
import org.apereo.cas.authentication.principal.ShibbolethCompatiblePersistentIdGenerator;
import org.apereo.cas.services.AnonymousRegisteredServiceUsernameAttributeProvider;
import org.apereo.cas.services.BaseWebBasedRegisteredService;
import org.apereo.cas.services.DefaultRegisteredServiceUsernameProvider;
import org.apereo.cas.services.GroovyRegisteredServiceUsernameProvider;
import org.apereo.cas.services.PairwiseOidcRegisteredServiceUsernameAttributeProvider;
import org.apereo.cas.services.PrincipalAttributeRegisteredServiceUsernameProvider;
import org.apereo.cas.util.RandomUtils;

public class DefaultUsernameAttributeProviderConfMapper implements UsernameAttributeProviderConf.Mapper {

    protected static Optional<org.apereo.cas.authentication.principal.PersistentIdGenerator> toPersistentIdGenerator(
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

    protected final BaseWebBasedRegisteredService service;

    public DefaultUsernameAttributeProviderConfMapper(final BaseWebBasedRegisteredService service) {
        this.service = service;
    }

    @Override
    public void map(final AnonymousUsernameAttributeProviderConf conf) {
        AnonymousRegisteredServiceUsernameAttributeProvider provider =
                new AnonymousRegisteredServiceUsernameAttributeProvider();
        toPersistentIdGenerator(conf.getPersistentIdGenerator()).ifPresent(provider::setPersistentIdGenerator);
        provider.setCanonicalizationMode(conf.getCaseCanonicalizationMode().name());
        service.setUsernameAttributeProvider(provider);
    }

    @Override
    public void map(final DefaultUsernameAttributeProviderConf conf) {
        DefaultRegisteredServiceUsernameProvider provider = new DefaultRegisteredServiceUsernameProvider();
        provider.setCanonicalizationMode(conf.getCaseCanonicalizationMode().name());
        service.setUsernameAttributeProvider(provider);
    }

    @Override
    public void map(final GroovyUsernameAttributeProviderConf conf) {
        GroovyRegisteredServiceUsernameProvider provider =
                new GroovyRegisteredServiceUsernameProvider(conf.getGroovyScript());
        provider.setCanonicalizationMode(conf.getCaseCanonicalizationMode().name());
        service.setUsernameAttributeProvider(provider);
    }

    @Override
    public void map(final PairwiseOidcUsernameAttributeProviderConf conf) {
        PairwiseOidcRegisteredServiceUsernameAttributeProvider provider =
                new PairwiseOidcRegisteredServiceUsernameAttributeProvider();
        toPersistentIdGenerator(conf.getPersistentIdGenerator()).ifPresent(provider::setPersistentIdGenerator);
        provider.setCanonicalizationMode(conf.getCaseCanonicalizationMode().name());
        service.setUsernameAttributeProvider(provider);
    }

    @Override
    public void map(final PrincipalAttributeUsernameAttributeProviderConf conf) {
        PrincipalAttributeRegisteredServiceUsernameProvider provider =
                new PrincipalAttributeRegisteredServiceUsernameProvider(conf.getUsernameAttribute());
        provider.setCanonicalizationMode(conf.getCaseCanonicalizationMode().name());
        service.setUsernameAttributeProvider(provider);
    }
}
