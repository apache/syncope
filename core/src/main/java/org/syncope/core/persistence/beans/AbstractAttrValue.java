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

import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.syncope.core.persistence.validation.entity.AttrValueCheck;

@MappedSuperclass
@AttrValueCheck
public abstract class AbstractAttrValue extends AbstractBaseBean {

    private String stringValue;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateValue;

    @Basic
    @Min(0)
    @Max(1)
    private Integer booleanValue;

    private Long longValue;

    private Double doubleValue;

    public abstract Long getId();

    public Boolean getBooleanValue() {
        if (booleanValue == null) {
            return null;
        }
        return isBooleanAsInteger(booleanValue);
    }

    public void setBooleanValue(Boolean booleanValue) {
        if (booleanValue == null) {
            this.booleanValue = null;
        } else {
            this.booleanValue = getBooleanAsInteger(booleanValue);
        }
    }

    public Date getDateValue() {
        return dateValue;
    }

    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public <T> T getValue() {
        return (T) (booleanValue != null ? getBooleanValue()
                : (dateValue != null ? getDateValue()
                : (doubleValue != null ? getDoubleValue()
                : (longValue != null ? getLongValue()
                : stringValue))));
    }

    public String getValueAsString() {
        String result = null;

        switch (getAttribute().getSchema().getType()) {

            case Boolean:
                result = getBooleanValue().toString();
                break;

            case Long:
                result = getAttribute().getSchema().getFormatter().
                        format(getLongValue());
                break;

            case Double:
                result = getAttribute().getSchema().getFormatter().
                        format(getDoubleValue());
                break;

            case Date:
                result = getAttribute().getSchema().getFormatter().
                        format(getDateValue());
                break;

            default:
                // applied to String and Enum SchemaType
                result = getStringValue();
                break;
        }

        return result;
    }

    public abstract <T extends AbstractAttr> T getAttribute();

    public abstract <T extends AbstractAttr> void setAttribute(
            T attribute);

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }
}
