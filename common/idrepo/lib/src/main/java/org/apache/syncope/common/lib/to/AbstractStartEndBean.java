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

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.lib.BaseBean;

public class AbstractStartEndBean implements BaseBean {

    private static final long serialVersionUID = 2399577415544539917L;

    private OffsetDateTime start;

    private OffsetDateTime end;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public OffsetDateTime getStart() {
        return start;
    }

    public void setStart(final OffsetDateTime start) {
        this.start = start;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public OffsetDateTime getEnd() {
        return end;
    }

    public void setEnd(final OffsetDateTime end) {
        this.end = end;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(start).
                append(end).
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
        final AbstractStartEndBean other = (AbstractStartEndBean) obj;
        return new EqualsBuilder().
                append(start, other.start).
                append(end, other.end).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).
                append(start).
                append(end).
                build();
    }
}
