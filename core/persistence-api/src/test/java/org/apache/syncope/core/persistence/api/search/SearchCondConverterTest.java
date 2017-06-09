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

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import org.apache.syncope.common.lib.search.AnyObjectFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.GroupFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.SpecialAttr;
import org.apache.syncope.common.lib.search.UserFiqlSearchConditionBuilder;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.junit.Test;

public class SearchCondConverterTest {

    @Test
    public void eq() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalTo("rossini").query();
        assertEquals("username==rossini", fiql);

        AnyCond attrCond = new AnyCond(AttributeCond.Type.EQ);
        attrCond.setSchema("username");
        attrCond.setExpression("rossini");
        SearchCond simpleCond = SearchCond.getLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void ieq() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalToIgnoreCase("rossini").query();
        assertEquals("username=~rossini", fiql);

        AnyCond attrCond = new AnyCond(AttributeCond.Type.IEQ);
        attrCond.setSchema("username");
        attrCond.setExpression("rossini");
        SearchCond simpleCond = SearchCond.getLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void nieq() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").notEqualTolIgnoreCase("rossini").query();
        assertEquals("username!~rossini", fiql);

        AnyCond attrCond = new AnyCond(AttributeCond.Type.IEQ);
        attrCond.setSchema("username");
        attrCond.setExpression("rossini");
        SearchCond simpleCond = SearchCond.getNotLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void like() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalTo("ros*").query();
        assertEquals("username==ros*", fiql);

        AttributeCond attrCond = new AnyCond(AttributeCond.Type.LIKE);
        attrCond.setSchema("username");
        attrCond.setExpression("ros%");
        SearchCond simpleCond = SearchCond.getLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void ilike() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").equalToIgnoreCase("ros*").query();
        assertEquals("username=~ros*", fiql);

