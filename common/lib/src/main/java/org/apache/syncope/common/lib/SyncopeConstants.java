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
package org.apache.syncope.common.lib;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

public final class SyncopeConstants {

    public static final String NS_PREFIX = "syncope2";

    public static final String NS = "http://syncope.apache.org/2.0";

    public static final String MASTER_DOMAIN = "Master";

    public static final String ROOT_REALM = "/";

    public static final String REALM_ANYTYPE = "REALM";

    public static final Set<String> FULL_ADMIN_REALMS = Collections.singleton("/");

    public static final String UNAUTHENTICATED = "unauthenticated";

    public static final String ENUM_VALUES_SEPARATOR = ";";

    public static final String NAME_PATTERN = "[\\p{L}\\p{gc=Mn}\\p{gc=Me}\\p{gc=Mc}\\p{Digit}\\p{gc=Pc} \\-@.]+";

    public static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "yyyy-MM-dd'T'HH:mm:ssz",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss.S", // explicitly added to import date into MySql repository
        "yyyy-MM-dd" };

    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";

    public static final String ROOT_LOGGER = "ROOT";

    public static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
            + "@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$",
            Pattern.CASE_INSENSITIVE);

    public static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    public static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

    private SyncopeConstants() {
        // private constructor for utility class
    }
}
