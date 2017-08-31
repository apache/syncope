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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.xmlsec.signature.X509Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SAML2IdPEntity {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2IdPEntity.class);

    private final String id;

    private SAML2IdPTO idpTO;

    private final Map<String, Endpoint> ssoBindings = new HashMap<>();

    private final Map<String, SingleLogoutService> sloBindings = new HashMap<>();

    private final List<String> nameIDFormats = new ArrayList<>();

    private final KeyStore trustStore;

    public SAML2IdPEntity(
            final EntityDescriptor entityDescriptor,
            final SAML2IdPTO idpTO,
            final String keyPass)
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {

        this.id = entityDescriptor.getEntityID();
        this.idpTO = idpTO;

        IDPSSODescriptor idpdescriptor = entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);

        for (SingleSignOnService sso : idpdescriptor.getSingleSignOnServices()) {
            LOG.debug("[{}] Add SSO binding {}({})", id, sso.getBinding(), sso.getLocation());
            this.ssoBindings.put(sso.getBinding(), sso);
        }

        for (SingleLogoutService slo : idpdescriptor.getSingleLogoutServices()) {
            LOG.debug("[{}] Add SLO binding '{}'\n\tLocation: '{}'\n\tResponse Location: '{}'",
                    id, slo.getBinding(), slo.getLocation(), slo.getResponseLocation());
            this.sloBindings.put(slo.getBinding(), slo);
        }

        for (NameIDFormat nameIDFormat : idpdescriptor.getNameIDFormats()) {
            LOG.debug("[{}] Add NameIDFormat '{}'", id, nameIDFormat.getFormat());
            nameIDFormats.add(nameIDFormat.getFormat());
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        List<X509Certificate> chain = new ArrayList<>();
        for (KeyDescriptor key : idpdescriptor.getKeyDescriptors()) {
            for (X509Data x509Data : key.getKeyInfo().getX509Datas()) {
                for (org.opensaml.xmlsec.signature.X509Certificate cert : x509Data.getX509Certificates()) {
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(
                            Base64.getMimeDecoder().decode(cert.getValue()))) {
                        chain.add(X509Certificate.class.cast(cf.generateCertificate(bais)));
                    }
                }
            }
        }

        this.trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        this.trustStore.load(null, keyPass.toCharArray());
        if (!chain.isEmpty()) {
            for (X509Certificate cert : chain) {
                LOG.debug("[{}] Add X.509 certificate {}", id, cert.getSubjectX500Principal().getName());
                this.trustStore.setCertificateEntry(cert.getSubjectX500Principal().getName(), cert);
            }
            LOG.debug("[{}] Set default X.509 certificate {}", id, chain.get(0).getSubjectX500Principal().getName());
            this.trustStore.setCertificateEntry(id, chain.get(0));
        }
    }

    public void setIdpTO(final SAML2IdPTO idpTO) {
        this.idpTO = idpTO;
    }

    public String getId() {
        return id;
    }

    public boolean isCreateUnmatching() {
        return idpTO.isCreateUnmatching();
    }

    public boolean isUpdateMatching() {
        return idpTO.isUpdateMatching();
    }

    public boolean isUseDeflateEncoding() {
        return idpTO.isUseDeflateEncoding();
    }

    public boolean isSupportUnsolicited() {
        return idpTO.isSupportUnsolicited();
    }

    public SAML2BindingType getBindingType() {
        return idpTO.getBindingType();
    }

    public ItemTO getConnObjectKeyItem() {
        return idpTO.getConnObjectKeyItem();
    }

    public List<ItemTO> getItems() {
        return idpTO.getItems();
    }

    public UserTO getUserTemplate() {
        return idpTO.getUserTemplate();
    }

    public Set<String> getActionsClassNames() {
        return idpTO.getActionsClassNames();
    }

    public Endpoint getSSOLocation(final SAML2BindingType bindingType) {
        return ssoBindings.get(bindingType.getUri());
    }

    public Endpoint getSLOLocation(final SAML2BindingType bindingType) {
        return sloBindings.get(bindingType.getUri());
    }

    public boolean supportsNameIDFormat(final String nameIDFormat) {
        return nameIDFormats.contains(nameIDFormat);
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

}
