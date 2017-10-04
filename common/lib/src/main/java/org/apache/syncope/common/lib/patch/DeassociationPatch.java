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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;

@XmlRootElement(name = "deassociationPatch")
@XmlType
public class DeassociationPatch extends AbstractBaseBean {

    private static final long serialVersionUID = 6295778399633883767L;

    public static class Builder {

        private final DeassociationPatch instance;

        public Builder() {
            this.instance = new DeassociationPatch();
        }

        public Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public Builder action(final ResourceDeassociationAction action) {
            instance.setAction(action);
            return this;
        }

        public Builder resource(final String resource) {
            if (resource != null) {
                instance.getResources().add(resource);
            }
            return this;
        }

        public Builder resources(final String... resources) {
            instance.getResources().addAll(Arrays.asList(resources));
            return this;
        }

        public Builder resources(final Collection<String> resources) {
            if (resources != null) {
                instance.getResources().addAll(resources);
            }
            return this;
        }

        public DeassociationPatch build() {
            return instance;
        }
    }

    private String key;

    private ResourceDeassociationAction action;

    private final List<String> resources = new ArrayList<>();

    public String getKey() {
        return key;
    }

    @PathParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public ResourceDeassociationAction getAction() {
        return action;
    }

    public void setAction(final ResourceDeassociationAction action) {
        this.action = action;
    }

    @XmlElementWrapper(name = "resources")
    @XmlElement(name = "resource")
    @JsonProperty("resources")
    public List<String> getResources() {
        return resources;
    }
}
