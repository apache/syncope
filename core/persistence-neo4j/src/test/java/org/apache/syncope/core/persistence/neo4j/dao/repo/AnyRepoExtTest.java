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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AnyRepoExtTest {

    @Test
    void escapeForLikeRegexShouldEscapeBackslash() {
        // Any plain-attribute search value ending with a backslash must have the
        // backslash itself escaped before the regular expression metacharacters are
        // escaped. Otherwise the generated Cypher string literal contains an escaped
        // closing quote and remains unterminated, corrupting the query structure.
        assertEquals("foo\\\\", AnyRepoExt.escapeForLikeRegex("foo\\"));
    }

    @Test
    void escapeForLikeRegexShouldEscapeRegularExpressionMetacharacters() {
        assertEquals("foo", AnyRepoExt.escapeForLikeRegex("foo"));
        assertEquals("f\\.oo", AnyRepoExt.escapeForLikeRegex("f.oo"));
        assertEquals("f\\*o\\*o", AnyRepoExt.escapeForLikeRegex("f*o*o"));
    }
}
