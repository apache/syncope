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
package org.apache.syncope.core.persistence.neo4j.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.Date;
import java.util.UUID;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.core.persistence.api.dao.SAML2SPEntityDAO;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPEntity;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SAML2SPEntityTest extends AbstractTest {

    private static Certificate createSelfSignedCert(final KeyPair keyPair) throws Exception {
        X500Name dn = new X500Name("cn=Unknown");
        V3TBSCertificateGenerator certGen = new V3TBSCertificateGenerator();

        certGen.setSerialNumber(new ASN1Integer(BigInteger.valueOf(1)));
        certGen.setIssuer(dn);
        certGen.setSubject(dn);
        certGen.setStartDate(new Time(new Date(System.currentTimeMillis() - 1000L)));

        Date expiration = new Date(System.currentTimeMillis() + 100000);
        certGen.setEndDate(new Time(expiration));

        AlgorithmIdentifier sigAlgID = new AlgorithmIdentifier(
                PKCSObjectIdentifiers.sha1WithRSAEncryption, DERNull.INSTANCE);
        certGen.setSignature(sigAlgID);
        certGen.setSubjectPublicKeyInfo(SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));

        Signature sig = Signature.getInstance("SHA1WithRSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(certGen.generateTBSCertificate().getEncoded(ASN1Encoding.DER));

        TBSCertificate tbsCert = certGen.generateTBSCertificate();
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(tbsCert);
        v.add(sigAlgID);
        v.add(new DERBitString(sig.sign()));

        Certificate cert = CertificateFactory.getInstance("X.509").
                generateCertificate(new ByteArrayInputStream(new DERSequence(v).getEncoded(ASN1Encoding.DER)));
        cert.verify(keyPair.getPublic());
        return cert;
    }

    @Autowired
    private SAML2SPEntityDAO saml2SPEntityDAO;

    private SAML2SPEntity create(final String owner) throws Exception {
        SAML2SPEntity entity = entityFactory.newEntity(SAML2SPEntity.class);
        entity.setKey(owner);
        entity.setMetadata(IOUtils.toString(
                new ClassPathResource("sp-metadata.xml").getInputStream()).getBytes(StandardCharsets.UTF_8));

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] pwdArray = "password".toCharArray();
        ks.load(null, pwdArray);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        Certificate certificate = createSelfSignedCert(keyPair);
        ks.setKeyEntry("main", keyPair.getPrivate(), "password".toCharArray(), new Certificate[] { certificate });

        try (ByteArrayOutputStream fos = new ByteArrayOutputStream()) {
            ks.store(fos, pwdArray);
            fos.flush();
            entity.setKeystore(fos.toByteArray());
        }
        assertNotNull(entity.getKeystore());

        entity = saml2SPEntityDAO.save(entity);
        assertNotNull(saml2SPEntityDAO.findById(entity.getKey()));
        return entity;
    }

    @Test
    public void find() throws Exception {
        create("Syncope");

        assertTrue(saml2SPEntityDAO.findById("Syncope").isPresent());

        assertTrue(saml2SPEntityDAO.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    public void save() throws Exception {
        SAML2SPEntity created = create("SyncopeCreate");

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] pwdArray = "password".toCharArray();
        ks.load(new ByteArrayInputStream(created.getKeystore()), pwdArray);
        assertTrue(ks.size() > 0);
    }

    @Test
    public void update() throws Exception {
        SAML2SPEntity entity = create("SyncopeUpdate");
        assertNotNull(entity);
        entity.setKey("OtherSyncope");

        entity = saml2SPEntityDAO.save(entity);
        assertNotNull(entity);

        SAML2SPEntity found = saml2SPEntityDAO.findById(entity.getKey()).orElseThrow();
        assertEquals("OtherSyncope", found.getKey());
    }
}
