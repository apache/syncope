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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.ListIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.search.ConnObjectTOFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.search.SpecialAttr;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.junit.jupiter.api.Test;

public class FilterConverterTest {

    private boolean equals(final Filter filter1, final Filter filter2) {
        return EqualsBuilder.reflectionEquals(filter1, filter2);
    }

    private boolean equals(final List<Filter> filters1, final List<Filter> filters2) {
        ListIterator<Filter> e1 = filters1.listIterator();
        ListIterator<Filter> e2 = filters2.listIterator();
        while (e1.hasNext() && e2.hasNext()) {
            Filter o1 = e1.next();
            Filter o2 = e2.next();
            if (!equals(o1, o2)) {
                return false;
            }
        }
        return !(e1.hasNext() || e2.hasNext());
    }

    @Test
    public void eq() {
        String fiql = new ConnObjectTOFiqlSearchConditionBuilder().is("username").equalTo("rossini").query();
        assertEquals("username==rossini", fiql);

        Filter filter = FilterBuilder.equalTo(AttributeBuilder.build("username", "rossini"));

        assertTrue(equals(filter, FilterConverter.convert(fiql)));
    }

    @Test
    public void ieq() {
        String fiql = new ConnObjectTOFiqlSearchConditionBuilder().is("username").equalToIgnoreCase("rossini").query();
        assertEquals("username=~rossini", fiql);

        Filter filter = FilterBuilder.equalsIgnoreCase(AttributeBuilder.build("username", "rossini"));

        assertTrue(equals(filter, FilterConverter.convert(fiql)));
    }

    @Test
    public void nieq() {
        String fiql = new ConnObjectTOFiqlSearchConditionBuilder().is("username").notEqualTolIgnoreCase("rossini").
                query();
        assertEquals("username!~rossini", fiql);

        Filter filter = FilterBuilder.not(
                FilterBuilder.equalsIgnoreCase(AttributeBuilder.build("username", "rossini")));
        assertTrue(filter instanceof NotFilter);

        Filter converted = FilterConverter.convert(fiql);
        assertTrue(converted instanceof NotFilter);

        assertTrue(equals(
                ((NotFilter) filter).getFilter(), ((NotFilter) converted).getFilter()));
    }

    @Test
    public void like() {
        String fiql = new ConnObjectTOFiqlSearchConditionBuilder().is("username").equalTo("ros*").query();
        assertEquals("username==ros*", fiql);

        Filter filter = FilterBuilder.startsWith(AttributeBuilder.build("username", "ros"));

        assertTrue(equals(filter, FilterConverter.convert(fiql)));

        fiql = new ConnObjectTOFiqlSearchConditionBuilder().is("username").equalTo("*ini").query();
        assertEquals("username==*ini", fiql);

        filter = FilterBuilder.endsWith(AttributeBuilder.build("username", "ini"));

        assertTrue(equals(filter, FilterConverter.convert(fiql)));

        fiql = new ConnObjectTOFiqlSearchConditionBuilder().is("username").equalTo("r*ini").query();
        assertEquals("username==r*ini", fiql);

        try {
            FilterConverter.convert(fiql);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void ilike() {
        String fiql = new ConnObjectTOFiqlSearchConditionBuilder().is("username").equalToIgnoreCase("ros*").query();
        assertEquals("username=~ros*", fiql);

        try {
            FilterConverter.convert(fiql);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void nilike() {
        String fiql = new ConnObjectTOFiqlSearchConditionBuilder().is("username").notEqualTolIgnoreCase("ros*").query();
        assertEquals("username!~ros*", fiql);

        try {
            FilterConverter.convert(fiql);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void isNull() {
        String fiql = new ConnObjectTOFiqlSearchConditionBuilder().is("loginDate").nullValue().query();
        assertEquals("loginDate==" + SpecialAttr.NULL, fiql);

        Filter filter = FilterBuilder.not(
                FilterBuilder.startsWith(AttributeBuilder.build("loginDate", StringUtils.EMPTY)));

        Filter converted = FilterConverter.convert(fiql);
        assertTrue(converted instanceof NotFilter);

        assertTrue(equals(
                ((NotFilter) filter).getFilter(), ((NotFilter) converted).getFilter()));
    }

    @Test
    public void isNotNull() {
        String fiql = new ConnObjectTOFiqlSearchConditionBuilder().is("loginDate").notNullValue().query();
        assertEquals("loginDate!=" + SpecialAttr.NULL, fiql);

        Filter filter = FilterBuilder.startsWith(AttributeBuilder.build("loginDate", StringUtils.EMPTY));

        assertTrue(equals(filter, FilterConverter.convert(fiql)));
    }

    @Test
    public void inDynRealms() {
        try {
            FilterConverter.convert(SpecialAttr.DYNREALMS + "==realm");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void notInDynRealms() {
        try {
            FilterConverter.convert(SpecialAttr.DYNREALMS + "!=realm");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void hasAuxClasses() {
        try {
            FilterConverter.convert(SpecialAttr.AUX_CLASSES + "==clazz1");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void hasNotAuxClasses() {
        try {
            FilterConverter.convert(SpecialAttr.AUX_CLASSES + "!=clazz1");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void hasResources() {
        try {
            FilterConverter.convert(SpecialAttr.RESOURCES + "==resource");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void hasNotResources() {
        try {
            FilterConverter.convert(SpecialAttr.RESOURCES + "!=resource");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSearchParameters, e.getType());
        }
    }

    @Test
    public void and() {
        String fiql = new ConnObjectTOFiqlSearchConditionBuilder().
                is("fullname").equalTo("ro*").and("fullname").equalTo("*i").query();
        assertEquals("fullname==ro*;fullname==*i", fiql);

        Filter filter1 = FilterBuilder.startsWith(AttributeBuilder.build("fullname", "ro"));
        Filter filter2 = FilterBuilder.endsWith(AttributeBuilder.build("fullname", "i"));

        Filter filter = FilterBuilder.and(filter1, filter2);
        assertTrue(filter instanceof AndFilter);

        Filter converted = FilterConverter.convert(fiql);
        assertTrue(converted instanceof AndFilter);

        assertTrue(equals(
                (List<Filter>) ((AndFilter) filter).getFilters(), (List<Filter>) ((AndFilter) converted).getFilters()));
    }

    @Test
    public void or() {
        String fiql = new ConnObjectTOFiqlSearchConditionBuilder().
                is("fullname").equalTo("ro*").or("fullname").equalTo("*i").query();
        assertEquals("fullname==ro*,fullname==*i", fiql);

        Filter filter1 = FilterBuilder.startsWith(AttributeBuilder.build("fullname", "ro"));
        Filter filter2 = FilterBuilder.endsWith(AttributeBuilder.build("fullname", "i"));

        Filter filter = FilterBuilder.or(filter1, filter2);
        assertTrue(filter instanceof OrFilter);

        Filter converted = FilterConverter.convert(fiql);
        assertTrue(converted instanceof OrFilter);

        assertTrue(equals(
                (List<Filter>) ((OrFilter) filter).getFilters(), (List<Filter>) ((OrFilter) converted).getFilters()));
    }

    @Test
    public void issueSYNCOPE1223() {
        String fiql = new ConnObjectTOFiqlSearchConditionBuilder().is("ctype").equalTo("ou=sample%252Co=isp").query();

        Filter filter = FilterBuilder.equalTo(AttributeBuilder.build("ctype", "ou=sample,o=isp"));

        assertTrue(equals(filter, FilterConverter.convert(fiql)));
    }
}
