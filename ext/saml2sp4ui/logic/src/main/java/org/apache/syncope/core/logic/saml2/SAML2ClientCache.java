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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.pac4j.core.http.callback.NoParameterCallbackUrlResolver;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.saml.metadata.SAML2IdentityProviderMetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;

/**
 * Basic in-memory cache for available {@link SAML2Client} instances.
 */
public class SAML2ClientCache {

    protected static final Logger LOG = LoggerFactory.getLogger(SAML2ClientCache.class);

    protected static Path METADATA_PATH;

    static {
        try {
            METADATA_PATH = Files.createTempDirectory("saml2sp4ui-").toAbsolutePath();
        } catch (IOException e) {
            LOG.error("Could not create a temp directory to store metadata files", e);
        }
    }

    public static Optional<String> getSPMetadataPath(final String spEntityID) {
        String entityIDPath = StringUtils.replaceChars(
                StringUtils.removeStart(StringUtils.removeStart(spEntityID, "https://"), "http://"), ":/", "__");
        return Optional.ofNullable(METADATA_PATH).map(path -> path.resolve(entityIDPath).toAbsolutePath().toString());
    }

    public static SAML2SP4UIIdPTO importMetadata(
            final InputStream metadata, final SAML2Configuration cfg) throws IOException {

        cfg.setIdentityProviderMetadataResource(new ByteArrayResource(IOUtils.readBytesFromStream(metadata)));
        SAML2IdentityProviderMetadataResolver metadataResolver = new SAML2IdentityProviderMetadataResolver(cfg);
        metadataResolver.init();
        cfg.setIdentityProviderMetadataResolver(metadataResolver);

        String entityId = metadataResolver.getEntityId();

        SAML2SP4UIIdPTO idpTO = new SAML2SP4UIIdPTO();
        idpTO.setEntityID(entityId);
        idpTO.setName(entityId);

        EntityDescriptor entityDescriptor = (EntityDescriptor) metadataResolver.getEntityDescriptorElement();

        if (idpTO.getBindingType() == null) {
            entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getSingleSignOnServices().forEach(sso -> {
                if (SAML2BindingType.POST.getUri().equals(sso.getBinding())) {
                    idpTO.setBindingType(SAML2BindingType.POST);
                } else if (SAML2BindingType.REDIRECT.getUri().equals(sso.getBinding())) {
                    idpTO.setBindingType(SAML2BindingType.REDIRECT);
                }
            });
        }
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

        idpTO.setMetadata(Base64.getEncoder().encodeToString(metadataResolver.getMetadata().getBytes()));

        Item connObjectKeyItem = new Item();
        connObjectKeyItem.setIntAttrName("username");
        connObjectKeyItem.setExtAttrName(NameID.DEFAULT_ELEMENT_LOCAL_NAME);
        idpTO.setConnObjectKeyItem(connObjectKeyItem);

        return idpTO;
    }

    protected final List<SAML2Client> cache = Collections.synchronizedList(new ArrayList<>());

    public Optional<SAML2Client> get(final String idpEntityID, final String spEntityID) {
        return cache.stream().filter(c -> idpEntityID.equals(c.getIdentityProviderResolvedEntityId())
                && spEntityID.equals(c.getConfiguration().getServiceProviderEntityId())).findFirst();
    }

    public SAML2Client add(
            final SAML2SP4UIIdP idp, final SAML2Configuration cfg, final String spEntityID, final String callbackUrl) {

        cfg.setIdentityProviderEntityId(idp.getEntityID());

        cfg.setIdentityProviderMetadataResource(new ByteArrayResource(idp.getMetadata()));
        SAML2IdentityProviderMetadataResolver metadataResolver = new SAML2IdentityProviderMetadataResolver(cfg);
        metadataResolver.init();
        cfg.setIdentityProviderMetadataResolver(metadataResolver);

        cfg.setServiceProviderEntityId(spEntityID);
        getSPMetadataPath(spEntityID).ifPresent(cfg::setServiceProviderMetadataResourceFilepath);

        SAML2Client saml2Client = new SAML2Client(cfg);
        saml2Client.setCallbackUrlResolver(new NoParameterCallbackUrlResolver());
        saml2Client.setCallbackUrl(callbackUrl);
        saml2Client.init();

        cache.add(saml2Client);
        return saml2Client;
    }

    public boolean removeAll(final String idpEntityID) {
        return cache.removeIf(c -> idpEntityID.equals(c.getIdentityProviderResolvedEntityId()));
    }
}
