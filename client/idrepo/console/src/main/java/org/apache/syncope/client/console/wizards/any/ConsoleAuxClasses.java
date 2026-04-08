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
package org.apache.syncope.client.console.wizards.any;

import java.util.List;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ConsoleAuxClasses extends WizardStep {

    private static final long serialVersionUID = 552437609667518888L;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    public <T extends AnyTO> ConsoleAuxClasses(final AnyWrapper<T> modelObject, final List<String> anyTypeClasses) {
        super();
        setOutputMarkupId(true);

        List<String> choices = anyTypeClassRestClient.list().stream().
                map(AnyTypeClassTO::getKey).filter(aux -> !anyTypeClasses.contains(aux)).
                sorted().
                toList();

        add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).build(
                "auxClasses",
                new PropertyModel<>(modelObject.getInnerObject(), "auxClasses"),
                new ListModel<>(choices)).
                hideLabel().setOutputMarkupId(true));

        // ------------------
        // insert changed label if needed
        // ------------------
        if (modelObject instanceof UserWrapper userWrapper
                && userWrapper.getPreviousUserTO() != null
                && !ListUtils.isEqualList(
                        userWrapper.getInnerObject().getAuxClasses(),
                        userWrapper.getPreviousUserTO().getAuxClasses())) {

            add(new LabelInfo("changed", StringUtils.EMPTY));
        } else {
            add(new Label("changed", StringUtils.EMPTY));
        }
        // ------------------
    }
}
