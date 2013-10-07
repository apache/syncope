package org.apache.syncope.console.pages.panels;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;

/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ImagePanel extends Panel {

    private static final long serialVersionUID = 5564818820574092960L;

    final Image img;

    public ImagePanel(final String id, final String img) {
        super(id);
        this.img = new Image("img", img);
        add(this.img);
    }

    @Override
    public Component add(Behavior... behaviors) {
        this.img.add(behaviors);
        return this;
    }
}
