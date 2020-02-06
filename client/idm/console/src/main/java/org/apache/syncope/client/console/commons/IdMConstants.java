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
package org.apache.syncope.client.console.commons;

public final class IdMConstants {

    public static final String PREF_RECONCILIATION_PAGINATOR_ROWS = "reconciliation.paginator.rows";

    public static final String PREF_RESOURCE_STATUS_PAGINATOR_ROWS = "resource.status.paginator.rows";

    public static final String PREF_RESOURCES_PAGINATOR_ROWS = "resources.paginator.rows";

    public static final String PREF_REMEDIATION_PAGINATOR_ROWS = "remediation.paginator.rows";

    /**
     * ConnId's GuardedString is not in the classpath.
     */
    public static final String GUARDED_STRING = "org.identityconnectors.common.security.GuardedString";

    /**
     * ConnId's GuardedByteArray is not in the classpath.
     */
    public static final String GUARDED_BYTE_ARRAY = "org.identityconnectors.common.security.GuardedByteArray";

    private IdMConstants() {
        // private constructor for static utility class
    }
}
