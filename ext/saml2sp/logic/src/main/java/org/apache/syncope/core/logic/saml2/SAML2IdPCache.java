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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.core.logic.init.SAML2SPLoader;
import org.apache.syncope.core.persistence.api.entity.SAML2IdP;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPDataBinder;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;

/**
 * Basic in-memory cache for available {@link SAML2IdPEntity} identity providers.
 */
@Component
public class SAML2IdPCache {

    private final Map<String, SAML2IdPEntity> cache =
            Collections.synchronizedMap(new HashMap<String, SAML2IdPEntity>());

    @Autowired
    private SAML2SPLoader loader;

    @Autowired
    private SAML2IdPDataBinder binder;

    public SAML2IdPEntity get(final String entityID) {
        return cache.get(entityID);
    }

    public SAML2IdPEntity getFirst() {
        return cache.isEmpty() ? null : cache.entrySet().iterator().next().getValue();
    }

    public SAML2IdPEntity put(
            final EntityDescriptor entityDescriptor,
            final MappingItemTO connObjectKeyItem,
            final boolean useDeflateEncoding)
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {

        return cache.put(entityDescriptor.getEntityID(),
                new SAML2IdPEntity(entityDescriptor, connObjectKeyItem, useDeflateEncoding, loader.getKeyPass()));
    }

    @Transactional(readOnly = true)
    public SAML2IdPEntity put(final SAML2IdP idp)
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, WSSecurityException,
            XMLParserException {

        Element element = OpenSAMLUtil.getParserPool().parse(
                new InputStreamReader(new ByteArrayInputStream(idp.getMetadata()))).getDocumentElement();
        EntityDescriptor entityDescriptor = (EntityDescriptor) OpenSAMLUtil.fromDom(element);
        return put(entityDescriptor, binder.getIdPTO(idp).getConnObjectKeyItem(), idp.isUseDeflateEncoding());
    }

    public SAML2IdPEntity remove(final String entityID) {
        return cache.remove(entityID);
    }
}
