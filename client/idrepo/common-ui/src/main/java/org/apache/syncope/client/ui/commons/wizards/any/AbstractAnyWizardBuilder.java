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

import org.apache.syncope.client.ui.commons.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.wicket.PageReference;

public abstract class AbstractAnyWizardBuilder<A extends AnyTO> extends AjaxWizardBuilder<AnyWrapper<A>> {

    private static final long serialVersionUID = -2480279868319546243L;

    public AbstractAnyWizardBuilder(final AnyWrapper<A> defaultItem, final PageReference pageRef) {
        super(defaultItem, pageRef);
    }

    protected void fixPlainAndVirAttrs(final AnyTO updated, final AnyTO original) {
        // re-add to the updated object any missing plain or virtual attribute (compared to original): this to cope with
        // form layout, which might have not included some plain or virtual attributes
        original.getPlainAttrs().stream().
                filter(attr -> updated.getPlainAttr(attr.getSchema()).isPresent()).
                forEach(attr -> updated.getPlainAttrs().add(attr));
        original.getVirAttrs().stream().
                filter(attr -> updated.getVirAttr(attr.getSchema()).isPresent()).
                forEach(attr -> updated.getVirAttrs().add(attr));
        if (updated instanceof GroupableRelatableTO && original instanceof GroupableRelatableTO) {
            GroupableRelatableTO.class.cast(original).getMemberships()
                    .forEach(oMemb -> GroupableRelatableTO.class.cast(updated).getMembership(oMemb.getGroupKey())
                    .ifPresent(uMemb -> {
                        oMemb.getPlainAttrs().stream().
                                filter(attr -> uMemb.getPlainAttr(attr.getSchema()).isPresent()).
                                forEach(attr -> uMemb.getPlainAttrs().add(attr));
                        oMemb.getVirAttrs().stream().
                                filter(attr -> uMemb.getVirAttr(attr.getSchema()).isPresent()).
                                forEach(attr -> uMemb.getVirAttrs().add(attr));
                    }));
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
