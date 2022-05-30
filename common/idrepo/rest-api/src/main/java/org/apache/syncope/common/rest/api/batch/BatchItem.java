/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.rest.api.batch;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class BatchItem implements Serializable {

    private static final long serialVersionUID = -1393976266651766259L;

    protected final Map<String, List<Object>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    protected String content;

    public Map<String, List<Object>> getHeaders() {
        return headers;
    }

    public void setHeaders(final Map<String, List<Object>> headers) {
        this.headers.clear();
        this.headers.putAll(headers);
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(headers).
                append(content).
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
        final BatchItem other = (BatchItem) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(headers, other.headers).
                append(content, other.content).
                build();
    }

    @Override
    public String toString() {
        return "BatchItem{"
                + "headers=" + headers + ", "
                + "content=" + content
                + '}';
    }
}
