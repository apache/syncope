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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.junit.jupiter.api.Test;

public class AnyOperationsTest {

    @Test
    public void mindiff() {
        AnyObjectTO oldOne = new AnyObjectTO();
        oldOne.setName("name");
        oldOne.getPlainAttrs().add(new Attr.Builder("plain").value("oldValue").build());
        oldOne.getPlainAttrs().add(new Attr.Builder("encrypted").value("oldValue").build());

        AnyObjectTO newOne = new AnyObjectTO();
        newOne.setName("name");
        newOne.getPlainAttrs().add(new Attr.Builder("plain").value("newValue").build());
        newOne.getPlainAttrs().add(new Attr.Builder("encrypted").value("oldValue").build());

        AnyObjectUR diff = AnyOperations.diff(newOne, oldOne, true);
        assertEquals(1, diff.getPlainAttrs().size());

        AttrPatch patch = diff.getPlainAttrs().iterator().next();
        assertEquals(PatchOperation.ADD_REPLACE, patch.getOperation());
        assertEquals("plain", patch.getAttr().getSchema());
    }
}
