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
package org.apache.syncope.core.persistence.jpa.entity;

import java.io.Serializable;
import javax.persistence.Embeddable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.core.persistence.api.entity.ConnPoolConf;

@Embeddable
public class JPAConnPoolConf implements ConnPoolConf, Serializable {

    private static final long serialVersionUID = -34259572059178970L;

    private Integer maxObjects;

    private Integer minIdle;

    private Integer maxIdle;

    private Long maxWait;

    private Long minEvictableIdleTimeMillis;

    @Override
    public Integer getMaxObjects() {
        return maxObjects;
    }

    @Override
    public void setMaxObjects(final Integer maxObjects) {
        this.maxObjects = maxObjects;
    }

    @Override
    public Integer getMinIdle() {
        return minIdle;
    }

    @Override
    public void setMinIdle(final Integer minIdle) {
        this.minIdle = minIdle;
    }

    @Override
    public Integer getMaxIdle() {
        return maxIdle;
    }

    @Override
    public void setMaxIdle(final Integer maxIdle) {
        this.maxIdle = maxIdle;
    }

    @Override
    public Long getMaxWait() {
        return maxWait;
    }

    @Override
    public void setMaxWait(final Long maxWait) {
        this.maxWait = maxWait;
    }

    @Override
    public Long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    @Override
    public void setMinEvictableIdleTimeMillis(final Long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
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
        final JPAConnPoolConf other = (JPAConnPoolConf) obj;
        return new EqualsBuilder().
                append(maxObjects, other.maxObjects).
                append(minIdle, other.minIdle).
                append(maxIdle, other.maxIdle).
                append(maxWait, other.maxWait).
                append(minEvictableIdleTimeMillis, other.minEvictableIdleTimeMillis).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(maxObjects).
                append(minIdle).
                append(maxIdle).
                append(maxWait).
                append(minEvictableIdleTimeMillis).
                build();
    }
}
