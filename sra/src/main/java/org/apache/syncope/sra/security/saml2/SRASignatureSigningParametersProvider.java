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
package org.apache.syncope.sra.security.saml2;

import java.util.List;
import java.util.Optional;
import org.opensaml.xmlsec.SignatureSigningConfiguration;
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.impl.BasicSignatureSigningConfiguration;
import org.pac4j.saml.config.SAML2Configuration;
import org.pac4j.saml.crypto.DefaultSignatureSigningParametersProvider;

public class SRASignatureSigningParametersProvider extends DefaultSignatureSigningParametersProvider {

    protected final SAML2Configuration configuration;

    public SRASignatureSigningParametersProvider(final SAML2Configuration configuration) {
        super(configuration);
        this.configuration = configuration;
    }

    @Override
    protected SignatureSigningConfiguration getSignatureSigningConfiguration() {
        BasicSignatureSigningConfiguration ssc =
                DefaultSecurityConfigurationBootstrap.buildDefaultSignatureSigningConfiguration();

        Optional.ofNullable(configuration.getBlackListedSignatureSigningAlgorithms()).
                ifPresent(ssc::setExcludedAlgorithms);

        Optional.ofNullable(configuration.getSignatureAlgorithms()).
                ifPresent(ssc::setSignatureAlgorithms);

        Optional.ofNullable(configuration.getSignatureCanonicalizationAlgorithm()).
                ifPresent(ssc::setSignatureCanonicalizationAlgorithm);

        Optional.ofNullable(configuration.getSignatureReferenceDigestMethods()).
                ifPresent(ssc::setSignatureReferenceDigestMethods);

        ssc.setSigningCredentials(List.of(configuration.getCredentialProvider().getCredential()));
        return ssc;
    }
}
