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
package org.apache.syncope.core.persistence.api.entity.am;

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.XmlSecAlgorithm;

public interface SAML2SPClientApp extends ClientApp {

    String getEntityId();

    void setEntityId(String id);

    String getMetadataLocation();

    void setMetadataLocation(String location);

    void setMetadataSignatureLocation(String location);

    String getMetadataSignatureLocation();

    void setSignAssertions(boolean location);

    boolean isSignAssertions();

    void setSignResponses(boolean location);

    boolean isSignResponses();

    void setEncryptionOptional(boolean location);

    boolean isEncryptionOptional();

    void setEncryptAssertions(boolean location);

    boolean isEncryptAssertions();

    void setRequiredAuthenticationContextClass(String location);

    String getRequiredAuthenticationContextClass();

    void setRequiredNameIdFormat(SAML2SPNameId location);

    SAML2SPNameId getRequiredNameIdFormat();

    void setSkewAllowance(Integer location);

    Integer getSkewAllowance();

    void setNameIdQualifier(String location);

    String getNameIdQualifier();

    Set<String> getAssertionAudiences();

    void setServiceProviderNameIdQualifier(String location);

    String getServiceProviderNameIdQualifier();

    List<XmlSecAlgorithm> getSigningSignatureAlgorithms();

    List<XmlSecAlgorithm> getSigningSignatureReferenceDigestMethods();

    List<XmlSecAlgorithm> getEncryptionDataAlgorithms();

    List<XmlSecAlgorithm> getEncryptionKeyAlgorithms();

    List<XmlSecAlgorithm> getSigningSignatureBlackListedAlgorithms();

    List<XmlSecAlgorithm> getEncryptionBlackListedAlgorithms();
}
