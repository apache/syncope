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
package org.apache.syncope.core.logic;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.SAML2RequestTO;
import org.apache.syncope.common.lib.to.SAML2LoginResponseTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.SAML2ReceivedResponseTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.logic.saml2.SAML2ReaderWriter;
import org.apache.syncope.core.logic.saml2.SAML2IdPCache;
import org.apache.syncope.core.logic.saml2.SAML2IdPEntity;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.SAML2IdP;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.data.MappingItemTransformer;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
import org.apache.syncope.core.provisioning.java.IntAttrNameParser;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.SessionIndex;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml.saml2.core.impl.LogoutRequestBuilder;
import org.opensaml.saml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml.saml2.core.impl.NameIDPolicyBuilder;
import org.opensaml.saml.saml2.core.impl.RequestedAuthnContextBuilder;
import org.opensaml.saml.saml2.core.impl.SessionIndexBuilder;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.impl.AssertionConsumerServiceBuilder;
import org.opensaml.saml.saml2.metadata.impl.EntityDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.KeyDescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.NameIDFormatBuilder;
import org.opensaml.saml.saml2.metadata.impl.SPSSODescriptorBuilder;
import org.opensaml.saml.saml2.metadata.impl.SingleLogoutServiceBuilder;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class SAML2SPLogic extends AbstractSAML2Logic<AbstractBaseBean> {

    private static final Integer JWT_RELAY_STATE_DURATION = 5;

    private static final String JWT_CLAIM_IDP_DEFLATE = "IDP_DEFLATE";

    private static final String JWT_CLAIM_IDP_ENTITYID = "IDP_ENTITYID";

    private static final String JWT_CLAIM_NAMEID_FORMAT = "NAMEID_FORMAT";

    private static final String JWT_CLAIM_NAMEID_VALUE = "NAMEID_VALUE";

    private static final String JWT_CLAIM_SESSIONINDEX = "SESSIONINDEX";

    private static final RandomBasedGenerator UUID_GENERATOR = Generators.randomBasedGenerator();

    @Autowired
    private JwsSignatureVerifier jwsSignatureCerifier;

    @Autowired
    private AccessTokenDataBinder accessTokenDataBinder;

    @Autowired
    private SAML2IdPCache cache;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private SAML2IdPDAO saml2IdPDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private SAML2ReaderWriter saml2rw;

    @PreAuthorize("hasRole('" + StandardEntitlement.ANONYMOUS + "')")
    public void getMetadata(final String spEntityID, final String urlContext, final OutputStream os) {
        check();

        try {
            EntityDescriptor spEntityDescriptor = new EntityDescriptorBuilder().buildObject();
            spEntityDescriptor.setEntityID(spEntityID);

            SPSSODescriptor spSSODescriptor = new SPSSODescriptorBuilder().buildObject();
            spSSODescriptor.setWantAssertionsSigned(true);
            spSSODescriptor.setAuthnRequestsSigned(true);
            spSSODescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);

            X509KeyInfoGeneratorFactory keyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();
            keyInfoGeneratorFactory.setEmitEntityCertificate(true);
            KeyInfoGenerator keyInfoGenerator = keyInfoGeneratorFactory.newInstance();
            keyInfoGenerator.generate(loader.getCredential());

            KeyDescriptor keyDescriptor = new KeyDescriptorBuilder().buildObject();
            keyDescriptor.setKeyInfo(keyInfoGenerator.generate(loader.getCredential()));
            spSSODescriptor.getKeyDescriptors().add(keyDescriptor);

            NameIDFormat nameIDFormat = new NameIDFormatBuilder().buildObject();
            nameIDFormat.setFormat(NameIDType.PERSISTENT);
            spSSODescriptor.getNameIDFormats().add(nameIDFormat);
            nameIDFormat = new NameIDFormatBuilder().buildObject();
            nameIDFormat.setFormat(NameIDType.TRANSIENT);
            spSSODescriptor.getNameIDFormats().add(nameIDFormat);

            for (SAML2BindingType bindingType : SAML2BindingType.values()) {
                AssertionConsumerService assertionConsumerService = new AssertionConsumerServiceBuilder().buildObject();
                assertionConsumerService.setIndex(bindingType.getIndex());
                assertionConsumerService.setBinding(bindingType.getUri());
                assertionConsumerService.setLocation(spEntityID + urlContext + "/assertion-consumer");
                spSSODescriptor.getAssertionConsumerServices().add(assertionConsumerService);
                spEntityDescriptor.getRoleDescriptors().add(spSSODescriptor);

                SingleLogoutService singleLogoutService = new SingleLogoutServiceBuilder().buildObject();
                singleLogoutService.setBinding(bindingType.getUri());
                singleLogoutService.setLocation(spEntityID + urlContext + "/logout");
                singleLogoutService.setResponseLocation(spEntityID + urlContext + "/logout");
                spSSODescriptor.getSingleLogoutServices().add(singleLogoutService);
            }

            spEntityDescriptor.getRoleDescriptors().add(spSSODescriptor);

            saml2rw.write(new OutputStreamWriter(os), spEntityDescriptor, true);
        } catch (Exception e) {
            LOG.error("While getting SP metadata", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    private SAML2IdPEntity getIdP(final String entityID) {
        SAML2IdPEntity idp = null;

        SAML2IdP saml2IdP = saml2IdPDAO.findByEntityID(entityID);
        if (saml2IdP != null) {
            try {
                idp = cache.put(saml2IdP);
            } catch (Exception e) {
                LOG.error("Could not build SAML 2.0 IdP with key ", entityID, e);
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
                sce.getElements().add(e.getMessage());
                throw sce;
            }
        }

        if (idp == null) {
            throw new NotFoundException("SAML 2.0 IdP '" + entityID + "'");
        }
        return idp;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.ANONYMOUS + "')")
    public SAML2RequestTO createLoginRequest(final String spEntityID, final String idpEntityID) {
        check();

        // 1. look for IdP
        SAML2IdPEntity idp = StringUtils.isBlank(idpEntityID) ? cache.getFirst() : cache.get(idpEntityID);
        if (idp == null) {
            if (StringUtils.isBlank(idpEntityID)) {
                List<SAML2IdP> all = saml2IdPDAO.findAll();
                if (!all.isEmpty()) {
                    idp = getIdP(all.get(0).getKey());
                }
            } else {
                idp = getIdP(idpEntityID);
            }
        }
        if (idp == null) {
            throw new NotFoundException(StringUtils.isBlank(idpEntityID)
                    ? "Any SAML 2.0 IdP"
                    : "SAML 2.0 IdP '" + idpEntityID + "'");
        }

        if (idp.getSSOLocation(idp.getBindingType()) == null) {
            throw new IllegalArgumentException("No SingleSignOnService available for " + idp.getId());
        }

        // 2. create AuthnRequest
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue(spEntityID);

        NameIDPolicy nameIDPolicy = new NameIDPolicyBuilder().buildObject();
        if (idp.supportsNameIDFormat(NameIDType.TRANSIENT)) {
            nameIDPolicy.setFormat(NameIDType.TRANSIENT);
        } else if (idp.supportsNameIDFormat(NameIDType.PERSISTENT)) {
            nameIDPolicy.setFormat(NameIDType.PERSISTENT);
        } else {
            throw new IllegalArgumentException("Could not find supported NameIDFormat for IdP " + idpEntityID);
        }
        nameIDPolicy.setAllowCreate(true);
        nameIDPolicy.setSPNameQualifier(spEntityID);

        AuthnContextClassRef authnContextClassRef = new AuthnContextClassRefBuilder().buildObject();
        authnContextClassRef.setAuthnContextClassRef(AuthnContext.PPT_AUTHN_CTX);
        RequestedAuthnContext requestedAuthnContext = new RequestedAuthnContextBuilder().buildObject();
        requestedAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.EXACT);
        requestedAuthnContext.getAuthnContextClassRefs().add(authnContextClassRef);

        AuthnRequest authnRequest = new AuthnRequestBuilder().buildObject();
        authnRequest.setID("_" + UUID_GENERATOR.generate().toString());
        authnRequest.setForceAuthn(false);
        authnRequest.setIsPassive(false);
        authnRequest.setVersion(SAMLVersion.VERSION_20);
        authnRequest.setProtocolBinding(idp.getBindingType().getUri());
        authnRequest.setIssueInstant(new DateTime());
        authnRequest.setIssuer(issuer);
        authnRequest.setNameIDPolicy(nameIDPolicy);
        authnRequest.setRequestedAuthnContext(requestedAuthnContext);
        authnRequest.setDestination(idp.getSSOLocation(idp.getBindingType()).getLocation());

        SAML2RequestTO requestTO = new SAML2RequestTO();
        requestTO.setIdpServiceAddress(authnRequest.getDestination());
        requestTO.setBindingType(idp.getBindingType());
        try {
            // 3. generate relay state as JWT
            Map<String, Object> claims = new HashMap<>();
            claims.put(JWT_CLAIM_IDP_DEFLATE, idp.isUseDeflateEncoding());
            Triple<String, String, Date> relayState =
                    accessTokenDataBinder.generateJWT(authnRequest.getID(), JWT_RELAY_STATE_DURATION, claims);

            // 4. sign and encode AuthnRequest
            switch (idp.getBindingType()) {
                case REDIRECT:
                    requestTO.setRelayState(URLEncoder.encode(relayState.getMiddle(), StandardCharsets.UTF_8.name()));
                    requestTO.setContent(URLEncoder.encode(
                            saml2rw.encode(authnRequest, true), StandardCharsets.UTF_8.name()));
                    requestTO.setSignAlg(URLEncoder.encode(saml2rw.getSigAlgo(), StandardCharsets.UTF_8.name()));
                    requestTO.setSignature(URLEncoder.encode(
                            saml2rw.sign(requestTO.getContent(), requestTO.getRelayState()),
                            StandardCharsets.UTF_8.name()));
                    break;

                case POST:
                default:
                    requestTO.setRelayState(relayState.getMiddle());
                    saml2rw.sign(authnRequest);
                    requestTO.setContent(saml2rw.encode(authnRequest, idp.isUseDeflateEncoding()));
            }
        } catch (Exception e) {
            LOG.error("While generating AuthnRequest", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return requestTO;
    }

    private List<String> findMatchingUser(final String keyValue, final MappingItemTO connObjectKeyItem) {
        List<String> result = new ArrayList<>();

        String transformed = keyValue;
        for (MappingItemTransformer transformer : MappingUtils.getMappingItemTransformers(connObjectKeyItem)) {
            List<Object> output = transformer.beforePull(
                    null,
                    null,
                    Collections.<Object>singletonList(transformed));
            if (output != null && !output.isEmpty()) {
                transformed = output.get(0).toString();
            }
        }

        IntAttrName intAttrName = intAttrNameParser.parse(connObjectKeyItem.getIntAttrName(), AnyTypeKind.USER);

        if (intAttrName.getField() != null) {
            switch (intAttrName.getField()) {
                case "key":
                    User byKey = userDAO.find(transformed);
                    if (byKey != null) {
                        result.add(byKey.getKey());
                    }
                    break;

                case "username":
                    User byUsername = userDAO.findByUsername(transformed);
                    if (byUsername != null) {
                        result.add(byUsername.getKey());
                    }
                    break;

                default:
            }
        } else if (intAttrName.getSchemaType() != null) {
            switch (intAttrName.getSchemaType()) {
                case PLAIN:
                    PlainAttrValue value = entityFactory.newEntity(UPlainAttrValue.class);

                    PlainSchema schema = plainSchemaDAO.find(intAttrName.getSchemaName());
                    if (schema == null) {
                        value.setStringValue(transformed);
                    } else {
                        try {
                            value.parseValue(schema, transformed);
                        } catch (ParsingValidationException e) {
                            LOG.error("While parsing provided key value {}", transformed, e);
                            value.setStringValue(transformed);
                        }
                    }

                    CollectionUtils.collect(userDAO.findByAttrValue(intAttrName.getSchemaName(), value),
                            EntityUtils.keyTransformer(), result);
                    break;

                case DERIVED:
                    CollectionUtils.collect(userDAO.findByDerAttrValue(intAttrName.getSchemaName(), transformed),
                            EntityUtils.keyTransformer(), result);
                    break;

                default:
            }
        }

        return result;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.ANONYMOUS + "')")
    public SAML2LoginResponseTO validateLoginResponse(final SAML2ReceivedResponseTO response) {
        check();

        // 1. first checks for the provided relay state
        JwsJwtCompactConsumer relayState = new JwsJwtCompactConsumer(response.getRelayState());
        if (!relayState.verifySignatureWith(jwsSignatureCerifier)) {
            throw new IllegalArgumentException("Invalid signature found in Relay State");
        }
        Boolean useDeflateEncoding = Boolean.valueOf(
                relayState.getJwtClaims().getClaim(JWT_CLAIM_IDP_DEFLATE).toString());

        // 2. parse the provided SAML response
        Response samlResponse;
        try {
            XMLObject responseObject = saml2rw.read(
                    SAML2BindingType.POST, useDeflateEncoding, response.getSamlResponse());
            if (!(responseObject instanceof Response)) {
                throw new IllegalArgumentException("Expected " + Response.class.getName()
                        + ", got " + responseObject.getClass().getName());
            }
            samlResponse = (Response) responseObject;
        } catch (Exception e) {
            LOG.error("While parsing AuthnResponse", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        // 3. further checks:
        //   3a. the SAML Reponse's InResponseTo
        if (!relayState.getJwtClaims().getSubject().equals(samlResponse.getInResponseTo())) {
            throw new IllegalArgumentException("Unmatching request ID: " + samlResponse.getInResponseTo());
        }
        //   3b. the SAML Response status
        if (!StatusCode.SUCCESS.equals(samlResponse.getStatus().getStatusCode().getValue())) {
            throw new BadCredentialsException("The SAML IdP replied with "
                    + samlResponse.getStatus().getStatusCode().getValue());
        }

        // 4. validate the SAML response and, if needed, decrypt the provided assertion(s)
        SAML2IdPEntity idp = getIdP(samlResponse.getIssuer().getValue());
        if (idp.getConnObjectKeyItem() == null) {
            throw new IllegalArgumentException("No mapping provided for SAML 2.0 IdP '" + idp.getId() + "'");
        }
        try {
            saml2rw.validate(samlResponse, idp.getTrustStore());
        } catch (Exception e) {
            LOG.error("While validating AuthnResponse", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        // 5. prepare the result: find matching user (if any) and return the received attributes
        SAML2LoginResponseTO responseTO = new SAML2LoginResponseTO();
        responseTO.setIdp(idp.getId());
        responseTO.setSloSupported(idp.getSLOLocation(idp.getBindingType()) != null);

        NameID nameID = null;
        String keyValue = null;
        for (Assertion assertion : samlResponse.getAssertions()) {
            nameID = assertion.getSubject().getNameID();
            if (StringUtils.isNotBlank(nameID.getValue())
                    && idp.getConnObjectKeyItem().getExtAttrName().equals("NameID")) {

                keyValue = nameID.getValue();
            }

            if (assertion.getConditions().getNotOnOrAfter() != null) {
                responseTO.setNotOnOrAfter(assertion.getConditions().getNotOnOrAfter().toDate());
            }
            for (AuthnStatement authnStmt : assertion.getAuthnStatements()) {
                responseTO.setSessionIndex(authnStmt.getSessionIndex());

                responseTO.setAuthInstant(authnStmt.getAuthnInstant().toDate());
                if (authnStmt.getSessionNotOnOrAfter() != null) {
                    responseTO.setNotOnOrAfter(authnStmt.getSessionNotOnOrAfter().toDate());
                }
            }

            for (AttributeStatement attrStmt : assertion.getAttributeStatements()) {
                for (Attribute attr : attrStmt.getAttributes()) {
                    if (!attr.getAttributeValues().isEmpty()) {
                        String attrName = attr.getFriendlyName() == null ? attr.getName() : attr.getFriendlyName();
                        if (attrName.equals(idp.getConnObjectKeyItem().getExtAttrName())
                                && attr.getAttributeValues().get(0) instanceof XSString) {

                            keyValue = ((XSString) attr.getAttributeValues().get(0)).getValue();
                        }

                        AttrTO attrTO = new AttrTO();
                        attrTO.setSchema(attrName);
                        for (XMLObject value : attr.getAttributeValues()) {
                            if (value.getDOM() != null) {
                                attrTO.getValues().add(value.getDOM().getTextContent());
                            }
                        }
                        responseTO.getAttrs().add(attrTO);
                    }
                }
            }
        }
        if (nameID == null) {
            throw new IllegalArgumentException("NameID not found");
        }

        List<String> matchingUsers = keyValue == null
                ? Collections.<String>emptyList()
                : findMatchingUser(keyValue, idp.getConnObjectKeyItem());
        LOG.debug("Found {} matching users for NameID {}", matchingUsers.size(), nameID.getValue());

        if (matchingUsers.isEmpty()) {
            throw new NotFoundException("User matching the provided NameID value " + nameID.getValue());
        } else if (matchingUsers.size() > 1) {
            throw new IllegalArgumentException("Several users match the provided NameID value " + nameID.getValue());
        }
        responseTO.setUsername(userDAO.find(matchingUsers.get(0)).getUsername());

        responseTO.setNameID(nameID.getValue());
        // 6. generate JWT for further access
        Map<String, Object> claims = new HashMap<>();
        claims.put(JWT_CLAIM_IDP_ENTITYID, idp.getId());
        claims.put(JWT_CLAIM_NAMEID_FORMAT, nameID.getFormat());
        claims.put(JWT_CLAIM_NAMEID_VALUE, nameID.getValue());
        claims.put(JWT_CLAIM_SESSIONINDEX, responseTO.getSessionIndex());
        responseTO.setAccessToken(accessTokenDataBinder.create(responseTO.getUsername(), claims, true));

        return responseTO;
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + StandardEntitlement.ANONYMOUS + "'))")
    public SAML2RequestTO createLogoutRequest(final String accessToken, final String spEntityID) {
        check();

        // 1. fetch the current JWT used for Syncope authentication
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(accessToken);
        if (!consumer.verifySignatureWith(jwsSignatureCerifier)) {
            throw new IllegalArgumentException("Invalid signature found in Access Token");
        }

        // 2. look for IdP
        String idpEntityID = (String) consumer.getJwtClaims().getClaim(JWT_CLAIM_IDP_ENTITYID);
        if (idpEntityID == null) {
            throw new NotFoundException("No SAML 2.0 IdP information found in the access token");
        }
        SAML2IdPEntity idp = cache.get(idpEntityID);
        if (idp == null) {
            throw new NotFoundException("SAML 2.0 IdP '" + idpEntityID + "'");
        }
        if (idp.getSLOLocation(idp.getBindingType()) == null) {
            throw new IllegalArgumentException("No SingleLogoutService available for " + idp.getId());
        }

        // 3. create LogoutRequest
        LogoutRequest logoutRequest = new LogoutRequestBuilder().buildObject();
        logoutRequest.setID("_" + UUID_GENERATOR.generate().toString());
        logoutRequest.setDestination(idp.getSLOLocation(idp.getBindingType()).getLocation());

        DateTime now = new DateTime();
        logoutRequest.setIssueInstant(now);
        logoutRequest.setNotOnOrAfter(now.plusMinutes(5));

        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue(spEntityID);
        logoutRequest.setIssuer(issuer);

        NameID nameID = new NameIDBuilder().buildObject();
        nameID.setFormat((String) consumer.getJwtClaims().getClaim(JWT_CLAIM_NAMEID_FORMAT));
        nameID.setValue((String) consumer.getJwtClaims().getClaim(JWT_CLAIM_NAMEID_VALUE));
        logoutRequest.setNameID(nameID);

        SessionIndex sessionIndex = new SessionIndexBuilder().buildObject();
        sessionIndex.setSessionIndex((String) consumer.getJwtClaims().getClaim(JWT_CLAIM_SESSIONINDEX));
        logoutRequest.getSessionIndexes().add(sessionIndex);

        SAML2RequestTO requestTO = new SAML2RequestTO();
        requestTO.setIdpServiceAddress(logoutRequest.getDestination());
        requestTO.setBindingType(idp.getBindingType());
        try {
            // 3. generate relay state as JWT
            Map<String, Object> claims = new HashMap<>();
            claims.put(JWT_CLAIM_IDP_DEFLATE, idp.isUseDeflateEncoding());
            Triple<String, String, Date> relayState =
                    accessTokenDataBinder.generateJWT(logoutRequest.getID(), JWT_RELAY_STATE_DURATION, claims);
            requestTO.setRelayState(relayState.getMiddle());

            // 4. sign and encode AuthnRequest
            switch (idp.getBindingType()) {
                case REDIRECT:
                    requestTO.setContent(saml2rw.encode(logoutRequest, true));
                    requestTO.setSignAlg(saml2rw.getSigAlgo());
                    requestTO.setSignature(saml2rw.sign(requestTO.getContent(), requestTO.getRelayState()));
                    break;

                case POST:
                default:
                    saml2rw.sign(logoutRequest);
                    requestTO.setContent(saml2rw.encode(logoutRequest, idp.isUseDeflateEncoding()));
            }
        } catch (Exception e) {
            LOG.error("While generating LogoutRequest", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        return requestTO;
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + StandardEntitlement.ANONYMOUS + "'))")
    public void validateLogoutResponse(final String accessToken, final SAML2ReceivedResponseTO response) {
        check();

        // 1. fetch the current JWT used for Syncope authentication
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(accessToken);
        if (!consumer.verifySignatureWith(jwsSignatureCerifier)) {
            throw new IllegalArgumentException("Invalid signature found in Access Token");
        }

        // 2. extract raw SAML response and relay state
        JwsJwtCompactConsumer relayState = null;
        Boolean useDeflateEncoding = false;
        if (StringUtils.isNotBlank(response.getRelayState())) {
            // first checks for the provided relay state, if available
            relayState = new JwsJwtCompactConsumer(response.getRelayState());
            if (!relayState.verifySignatureWith(jwsSignatureCerifier)) {
                throw new IllegalArgumentException("Invalid signature found in Relay State");
            }
            useDeflateEncoding = Boolean.valueOf(
                    relayState.getJwtClaims().getClaim(JWT_CLAIM_IDP_DEFLATE).toString());
        }

        // 3. parse the provided SAML response
        LogoutResponse logoutResponse;
        try {
            XMLObject responseObject = saml2rw.read(
                    response.getBindingType(), useDeflateEncoding, response.getSamlResponse());
            if (!(responseObject instanceof LogoutResponse)) {
                throw new IllegalArgumentException("Expected " + LogoutResponse.class.getName()
                        + ", got " + responseObject.getClass().getName());
            }
            logoutResponse = (LogoutResponse) responseObject;
        } catch (Exception e) {
            LOG.error("While parsing LogoutResponse", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        // 4. if relay state was available, check the SAML Reponse's InResponseTo
        if (relayState != null && !relayState.getJwtClaims().getSubject().equals(logoutResponse.getInResponseTo())) {
            throw new IllegalArgumentException("Unmatching request ID: " + logoutResponse.getInResponseTo());
        }

        // 5. finally check for the logout status
        if (StatusCode.SUCCESS.equals(logoutResponse.getStatus().getStatusCode().getValue())) {
            accessTokenDAO.delete(consumer.getJwtClaims().getTokenId());
        } else {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            if (logoutResponse.getStatus().getStatusMessage() == null) {
                sce.getElements().add(logoutResponse.getStatus().getStatusCode().getValue());
            } else {
                sce.getElements().add(logoutResponse.getStatus().getStatusMessage().getMessage());
            }
            throw sce;
        }
    }

    @Override
    protected AbstractBaseBean resolveReference(
            final Method method, final Object... args) throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }

}
