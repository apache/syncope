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

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;

/**
 * @see http://commons.apache.org/jexl/reference/index.html
 */
@Entity
public class DerivedAttribute extends AbstractBaseBean {

    private static final JexlEngine jexlEngine = new JexlEngine();

    static {
        jexlEngine.setCache(512);
        jexlEngine.setLenient(false);
        jexlEngine.setSilent(false);
    }
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    private DerivedAttributeSchema schema;

    /**
     * @see http://commons.apache.org/jexl/reference/index.html
     * @return
     */
    public String getValue(Set<Attribute> attributes) {
        Expression jexlExpression = jexlEngine.createExpression(
                schema.getExpression());
        JexlContext jexlContext = new MapContext();

        Set<AttributeValue> attributeValues = null;
        String expressionValue = null;
        AttributeValue attributeValue = null;
        for (Attribute attribute : attributes) {
            attributeValues = attribute.getValues();
            if (attributeValues.isEmpty()
                    || !schema.getAttributeSchemas().contains(
                    attribute.getSchema())) {

                expressionValue = "";
            } else {
                attributeValue = attributeValues.iterator().next();
                expressionValue = attributeValue.getValue();
            }

            jexlContext.set(attribute.getSchema().getName(), expressionValue);
        }

        return jexlExpression.evaluate(jexlContext).toString();
    }

    public Long getId() {
        return id;
    }

    public DerivedAttributeSchema getSchema() {
        return schema;
    }

    public void setSchema(DerivedAttributeSchema attributeSchema) {
        this.schema = attributeSchema;
    }
}
