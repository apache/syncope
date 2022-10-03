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
package org.apache.syncope.fit.core.reference;

import org.apache.syncope.common.lib.policy.CommandArgs;

public class TestCommandArgs implements CommandArgs {

    private static final long serialVersionUID = 1408260716514938521L;

    private String name = "Test";

    private String parentRealm = "/even/two";

    private String realmName = "realm123";

    private String printerName = "printer123";

    @Override
    public String getName() {
        return name;
    }

    public String getParentRealm() {
        return parentRealm;
    }

    public String getRealmName() {
        return realmName;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setParentRealm(final String parentRealm) {
        this.parentRealm = parentRealm;
    }

    public void setRealmName(final String realmName) {
        this.realmName = realmName;
    }

    public void setPrinterName(final String printerName) {
        this.printerName = printerName;
    }

    @Override
    public String toString() {
        return "TestCommandArgs{"
                + "name=" + name
                + ", parentRealm=" + parentRealm
                + ", realmName=" + realmName
                + ", printerName=" + printerName
                + '}';
    }
}
