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
package org.apache.syncope.client.enduser.panels;

import java.util.List;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.commons.EnduserConstants;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.panels.any.DerAttrs;
import org.apache.syncope.client.enduser.panels.any.Details;
import org.apache.syncope.client.enduser.panels.any.Groups;
import org.apache.syncope.client.enduser.panels.any.PlainAttrs;
import org.apache.syncope.client.enduser.panels.any.Resources;
import org.apache.syncope.client.enduser.panels.any.VirAttrs;
import org.apache.syncope.client.enduser.panels.captcha.CaptchaPanel;
import org.apache.syncope.client.ui.commons.panels.CardPanel;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;

public abstract class AnyFormPanel extends AbstractAnyFormPanel<UserWrapper> {

    private static final long serialVersionUID = -2720486919461006370L;

    protected final List<String> anyTypeClasses;

    protected CaptchaPanel<Void> captcha;

    protected UserFormLayoutInfo formLayoutInfo;

    public AnyFormPanel(final String id,
            final UserTO anyTO,
            final List<String> anyTypeClasses,
            final UserFormLayoutInfo formLayoutInfo,
            final PageReference pageReference) {

        super(id, new UserWrapper(anyTO), pageReference);

        this.formLayoutInfo = formLayoutInfo;
        this.anyTypeClasses = anyTypeClasses;
    }

    @SuppressWarnings("unchecked")
    public AnyFormPanel(final String id,
            final UserWrapper wrapper,
            final List<String> anyTypeClasses,
            final UserFormLayoutInfo formLayoutInfo,
            final PageReference pageReference) {

        super(id, wrapper, pageReference);

        this.formLayoutInfo = formLayoutInfo;
        this.anyTypeClasses = anyTypeClasses;
    }

    protected Details<UserTO> addOptionalDetailsPanel(final UserWrapper modelObject) {
        Details<UserTO> details = new Details<>(EnduserConstants.CONTENT_PANEL, pageRef);
        details.setOutputMarkupId(true);
        return details;
    }

    @Override
    protected void buildLayout(final UserWrapper modelObject) {
        form.add(new CardPanel.Builder<>()
                .setName("details")
                .setComponent(addOptionalDetailsPanel(modelObject))
                .isVisible(formLayoutInfo.isDetailsManagement()).build("userDetailsPanelCard"));

        Groups groups = new Groups(EnduserConstants.CONTENT_PANEL, modelObject);
        setOutputMarkupId(true);

        form.add(new CardPanel.Builder<Groups>()
                .setName("groups")
                .setComponent(groups)
                .isVisible(formLayoutInfo.isGroups()).build("groupsPanelCard"));

        PlainAttrs plainAttrs = new PlainAttrs(EnduserConstants.CONTENT_PANEL,
                modelObject, anyTypeClasses, formLayoutInfo.getWhichPlainAttrs());
        plainAttrs.setOutputMarkupId(true);

        form.add(new CardPanel.Builder<PlainAttrs>()
                .setName("attributes.plain")
                .setComponent(plainAttrs)
                .isVisible(formLayoutInfo.isPlainAttrs() && plainAttrs.isPanelVisible()).build("plainAttrsPanelCard"));

        DerAttrs derAttrs = new DerAttrs(EnduserConstants.CONTENT_PANEL,
                modelObject, anyTypeClasses, formLayoutInfo.getWhichDerAttrs());
        derAttrs.setOutputMarkupId(true);

        form.add(new CardPanel.Builder<DerAttrs>()
                .setName("attributes.derived")
                .setComponent(derAttrs)
                .isVisible(formLayoutInfo.isVirAttrs() && derAttrs.isPanelVisible()).build("derAttrsPanelCard"));

        VirAttrs virAttrs = new VirAttrs(EnduserConstants.CONTENT_PANEL,
                modelObject, anyTypeClasses, formLayoutInfo.getWhichVirAttrs());
        virAttrs.setOutputMarkupId(true);

        form.add(new CardPanel.Builder<VirAttrs>()
                .setName("attributes.virtual")
                .setComponent(virAttrs)
                .isVisible(formLayoutInfo.isVirAttrs() && virAttrs.isPanelVisible()).build("virAttrsPanelCard"));

        Resources resources = new Resources(EnduserConstants.CONTENT_PANEL, modelObject);
        resources.setOutputMarkupId(true);

        form.add(new CardPanel.Builder<Resources>()
                .setName("resources")
                .setComponent(resources)
                .isVisible(formLayoutInfo.isResources()).build("resourcesPanelCard"));

        // add captcha
        captcha = new CaptchaPanel<>(EnduserConstants.CONTENT_PANEL);
        captcha.setOutputMarkupPlaceholderTag(true);

        form.add(new CardPanel.Builder<CaptchaPanel<Void>>()
                .setName("captcha")
                .setComponent(captcha)
                .isVisible(SyncopeWebApplication.get().isCaptchaEnabled()).build("captchaPanelCard"));
    }

    protected void fixPlainAndVirAttrs(final AnyTO updated, final AnyTO original) {
        // re-add to the updated object any missing plain or virtual attribute (compared to original): this to cope with
        // form layout, which might have not included some plain or virtual attributes
        for (Attr plainAttr : original.getPlainAttrs()) {
            if (updated.getPlainAttr(plainAttr.getSchema()).isEmpty()) {
                updated.getPlainAttrs().add(plainAttr);
            }
        }
        for (Attr virAttr : original.getVirAttrs()) {
            if (updated.getVirAttr(virAttr.getSchema()).isEmpty()) {
                updated.getVirAttrs().add(virAttr);
            }
        }

        if (updated instanceof GroupableRelatableTO && original instanceof GroupableRelatableTO) {
            GroupableRelatableTO.class
                    .cast(original).getMemberships().forEach(oMemb -> {
                GroupableRelatableTO.class
                        .cast(updated).getMembership(oMemb.getGroupKey()).ifPresent(uMemb -> {
                    oMemb.getPlainAttrs()
                            .stream().
                            filter(attr -> uMemb.getPlainAttr(attr.getSchema()).isEmpty()).
                            forEach(attr -> uMemb.getPlainAttrs().add(attr));
                    oMemb.getVirAttrs()
                            .stream().
                            filter(attr -> uMemb.getVirAttr(attr.getSchema()).isEmpty()).
                            forEach(attr -> uMemb.getVirAttrs().add(attr));
                }
                );
            });
        }

        // remove from the updated object any plain or virtual attribute without values, thus triggering for removal in
        // the generated patch
        updated.getPlainAttrs().removeIf(attr -> attr.getValues().isEmpty());
        updated.getVirAttrs().removeIf(attr -> attr.getValues().isEmpty());
        if (updated instanceof GroupableRelatableTO) {
            GroupableRelatableTO.class.cast(updated).getMemberships().forEach(memb -> {
                memb.getPlainAttrs().removeIf(attr -> attr.getValues().isEmpty());
                memb.getVirAttrs().removeIf(attr -> attr.getValues().isEmpty());
            });
        }
    }
}
