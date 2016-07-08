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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Details<T extends AnyTO> extends WizardStep {

    private static final long serialVersionUID = -8995647450549098844L;

    protected static final Logger LOG = LoggerFactory.getLogger(Details.class);

    protected final PageReference pageRef;

    protected final StatusPanel statusPanel;

    public Details(
            final AnyWrapper<T> wrapper,
            final IModel<List<StatusBean>> statusModel,
            final boolean templateMode,
            final boolean includeStatusPanel,
            final PageReference pageRef) {

        this.pageRef = pageRef;

        final T inner = wrapper.getInnerObject();

        final AjaxTextFieldPanel realm = new AjaxTextFieldPanel(
                "destinationRealm", "destinationRealm", new PropertyModel<String>(inner, "realm"), false);
        add(realm.setReadOnly(StringUtils.isNotEmpty(inner.getRealm())));
        if (templateMode) {
            realm.enableJexlHelp();
        }
        
        statusPanel = new StatusPanel("status", inner, statusModel, pageRef);

        add(statusPanel.setEnabled(includeStatusPanel).
                setVisible(includeStatusPanel).setRenderBodyOnly(true));

        add(getGeneralStatusInformation("generalStatusInformation", inner).
                setEnabled(includeStatusPanel).setVisible(includeStatusPanel).setRenderBodyOnly(true));
    }

    protected AnnotatedBeanPanel getGeneralStatusInformation(final String id, final T anyTO) {
        return new AnnotatedBeanPanel(id, anyTO);
    }
}
