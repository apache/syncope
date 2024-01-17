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
package org.apache.syncope.core.persistence.api.search;

import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class SyncopePage<T> extends PageImpl<T> {

    private static final long serialVersionUID = 3590869979849219972L;

    private final long total;

    public SyncopePage(final List<T> content, final Pageable pageable, final long total) {
        super(content, pageable, total);
        this.total = total;
    }

    @Override
    public long getTotalElements() {
        return total;
    }

    @Override
    public int getTotalPages() {
        return getSize() == 0 ? 1 : (int) Math.ceil(total / (double) getSize());
    }

    @Override
    public boolean equals(final Object obj) {
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(total, total).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(total).
                build();
    }
}
