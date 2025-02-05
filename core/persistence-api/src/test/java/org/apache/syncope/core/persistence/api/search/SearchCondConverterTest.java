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
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.AuxClassCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
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
        SearchCond leaf = SearchCond.of(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void ieq() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalToIgnoreCase("rossini").query();
        assertEquals("username=~rossini", fiql);

        AnyCond attrCond = new AnyCond(AttrCond.Type.IEQ);
        attrCond.setSchema("username");
        attrCond.setExpression("rossini");
        SearchCond leaf = SearchCond.of(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void nieq() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").notEqualTolIgnoreCase("rossini").query();
        assertEquals("username!~rossini", fiql);

        AnyCond anyCond = new AnyCond(AttrCond.Type.IEQ);
        anyCond.setSchema("username");
        anyCond.setExpression("rossini");
        SearchCond leaf = SearchCond.negate(anyCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void like() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalTo("ros*").query();
        assertEquals("username==ros*", fiql);

        AttrCond attrCond = new AnyCond(AttrCond.Type.LIKE);
        attrCond.setSchema("username");
        attrCond.setExpression("ros%");
        SearchCond leaf = SearchCond.of(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void ilike() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalToIgnoreCase("ros*").query();
        assertEquals("username=~ros*", fiql);

        AttrCond attrCond = new AnyCond(AttrCond.Type.ILIKE);
        attrCond.setSchema("username");
        attrCond.setExpression("ros%");
        SearchCond leaf = SearchCond.of(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void nilike() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").notEqualTolIgnoreCase("ros*").query();
        assertEquals("username!~ros*", fiql);

        AttrCond attrCond = new AnyCond(AttrCond.Type.ILIKE);
        attrCond.setSchema("username");
        attrCond.setExpression("ros%");
        SearchCond leaf = SearchCond.negate(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void isNull() {
        String fiql = new UserFiqlSearchConditionBuilder().is("loginDate").nullValue().query();
        assertEquals("loginDate==" + SpecialAttr.NULL, fiql);

        AttrCond attrCond = new AttrCond(AttrCond.Type.ISNULL);
        attrCond.setSchema("loginDate");
        SearchCond leaf = SearchCond.of(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void isNotNull() {
        String fiql = new UserFiqlSearchConditionBuilder().is("loginDate").notNullValue().query();
        assertEquals("loginDate!=" + SpecialAttr.NULL, fiql);

        AttrCond attrCond = new AttrCond(AttrCond.Type.ISNOTNULL);
        attrCond.setSchema("loginDate");
        SearchCond leaf = SearchCond.of(attrCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void relationships() {
        String fiql = new UserFiqlSearchConditionBuilder().
                inRelationships("ca20ffca-1305-442f-be9a-3723a0cd88ca").query();
        assertEquals(SpecialAttr.RELATIONSHIPS + "==ca20ffca-1305-442f-be9a-3723a0cd88ca", fiql);

        RelationshipCond relationshipCond = new RelationshipCond();
        relationshipCond.setAnyObject("ca20ffca-1305-442f-be9a-3723a0cd88ca");
        SearchCond leaf = SearchCond.of(relationshipCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void relationshipTypes() {
        String fiql = new UserFiqlSearchConditionBuilder().inRelationshipTypes("type1").query();
        assertEquals(SpecialAttr.RELATIONSHIP_TYPES + "==type1", fiql);

        RelationshipTypeCond relationshipCond = new RelationshipTypeCond();
        relationshipCond.setRelationshipType("type1");
        SearchCond leaf = SearchCond.of(relationshipCond);

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
        SearchCond leaf = SearchCond.of(groupCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void roles() {
        String fiql = new UserFiqlSearchConditionBuilder().inRoles("User reviewer").query();
        assertEquals(SpecialAttr.ROLES + "==User reviewer", fiql);

        RoleCond roleCond = new RoleCond();
        roleCond.setRole("User reviewer");
        SearchCond leaf = SearchCond.of(roleCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void dynRealms() {
        String dynRealm = UUID.randomUUID().toString();
        String fiql = new UserFiqlSearchConditionBuilder().inDynRealms(dynRealm).query();
        assertEquals(SpecialAttr.DYNREALMS + "==" + dynRealm, fiql);

        DynRealmCond dynRealmCond = new DynRealmCond();
        dynRealmCond.setDynRealm(dynRealm);
        SearchCond leaf = SearchCond.of(dynRealmCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void auxClasses() {
        String fiql = new UserFiqlSearchConditionBuilder().hasAuxClasses("clazz1").query();
        assertEquals(SpecialAttr.AUX_CLASSES + "==clazz1", fiql);

        AuxClassCond cond = new AuxClassCond();
        cond.setAuxClass("clazz1");
        SearchCond leaf = SearchCond.of(cond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void resources() {
        String fiql = new UserFiqlSearchConditionBuilder().hasResources("resource-ldap").query();
        assertEquals(SpecialAttr.RESOURCES + "==resource-ldap", fiql);

        ResourceCond resCond = new ResourceCond();
        resCond.setResource("resource-ldap");
        SearchCond leaf = SearchCond.of(resCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void type() {
        String fiql = new AnyObjectFiqlSearchConditionBuilder("PRINTER").query();
        assertEquals(SpecialAttr.TYPE + "==PRINTER", fiql);

        AnyTypeCond acond = new AnyTypeCond();
        acond.setAnyTypeKey("PRINTER");
        SearchCond leaf = SearchCond.of(acond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void member() {
        String fiql = new GroupFiqlSearchConditionBuilder().withMembers("rossini").query();
        assertEquals(SpecialAttr.MEMBER + "==rossini", fiql);

        MemberCond mcond = new MemberCond();
        mcond.setMember("rossini");
        SearchCond leaf = SearchCond.of(mcond);

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
        SearchCond andCond = SearchCond.and(
                SearchCond.of(fullnameLeafCond1),
                SearchCond.of(fullnameLeafCond2));

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
        SearchCond orCond = SearchCond.or(SearchCond.of(fullnameLeafCond1),
                SearchCond.or(
                        SearchCond.of(fullnameLeafCond2),
                        SearchCond.of(fullnameLeafCond3)));

        assertEquals(orCond, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void issueSYNCOPE1223() {
        String fiql = new UserFiqlSearchConditionBuilder().is("ctype").equalTo("ou=sample%252Co=isp").query();

        AttrCond cond = new AttrCond(AttrCond.Type.EQ);
        cond.setSchema("ctype");
        cond.setExpression("ou=sample,o=isp");

        assertEquals(SearchCond.of(cond), SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void issueSYNCOPE1779() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalToIgnoreCase("ros_*").query();
        assertEquals("username=~ros_*", fiql);

        AnyCond anyCond = new AnyCond(AttrCond.Type.ILIKE);
        anyCond.setSchema("username");
        anyCond.setExpression("ros\\_%");
        SearchCond leaf = SearchCond.of(anyCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));

        fiql = "name==_018c34a9-f86b-75cf-855b-a3915cc5ff44";

        anyCond = new AnyCond(AttrCond.Type.EQ);
        anyCond.setSchema("name");
        anyCond.setExpression("_018c34a9-f86b-75cf-855b-a3915cc5ff44");
        leaf = SearchCond.of(anyCond);

        assertEquals(leaf, SearchCondConverter.convert(VISITOR, fiql));
    }

    @Test
    public void issueSYNCOPE1826() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalToIgnoreCase("sh test app 0722").query();
        assertEquals("username=~sh test app 0722", fiql);

        AnyCond anyCond = new AnyCond(AttrCond.Type.IEQ);
        anyCond.setSchema("username");
        anyCond.setExpression("sh test app 0722");

        assertEquals(SearchCond.of(anyCond), SearchCondConverter.convert(VISITOR, fiql));

        fiql = "lastLoginDate==2016-03-02T15:21:22%2B0300";

        AnyCond lastLoginDateCond = new AnyCond(AttrCond.Type.EQ);
        lastLoginDateCond.setSchema("lastLoginDate");
        lastLoginDateCond.setExpression("2016-03-02T15:21:22+0300");

        assertEquals(SearchCond.of(lastLoginDateCond), SearchCondConverter.convert(VISITOR, fiql));
    }
}
