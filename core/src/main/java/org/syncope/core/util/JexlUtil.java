/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.util;

import java.util.Collection;
import java.util.List;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.user.SyncopeUser;

/**
 * @see http://commons.apache.org/jexl/reference/index.html
 */
public class JexlUtil {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JexlUtil.class);

    @Autowired
    private JexlEngine jexlEngine;

    public boolean isExpressionValid(final String expression) {
        boolean result;
        try {
            jexlEngine.createExpression(expression);
            result = true;
        } catch (JexlException e) {
            LOG.error("Invalid jexl expression: " + expression, e);
            result = false;
        }

        return result;
    }

    public String evaluate(final String expression,
            final JexlContext jexlContext) {

        String result = "";

        if (expression != null
                && !expression.isEmpty() && jexlContext != null) {

            try {
                Expression jexlExpression =
                        jexlEngine.createExpression(expression);
                Object evaluated = jexlExpression.evaluate(jexlContext);
                if (evaluated != null) {
                    result = evaluated.toString();
                }
            } catch (JexlException e) {
                LOG.error("Invalid jexl expression: " + expression, e);
                result = "";
            }
        } else {
            LOG.debug("Expression not provided or invalid context");
        }

        return result;
    }

    public String evaluate(final String expression,
            final AbstractAttributable attributable) {

        final JexlContext jexlContext = new MapContext();

        if (attributable instanceof SyncopeUser) {
            SyncopeUser user = (SyncopeUser) attributable;

            jexlContext.set("username",
                    user.getUsername() != null
                    ? user.getUsername()
                    : "");
            jexlContext.set("creationDate",
                    user.getCreationDate() != null
                    ? user.getDateFormatter().format(user.getCreationDate())
                    : "");
            jexlContext.set("lastLoginDate",
                    user.getLastLoginDate() != null
                    ? user.getDateFormatter().format(user.getLastLoginDate())
                    : "");
            jexlContext.set("failedLogins",
                    user.getFailedLogins() != null
                    ? user.getFailedLogins()
                    : "");
            jexlContext.set("changePwdDate",
                    user.getChangePwdDate() != null
                    ? user.getDateFormatter().format(user.getChangePwdDate())
                    : "");
        }

        addAttrsToContext(
                attributable.getAttributes(),
                jexlContext);

        addDerAttrsToContext(
                attributable.getDerivedAttributes(),
                attributable.getAttributes(),
                jexlContext);

        // Evaluate expression using the context prepared before
        return evaluate(expression, jexlContext);
    }

    public JexlContext addAttrsToContext(
            final Collection<? extends AbstractAttr> attributes,
            final JexlContext jexlContext) {

        JexlContext context = jexlContext == null
                ? new MapContext() : jexlContext;

        for (AbstractAttr attribute : attributes) {
            List<String> attributeValues = attribute.getValuesAsStrings();
            String expressionValue = attributeValues.isEmpty()
                    ? "" : attributeValues.get(0);

            LOG.debug("Add attribute {} with value {}",
                    new Object[]{attribute.getSchema().getName(),
                        expressionValue});

            context.set(attribute.getSchema().getName(), expressionValue);
        }

        return context;
    }

    public JexlContext addDerAttrsToContext(
            final Collection<? extends AbstractDerAttr> derAttributes,
            final Collection<? extends AbstractAttr> attributes,
            final JexlContext jexlContext) {

        JexlContext context = jexlContext == null
                ? new MapContext() : jexlContext;

        for (AbstractDerAttr attribute : derAttributes) {
            String expressionValue = attribute.getValue(attributes);
            if (expressionValue == null) {
                expressionValue = "";
            }

            LOG.debug("Add derived attribute {} with value {}",
                    new Object[]{attribute.getDerivedSchema().getName(),
                        expressionValue});

            context.set(
                    attribute.getDerivedSchema().getName(), expressionValue);
        }

        return context;
    }

    public String evaluate(final String expression,
            final AbstractAttributableTO attributableTO) {

        final JexlContext context = new MapContext();

        if (attributableTO instanceof UserTO) {
            UserTO user = (UserTO) attributableTO;

            context.set("username",
                    user.getUsername() != null
                    ? user.getUsername()
                    : "");
            context.set("password",
                    user.getPassword() != null
                    ? user.getPassword()
                    : "");
        }

        for (AttributeTO attribute : attributableTO.getAttributes()) {
            List<String> attributeValues = attribute.getValues();
            String expressionValue = attributeValues.isEmpty()
                    ? "" : attributeValues.get(0);

            LOG.debug("Add attribute {} with value {}",
                    new Object[]{attribute.getSchema(), expressionValue});

            context.set(attribute.getSchema(), expressionValue);
        }
        for (AttributeTO attribute : attributableTO.getDerivedAttributes()) {
            List<String> attributeValues = attribute.getValues();
            String expressionValue = attributeValues.isEmpty()
                    ? "" : attributeValues.get(0);

            LOG.debug("Add attribute {} with value {}",
                    new Object[]{attribute.getSchema(), expressionValue});

            context.set(attribute.getSchema(), expressionValue);
        }
        for (AttributeTO attribute : attributableTO.getVirtualAttributes()) {
            List<String> attributeValues = attribute.getValues();
            String expressionValue = attributeValues.isEmpty()
                    ? "" : attributeValues.get(0);

            LOG.debug("Add attribute {} with value {}",
                    new Object[]{attribute.getSchema(), expressionValue});

            context.set(attribute.getSchema(), expressionValue);
        }

        // Evaluate expression using the context prepared before
        return evaluate(expression, context);
    }
}
