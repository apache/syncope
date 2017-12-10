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

public class SCIMUserNameConf implements Serializable {

    private static final long serialVersionUID = -2256008193008290376L;

    private String formatted;

    private String familyName;

    private String givenName;

    private String middleName;

    private String honorificPrefix;

    private String honorificSuffix;

    @JsonIgnore
    public Map<String, String> asMap() {
        Map<String, String> map = new HashMap<>();

        if (formatted != null) {
            map.put("formatted", formatted);
        }
        if (familyName != null) {
            map.put("familyName", familyName);
        }
        if (givenName != null) {
            map.put("givenName", givenName);
        }
        if (middleName != null) {
            map.put("middleName", middleName);
        }
        if (honorificPrefix != null) {
            map.put("honorificPrefix", honorificPrefix);
        }
        if (honorificSuffix != null) {
            map.put("honorificSuffix", honorificSuffix);
        }

        return Collections.unmodifiableMap(map);
    }

    public String getFormatted() {
        return formatted;
    }

    public void setFormatted(final String formatted) {
        this.formatted = formatted;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(final String familyName) {
        this.familyName = familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(final String givenName) {
        this.givenName = givenName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(final String middleName) {
        this.middleName = middleName;
    }

    public String getHonorificPrefix() {
        return honorificPrefix;
    }

    public void setHonorificPrefix(final String honorificPrefix) {
        this.honorificPrefix = honorificPrefix;
    }

    public String getHonorificSuffix() {
        return honorificSuffix;
    }

    public void setHonorificSuffix(final String honorificSuffix) {
        this.honorificSuffix = honorificSuffix;
    }

}
