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
package org.apache.syncope.client.console.panels;

import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMExtensionAnyConf;

public class SCIMConfExtensionGroupPanel extends SCIMConfExtensionAnyPanel {

    private static final long serialVersionUID = -3719006384765921047L;

    public SCIMConfExtensionGroupPanel(final String id, final SCIMConf scimConf, final String anyTypeKey) {
        super(id, scimConf, anyTypeKey);
    }

    @Override
    public SCIMExtensionAnyConf getExtensionAnyConf(final SCIMConf scimConf) {
        if (scimConf.getExtensionGroupConf() == null) {
            scimConf.setExtensionGroupConf(new SCIMExtensionAnyConf());
        }
        return scimConf.getExtensionGroupConf();
    }
}
