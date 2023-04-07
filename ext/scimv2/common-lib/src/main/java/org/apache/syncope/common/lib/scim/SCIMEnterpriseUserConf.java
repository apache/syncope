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
package org.apache.syncope.common.lib.scim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SCIMEnterpriseUserConf implements Serializable {

    private static final long serialVersionUID = 5929414914887282638L;

    private String employeeNumber;

    private String costCenter;

    private String organization;

    private String division;

    private String department;

    private SCIMManagerConf manager;

    @JsonIgnore
    public Map<String, String> asMap() {
        Map<String, String> map = new HashMap<>();

        if (employeeNumber != null) {
            map.put("employeeNumber", employeeNumber);
        }
        if (costCenter != null) {
            map.put("costCenter", costCenter);
        }
        if (organization != null) {
            map.put("organization", organization);
        }
        if (division != null) {
            map.put("division", division);
        }
        if (department != null) {
            map.put("department", department);
        }

        return Collections.unmodifiableMap(map);
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(final String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(final String costCenter) {
        this.costCenter = costCenter;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(final String organization) {
        this.organization = organization;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(final String division) {
        this.division = division;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(final String department) {
        this.department = department;
    }

    public SCIMManagerConf getManager() {
        return manager;
    }

    public void setManager(final SCIMManagerConf manager) {
        this.manager = manager;
    }
}
