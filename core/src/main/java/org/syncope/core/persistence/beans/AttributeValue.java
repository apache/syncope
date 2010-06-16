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
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class AttributeValue extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    private String stringValue;
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateValue;
    private Boolean booleanValue;
    private Long longValue;
    private Double doubleValue;
    @ManyToOne
    private Attribute attribute;

    public Long getId() {
        return id;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute Attribute) {
        this.attribute = Attribute;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public <T> T getValue() {
        return (T) (booleanValue != null
                ? booleanValue : (dateValue != null
                ? dateValue : (doubleValue != null
                ? doubleValue : (longValue != null
                ? longValue : stringValue))));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AttributeValue other = (AttributeValue) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {

            return false;
        }
        if ((this.stringValue == null)
                ? (other.stringValue != null)
                : !this.stringValue.equals(other.stringValue)) {

            return false;
        }
        if (this.dateValue != other.dateValue
                && (this.dateValue == null
                || !this.dateValue.equals(other.dateValue))) {
            return false;
        }
        if (this.booleanValue != other.booleanValue
                && (this.booleanValue == null
                || !this.booleanValue.equals(other.booleanValue))) {

            return false;
        }
        if (this.longValue != other.longValue
                && (this.longValue == null
                || !this.longValue.equals(other.longValue))) {

            return false;
        }
        if (this.doubleValue != other.doubleValue
                && (this.doubleValue == null
                || !this.doubleValue.equals(other.doubleValue))) {

            return false;
        }
        if (this.attribute != other.attribute
                && (this.attribute == null
                || !this.attribute.equals(other.attribute))) {

            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 41 * hash + (this.stringValue != null
                ? this.stringValue.hashCode() : 0);
        hash = 41 * hash + (this.dateValue != null
                ? this.dateValue.hashCode() : 0);
        hash = 41 * hash + (this.booleanValue != null
                ? this.booleanValue.hashCode() : 0);
        hash = 41 * hash + (this.longValue != null
                ? this.longValue.hashCode() : 0);
        hash = 41 * hash + (this.doubleValue != null
                ? this.doubleValue.hashCode() : 0);
        hash = 41 * hash + (this.attribute != null
                ? this.attribute.hashCode() : 0);
        return hash;
    }
}
