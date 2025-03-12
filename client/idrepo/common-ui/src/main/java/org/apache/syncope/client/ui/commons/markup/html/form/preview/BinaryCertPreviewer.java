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
package org.apache.syncope.client.ui.commons.markup.html.form.preview;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.annotations.BinaryPreview;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

@BinaryPreview(mimeTypes = { "application/x-x509-ca-cert", "application/x-x509-user-cert", "application/pkix-cert" })
public class BinaryCertPreviewer extends BinaryPreviewer {

    private static final long serialVersionUID = -5843835939538055110L;

    public BinaryCertPreviewer(final String mimeType) {
        super(mimeType);
    }

    @Override
    public Component preview(final byte[] uploadedBytes) {
        Label commonNameLabel = new Label("certCommonName", new Model<>());
        if (uploadedBytes.length == 0) {
            LOG.info("Empty certificate");
            return commonNameLabel;
        }

        try (ByteArrayInputStream certificateStream = new ByteArrayInputStream(uploadedBytes)) {
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").
                    generateCertificate(certificateStream);

            StringBuilder commonNameBuilder = new StringBuilder("cn=");

            LdapName ldapName = new LdapName(certificate.getIssuerX500Principal().getName());

            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    commonNameBuilder.append(rdn.getValue() == null
                            ? StringUtils.EMPTY
                            : rdn.getValue().toString());
                }
            }
            commonNameLabel.setDefaultModelObject(commonNameBuilder.toString());
        } catch (Exception e) {
            LOG.error("Error evaluating certificate file", e);
            commonNameLabel.setDefaultModelObject(getString(Constants.ERROR));
        }

        return this.addOrReplace(commonNameLabel);
    }
}
