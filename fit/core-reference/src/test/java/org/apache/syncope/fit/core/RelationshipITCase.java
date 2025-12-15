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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.RelationshipUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class RelationshipITCase extends AbstractITCase {

    @Test
    public void unlimitedRelationships() {
        AnyObjectCR anyObjectCR = AnyObjectITCase.getSample("unlimited1");
        anyObjectCR.setRealm("/even/two");
        anyObjectCR.getResources().clear();
        AnyObjectTO left = createAnyObject(anyObjectCR).getEntity();

        anyObjectCR = AnyObjectITCase.getSample("unlimited2");
        anyObjectCR.setRealm(SyncopeConstants.ROOT_REALM);
        anyObjectCR.getResources().clear();
        anyObjectCR.getRelationships().add(new RelationshipTO.Builder("inclusion").
                otherEnd(left.getType(), left.getKey()).build());
        AnyObjectTO right = createAnyObject(anyObjectCR).getEntity();

        assertEquals(1, right.getRelationships().size());
        assertEquals(left.getKey(), right.getRelationships().getFirst().getOtherEndKey());

        AnyObjectUR anyObjectUR = new AnyObjectUR.Builder(left.getKey()).
                relationship(new RelationshipUR.Builder("inclusion").
                        otherEnd(right.getType(), right.getKey()).build()).build();
        left = updateAnyObject(anyObjectUR).getEntity();
        assertEquals(2, left.getRelationships().size());
        assertTrue(left.getRelationships().stream().anyMatch(r -> right.getKey().equals(r.getOtherEndKey())));
    }

    @Test
    public void relationshipWithAttr() {
        // first add type extension to neighborhood
        RelationshipTypeTO relTypeTO = RELATIONSHIP_TYPE_SERVICE.read("neighborhood");

        if (relTypeTO.getTypeExtension(AnyTypeKind.USER.name()).isEmpty()) {
            TypeExtensionTO typeExt = new TypeExtensionTO();
            typeExt.setAnyType(AnyTypeKind.USER.name());
            typeExt.getAuxClasses().add("other");
            relTypeTO.getTypeExtensions().add(typeExt);

            RELATIONSHIP_TYPE_SERVICE.update(relTypeTO);
            relTypeTO = RELATIONSHIP_TYPE_SERVICE.read("neighborhood");
        }
        assertEquals("other", relTypeTO.getTypeExtension(AnyTypeKind.USER.name()).get().getAuxClasses().getFirst());

        // then add relationship with attribute
        UserCR userCR = UserITCase.getUniqueSample("relationshipWithAttr@syncope.apache.org");
        userCR.getRelationships().add(new RelationshipTO.Builder("neighborhood").
                otherEnd("PRINTER", "8559d14d-58c2-46eb-a2d4-a7d35161e8f8").
                plainAttr(new Attr.Builder("obscure").value("testvalue3").build()).
                build());

        UserTO user = createUser(userCR).getEntity();

        RelationshipTO rel = user.getRelationship("neighborhood", "8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElseThrow();
        assertEquals(1, rel.getPlainAttrs().size());
        assertEquals(1, rel.getPlainAttr("obscure").orElseThrow().getValues().size());
        assertEquals(1, rel.getDerAttrs().size());
        assertEquals(1, rel.getDerAttr("noschema").orElseThrow().getValues().size());
    }

    @Test
    public void issueSYNCOPE1686() {
        // Create printers
        AnyObjectCR printer1CR = AnyObjectITCase.getSample("printer1");
        printer1CR.getResources().clear();
        String key1 = createAnyObject(printer1CR).getEntity().getKey();

        AnyObjectCR printer2CR = AnyObjectITCase.getSample("printer2");
        printer2CR.getResources().clear();
        String key2 = createAnyObject(printer2CR).getEntity().getKey();

        AnyObjectCR printer3CR = AnyObjectITCase.getSample("printer3");
        printer3CR.getResources().clear();
        String key3 = createAnyObject(printer3CR).getEntity().getKey();

        // Add relationships: printer1 -> printer2 and printer2 -> printer3
        AnyObjectUR relationship1To2 = new AnyObjectUR.Builder(key1)
                .relationship(new RelationshipUR.Builder("inclusion").otherEnd(PRINTER, key2).build())
                .build();
        AnyObjectUR relationship2To3 = new AnyObjectUR.Builder(key2)
                .relationship(new RelationshipUR.Builder("inclusion").otherEnd(PRINTER, key3).build())
                .build();

        updateAnyObject(relationship1To2);
        updateAnyObject(relationship2To3);

        // Read updated printers
        AnyObjectTO printer1 = ANY_OBJECT_SERVICE.read(key1);
        AnyObjectTO printer2 = ANY_OBJECT_SERVICE.read(key2);
        AnyObjectTO printer3 = ANY_OBJECT_SERVICE.read(key3);

        // Verify relationships for printer1
        assertEquals(1, printer1.getRelationships().size());
        RelationshipTO rel1 = printer1.getRelationships().getFirst();
        assertEquals(RelationshipTO.End.LEFT, rel1.getEnd());
        assertEquals(printer2.getKey(), rel1.getOtherEndKey());
        assertEquals(printer2.getType(), rel1.getOtherEndType());
        assertEquals(printer2.getName(), rel1.getOtherEndName());

        // Verify relationships for printer2
        assertEquals(2, printer2.getRelationships().size());
        assertTrue(printer2.getRelationships().stream()
                .anyMatch(r -> r.getEnd() == RelationshipTO.End.LEFT
                && printer3.getKey().equals(r.getOtherEndKey())
                && printer3.getType().equals(r.getOtherEndType())
                && printer3.getName().equals(r.getOtherEndName())));
        assertTrue(printer2.getRelationships().stream()
                .anyMatch(r -> r.getEnd() == RelationshipTO.End.RIGHT
                && printer1.getKey().equals(r.getOtherEndKey())
                && printer1.getType().equals(r.getOtherEndType())
                && printer1.getName().equals(r.getOtherEndName())));

        // Verify relationships for printer3
        assertEquals(1, printer3.getRelationships().size());
        RelationshipTO rel3 = printer3.getRelationships().getFirst();
        assertEquals(RelationshipTO.End.RIGHT, rel3.getEnd());
        assertEquals(printer2.getKey(), rel3.getOtherEndKey());
        assertEquals(printer2.getType(), rel3.getOtherEndType());
        assertEquals(printer2.getName(), rel3.getOtherEndName());

        // Test invalid relationship with End.RIGHT
        AnyObjectCR printer4CR = AnyObjectITCase.getSample("printer4");
        printer4CR.getResources().clear();
        printer4CR.getRelationships().add(
                new RelationshipTO.Builder("inclusion", RelationshipTO.End.RIGHT).otherEnd(PRINTER, key1).build());

        SyncopeClientException e = assertThrows(SyncopeClientException.class, () -> createAnyObject(printer4CR));
        assertEquals(ClientExceptionType.InvalidRelationship, e.getType());
        assertTrue(e.getMessage().contains("Relationships shall be created or updated only from their left end"));
    }
}
