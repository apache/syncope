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

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public final class SAML2SP4UIImplementationType {

    public static final String IDP_ACTIONS = "IDP_ACTIONS";

    public static final String REQUESTED_AUTHN_CONTEXT_PROVIDER = "REQUESTED_AUTHN_CONTEXT_PROVIDER";

    private static final Map<String, String> VALUES = Map.ofEntries(
            Pair.of(IDP_ACTIONS,
                    "org.apache.syncope.core.provisioning.api.SAML2SP4UIIdPActions"),
            Pair.of(REQUESTED_AUTHN_CONTEXT_PROVIDER,
                    "org.apache.syncope.core.provisioning.api.RequestedAuthnContextProvider"));

    public static Map<String, String> values() {
        return VALUES;
    }

    private SAML2SP4UIImplementationType() {
        // private constructor for static utility class
    }
}
