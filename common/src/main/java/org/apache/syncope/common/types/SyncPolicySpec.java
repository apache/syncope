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
package org.apache.syncope.common.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.annotation.ClassList;
import org.apache.syncope.common.annotation.SchemaList;

@XmlType
public class SyncPolicySpec extends AbstractPolicySpec {

    private static final long serialVersionUID = -3144027171719498127L;

    /**
     * SyncopeUser attributes and fields for matching during synchronization.
     */
    @SchemaList(extended = true)
    private List<String> uAltSearchSchemas;

    @ClassList
    private String userJavaRule;

    /**
     * SyncopeRole attributes and fields for matching during synchronization.
     */
    @SchemaList(extended = true)
    private List<String> rAltSearchSchemas;

    @ClassList
    private String roleJavaRule;

    /**
     * Conflict resolution action.
     */
    private ConflictResolutionAction conflictResolutionAction;

    public SyncPolicySpec() {
        super();

        uAltSearchSchemas = new ArrayList<String>();
        rAltSearchSchemas = new ArrayList<String>();
    }

    public ConflictResolutionAction getConflictResolutionAction() {
        return conflictResolutionAction == null
                ? ConflictResolutionAction.IGNORE
                : conflictResolutionAction;
    }

    public void setConflictResolutionAction(final ConflictResolutionAction conflictResolutionAction) {
        this.conflictResolutionAction = conflictResolutionAction;
    }

    @XmlElementWrapper(name = "userAltSearchSchemas")
    @XmlElement(name = "userAltSearchSchema")
    @JsonProperty("userAltSearchSchemas")
    public List<String> getuAltSearchSchemas() {
        return uAltSearchSchemas;
    }

    public void setuAltSearchSchemas(final List<String> uAltSearchSchemas) {
        this.uAltSearchSchemas = uAltSearchSchemas;
    }

    @XmlElementWrapper(name = "roleAltSearchSchemas")
    @XmlElement(name = "roleAltSearchSchema")
    @JsonProperty("roleAltSearchSchemas")
    public List<String> getrAltSearchSchemas() {
        return rAltSearchSchemas;
    }

    public void setrAltSearchSchemas(final List<String> rAltSearchSchemas) {
        this.rAltSearchSchemas = rAltSearchSchemas;
    }


    public String getRoleJavaRule() {
        return roleJavaRule;
    }

    public void setRoleJavaRule(final String roleJavaRule) {
        this.roleJavaRule = roleJavaRule;
    }

    public String getUserJavaRule() {
        return userJavaRule;
    }

    public void setUserJavaRule(final String userJavaRule) {
        this.userJavaRule = userJavaRule;
    }
}
