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
package org.apache.syncope.core.logic;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.OIDCProviderTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.OIDCClientEntitlement;
import org.apache.syncope.core.logic.init.OIDCClientClassPathScanImplementationLookup;
import org.apache.syncope.core.logic.model.OIDCProviderDiscoveryDocument;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCProviderDAO;
import org.apache.syncope.core.persistence.api.entity.OIDCProvider;
import org.apache.syncope.core.provisioning.api.data.OIDCProviderDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OIDCProviderLogic extends AbstractTransactionalLogic<OIDCProviderTO> {

    @Autowired
    private OIDCProviderDAO opDAO;

    @Autowired
    private OIDCProviderDataBinder binder;

    @Autowired
    private OIDCClientClassPathScanImplementationLookup implLookup;

    @PreAuthorize("isAuthenticated()")
    public Set<String> getActionsClasses() {
        return implLookup.getActionsClasses();
    }

    private OIDCProviderDiscoveryDocument getDiscoveryDocument(final String issuer) {
        WebClient client = WebClient.create(
                issuer + "/.well-known/openid-configuration", Arrays.asList(new JacksonJsonProvider())).
                accept(MediaType.APPLICATION_JSON);
        return client.get(OIDCProviderDiscoveryDocument.class);
    }

    @PreAuthorize("hasRole('" + OIDCClientEntitlement.OP_CREATE + "')")
    public String createFromDiscovery(final OIDCProviderTO opTO) {
        OIDCProviderDiscoveryDocument discoveryDocument = getDiscoveryDocument(opTO.getIssuer());

        opTO.setAuthorizationEndpoint(discoveryDocument.getAuthorizationEndpoint());
        opTO.setIssuer(discoveryDocument.getIssuer());
        opTO.setJwksUri(discoveryDocument.getJwksUri());
        opTO.setTokenEndpoint(discoveryDocument.getTokenEndpoint());
        opTO.setUserinfoEndpoint(discoveryDocument.getUserinfoEndpoint());
        opTO.setEndSessionEndpoint(discoveryDocument.getEndSessionEndpoint());

        return create(opTO);
    }

    @PreAuthorize("hasRole('" + OIDCClientEntitlement.OP_CREATE + "')")
    public String create(final OIDCProviderTO opTO) {
        if (opTO.getConnObjectKeyItem() == null) {
            ItemTO connObjectKeyItem = new ItemTO();
            connObjectKeyItem.setIntAttrName("username");
            connObjectKeyItem.setExtAttrName("email");
            opTO.setConnObjectKeyItem(connObjectKeyItem);
        }

        OIDCProvider provider = opDAO.save(binder.create(opTO));

        return provider.getKey();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<OIDCProviderTO> list() {
        return CollectionUtils.collect(opDAO.findAll(), new Transformer<OIDCProvider, OIDCProviderTO>() {

            @Override
            public OIDCProviderTO transform(final OIDCProvider input) {
                return binder.getOIDCProviderTO(input);
            }
        }, new ArrayList<OIDCProviderTO>());
    }

    @PreAuthorize("hasRole('" + OIDCClientEntitlement.OP_READ + "')")
    @Transactional(readOnly = true)
    public OIDCProviderTO read(final String key) {
        OIDCProvider op = opDAO.find(key);
        if (op == null) {
            throw new NotFoundException("OIDC Provider '" + key + "'");
        }
        return binder.getOIDCProviderTO(op);
    }

    @PreAuthorize("hasRole('" + OIDCClientEntitlement.OP_UPDATE + "')")
    public void update(final OIDCProviderTO oidcProviderTO) {
        OIDCProvider oidcProvider = opDAO.find(oidcProviderTO.getKey());
        if (oidcProvider == null) {
            throw new NotFoundException("OIDC Provider '" + oidcProviderTO.getKey() + "'");
        }

        if (!oidcProvider.getIssuer().equals(oidcProviderTO.getIssuer())) {
            LOG.error("Issuers do not match: expected {}, found {}",
                    oidcProvider.getIssuer(), oidcProviderTO.getIssuer());
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidEntity);
            sce.getElements().add("Issuers do not match");
            throw sce;
        }

        opDAO.save(binder.update(oidcProvider, oidcProviderTO));
    }

    @PreAuthorize("hasRole('" + OIDCClientEntitlement.OP_DELETE + "')")
    public void delete(final String key) {
        OIDCProvider op = opDAO.find(key);
        if (op == null) {
            throw new NotFoundException("OIDC Provider '" + key + "'");
        }
        opDAO.delete(key);
    }

    @Override
    protected OIDCProviderTO resolveReference(
            final Method method, final Object... args) throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof OIDCProviderTO) {
                    key = ((OIDCProviderTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getOIDCProviderTO(opDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
