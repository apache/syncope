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

import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.junit.jupiter.api.Test;

public class SCIMFilterTest {

    @Test
    public void eq() {
        SearchCond cond = SearchCondConverter.convert("userName eq \"bjensen\"");
        assertNotNull(cond);
        assertNotNull(cond.getAnyCond());
        assertEquals("username", cond.getAnyCond().getSchema());
        assertEquals(AttributeCond.Type.IEQ, cond.getAnyCond().getType());
        assertEquals("bjensen", cond.getAnyCond().getExpression());
    }

    @Test
    public void sw() {
        SearchCond cond = SearchCondConverter.convert("userName sw \"J\"");
        assertNotNull(cond);
        assertNotNull(cond.getAnyCond());
        assertEquals("username", cond.getAnyCond().getSchema());
        assertEquals(AttributeCond.Type.ILIKE, cond.getAnyCond().getType());
        assertEquals("J%", cond.getAnyCond().getExpression());

        SearchCond fqn = SearchCondConverter.convert("urn:ietf:params:scim:schemas:core:2.0:User:userName sw \"J\"");
        assertEquals(cond, fqn);
    }

    @Test
    public void pr() {
        SearchCond cond = SearchCondConverter.convert("title pr");
        assertNotNull(cond);
        assertNotNull(cond.getAttributeCond());
        assertEquals("title", cond.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.ISNOTNULL, cond.getAttributeCond().getType());
        assertNull(cond.getAttributeCond().getExpression());
    }

    @Test
    public void gt() {
        SearchCond cond = SearchCondConverter.convert("meta.lastModified gt \"2011-05-13T04:42:34Z\"");
        assertNotNull(cond);
        assertNotNull(cond.getAnyCond());
        assertEquals("lastChangeDate", cond.getAnyCond().getSchema());
        assertEquals(AttributeCond.Type.GT, cond.getAnyCond().getType());
        assertEquals("2011-05-13T04:42:34Z", cond.getAnyCond().getExpression());
    }

    @Test
    public void not() {
        SearchCond cond = SearchCondConverter.convert("not (title pr)");
        assertNotNull(cond);
        assertNotNull(cond.getAttributeCond());
        assertEquals("title", cond.getAttributeCond().getSchema());
        assertEquals(AttributeCond.Type.ISNULL, cond.getAttributeCond().getType());
        assertNull(cond.getAttributeCond().getExpression());
    }

    @Test
    public void and() {
        SearchCond cond = SearchCondConverter.convert("title pr and userName sw \"J\"");
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
        SearchCond cond = SearchCondConverter.convert("title pr or displayName eq \"Other\"");
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
        assertNotNull(right.getAnyCond());
        assertEquals("name", right.getAnyCond().getSchema());
        assertEquals(AttributeCond.Type.IEQ, right.getAnyCond().getType());
        assertEquals("Other", right.getAnyCond().getExpression());
    }
}
