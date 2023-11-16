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

public enum X509PrincipalType {
    /**
     * Create principal by common name and EDIPI.
     */
    CN_EDIPI,
    /**
     * Create principal from the RFC822 type name (aka email address) in the subject alternative name field.
     * The subject alternative name field contains a list of various types of names, one type is RFC822 e-mail
     * address. This will return the first e-mail address that is found (if there are more than one).
     */
    RFC822_EMAIL,
    /**
     * Create principal by serial no.
     * Resolve the principal by the serial number with a configurable <strong>radix</strong>, ranging from 2 to 36.
     * If {@code radix} is {@code 16}, then the serial number could be filled with leading zeros to even the number of
     * digits.
     */
    SERIAL_NO,
    /**
     * Create principal by serial no and DN.
     */
    SERIAL_NO_DN,
    /**
     * Create principal by subject.
     * Resolve the principal by extracting one or more attribute values from the
     * certificate subject DN and combining them with intervening delimiters.
     */
    SUBJECT,
    /**
     * Create principal by subject alternative name.
     * Resolve the principal by the subject alternative name extension. (type: otherName)
     */
    SUBJECT_ALT_NAME,
    /**
     * Create principal by subject DN.
     */
    SUBJECT_DN;

}
