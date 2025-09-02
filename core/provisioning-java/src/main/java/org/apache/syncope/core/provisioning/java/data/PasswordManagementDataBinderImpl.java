package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.provisioning.api.data.PasswordManagementDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordManagementDataBinderImpl implements PasswordManagementDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(PasswordManagementDataBinder.class);

    protected final EntityFactory entityFactory;

    public PasswordManagementDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

//    protected void populateItems(final PasswordManagementTO passwordManagementTO, final PasswordManagement passwordManagement) {
//        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
//        SyncopeClientException invalidMapping =
//                SyncopeClientException.build(ClientExceptionType.InvalidMapping);
//        SyncopeClientException requiredValuesMissing =
//                SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
//
//        passwordManagementTO.getItems().forEach(itemTO -> {
//            if (itemTO == null) {
//                LOG.error("Null {}", Item.class.getSimpleName());
//                invalidMapping.getElements().add("Null " + Item.class.getSimpleName());
//            } else if (itemTO.getIntAttrName() == null) {
//                requiredValuesMissing.getElements().add("intAttrName");
//                scce.addException(requiredValuesMissing);
//            } else {
//                // no mandatory condition implies mandatory condition false
//                if (!JexlUtils.isExpressionValid(itemTO.getMandatoryCondition() == null
//                        ? "false" : itemTO.getMandatoryCondition())) {
//
//                    SyncopeClientException invalidMandatoryCondition =
//                            SyncopeClientException.build(ClientExceptionType.InvalidValues);
//                    invalidMandatoryCondition.getElements().add(itemTO.getMandatoryCondition());
//                    scce.addException(invalidMandatoryCondition);
//                }
//
//                Item item = new Item();
//                item.setIntAttrName(itemTO.getIntAttrName());
//                item.setExtAttrName(itemTO.getExtAttrName());
//                item.setMandatoryCondition(itemTO.getMandatoryCondition());
//                item.setConnObjectKey(itemTO.isConnObjectKey());
//                item.setPassword(itemTO.isPassword());
//                item.setPropagationJEXLTransformer(itemTO.getPropagationJEXLTransformer());
//                item.setPullJEXLTransformer(itemTO.getPullJEXLTransformer());
//                passwordManagement.getItems().add(item);
//            }
//        });
//
//        if (!invalidMapping.getElements().isEmpty()) {
//            scce.addException(invalidMapping);
//        }
//        if (scce.hasExceptions()) {
//            throw scce;
//        }
//    }

//    protected void populateItems(final PasswordManagement passwordManagement, final PasswordManagementTO passwordManagementTO) {
//        passwordManagement.getItems().forEach(item -> {
//            Item itemTO = new Item();
//            itemTO.setIntAttrName(item.getIntAttrName());
//            itemTO.setExtAttrName(item.getExtAttrName());
//            itemTO.setMandatoryCondition(item.getMandatoryCondition());
//            itemTO.setConnObjectKey(item.isConnObjectKey());
//            itemTO.setPassword(item.isPassword());
//            itemTO.setPropagationJEXLTransformer(item.getPropagationJEXLTransformer());
//            itemTO.setPullJEXLTransformer(item.getPullJEXLTransformer());
//            itemTO.setPurpose(MappingPurpose.NONE);
//
//            passwordManagementTO.getItems().add(itemTO);
//        });
//    }

    @Override
    public PasswordManagement create(final PasswordManagementTO passwordManagementTO) {
        PasswordManagement passwordManagement = entityFactory.newEntity(PasswordManagement.class);
        passwordManagement.setKey(passwordManagementTO.getKey());
        return update(passwordManagement, passwordManagementTO);
    }

    @Override
    public PasswordManagement update(final PasswordManagement passwordManagement, final PasswordManagementTO passwordManagementTO) {
        passwordManagement.setDescription(passwordManagementTO.getDescription());
        passwordManagement.setEnabled(passwordManagementTO.isEnabled());
        passwordManagement.setConf(passwordManagementTO.getConf());

        passwordManagement.getItems().clear();
//        populateItems(passwordManagementTO, passwordManagement);

        return passwordManagement;
    }

    @Override
    public PasswordManagementTO getPasswordManagementTO(final PasswordManagement passwordManagement) {
        PasswordManagementTO passwordManagementTO = new PasswordManagementTO();

        passwordManagementTO.setKey(passwordManagement.getKey());
        passwordManagementTO.setDescription(passwordManagement.getDescription());
        passwordManagementTO.setEnabled(passwordManagement.isEnabled());
        passwordManagementTO.setConf(passwordManagement.getConf());

//        populateItems(passwordManagement, passwordManagementTO);

        return passwordManagementTO;
    }
}
