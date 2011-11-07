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

import java.io.Serializable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.AttributeCond.Type;

/**
 * Generic search condition wrapper class.
 */
public class SearchCondWrapper implements Serializable {

    private static final long serialVersionUID = -5828622221257732958L;

    public enum OperationType {

        AND,
        OR

    };

    public enum FilterType {

        ATTRIBUTE,
        MEMBERSHIP,
        RESOURCE

    };

    private boolean notOperator;

    private OperationType operationType = null;

    private Type type;

    private FilterType filterType;

    private String filterName;

    private String filterValue;

    public boolean isNotOperator() {
        return notOperator;
    }

    public void setNotOperator(boolean notOperator) {
        this.notOperator = notOperator;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public AttributeCond.Type getType() {
        return type;
    }

    public void setType(AttributeCond.Type type) {
        this.type = type;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }
}
