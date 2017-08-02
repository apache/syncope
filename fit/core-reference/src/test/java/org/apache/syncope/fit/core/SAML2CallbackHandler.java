/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.fit.core;

import java.io.IOException;
import java.util.Collections;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.AuthenticationStatementBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.SubjectConfirmationDataBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

/**
 * A Callback Handler implementation for a SAML 2 assertion.
 */
public class SAML2CallbackHandler implements CallbackHandler {
    private String subjectName = "uid=joe,ou=people,ou=saml-demo,o=example.com";
    private String subjectQualifier = "www.example.com";
    private String issuer;
    private ConditionsBean conditions;
    private SubjectConfirmationDataBean subjectConfirmationData;
    private String subjectConfirmationMethod = SAML2Constants.CONF_BEARER;

    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                callback.setSamlVersion(Version.SAML_20);
                callback.setIssuer(issuer);
                if (conditions != null) {
                    callback.setConditions(conditions);
                }

                SubjectBean subjectBean =
                    new SubjectBean(
                        subjectName, subjectQualifier, subjectConfirmationMethod
                    );
                subjectBean.setSubjectConfirmationData(subjectConfirmationData);
                callback.setSubject(subjectBean);
                AuthenticationStatementBean authBean = new AuthenticationStatementBean();
                authBean.setAuthenticationMethod("Password");
                callback.setAuthenticationStatementData(Collections.singletonList(authBean));
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectQualifier() {
        return subjectQualifier;
    }

    public void setSubjectQualifier(String subjectQualifier) {
        this.subjectQualifier = subjectQualifier;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public ConditionsBean getConditions() {
        return conditions;
    }

    public void setConditions(ConditionsBean conditions) {
        this.conditions = conditions;
    }

    public SubjectConfirmationDataBean getSubjectConfirmationData() {
        return subjectConfirmationData;
    }

    public void setSubjectConfirmationData(SubjectConfirmationDataBean subjectConfirmationData) {
        this.subjectConfirmationData = subjectConfirmationData;
    }

    public String getSubjectConfirmationMethod() {
        return subjectConfirmationMethod;
    }

    public void setSubjectConfirmationMethod(String subjectConfirmationMethod) {
        this.subjectConfirmationMethod = subjectConfirmationMethod;
    }
}
