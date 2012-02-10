/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.client.to;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.syncope.types.PropagationTaskExecStatus;

public class UserTO extends AbstractAttributableTO {

    private static final long serialVersionUID = 7791304495192615740L;

    private String password;

    private List<MembershipTO> memberships;

    private List<ConnObjectTO> connObjectTOs;

    private String status;

    private String token;

    private Date tokenExpireTime;

    private String username;

    private Date lastLoginDate;

    private Date creationDate;

    private Date changePwdDate;

    private Integer failedLogins;

    private Map<String, PropagationTaskExecStatus> propagationStatusMap;

    public UserTO() {
        super();

        memberships = new ArrayList<MembershipTO>();
        connObjectTOs = new ArrayList<ConnObjectTO>();
        propagationStatusMap = new HashMap<String, PropagationTaskExecStatus>();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean addMembership(MembershipTO membershipTO) {
        return memberships.add(membershipTO);
    }

    public boolean removeMembership(MembershipTO membershipTO) {
        return memberships.remove(membershipTO);
    }

    public List<MembershipTO> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<MembershipTO> memberships) {
        this.memberships = memberships;
    }

    @JsonIgnore
    public Map<Long, MembershipTO> getMembershipMap() {
        Map<Long, MembershipTO> result = new HashMap<Long, MembershipTO>(
                getMemberships().size());

        for (MembershipTO membership : getMemberships()) {
            result.put(membership.getRoleId(), membership);
        }

        return result;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getTokenExpireTime() {
        return tokenExpireTime == null
                ? null : new Date(tokenExpireTime.getTime());
    }

    public void setTokenExpireTime(Date tokenExpireTime) {
        if (tokenExpireTime != null) {
            this.tokenExpireTime = new Date(tokenExpireTime.getTime());
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getChangePwdDate() {
        return changePwdDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Integer getFailedLogins() {
        return failedLogins;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public void setChangePwdDate(Date changePwdDate) {
        this.changePwdDate = changePwdDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setFailedLogins(Integer failedLogins) {
        this.failedLogins = failedLogins;
    }

    public void setLastLoginDate(Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public void addPropagationStatus(String resource,
            PropagationTaskExecStatus status) {

        propagationStatusMap.put(resource, status);
    }

    public void removePropagationStatus(String resource) {

        propagationStatusMap.remove(resource);
    }

    public Map<String, PropagationTaskExecStatus> getPropagationStatusMap() {
        return propagationStatusMap;
    }

    public void setPropagationStatusMap(
            Map<String, PropagationTaskExecStatus> propagationStatusMap) {

        this.propagationStatusMap.clear();
        if (propagationStatusMap != null && !propagationStatusMap.isEmpty()) {
            this.propagationStatusMap.putAll(propagationStatusMap);
        }
    }

    public List<ConnObjectTO> getConnObjectTOs() {
        return connObjectTOs;
    }

    public void setConnObjectTOs(final List<ConnObjectTO> connObjectTOs) {
        this.connObjectTOs = connObjectTOs;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this,
                ToStringStyle.MULTI_LINE_STYLE) {

            @Override
            protected boolean accept(Field f) {
                return super.accept(f) && !f.getName().equals("password");
            }
        }.toString();
    }
}
