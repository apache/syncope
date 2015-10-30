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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

/**
 * Metadata description of ConnId ObjectClass.
 */
@XmlRootElement(name = "connIdObjectClass")
@XmlType
public class ConnIdObjectClassTO extends AbstractBaseBean {

    private static final long serialVersionUID = -3719658595689434648L;

    private String type;

    private boolean container;

    private boolean auxiliary;

    private final List<String> attributes = new ArrayList<>();

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public boolean isContainer() {
        return container;
    }

    public void setContainer(final boolean container) {
        this.container = container;
    }

    public boolean isAuxiliary() {
        return auxiliary;
    }

    public void setAuxiliary(final boolean auxiliary) {
        this.auxiliary = auxiliary;
    }

    @XmlElementWrapper(name = "attributes")
    @XmlElement(name = "attribute")
    @JsonProperty("attributes")
    public List<String> getAttributes() {
        return attributes;
    }

}
