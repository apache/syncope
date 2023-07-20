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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.DefaultResourceCache;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.syncope.client.ui.commons.annotations.BinaryPreview;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;

@BinaryPreview(mimeTypes = { "application/pdf" })
public class BinaryPDFPreviewer extends BinaryPreviewer {

    private static final long serialVersionUID = -6606409541566687016L;

    private static final int IMG_SIZE = 300;

    private static final float DPI = 100;

    private static final ImageType IMAGE_TYPE = ImageType.RGB;

    private transient BufferedImage firstPage;

    public BinaryPDFPreviewer(final String mimeType) {
        super(mimeType);
    }

    @Override
    public Component preview(final byte[] uploadedBytes) {
        firstPage = null;

        try (PDDocument document = Loader.loadPDF(uploadedBytes)) {
            document.setResourceCache(new DefaultResourceCache() {

                @Override
                public void put(final COSObject indirect, final PDXObject xobject) {
                    // no cache
                }
            });
            if (document.isEncrypted()) {
                LOG.info("Document is encrypted, no preview is possible");
            } else {
                firstPage = new PDFRenderer(document).renderImageWithDPI(0, DPI, IMAGE_TYPE);
            }
        } catch (IOException e) {
            LOG.error("While generating thumbnail from first page", e);
        }

        Fragment fragment;
        if (firstPage == null) {
            fragment = new Fragment("preview", "noPreviewFragment", this);
        } else {
            fragment = new Fragment("preview", "previewFragment", this);
            fragment.add(new NonCachingImage("previewImage", new ThumbnailImageResource(firstPage)));
        }

        WebMarkupContainer previewContainer = new WebMarkupContainer("previewContainer");
        previewContainer.setOutputMarkupId(true);
        previewContainer.add(fragment);
        return this.addOrReplace(previewContainer);
    }

    protected static class ThumbnailImageResource extends DynamicImageResource implements Serializable {

        private static final long serialVersionUID = 923201517955737928L;

        protected final transient BufferedImage image;

        protected transient byte[] thumbnail;

        protected ThumbnailImageResource(final BufferedImage image) {
            this.image = image;
        }

        @Override
        protected byte[] getImageData(final IResource.Attributes attributes) {
            if (thumbnail == null) {
                thumbnail = toImageData(getScaledImageInstance());
                setLastModifiedTime(Instant.now());
            }
            return thumbnail;
        }

        protected BufferedImage getScaledImageInstance() {
            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();

            if ((originalWidth > IMG_SIZE) || (originalHeight > IMG_SIZE)) {
                final int newWidth;
                final int newHeight;

                if (originalWidth > originalHeight) {
                    newWidth = IMG_SIZE;
                    newHeight = (IMG_SIZE * originalHeight) / originalWidth;
                } else {
                    newWidth = (IMG_SIZE * originalWidth) / originalHeight;
                    newHeight = IMG_SIZE;
                }

                // http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
                BufferedImage dimg = new BufferedImage(newWidth, newHeight, image.getType());
                Graphics2D g = dimg.createGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(image, 0, 0, newWidth, newHeight, 0, 0, originalWidth,
                            originalHeight, null);
                } finally {
                    g.dispose();
                }

                return dimg;
            }

            // no need for resizing
            return image;
        }
    }
}
