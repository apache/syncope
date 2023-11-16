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

public enum X509RevocationCheckerType {
    /**
     * No revocation is performed.
     */
    NONE,
    /**
     * The CRL URI(s) mentioned in the certificate cRLDistributionPoints extension field.
     *
     * Caches are available to prevent excessive IO against CRL endpoints. CRL data fetched if does not exist in the
     * cache or if it is expired
     */
    CRL,
    /**
     * A CRL hosted at a fixed location. The CRL is fetched at periodic intervals and cached.
     */
    RESOURCE;

}
