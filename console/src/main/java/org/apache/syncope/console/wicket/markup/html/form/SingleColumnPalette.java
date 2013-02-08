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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.extensions.markup.html.form.palette.component.Selection;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.value.IValueMap;

/**
 * SingleColumnPalette is a components that allows the user to easily add, remove and order items in a single select
 * box.
 *
 * @see org.apache.wicket.extensions.markup.html.form.palette.Palette
 */
public class SingleColumnPalette<T> extends NonI18nPalette<T> {

    private static final long serialVersionUID = -1126599052871074501L;

    private AjaxLink addLink;

    private AjaxLink editLink;

    private AjaxLink removeLink;

    private List<Behavior> recordBehaviors;

    public SingleColumnPalette(final String id, final IModel<? extends List<? extends T>> model,
            final IChoiceRenderer<T> choiceRenderer, final int rows, final boolean allowOrder) {

        super(id, new ListModel<T>((List<T>) model.getObject()), model, choiceRenderer, rows, allowOrder);
        recordBehaviors = new ArrayList<Behavior>();
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if (editLink != null) {
            add(editLink);
        }

        for (Behavior behavior : recordBehaviors) {
            if (!getRecorderComponent().getBehaviors().contains(behavior)) {
                getRecorderComponent().add(behavior);
            }
        }
    }

    public AjaxLink getAddLink() {
        return addLink;
    }

    public void setAddLink(final AjaxLink addLink) {
        this.addLink = addLink;
    }

    public AjaxLink getEditLink() {
        return editLink;
    }

    public void setEditLink(final AjaxLink editLink) {
        this.editLink = editLink;
    }

    public AjaxLink getRemoveLink() {
        return removeLink;
    }

    public void setRemoveLink(final AjaxLink removeLink) {
        this.removeLink = removeLink;
    }

    public List<Behavior> getRecordBehaviors() {
        return recordBehaviors;
    }

    public T getSelectedItem() {
        return ((SelectableRecorder<T>) getRecorderComponent()).getSelectedItem();
    }

    public boolean addRecordBehavior(final Behavior behavior) {
        return !recordBehaviors.contains(behavior) && recordBehaviors.add(behavior);
    }

    public boolean removeRecordBehavior(final Behavior behavior) {
        return recordBehaviors.remove(behavior);
    }

    public void setRecordBehaviors(final List<Behavior> recordBehaviors) {
        this.recordBehaviors.clear();
        if (recordBehaviors != null && !recordBehaviors.isEmpty()) {
            this.recordBehaviors.addAll(recordBehaviors);
        }
    }

    @Override
    protected Component newAddComponent() {
        return addLink == null
                ? super.newAddComponent()
                : addLink;
    }

    @Override
    protected Component newRemoveComponent() {
        return removeLink == null
                ? super.newRemoveComponent()
                : removeLink;
    }

    @Override
    protected Recorder<T> newRecorderComponent() {
        return new SelectableRecorder<T>("recorder", this) {

            private static final long serialVersionUID = 3019792558927545591L;

            @Override
            public void updateModel() {
                super.updateModel();
                SingleColumnPalette.this.updateModel();
            }
        };
    }

    /**
     * Overriden from parent with purpose of removing ondblclick event and multiple selection.
     *
     * @return selected items component
     */
    @Override
    protected Component newSelectionComponent() {
        return new Selection<T>("selection", this) {

            private static final long serialVersionUID = -4146708301120705199L;

            @Override
            protected Map<String, String> getAdditionalAttributes(final Object choice) {
                return SingleColumnPalette.this.getAdditionalAttributesForSelection(choice);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                IValueMap attrs = tag.getAttributes();

                String onFocus = getPalette().getSelectionOnFocusJS();
                if (onFocus != null) {
                    attrs.put("onfocus", onFocus);
                }

                attrs.put("ondblclick", "");
                attrs.remove("multiple");
            }

            @Override
            public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                SingleColumnPalette.this.nonI18nOnComponentTagBody(markupStream, openTag, getOptionsIterator());
            }
        };
    }

    @Override
    protected Component newAvailableHeader(final String componentId) {
        Component availableHeader = super.newAvailableHeader(componentId);
        availableHeader.setVisible(false);
        return availableHeader;
    }

    @Override
    protected Component newSelectedHeader(final String componentId) {
        Component selectedHeader = super.newSelectedHeader(componentId);
        selectedHeader.setVisible(false);
        return selectedHeader;
    }

    public String getEditOnClickJS() {
        return buildJSCall("Syncope.SingleColumnPalette.choicesOnFocus");
    }

    @Override
    public String getChoicesOnFocusJS() {
        return "";
    }

    @Override
    public String getSelectionOnFocusJS() {
        return "";
    }

    @Override
    public String getAddOnClickJS() {
        return "";
    }
}
