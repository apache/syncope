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

import jakarta.validation.ValidationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.parser.ParserConstants;
import org.apache.commons.jexl3.parser.Token;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSearchDAO {

    public record CheckResult<C extends AttrCond>(PlainSchema schema, PlainAttrValue value, C cond) {

    }

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSearchDAO.class);

    protected static final String ALWAYS_FALSE_CLAUSE = "1=2";

    public static String key(final AttrSchemaType schemaType) {
        return switch (schemaType) {
            case Boolean ->
                "booleanValue";

            case Date ->
                "dateValue";

            case Double ->
                "doubleValue";

            case Long ->
                "longValue";

            case Binary ->
                "binaryValue";

            default ->
                "stringValue";
        };
    }

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

    protected static Supplier<SyncopeClientException> syncopeClientException(final String message) {
        return () -> {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
            sce.getElements().add(message);
            return sce;
        };
    }

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final EntityFactory entityFactory;

    protected final PlainAttrValidationManager validator;

    protected AbstractSearchDAO(
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final PlainAttrValidationManager validator) {

        this.plainSchemaDAO = plainSchemaDAO;
        this.entityFactory = entityFactory;
        this.validator = validator;
    }

    protected List<SearchCond> buildDerAttrValueConditions(
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

        List<SearchCond> conditions = new ArrayList<>();

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
            conditions.add(SearchCond.of(cond));
        }

        return conditions;
    }

    protected CheckResult<AttrCond> check(final AttrCond cond) {
        PlainSchema schema = plainSchemaDAO.findById(cond.getSchema()).
                orElseThrow(() -> new IllegalArgumentException("Invalid schema " + cond.getSchema()));

        PlainAttrValue attrValue = new PlainAttrValue();

        if (AttrSchemaType.Encrypted == schema.getType()) {
            throw new IllegalArgumentException("Cannot search by encrypted schema " + cond.getSchema());
        }

        try {
            if (cond.getType() != AttrCond.Type.LIKE
                    && cond.getType() != AttrCond.Type.ILIKE
                    && cond.getType() != AttrCond.Type.ISNULL
                    && cond.getType() != AttrCond.Type.ISNOTNULL) {

                validator.validate(schema, cond.getExpression(), attrValue);
            }
        } catch (ValidationException e) {
            throw new IllegalArgumentException("Could not validate expression " + cond.getExpression());
        }

        return new CheckResult<>(schema, attrValue, cond);
    }

    protected CheckResult<AnyCond> check(final AnyCond cond, final Field field, final Set<String> relationshipsFields) {
        AnyCond computed = new AnyCond(cond.getType());
        computed.setSchema(cond.getSchema());
        computed.setExpression(cond.getExpression());

        // Keeps track of difference between entity's getKey() and @Id fields
        if ("key".equals(computed.getSchema())) {
            computed.setSchema("id");
        }

        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setKey(field.getName());
        for (AttrSchemaType attrSchemaType : AttrSchemaType.values()) {
            if (field.getType().isAssignableFrom(attrSchemaType.getType())) {
                schema.setType(attrSchemaType);
            }
        }
        if (schema.getType() == null || schema.getType() == AttrSchemaType.Dropdown) {
            schema.setType(AttrSchemaType.String);
        }

        // Deal with any Integer fields logically mapping to boolean values
        boolean foundBooleanMin = false;
        boolean foundBooleanMax = false;
        if (Integer.class.equals(field.getType())) {
            for (Annotation annotation : field.getAnnotations()) {
                if (Min.class.equals(annotation.annotationType())) {
                    foundBooleanMin = ((Min) annotation).value() == 0;
                } else if (Max.class.equals(annotation.annotationType())) {
                    foundBooleanMax = ((Max) annotation).value() == 1;
                }
            }
        }
        if (foundBooleanMin && foundBooleanMax) {
            schema.setType(AttrSchemaType.Boolean);
        }

        // Deal with fields representing relationships to other entities
        if (relationshipsFields.contains(computed.getSchema())) {
            computed.setSchema(computed.getSchema() + "_id");
            schema.setType(AttrSchemaType.String);
        }

        PlainAttrValue attrValue = new PlainAttrValue();
        if (computed.getType() != AttrCond.Type.LIKE
                && computed.getType() != AttrCond.Type.ILIKE
                && computed.getType() != AttrCond.Type.ISNULL
                && computed.getType() != AttrCond.Type.ISNOTNULL) {

            try {
                validator.validate(schema, computed.getExpression(), attrValue);
            } catch (ValidationException e) {
                LOG.error("Could not validate expression {}", computed.getExpression(), e);
                throw new IllegalArgumentException("Could not validate expression " + computed.getExpression());
            }
        }

        return new CheckResult<>(schema, attrValue, computed);
    }
}
