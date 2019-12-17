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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts SCIM filter expressions to Syncope's {@link SearchCond}.
 */
public final class SearchCondConverter {

    private static final Logger LOG = LoggerFactory.getLogger(SearchCondConverter.class);

    public static SearchCond convert(final SearchCondVisitor visitor, final String filter) {
        SCIMFilterParser parser = new SCIMFilterParser(
                new CommonTokenStream(new SCIMFilterLexer(CharStreams.fromString(filter))));
        parser.setBuildParseTree(true);
        parser.setTrimParseTree(true);
        parser.setProfile(true);
        parser.removeErrorListeners();
        parser.setErrorHandler(new SCIMFilterErrorHandler());

        try {
            return visitor.visit(parser.scimFilter());
        } catch (Exception e) {
            LOG.error("Could not parse '{}'", filter, e);
            throw new BadRequestException(ErrorType.invalidFilter, "Could not parse '" + filter + '\'');
        }
    }

    private SearchCondConverter() {
        // empty constructor for static utility class        
    }
}