        AttributeCond attrCond = new AnyCond(AttributeCond.Type.ILIKE);
        attrCond.setSchema("username");
        attrCond.setExpression("ros%");
        SearchCond simpleCond = SearchCond.getLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void nilike() {
        String fiql = new UserFiqlSearchConditionBuilder().is("username").notEqualTolIgnoreCase("ros*").query();
        assertEquals("username!~ros*", fiql);

        AttributeCond attrCond = new AnyCond(AttributeCond.Type.ILIKE);
        attrCond.setSchema("username");
        attrCond.setExpression("ros%");
        SearchCond simpleCond = SearchCond.getNotLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void isNull() {
        String fiql = new UserFiqlSearchConditionBuilder().is("loginDate").nullValue().query();
        assertEquals("loginDate==" + SpecialAttr.NULL, fiql);

        AttributeCond attrCond = new AttributeCond(AttributeCond.Type.ISNULL);
        attrCond.setSchema("loginDate");
        SearchCond simpleCond = SearchCond.getLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void isNotNull() {
        String fiql = new UserFiqlSearchConditionBuilder().is("loginDate").notNullValue().query();
        assertEquals("loginDate!=" + SpecialAttr.NULL, fiql);

        AttributeCond attrCond = new AttributeCond(AttributeCond.Type.ISNOTNULL);
        attrCond.setSchema("loginDate");
        SearchCond simpleCond = SearchCond.getLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void relationships() {
        String fiql = new UserFiqlSearchConditionBuilder().
                inRelationships("ca20ffca-1305-442f-be9a-3723a0cd88ca").query();
        assertEquals(SpecialAttr.RELATIONSHIPS + "==ca20ffca-1305-442f-be9a-3723a0cd88ca", fiql);

        RelationshipCond relationshipCond = new RelationshipCond();
        relationshipCond.setAnyObject("ca20ffca-1305-442f-be9a-3723a0cd88ca");
        SearchCond simpleCond = SearchCond.getLeafCond(relationshipCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void relationshipTypes() {
        String fiql = new UserFiqlSearchConditionBuilder().inRelationshipTypes("type1").query();
        assertEquals(SpecialAttr.RELATIONSHIP_TYPES + "==type1", fiql);

        RelationshipTypeCond relationshipCond = new RelationshipTypeCond();
        relationshipCond.setRelationshipTypeKey("type1");
        SearchCond simpleCond = SearchCond.getLeafCond(relationshipCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));

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
        SearchCond simpleCond = SearchCond.getLeafCond(groupCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void roles() {
        String fiql = new UserFiqlSearchConditionBuilder().inRoles("User reviewer").query();
        assertEquals(SpecialAttr.ROLES + "==User reviewer", fiql);

        RoleCond roleCond = new RoleCond();
        roleCond.setRole("User reviewer");
        SearchCond simpleCond = SearchCond.getLeafCond(roleCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void dynRealms() {
        String dynRealm = UUID.randomUUID().toString();
        String fiql = new UserFiqlSearchConditionBuilder().inDynRealms(dynRealm).query();
        assertEquals(SpecialAttr.DYNREALMS + "==" + dynRealm, fiql);

        DynRealmCond dynRealmCond = new DynRealmCond();
        dynRealmCond.setDynRealm(dynRealm);
        SearchCond simpleCond = SearchCond.getLeafCond(dynRealmCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void resources() {
        String fiql = new UserFiqlSearchConditionBuilder().hasResources("resource-ldap").query();
        assertEquals(SpecialAttr.RESOURCES + "==resource-ldap", fiql);

        ResourceCond resCond = new ResourceCond();
        resCond.setResourceKey("resource-ldap");
        SearchCond simpleCond = SearchCond.getLeafCond(resCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void assignable() {
        String fiql = new GroupFiqlSearchConditionBuilder().isAssignable().query();
        assertEquals(SpecialAttr.ASSIGNABLE + "==" + SpecialAttr.NULL, fiql);

        AssignableCond assignableCond = new AssignableCond();
        assignableCond.setRealmFullPath("/even/two");
        SearchCond simpleCond = SearchCond.getLeafCond(assignableCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql, "/even/two"));
    }

    @Test
    public void type() {
        String fiql = new AnyObjectFiqlSearchConditionBuilder("PRINTER").query();
        assertEquals(SpecialAttr.TYPE + "==PRINTER", fiql);

        AnyTypeCond acond = new AnyTypeCond();
        acond.setAnyTypeKey("PRINTER");
        SearchCond simpleCond = SearchCond.getLeafCond(acond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void member() {
        String fiql = new GroupFiqlSearchConditionBuilder().withMembers("rossini").query();
        assertEquals(SpecialAttr.MEMBER + "==rossini", fiql);

        MemberCond mcond = new MemberCond();
        mcond.setMember("rossini");
        SearchCond simpleCond = SearchCond.getLeafCond(mcond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void and() {
        String fiql = new UserFiqlSearchConditionBuilder().
                is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query();
        assertEquals("fullname==*o*;fullname==*i*", fiql);

        AttributeCond fullnameLeafCond1 = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond1.setSchema("fullname");
        fullnameLeafCond1.setExpression("%o%");
        AttributeCond fullnameLeafCond2 = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond2.setSchema("fullname");
        fullnameLeafCond2.setExpression("%i%");
        SearchCond andCond = SearchCond.getAndCond(
                SearchCond.getLeafCond(fullnameLeafCond1),
                SearchCond.getLeafCond(fullnameLeafCond2));

        assertEquals(andCond, SearchCondConverter.convert(fiql));
    }

    @Test
    public void or() {
        String fiql = new UserFiqlSearchConditionBuilder().
                is("fullname").equalTo("*o*", "*i*", "*ini").query();
        assertEquals("fullname==*o*,fullname==*i*,fullname==*ini", fiql);
        fiql = new UserFiqlSearchConditionBuilder().
                is("fullname").equalTo("*o*").or("fullname").equalTo("*i*").or("fullname").equalTo("*ini").query();
        assertEquals("fullname==*o*,fullname==*i*,fullname==*ini", fiql);

        AttributeCond fullnameLeafCond1 = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond1.setSchema("fullname");
        fullnameLeafCond1.setExpression("%o%");
        AttributeCond fullnameLeafCond2 = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond2.setSchema("fullname");
        fullnameLeafCond2.setExpression("%i%");
        AttributeCond fullnameLeafCond3 = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond3.setSchema("fullname");
        fullnameLeafCond3.setExpression("%ini");
        SearchCond orCond = SearchCond.getOrCond(
                SearchCond.getLeafCond(fullnameLeafCond1),
                SearchCond.getOrCond(
                        SearchCond.getLeafCond(fullnameLeafCond2),
                        SearchCond.getLeafCond(fullnameLeafCond3)));

        assertEquals(orCond, SearchCondConverter.convert(fiql));
    }

}
