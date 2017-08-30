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
package org.apache.syncope.common.lib.types;

public enum SignatureAlgorithm {

    RSA_SHA1("http://www.w3.org/2000/09/xmldsig#rsa-sha1"),
    RSA_SHA224("http://www.w3.org/2001/04/xmldsig-more#rsa-sha224"),
    RSA_SHA256("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"),
    RSA_SHA384("http://www.w3.org/2001/04/xmldsig-more#rsa-sha384"),
    RSA_SHA512("http://www.w3.org/2001/04/xmldsig-more#rsa-sha512"),

    RSA_SHA1_MGF1("http://www.w3.org/2007/05/xmldsig-more#sha1-rsa-MGF1"),
    RSA_SHA224_MGF1("http://www.w3.org/2007/05/xmldsig-more#sha224-rsa-MGF1"),
    RSA_SHA256_MGF1("http://www.w3.org/2007/05/xmldsig-more#sha256-rsa-MGF1"),
    RSA_SHA384_MGF1("http://www.w3.org/2007/05/xmldsig-more#sha384-rsa-MGF1"),
    RSA_SHA512_MGF1("http://www.w3.org/2007/05/xmldsig-more#sha512-rsa-MGF1"),

    EC_SHA1("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1"),
    EC_SHA224("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha224"),
    EC_SHA256("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256"),
    EC_SHA384("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384"),
    EC_SHA512("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512"),

    HMAC_SHA1("http://www.w3.org/2000/09/xmldsig#hmac-sha1"),
    HMAC_SHA224("http://www.w3.org/2001/04/xmldsig-more#hmac-sha224"),
    HMAC_SHA256("http://www.w3.org/2001/04/xmldsig-more#hmac-sha256"),
    HMAC_SHA384("http://www.w3.org/2001/04/xmldsig-more#hmac-sha384"),
    HMAC_SHA512("http://www.w3.org/2001/04/xmldsig-more#hmac-sha512"),

    DSA_SHA1("http://www.w3.org/2000/09/xmldsig#dsa-sha1");

    private final String algorithm;

    SignatureAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }

}
