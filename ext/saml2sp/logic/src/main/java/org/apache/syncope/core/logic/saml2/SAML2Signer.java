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
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.TransformerException;
import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.syncope.core.logic.init.SAML2SPLoader;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.opensaml.security.SecurityException;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SAML2Signer {

    @Autowired
    private SAML2SPLoader loader;

    @Autowired
    private SAML2ReaderWriter saml2rw;

    private KeyInfoGenerator keyInfoGenerator;

    private String signatureAlgorithm;

    public void init() {
        X509KeyInfoGeneratorFactory keyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();
        keyInfoGeneratorFactory.setEmitEntityCertificate(true);
        keyInfoGenerator = keyInfoGeneratorFactory.newInstance();

        signatureAlgorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
        String pubKeyAlgo = loader.getCredential().getPublicKey().getAlgorithm();
        if (pubKeyAlgo.equalsIgnoreCase("DSA")) {
            signatureAlgorithm = SignatureConstants.ALGO_ID_SIGNATURE_DSA_SHA1;
        }
    }

    public String signAndEncode(final RequestAbstractType request, final boolean useDeflateEncoding)
            throws SecurityException, WSSecurityException, TransformerException, IOException {

        // 1. sign request
        Signature signature = OpenSAMLUtil.buildSignature();
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        signature.setSignatureAlgorithm(signatureAlgorithm);
        signature.setSigningCredential(loader.getCredential());
        signature.setKeyInfo(keyInfoGenerator.generate(loader.getCredential()));

        SignableSAMLObject signableObject = (SignableSAMLObject) request;
        signableObject.setSignature(signature);
        signableObject.releaseDOM();
        signableObject.releaseChildrenDOM(true);

        // 2. serialize and encode request
        StringWriter writer = new StringWriter();
        saml2rw.write(writer, request, true);
        writer.close();

        String requestMessage = writer.toString();
        byte[] deflatedBytes;
        // not correct according to the spec but required by some IdPs.
        if (useDeflateEncoding) {
            deflatedBytes = new DeflateEncoderDecoder().
                    deflateToken(requestMessage.getBytes(StandardCharsets.UTF_8));
        } else {
            deflatedBytes = requestMessage.getBytes(StandardCharsets.UTF_8);
        }

        return Base64.encodeBase64String(deflatedBytes);
    }

}
