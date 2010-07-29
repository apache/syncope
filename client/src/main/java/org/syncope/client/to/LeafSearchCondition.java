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
package org.syncope.client.to;

import org.syncope.client.AbstractBaseBean;

public class LeafSearchCondition extends AbstractBaseBean {

    public enum Type {

        LIKE, EQ, GT, LT, GE, LE
    }
    private Type type;
    private String schema;
    private String expression;

    public LeafSearchCondition() {
    }

    public LeafSearchCondition(Type type) {
        this.type = type;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean checkValidity() {
        if (type == null) {
            return false;
        }

        return schema != null && expression != null;
    }
}
