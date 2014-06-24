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
package org.apache.syncope.core.rest.data;

import static org.junit.Assert.assertEquals;

import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.search.SpecialAttr;
import org.apache.syncope.core.AbstractNonDAOTest;
import org.apache.syncope.core.persistence.dao.search.SubjectCond;
import org.apache.syncope.core.persistence.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.dao.search.EntitlementCond;
import org.apache.syncope.core.persistence.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.junit.Test;

public class SearchCondConverterTest extends AbstractNonDAOTest {

    @Test
    public void eq() {
        String fiqlExpression = SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("rossini").query();
        assertEquals("username==rossini", fiqlExpression);

        SubjectCond attrCond = new SubjectCond(AttributeCond.Type.EQ);
        attrCond.setSchema("username");
        attrCond.setExpression("rossini");
        SearchCond simpleCond = SearchCond.getLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiqlExpression));
    }

    @Test
    public void like() {
        String fiqlExpression = SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("ros*").query();
        assertEquals("username==ros*", fiqlExpression);

        AttributeCond attrCond = new SubjectCond(AttributeCond.Type.LIKE);
        attrCond.setSchema("username");
        attrCond.setExpression("ros%");
        SearchCond simpleCond = SearchCond.getLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiqlExpression));
    }

    @Test
    public void isNull() {
        String fiqlExpression = SyncopeClient.getUserSearchConditionBuilder().is("loginDate").nullValue().query();
        assertEquals("loginDate==" + SpecialAttr.NULL, fiqlExpression);

        AttributeCond attrCond = new AttributeCond(AttributeCond.Type.ISNULL);
        attrCond.setSchema("loginDate");
        SearchCond simpleCond = SearchCond.getLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiqlExpression));
    }

    @Test
    public void isNotNull() {
        String fiqlExpression = SyncopeClient.getUserSearchConditionBuilder().is("loginDate").notNullValue().query();
        assertEquals("loginDate!=" + SpecialAttr.NULL, fiqlExpression);

        AttributeCond attrCond = new AttributeCond(AttributeCond.Type.ISNOTNULL);
        attrCond.setSchema("loginDate");
        SearchCond simpleCond = SearchCond.getLeafCond(attrCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiqlExpression));
    }

    @Test
    public void roles() {
        String fiqlExpression = SyncopeClient.getUserSearchConditionBuilder().hasRoles(1L).query();
        assertEquals(SpecialAttr.ROLES + "==1", fiqlExpression);

        MembershipCond membCond = new MembershipCond();
        membCond.setRoleId(1L);
        SearchCond simpleCond = SearchCond.getLeafCond(membCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiqlExpression));
    }

    @Test
    public void resources() {
        String fiqlExpression = SyncopeClient.getUserSearchConditionBuilder().hasResources("resource-ldap").query();
        assertEquals(SpecialAttr.RESOURCES + "==resource-ldap", fiqlExpression);

        ResourceCond resCond = new ResourceCond();
        resCond.setResourceName("resource-ldap");
        SearchCond simpleCond = SearchCond.getLeafCond(resCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiqlExpression));
    }

    @Test
    public void entitlements() {
        String fiqlExpression = SyncopeClient.getRoleSearchConditionBuilder().hasEntitlements("USER_LIST").query();
        assertEquals(SpecialAttr.ENTITLEMENTS + "==USER_LIST", fiqlExpression);

        EntitlementCond entCond = new EntitlementCond();
        entCond.setExpression("USER_LIST");
        SearchCond simpleCond = SearchCond.getLeafCond(entCond);

        assertEquals(simpleCond, SearchCondConverter.convert(fiqlExpression));
    }

    @Test
    public void and() {
        String fiqlExpression = SyncopeClient.getUserSearchConditionBuilder().
                is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query();
        assertEquals("fullname==*o*;fullname==*i*", fiqlExpression);

        AttributeCond fullnameLeafCond1 = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond1.setSchema("fullname");
        fullnameLeafCond1.setExpression("%o%");
        AttributeCond fullnameLeafCond2 = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond2.setSchema("fullname");
        fullnameLeafCond2.setExpression("%i%");
        SearchCond andCond = SearchCond.getAndCond(
                SearchCond.getLeafCond(fullnameLeafCond1),
                SearchCond.getLeafCond(fullnameLeafCond2));

        assertEquals(andCond, SearchCondConverter.convert(fiqlExpression));
    }

    @Test
    public void or() {
        String fiqlExpression = SyncopeClient.getUserSearchConditionBuilder().
                is("fullname").equalTo("*o*", "*i*", "*ini").query();
        assertEquals("fullname==*o*,fullname==*i*,fullname==*ini", fiqlExpression);

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

        assertEquals(orCond, SearchCondConverter.convert(fiqlExpression));
    }

}
