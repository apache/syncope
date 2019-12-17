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
package org.apache.syncope.core.persistence.api.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.apache.syncope.common.lib.search.AnyObjectFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.GroupFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.SpecialAttr;
import org.apache.syncope.common.lib.search.UserFiqlSearchConditionBuilder;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.PrivilegeCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.junit.jupiter.api.Test;

public class SearchCondConverterTest {

    private static final SearchCondVisitor VISITOR = new SearchCondVisitor();

    @Test
    public void eq() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalTo("rossini").query();
        assertEquals("username==rossini", fiql);

        AnyCond attrCond = new AnyCond(AttrCond.Type.EQ);
        attrCond.setSchema("username");
        attrCond.setExpression("rossini");
        SearchCond leaf = SearchCond.getLeaf(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void ieq() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalToIgnoreCase("rossini").query();
        assertEquals("username=~rossini", fiql);

        AnyCond attrCond = new AnyCond(AttrCond.Type.IEQ);
        attrCond.setSchema("username");
        attrCond.setExpression("rossini");
        SearchCond leaf = SearchCond.getLeaf(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void nieq() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").notEqualTolIgnoreCase("rossini").query();
        assertEquals("username!~rossini", fiql);

        AnyCond anyCond = new AnyCond(AttrCond.Type.IEQ);
        anyCond.setSchema("username");
        anyCond.setExpression("rossini");
        SearchCond leaf = SearchCond.getNotLeaf(anyCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void like() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalTo("ros*").query();
        assertEquals("username==ros*", fiql);

        AttrCond attrCond = new AnyCond(AttrCond.Type.LIKE);
        attrCond.setSchema("username");
        attrCond.setExpression("ros%");
        SearchCond leaf = SearchCond.getLeaf(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void ilike() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalToIgnoreCase("ros*").query();
        assertEquals("username=~ros*", fiql);

        AttrCond attrCond = new AnyCond(AttrCond.Type.ILIKE);
        attrCond.setSchema("username");
        attrCond.setExpression("ros%");
        SearchCond leaf = SearchCond.getLeaf(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void nilike() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").notEqualTolIgnoreCase("ros*").query();
        assertEquals("username!~ros*", fiql);

        AttrCond attrCond = new AnyCond(AttrCond.Type.ILIKE);
        attrCond.setSchema("username");
        attrCond.setExpression("ros%");
        SearchCond leaf = SearchCond.getNotLeaf(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void isNull() {
        String fiql = new UserFiqlSearchConditionBuilder().is("loginDate").nullValue().query();
        assertEquals("loginDate==" + SpecialAttr.NULL, fiql);

        AttrCond attrCond = new AttrCond(AttrCond.Type.ISNULL);
        attrCond.setSchema("loginDate");
        SearchCond leaf = SearchCond.getLeaf(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void isNotNull() {
        String fiql = new UserFiqlSearchConditionBuilder().is("loginDate").notNullValue().query();
        assertEquals("loginDate!=" + SpecialAttr.NULL, fiql);

        AttrCond attrCond = new AttrCond(AttrCond.Type.ISNOTNULL);
        attrCond.setSchema("loginDate");
        SearchCond leaf = SearchCond.getLeaf(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void relationships() {
        String fiql = new UserFiqlSearchConditionBuilder().
                inRelationships("ca20ffca-1305-442f-be9a-3723a0cd88ca").query();
        assertEquals(SpecialAttr.RELATIONSHIPS + "==ca20ffca-1305-442f-be9a-3723a0cd88ca", fiql);

        RelationshipCond relationshipCond = new RelationshipCond();
        relationshipCond.setAnyObject("ca20ffca-1305-442f-be9a-3723a0cd88ca");
        SearchCond leaf = SearchCond.getLeaf(relationshipCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void relationshipTypes() {
        String fiql = new UserFiqlSearchConditionBuilder().inRelationshipTypes("type1").query();
        assertEquals(SpecialAttr.RELATIONSHIP_TYPES + "==type1", fiql);

        RelationshipTypeCond relationshipCond = new RelationshipTypeCond();
        relationshipCond.setRelationshipTypeKey("type1");
        SearchCond leaf = SearchCond.getLeaf(relationshipCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));

        fiql = new AnyObjectFiqlSearchConditionBuilder("PRINTER").inRelationshipTypes("neighborhood").query();
        assertEquals(
                SpecialAttr.RELATIONSHIP_TYPES + "==neighborhood;" + SpecialAttr.TYPE + "==PRINTER",
                fiql);
    }

    @Test
    public void groups() {
        String fiql = new UserFiqlSearchConditionBuilder().
                inGroups("e7ff94e8-19c9-4f0a-b8b7-28327edbf6ed").query();
        assertEquals(SpecialAttr.GROUPS + "==e7ff94e8-19c9-4f0a-b8b7-28327edbf6ed", fiql);

        MembershipCond groupCond = new MembershipCond();
        groupCond.setGroup("e7ff94e8-19c9-4f0a-b8b7-28327edbf6ed");
        SearchCond leaf = SearchCond.getLeaf(groupCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void roles() {
        String fiql = new UserFiqlSearchConditionBuilder().inRoles("User reviewer").query();
        assertEquals(SpecialAttr.ROLES + "==User reviewer", fiql);

        RoleCond roleCond = new RoleCond();
        roleCond.setRole("User reviewer");
        SearchCond leaf = SearchCond.getLeaf(roleCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void privileges() {
        String fiql = new UserFiqlSearchConditionBuilder().withPrivileges("postMighty").query();
        assertEquals(SpecialAttr.PRIVILEGES + "==postMighty", fiql);

        PrivilegeCond privilegeCond = new PrivilegeCond();
        privilegeCond.setPrivilege("postMighty");
        SearchCond leaf = SearchCond.getLeaf(privilegeCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void dynRealms() {
        String dynRealm = UUID.randomUUID().toString();
        String fiql = new UserFiqlSearchConditionBuilder().inDynRealms(dynRealm).query();
        assertEquals(SpecialAttr.DYNREALMS + "==" + dynRealm, fiql);

        DynRealmCond dynRealmCond = new DynRealmCond();
        dynRealmCond.setDynRealm(dynRealm);
        SearchCond leaf = SearchCond.getLeaf(dynRealmCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void resources() {
        String fiql = new UserFiqlSearchConditionBuilder().hasResources("resource-ldap").query();
        assertEquals(SpecialAttr.RESOURCES + "==resource-ldap", fiql);

        ResourceCond resCond = new ResourceCond();
        resCond.setResourceKey("resource-ldap");
        SearchCond leaf = SearchCond.getLeaf(resCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void assignable() {
        String fiql = new GroupFiqlSearchConditionBuilder().isAssignable().query();
        assertEquals(SpecialAttr.ASSIGNABLE + "==" + SpecialAttr.NULL, fiql);

        AssignableCond assignableCond = new AssignableCond();
        assignableCond.setRealmFullPath("/even/two");
        SearchCond leaf = SearchCond.getLeaf(assignableCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql, "/even/two"));
    }

    @Test
    public void type() {
        String fiql = new AnyObjectFiqlSearchConditionBuilder("PRINTER").query();
        assertEquals(SpecialAttr.TYPE + "==PRINTER", fiql);

        AnyTypeCond acond = new AnyTypeCond();
        acond.setAnyTypeKey("PRINTER");
        SearchCond leaf = SearchCond.getLeaf(acond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void member() {
        String fiql = new GroupFiqlSearchConditionBuilder().withMembers("rossini").query();
        assertEquals(SpecialAttr.MEMBER + "==rossini", fiql);

        MemberCond mcond = new MemberCond();
        mcond.setMember("rossini");
        SearchCond leaf = SearchCond.getLeaf(mcond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void and() {
        String fiql = new UserFiqlSearchConditionBuilder().
                is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query();
        assertEquals("fullname==*o*;fullname==*i*", fiql);

        AttrCond fullnameLeafCond1 = new AttrCond(AttrCond.Type.LIKE);
        fullnameLeafCond1.setSchema("fullname");
        fullnameLeafCond1.setExpression("%o%");
        AttrCond fullnameLeafCond2 = new AttrCond(AttrCond.Type.LIKE);
        fullnameLeafCond2.setSchema("fullname");
        fullnameLeafCond2.setExpression("%i%");
        SearchCond andCond = SearchCond.getAnd(
                SearchCond.getLeaf(fullnameLeafCond1),
                SearchCond.getLeaf(fullnameLeafCond2));

        assertEquals(andCond, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void or() {
        String fiql = new UserFiqlSearchConditionBuilder().
                is("fullname").equalTo("*o*", "*i*", "*ini").query();
        assertEquals("fullname==*o*,fullname==*i*,fullname==*ini", fiql);
        fiql = new UserFiqlSearchConditionBuilder().
                is("fullname").equalTo("*o*").or("fullname").equalTo("*i*").or("fullname").equalTo("*ini").query();
        assertEquals("fullname==*o*,fullname==*i*,fullname==*ini", fiql);

        AttrCond fullnameLeafCond1 = new AttrCond(AttrCond.Type.LIKE);
        fullnameLeafCond1.setSchema("fullname");
        fullnameLeafCond1.setExpression("%o%");
        AttrCond fullnameLeafCond2 = new AttrCond(AttrCond.Type.LIKE);
        fullnameLeafCond2.setSchema("fullname");
        fullnameLeafCond2.setExpression("%i%");
        AttrCond fullnameLeafCond3 = new AttrCond(AttrCond.Type.LIKE);
        fullnameLeafCond3.setSchema("fullname");
        fullnameLeafCond3.setExpression("%ini");
        SearchCond orCond = SearchCond.getOr(SearchCond.getLeaf(fullnameLeafCond1),
                SearchCond.getOr(
                        SearchCond.getLeaf(fullnameLeafCond2),
                        SearchCond.getLeaf(fullnameLeafCond3)));

        assertEquals(orCond, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void issueSYNCOPE1223() {
        String fiql = new UserFiqlSearchConditionBuilder().is("ctype").equalTo("ou=sample%252Co=isp").query();

        AttrCond cond = new AttrCond(AttrCond.Type.EQ);
        cond.setSchema("ctype");
        cond.setExpression("ou=sample,o=isp");

        assertEquals(SearchCond.getLeaf(cond), SearchCondConverter.convert(VISITOR, fiql));
    }

}
