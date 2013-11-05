/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.common.to;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.types.AuditElements;

@XmlRootElement(name = "user")
@XmlType
public class EventCategoryTO extends AbstractBaseBean {

    private static final long serialVersionUID = -4340060002701633401L;

    private AuditElements.EventCategoryType type;

    private String category;

    private String subcategory;

    private List<String> events;

    /**
     * Constructor for Type.REST event category.
     */
    public EventCategoryTO() {
        this.type = AuditElements.EventCategoryType.REST;
    }

    /**
     * Constructor for the given Type event category.
     */
    public EventCategoryTO(final AuditElements.EventCategoryType type) {
        this.type = type;
    }

    public AuditElements.EventCategoryType getType() {
        return type;
    }

    public void setType(final AuditElements.EventCategoryType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(final String subcategory) {
        this.subcategory = subcategory;
    }

    public List<String> getEvents() {
        if (events == null) {
            events = new ArrayList<String>();
        }
        return events;
    }

    public void setEvents(final List<String> events) {
        this.events = events;
    }
}
