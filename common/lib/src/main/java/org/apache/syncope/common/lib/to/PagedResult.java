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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "pagedResult")
@XmlType
public class PagedResult<T extends EntityTO> extends AbstractBaseBean {

    private static final long serialVersionUID = 3472875885259250934L;

    private URI prev;

    private URI next;

    private final List<T> result = new ArrayList<>();

    private int page;

    private int size;

    private int totalCount;

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

    @XmlElementWrapper(name = "result")
    @XmlElement(name = "item")
    @JsonProperty("result")
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
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

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(final int totalCount) {
        this.totalCount = totalCount;
    }

}
