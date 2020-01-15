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

import java.util.Date;
import java.util.Optional;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;

@XmlRootElement(name = "accessToken")
@XmlType
public class AccessTokenTO extends BaseBean implements EntityTO {

    private static final long serialVersionUID = 6577639976115661357L;

    private String key;

    private String body;

    private Date expiryTime;

    private String owner;

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public Date getExpiryTime() {
        return Optional.ofNullable(expiryTime).map(time -> new Date(time.getTime())).orElse(null);
    }

    public void setExpiryTime(final Date expiryTime) {
        this.expiryTime = Optional.ofNullable(expiryTime).map(time -> new Date(time.getTime())).orElse(null);
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
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
        AccessTokenTO other = (AccessTokenTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(body, other.body).
                append(expiryTime, other.expiryTime).
                append(owner, other.owner).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(body).
                append(expiryTime).
                append(owner).
                build();
    }
}
