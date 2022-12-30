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
package org.apache.syncope.common.rest.api.beans;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.QueryParam;
import java.time.OffsetDateTime;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class AbstractTimeframeQuery extends AbstractQuery {

    private static final long serialVersionUID = -6858655425207486223L;

    protected abstract static class Builder<Q extends AbstractTimeframeQuery, B extends Builder<Q, B>>
            extends AbstractQuery.Builder<Q, B> {

        @Override
        protected Q getInstance() {
            return super.getInstance();
        }

        @SuppressWarnings("unchecked")
        public B before(final OffsetDateTime before) {
            getInstance().setBefore(before);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B after(final OffsetDateTime after) {
            getInstance().setAfter(after);
            return (B) this;
        }
    }

    private OffsetDateTime before;

    private OffsetDateTime after;

    @Parameter(name = "before", in = ParameterIn.QUERY, schema =
            @Schema(implementation = OffsetDateTime.class))
    public OffsetDateTime getBefore() {
        return before;
    }

    @QueryParam("before")
    public void setBefore(final OffsetDateTime before) {
        this.before = before;
    }

    @Parameter(name = "after", in = ParameterIn.QUERY, schema =
            @Schema(implementation = OffsetDateTime.class))
    public OffsetDateTime getAfter() {
        return after;
    }

    @QueryParam("after")
    public void setAfter(final OffsetDateTime after) {
        this.after = after;
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
        AbstractTimeframeQuery other = (AbstractTimeframeQuery) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(before, other.before).
                append(after, other.after).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(before).
                append(after).
                build();
    }
}
