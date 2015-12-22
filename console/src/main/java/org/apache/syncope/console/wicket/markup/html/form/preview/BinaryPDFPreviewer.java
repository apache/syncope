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
package org.apache.syncope.console.wicket.markup.html.form.preview;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.syncope.console.preview.BinaryPreview;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.time.Time;

@BinaryPreview(mimeTypes = { "application/pdf" })
public class BinaryPDFPreviewer extends AbstractBinaryPreviewer {

    private static final long serialVersionUID = 7743389062928086373L;

    private static final int IMG_SIZE = 230;

    private static final int RESOLUTION = 96;

    private static final int IMAGE_TYPE = BufferedImage.TYPE_INT_RGB;

    private transient BufferedImage firstPage;

    public BinaryPDFPreviewer(final String id, final String mimeType, final byte[] uploadedBytes) {
        super(id, mimeType, uploadedBytes);
    }

    @Override
    public Component preview() {
        firstPage = null;

        PDDocument document = null;
        try {
            document = PDDocument.load(new ByteArrayInputStream(uploadedBytes));
            if (document.isEncrypted()) {
                LOG.info("Document is encrypted, no preview is possible");
            } else {
                @SuppressWarnings("unchecked")
                List<PDPage> pages = document.getDocumentCatalog().getAllPages();
                firstPage = pages.get(0).convertToImage(IMAGE_TYPE, RESOLUTION);
            }
        } catch (IOException e) {
            LOG.error("While generating thumbnail from first page", e);
        } finally {
            IOUtils.closeQuietly(document);
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

    private static class ThumbnailImageResource extends DynamicImageResource implements Serializable {

        private static final long serialVersionUID = 923201517955737928L;

        private transient final BufferedImage image;

        private transient byte[] thumbnail;

        public ThumbnailImageResource(final BufferedImage originalImage) {
            this.image = originalImage;
        }

        @Override
        protected byte[] getImageData(final IResource.Attributes attributes) {
            if (thumbnail == null) {
                final BufferedImage image = getScaledImageInstance();
                thumbnail = toImageData(image);
                setLastModifiedTime(Time.now());
            }
            return thumbnail;
        }

        private BufferedImage getScaledImageInstance() {
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
