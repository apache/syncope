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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.syncope.common.lib.jaxb.XmlGenericMapAdapter;
import org.apache.syncope.common.lib.types.PullMode;

@XmlRootElement(name = "pullTask")
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class PullTaskTO extends AbstractProvisioningTaskTO implements TemplatableTO {

    private static final long serialVersionUID = -2143537546915809017L;

    private PullMode pullMode;

    private String reconciliationFilterBuilderClassName;

    private String destinationRealm;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, AnyTO> templates = new HashMap<>();

    public PullMode getPullMode() {
        return pullMode;
    }

    public void setPullMode(final PullMode pullMode) {
        this.pullMode = pullMode;
    }

    public String getReconciliationFilterBuilderClassName() {
        return reconciliationFilterBuilderClassName;
    }

    public void setReconciliationFilterBuilderClassName(final String reconciliationFilterBuilderClassName) {
        this.reconciliationFilterBuilderClassName = reconciliationFilterBuilderClassName;
    }

    public String getDestinationRealm() {
        return destinationRealm;
    }

    public void setDestinationRealm(final String destinationRealm) {
        this.destinationRealm = destinationRealm;
    }

    @JsonProperty
    @Override
    public Map<String, AnyTO> getTemplates() {
        return templates;
    }
}
