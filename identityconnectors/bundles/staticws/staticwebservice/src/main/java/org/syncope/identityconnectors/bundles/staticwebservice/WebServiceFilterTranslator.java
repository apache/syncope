/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.syncope.identityconnectors.bundles.staticwebservice;

import java.util.HashSet;
import java.util.Set;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.common.StringUtil;
import org.syncope.identityconnectors.bundles.staticwebservice.utilities.Operator;
import org.syncope.identityconnectors.bundles.staticwebservice.utilities.Operand;

/**
 * This is an implementation of AbstractFilterTranslator that gives a concrete representation
 * of which filters can be applied at the connector level (natively). If the 
 * WebService doesn't support a certain expression type, that factory
 * method should return null. This level of filtering is present only to allow any
 * native contructs that may be available to help reduce the result set for the framework,
 * which will (strictly) reapply all filters specified after the connector does the initial
 * filtering.<p><p>Note: The generic query type is most commonly a String, but does not have to be.
 * 
 * @author fabio
 * @version 1.0
 * @since 1.0
 */
public class WebServiceFilterTranslator
        extends AbstractFilterTranslator<Operand> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Operand createContainsExpression(
            ContainsFilter filter, boolean not) {

        if (filter == null) return null;

        String name = filter.getAttribute().getName();
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        }

        return new Operand(Operator.CONTAINS, name, value, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Operand createEndsWithExpression(
            EndsWithFilter filter, boolean not) {

        if (filter == null) return null;

        String name = filter.getAttribute().getName();
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        }

        return new Operand(Operator.ENDS, name, value, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Operand createStartsWithExpression(
            StartsWithFilter filter, boolean not) {

        if (filter == null) return null;

        String name = filter.getAttribute().getName();
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        }

        return new Operand(Operator.STARTS, name, value, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Operand createEqualsExpression(
            EqualsFilter filter, boolean not) {

        if (filter == null) return null;

        String name = filter.getAttribute().getName();
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        }

        return new Operand(Operator.EQ, name, value, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Operand createAndExpression(
            Operand leftExpression, Operand rightExpression) {

        if (leftExpression == null || rightExpression == null) return null;

        Set<Operand> operands = new HashSet<Operand>();
        operands.add(leftExpression);
        operands.add(rightExpression);

        return new Operand(Operator.AND, operands);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Operand createOrExpression(
            Operand leftExpression, Operand rightExpression) {

        if (leftExpression == null || rightExpression == null) return null;

        Set<Operand> operands = new HashSet<Operand>();
        operands.add(leftExpression);
        operands.add(rightExpression);

        return new Operand(Operator.OR, operands);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Operand createGreaterThanExpression(
            GreaterThanFilter filter, boolean not) {

        if (filter == null) return null;

        String name = filter.getAttribute().getName();
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        }

        return new Operand(Operator.GT, name, value, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Operand createGreaterThanOrEqualExpression(
            GreaterThanOrEqualFilter filter, boolean not) {

        if (filter == null) return null;

        String name = filter.getAttribute().getName();
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        }

        return new Operand(Operator.LT, name, value, !not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Operand createLessThanExpression(
            LessThanFilter filter, boolean not) {

        if (filter == null) return null;

        String name = filter.getAttribute().getName();
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        }

        return new Operand(Operator.LT, name, value, not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Operand createLessThanOrEqualExpression(
            LessThanOrEqualFilter filter, boolean not) {

        if (filter == null) return null;

        String name = filter.getAttribute().getName();
        String value = AttributeUtil.getAsStringValue(filter.getAttribute());
        if (StringUtil.isBlank(value)) {
            return null;
        }

        return new Operand(Operator.GT, name, value, !not);
    }
}
