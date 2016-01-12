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
package org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.behavior.Draggable;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.behavior.DraggableConfig;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.behavior.Resizable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.NotificationAwareComponent;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.buttons.DefaultModalCloseButton;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.buttons.PrimaryModalButton;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseModal<T extends Serializable> extends Modal<T> implements NotificationAwareComponent {

    private static final long serialVersionUID = -6142277554912316095L;

    protected static final Logger LOG = LoggerFactory.getLogger(BaseModal.class);

    /** the default id of the content component */
    public static final String CONTENT_ID = "content";

    private static final String SUBMIT = "submit";

    private static final String FORM = "form";

    protected NotificationPanel notificationPanel;

    private final List<Component> components;

    private WindowClosedCallback windowClosedCallback;

    private Panel content;

    private PrimaryModalButton submitButton;

    private final Form<T> form;

    public BaseModal(final String id) {
        super(id);

        form = new Form<>(FORM);
        form.setOutputMarkupId(true);
        add(form);

        content = new AbstractModalPanel<T>(this, null) {

            private static final long serialVersionUID = 1L;

        };

        notificationPanel = new NotificationPanel(Constants.FEEDBACK);
        notificationPanel.setOutputMarkupId(true);
        addOrReplace(notificationPanel);

        content.setOutputMarkupId(true);

        form.add(content);

        setUseCloseHandler(true);
        this.windowClosedCallback = null;
        components = new ArrayList<>();

        // Note: it would imply the adding of WebjarsJavaScriptResourceReference about JQuery resizable and mouse
        add(new Resizable().withChildSelector(".modal-content"));

        // Note: it would imply the adding of WebjarsJavaScriptResourceReference about JQuery draggable
        add(new Draggable(new DraggableConfig().withHandle(".modal-header").withCursor("move")));

        addButton(new DefaultModalCloseButton());
        setUseKeyboard(true);
        setFadeIn(true);
    }

    @Override
    public final Component add(final Behavior... behaviors) {
        return super.add(behaviors);
    }

    @Override
    public final MarkupContainer add(final Component... childs) {
        return super.add(childs);
    }

    @Override
    public final MarkupContainer addOrReplace(final Component... childs) {
        return super.addOrReplace(childs);
    }

    @Override
    public NotificationPanel getNotificationPanel() {
        return notificationPanel;
    }

    public Form<T> getForm() {
        return form;
    }

    public BaseModal<T> setFormModel(final T modelObject) {
        form.setModel(new CompoundPropertyModel<>(modelObject));
        return this;
    }

    public BaseModal<T> setFormModel(final IModel<T> model) {
        form.setModel(model);
        return this;
    }

    public T getFormModel() {
        return form.getModelObject();
    }

    public ModalPanel<T> getContent() {
        if (content instanceof ModalPanel) {
            return (ModalPanel<T>) content;
        }
        throw new IllegalStateException();
    }

    public BaseModal<T> setContent(final ModalPanel<T> component) {
        if (component instanceof Panel) {
            return setInternalContent(Panel.class.cast(component));
        }
        throw new IllegalArgumentException("Panel instance is required");

    }

    private BaseModal<T> setInternalContent(final Panel component) {
        if (!component.getId().equals(getContentId())) {
            throw new WicketRuntimeException(
                    "Modal content id is wrong. Component ID:" + component.getId() + "; content ID: " + getContentId());
        }

        content.replaceWith(component);
        content = component;

        return this;
    }

    public static String getContentId() {
        return CONTENT_ID;
    }

    public BaseModal<T> setWindowClosedCallback(final WindowClosedCallback callback) {
        windowClosedCallback = callback;
        return this;
    }

    @Override
    protected void onClose(final IPartialPageRequestHandler target) {
        if (windowClosedCallback != null) {
            windowClosedCallback.onClose((AjaxRequestTarget) target);
        }
    }

    public PrimaryModalButton addSumbitButton() {

        final PrimaryModalButton submit = new PrimaryModalButton(SUBMIT, SUBMIT, form) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                BaseModal.this.getContent().onSubmit(target, form);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                BaseModal.this.getContent().onError(target, form);
            }
        };

        submit.setOutputMarkupId(true);

        if (submitButton == null) {
            submitButton = submit;
            this.components.add(submitButton);
        } else {
            submitButton.replaceWith(submit);
            submitButton = submit;
        }

        return submit;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        final WebMarkupContainer dialog = (WebMarkupContainer) this.get("dialog");
        dialog.setMarkupId(this.getId());

        final WebMarkupContainer footer = (WebMarkupContainer) this.get("dialog:footer");
        footer.addOrReplace(new ListView<Component>("inputs", components) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<Component> item) {
                item.add(item.getModelObject());
            }
        }.setOutputMarkupId(true));
    }

    /**
     * Generic modal event.
     */
    public static class ModalEvent {

        /**
         * Request target.
         */
        private final AjaxRequestTarget target;

        /**
         * Constructor.
         *
         * @param target request target.
         */
        public ModalEvent(final AjaxRequestTarget target) {
            this.target = target;
        }

        /**
         * Target getter.
         *
         * @return request target.
         */
        public AjaxRequestTarget getTarget() {
            return target;
        }
    }
}
