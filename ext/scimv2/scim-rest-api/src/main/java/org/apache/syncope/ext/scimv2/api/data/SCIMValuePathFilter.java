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
package org.apache.syncope.ext.scimv2.api.data;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;

/**
 * Parser and evaluator for the filter of a SCIM PATCH value path, see
 * <a href="https://www.rfc-editor.org/rfc/rfc7644#section-3.4.2.2">RFC 7644, section 3.4.2.2</a>.
 *
 * Only attribute comparisons ({@code eq}, {@code ne}, {@code co}, {@code sw}, {@code ew}), presence
 * ({@code pr}) and their combination with {@code and} / {@code or} and parentheses are accepted:
 * no expression language is ever evaluated on client-supplied input.
 */
public final class SCIMValuePathFilter {

    private static final int MAX_FILTER_LENGTH = 4096;

    private static final int MAX_TERMS = 64;

    private static final int MAX_DEPTH = 16;

    private static BadRequestException invalid(final String filter) {
        return new BadRequestException(
                ErrorType.invalidFilter, "Invalid SCIM value path filter: " + filter);
    }

    private enum Operator {
        eq {
            @Override
            boolean matches(final String actual, final String expected) {
                return Objects.equals(actual, expected);
            }
        },
        ne {
            @Override
            boolean matches(final String actual, final String expected) {
                return !Objects.equals(actual, expected);
            }
        },
        co {
            @Override
            boolean matches(final String actual, final String expected) {
                return actual != null && actual.contains(expected);
            }
        },
        sw {
            @Override
            boolean matches(final String actual, final String expected) {
                return actual != null && actual.startsWith(expected);
            }
        },
        ew {
            @Override
            boolean matches(final String actual, final String expected) {
                return actual != null && actual.endsWith(expected);
            }
        };

        abstract boolean matches(String actual, String expected);

        static Operator of(final String value) {
            return value == null
                    ? null
                    : Arrays.stream(values()).
                            filter(operator -> operator.name().equals(value.toLowerCase(Locale.ROOT))).
                            findFirst().orElse(null);
        }
    }

    private interface Node {

        boolean matches(Map<String, String> attributes);
    }

    private record Comparison(String attribute, Operator operator, String value) implements Node {

        @Override
        public boolean matches(final Map<String, String> attributes) {
            return operator.matches(attributes.get(attribute), value);
        }
    }

    private record Presence(String attribute) implements Node {

        @Override
        public boolean matches(final Map<String, String> attributes) {
            String value = attributes.get(attribute);
            return value != null && !value.isEmpty();
        }
    }

    private record And(Node left, Node right) implements Node {

        @Override
        public boolean matches(final Map<String, String> attributes) {
            return left.matches(attributes) && right.matches(attributes);
        }
    }

    private record Or(Node left, Node right) implements Node {

        @Override
        public boolean matches(final Map<String, String> attributes) {
            return left.matches(attributes) || right.matches(attributes);
        }
    }

    private static final class Parser {

        private final String filter;

        private final String input;

        private int position;

        private int terms;

        private int depth;

        Parser(final String filter) {
            this.filter = filter;
            this.input = filter;
        }

        Node parse() {
            Node node = parseOr();
            skipWhitespace();
            if (position != input.length()) {
                throw invalid(filter);
            }
            return node;
        }

        private Node parseOr() {
            Node node = parseAnd();
            while (consumeKeyword("or")) {
                node = new Or(node, parseAnd());
            }
            return node;
        }

        private Node parseAnd() {
            Node node = parseAtom();
            while (consumeKeyword("and")) {
                node = new And(node, parseAtom());
            }
            return node;
        }

        private Node parseAtom() {
            skipWhitespace();
            if (position < input.length() && input.charAt(position) == '(') {
                if (depth >= MAX_DEPTH) {
                    throw invalid(filter);
                }
                position++;
                depth++;
                try {
                    Node node = parseOr();
                    skipWhitespace();
                    if (position >= input.length() || input.charAt(position) != ')') {
                        throw invalid(filter);
                    }
                    position++;
                    return node;
                } finally {
                    depth--;
                }
            }

            return parseComparison();
        }

        private Node parseComparison() {
            // SCIM attribute names are case-insensitive (RFC 7644, section 2.2).
            String attribute = readIdentifier().toLowerCase(Locale.ROOT);

            if (consumeKeyword("pr")) {
                countTerm();
                return new Presence(attribute);
            }

            Operator operator = Operator.of(readIdentifier());
            if (operator == null) {
                throw invalid(filter);
            }

            String value = readString();
            countTerm();
            return new Comparison(attribute, operator, value);
        }

        private void countTerm() {
            if (++terms > MAX_TERMS) {
                throw invalid(filter);
            }
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        private boolean consumeKeyword(final String keyword) {
            skipWhitespace();
            int end = position + keyword.length();
            if (end > input.length()
                    || !input.regionMatches(true, position, keyword, 0, keyword.length())
                    || end < input.length() && isIdentifierPart(input.charAt(end))) {

                return false;
            }

            position = end;
            return true;
        }

        private String readIdentifier() {
            skipWhitespace();
            int start = position;
            if (position < input.length() && isIdentifierStart(input.charAt(position))) {
                position++;
                while (position < input.length() && isIdentifierPart(input.charAt(position))) {
                    position++;
                }
            }

            if (start == position) {
                throw invalid(filter);
            }

            return input.substring(start, position);
        }

        private String readString() {
            skipWhitespace();
            if (position >= input.length() || input.charAt(position) != '"') {
                throw invalid(filter);
            }
            position++;

            StringBuilder value = new StringBuilder();
            while (position < input.length()) {
                char c = input.charAt(position++);
                if (c == '"') {
                    return value.toString();
                }
                if (c == '\\') {
                    if (position >= input.length()) {
                        break;
                    }
                    char escaped = input.charAt(position++);
                    if (escaped != '"' && escaped != '\\') {
                        break;
                    }
                    value.append(escaped);
                } else {
                    value.append(c);
                }
            }

            throw invalid(filter);
        }

        private static boolean isIdentifierStart(final char c) {
            return Character.isLetter(c) || c == '_';
        }

        private static boolean isIdentifierPart(final char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '$';
        }
    }

    private final Node root;

    private SCIMValuePathFilter(final Node root) {
        this.root = root;
    }

    public static SCIMValuePathFilter parse(final String filter) {
        if (filter == null) {
            throw invalid(null);
        }
        if (filter.length() > MAX_FILTER_LENGTH) {
            throw invalid("filter too long");
        }

        return new SCIMValuePathFilter(new Parser(filter).parse());
    }

    public static boolean matches(final String filter, final Map<String, String> attributes) {
        return parse(filter).matches(attributes);
    }

    public boolean matches(final Map<String, String> attributes) {
        return root.matches(attributes);
    }
}
