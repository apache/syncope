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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class AbstractPatchItem<T> extends AbstractPatch {

    private static final long serialVersionUID = -8889326446009942028L;

    protected abstract static class Builder<T, P extends AbstractPatchItem<T>, B extends Builder<T, P, B>>
            extends AbstractPatch.Builder<P, B> {

        @SuppressWarnings("unchecked")
        public B value(final T value) {
            getInstance().setValue(value);
            return (B) this;
        }
    }

    private T value;

    @JsonProperty(required = true)
    public T getValue() {
        return value;
    }

    public void setValue(final T value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(value).
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
        final AbstractPatchItem<T> other = (AbstractPatchItem<T>) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(value, other.value).
                build();
    }
}
