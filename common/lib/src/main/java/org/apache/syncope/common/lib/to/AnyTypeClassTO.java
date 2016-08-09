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
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "anyTypeClass")
@XmlType
public class AnyTypeClassTO extends AbstractBaseBean implements EntityTO {

    private static final long serialVersionUID = -591757688607551266L;

    private String key;

    private final List<String> plainSchemas = new ArrayList<>();

    private final List<String> derSchemas = new ArrayList<>();

    private final List<String> virSchemas = new ArrayList<>();

    private final List<String> inUseByTypes = new ArrayList<>();

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @XmlElementWrapper(name = "plainSchemas")
    @XmlElement(name = "schema")
    @JsonProperty("plainSchemas")
    public List<String> getPlainSchemas() {
        return plainSchemas;
    }

    @XmlElementWrapper(name = "derSchemas")
    @XmlElement(name = "schema")
    @JsonProperty("derSchemas")
    public List<String> getDerSchemas() {
        return derSchemas;
    }

    @XmlElementWrapper(name = "virSchemas")
    @XmlElement(name = "schema")
    @JsonProperty("virSchemas")
    public List<String> getVirSchemas() {
        return virSchemas;
    }

    @XmlElementWrapper(name = "inUseByTypes")
    @XmlElement(name = "anyTypeClass")
    @JsonProperty("inUseByTypes")
    public List<String> getInUseByTypes() {
        return inUseByTypes;
    }

}
