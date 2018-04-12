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
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ReconciliationAction;

public class ReconciliationRequest extends AbstractBaseBean {

    private static final long serialVersionUID = -2592156800185957182L;

    private AnyTypeKind anyTypeKind;

    private String anyKey;

    private String resourceKey;

    private ReconciliationAction action;

    private final List<String> actionsClassNames = new ArrayList<>();

    @JsonProperty(required = true)
    @XmlElement(required = true)
    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    @JsonProperty(required = true)
    @XmlElement(required = true)
    public String getAnyKey() {
        return anyKey;
    }

    public void setAnyKey(final String anyKey) {
        this.anyKey = anyKey;
    }

    @JsonProperty(required = true)
    @XmlElement(required = true)
    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(final String resourceKey) {
        this.resourceKey = resourceKey;
    }

    @JsonProperty(required = true)
    @XmlElement(required = true)
    public ReconciliationAction getAction() {
        return action;
    }

    public void setAction(final ReconciliationAction action) {
        this.action = action;
    }

    @XmlElementWrapper(name = "actionsClassNames")
    @XmlElement(name = "actionsClassName")
    @JsonProperty("actionsClassNames")
    public List<String> getActionsClassNames() {
        return actionsClassNames;
    }
}
