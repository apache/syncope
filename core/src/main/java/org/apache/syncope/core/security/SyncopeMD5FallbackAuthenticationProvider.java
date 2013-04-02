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
package org.apache.syncope.core.security;

import org.apache.syncope.common.types.CipherAlgorithm;
import org.jasypt.commons.CommonUtils;
import org.jasypt.digest.StandardStringDigester;

/**
 * Extend standard authentication by checking passwords using MD5 (not supported anymore as per SYNCOPE-51).
 */
public class SyncopeMD5FallbackAuthenticationProvider extends SyncopeAuthenticationProvider {

    @Override
    protected boolean authenticate(final String password, final CipherAlgorithm cipherAlgorithm,
            final String digestedPassword) {

        boolean authenticated = super.authenticate(password, cipherAlgorithm, digestedPassword);
        // if "normal" authentication fails and cipher is SMD5, we're probably handling an "old" MD5 password
        if (!authenticated && CipherAlgorithm.SMD5 == cipherAlgorithm) {
            StandardStringDigester digester = new StandardStringDigester();
            digester.setAlgorithm("MD5");
            digester.setIterations(1);
            digester.setSaltSizeBytes(0);
            digester.setStringOutputType(CommonUtils.STRING_OUTPUT_TYPE_HEXADECIMAL);

            authenticated = digester.matches(password, digestedPassword);
        }
        return authenticated;
    }
}
