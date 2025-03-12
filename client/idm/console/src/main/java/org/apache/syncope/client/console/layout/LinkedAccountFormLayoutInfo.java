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
package org.apache.syncope.client.console.layout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.wizards.any.LinkedAccountWizardBuilder;

public class LinkedAccountFormLayoutInfo implements Serializable {

    private static final long serialVersionUID = -5573691733739618511L;

    private Class<? extends LinkedAccountForm> formClass;

    private boolean credentials = true;

    private final List<String> whichCredentials = new ArrayList<>();

    private boolean plainAttrs = true;

    private final List<String> whichPlainAttrs = new ArrayList<>();

    protected Class<? extends LinkedAccountForm> getDefaultFormClass() {
        return LinkedAccountWizardBuilder.class;
    }

    public Class<? extends LinkedAccountForm> getFormClass() {
        return formClass == null ? getDefaultFormClass() : formClass;
    }

    public void setFormClass(final Class<? extends LinkedAccountForm> formClass) {
        this.formClass = formClass;
    }

    public boolean isPlainAttrs() {
        return plainAttrs;
    }

    public void setPlainAttrs(final boolean plainAttrs) {
        this.plainAttrs = plainAttrs;
    }

    public List<String> getWhichPlainAttrs() {
        return whichPlainAttrs;
    }

    public boolean isCredentials() {
        return credentials;
    }

    public void setCredentials(final boolean credentials) {
        this.credentials = credentials;
    }

    public List<String> getWhichCredentials() {
        return whichCredentials;
    }
}
