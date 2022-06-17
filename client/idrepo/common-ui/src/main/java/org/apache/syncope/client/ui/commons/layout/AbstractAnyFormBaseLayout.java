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
package org.apache.syncope.client.ui.commons.layout;

import java.io.Serializable;
import org.apache.syncope.client.ui.commons.wizards.any.AnyForm;
import org.apache.syncope.common.lib.to.AnyTO;

public abstract class AbstractAnyFormBaseLayout<A extends AnyTO, F extends AnyForm<A>> implements Serializable {

    private static final long serialVersionUID = -6061683026789976508L;

    private Class<? extends F> formClass;

    private boolean auxClasses = true;

    private boolean groups = true;

    private boolean plainAttrs = true;

    private boolean derAttrs = true;

    private boolean virAttrs = true;

    private boolean resources = true;

    protected abstract Class<? extends F> getDefaultFormClass();

    public Class<? extends F> getFormClass() {
        return formClass == null ? getDefaultFormClass() : formClass;
    }

    public void setFormClass(final Class<? extends F> formClass) {
        this.formClass = formClass;
    }

    public boolean isAuxClasses() {
        return auxClasses;
    }

    public void setAuxClasses(final boolean auxClasses) {
        this.auxClasses = auxClasses;
    }

    public boolean isGroups() {
        return groups;
    }

    public void setGroups(final boolean groups) {
        this.groups = groups;
    }

    public boolean isPlainAttrs() {
        return plainAttrs;
    }

    public void setPlainAttrs(final boolean plainAttrs) {
        this.plainAttrs = plainAttrs;
    }

    public boolean isDerAttrs() {
        return derAttrs;
    }

    public void setDerAttrs(final boolean derAttrs) {
        this.derAttrs = derAttrs;
    }

    public boolean isVirAttrs() {
        return virAttrs;
    }

    public void setVirAttrs(final boolean virAttrs) {
        this.virAttrs = virAttrs;
    }

    public boolean isResources() {
        return resources;
    }

    public void setResources(final boolean resources) {
        this.resources = resources;
    }
}
