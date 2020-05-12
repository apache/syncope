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
package org.apache.syncope.core.persistence.jpa.inner;

import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPKeystoreDAO;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SPKeystore;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional("Master")
public class SAML2SPKeystoreTest extends AbstractTest {

    @Autowired
    private SAML2SPKeystoreDAO saml2SPKeystoreDAO;

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
    public void find() throws Exception {
        create("Syncope");
        SAML2SPKeystore saml2SPKeystore = saml2SPKeystoreDAO.findByOwner("Syncope");
        assertNotNull(saml2SPKeystore);

        saml2SPKeystore = saml2SPKeystoreDAO.findByOwner(UUID.randomUUID().toString());
        assertNull(saml2SPKeystore);
    }

    @Test
    public void save() throws Exception {
        final SAML2SPKeystore keystore = create("SyncopeCreate");
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] pwdArray = "password".toCharArray();
        ks.load(new ByteArrayInputStream(Base64.getDecoder().decode(keystore.getKeystore())), pwdArray);
        assertTrue(ks.size() > 0);
    }

    @Test
    public void update() throws Exception {
        SAML2SPKeystore saml2SPKeystore = create("SyncopeUpdate");
        assertNotNull(saml2SPKeystore);
        saml2SPKeystore.setOwner("OtherSyncope");

        saml2SPKeystore = saml2SPKeystoreDAO.save(saml2SPKeystore);
        assertNotNull(saml2SPKeystore);
        assertNotNull(saml2SPKeystore.getKey());
        SAML2SPKeystore found = saml2SPKeystoreDAO.findByOwner(saml2SPKeystore.getOwner());
        assertNotNull(found);
        assertEquals("OtherSyncope", found.getOwner());
    }

    private SAML2SPKeystore create(final String owner) throws Exception {
        final SAML2SPKeystore saml2SPKeystore = entityFactory.newEntity(SAML2SPKeystore.class);
        saml2SPKeystore.setOwner(owner);

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] pwdArray = "password".toCharArray();
        ks.load(null, pwdArray);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        Certificate certificate = createSelfSignedCert(keyPair);
        ks.setKeyEntry("main", keyPair.getPrivate(), "password".toCharArray(), new Certificate[]{certificate});

        try (ByteArrayOutputStream fos = new ByteArrayOutputStream()) {
            ks.store(fos, pwdArray);
            fos.flush();
            String keystore = Base64.getEncoder().encodeToString(fos.toByteArray());
            saml2SPKeystore.setKeystore(keystore);
        }
        saml2SPKeystoreDAO.save(saml2SPKeystore);
        assertNotNull(saml2SPKeystore);
        assertNotNull(saml2SPKeystore.getKey());
        assertNotNull(saml2SPKeystoreDAO.findByOwner(saml2SPKeystore.getOwner()));
        return saml2SPKeystore;
    }
}
