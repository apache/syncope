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
package org.apache.syncope.common.keymaster.client.api.model;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class NetworkService implements Serializable {

    private static final long serialVersionUID = 7144725980960412224L;

    public enum Type {
        CORE,
        CONSOLE,
        ENDUSER,
        SRA,
        WA

    }

    private Type type;

    private String address;

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(type).
                append(address).
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
        final NetworkService other = (NetworkService) obj;
        return new EqualsBuilder().
                append(type, other.type).
                append(address, other.address).
                build();
    }

    @Override
    public String toString() {
        return "NetworkService{"
                + "type=" + type
                + ", address=" + address
                + '}';
    }
}
