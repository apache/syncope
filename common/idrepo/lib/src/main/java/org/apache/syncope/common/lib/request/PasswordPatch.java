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
package org.apache.syncope.common.lib.request;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
            getInstance().getResources().addAll(List.of(resources));
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

    @JacksonXmlElementWrapper(localName = "resources")
    @JacksonXmlProperty(localName = "resource")
    public List<String> getResources() {
        return resources;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(onSyncope).
                append(resources).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PasswordPatch other = (PasswordPatch) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(onSyncope, other.onSyncope).
                append(resources, other.resources).
                build();
    }
}
