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
}
