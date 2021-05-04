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
package org.apache.syncope.common.lib.auth;

public class JaasAuthModuleConf implements AuthModuleConf {

    private static final long serialVersionUID = -7775771400318503131L;

    /**
     * The realm that contains the login module information.
     */
    private String realm;

    /**
     * System property value to overwrite the realm in krb5 config.
     */
    private String kerberosRealmSystemProperty;

    /**
     * System property value to overwrite the kdc in krb5 config.
     */
    private String kerberosKdcSystemProperty;

    private String loginConfigType;

    private String loginConfigurationFile;

    public String getRealm() {
        return realm;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    public String getKerberosRealmSystemProperty() {
        return kerberosRealmSystemProperty;
    }

    public void setKerberosRealmSystemProperty(final String kerberosRealmSystemProperty) {
        this.kerberosRealmSystemProperty = kerberosRealmSystemProperty;
    }

    public String getKerberosKdcSystemProperty() {
        return kerberosKdcSystemProperty;
    }

    public void setKerberosKdcSystemProperty(final String kerberosKdcSystemProperty) {
        this.kerberosKdcSystemProperty = kerberosKdcSystemProperty;
    }

    public String getLoginConfigType() {
        return loginConfigType;
    }

    public void setLoginConfigType(final String loginConfigType) {
        this.loginConfigType = loginConfigType;
    }

    public String getLoginConfigurationFile() {
        return loginConfigurationFile;
    }

    public void setLoginConfigurationFile(final String loginConfigurationFile) {
        this.loginConfigurationFile = loginConfigurationFile;
    }
}
