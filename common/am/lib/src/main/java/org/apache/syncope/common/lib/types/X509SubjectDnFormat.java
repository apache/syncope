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

public enum X509SubjectDnFormat {
    /**
     * Denigrated result of calling certificate.getSubjectDN() method.
     * Javadocs designate this method as "denigrated" for not being portable and/or not being well defined.
     * It is what has been used by CAS for a long time so it remains the default.
     */
    DEFAULT,
    /**
     * RFC 1779 String format of Distinguished Names.
     * Calls {@code X500Principal.getName("RFC1779")} which emits a subject DN with the attribute keywords defined
     * in RFC 1779 (CN, L, ST, O, OU, C, STREET). Any other attribute type is emitted as an OID.
     */
    RFC1779,
    /**
     * RFC 2253 String format of Distinguished Names.
     * Calls {@code X500Principal.getName("RFC2253")} which emits a subject DN with the attribute keywords defined in
     * RFC 2253 (CN, L, ST, O, OU, C, STREET, DC, UID). Any other attribute type is emitted as an OID.
     */
    RFC2253,
    /**
     * Canonical String format of Distinguished Names.
     * Calls X500Principal.getName("CANONICAL" which emits a subject DN that starts with RFC 2253 and applies
     * additional canonicalizations described in the javadoc.
     */
    CANONICAL;

}
