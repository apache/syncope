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
import org.apache.syncope.common.lib.scim.SCIMEnterpriseUserConf;
import org.apache.syncope.common.lib.scim.SCIMExtensionUserConf;
import org.apache.syncope.common.lib.scim.SCIMItem;
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

        SCIMEnterpriseUserConf entConf = new SCIMEnterpriseUserConf();
        entConf.setOrganization("org");
        conf.setEnterpriseUserConf(entConf);

        SCIMExtensionUserConf extConf = new SCIMExtensionUserConf();
        SCIMItem item = new SCIMItem();
        item.setIntAttrName("realm");
        item.setExtAttrName("realm");
        extConf.add(item);
        conf.setExtensionUserConf(extConf);

        VISITOR = new SearchCondVisitor(Resource.User, conf);
    }

    @Test
    public void eq() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "userName eq \"bjensen\"");
        assertNotNull(cond);
        assertEquals("username", cond.asLeaf(AnyCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.IEQ, cond.asLeaf(AnyCond.class).orElseThrow().getType());
        assertEquals("bjensen", cond.asLeaf(AnyCond.class).orElseThrow().getExpression());
    }

    @Test
    public void sw() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "userName sw \"J\"");
        assertNotNull(cond);
        assertEquals("username", cond.asLeaf(AnyCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ILIKE, cond.asLeaf(AnyCond.class).orElseThrow().getType());
        assertEquals("J%", cond.asLeaf(AnyCond.class).orElseThrow().getExpression());

        SearchCond fqn = SearchCondConverter.convert(
                VISITOR, "urn:ietf:params:scim:schemas:core:2.0:User:userName sw \"J\"");
        assertEquals(cond, fqn);
    }

    @Test
    public void pr() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "title pr");
        assertNotNull(cond);
        assertEquals("title", cond.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ISNOTNULL, cond.asLeaf(AttrCond.class).orElseThrow().getType());
        assertNull(cond.asLeaf(AttrCond.class).orElseThrow().getExpression());
    }

    @Test
    public void gt() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "meta.lastModified gt \"2011-05-13T04:42:34Z\"");
        assertNotNull(cond);
        assertEquals("lastChangeDate", cond.asLeaf(AnyCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.GT, cond.asLeaf(AnyCond.class).orElseThrow().getType());
        assertEquals("2011-05-13T04:42:34Z", cond.asLeaf(AnyCond.class).orElseThrow().getExpression());
    }

    @Test
    public void not() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "not (title pr)");
        assertNotNull(cond);
        assertEquals("title", cond.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ISNULL, cond.asLeaf(AttrCond.class).orElseThrow().getType());
        assertNull(cond.asLeaf(AttrCond.class).orElseThrow().getExpression());
    }

    @Test
    public void and() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "title pr and userName sw \"J\"");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.AND, cond.getType());

        SearchCond left = cond.getLeft();
        assertNotNull(left);
        assertEquals("title", left.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ISNOTNULL, left.asLeaf(AttrCond.class).orElseThrow().getType());
        assertNull(left.asLeaf(AttrCond.class).orElseThrow().getExpression());

        SearchCond right = cond.getRight();
        assertNotNull(right);
        assertEquals("username", right.asLeaf(AnyCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ILIKE, right.asLeaf(AnyCond.class).orElseThrow().getType());
        assertEquals("J%", right.asLeaf(AnyCond.class).orElseThrow().getExpression());
    }

    @Test
    public void or() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "title pr or displayName eq \"Other\"");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.OR, cond.getType());

        SearchCond left = cond.getLeft();
        assertNotNull(left);
        assertEquals("title", left.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ISNOTNULL, left.asLeaf(AttrCond.class).orElseThrow().getType());
        assertNull(left.asLeaf(AttrCond.class).orElseThrow().getExpression());

        SearchCond right = cond.getRight();
        assertNotNull(right);
        assertEquals("cn", right.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.IEQ, right.asLeaf(AttrCond.class).orElseThrow().getType());
        assertEquals("Other", right.asLeaf(AttrCond.class).orElseThrow().getExpression());
    }

    @Test
    public void type() {
        SearchCond cond = SearchCondConverter.convert(
                VISITOR, "userType eq \"Employee\" and (emails.type eq \"work\")");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.AND, cond.getType());

        SearchCond left = cond.getLeft();
        assertNotNull(left);
        assertEquals("userType", left.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.IEQ, left.asLeaf(AttrCond.class).orElseThrow().getType());
        assertEquals("Employee", left.asLeaf(AttrCond.class).orElseThrow().getExpression());

        SearchCond right = cond.getRight();
        assertNotNull(right);
        assertEquals("email", right.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ISNOTNULL, right.asLeaf(AttrCond.class).orElseThrow().getType());
    }

    @Test
    public void name() {
        SearchCond cond = SearchCondConverter.convert(VISITOR, "name.familyName co \"O'Malley\"");
        assertNotNull(cond);
        assertEquals(SearchCond.Type.LEAF, cond.getType());

        AttrCond leaf = cond.asLeaf(AttrCond.class).orElseThrow();
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
        assertEquals("email", left1.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ILIKE, left1.asLeaf(AttrCond.class).orElseThrow().getType());
        assertEquals("%example.com%", left1.asLeaf(AttrCond.class).orElseThrow().getExpression());

        SearchCond left2 = left.getRight();
        assertNotNull(left2);
        assertEquals("gmail", left2.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ILIKE, left2.asLeaf(AttrCond.class).orElseThrow().getType());
        assertEquals("%example.com%", left2.asLeaf(AttrCond.class).orElseThrow().getExpression());

        SearchCond right = cond.getRight();
        assertNotNull(right);
        assertEquals(SearchCond.Type.OR, right.getType());

        SearchCond right1 = right.getLeft();
        assertNotNull(right1);
        assertEquals("email", right1.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ILIKE, right1.asLeaf(AttrCond.class).orElseThrow().getType());
        assertEquals("%example.org%", right1.asLeaf(AttrCond.class).orElseThrow().getExpression());

        SearchCond right2 = right.getRight();
        assertNotNull(right2);
        assertEquals("gmail", right2.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ILIKE, right2.asLeaf(AttrCond.class).orElseThrow().getType());
        assertEquals("%example.org%", right2.asLeaf(AttrCond.class).orElseThrow().getExpression());
    }

    @Test
    public void enterpriseUserAttr() {
        SearchCond cond = SearchCondConverter.convert(VISITOR,
                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:organization eq \"The ASF\"");
        assertNotNull(cond);
        assertEquals("org", cond.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.IEQ, cond.asLeaf(AttrCond.class).orElseThrow().getType());
        assertEquals("The ASF", cond.asLeaf(AttrCond.class).orElseThrow().getExpression());
    }

    @Test
    public void extensionUserAttr() {
        SearchCond cond = SearchCondConverter.convert(VISITOR,
                "urn:ietf:params:scim:schemas:extension:syncope:2.0:User:realm sw \"/\"");
        assertNotNull(cond);
        assertEquals("realm", cond.asLeaf(AttrCond.class).orElseThrow().getSchema());
        assertEquals(AttrCond.Type.ILIKE, cond.asLeaf(AttrCond.class).orElseThrow().getType());
        assertEquals("/%", cond.asLeaf(AttrCond.class).orElseThrow().getExpression());
    }
}
