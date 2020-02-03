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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.Base64;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.zip.DataFormatException;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.rs.security.saml.sso.SAMLProtocolResponseValidator;
import org.apache.cxf.rs.security.saml.sso.SAMLSSOResponseValidator;
import org.apache.cxf.rs.security.saml.sso.SSOValidatorResponse;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.syncope.common.lib.SSOConstants;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.lib.types.SignatureAlgorithm;
import org.apache.syncope.core.logic.init.SAML2SPLoader;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.xml.security.algorithms.JCEMapper;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.security.SecurityException;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
public class SAML2ReaderWriter {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2ReaderWriter.class);

    private static final TransformerFactory TRANSFORMER_FACTORY;

    static {
        TRANSFORMER_FACTORY = TransformerFactory.newInstance();
        TRANSFORMER_FACTORY.setURIResolver((href, base) -> null);
        try {
            TRANSFORMER_FACTORY.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException e) {
            LOG.error("Could not enable secure XML processing", e);
        }

        try {
            TRANSFORMER_FACTORY.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            TRANSFORMER_FACTORY.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException ex) {
             LOG.debug("The JAXP parser does not support the following attribute: ", ex);
        }
    }

    @Autowired
    private SAML2SPLoader loader;

    private KeyInfoGenerator keyInfoGenerator;

    private String sigAlgo;

    private String jceSigAlgo;

    private SAMLSPCallbackHandler callbackHandler;

    public void init() {
        X509KeyInfoGeneratorFactory keyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();
        keyInfoGeneratorFactory.setEmitEntityCertificate(true);
        keyInfoGenerator = keyInfoGeneratorFactory.newInstance();

        // Try to load a signature algorithm
        if (loader.getSignatureAlgorithm() != null) {
            SignatureAlgorithm loadedSignatureAlgorithm =
                    SignatureAlgorithm.valueOf(loader.getSignatureAlgorithm());
            sigAlgo = loadedSignatureAlgorithm.getAlgorithm();
            jceSigAlgo = JCEMapper.translateURItoJCEID(sigAlgo);
            if (jceSigAlgo == null) {
                LOG.warn("Signature algorithm {} is not valid. Using default algorithm instead.",
                        loader.getSignatureAlgorithm());
                sigAlgo = null;
            }
        }

        if (sigAlgo == null) {
            sigAlgo = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
            String pubKeyAlgo = loader.getCredential().getPublicKey().getAlgorithm();
            if (pubKeyAlgo.equalsIgnoreCase("DSA")) {
                sigAlgo = SignatureConstants.ALGO_ID_SIGNATURE_DSA_SHA1;
            } else if (pubKeyAlgo.equalsIgnoreCase("EC")) {
                sigAlgo = SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA1;
            }
            jceSigAlgo = JCEMapper.translateURItoJCEID(sigAlgo);
        }

        callbackHandler = new SAMLSPCallbackHandler(loader.getKeyPass());
    }

    public String getSigAlgo() {
        return sigAlgo;
    }

    public static void write(final Writer writer, final XMLObject object, final boolean signObject)
            throws TransformerConfigurationException, WSSecurityException, TransformerException {

        Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        StreamResult streamResult = new StreamResult(writer);
        DOMSource source = new DOMSource(OpenSAMLUtil.toDom(object, null, signObject));
        transformer.transform(source, streamResult);
    }

    public static XMLObject read(final boolean useDeflateEncoding, final String response)
            throws DataFormatException, UnsupportedEncodingException, XMLStreamException, WSSecurityException {

        InputStream tokenStream;
        byte[] deflatedToken = Base64.getDecoder().decode(response);
        tokenStream = useDeflateEncoding
                ? new DeflateEncoderDecoder().inflateToken(deflatedToken)
                : new ByteArrayInputStream(deflatedToken);

        // parse the provided SAML response
        Document responseDoc = StaxUtils.read(new InputStreamReader(tokenStream, StandardCharsets.UTF_8));
        XMLObject responseObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());

        if (LOG.isDebugEnabled()) {
            try {
                StringWriter writer = new StringWriter();
                write(writer, responseObject, false);
                writer.close();

                LOG.debug("Parsed SAML response: {}", writer.toString());
            } catch (Exception e) {
                LOG.error("Could not log the received SAML response", e);
            }
        }

        return responseObject;
    }

    public void sign(final SignableSAMLObject signableObject) throws SecurityException {
        org.opensaml.xmlsec.signature.Signature signature = OpenSAMLUtil.buildSignature();
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        signature.setSignatureAlgorithm(sigAlgo);
        signature.setSigningCredential(loader.getCredential());
        signature.setKeyInfo(keyInfoGenerator.generate(loader.getCredential()));

        signableObject.setSignature(signature);
        signableObject.releaseDOM();
        signableObject.releaseChildrenDOM(true);
    }

    public String sign(final String request, final String relayState)
            throws NoSuchAlgorithmException, WSSecurityException, InvalidKeyException, UnsupportedEncodingException,
            SignatureException {

        Merlin crypto = new Merlin();
        crypto.setKeyStore(loader.getKeyStore());
        PrivateKey privateKey = crypto.getPrivateKey(loader.getCredential().getPublicKey(), callbackHandler);

        java.security.Signature signature = java.security.Signature.getInstance(jceSigAlgo);
        signature.initSign(privateKey);

        String requestToSign =
                SSOConstants.SAML_REQUEST + '=' + request + '&'
                + SSOConstants.RELAY_STATE + '=' + relayState + '&'
                + SSOConstants.SIG_ALG + '=' + URLEncoder.encode(sigAlgo, StandardCharsets.UTF_8);
        signature.update(requestToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    public static String encode(final RequestAbstractType request, final boolean useDeflateEncoding)
            throws WSSecurityException, TransformerException, IOException {

        StringWriter writer = new StringWriter();
        write(writer, request, true);
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

        return Base64.getEncoder().encodeToString(deflatedBytes);
    }

    public SSOValidatorResponse validate(
            final Response samlResponse,
            final SAML2IdPEntity idp,
            final String assertionConsumerURL,
            final String requestId,
            final String spEntityID)
            throws WSSecurityException {

        // validate the SAML response and, if needed, decrypt the provided assertion(s)
        Merlin crypto = new Merlin();
        crypto.setKeyStore(loader.getKeyStore());
        crypto.setTrustStore(idp.getTrustStore());

        SAMLProtocolResponseValidator protocolValidator = new SAMLProtocolResponseValidator();
        protocolValidator.setKeyInfoMustBeAvailable(true);
        protocolValidator.validateSamlResponse(samlResponse, crypto, callbackHandler);

        SAMLSSOResponseValidator ssoResponseValidator = new SAMLSSOResponseValidator();
        ssoResponseValidator.setAssertionConsumerURL(assertionConsumerURL);
        ssoResponseValidator.setIssuerIDP(idp.getId());
        ssoResponseValidator.setRequestId(requestId);
        ssoResponseValidator.setSpIdentifier(spEntityID);
        SSOValidatorResponse validatorResponse =
                ssoResponseValidator.validateSamlResponse(samlResponse, idp.getBindingType() == SAML2BindingType.POST);

        if (LOG.isDebugEnabled()) {
            try {
                StringWriter writer = new StringWriter();
                write(writer, samlResponse, false);
                writer.close();

                LOG.debug("SAML response with decrypted assertions: {}", writer.toString());
            } catch (Exception e) {
                LOG.error("Could not log the SAML response with decrypted assertions", e);
            }
        }

        return validatorResponse;
    }
}
