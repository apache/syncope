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
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class AnyObjectDetails extends Details<AnyObjectTO> {

    private static final long serialVersionUID = 855618618337931784L;

    public AnyObjectDetails(
            final AnyWrapper<AnyObjectTO> wrapper,
            final IModel<List<StatusBean>> statusModel,
            final boolean templateMode,
            final boolean includeStatusPanel,
            final PageReference pageRef) {

        super(wrapper, statusModel, includeStatusPanel, pageRef);

        AnyObjectTO anyObjectTO = wrapper.getInnerObject();

        AjaxTextFieldPanel name = new AjaxTextFieldPanel("name", "name",
                new PropertyModel<String>(anyObjectTO, "name"), false);
        if (templateMode) {
            name.enableJexlHelp();
        } else {
            name.addRequiredLabel();
        }
        this.add(name);
    }
}
