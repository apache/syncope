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
package org.apache.syncope.client.console.wizards.mapping;

import java.util.List;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

public class AuthModuleMappingPanel extends AbstractMappingPanel {

    private static final long serialVersionUID = -8940651851569691064L;

    public AuthModuleMappingPanel(final String id, final AuthModuleTO authModule) {
        super(id,
                null,
                null,
                new ListModel<>(authModule.getItems()),
                true,
                MappingPurpose.NONE);

        setOutputMarkupId(true);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        intAttrNameInfo.setVisible(false);
    }

    @Override
    protected boolean hidePurpose() {
        return true;
    }

    @Override
    protected boolean hideMandatory() {
        return true;
    }

    @Override
    protected boolean hideConnObjectKey() {
        return true;
    }

    @Override
    protected IModel<List<String>> getExtAttrNames() {
        return Model.ofList(List.of());
    }

    @Override
    protected void setAttrNames(final AjaxTextFieldPanel toBeUpdated) {
        // nothing to do
    }
}
