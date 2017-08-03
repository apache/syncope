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
package org.apache.syncope.fit.core;

import java.util.UUID;
import org.joda.time.DateTime;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml.saml2.core.impl.ResponseBuilder;
import org.opensaml.saml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml.saml2.core.impl.StatusMessageBuilder;

/**
 * A (basic) set of utility methods to construct SAML 2.0 Protocol Response statements.
 */
public final class SAML2PResponseComponentBuilder {

    private static SAMLObjectBuilder<Response> responseBuilder;

    private static SAMLObjectBuilder<Issuer> issuerBuilder;

    private static SAMLObjectBuilder<Status> statusBuilder;

    private static SAMLObjectBuilder<StatusCode> statusCodeBuilder;

    private static SAMLObjectBuilder<StatusMessage> statusMessageBuilder;

    private static SAMLObjectBuilder<AuthnContextClassRef> authnContextClassRefBuilder;

    public static Response createSAMLResponse(final String inResponseTo, final String issuer, final Status status) {
        if (responseBuilder == null) {
            responseBuilder = new ResponseBuilder();
        }
        Response response = responseBuilder.buildObject();

        response.setID(UUID.randomUUID().toString());
        response.setIssueInstant(new DateTime());
        response.setInResponseTo(inResponseTo);
        response.setIssuer(createIssuer(issuer));
        response.setStatus(status);
        response.setVersion(SAMLVersion.VERSION_20);

        return response;
    }

    public static Issuer createIssuer(final String issuerValue) {
        if (issuerBuilder == null) {
            issuerBuilder = new IssuerBuilder();
        }
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(issuerValue);

        return issuer;
    }

    public static Status createStatus(final String statusCodeValue, final String statusMessage) {
        if (statusBuilder == null) {
            statusBuilder = new StatusBuilder();
        }
        if (statusCodeBuilder == null) {
            statusCodeBuilder = new StatusCodeBuilder();
        }
        if (statusMessageBuilder == null) {
            statusMessageBuilder = new StatusMessageBuilder();
        }

        Status status = statusBuilder.buildObject();

        StatusCode statusCode = statusCodeBuilder.buildObject();
        statusCode.setValue(statusCodeValue);
        status.setStatusCode(statusCode);

        if (statusMessage != null) {
            StatusMessage statusMessageObject = statusMessageBuilder.buildObject();
            statusMessageObject.setMessage(statusMessage);
            status.setStatusMessage(statusMessageObject);
        }

        return status;
    }

    public static AuthnContextClassRef createAuthnContextClassRef(final String newAuthnContextClassRef) {
        if (authnContextClassRefBuilder == null) {
            authnContextClassRefBuilder = new AuthnContextClassRefBuilder();
        }

        AuthnContextClassRef authnContextClassRef = authnContextClassRefBuilder.buildObject();
        authnContextClassRef.setAuthnContextClassRef(newAuthnContextClassRef);

        return authnContextClassRef;
    }

    private SAML2PResponseComponentBuilder() {
        // private constructor for static utility class
    }
}
