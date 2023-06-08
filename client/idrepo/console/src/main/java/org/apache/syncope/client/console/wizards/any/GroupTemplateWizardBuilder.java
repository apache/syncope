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
import java.util.Optional;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.TemplatableTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;

public class GroupTemplateWizardBuilder extends GroupWizardBuilder implements TemplateWizardBuilder<GroupTO> {

    private static final long serialVersionUID = 6716803168859873877L;

    protected final TemplatableTO templatable;

    public GroupTemplateWizardBuilder(
            final TemplatableTO templatable,
            final List<String> anyTypeClasses,
            final GroupFormLayoutInfo formLayoutInfo,
            final GroupRestClient groupRestClient,
            final PageReference pageRef) {

        super(null, anyTypeClasses, formLayoutInfo, groupRestClient, pageRef);

        this.templatable = templatable;

        if (templatable.getTemplates().containsKey(AnyTypeKind.GROUP.name())) {
            setItem(new GroupWrapper(GroupTO.class.cast(templatable.getTemplates().get(AnyTypeKind.GROUP.name()))));
        } else {
            GroupTO groupTO = new GroupTO();
            if (templatable instanceof RealmTO) {
                groupTO.setRealm(
                        String.format("'%s'", RealmsUtils.getFullPath(RealmTO.class.cast(templatable).getFullPath())));
            }
            setItem(new GroupWrapper(groupTO));
        }
    }

    @Override
    protected Optional<Details<GroupTO>> addOptionalDetailsPanel(final AnyWrapper<GroupTO> modelObject) {
        Optional<Details<GroupTO>> details = super.addOptionalDetailsPanel(modelObject);
        if (templatable instanceof RealmTO) {
            details.ifPresent(Details::disableRealmSpecification);
        }
        return details;
    }

    @Override
    public AjaxWizard<AnyWrapper<GroupTO>> build(final String id) {
        return super.build(id, AjaxWizard.Mode.TEMPLATE);
    }
}
