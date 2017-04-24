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
package org.apache.syncope.client.console.resources;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.util.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mounts directory on local filesystem as subcontext.
 */
public class FilesystemResource extends AbstractResource {

    private static final long serialVersionUID = -4791087117785935198L;

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemResource.class);

    private final String baseCtx;

    private final String basePath;

    public FilesystemResource(final String baseCtx, final String basePath) {
        this.baseCtx = baseCtx;
        this.basePath = basePath;
    }

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        ResourceResponse response = new ResourceResponse();

        final File baseDir = new File(basePath);
        if (baseDir.exists() && baseDir.canRead() && baseDir.isDirectory()) {
            String reqPath = attributes.getRequest().getUrl().getPath();
            final String subPath = reqPath.substring(reqPath.indexOf(baseCtx) + baseCtx.length()).
                    replace('/', File.separatorChar);
            LOG.debug("Request for {}", subPath);

            response.setTextEncoding(StandardCharsets.UTF_8.name());
            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    InputStream resourceIS = null;
                    try {
                        resourceIS = Files.newInputStream(new File(baseDir, subPath).toPath());
                        IOUtils.copy(resourceIS, attributes.getResponse().getOutputStream());
                    } catch (IOException e) {
                        LOG.error("Could not read from {}", baseDir.getAbsolutePath() + subPath, e);
                    } finally {
                        IOUtils.closeQuietly(resourceIS);
                    }
                }
            });
        } else {
            LOG.error("{} not found, not readable or not a directory", basePath);
        }

        return response;
    }

}
