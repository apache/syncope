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
import org.apache.syncope.common.lib.scim.types.AddressCanonicalType;

public class SCIMUserAddressConf implements Serializable {

    private static final long serialVersionUID = 8093531407836615577L;

    private String formatted;

    private String streetAddress;

    private String locality;

    private String region;

    private String postalCode;

    private String country;

    private AddressCanonicalType type;

    private boolean primary;

    @JsonIgnore
    public Map<String, String> asMap() {
        Map<String, String> map = new HashMap<>();

        if (formatted != null) {
            map.put("formatted", formatted);
        }
        if (streetAddress != null) {
            map.put("streetAddress", streetAddress);
        }
        if (locality != null) {
            map.put("locality", locality);
        }
        if (region != null) {
            map.put("region", region);
        }
        if (postalCode != null) {
            map.put("postalCode", postalCode);
        }
        if (country != null) {
            map.put("country", country);
        }

        return Collections.unmodifiableMap(map);
    }

    public String getFormatted() {
        return formatted;
    }

    public void setFormatted(final String formatted) {
        this.formatted = formatted;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(final String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(final String locality) {
        this.locality = locality;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(final String region) {
        this.region = region;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(final String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public AddressCanonicalType getType() {
        return type;
    }

    public void setType(final AddressCanonicalType type) {
        this.type = type;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(final boolean primary) {
        this.primary = primary;
    }
}
