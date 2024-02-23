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
package org.apache.syncope.common.lib.types;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class ConnPoolConf implements BaseBean {

    private static final long serialVersionUID = -214360178113476623L;

    private Integer maxObjects;

    private Integer minIdle;

    private Integer maxIdle;

    private Long maxWait;

    private Long minEvictableIdleTimeMillis;

    public Integer getMaxObjects() {
        return maxObjects;
    }

    public void setMaxObjects(final Integer maxObjects) {
        this.maxObjects = maxObjects;
    }

    public Integer getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(final Integer minIdle) {
        this.minIdle = minIdle;
    }

    public Integer getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(final Integer maxIdle) {
        this.maxIdle = maxIdle;
    }

    public Long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(final Long maxWait) {
        this.maxWait = maxWait;
    }

    public Long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public void setMinEvictableIdleTimeMillis(final Long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
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
        ConnPoolConf other = (ConnPoolConf) obj;
        return new EqualsBuilder().
                append(maxObjects, other.maxObjects).
                append(minIdle, other.minIdle).
                append(maxIdle, other.maxIdle).
                append(maxWait, other.maxWait).
                append(minEvictableIdleTimeMillis, other.minEvictableIdleTimeMillis).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(maxObjects).
                append(minIdle).
                append(maxIdle).
                append(maxWait).
                append(minEvictableIdleTimeMillis).
                build();
    }
}
