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

import org.apache.syncope.common.lib.AbstractBaseBean;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "connPoolConf")
@XmlType
public class ConnPoolConfTO extends AbstractBaseBean {

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

}
