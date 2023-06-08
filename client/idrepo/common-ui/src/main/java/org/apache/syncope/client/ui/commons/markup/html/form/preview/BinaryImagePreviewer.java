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
package org.apache.syncope.client.ui.commons.markup.html.form.preview;

import org.apache.syncope.client.ui.commons.annotations.BinaryPreview;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.image.resource.ThumbnailImageResource;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;

@BinaryPreview(mimeTypes = { "image/jpeg", "image/png", "image/gif", "image/bmp", "image/x-png", "image/vnd.wap.wbmp" })
public class BinaryImagePreviewer extends BinaryPreviewer {

    private static final long serialVersionUID = 3338812359368457349L;

    protected static final int IMG_SIZE = 300;

    public BinaryImagePreviewer(final String mimeType) {
        super(mimeType);
    }

    @Override
    public Component preview(final byte[] uploadedBytes) {
        return addOrReplace(new NonCachingImage("previewImage", new ThumbnailImageResource(new DynamicImageResource() {

            private static final long serialVersionUID = 923201517955737928L;

            @Override
            protected byte[] getImageData(final IResource.Attributes attributes) {
                return uploadedBytes;
            }
        }, IMG_SIZE)));
    }
}
