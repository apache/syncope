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
package org.apache.syncope.common.lib.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.annotation.ClassList;
import org.apache.syncope.common.lib.annotation.SchemaList;

@XmlType
public class SyncPolicySpecItem extends AbstractBaseBean {

    private static final long serialVersionUID = 692466729711976485L;

    private String anyTypeKey;

    @SchemaList(extended = true)
    private final List<String> altSearchSchemas = new ArrayList<>();

    @ClassList
    private String javaRule;

    public String getAnyTypeKey() {
        return anyTypeKey;
    }

    public void setAnyTypeKey(final String anyTypeKey) {
        this.anyTypeKey = anyTypeKey;
    }

    public String getJavaRule() {
        return javaRule;
    }

    public void setJavaRule(final String javaRule) {
        this.javaRule = javaRule;
    }

    @XmlElementWrapper(name = "altSearchSchemas")
    @XmlElement(name = "altSearchSchema")
    @JsonProperty("altSearchSchemas")
    public List<String> getAltSearchSchemas() {
        return altSearchSchemas;
    }

}
