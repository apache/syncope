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
import java.util.Iterator;
import java.util.List;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractAttrValue;

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
        boolean result = true;
        try {
            jexlEngine.createExpression(expression);
        } catch (JexlException e) {
            LOG.error("Invalid jexl expression: " + expression, e);
            result = false;
        }
        return result;
    }

    public String evaluateWithAttributes(final String expression,
            final Collection<? extends AbstractAttr> attributes) {

        if (expression == null || expression.isEmpty()) {
            return "";
        }

        JexlContext jexlContext = new MapContext();

        List<String> attributeValues;
        String expressionValue;
        AbstractAttr attribute;
        for (Iterator<? extends AbstractAttr> itor =
                attributes.iterator(); itor.hasNext();) {

            attribute = itor.next();
            attributeValues = attribute.getValuesAsStrings();
            if (attributeValues.isEmpty()) {
                expressionValue = "";
            } else {
                expressionValue = attributeValues.iterator().next();
            }

            LOG.debug("Add attribute {} with value {}",
                    new Object[]{attribute.getSchema().getName(),
                        expressionValue
                    });

            jexlContext.set(attribute.getSchema().getName(), expressionValue);
        }

        String result;
        try {
            Expression jexlExpression = jexlEngine.createExpression(expression);
            result = jexlExpression.evaluate(jexlContext).toString();
        } catch (JexlException e) {
            LOG.error("Invalid jexl expression: " + expression, e);
            result = "";
        }

        return result;
    }
}
