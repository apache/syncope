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
package org.apache.syncope.console.wicket.markup.html.form;

import org.apache.syncope.common.types.MappingPurpose;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class MappingPurposePanel extends Panel {

    private static final long serialVersionUID = 322966537010107771L;

    private final AjaxLink<Void> propagation;

    private final AjaxLink<Void> synchronization;

    private final AjaxLink<Void> both;

    private final AjaxLink<Void> none;

    public MappingPurposePanel(final String componentId, final IModel<MappingPurpose> model, final WebMarkupContainer container) {
        super(componentId, model);

        propagation = new AjaxLink<Void>("propagationPurposeLink") {

            private static final long serialVersionUID = -6957616042924610305L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                model.setObject(MappingPurpose.PROPAGATION);
                setOpacity(MappingPurpose.PROPAGATION);
                target.add(container);
            }
        };

        synchronization = new AjaxLink<Void>("synchronizationPurposeLink") {

                    private static final long serialVersionUID = -6957616042924610305L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        model.setObject(MappingPurpose.SYNCHRONIZATION);
                        setOpacity(MappingPurpose.SYNCHRONIZATION);
                        target.add(container);
                    }
                };

        both = new AjaxLink<Void>("bothPurposeLink") {

            private static final long serialVersionUID = -6957616042924610305L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                model.setObject(MappingPurpose.BOTH);
                setOpacity(MappingPurpose.BOTH);
                target.add(container);
            }
        };

        none = new AjaxLink<Void>("nonePurposeLink") {

            private static final long serialVersionUID = -6957616042924610305L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                model.setObject(MappingPurpose.NONE);
                setOpacity(MappingPurpose.NONE);
                target.add(container);
            }
        };

        add(propagation);
        add(synchronization);
        add(both);
        add(none);

        setOpacity(model.getObject());
    }

    private void setOpacity(final MappingPurpose mappingPurpose) {
        switch (mappingPurpose) {
            case PROPAGATION:
                propagation.add(new AttributeModifier("style", new Model<String>("opacity: 1;")));
                synchronization.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                both.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                none.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                break;
            case SYNCHRONIZATION:
                synchronization.add(new AttributeModifier("style", new Model<String>("opacity: 1;")));
                propagation.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                both.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                none.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                break;
            case BOTH:
                both.add(new AttributeModifier("style", new Model<String>("opacity: 1;")));
                propagation.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                synchronization.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                none.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                break;
            case NONE:
                none.add(new AttributeModifier("style", new Model<String>("opacity: 1;")));
                synchronization.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                propagation.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                both.add(new AttributeModifier("style", new Model<String>("opacity: 0.3;")));
                break;
            default:
            // do nothing
        }
    }
}
