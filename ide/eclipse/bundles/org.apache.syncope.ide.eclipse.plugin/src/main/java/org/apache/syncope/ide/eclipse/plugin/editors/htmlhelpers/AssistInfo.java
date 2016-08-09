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
package org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;

public class AssistInfo {

    private String displayString;
    private String replaceString;
    private Image image;
    private String additionalInfo;

    public AssistInfo(final String displayString) {
        this.displayString = displayString;
        this.replaceString = displayString;
    }

    public AssistInfo(final String displayString, final Image image) {
        this.displayString = displayString;
        this.replaceString = displayString;
        this.image = image;
    }

    public AssistInfo(final String replaceString, final String displayString) {
        this.displayString = displayString;
        this.replaceString = replaceString;
    }

    public AssistInfo(final String replaceString, final String displayString, final Image image) {
        this.displayString = displayString;
        this.replaceString = replaceString;
        this.image = image;
    }

    public AssistInfo(final String replaceString, final String displayString, final Image image,
            final String additionalInfo) {
        this.displayString = displayString;
        this.replaceString = replaceString;
        this.image = image;
        this.additionalInfo = additionalInfo;
    }

    public String getDisplayString() {
        return displayString;
    }

    public String getReplaceString() {
        return replaceString;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public Image getImage() {
        return this.image;
    }

    public ICompletionProposal toCompletionProposal(final int offset, final String matchString,
            final Image defaultImage) {
        return new CompletionProposal(
                getReplaceString(),
                offset - matchString.length(), matchString.length(),
                getReplaceString().length(),
                getImage() == null ? defaultImage : getImage(),
                getDisplayString(), null, getAdditionalInfo());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof AssistInfo) {
            AssistInfo info = (AssistInfo) obj;
            if (compareString(info.getReplaceString(), getReplaceString())
                && compareString(info.getDisplayString(), getDisplayString())
                && compareString(info.getAdditionalInfo(), getAdditionalInfo())) {
                return true;
            }
        }
        return false;
    }

    @Override public int hashCode() {
        return (this.getReplaceString().hashCode()
            + this.getDisplayString().hashCode()
            + this.getAdditionalInfo().hashCode());
    }

    public static boolean compareString(final String value1, final String value2) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 != null && value1.equals(value2)) {
            return true;
        }
        return false;
    }

}
