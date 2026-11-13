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
package org.apache.syncope.common.lib.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.junit.jupiter.api.Test;

class FiqlSearchConditionBuildersTest {

    @Test
    void contextualProperties() {
        assertEquals("true", AbstractFiqlSearchConditionBuilder.CONTEXTUAL_PROPERTIES.get(
                SearchUtils.LAX_PROPERTY_MATCH));
    }

    @Test
    void user() {
        UserFiqlSearchConditionBuilder builder = new UserFiqlSearchConditionBuilder();
        assertEquals("username==rossini", builder.is("username").equalTo("rossini").query());
        assertEquals("username=~rossini", builder.is("username").equalToIgnoreCase("rossini").query());
        assertEquals("username!~rossini", builder.is("username").notEqualTolIgnoreCase("rossini").query());
        assertEquals("loginDate==$null", builder.is("loginDate").nullValue().query());
        assertEquals("loginDate!=$null", builder.is("loginDate").notNullValue().query());
        assertEquals("$auxClasses==clazz", builder.hasAuxClasses("clazz").query());
        assertEquals("$auxClasses!=clazz", builder.hasNotAuxClasses("clazz").query());
        assertEquals("$resources==resource", builder.hasResources("resource").query());
        assertEquals("$resources!=resource", builder.hasNotResources("resource").query());
        assertEquals("$groups==group", builder.inGroups("group").query());
        assertEquals("$groups!=group", builder.notInGroups("group").query());
        assertEquals("$relationships==anyObject", builder.inRelationships("anyObject").query());
        assertEquals("$relationships!=anyObject", builder.notInRelationships("anyObject").query());
        assertEquals("$relationshipTypes==type", builder.inRelationshipTypes("type").query());
        assertEquals("$relationshipTypes!=type", builder.notInRelationshipTypes("type").query());
        assertEquals("$roles==role", builder.inRoles("role").query());
        assertEquals("$roles!=role", builder.notInRoles("role").query());
    }

