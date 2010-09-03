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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@MappedSuperclass
public abstract class AbstractAttributeValue extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    private String stringValue;
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateValue;
    @Basic
    private Character booleanValue;
    private Long longValue;
    private Double doubleValue;

    public Long getId() {
        return id;
    }

    public Boolean getBooleanValue() {
        if (booleanValue == null) {
            return null;
        }
        return isBooleanAsCharacter(booleanValue);
    }

    public void setBooleanValue(Boolean booleanValue) {
        if (booleanValue == null) {
            this.booleanValue = null;
        } else {
            this.booleanValue = getBooleanAsCharacter(booleanValue);
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
        return (T) (booleanValue != null
                ? getBooleanValue() : (dateValue != null
                ? getDateValue() : (doubleValue != null
                ? getDoubleValue() : (longValue != null
                ? getLongValue() : stringValue))));
    }

    public String getValueAsString() {
        String result = null;

        switch (getAttribute().getSchema().getType()) {

            case String:
                result = stringValue;
                break;

            case Boolean:
                result = booleanValue.toString();
                break;

            case Long:
                result = getAttribute().getSchema().getFormatter(
                        DecimalFormat.class).format(longValue);
                break;

            case Double:
                result = getAttribute().getSchema().getFormatter(
                        DecimalFormat.class).format(doubleValue);
                break;

            case Date:
                result = getAttribute().getSchema().getFormatter(
                        SimpleDateFormat.class).format(dateValue);
        }

        return result;
    }

    public abstract <T extends AbstractAttribute> T getAttribute();

    public abstract <T extends AbstractAttribute> void setAttribute(
            T attribute);

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }
}
