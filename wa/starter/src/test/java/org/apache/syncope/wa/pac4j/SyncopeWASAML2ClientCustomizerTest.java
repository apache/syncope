/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.wa.pac4j;

import org.apache.commons.io.IOUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.SAML2SPKeystoreTO;
import org.apache.syncope.common.lib.to.SAML2SPMetadataTO;
import org.apache.syncope.common.rest.api.service.SAML2SPKeystoreService;
import org.apache.syncope.common.rest.api.service.SAML2SPMetadataService;
import org.apache.syncope.wa.WARestClient;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.junit.jupiter.api.Test;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SyncopeWASAML2ClientCustomizerTest {

    private static Certificate createSelfSignedCert(final KeyPair keyPair) throws Exception {
        final X500Name dn = new X500Name("cn=Unknown");
        final V3TBSCertificateGenerator certGen = new V3TBSCertificateGenerator();

        certGen.setSerialNumber(new ASN1Integer(BigInteger.valueOf(1)));
        certGen.setIssuer(dn);
        certGen.setSubject(dn);
        certGen.setStartDate(new Time(new Date(System.currentTimeMillis() - 1000L)));

        final Date expiration = new Date(System.currentTimeMillis() + 100000);
        certGen.setEndDate(new Time(expiration));

        final AlgorithmIdentifier sigAlgID = new AlgorithmIdentifier(PKCSObjectIdentifiers.sha1WithRSAEncryption, DERNull.INSTANCE);
        certGen.setSignature(sigAlgID);
        certGen.setSubjectPublicKeyInfo(SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));

        final Signature sig = Signature.getInstance("SHA1WithRSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(certGen.generateTBSCertificate().getEncoded(ASN1Encoding.DER));

        final TBSCertificate tbsCert = certGen.generateTBSCertificate();
        final ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(tbsCert);
        v.add(sigAlgID);
        v.add(new DERBitString(sig.sign()));

        final Certificate cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(new ByteArrayInputStream(new DERSequence(v).getEncoded(ASN1Encoding.DER)));
        cert.verify(keyPair.getPublic());
        return cert;
    }

    @Test
    public void customize() throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] pwdArray = "password".toCharArray();
        ks.load(null, pwdArray);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        Certificate certificate = createSelfSignedCert(keyPair);
        ks.setKeyEntry("Syncope", keyPair.getPrivate(), "password".toCharArray(), new Certificate[]{certificate});

        try (ByteArrayOutputStream fos = new ByteArrayOutputStream()) {
            ks.store(fos, pwdArray);
            fos.flush();
            String keystore = Base64.getEncoder().encodeToString(fos.toByteArray());

            SAML2SPKeystoreTO keystoreTO = new SAML2SPKeystoreTO.Builder()
                .keystore(keystore)
                .owner("CAS")
                .build();
            SAML2SPKeystoreService saml2SPKeystoreService = mock(SAML2SPKeystoreService.class);
            when(saml2SPKeystoreService.get(anyString())).thenReturn(keystoreTO);
            when(saml2SPKeystoreService.set(any())).thenReturn(Response.ok().build());

            SAML2SPMetadataTO metadataTO = new SAML2SPMetadataTO.Builder()
                .owner("Syncope")
                .metadata(IOUtils.toString(new ClassPathResource("sp-metadata.xml").getInputStream(), StandardCharsets.UTF_8))
                .build();

            SAML2SPMetadataService saml2SPMetadataService = mock(SAML2SPMetadataService.class);
            when(saml2SPMetadataService.get(anyString())).thenReturn(metadataTO);
            when(saml2SPMetadataService.set(any())).thenReturn(Response.ok().build());

            WARestClient restClient = mock(WARestClient.class);

            SyncopeClient syncopeClient = mock(SyncopeClient.class);
            when(syncopeClient.getService(SAML2SPKeystoreService.class)).thenReturn(saml2SPKeystoreService);
            when(syncopeClient.getService(SAML2SPMetadataService.class)).thenReturn(saml2SPMetadataService);
            when(restClient.getSyncopeClient()).thenReturn(syncopeClient);
            
            SyncopeWASAML2ClientCustomizer customizer = new SyncopeWASAML2ClientCustomizer(restClient);
            SAML2Configuration saml2Configuration = new SAML2Configuration();
            saml2Configuration.setKeystorePassword("password");
            saml2Configuration.setPrivateKeyPassword("password");
            saml2Configuration.setKeystoreAlias("Syncope");
            saml2Configuration.setIdentityProviderMetadataResource(new ClassPathResource("idp-metadata.xml"));
            SAML2Client client = new SAML2Client(saml2Configuration);
            client.setCallbackUrl("https://syncope.apache.org");
            customizer.customize(client);
            client.init();
            assertTrue(client.getConfiguration().getKeystoreGenerator() instanceof SyncopeWASamlClientKeystoreGenerator);
            assertTrue(client.getConfiguration().toMetadataGenerator() instanceof SyncopeWASamlClientMetadataGenerator);
        }
    }

}