    @Test
    void userMultipleValues() {
        UserFiqlSearchConditionBuilder builder = new UserFiqlSearchConditionBuilder();
        String fiql = builder.hasAuxClasses("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.AUX_CLASSES + "=="));
        assertTrue(fiql.contains("one"));
        assertTrue(fiql.contains("two"));

        fiql = builder.hasNotAuxClasses("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.AUX_CLASSES + "!="));
        assertTrue(fiql.contains("one"));
        assertTrue(fiql.contains("two"));

        fiql = builder.hasResources("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.RESOURCES + "=="));

        fiql = builder.hasNotResources("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.RESOURCES + "!="));

        fiql = builder.inGroups("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.GROUPS + "=="));

        fiql = builder.notInGroups("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.GROUPS + "!="));

        fiql = builder.inRelationships("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.RELATIONSHIPS + "=="));

        fiql = builder.notInRelationships("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.RELATIONSHIPS + "!="));

        fiql = builder.inRelationshipTypes("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.RELATIONSHIP_TYPES + "=="));

        fiql = builder.notInRelationshipTypes("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.RELATIONSHIP_TYPES + "!="));

        fiql = builder.inRoles("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.ROLES + "=="));

        fiql = builder.notInRoles("one", "two").query();
        assertTrue(fiql.startsWith(SpecialAttr.ROLES + "!="));
    }

    @Test
    void userComposition() {
        UserFiqlSearchConditionBuilder builder = new UserFiqlSearchConditionBuilder();
        assertEquals("username==a;username==b",
                builder.is("username").equalTo("a").and("username").equalTo("b").query());
        assertEquals("username==a,username==b",
                builder.is("username").equalTo("a").or("username").equalTo("b").query());

        UserCompleteCondition first = (UserCompleteCondition) builder.is("username").equalTo("a");
        UserCompleteCondition second = (UserCompleteCondition) builder.is("username").equalTo("b");

        UserPartialCondition partial = (UserPartialCondition) builder.is("username").equalTo("a").and();
        String varargsAnd = partial.and(first, second).query();
        assertTrue(varargsAnd.contains("username==a"));
        assertTrue(varargsAnd.contains("username==b"));
        assertTrue(varargsAnd.contains(";"));

        String listAnd = partial.and(List.of(first, second)).query();
        assertTrue(listAnd.contains("username==a"));
        assertTrue(listAnd.contains("username==b"));
        assertTrue(listAnd.contains(";"));

        UserPartialCondition orPartial = (UserPartialCondition) builder.is("username").equalTo("a").or();
        String varargsOr = orPartial.or(first, second).query();
        assertTrue(varargsOr.contains("username==a"));
        assertTrue(varargsOr.contains("username==b"));
        assertTrue(varargsOr.contains(","));

        String listOr = builder.is("username").equalTo("a").or().or(List.of(first, second)).query();
        assertTrue(listOr.contains("username==a"));
        assertTrue(listOr.contains("username==b"));
        assertTrue(listOr.contains(","));
    }

    @Test
    void group() {
        GroupFiqlSearchConditionBuilder builder = new GroupFiqlSearchConditionBuilder();
        assertEquals("name==group", builder.is("name").equalTo("group").query());
        assertEquals("$member==member", builder.withMembers("member").query());
        assertEquals("$member!=member", builder.withoutMembers("member").query());
        assertTrue(builder.withMembers("one", "two").query().startsWith(SpecialAttr.MEMBER + "=="));
        assertTrue(builder.withoutMembers("one", "two").query().startsWith(SpecialAttr.MEMBER + "!="));
    }

    @Test
    void anyObject() {
        AnyObjectFiqlSearchConditionBuilder builder = new AnyObjectFiqlSearchConditionBuilder("PRINTER");
        assertEquals("$type==PRINTER", builder.query());
        assertEquals("name==printer;$type==PRINTER", builder.is("name").equalTo("printer").query());
        assertEquals("$groups==group;$type==PRINTER", builder.inGroups("group").query());
        assertEquals("$groups!=group;$type==PRINTER", builder.notInGroups("group").query());
        assertEquals("$relationships==anyObject;$type==PRINTER", builder.inRelationships("anyObject").query());
        assertEquals("$relationships!=anyObject;$type==PRINTER", builder.notInRelationships("anyObject").query());
        assertEquals("$relationshipTypes==type;$type==PRINTER", builder.inRelationshipTypes("type").query());
        assertEquals("$relationshipTypes!=type;$type==PRINTER", builder.notInRelationshipTypes("type").query());
    }

    @Test
    void realm() {
        RealmFiqlSearchConditionBuilder builder = new RealmFiqlSearchConditionBuilder();
        assertEquals("name==realm", builder.is("name").equalTo("realm").query());
        assertEquals("name=~realm", builder.is("name").equalToIgnoreCase("realm").query());
        assertEquals("name!~realm", builder.is("name").notEqualTolIgnoreCase("realm").query());
        assertEquals("name==$null", builder.is("name").nullValue().query());
        assertEquals("name!=$null", builder.is("name").notNullValue().query());
        assertEquals("$auxClasses==clazz", builder.hasAuxClasses("clazz").query());
    }

    @Test
    void connObject() {
        ConnObjectTOFiqlSearchConditionBuilder builder = new ConnObjectTOFiqlSearchConditionBuilder();
        assertEquals("username==rossini", builder.is("username").equalTo("rossini").query());
        assertEquals("username=~rossini", builder.is("username").equalToIgnoreCase("rossini").query());
        assertEquals("username!~rossini", builder.is("username").notEqualTolIgnoreCase("rossini").query());
        assertEquals("loginDate==$null", builder.is("loginDate").nullValue().query());
        assertEquals("loginDate!=$null", builder.is("loginDate").notNullValue().query());
    }

    @Test
    void completeConditionAsObject() {
        UserFiqlSearchConditionBuilder builder = new UserFiqlSearchConditionBuilder();
        CompleteCondition condition = builder.is("username").equalTo("rossini");
        assertEquals("username==rossini", condition.query());
    }
}
