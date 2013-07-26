/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.mod;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.AbstractBaseBean;

@XmlRootElement
@XmlType
public class AttributeMod extends AbstractBaseBean {

    private static final long serialVersionUID = -913573979137431406L;

    private String schema;

    private List<String> valuesToBeAdded;

    private List<String> valuesToBeRemoved;

    public AttributeMod() {
        super();

        valuesToBeAdded = new ArrayList<String>();
        valuesToBeRemoved = new ArrayList<String>();
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @XmlElementWrapper(name = "valuesToBeAdded")
    @XmlElement(name = "value")
    @JsonProperty("valuesToBeAdded")
    public List<String> getValuesToBeAdded() {
        return valuesToBeAdded;
    }

    @XmlElementWrapper(name = "valuesToBeRemoved")
    @XmlElement(name = "value")
    @JsonProperty("valuesToBeRemoved")
    public List<String> getValuesToBeRemoved() {
        return valuesToBeRemoved;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return valuesToBeAdded.isEmpty() && valuesToBeRemoved.isEmpty();
    }
}
