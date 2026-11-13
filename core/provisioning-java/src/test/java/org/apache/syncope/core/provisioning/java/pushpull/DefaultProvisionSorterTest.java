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
package org.apache.syncope.core.provisioning.java.pushpull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.junit.jupiter.api.Test;

class DefaultProvisionSorterTest {

    private static Provision provision(final String anyType) {
        Provision provision = new Provision();
        provision.setAnyType(anyType);
        return provision;
    }

    @Test
    void userBeforeGroup() {
        assertTrue(new DefaultProvisionSorter().
                compare(provision(AnyTypeKind.USER.name()), provision(AnyTypeKind.GROUP.name())) < 0);
    }

    @Test
    void groupAfterUser() {
        assertTrue(new DefaultProvisionSorter().
                compare(provision(AnyTypeKind.GROUP.name()), provision(AnyTypeKind.USER.name())) > 0);
    }

    @Test
    void userBeforeAnyObject() {
        assertTrue(new DefaultProvisionSorter().
                compare(provision(AnyTypeKind.USER.name()), provision(AnyTypeKind.ANY_OBJECT.name())) < 0);
    }

    @Test
    void anyObjectAfterUser() {
        assertTrue(new DefaultProvisionSorter().
                compare(provision(AnyTypeKind.ANY_OBJECT.name()), provision(AnyTypeKind.USER.name())) > 0);
    }

    @Test
    void groupBeforeAnyObject() {
        assertTrue(new DefaultProvisionSorter().
                compare(provision(AnyTypeKind.GROUP.name()), provision(AnyTypeKind.ANY_OBJECT.name())) < 0);
    }

    @Test
    void anyObjectAfterGroup() {
        assertTrue(new DefaultProvisionSorter().
                compare(provision(AnyTypeKind.ANY_OBJECT.name()), provision(AnyTypeKind.GROUP.name())) > 0);
    }

    @Test
    void sameTypeIsEqual() {
        // ANY_OBJECT is the only type that falls through to ObjectUtils.compare
        assertEquals(0, new DefaultProvisionSorter().
                compare(provision(AnyTypeKind.ANY_OBJECT.name()), provision(AnyTypeKind.ANY_OBJECT.name())));
    }
}
