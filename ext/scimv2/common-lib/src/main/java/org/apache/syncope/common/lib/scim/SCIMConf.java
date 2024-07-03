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

import java.io.Serializable;

public class SCIMConf implements Serializable {

    private static final long serialVersionUID = 5032136914118958611L;

    public static final String KEY = "scimv2.conf";

    private SCIMGeneralConf generalConf;

    private SCIMUserConf userConf;

    private SCIMEnterpriseUserConf enterpriseUserConf;

    private SCIMExtensionUserConf extensionUserConf;

    private SCIMGroupConf groupConf;

    public SCIMGeneralConf getGeneralConf() {
        return generalConf;
    }

    public void setGeneralConf(final SCIMGeneralConf generalConf) {
        this.generalConf = generalConf;
    }

    public SCIMUserConf getUserConf() {
        return userConf;
    }

    public void setUserConf(final SCIMUserConf userConf) {
        this.userConf = userConf;
    }

    public SCIMEnterpriseUserConf getEnterpriseUserConf() {
        return enterpriseUserConf;
    }

    public void setEnterpriseUserConf(final SCIMEnterpriseUserConf enterpriseUserConf) {
        this.enterpriseUserConf = enterpriseUserConf;
    }

    public SCIMExtensionUserConf getExtensionUserConf() {
        return extensionUserConf;
    }

    public void setExtensionUserConf(final SCIMExtensionUserConf extensionUserConf) {
        this.extensionUserConf = extensionUserConf;
    }

    public SCIMGroupConf getGroupConf() {
        return groupConf;
    }

    public void setGroupConf(final SCIMGroupConf groupConf) {
        this.groupConf = groupConf;
    }
}
