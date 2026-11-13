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
package org.apache.syncope.client.enduser.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.enduser.pages.SelfRegistration;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.jackson.SyncopeJsonMapper;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.jupiter.api.Test;

class EnduserCodeConsumerResourceTest {

    private static final SyncopeJsonMapper MAPPER = new SyncopeJsonMapper();

    @Test
    void getSelfRegInfoSerializesNewUser() {
        UserTO newUser = new UserTO();
        newUser.setUsername("john");
        newUser.getPlainAttrs().add(new Attr.Builder("email").value("john@example.com").build());

        Pair<Class<? extends WebPage>, PageParameters> selfRegInfo =
                new EnduserCodeConsumerResource().getSelfRegInfo(newUser);

        assertEquals(SelfRegistration.class, selfRegInfo.getLeft());

        String serialized = selfRegInfo.getRight().
                get(SelfRegistration.NEW_USER_PARAM).toOptionalString();
        UserTO deserialized = MAPPER.readValue(serialized, UserTO.class);
        assertEquals("john", deserialized.getUsername());
        assertEquals(
                "john@example.com",
                deserialized.getPlainAttr("email").orElseThrow().getValues().getFirst());
    }
}
