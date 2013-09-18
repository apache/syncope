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

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.types.UserRequestType;

@XmlRootElement(name = "userRequest")
@XmlType
public class UserRequestTO extends AbstractBaseBean {

    private static final long serialVersionUID = 1228351243795629329L;

    private long id;

    private UserTO userTO;

    private UserMod userMod;

    private Long userId;

    private String username;

    private UserRequestType type;

    private boolean executed;

    /**
     * Username of the last user who claimed the request.
     */
    private String owner;

    /**
     * Creation date.
     */
    private Date creationDate;

    /**
     * Claim date.
     */
    private Date claimDate;

    /**
     * Execution date.
     */
    private Date executionDate;

    public UserRequestTO() {
    }

    /**
     * Request to create an user.
     *
     * @param userTO
     */
    public UserRequestTO(final UserTO userTO) {
        this.type = UserRequestType.CREATE;
        this.userTO = userTO;
    }

    /**
     * Request to update an user.
     *
     * @param userMod
     */
    public UserRequestTO(final UserMod userMod) {
        this.userMod = userMod;
        this.type = UserRequestType.UPDATE;
    }

    /**
     * Request to delete a user.
     *
     * @param userId
     */
    public UserRequestTO(final long userId) {
        this.type = UserRequestType.DELETE;
        this.userId = userId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UserRequestType getType() {
        return type;
    }

    public void setType(final UserRequestType type) {
        this.type = type;
    }

    public UserTO getUserTO() {
        return type != UserRequestType.CREATE
                ? null
                : userTO;
    }

    public void setUserTO(final UserTO userTO) {
        this.userTO = userTO;
    }

    public UserMod getUserMod() {
        return type != UserRequestType.UPDATE
                ? null
                : userMod;
    }

    public void setUserMod(final UserMod userMod) {
        this.userMod = userMod;
    }

    public Long getUserId() {
        return type != UserRequestType.DELETE
                ? null
                : userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public boolean isExecuted() {
        return executed;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getClaimDate() {
        return claimDate;
    }

    public void setClaimDate(Date claimDate) {
        this.claimDate = claimDate;
    }

    public Date getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(Date executionDate) {
        this.executionDate = executionDate;
    }
}
