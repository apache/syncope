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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.junit.jupiter.api.Test;

class SyncopeFiqlParserTest {

    public static class TestBean {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    private static final SyncopeFiqlParser<SearchBean> FIQL_PARSER = new SyncopeFiqlParser<>(
            SearchBean.class, AbstractFiqlSearchConditionBuilder.CONTEXTUAL_PROPERTIES);

    private static SyncopeFiqlSearchCondition<SearchBean> parse(final String fiql) {
        SearchCondition<SearchBean> parsed = FIQL_PARSER.parse(fiql);
        assertTrue(parsed instanceof SyncopeFiqlSearchCondition);
        return (SyncopeFiqlSearchCondition<SearchBean>) parsed;
    }

    @Test
    void equalsIgnoreCase() {
        SyncopeFiqlSearchCondition<SearchBean> cond = parse("name=~ami*");
        assertEquals(SyncopeFiqlParser.IEQ, cond.getOperator());
        assertEquals(ConditionType.CUSTOM, cond.getConditionType());
        assertEquals("ami*", cond.getCondition().get("name"));
    }

    @Test
    void notEqualsIgnoreCase() {
        SyncopeFiqlSearchCondition<SearchBean> cond = parse("name!~ami*");
        assertEquals(SyncopeFiqlParser.NIEQ, cond.getOperator());
        assertEquals(ConditionType.CUSTOM, cond.getConditionType());
        assertEquals("ami*", cond.getCondition().get("name"));
    }

    /**
     * Simple test for ensuring there's no regression.
     */
    @Test
    void equals() {
        SyncopeFiqlSearchCondition<SearchBean> cond = parse("name==ami*");
        assertEquals(FiqlParser.EQ, cond.getOperator());
        assertEquals(ConditionType.EQUALS, cond.getConditionType());
        assertEquals("ami*", cond.getCondition().get("name"));
    }

    @Test
    void notEquals() {
        SyncopeFiqlSearchCondition<SearchBean> cond = parse("name!=ami*");
        assertEquals(FiqlParser.NEQ, cond.getOperator());
        assertEquals(ConditionType.NOT_EQUALS, cond.getConditionType());
    }

    @Test
    void comparators() {
        assertEquals(ConditionType.GREATER_THAN, parse("age=gt=10").getConditionType());
        assertEquals(ConditionType.GREATER_OR_EQUALS, parse("age=ge=10").getConditionType());
        assertEquals(ConditionType.LESS_THAN, parse("age=lt=10").getConditionType());
        assertEquals(ConditionType.LESS_OR_EQUALS, parse("age=le=10").getConditionType());
    }

    @Test
    void and() {
        SearchCondition<SearchBean> parsed = FIQL_PARSER.parse("name==a;key==b");

        assertEquals(ConditionType.AND, parsed.getConditionType());
        assertEquals(2, parsed.getSearchConditions().size());
    }

    @Test
    void or() {
        SearchCondition<SearchBean> parsed = FIQL_PARSER.parse("name==a,key==b");

        assertEquals(ConditionType.OR, parsed.getConditionType());
        assertEquals(2, parsed.getSearchConditions().size());
    }

    @Test
    void missingValue() {
        assertThrows(SearchParseException.class, () -> FIQL_PARSER.parse("name=="));
    }

    @Test
    void notAComparison() {
        assertThrows(SearchParseException.class, () -> FIQL_PARSER.parse("name"));
    }

    @Test
    void bean() {
        SyncopeFiqlParser<TestBean> parser = new SyncopeFiqlParser<>(
                TestBean.class, AbstractFiqlSearchConditionBuilder.CONTEXTUAL_PROPERTIES);

        SearchCondition<TestBean> parsed = parser.parse("name=~ami*");

        assertTrue(parsed instanceof SyncopeFiqlSearchCondition);
        assertEquals(SyncopeFiqlParser.IEQ, ((SyncopeFiqlSearchCondition<TestBean>) parsed).getOperator());
        assertEquals(ConditionType.CUSTOM, parsed.getConditionType());
    }
}
