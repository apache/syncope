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
package org.apache.syncope.client.cli.util;

import org.jasypt.util.text.BasicTextEncryptor;

public final class JasyptUtils {

    private static final String JASYPT_KEY = "Ka9s8yadaisj9mud87ssdaifansy";

    private final BasicTextEncryptor textEncryptor;

    private static JasyptUtils JASYPT_UTILS;

    public static JasyptUtils get() {
        if (JASYPT_UTILS == null) {
            JASYPT_UTILS = new JasyptUtils();
        }
        return JASYPT_UTILS;
    }

    private JasyptUtils() {
        textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(JASYPT_KEY);
    }

    public String encrypt(final String password) {
        return textEncryptor.encrypt(password);
    }

    public String decrypt(final String encryptedString) {
        return textEncryptor.decrypt(encryptedString);
    }
}
