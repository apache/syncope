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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.zip.DataFormatException;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.rs.security.saml.sso.SAMLProtocolResponseValidator;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.syncope.core.logic.init.SAML2SPLoader;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Response;
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
        try {
            TRANSFORMER_FACTORY.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException e) {
            LOG.error("Could not enable secure XML processing", e);
        }
    }

    @Autowired
    private SAML2SPLoader loader;

    private SAMLProtocolResponseValidator protocolValidator;

    private SAML2IdPCallbackHandler callbackHandler;

    public void init() {
        protocolValidator = new SAMLProtocolResponseValidator();
        protocolValidator.setKeyInfoMustBeAvailable(true);

        callbackHandler = new SAML2IdPCallbackHandler(loader.getKeyPass());
    }

    public void write(final Writer writer, final XMLObject object, final boolean signObject)
            throws TransformerConfigurationException, WSSecurityException, TransformerException {

        Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        StreamResult streamResult = new StreamResult(writer);
        DOMSource source = new DOMSource(OpenSAMLUtil.toDom(object, null, signObject));
        transformer.transform(source, streamResult);
    }

    public XMLObject read(final boolean postBinding, final boolean useDeflateEncoding, final String response)
            throws DataFormatException, UnsupportedEncodingException, XMLStreamException, WSSecurityException {

        String decodedResponse = response;
        // URL Decoding only applies for the redirect binding
        if (!postBinding) {
            decodedResponse = URLDecoder.decode(response, StandardCharsets.UTF_8.name());
        }

        InputStream tokenStream;
        byte[] deflatedToken = Base64.decodeBase64(decodedResponse);
        tokenStream = !postBinding && useDeflateEncoding
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

    public void validate(final Response samlResponse, final KeyStore idpTrustStore) throws WSSecurityException {
        // validate the SAML response and, if needed, decrypt the provided assertion(s)
        Merlin crypto = new Merlin();
        crypto.setKeyStore(loader.getKeyStore());
        crypto.setTrustStore(idpTrustStore);

        protocolValidator.validateSamlResponse(samlResponse, crypto, callbackHandler);

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
    }

}
