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
package org.apache.syncope.wa.starter.pac4j.saml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Date;
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
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.saml.metadata.SAML2IdentityProviderMetadataResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

public abstract class BaseWASAML2ClientTest {

    protected static Certificate createSelfSignedCert(final KeyPair keyPair) throws Exception {
        final X500Name dn = new X500Name("cn=Unknown");
        final V3TBSCertificateGenerator certGen = new V3TBSCertificateGenerator();

        certGen.setSerialNumber(new ASN1Integer(BigInteger.valueOf(1)));
        certGen.setIssuer(dn);
        certGen.setSubject(dn);
        certGen.setStartDate(new Time(new Date(System.currentTimeMillis() - 1000L)));

        final Date expiration = new Date(System.currentTimeMillis() + 100000);
        certGen.setEndDate(new Time(expiration));

        final AlgorithmIdentifier sigAlgID = new AlgorithmIdentifier(PKCSObjectIdentifiers.sha1WithRSAEncryption,
                DERNull.INSTANCE);
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

    protected static SAML2Client getSAML2Client() throws Exception {
        SAML2Configuration cfg = new SAML2Configuration();
        cfg.setKeystorePassword("password");
        cfg.setPrivateKeyPassword("password");

        cfg.setIdentityProviderMetadataResource(new ClassPathResource("idp-metadata.xml"));

        SAML2IdentityProviderMetadataResolver idpMetadataResolver = new SAML2IdentityProviderMetadataResolver(cfg);
        idpMetadataResolver.init();
        cfg.setIdentityProviderMetadataResolver(idpMetadataResolver);

        cfg.setServiceProviderMetadataResource(new FileSystemResource(File.createTempFile("sp-metadata", ".xml")));

        SAML2Client client = new SAML2Client(cfg);
        client.setCallbackUrl("https://syncope.apache.org");
        return client;
    }

    protected static KeyStore getKeystore() throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] pwdArray = "password".toCharArray();
        ks.load(null, pwdArray);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        Certificate certificate = createSelfSignedCert(keyPair);
        ks.setKeyEntry("Syncope", keyPair.getPrivate(), "password".toCharArray(), new Certificate[] { certificate });
        return ks;
    }

    protected static String getKeystoreAsString() throws Exception {
        char[] pwdArray = "password".toCharArray();
        try (ByteArrayOutputStream fos = new ByteArrayOutputStream()) {
            getKeystore().store(fos, pwdArray);
            fos.flush();
            return Base64.getEncoder().encodeToString(fos.toByteArray());
        }
    }
}
