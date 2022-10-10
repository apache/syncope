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
package org.apache.syncope.core.spring.security;

import java.util.Optional;
import org.apache.syncope.common.lib.types.AnyTypeKind;

public class DelegatedAdministrationException extends RuntimeException {

    private static final long serialVersionUID = 7540587364235915081L;

    public DelegatedAdministrationException(final String realm, final String type, final String key) {
        super("Missing entitlement or realm administration under " + realm + " for "
                + Optional.ofNullable(key).map(s -> type + ' ' + s).orElseGet(() -> "new " + type));
    }

    public DelegatedAdministrationException(final AnyTypeKind type, final String key) {
        super("The requested UPDATE would alter the set of dynamic realms for " + type + ' ' + key);
    }
}
