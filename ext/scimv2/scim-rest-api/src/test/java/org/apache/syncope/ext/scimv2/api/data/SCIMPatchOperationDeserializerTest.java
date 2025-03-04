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
package org.apache.syncope.ext.scimv2.api.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import org.apache.syncope.ext.scimv2.api.type.PatchOp;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.junit.jupiter.api.Test;

public class SCIMPatchOperationDeserializerTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().
            findAndAddModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();

    @Test
    public void addMember() throws JsonProcessingException {
        String input =
                "{ "
                + "\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "     \"Operations\":["
                + "       {"
                + "        \"op\":\"add\","
                + "        \"path\":\"members\","
                + "        \"value\":["
                + "         {"
                + "           \"display\": \"Babs Jensen\","
                + "           \"$ref\": \"https://example.com/v2/Users/2819c223...413861904646\","
                + "           \"value\": \"2819c223-7f76-453a-919d-413861904646\""
                + "         }"
                + "        ]"
                + "       }"
                + "     ]"
                + "   }";

        SCIMPatchOp scimPatchOp = MAPPER.readValue(input, SCIMPatchOp.class);
        assertNotNull(scimPatchOp);
        assertEquals(List.of(Resource.PatchOp.schema()), scimPatchOp.getSchemas());
        assertEquals(1, scimPatchOp.getOperations().size());

        SCIMPatchOperation op = scimPatchOp.getOperations().getFirst();
        assertNotNull(op);
        assertEquals(PatchOp.add, op.getOp());
        assertEquals("members", op.getPath().getAttribute());

        assertEquals(1, op.getValue().size());

        Member member = (Member) op.getValue().getFirst();
        assertEquals("Babs Jensen", member.getDisplay());
        assertEquals("https://example.com/v2/Users/2819c223...413861904646", member.getRef());
        assertEquals("2819c223-7f76-453a-919d-413861904646", member.getValue());
    }

    @Test
    public void removeMembers() throws JsonProcessingException {
        String input =
                "{"
                + "     \"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "     \"Operations\":[{"
                + "       \"op\":\"remove\","
                + "       \"path\":\"members[value eq \\\"2819c223-7f76-...413861904646\\\"]\""
                + "     }]"
                + "   }";

        SCIMPatchOp scimPatchOp = MAPPER.readValue(input, SCIMPatchOp.class);
        assertNotNull(scimPatchOp);
        assertEquals(List.of(Resource.PatchOp.schema()), scimPatchOp.getSchemas());
        assertEquals(1, scimPatchOp.getOperations().size());

        SCIMPatchOperation op = scimPatchOp.getOperations().getFirst();
        assertNotNull(op);
        assertEquals(PatchOp.remove, op.getOp());
        assertEquals("members", op.getPath().getAttribute());
        assertEquals("value eq \"2819c223-7f76-...413861904646\"", op.getPath().getFilter());
    }

    @Test
    public void removeAndAddMembers() throws JsonProcessingException {
        String input =
                "{ \"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "     \"Operations\": ["
                + "       {"
                + "         \"op\":\"remove\","
                + "         \"path\":"
                + "           \"members[value eq \\\"2819c223...919d-413861904646\\\"]\""
                + "       },"
                + "       {"
                + "         \"op\":\"add\","
                + "         \"path\":\"members\","
                + "         \"value\": ["
                + "           {"
                + "            \"display\": \"Babs Jensen\","
                + "            \"$ref\":\"https://example.com/v2/Users/2819c223...413861904646\","
                + "            \"value\": \"2819c223-7f76-453a-919d-413861904646\""
                + "           },"
                + "           {"
                + "             \"display\": \"James Smith\","
                + "             \"$ref\":\"https://example.com/v2/Users/08e1d05d...473d93df9210\","
                + "             \"value\": \"08e1d05d...473d93df9210\""
                + "           }"
                + "         ]"
                + "       }"
                + "     ]"
                + "   }";

        SCIMPatchOp scimPatchOp = MAPPER.readValue(input, SCIMPatchOp.class);
        assertNotNull(scimPatchOp);
        assertEquals(List.of(Resource.PatchOp.schema()), scimPatchOp.getSchemas());
        assertEquals(2, scimPatchOp.getOperations().size());

        SCIMPatchOperation op = scimPatchOp.getOperations().getFirst();
        assertNotNull(op);
        assertEquals(PatchOp.remove, op.getOp());
        assertEquals("members", op.getPath().getAttribute());
        assertEquals("value eq \"2819c223...919d-413861904646\"", op.getPath().getFilter());

        op = scimPatchOp.getOperations().get(1);
        assertNotNull(op);
        assertEquals(PatchOp.add, op.getOp());
        assertEquals("members", op.getPath().getAttribute());

        assertEquals(2, op.getValue().size());

        Member member = (Member) op.getValue().getFirst();
        assertEquals("Babs Jensen", member.getDisplay());
        assertEquals("https://example.com/v2/Users/2819c223...413861904646", member.getRef());
        assertEquals("2819c223-7f76-453a-919d-413861904646", member.getValue());

        member = (Member) op.getValue().get(1);
        assertEquals("James Smith", member.getDisplay());
        assertEquals("https://example.com/v2/Users/08e1d05d...473d93df9210", member.getRef());
        assertEquals("08e1d05d...473d93df9210", member.getValue());
    }

    @Test
    public void addAttributes() throws JsonProcessingException {
        String input =
                "{"
                + "  \"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "  \"Operations\": ["
                + "    {"
                + "      \"op\": \"add\","
                + "      \"value\": {"
                + "        \"emails\": ["
                + "          {"
                + "            \"value\": \"babs@jensen.org\","
                + "            \"type\": \"home\""
                + "          }"
                + "        ],"
                + "        \"nickName\": \"Babs\""
                + "      }"
                + "    }"
                + "  ]"
                + "}";

        SCIMPatchOp scimPatchOp = MAPPER.readValue(input, SCIMPatchOp.class);
        assertNotNull(scimPatchOp);
        assertEquals(List.of(Resource.PatchOp.schema()), scimPatchOp.getSchemas());
        assertEquals(1, scimPatchOp.getOperations().size());

        SCIMPatchOperation op = scimPatchOp.getOperations().getFirst();
        assertNotNull(op);
        assertEquals(PatchOp.add, op.getOp());
        assertNull(op.getPath());

        assertEquals(1, op.getValue().size());

        SCIMUser user = (SCIMUser) op.getValue().getFirst();
        assertEquals("Babs", user.getNickName());

        assertEquals(1, user.getEmails().size());

        SCIMComplexValue email = user.getEmails().getFirst();
        assertEquals("home", email.getType());
        assertEquals("babs@jensen.org", email.getValue());
    }

    @Test
    public void replaceMembers() throws JsonProcessingException {
        String input =
                " {"
                + "     \"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "     \"Operations\": [{"
                + "       \"op\":\"replace\","
                + "       \"path\":\"members\","
                + "       \"value\":["
                + "         {"
                + "           \"display\": \"Babs Jensen\","
                + "           \"$ref\": \"https://example.com/v2/Users/2819c223...413861904646\","
                + "           \"value\": \"2819c223...413861904646\""
                + "         },"
                + "         {"
                + "           \"display\": \"James Smith\","
                + "           \"$ref\":\"https://example.com/v2/Users/08e1d05d...473d93df9210\","
                + "           \"value\": \"08e1d05d...473d93df9210\""
                + "         }"
                + "       ]"
                + "     }]"
                + "   }";

        SCIMPatchOp scimPatchOp = MAPPER.readValue(input, SCIMPatchOp.class);
        assertNotNull(scimPatchOp);
        assertEquals(List.of(Resource.PatchOp.schema()), scimPatchOp.getSchemas());
        assertEquals(1, scimPatchOp.getOperations().size());

        SCIMPatchOperation op = scimPatchOp.getOperations().getFirst();
        assertNotNull(op);
        assertEquals(PatchOp.replace, op.getOp());
        assertEquals("members", op.getPath().getAttribute());

        assertEquals(2, op.getValue().size());

        Member member = (Member) op.getValue().getFirst();
        assertEquals("Babs Jensen", member.getDisplay());
        assertEquals("https://example.com/v2/Users/2819c223...413861904646", member.getRef());
        assertEquals("2819c223...413861904646", member.getValue());

        member = (Member) op.getValue().get(1);
        assertEquals("James Smith", member.getDisplay());
        assertEquals("https://example.com/v2/Users/08e1d05d...473d93df9210", member.getRef());
        assertEquals("08e1d05d...473d93df9210", member.getValue());
    }

    @Test
    public void replaceAttribute() throws JsonProcessingException {
        String input =
                "{"
                + "     \"schemas\": [\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "     \"Operations\": [{"
                + "       \"op\":\"replace\","
                + "       \"path\":\"addresses[type eq \\\"work\\\"]\","
                + "       \"value\":"
                + "       {"
                + "         \"type\": \"work\","
                + "         \"streetAddress\": \"911 Universal City Plaza\","
                + "         \"locality\": \"Hollywood\","
                + "         \"region\": \"CA\","
                + "         \"postalCode\": \"91608\","
                + "         \"country\": \"US\","
                + "         \"formatted\":\"911 Universal City Plaza\\nHollywood, CA 91608 US\","
                + "         \"primary\": true"
                + "       }"
                + "     }]"
                + "   }";

        SCIMPatchOp scimPatchOp = MAPPER.readValue(input, SCIMPatchOp.class);
        assertNotNull(scimPatchOp);
        assertEquals(List.of(Resource.PatchOp.schema()), scimPatchOp.getSchemas());
        assertEquals(1, scimPatchOp.getOperations().size());

        SCIMPatchOperation op = scimPatchOp.getOperations().getFirst();
        assertNotNull(op);
        assertEquals(PatchOp.replace, op.getOp());
        assertEquals("addresses", op.getPath().getAttribute());
        assertEquals("type eq \"work\"", op.getPath().getFilter());

        assertEquals(1, op.getValue().size());

        SCIMUser user = (SCIMUser) op.getValue().getFirst();

        assertEquals(1, user.getAddresses().size());

        SCIMUserAddress address = user.getAddresses().getFirst();
        assertEquals("work", address.getType());
        assertTrue(address.isPrimary());
    }

    @Test
    public void replaceAttributeValue() throws JsonProcessingException {
        String input =
                "{"
                + "     \"schemas\": [\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "     \"Operations\": [{"
                + "       \"op\":\"replace\","
                + "       \"path\":\"addresses[type eq \\\"work\\\"].streetAddress\","
                + "       \"value\":\"1010 Broadway Ave\""
                + "     }]"
                + "   }";

        SCIMPatchOp scimPatchOp = MAPPER.readValue(input, SCIMPatchOp.class);
        assertNotNull(scimPatchOp);
        assertEquals(List.of(Resource.PatchOp.schema()), scimPatchOp.getSchemas());
        assertEquals(1, scimPatchOp.getOperations().size());

        SCIMPatchOperation op = scimPatchOp.getOperations().getFirst();
        assertNotNull(op);
        assertEquals(PatchOp.replace, op.getOp());
        assertEquals("addresses", op.getPath().getAttribute());
        assertEquals("type eq \"work\"", op.getPath().getFilter());
        assertEquals("streetAddress", op.getPath().getSub());

        assertEquals(1, op.getValue().size());

        String value = (String) op.getValue().getFirst();
        assertEquals("1010 Broadway Ave", value);
    }

    @Test
    public void addAttributeValue() throws JsonProcessingException {
        String input =
                "{"
                + "  \"schemas\": [\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                + "  \"Operations\": [{"
                + "      \"op\": \"Add\","
                + "      \"path\": \"externalId\","
                + "      \"value\": \"da91cb5d-addb-424f-aa41-799fda8658c3\""
                + "    }"
                + "  ]"
                + "}";

        SCIMPatchOp scimPatchOp = MAPPER.readValue(input, SCIMPatchOp.class);
        assertNotNull(scimPatchOp);
        assertEquals(List.of(Resource.PatchOp.schema()), scimPatchOp.getSchemas());
        assertEquals(1, scimPatchOp.getOperations().size());

        SCIMPatchOperation op = scimPatchOp.getOperations().getFirst();
        assertNotNull(op);
        assertEquals(PatchOp.add, op.getOp());
        assertEquals("externalId", op.getPath().getAttribute());

        assertEquals(1, op.getValue().size());

        String value = (String) op.getValue().getFirst();
        assertEquals("da91cb5d-addb-424f-aa41-799fda8658c3", value);
    }
}
