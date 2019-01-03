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
package org.apache.syncope.client.ui.commons.wizards.any;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public abstract class AbstractAuxClasses extends WizardStep {

    private static final long serialVersionUID = 552437609667518888L;

    public <T extends AnyTO> AbstractAuxClasses(final AnyWrapper<T> modelObject, final List<String> anyTypeClasses) {
        super();
        setOutputMarkupId(true);

        List<AnyTypeClassTO> allAnyTypeClasses = listAnyTypecClasses();

        List<String> choices = new ArrayList<>();
        for (AnyTypeClassTO aux : allAnyTypeClasses) {
            if (!anyTypeClasses.contains(aux.getKey())) {
                choices.add(aux.getKey());
            }
        }
        Collections.sort(choices);
        add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).build("auxClasses",
                new PropertyModel<>(modelObject.getInnerObject(), "auxClasses"),
                new ListModel<>(choices)).hideLabel().setOutputMarkupId(true));

        // ------------------
        // insert changed label if needed
        // ------------------
        if (modelObject instanceof UserWrapper
                && UserWrapper.class.cast(modelObject).getPreviousUserTO() != null
                && !ListUtils.isEqualList(
                        modelObject.getInnerObject().getAuxClasses(),
                        UserWrapper.class.cast(modelObject).getPreviousUserTO().getAuxClasses())) {
            add(new LabelInfo("changed", StringUtils.EMPTY));
        } else {
            add(new Label("changed", StringUtils.EMPTY));
        }
        // ------------------
    }

    protected abstract List<AnyTypeClassTO> listAnyTypecClasses();
}
