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
package org.apache.syncope.common.mod;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.wrap.ResourceName;

/**
 * This class is used to specify the willing to create associations between user and external references.
 * Password can be provided if required by an assign or provisioning operation.
 *
 * @see org.apache.syncope.common.types.ResourceAssociationActionType
 */
@XmlRootElement(name = "resourceAssociationMod")
@XmlType
public class ResourceAssociationMod extends AbstractBaseBean {

    private static final long serialVersionUID = -4188817853738067678L;

    /**
     * Target external resources.
     */
    private final List<ResourceName> targetResources = new ArrayList<ResourceName>();

    /**
     * Indicate the willing to change password on target external resources.
     */
    private boolean changePwd;

    /**
     * Indicate the new password to be provisioned on target external resources.
     */
    private String password;

    @XmlElementWrapper(name = "resources")
    @XmlElement(name = "resource")
    @JsonProperty("resources")
    public List<ResourceName> getTargetResources() {
        return targetResources;
    }

    public boolean isChangePwd() {
        return changePwd;
    }

    public void setChangePwd(boolean changePwd) {
        this.changePwd = changePwd;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
