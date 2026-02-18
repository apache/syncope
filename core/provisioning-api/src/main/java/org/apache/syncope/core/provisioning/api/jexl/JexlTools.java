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
package org.apache.syncope.core.provisioning.api.jexl;

import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JxltEngine;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * JEXL <a href="http://commons.apache.org/jexl/reference/index.html">reference</a> is available.
 */
@SuppressWarnings({ "squid:S3008", "squid:S3776", "squid:S1141" })
public class JexlTools {

    protected static final Logger LOG = LoggerFactory.getLogger(JexlTools.class);

    protected final JexlEngine jexlEngine;

    protected final JxltEngine jxltEngine;

    public JexlTools(final JexlEngine jexlEngine) {
        this.jexlEngine = jexlEngine;
        this.jxltEngine = jexlEngine.createJxltEngine(false);
    }

    public JexlTools(final JexlEngine jexlEngine, final JxltEngine jxltEngine) {
        this.jexlEngine = jexlEngine;
        this.jxltEngine = jxltEngine;
    }

    public boolean isExpressionValid(final String expression) {
        boolean result;
        try {
            jexlEngine.createExpression(expression);
            result = true;
        } catch (JexlException e) {
            LOG.error("Invalid JEXL expression: {}", expression, e);
            result = false;
        }

        return result;
    }

    public Object evaluateExpression(final String expression, final JexlContext jexlContext) {
        Object result = null;

        if (StringUtils.isNotBlank(expression) && jexlContext != null) {
            try {
                JexlExpression jexlExpression = jexlEngine.createExpression(expression);
                result = jexlExpression.evaluate(jexlContext);
            } catch (Exception e) {
                LOG.error("Error while evaluating JEXL expression: {}", expression, e);
            }
        } else {
            LOG.debug("Expression not provided or invalid context");
        }

        return Optional.ofNullable(result).orElse(StringUtils.EMPTY);
    }

    public String evaluateTemplate(final String template, final JexlContext jexlContext) {
        String result = null;

        if (StringUtils.isNotBlank(template) && jexlContext != null) {
            try {
                StringWriter writer = new StringWriter();
                jxltEngine.createTemplate(template).evaluate(jexlContext, writer);
                result = writer.toString();
            } catch (Exception e) {
                LOG.error("Error while evaluating JEXL template: {}", template, e);
            }
        } else {
            LOG.debug("Template not provided or invalid context");
        }

        return Optional.ofNullable(result).orElse(template);
    }

    @Transactional(readOnly = true)
    public Map<String, String> derAttrs(final Attributable attributable, final DerAttrHandler derAttrHandler) {
        return attributable instanceof Realm realm
                ? derAttrHandler.getValues(realm)
                : attributable instanceof Any any
                        ? derAttrHandler.getValues(any)
                        : Map.of();
    }

    @Transactional(readOnly = true)
    public boolean evaluateMandatoryCondition(
            final String mandatoryCondition,
            final Attributable attributable,
            final DerAttrHandler derAttrHandler) {

        JexlContext jexlContext = new JexlContextBuilder().
                plainAttrs(attributable.getPlainAttrs()).
                derAttrs(derAttrs(attributable, derAttrHandler)).
                build();

        return Boolean.parseBoolean(evaluateExpression(mandatoryCondition, jexlContext).toString());
    }
}
