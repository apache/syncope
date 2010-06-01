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
package org.syncope.core.beans;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
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
public class UserDerivedAttribute implements Serializable {

    private static final JexlEngine jexlEngine = new JexlEngine();

    static {
        jexlEngine.setCache(512);
        jexlEngine.setLenient(false);
        jexlEngine.setSilent(false);
    }
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    @ManyToOne
    private SyncopeUser owner;
    @ManyToOne(fetch = FetchType.EAGER)
    private UserDerivedAttributeSchema schema;

    /**
     * @see http://commons.apache.org/jexl/reference/index.html
     * @return
     */
    public String getValue() {
        Expression jexlExpression = jexlEngine.createExpression(
                schema.getExpression());
        JexlContext jexlContext = new MapContext();

        Set<UserAttribute> attributes = owner.getAttributes();
        Set<UserAttributeValue> attributeValues = null;
        String expressionValue = null;
        UserAttributeValue userAttributeValue = null;
        for (UserAttribute attribute : attributes) {
            attributeValues = attribute.getValues();
            if (attributeValues.isEmpty()) {
                expressionValue = "";
            } else {
                userAttributeValue = attributeValues.iterator().next();
                switch (attribute.getSchema().getType()) {
                    case Boolean:
                        expressionValue =
                                ((UserAttributeValueAsBoolean) userAttributeValue).getActualValue().toString();
                        break;
                    case Date:
                        expressionValue = attribute.getSchema().getFormatter(
                                SimpleDateFormat.class).format(
                                ((UserAttributeValueAsDate) userAttributeValue).getActualValue());
                        break;
                    case Double:
                        expressionValue = attribute.getSchema().getFormatter(
                                DecimalFormat.class).format(
                                ((UserAttributeValueAsDouble) userAttributeValue).getActualValue());
                        break;
                    case Long:
                        expressionValue = attribute.getSchema().getFormatter(
                                DecimalFormat.class).format(
                                ((UserAttributeValueAsDouble) userAttributeValue).getActualValue());
                        break;
                    case String:
                        expressionValue =
                                ((UserAttributeValueAsString) userAttributeValue).getActualValue();
                        break;
                }
            }

            jexlContext.set(attribute.getSchema().getName(), expressionValue);
        }

        return jexlExpression.evaluate(jexlContext).toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SyncopeUser getOwner() {
        return owner;
    }

    public void setOwner(SyncopeUser owner) {
        this.owner = owner;
    }

    public UserDerivedAttributeSchema getSchema() {
        return schema;
    }

    public void setSchema(UserDerivedAttributeSchema attributeSchema) {
        this.schema = attributeSchema;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UserDerivedAttribute other = (UserDerivedAttribute) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {

            return false;
        }
        if (this.owner != other.owner
                && (this.owner == null || !this.owner.equals(other.owner))) {

            return false;
        }
        if (this.schema != other.schema
                && (this.schema == null
                || !this.schema.equals(other.schema))) {

            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 59 * hash + (this.owner != null ? this.owner.hashCode() : 0);
        hash = 59 * hash + (this.schema != null
                ? this.schema.hashCode() : 0);

        return hash;
    }

    @Override
    public String toString() {
        return "("
                + "id=" + getId() + ","
                + "owner=" + getOwner().getId() + ","
                + "schema=" + getSchema()
                + ")";
    }
}
