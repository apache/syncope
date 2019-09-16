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

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.junit.jupiter.api.Test;

public class SyncopeFiqlParserTest {

    private static final SyncopeFiqlParser<SearchBean> FIQL_PARSER = new SyncopeFiqlParser<>(
            SearchBean.class, AbstractFiqlSearchConditionBuilder.CONTEXTUAL_PROPERTIES);

    @SuppressWarnings("unchecked")
    private static SyncopeFiqlSearchCondition<SearchBean> parse(final String fiql) {
        SearchCondition<SearchBean> parsed = FIQL_PARSER.parse(fiql);
        assertTrue(parsed instanceof SyncopeFiqlSearchCondition);
        return (SyncopeFiqlSearchCondition) parsed;
    }

    @Test
    public void testEqualsIgnoreCase() {
        SyncopeFiqlSearchCondition<SearchBean> cond = parse("name=~ami*");
        assertEquals(SyncopeFiqlParser.IEQ, cond.getOperator());
        assertEquals(ConditionType.CUSTOM, cond.getConditionType());
        assertEquals("ami*", cond.getCondition().get("name"));
    }

    @Test
    public void testNotEqualsIgnoreCase() {
        SyncopeFiqlSearchCondition<SearchBean> cond = parse("name!~ami*");
        assertEquals(SyncopeFiqlParser.NIEQ, cond.getOperator());
        assertEquals(ConditionType.CUSTOM, cond.getConditionType());
        assertEquals("ami*", cond.getCondition().get("name"));
    }

    /**
     * Simple test for ensuring there's no regression.
     */
    @Test
    public void testEquals() {
        SyncopeFiqlSearchCondition<SearchBean> cond = parse("name==ami*");
        assertEquals(FiqlParser.EQ, cond.getOperator());
        assertEquals(ConditionType.EQUALS, cond.getConditionType());
        assertEquals("ami*", cond.getCondition().get("name"));
    }

}
