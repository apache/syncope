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
package org.apache.syncope.core.persistence.common.dao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.parser.ParserConstants;
import org.apache.commons.jexl3.parser.Token;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class AnyFinder {

    protected static final Logger LOG = LoggerFactory.getLogger(AnyFinder.class);

    protected static final Comparator<String> LITERAL_COMPARATOR = (l1, l2) -> {
        if (l1 == null && l2 == null) {
            return 0;
        } else if (l1 != null && l2 == null) {
            return -1;
        } else if (l1 == null) {
            return 1;
        } else if (l1.length() == l2.length()) {
            return 0;
        } else if (l1.length() > l2.length()) {
            return -1;
        } else {
            return 1;
        }
    };

    /**
     * Split an attribute value recurring on provided literals/tokens.
     *
     * @param attrValue value to be split
     * @param literals literals/tokens
     * @return split value
     */
    protected static List<String> split(final String attrValue, final List<String> literals) {
        final List<String> attrValues = new ArrayList<>();

        if (literals.isEmpty()) {
            attrValues.add(attrValue);
        } else {
            for (String token : attrValue.split(Pattern.quote(literals.getFirst()))) {
                if (!token.isEmpty()) {
                    attrValues.addAll(split(token, literals.subList(1, literals.size())));
                }
            }
        }

        return attrValues;
    }

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final AnySearchDAO anySearchDAO;

    public AnyFinder(final PlainSchemaDAO plainSchemaDAO, final AnySearchDAO anySearchDAO) {
        this.plainSchemaDAO = plainSchemaDAO;
        this.anySearchDAO = anySearchDAO;
    }

    @Transactional(readOnly = true)
    public <A extends Any> List<A> findByDerAttrValue(
            final AnyTypeKind anyTypeKind,
            final String expression,
            final String value,
            final boolean ignoreCaseMatch) {

        Parser parser = new Parser(expression);

        // Schema keys
        List<String> identifiers = new ArrayList<>();

        // Literals
        List<String> literals = new ArrayList<>();

        // Get schema keys and literals
        for (Token token = parser.getNextToken(); token != null && StringUtils.isNotBlank(token.toString());
                token = parser.getNextToken()) {

            if (token.kind == ParserConstants.STRING_LITERAL) {
                literals.add(token.toString().substring(1, token.toString().length() - 1));
            }

            if (token.kind == ParserConstants.IDENTIFIER) {
                identifiers.add(token.toString());
            }
        }

        // Sort literals in order to process later literals included into others
        literals.sort(LITERAL_COMPARATOR);

        // Split value on provided literals
        List<String> attrValues = split(value, literals);

        if (attrValues.size() != identifiers.size()) {
            LOG.error("Ambiguous JEXL expression resolution: literals and values have different size");
            return List.of();
        }

        List<SearchCond> andConditions = new ArrayList<>();

        // Contains used identifiers in order to avoid replications
        Set<String> used = new HashSet<>();

        // Create several clauses: one for eanch identifiers
        for (int i = 0; i < identifiers.size() && !used.contains(identifiers.get(i)); i++) {
            used.add(identifiers.get(i));

            AttrCond cond = plainSchemaDAO.findById(identifiers.get(i)).
                    map(schema -> new AttrCond()).
                    orElseGet(() -> new AnyCond());
            cond.setType(ignoreCaseMatch ? AttrCond.Type.IEQ : AttrCond.Type.EQ);
            cond.setSchema(identifiers.get(i));
            cond.setExpression(attrValues.get(i));
            andConditions.add(SearchCond.of(cond));
        }

        LOG.debug("Generated search {} conditions: {}", anyTypeKind, andConditions);

        return anySearchDAO.search(SearchCond.and(andConditions), anyTypeKind);
    }
}
