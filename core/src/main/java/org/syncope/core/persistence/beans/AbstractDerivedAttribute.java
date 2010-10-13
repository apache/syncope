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
package org.syncope.core.persistence.beans;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.MapContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.persistence.util.ApplicationContextManager;

/**
 * @see http://commons.apache.org/jexl/reference/index.html
 */
@MappedSuperclass
public abstract class AbstractDerivedAttribute extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    public Long getId() {
        return id;
    }

    /**
     * @see http://commons.apache.org/jexl/reference/index.html
     * @param attributes the set of attributes against which evaluate this
     * derived attribute
     * @return the value of this derived attribute
     */
    public String getValue(
            final Collection<? extends AbstractAttribute> attributes) {

        JexlContext jexlContext = new MapContext();

        List<? extends AbstractAttributeValue> attributeValues = null;
        String expressionValue = null;
        AbstractAttribute attribute = null;
        AbstractAttributeValue attributeValue = null;
        for (Iterator<? extends AbstractAttribute> itor =
                attributes.iterator(); itor.hasNext();) {

            attribute = itor.next();
            attributeValues = attribute.getValues();
            if (attributeValues.isEmpty()
                    || !getDerivedSchema().getSchemas().contains(
                    attribute.getSchema())) {

                expressionValue = "";
            } else {
                attributeValue = attributeValues.iterator().next();
                expressionValue = attributeValue.getValueAsString();
            }

            jexlContext.set(attribute.getSchema().getName(), expressionValue);
        }

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();
        JexlEngine jexlEngine = (JexlEngine) context.getBean("jexlEngine");
        String result = null;
        try {
            Expression jexlExpression = jexlEngine.createExpression(
                    getDerivedSchema().getExpression());
            result = jexlExpression.evaluate(jexlContext).toString();
        } catch (JexlException e) {
            LOG.error("Invalid jexl expression: "
                    + getDerivedSchema().getExpression(), e);
        }

        return result;
    }

    public abstract <T extends AbstractAttributable> T getOwner();

    public abstract <T extends AbstractAttributable> void setOwner(T owner);

    public abstract <T extends AbstractDerivedSchema> T getDerivedSchema();

    public abstract <T extends AbstractDerivedSchema> void setDerivedSchema(
            T derivedSchema);
}
