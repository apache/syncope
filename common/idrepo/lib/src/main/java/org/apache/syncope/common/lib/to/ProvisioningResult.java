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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class ProvisioningResult<E extends EntityTO> implements BaseBean {

    private static final long serialVersionUID = 351317476398082746L;

    private E entity;

    private final List<PropagationStatus> propagationStatuses = new ArrayList<>();

    public E getEntity() {
        return entity;
    }

    public void setEntity(final E entity) {
        this.entity = entity;
    }

    @JacksonXmlElementWrapper(localName = "propagationStatuses")
    @JacksonXmlProperty(localName = "propagationStatus")
    public List<PropagationStatus> getPropagationStatuses() {
        return propagationStatuses;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(entity).
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
        @SuppressWarnings("unchecked")
        final ProvisioningResult<E> other = (ProvisioningResult<E>) obj;
        return new EqualsBuilder().
                append(entity, other.entity).
                build();
    }
}
