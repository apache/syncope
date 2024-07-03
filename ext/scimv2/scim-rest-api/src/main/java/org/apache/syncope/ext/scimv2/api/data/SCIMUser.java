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
package org.apache.syncope.ext.scimv2.api.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JsonPropertyOrder({ "schemas", "id", "externalId",
    "userName", "password", "active",
    "name", "displayName", "nickName", "profileUrl", "title", "userType", "preferredLanguage", "locale", "timezone",
    "emails", "phoneNumbers", "ims", "photos", "addresses", "x509Certificates",
    "groups", "entitlements", "roles",
    "enterpriseInfo", "extensionInfo",
    "meta" })
public class SCIMUser extends SCIMResource {

    private static final long serialVersionUID = -2935466041674390279L;

    private final String userName;

    private String password;

    private final Boolean active;

    private SCIMUserName name;

    private String nickName;

    private String profileUrl;

    private String title;

    private String userType;

    private String preferredLanguage;

    private String locale;

    private String timezone;

    private final List<SCIMComplexValue> emails = new ArrayList<>();

    private final List<SCIMComplexValue> phoneNumbers = new ArrayList<>();

    private final List<SCIMComplexValue> ims = new ArrayList<>();

    private final List<SCIMComplexValue> photos = new ArrayList<>();

    private final List<SCIMUserAddress> addresses = new ArrayList<>();

    private final List<Value> x509Certificates = new ArrayList<>();

    private final List<Group> groups = new ArrayList<>();

    private final List<Value> entitlements = new ArrayList<>();

    private final List<Value> roles = new ArrayList<>();

    @JsonProperty("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User")
    private SCIMEnterpriseInfo enterpriseInfo;

    @JsonProperty("urn:ietf:params:scim:schemas:extension:syncope:2.0:User")
    private SCIMExtensionInfo extensionInfo;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SCIMUser(
            @JsonProperty("id") final String id,
            @JsonProperty("schemas") final List<String> schemas,
            @JsonProperty("meta") final Meta meta,
            @JsonProperty("userName") final String userName,
            @JsonProperty("active") final Boolean active) {

        super(id, schemas, meta);
        this.userName = userName;
        this.active = active;
    }

    public String getUserName() {
        return userName;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    @JsonIgnore
    public Boolean getActive() {
        return active;
    }

    public boolean isActive() {
        return Optional.ofNullable(active).orElse(true);
    }

    public SCIMUserName getName() {
        return name;
    }

    public void setName(final SCIMUserName name) {
        this.name = name;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(final String nickName) {
        this.nickName = nickName;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(final String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(final String userType) {
        this.userType = userType;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(final String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(final String locale) {
        this.locale = locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(final String timezone) {
        this.timezone = timezone;
    }

    public List<SCIMComplexValue> getEmails() {
        return emails;
    }

    public List<SCIMComplexValue> getPhoneNumbers() {
        return phoneNumbers;
    }

    public List<SCIMComplexValue> getIms() {
        return ims;
    }

    public List<SCIMComplexValue> getPhotos() {
        return photos;
    }

    public List<SCIMUserAddress> getAddresses() {
        return addresses;
    }

    public List<Value> getX509Certificates() {
        return x509Certificates;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public List<Value> getEntitlements() {
        return entitlements;
    }

    public List<Value> getRoles() {
        return roles;
    }

    public SCIMEnterpriseInfo getEnterpriseInfo() {
        return enterpriseInfo;
    }

    public void setEnterpriseInfo(final SCIMEnterpriseInfo enterpriseInfo) {
        this.enterpriseInfo = enterpriseInfo;
    }

    public SCIMExtensionInfo getExtensionInfo() {
        return extensionInfo;
    }

    public void setExtensionInfo(final SCIMExtensionInfo extensionInfo) {
        this.extensionInfo = extensionInfo;
    }
}
