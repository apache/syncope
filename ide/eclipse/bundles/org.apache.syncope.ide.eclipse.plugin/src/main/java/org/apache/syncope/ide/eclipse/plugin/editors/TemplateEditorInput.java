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
package org.apache.syncope.ide.eclipse.plugin.editors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public class TemplateEditorInput implements IStorageEditorInput {

    private final String inputString;
    private final String title;
    private final String tooltip;

    private String[] inputStringList;
    private String[] titleList;
    private String[] tooltipList;

    public String[] getInputStringList() {
        return inputStringList;
    }

    public String[] getTitleList() {
        return titleList;
    }

    public String[] getTooltipList() {
        return tooltipList;
    }

    public String getInputString() {
        return inputString;
    }

    public TemplateEditorInput(final String inputString, final String title, final String tooltip) {
        this.inputString = inputString;
        this.title = title;
        this.tooltip = tooltip;
    }

    public TemplateEditorInput(final String[] inputStringList, final String[] titleList,
            final String[] tooltipList) {
        this.inputString = null;
        this.title = null;
        this.tooltip = null;

        this.inputStringList = inputStringList;
        this.titleList = titleList;
        this.tooltipList = tooltipList;
    }

    public boolean exists() {
        return false;
    }

    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    public IPersistableElement getPersistable() {
        return null;
    }

    public <T> T getAdapter(final Class<T> adapter) {
        return null;
    }

    public String getName() {
        return title;
    }

    public String getToolTipText() {
        return tooltip;
    }

    public IStorage getStorage() throws CoreException {
        return new IStorage() {
            public InputStream getContents() throws CoreException {
                return new ByteArrayInputStream(inputString.getBytes(
                    java.nio.charset.Charset.defaultCharset()));
            }

            public IPath getFullPath() {
                return null;
            }

            public String getName() {
                return TemplateEditorInput.this.getName();
            }

            public boolean isReadOnly() {
                return false;
            }

            public <T> T getAdapter(final Class<T> adapter) {
                return null;
            }
        };
    }
}
