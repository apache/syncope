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

import com.google.common.net.HttpHeaders;
import javax.xml.transform.TransformerException;
import org.apache.syncope.sra.SecurityConfig;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.NameIDType;
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
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlUtils;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(Saml2MetadataEndpoint.METADATA_URL)
@ConditionalOnProperty(name = SecurityConfig.AM_TYPE, havingValue = "SAML2")
public class Saml2MetadataEndpoint {

    public static final String METADATA_URL = "/saml2/service-provider-metadata/SAML2";

    private final Credential credential;

    public Saml2MetadataEndpoint(final Credential credential) {
        this.credential = credential;
    }

    @GetMapping(produces = { MediaType.APPLICATION_XML_VALUE })
    @ResponseBody
    public Mono<ResponseEntity<String>> metadata(final ServerHttpRequest request)
            throws org.opensaml.security.SecurityException, TransformerException {

        String spEntityID = UriComponentsBuilder.fromHttpRequest(request).build().toUriString();

        EntityDescriptor spEntityDescriptor = new EntityDescriptorBuilder().buildObject();
        spEntityDescriptor.setEntityID(spEntityID);

        SPSSODescriptor spSSODescriptor = new SPSSODescriptorBuilder().buildObject();
        spSSODescriptor.setWantAssertionsSigned(true);
        spSSODescriptor.setAuthnRequestsSigned(true);
        spSSODescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);

        X509KeyInfoGeneratorFactory keyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();
        keyInfoGeneratorFactory.setEmitEntityCertificate(true);
        KeyInfoGenerator keyInfoGenerator = keyInfoGeneratorFactory.newInstance();
        keyInfoGenerator.generate(credential);

        KeyDescriptor keyDescriptor = new KeyDescriptorBuilder().buildObject();
        keyDescriptor.setKeyInfo(keyInfoGenerator.generate(credential));
        spSSODescriptor.getKeyDescriptors().add(keyDescriptor);

        NameIDFormat nameIDFormat = new NameIDFormatBuilder().buildObject();
        nameIDFormat.setFormat(NameIDType.PERSISTENT);
        spSSODescriptor.getNameIDFormats().add(nameIDFormat);
        nameIDFormat = new NameIDFormatBuilder().buildObject();
        nameIDFormat.setFormat(NameIDType.TRANSIENT);
        spSSODescriptor.getNameIDFormats().add(nameIDFormat);
        nameIDFormat = new NameIDFormatBuilder().buildObject();
        nameIDFormat.setFormat(NameIDType.EMAIL);
        spSSODescriptor.getNameIDFormats().add(nameIDFormat);

        String loginAssertionConsumerURL = UriComponentsBuilder.fromHttpRequest(request).
                replacePath(Saml2WebSsoAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI.
                        replace("{registrationId}", "SAML2")).build().toUriString();
        String logoutAssertionConsumerURL = loginAssertionConsumerURL.replace("/login/", "/logout/");
        for (Saml2MessageBinding bindingType : Saml2MessageBinding.values()) {
            AssertionConsumerService assertionConsumerService = new AssertionConsumerServiceBuilder().buildObject();
            assertionConsumerService.setIndex(bindingType.ordinal());
            assertionConsumerService.setBinding(bindingType.getUrn());
            assertionConsumerService.setLocation(loginAssertionConsumerURL);
            spSSODescriptor.getAssertionConsumerServices().add(assertionConsumerService);
            spEntityDescriptor.getRoleDescriptors().add(spSSODescriptor);

            SingleLogoutService singleLogoutService = new SingleLogoutServiceBuilder().buildObject();
            singleLogoutService.setBinding(bindingType.getUrn());
            singleLogoutService.setLocation(logoutAssertionConsumerURL);
            singleLogoutService.setResponseLocation(logoutAssertionConsumerURL);
            spSSODescriptor.getSingleLogoutServices().add(singleLogoutService);
        }

        spEntityDescriptor.getRoleDescriptors().add(spSSODescriptor);

        OpenSamlUtils.sign(spEntityDescriptor, credential);

        return Mono.just(ResponseEntity.ok().
                header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE).
                body(OpenSamlUtils.write(spEntityDescriptor)));
    }
}
