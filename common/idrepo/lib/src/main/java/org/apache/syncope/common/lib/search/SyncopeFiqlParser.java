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

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

/**
 * This parser introduces 2 new operands {@link #IEQ} (case-insensitive equals) and {@link #NIEQ} (case-insensitive
 * not equals) to the native FIQL operands.
 *
 * @param <T> type of search condition.
 */
public class SyncopeFiqlParser<T> extends FiqlParser<T> {

    public static final String IEQ = "=~";

    public static final String NIEQ = "!~";

    public SyncopeFiqlParser(
            final Class<T> tclass,
            final Map<String, String> contextProperties) {

        this(tclass, contextProperties, null);
    }

    public SyncopeFiqlParser(
            final Class<T> tclass,
            final Map<String, String> contextProperties,
            final Map<String, String> beanProperties) {

        super(tclass, contextProperties, beanProperties);

        operatorsMap.put(IEQ, ConditionType.CUSTOM);
        operatorsMap.put(NIEQ, ConditionType.CUSTOM);

        CONDITION_MAP.put(ConditionType.CUSTOM, IEQ);
        CONDITION_MAP.put(ConditionType.CUSTOM, NIEQ);

        String comparators = GT + '|' + GE + '|' + LT + '|' + LE + '|' + EQ + '|' + NEQ + '|' + IEQ + '|' + NIEQ;
        String s1 = "[\\p{ASCII}]+(" + comparators + ')';
        comparatorsPattern = Pattern.compile(s1);
    }

    @Override
    protected ASTNode<T> parseComparison(final String expr) throws SearchParseException {
        Matcher m = comparatorsPattern.matcher(expr);
        if (m.find()) {
            String propertyName = expr.substring(0, m.start(1));
            String operator = m.group(1);
            String value = expr.substring(m.end(1));
            if (StringUtils.isBlank(value)) {
                throw new SearchParseException("Not a comparison expression: " + expr);
            }

            String name = getActualSetterName(unwrapSetter(propertyName));
            return Optional.ofNullable(parseType(propertyName, name, value)).
                    map(typeInfoObject -> new SyncopeComparison(name, operator, typeInfoObject)).
                    orElse(null);
        }

        throw new SearchParseException("Not a comparison expression: " + expr);
    }

    private class SyncopeComparison implements ASTNode<T> {

        private final String name;

        private final String operator;

        private final TypeInfoObject tvalue;

        SyncopeComparison(final String name, final String operator, final TypeInfoObject value) {
            this.name = name;
            this.operator = operator;
            this.tvalue = value;
        }

        @Override
        public String toString() {
            return name + ' ' + operator + ' ' + tvalue.getObject()
                    + " (" + tvalue.getObject().getClass().getSimpleName() + ')';
        }

        @Override
        public SearchCondition<T> build() throws SearchParseException {
            String templateName = getSetter(name);
            T cond = createTemplate(templateName);
            ConditionType ct = operatorsMap.get(operator);

            if (isPrimitive(cond)) {
                return new SyncopeFiqlSearchCondition<>(ct, cond);
            } else {
                String templateNameLCase = templateName.toLowerCase();
                return new SyncopeFiqlSearchCondition<>(Map.of(templateNameLCase, ct),
                        Map.of(templateNameLCase, name),
                        Map.of(templateNameLCase, tvalue.getTypeInfo()),
                        cond, operator);
            }
        }

        private boolean isPrimitive(final T pojo) {
            return pojo.getClass().getName().startsWith("java.lang");
        }

        @SuppressWarnings("unchecked")
        private T createTemplate(final String setter) throws SearchParseException {
            try {
                if (beanspector != null) {
                    beanspector.instantiate().setValue(setter, tvalue.getObject());
                    return beanspector.getBean();
                } else {
                    SearchBean bean = (SearchBean) conditionClass.getDeclaredConstructor().newInstance();
                    bean.set(setter, tvalue.getObject().toString());
                    return (T) bean;
                }
            } catch (Throwable e) {
                throw new SearchParseException(e);
            }
        }
    }
}
