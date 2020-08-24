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
package org.apache.syncope.core.logic.saml2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.pac4j.core.http.callback.NoParameterCallbackUrlResolver;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;

/**
 * Basic in-memory cache for available {@link SAML2Client} instances.
 */
@Component
public class SAML2ClientCache {

    private final List<SAML2Client> cache = Collections.synchronizedList(new ArrayList<>());

    public Optional<SAML2Client> get(final String idpEntityID, final String spEntityID) {
        return cache.stream().filter(c -> idpEntityID.equals(c.getIdentityProviderResolvedEntityId())
                && spEntityID.equals(c.getConfiguration().getServiceProviderEntityId())).findFirst();
    }

    private static SAML2Client newSAML2Client(final Resource metadata, final SAML2Configuration cfg) {
        cfg.setIdentityProviderMetadataResource(metadata);

        SAML2Client saml2Client = new SAML2Client(cfg);
        saml2Client.setCallbackUrlResolver(new NoParameterCallbackUrlResolver());

        return saml2Client;
    }

    public static SAML2SP4UIIdPTO importMetadata(
            final InputStream metadata, final SAML2Configuration cfg) throws IOException {

        SAML2Client saml2Client = newSAML2Client(new ByteArrayResource(IOUtils.readBytesFromStream(metadata)), cfg);
        String entityId = saml2Client.getConfiguration().getIdentityProviderMetadataResolver().getEntityId();

        SAML2SP4UIIdPTO idpTO = new SAML2SP4UIIdPTO();
        idpTO.setEntityID(entityId);
        idpTO.setName(entityId);

        EntityDescriptor entityDescriptor = (EntityDescriptor) saml2Client.getConfiguration().
                getIdentityProviderMetadataResolver().getEntityDescriptorElement();
        entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getSingleSignOnServices().
                forEach(sso -> {
                    if (idpTO.getBindingType() == null) {
                        if (SAML2BindingType.POST.getUri().equals(sso.getBinding())) {
                            idpTO.setBindingType(SAML2BindingType.POST);
                        } else if (SAML2BindingType.REDIRECT.getUri().equals(sso.getBinding())) {
                            idpTO.setBindingType(SAML2BindingType.REDIRECT);
                        }
                    }
                });
        if (idpTO.getBindingType() == null) {
            throw new IllegalArgumentException("Neither POST nor REDIRECT artifacts supported by " + entityId);
        }

        cfg.setAuthnRequestBindingType(idpTO.getBindingType().getUri());
        cfg.setResponseBindingType(SAML2BindingType.POST.getUri());
        cfg.setSpLogoutRequestBindingType(idpTO.getBindingType().getUri());
        cfg.setSpLogoutResponseBindingType(idpTO.getBindingType().getUri());

        entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getSingleLogoutServices().stream().
                filter(slo -> SAML2BindingType.POST.getUri().equals(slo.getBinding())
                || SAML2BindingType.REDIRECT.getUri().equals(slo.getBinding())).
                findFirst().
                ifPresent(slo -> idpTO.setLogoutSupported(true));

        idpTO.setMetadata(Base64.getEncoder().encodeToString(
                saml2Client.getConfiguration().getIdentityProviderMetadataResolver().getMetadata().getBytes()));

        ItemTO connObjectKeyItem = new ItemTO();
        connObjectKeyItem.setIntAttrName("username");
        connObjectKeyItem.setExtAttrName(NameID.DEFAULT_ELEMENT_LOCAL_NAME);
        idpTO.setConnObjectKeyItem(connObjectKeyItem);

        return idpTO;
    }

    public SAML2Client add(
            final SAML2SP4UIIdP idp, final SAML2Configuration cfg, final String spEntityID, final String callbackUrl) {

        SAML2Client saml2Client = newSAML2Client(new ByteArrayResource(idp.getMetadata()), cfg);
        saml2Client.getConfiguration().setServiceProviderEntityId(spEntityID);
        saml2Client.setCallbackUrl(callbackUrl);
        saml2Client.init();

        cache.add(saml2Client);
        return saml2Client;
    }

    public boolean removeAll(final String idpEntityID) {
        return cache.removeIf(c -> idpEntityID.equals(c.getIdentityProviderResolvedEntityId()));
    }
}
