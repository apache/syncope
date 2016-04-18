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
package org.apache.syncope.common.lib.patch;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;

@XmlRootElement(name = "resourceDeassociationPatch")
@XmlType
public class ResourceDeassociationPatch extends AbstractBaseBean {

    private static final long serialVersionUID = -9116268525079837276L;

    private String key;

    private String anyTypeKey;

    private ResourceDeassociationAction action;

    private final List<String> anyKyes = new ArrayList<>();

    public String getKey() {
        return key;
    }

    @PathParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public String getAnyTypeKey() {
        return anyTypeKey;
    }

    @PathParam("anyTypeKey")
    public void setAnyTypeKey(final String anyTypeKey) {
        this.anyTypeKey = anyTypeKey;
    }

    public ResourceDeassociationAction getAction() {
        return action;
    }

    @PathParam("action")
    public void setAction(final ResourceDeassociationAction action) {
        this.action = action;
    }

    @XmlElementWrapper(name = "anyKyes")
    @XmlElement(name = "key")
    @JsonProperty("anyKyes")
    public List<String> getAnyKyes() {
        return anyKyes;
    }

}
