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
package org.apache.syncope.client.console.wicket.markup.html.form;

import java.io.Serializable;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;

public class ImageModalPanel<T extends Serializable> extends AbstractModalPanel<T> {

    private static final long serialVersionUID = 5044632306261219075L;

    public ImageModalPanel(final BaseModal<T> modal, final byte[] content, final PageReference pageRef) {
        super(modal, pageRef);

        Image image = new Image("image", new Model<IResource>()) {

            private static final long serialVersionUID = -8457850449086490660L;

            @Override
            protected IResource getImageResource() {
                return new DynamicImageResource() {

                    private static final long serialVersionUID = 923201517955737928L;

                    @Override
                    protected byte[] getImageData(final IResource.Attributes attributes) {
                        return content;
                    }
                };
            }
        };
        image.setOutputMarkupId(true);
        add(image);
    }
}
