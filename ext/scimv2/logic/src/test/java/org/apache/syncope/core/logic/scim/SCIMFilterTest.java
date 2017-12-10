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

import org.apache.syncope.common.lib.scim.SCIMComplexConf;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.scim.SCIMUserNameConf;
import org.apache.syncope.common.lib.scim.types.EmailCanonicalType;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
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
        assertNotNull(cond.getAnyCond());
        assertEquals("username", cond.getAnyCond().getSchema());
        assertEquals(AttributeCond.Type.IEQ, cond.getAnyCond().getType());
        assertEquals("bjensen", cond.getAnyCond().getExpression());
    }

    @Test
    public void sw() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "userName sw \"J\"");
        assertNotNull(cond);
        assertNotNull(cond.getAnyCond());
        assertEquals("username", cond.getAnyCond().getSchema());
        assertEquals(AttributeCond.Type.ILIKE, cond.getAnyCond().getType());
        assertEquals("J%", cond.getAnyCond().getExpression());

        SearchCond fqn = SearchCondConverter.convert(
                VISITOR, "urn:ietf:params:scim:schemas:core:2.0:User:userName sw \"J\"");
        assertEquals(cond, fqn);
    }

    @Test
    public void pr() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "title pr");
        assertNotNull(cond);
        assertNotNull(cond.getAttributeCond());
        assertEquals("title", cond.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.ISNOTNULL, cond.getAttributeCond().getType());
        assertNull(cond.getAttributeCond().getExpression());
    }

    @Test
    public void gt() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "meta.lastModified gt \"2011-05-13T04:42:34Z\"");
        assertNotNull(cond);
        assertNotNull(cond.getAnyCond());
        assertEquals("lastChangeDate", cond.getAnyCond().getSchema());
        assertEquals(AttributeCond.Type.GT, cond.getAnyCond().getType());
        assertEquals("2011-05-13T04:42:34Z", cond.getAnyCond().getExpression());
    }

    @Test
    public void not() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "not (title pr)");
        assertNotNull(cond);
        assertNotNull(cond.getAttributeCond());
        assertEquals("title", cond.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.ISNULL, cond.getAttributeCond().getType());
        assertNull(cond.getAttributeCond().getExpression());
    }

    @Test
    public void and() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "title pr and userName sw \"J\"");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.AND, cond.getType());

        SearchCond left = cond.getLeftSearchCond();
        assertNotNull(left);
        assertNotNull(left.getAttributeCond());
        assertEquals("title", left.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.ISNOTNULL, left.getAttributeCond().getType());
        assertNull(left.getAttributeCond().getExpression());

        SearchCond right = cond.getRightSearchCond();
        assertNotNull(right);
        assertNotNull(right.getAnyCond());
        assertEquals("username", right.getAnyCond().getSchema());
        assertEquals(AttributeCond.Type.ILIKE, right.getAnyCond().getType());
        assertEquals("J%", right.getAnyCond().getExpression());
    }

    @Test
    public void or() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "title pr or displayName eq \"Other\"");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.OR, cond.getType());

        SearchCond left = cond.getLeftSearchCond();
        assertNotNull(left);
        assertNotNull(left.getAttributeCond());
        assertEquals("title", left.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.ISNOTNULL, left.getAttributeCond().getType());
        assertNull(left.getAttributeCond().getExpression());

        SearchCond right = cond.getRightSearchCond();
        assertNotNull(right);
        assertNotNull(right.getAttributeCond());
        assertEquals("cn", right.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.IEQ, right.getAttributeCond().getType());
        assertEquals("Other", right.getAttributeCond().getExpression());
    }

    @Test
    public void type() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "userType eq \"Employee\" and (emails.type eq \"work\")");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.AND, cond.getType());

        SearchCond left = cond.getLeftSearchCond();
        assertNotNull(left);
        assertNotNull(left.getAttributeCond());
        assertEquals("userType", left.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.IEQ, left.getAttributeCond().getType());
        assertEquals("Employee", left.getAttributeCond().getExpression());

        SearchCond right = cond.getRightSearchCond();
        assertNotNull(right);
        assertNotNull(right.getAttributeCond());
        assertEquals("email", right.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.ISNOTNULL, right.getAttributeCond().getType());
    }

    @Test
    public void name() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "name.familyName co \"O'Malley\"");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.LEAF, cond.getType());

        AttributeCond leaf = cond.getAttributeCond();
        assertNotNull(leaf);
        assertEquals("surname", leaf.getSchema());
        assertEquals(AttributeCond.Type.ILIKE, leaf.getType());
        assertEquals("%O'Malley%", leaf.getExpression());
    }

    @Test
    public void emails() {
        SearchCond cond = SearchCondConverter.convert(VISITOR,
                "emails co \"example.com\" or emails.value co \"example.org\"");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.OR, cond.getType());

        SearchCond left = cond.getLeftSearchCond();
        assertNotNull(left);
        assertEquals(SearchCond.Type.OR, left.getType());

        SearchCond left1 = left.getLeftSearchCond();
        assertNotNull(left1);
        assertNotNull(left1.getAttributeCond());
        assertEquals("email", left1.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.ILIKE, left1.getAttributeCond().getType());
        assertEquals("%example.com%", left1.getAttributeCond().getExpression());

        SearchCond left2 = left.getRightSearchCond();
        assertNotNull(left2);
        assertNotNull(left2.getAttributeCond());
        assertEquals("gmail", left2.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.ILIKE, left2.getAttributeCond().getType());
        assertEquals("%example.com%", left2.getAttributeCond().getExpression());

        SearchCond right = cond.getRightSearchCond();
        assertNotNull(right);
        assertEquals(SearchCond.Type.OR, right.getType());

        SearchCond right1 = right.getLeftSearchCond();
        assertNotNull(right1);
        assertNotNull(right1.getAttributeCond());
        assertEquals("email", right1.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.ILIKE, right1.getAttributeCond().getType());
        assertEquals("%example.org%", right1.getAttributeCond().getExpression());

        SearchCond right2 = right.getRightSearchCond();
        assertNotNull(right2);
        assertNotNull(right2.getAttributeCond());
        assertEquals("gmail", right2.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.ILIKE, right2.getAttributeCond().getType());
        assertEquals("%example.org%", right2.getAttributeCond().getExpression());
    }
}
