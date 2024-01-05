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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class PagedResult<T extends BaseBean> implements BaseBean {

    private static final long serialVersionUID = 3472875885259250934L;

    private URI prev;

    private URI next;

    private final List<T> result = new ArrayList<>();

    private int page;

    private int size;

    private long totalCount;

    public URI getPrev() {
        return prev;
    }

    public void setPrev(final URI prev) {
        this.prev = prev;
    }

    public URI getNext() {
        return next;
    }

    public void setNext(final URI next) {
        this.next = next;
    }

    @JacksonXmlElementWrapper(localName = "result")
    @JacksonXmlProperty(localName = "item")
    public List<T> getResult() {
        return result;
    }

    public int getPage() {
        return page;
    }

    public void setPage(final int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(final int size) {
        this.size = size;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(final long totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(prev).
                append(next).
                append(result).
                append(page).
                append(size).
                append(totalCount).
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
        final PagedResult<T> other = (PagedResult<T>) obj;
        return new EqualsBuilder().
                append(prev, other.prev).
                append(next, other.next).
                append(result, other.result).
                append(page, other.page).
                append(size, other.size).
                append(totalCount, other.totalCount).
                build();
    }
}
