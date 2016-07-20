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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.swt.graphics.Image;

public class HTMLTemplateAssistProcessor extends TemplateCompletionProcessor {

    protected Template[] getTemplates(final String contextTypeId) {
        HTMLTemplateManager manager = HTMLTemplateManager.getInstance();
        return manager.getTemplateStore().getTemplates();
    }

    protected TemplateContextType getContextType(final ITextViewer viewer, final IRegion region) {
        HTMLTemplateManager manager = HTMLTemplateManager.getInstance();
        return manager.getContextTypeRegistry().getContextType(HTMLContextType.CONTEXT_TYPE);
    }

    public ICompletionProposal[] computeCompletionProposals(final ITextViewer viewer, final int offsetinp) {

        int offset = offsetinp;
        ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();

        // adjust offset to end of normalized selection
        if (selection.getOffset() == offset) {
            offset = selection.getOffset() + selection.getLength();
        }

        String prefix = extractPrefix(viewer, offset);
        Region region = new Region(offset - prefix.length(), prefix.length());
        TemplateContext context = createContext(viewer, region);
        if (context == null) {
            return new ICompletionProposal[0];
        }
        context.setVariable("selection", selection.getText());
        Template[] templates = getTemplates(context.getContextType().getId());
        List<ICompletionProposal> matches = new ArrayList<ICompletionProposal>();
        for (int i = 0; i < templates.length; i++) {
            Template template = templates[i];
            try {
                context.getContextType().validate(template.getPattern());
            } catch (final TemplateException e) {
                continue;
            }
            if (template.getName().startsWith(prefix)
                && template.matches(prefix, context.getContextType().getId())) {
                matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));
            }
        }
        return matches.toArray(new ICompletionProposal[matches.size()]);
    }

    @Override
    protected Image getImage(final Template arg0) {
        return null;
    }

}
