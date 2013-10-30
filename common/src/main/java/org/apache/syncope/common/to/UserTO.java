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
package org.apache.syncope.common.to;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@XmlRootElement(name = "user")
@XmlType
public class UserTO extends AbstractAttributableTO {

    private static final long serialVersionUID = 7791304495192615740L;

    private String password;

    private final List<MembershipTO> memberships = new ArrayList<MembershipTO>();

    private String status;

    private String token;

    private Date tokenExpireTime;

    private String username;

    private Date lastLoginDate;

    private Date changePwdDate;

    private Integer failedLogins;

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    @XmlElementWrapper(name = "memberships")
    @XmlElement(name = "membership")
    @JsonProperty("memberships")
    public List<MembershipTO> getMemberships() {
        return memberships;
    }

    @JsonIgnore
    public Map<Long, MembershipTO> getMembershipMap() {
        Map<Long, MembershipTO> result;

        if (getMemberships() == null) {
            result = Collections.emptyMap();
        } else {
            result = new HashMap<Long, MembershipTO>(getMemberships().size());
            for (MembershipTO membership : getMemberships()) {
                result.put(membership.getRoleId(), membership);
            }
            result = Collections.unmodifiableMap(result);
        }

        return result;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public Date getTokenExpireTime() {
        return tokenExpireTime == null
                ? null
                : new Date(tokenExpireTime.getTime());
    }

    public void setTokenExpireTime(final Date tokenExpireTime) {
        if (tokenExpireTime != null) {
            this.tokenExpireTime = new Date(tokenExpireTime.getTime());
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Date getChangePwdDate() {
        return changePwdDate;
    }

    public Integer getFailedLogins() {
        return failedLogins;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public void setChangePwdDate(final Date changePwdDate) {
        this.changePwdDate = changePwdDate;
    }

    public void setFailedLogins(final Integer failedLogins) {
        this.failedLogins = failedLogins;
    }

    public void setLastLoginDate(final Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE) {

            @Override
            protected boolean accept(final Field f) {
                return super.accept(f) && !f.getName().equals("password");
            }
        }.toString();
    }
}
