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
package org.apache.syncope.core.logic;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SAML2SPEntitlement;
import org.apache.syncope.core.logic.saml2.SAML2ReaderWriter;
import org.apache.syncope.core.logic.saml2.SAML2IdPCache;
import org.apache.syncope.core.logic.saml2.SAML2IdPEntity;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPDAO;
import org.apache.syncope.core.persistence.api.entity.SAML2IdP;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPDataBinder;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class SAML2IdPLogic extends AbstractSAML2Logic<SAML2IdPTO> {

    @Autowired
    private SAML2IdPCache cache;

    @Autowired
    private SAML2IdPDataBinder binder;

    @Autowired
    private SAML2IdPDAO idpDAO;

    @Autowired
    private SAML2ReaderWriter saml2rw;

    private SAML2IdPTO complete(final SAML2IdP idp, final SAML2IdPTO idpTO) {
        SAML2IdPEntity idpEntity = cache.get(idpTO.getEntityID());
        if (idpEntity == null) {
            try {
                idpEntity = cache.put(idp);
            } catch (Exception e) {
                LOG.error("Could not build SAML 2.0 IdP with key ", idp.getEntityID(), e);
            }
        }

        idpTO.setLogoutSupported(idpEntity == null
                ? false
                : idpEntity.getSLOLocation(SAMLConstants.SAML2_POST_BINDING_URI) != null);
        return idpTO;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<SAML2IdPTO> list() {
        return CollectionUtils.collect(idpDAO.findAll(), new Transformer<SAML2IdP, SAML2IdPTO>() {

            @Override
            public SAML2IdPTO transform(final SAML2IdP input) {
                return complete(input, binder.getIdPTO(input));
            }
        }, new ArrayList<SAML2IdPTO>());
    }

    @PreAuthorize("hasRole('" + SAML2SPEntitlement.IDP_READ + "')")
    @Transactional(readOnly = true)
    public SAML2IdPTO read(final String key) {
        check();

        SAML2IdP idp = idpDAO.find(key);
        if (idp == null) {
            throw new NotFoundException("SAML 2.0 IdP '" + key + "'");
        }

        return complete(idp, binder.getIdPTO(idp));
    }

    private List<SAML2IdPTO> importIdPs(final InputStream input) throws Exception {
        List<EntityDescriptor> idpEntityDescriptors = new ArrayList<>();

        Element root = OpenSAMLUtil.getParserPool().parse(new InputStreamReader(input)).getDocumentElement();
        if (SAMLConstants.SAML20MD_NS.equals(root.getNamespaceURI())
                && EntityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME.equals(root.getLocalName())) {

            idpEntityDescriptors.add((EntityDescriptor) OpenSAMLUtil.fromDom(root));
        } else if (SAMLConstants.SAML20MD_NS.equals(root.getNamespaceURI())
                && EntitiesDescriptor.DEFAULT_ELEMENT_LOCAL_NAME.equals(root.getLocalName())) {

            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (SAMLConstants.SAML20MD_NS.equals(child.getNamespaceURI())
                        && EntityDescriptor.DEFAULT_ELEMENT_LOCAL_NAME.equals(child.getLocalName())) {

                    NodeList descendants = child.getChildNodes();
                    for (int j = 0; j < descendants.getLength(); j++) {
                        Node descendant = descendants.item(j);
                        if (SAMLConstants.SAML20MD_NS.equals(descendant.getNamespaceURI())
                                && IDPSSODescriptor.DEFAULT_ELEMENT_LOCAL_NAME.equals(descendant.getLocalName())) {

                            idpEntityDescriptors.add((EntityDescriptor) OpenSAMLUtil.fromDom((Element) child));
                        }
                    }
                }
            }
        }

        List<SAML2IdPTO> result = new ArrayList<>(idpEntityDescriptors.size());
        for (EntityDescriptor idpEntityDescriptor : idpEntityDescriptors) {
            SAML2IdPTO idpTO = new SAML2IdPTO();
            idpTO.setEntityID(idpEntityDescriptor.getEntityID());
            idpTO.setName(idpEntityDescriptor.getEntityID());
            idpTO.setUseDeflateEncoding(false);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                saml2rw.write(new OutputStreamWriter(baos), idpEntityDescriptor, false);
                idpTO.setMetadata(Base64.encodeBase64String(baos.toByteArray()));
            }
            MappingItemTO connObjectKeyItem = new MappingItemTO();
            connObjectKeyItem.setIntAttrName("username");
            connObjectKeyItem.setExtAttrName("NameID");
            idpTO.setConnObjectKeyItem(connObjectKeyItem);
            result.add(idpTO);

            cache.put(idpEntityDescriptor, connObjectKeyItem, false);
        }

        return result;
    }

    @PreAuthorize("hasRole('" + SAML2SPEntitlement.IDP_IMPORT + "')")
    public List<String> importFromMetadata(final InputStream input) {
        check();

        List<String> imported = new ArrayList<>();

        try {
            for (SAML2IdPTO idpTO : importIdPs(input)) {
                SAML2IdP idp = idpDAO.save(binder.create(idpTO));
                imported.add(idp.getKey());
            }
        } catch (SyncopeClientException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Unexpected error while importing IdP metadata", e);
            SyncopeClientException ex = SyncopeClientException.build(ClientExceptionType.InvalidEntity);
            ex.getElements().add(e.getMessage());
            throw ex;
        }

        return imported;
    }

    @PreAuthorize("hasRole('" + SAML2SPEntitlement.IDP_UPDATE + "')")
    public void update(final SAML2IdPTO saml2IdpTO) {
        check();

        SAML2IdP saml2Idp = idpDAO.find(saml2IdpTO.getKey());
        if (saml2Idp == null) {
            throw new NotFoundException("SAML 2.0 IdP '" + saml2IdpTO.getKey() + "'");
        }

        saml2Idp = idpDAO.save(binder.update(saml2Idp, saml2IdpTO));

        SAML2IdPEntity idpEntity = cache.get(saml2Idp.getEntityID());
        if (idpEntity != null) {
            idpEntity.setUseDeflateEncoding(saml2Idp.isUseDeflateEncoding());
            idpEntity.setConnObjectKeyItem(binder.getIdPTO(saml2Idp).getConnObjectKeyItem());
        }
    }

    @PreAuthorize("hasRole('" + SAML2SPEntitlement.IDP_DELETE + "')")
    public void delete(final String key) {
        check();

        SAML2IdP idp = idpDAO.find(key);
        if (idp == null) {
            throw new NotFoundException("SAML 2.0 IdP '" + key + "'");
        }

        idpDAO.delete(key);
        cache.remove(idp.getEntityID());
    }

    @Override
    protected SAML2IdPTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof SAML2IdPTO) {
                    key = ((SAML2IdPTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                SAML2IdP idp = idpDAO.find(key);
                return complete(idp, binder.getIdPTO(idp));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

}
