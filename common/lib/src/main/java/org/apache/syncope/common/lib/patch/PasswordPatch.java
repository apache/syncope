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
import java.util.Collection;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "passwordPatch")
@XmlType
@XmlSeeAlso({ AssociationPatch.class, StatusPatch.class })
public class PasswordPatch extends StringReplacePatchItem {

    private static final long serialVersionUID = 961023537479513071L;

    public static class Builder extends AbstractReplacePatchItem.Builder<String, PasswordPatch, Builder> {

        @Override
        protected PasswordPatch newInstance() {
            return new PasswordPatch();
        }

        public Builder onSyncope(final boolean onSyncope) {
            getInstance().setOnSyncope(onSyncope);
            return this;
        }

        public Builder resource(final String resource) {
            if (resource != null) {
                getInstance().getResources().add(resource);
            }
            return this;
        }

        public Builder resources(final String... resources) {
            for (String resource : resources) {
                getInstance().getResources().add(resource);
            }
            return this;
        }

        public Builder resources(final Collection<String> resources) {
            if (resources != null) {
                getInstance().getResources().addAll(resources);
            }
            return this;
        }

    }

    /**
     * Whether update should be performed on internal storage.
     */
    private boolean onSyncope = true;

    /**
     * External resources for which update is needed to be propagated.
     */
    private final List<String> resources = new ArrayList<>();

    public boolean isOnSyncope() {
        return onSyncope;
    }

    public void setOnSyncope(final boolean onSyncope) {
        this.onSyncope = onSyncope;
    }

    @XmlElementWrapper(name = "resources")
    @XmlElement(name = "resource")
    @JsonProperty("resources")
    public List<String> getResources() {
        return resources;
    }

}
