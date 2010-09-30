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

package org.syncope.console.commons;

import org.syncope.client.to.LeafSearchCondition.Type;

/**
 * Generic search condition wrapper class.
 */
public class SearchConditionWrapper {

    public enum ConditionType {AND,OR,NOT};

    private ConditionType conditionType;

    private Type type;

    private String schemaName;

    private String schemaValue;

    public ConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ConditionType expressionType) {
        this.conditionType = expressionType;
    }
    
    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getSchemaValue() {
        return schemaValue;
    }

    public void setSchemaValue(String schemaValue) {
        this.schemaValue = schemaValue;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
