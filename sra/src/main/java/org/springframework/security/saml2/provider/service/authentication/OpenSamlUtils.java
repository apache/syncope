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
package org.springframework.security.saml2.provider.service.authentication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.sra.security.saml2.ExtendedRelyingPartyRegistration;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.security.SecurityException;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.signature.SignableXMLObject;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureSupport;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.credentials.Saml2X509Credential;
import org.springframework.security.saml2.credentials.Saml2X509Credential.Saml2X509CredentialType;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;

public final class OpenSamlUtils {

    private static final List<String> BINDINGS = List.of(
            Saml2MessageBinding.POST.getUrn(), Saml2MessageBinding.REDIRECT.getUrn());

    private static ExtendedRelyingPartyRegistration build(
            final X509Credential credential, final EntityDescriptor entityDescriptor)
            throws IOException, CertificateException {

        RelyingPartyRegistration.Builder builder = RelyingPartyRegistration.withRegistrationId("SAML2").
                localEntityIdTemplate("{baseUrl}/saml2/service-provider-metadata/{registrationId}").
                assertionConsumerServiceUrlTemplate(
                        "{baseUrl}" + Saml2WebSsoAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI).
                providerDetails(config -> config.entityId(entityDescriptor.getEntityID()));

        IDPSSODescriptor ssoDesc = entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
        ssoDesc.getSingleSignOnServices().stream().
                filter(service -> BINDINGS.contains(service.getBinding())).
                findFirst().ifPresent(service -> {
                    builder.providerDetails(config -> config.
                            binding(Saml2MessageBinding.REDIRECT.getUrn().equals(service.getBinding())
                                    ? Saml2MessageBinding.REDIRECT : Saml2MessageBinding.POST).
                            webSsoUrl(service.getLocation()));
                });

        builder.credentials(c -> c.add(new Saml2X509Credential(
                credential.getPrivateKey(),
                credential.getEntityCertificate(),
                Saml2X509CredentialType.SIGNING,
                Saml2X509CredentialType.DECRYPTION)));

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        for (KeyDescriptor key : ssoDesc.getKeyDescriptors()) {
            if (key.getUse() != UsageType.ENCRYPTION) {
                for (X509Data x509Data : key.getKeyInfo().getX509Datas()) {
                    for (org.opensaml.xmlsec.signature.X509Certificate cert : x509Data.getX509Certificates()) {
                        try (ByteArrayInputStream bais = new ByteArrayInputStream(
                                Base64.getMimeDecoder().decode(cert.getValue()))) {

                            X509Certificate x509cert = X509Certificate.class.cast(cf.generateCertificate(bais));
                            builder.credentials(c -> c.add(new Saml2X509Credential(
                                    x509cert,
                                    Saml2X509CredentialType.VERIFICATION)));
                        }
                    }
                }
            }
        }

        ExtendedRelyingPartyRegistration extended = new ExtendedRelyingPartyRegistration(builder.build());

        ssoDesc.getSingleLogoutServices().stream().
                filter(service -> BINDINGS.contains(service.getBinding())).
                findFirst().ifPresent(service -> {
                    extended.setLogoutDetails(new RelyingPartyRegistration.ProviderDetails.Builder().
                            entityId(extended.getRelyingPartyRegistration().getProviderDetails().getEntityId()).
                            binding(Saml2MessageBinding.REDIRECT.getUrn().equals(service.getBinding())
                                    ? Saml2MessageBinding.REDIRECT : Saml2MessageBinding.POST).
                            webSsoUrl(service.getLocation()).build());
                });

        return extended;
    }

    public static List<ExtendedRelyingPartyRegistration> build(
            final X509Credential credential, final InputStream input) throws IOException, CertificateException {

        List<ExtendedRelyingPartyRegistration> result = new ArrayList<>();

        OpenSamlImplementation instance = OpenSamlImplementation.getInstance();
        XMLObject metadata = instance.resolve(IOUtils.toString(input, StandardCharsets.UTF_8.name()));
        if (metadata instanceof EntityDescriptor) {
            EntityDescriptor entityDescriptor = EntityDescriptor.class.cast(metadata);

            result.add(build(credential, entityDescriptor));
        } else if (metadata instanceof EntitiesDescriptor) {
            for (EntityDescriptor entityDescriptor : EntitiesDescriptor.class.cast(metadata).getEntityDescriptors()) {
                result.add(build(credential, entityDescriptor));
            }
        }

        return result;
    }

    public static String write(final XMLObject object) throws TransformerException {
        return OpenSamlImplementation.getInstance().serialize(object);
    }

    public static void sign(final SignableXMLObject object, final Credential credential) {
        SignatureSigningParameters parameters = new SignatureSigningParameters();
        parameters.setSigningCredential(credential);
        parameters.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        parameters.setSignatureReferenceDigestMethod(SignatureConstants.ALGO_ID_DIGEST_SHA256);
        parameters.setSignatureCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        try {
            SignatureSupport.signObject(object, parameters);
        } catch (MarshallingException | SignatureException | SecurityException e) {
            throw new Saml2Exception(e);
        }
    }

    private OpenSamlUtils() {
        // private constructor for static utility class
    }
}
