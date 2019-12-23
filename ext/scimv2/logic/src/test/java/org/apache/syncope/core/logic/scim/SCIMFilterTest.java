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
package org.apache.syncope.core.logic.scim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.scim.SCIMComplexConf;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.scim.SCIMUserNameConf;
import org.apache.syncope.common.lib.scim.types.EmailCanonicalType;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SCIMFilterTest {

    private static SearchCondVisitor VISITOR;

    @BeforeAll
    public static void setup() {
        SCIMConf conf = new SCIMConf();
        conf.setUserConf(new SCIMUserConf());
        conf.getUserConf().setTitle("title");
        conf.getUserConf().setDisplayName("cn");
        conf.getUserConf().setUserType("userType");

        conf.getUserConf().setName(new SCIMUserNameConf());
        conf.getUserConf().getName().setFamilyName("surname");

        SCIMComplexConf<EmailCanonicalType> email = new SCIMComplexConf<>();
        email.setValue("email");
        email.setType(EmailCanonicalType.work);
        conf.getUserConf().getEmails().add(email);
        email = new SCIMComplexConf<>();
        email.setValue("gmail");
        email.setType(EmailCanonicalType.home);
        conf.getUserConf().getEmails().add(email);

        VISITOR = new SearchCondVisitor(Resource.User, conf);
    }

    @Test
    public void eq() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "userName eq \"bjensen\"");
        assertNotNull(cond);
        assertTrue(cond.getLeaf(AnyCond.class).isPresent());
        assertEquals("username", cond.getLeaf(AnyCond.class).get().getSchema());
        assertEquals(AttrCond.Type.IEQ, cond.getLeaf(AnyCond.class).get().getType());
        assertEquals("bjensen", cond.getLeaf(AnyCond.class).get().getExpression());
    }

    @Test
    public void sw() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "userName sw \"J\"");
        assertNotNull(cond);
        assertTrue(cond.getLeaf(AnyCond.class).isPresent());
        assertEquals("username", cond.getLeaf(AnyCond.class).get().getSchema());
        assertEquals(AttrCond.Type.ILIKE, cond.getLeaf(AnyCond.class).get().getType());
        assertEquals("J%", cond.getLeaf(AnyCond.class).get().getExpression());

        SearchCond fqn = SearchCondConverter.convert(
                VISITOR, "urn:ietf:params:scim:schemas:core:2.0:User:userName sw \"J\"");
        assertEquals(cond, fqn);
    }

    @Test
    public void pr() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "title pr");
        assertNotNull(cond);
        assertTrue(cond.getLeaf(AttrCond.class).isPresent());
        assertEquals("title", cond.getLeaf(AttrCond.class).get().getSchema());
        assertEquals(AttrCond.Type.ISNOTNULL, cond.getLeaf(AttrCond.class).get().getType());
        assertNull(cond.getLeaf(AttrCond.class).get().getExpression());
    }

    @Test
    public void gt() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "meta.lastModified gt \"2011-05-13T04:42:34Z\"");
        assertNotNull(cond);
        assertTrue(cond.getLeaf(AnyCond.class).isPresent());
        assertEquals("lastChangeDate", cond.getLeaf(AnyCond.class).get().getSchema());
        assertEquals(AttrCond.Type.GT, cond.getLeaf(AnyCond.class).get().getType());
        assertEquals("2011-05-13T04:42:34Z", cond.getLeaf(AnyCond.class).get().getExpression());
    }

    @Test
    public void not() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "not (title pr)");
        assertNotNull(cond);
        assertTrue(cond.getLeaf(AttrCond.class).isPresent());
        assertEquals("title", cond.getLeaf(AttrCond.class).get().getSchema());
        assertEquals(AttrCond.Type.ISNULL, cond.getLeaf(AttrCond.class).get().getType());
        assertNull(cond.getLeaf(AttrCond.class).get().getExpression());
    }

    @Test
    public void and() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "title pr and userName sw \"J\"");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.AND, cond.getType());

        SearchCond left = cond.getLeft();
        assertNotNull(left);
        assertTrue(left.getLeaf(AttrCond.class).isPresent());
        assertEquals("title", left.getLeaf(AttrCond.class).get().getSchema());
        assertEquals(AttrCond.Type.ISNOTNULL, left.getLeaf(AttrCond.class).get().getType());
        assertNull(left.getLeaf(AttrCond.class).get().getExpression());

        SearchCond right = cond.getRight();
        assertNotNull(right);
        assertTrue(right.getLeaf(AnyCond.class).isPresent());
        assertEquals("username", right.getLeaf(AnyCond.class).get().getSchema());
        assertEquals(AttrCond.Type.ILIKE, right.getLeaf(AnyCond.class).get().getType());
        assertEquals("J%", right.getLeaf(AnyCond.class).get().getExpression());
    }

    @Test
    public void or() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "title pr or displayName eq \"Other\"");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.OR, cond.getType());

        SearchCond left = cond.getLeft();
        assertNotNull(left);
        assertTrue(left.getLeaf(AttrCond.class).isPresent());
        assertEquals("title", left.getLeaf(AttrCond.class).get().getSchema());
        assertEquals(AttrCond.Type.ISNOTNULL, left.getLeaf(AttrCond.class).get().getType());
        assertNull(left.getLeaf(AttrCond.class).get().getExpression());

        SearchCond right = cond.getRight();
        assertNotNull(right);
        assertTrue(right.getLeaf(AttrCond.class).isPresent());
        assertEquals("cn", right.getLeaf(AttrCond.class).get().getSchema());
        assertEquals(AttrCond.Type.IEQ, right.getLeaf(AttrCond.class).get().getType());
        assertEquals("Other", right.getLeaf(AttrCond.class).get().getExpression());
    }

    @Test
    public void type() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "userType eq \"Employee\" and (emails.type eq \"work\")");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.AND, cond.getType());

        SearchCond left = cond.getLeft();
        assertNotNull(left);
        assertTrue(left.getLeaf(AttrCond.class).isPresent());
        assertEquals("userType", left.getLeaf(AttrCond.class).get().getSchema());
        assertEquals(AttrCond.Type.IEQ, left.getLeaf(AttrCond.class).get().getType());
        assertEquals("Employee", left.getLeaf(AttrCond.class).get().getExpression());

        SearchCond right = cond.getRight();
        assertNotNull(right);
        assertTrue(right.getLeaf(AttrCond.class).isPresent());
        assertEquals("email", right.getLeaf(AttrCond.class).get().getSchema());
        assertEquals(AttrCond.Type.ISNOTNULL, right.getLeaf(AttrCond.class).get().getType());
    }

    @Test
    public void name() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "name.familyName co \"O'Malley\"");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.LEAF, cond.getType());

        AttrCond leaf = cond.getLeaf(AttrCond.class).get();
        assertNotNull(leaf);
        assertEquals("surname", leaf.getSchema());
        assertEquals(AttrCond.Type.ILIKE, leaf.getType());
        assertEquals("%O'Malley%", leaf.getExpression());
    }

    @Test
    public void emails() {
        SearchCond cond = SearchCondConverter.convert(VISITOR,
                "emails co \"example.com\" or emails.value co \"example.org\"");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.OR, cond.getType());

        SearchCond left = cond.getLeft();
        assertNotNull(left);
        assertEquals(SearchCond.Type.OR, left.getType());

        SearchCond left1 = left.getLeft();
        assertNotNull(left1);
        assertTrue(left1.getLeaf(AttrCond.class).isPresent());
        assertEquals("email", left1.getLeaf(AttrCond.class).get().getSchema());
        assertEquals(AttrCond.Type.ILIKE, left1.getLeaf(AttrCond.class).get().getType());
        assertEquals("%example.com%", left1.getLeaf(AttrCond.class).get().getExpression());

        SearchCond left2 = left.getRight();
        assertNotNull(left2);
        assertTrue(left2.getLeaf(AttrCond.class).isPresent());
        assertEquals("gmail", left2.getLeaf(AttrCond.class).get().getSchema());
        assertEquals(AttrCond.Type.ILIKE, left2.getLeaf(AttrCond.class).get().getType());
        assertEquals("%example.com%", left2.getLeaf(AttrCond.class).get().getExpression());

        SearchCond right = cond.getRight();
        assertNotNull(right);
        assertEquals(SearchCond.Type.OR, right.getType());

        SearchCond right1 = right.getLeft();
        assertNotNull(right1);
        assertTrue(right1.getLeaf(AttrCond.class).isPresent());
        assertEquals("email", right1.getLeaf(AttrCond.class).get().getSchema());
        assertEquals(AttrCond.Type.ILIKE, right1.getLeaf(AttrCond.class).get().getType());
        assertEquals("%example.org%", right1.getLeaf(AttrCond.class).get().getExpression());

        SearchCond right2 = right.getRight();
        assertNotNull(right2);
        assertTrue(right2.getLeaf(AttrCond.class).isPresent());
        assertEquals("gmail", right2.getLeaf(AttrCond.class).get().getSchema());
        assertEquals(AttrCond.Type.ILIKE, right2.getLeaf(AttrCond.class).get().getType());
        assertEquals("%example.org%", right2.getLeaf(AttrCond.class).get().getExpression());
    }
}
