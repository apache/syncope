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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorModalCloseBehavior;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.buttons.DefaultModalCloseButton;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.client.ui.commons.panels.NotificationPanel;
import org.apache.syncope.client.ui.commons.panels.SubmitableModalPanel;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseModal<T extends Serializable> extends Modal<T> {

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

    private AjaxSubmitLink submitButton;

    private final Form<T> form;

    private final DefaultModalCloseButton defaultModalCloseButton;

    private AjaxEventBehavior closeBehavior;

    private WebMarkupContainer footer;

    public BaseModal(final String id) {
        super(id);

        form = new Form<>(FORM);
        form.setOutputMarkupId(true);
        add(form);

        content = new AbstractModalPanel<>(this, null) {

            private static final long serialVersionUID = -6142277554912316095L;

        };

        content.setOutputMarkupId(true);

        form.add(content);

        useCloseHandler(true);
        this.windowClosedCallback = null;
        components = new ArrayList<>();

        // Note: not adding this would imply adding of WebjarsJavaScriptResourceReference about JQuery draggable
        add(new Draggable(new DraggableConfig().withHandle(".modal-header").withCursor("move")));

        defaultModalCloseButton = new DefaultModalCloseButton();
        addButton(defaultModalCloseButton);
        setUseKeyboard(true);
        setFadeIn(true);
        setBackdrop(Modal.Backdrop.STATIC);
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

    public BaseModal<T> setFormAsMultipart(final boolean multipart) {
        form.setMultiPart(multipart);
        return this;
    }

    public T getFormModel() {
        return form.getModelObject();
    }

    public ModalPanel getContent() {
        if (content instanceof ModalPanel modalPanel) {
            return modalPanel;
        }
        throw new IllegalStateException();
    }

    public BaseModal<T> setContent(final ModalPanel component) {
        if (component instanceof Panel) {
            return setInternalContent(Panel.class.cast(component));
        }
        throw new IllegalArgumentException("Panel instance is required");
    }

    public BaseModal<T> setContent(final ModalPanel component, final AjaxRequestTarget target) {
        setContent(component);
        target.add(content);
        return this;
    }

    public BaseModal<T> changeCloseButtonLabel(final String label) {
        defaultModalCloseButton.getModel().setObject(label);
        return this;
    }

    public BaseModal<T> changeCloseButtonLabel(final String label, final AjaxRequestTarget target) {
        changeCloseButtonLabel(label);
        target.add(defaultModalCloseButton);
        return this;
    }

    private BaseModal<T> setInternalContent(final Panel component) {
        if (!component.getId().equals(CONTENT_ID)) {
            throw new WicketRuntimeException("Modal content id is wrong. "
                    + "Component ID: " + component.getId() + "; content ID: " + CONTENT_ID);
        }

        content.replaceWith(component);
        content = component;

        return this;
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

    public AjaxSubmitLink addSubmitButton() {
        if (!(BaseModal.this.getContent() instanceof SubmitableModalPanel)) {
            throw new IllegalStateException();
        }

        AjaxSubmitLink submit = new AjaxSubmitLink(SUBMIT, form) {

            private static final long serialVersionUID = -5783994974426198290L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                SubmitableModalPanel.class.cast(BaseModal.this.getContent()).onSubmit(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                SubmitableModalPanel.class.cast(BaseModal.this.getContent()).onError(target);
            }
        };

        submit.setOutputMarkupId(true);

        if (submitButton == null) {
            submitButton = submit;
            components.add(submitButton);
        } else {
            submitButton.replaceWith(submit);
            submitButton = submit;
        }

        return submit;
    }

    public void removeSubmitButton() {
        if (!(BaseModal.this.getContent() instanceof SubmitableModalPanel)) {
            throw new IllegalStateException();
        }

        components.stream().
                filter(component -> SUBMIT.equals(component.getId())).
                findFirst().
                ifPresent(components::remove);

        submitButton = null;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        final WebMarkupContainer dialog = (WebMarkupContainer) this.get("dialog");
        dialog.setMarkupId(this.getId());

        footer = (WebMarkupContainer) this.get("dialog:footer");
        footer.addOrReplace(new ListView<>("inputs", components) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<Component> item) {
                item.add(item.getModelObject());
            }
        }.setOutputMarkupId(true)).setOutputMarkupId(true);
    }

    /**
     * Generic modal event.
     */
    public static class ModalEvent implements Serializable {

        private static final long serialVersionUID = 2668922412196063559L;

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

    public static class ChangeFooterVisibilityEvent extends ModalEvent {

        private static final long serialVersionUID = -6157576856659866343L;

        public ChangeFooterVisibilityEvent(final AjaxRequestTarget target) {
            super(target);
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ChangeFooterVisibilityEvent) {
            if (BaseModal.this.footer != null) {
                final AjaxRequestTarget target = ChangeFooterVisibilityEvent.class.cast(event.getPayload()).getTarget();
                target.add(BaseModal.this.footer.setEnabled(!BaseModal.this.footer.isEnabled()));
            }
        }
    }

    //--------------------------------------------------------
    // Required for SYNCOPE-846
    //--------------------------------------------------------
    /**
     * Sets whether the close handler is used or not. Default is false.
     *
     * @param useCloseHandler True if close handler should be used
     * @return This
     */
    public final Modal<T> useCloseHandler(final boolean useCloseHandler) {
        if (useCloseHandler) {
            if (closeBehavior == null) {
                closeBehavior = new IndicatorModalCloseBehavior() {

                    private static final long serialVersionUID = -4955472558917915340L;

                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        if (isVisible()) {
                            onClose(target);
                            appendCloseDialogJavaScript(target);
                        }
                    }
                };
                add(closeBehavior);
            }
        } else if (closeBehavior != null) {
            remove(closeBehavior);
            closeBehavior = null;
        }
        return this;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnDomReadyHeaderItem.forScript(createInitializerScript(getMarkupId(true))));
    }

    /**
     * creates the initializer script of the modal dialog.
     *
     * @param markupId The component's markup id
     * @return initializer script
     */
    private String createInitializerScript(final String markupId) {
        return addCloseHandlerScript(markupId, createBasicInitializerScript(markupId));
    }

    @Override
    protected String createBasicInitializerScript(final String markupId) {
        return "bootstrap.Modal.getOrCreateInstance(document.getElementById('" + markupId + "'))"
                + (showImmediately() ? ".show()" : "")
                + ";";
    }
    
    /**
     * adds close handler to initializer script, if use of close handler has been defined.
     *
     * @param markupId markup id
     * @param script base script to prepend
     * @return close handler script
     */
    private String addCloseHandlerScript(final String markupId, final String script) {
        if (closeBehavior != null) {
            return script + ";$('#" + markupId + "').on('hidden', function () { "
                    + "  Wicket.Ajax.ajax({'u':'" + closeBehavior.getCallbackUrl() + "','c':'" + markupId + "'});"
                    + "})";
        }

        return script;
    }
    //--------------------------------------------------------

    /**
     * Callback called after the window has been closed. If no callback instance is specified using
     * {@link BaseModal#setWindowClosedCallback(BaseModal.WindowClosedCallback)}, no ajax
     * request will be fired.
     */
    @FunctionalInterface
    public interface WindowClosedCallback extends IClusterable {

        /**
         * Called after the window has been closed.
         *
         * @param target {@link org.apache.wicket.ajax.AjaxRequestTarget} instance bound with the ajax request.
         */
        void onClose(AjaxRequestTarget target);
    }
}
