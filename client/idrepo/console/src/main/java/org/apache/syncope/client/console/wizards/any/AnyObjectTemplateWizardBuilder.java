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
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.TemplatableTO;
import org.apache.wicket.PageReference;

public class AnyObjectTemplateWizardBuilder extends AnyObjectWizardBuilder
        implements TemplateWizardBuilder<AnyObjectTO> {

    private static final long serialVersionUID = 6716803168859873877L;

    private final TemplatableTO templatable;

    public AnyObjectTemplateWizardBuilder(
            final TemplatableTO templatable,
            final String anyType,
            final List<String> anyTypeClasses,
            final AnyObjectFormLayoutInfo formLayoutInfo,
            final PageReference pageRef) {
        super(null, anyTypeClasses, formLayoutInfo, pageRef);
        this.templatable = templatable;

        if (templatable.getTemplates().containsKey(anyType)) {
            setItem(new AnyWrapper<>(AnyObjectTO.class.cast(templatable.getTemplates().get(anyType))));
        } else {
            AnyObjectTO anyObjectTO = new AnyObjectTO();
            anyObjectTO.setType(anyType);
            if (templatable instanceof RealmTO) {
                anyObjectTO.setRealm(String.format("'%s'", RealmTO.class.cast(templatable).getFullPath()));
            }
            setItem(new AnyWrapper<>(anyObjectTO));
        }
    }

    @Override
    protected Details<AnyObjectTO> addOptionalDetailsPanel(final AnyWrapper<AnyObjectTO> modelObject) {
        final Details<AnyObjectTO> details = super.addOptionalDetailsPanel(modelObject);
        if (templatable instanceof RealmTO) {
            details.disableRealmSpecification();
        }
        return details;
    }

    @Override
    public AjaxWizard<AnyWrapper<AnyObjectTO>> build(final String id) {
        return super.build(id, AjaxWizard.Mode.TEMPLATE);
    }
}
