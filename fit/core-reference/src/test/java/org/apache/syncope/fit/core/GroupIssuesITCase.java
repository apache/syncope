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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class GroupIssuesITCase extends AbstractITCase {

    @Test
    public void issue178() {
        GroupTO groupTO = new GroupTO();
        String groupName = "torename" + getUUIDString();
        groupTO.setName(groupName);
        groupTO.setRealm("/");

        GroupTO actual = createGroup(groupTO).getEntity();

        assertNotNull(actual);
        assertEquals(groupName, actual.getName());

        GroupPatch groupPatch = new GroupPatch();
        groupPatch.setKey(actual.getKey());
        String renamedGroup = "renamed" + getUUIDString();
        groupPatch.setName(new StringReplacePatchItem.Builder().value(renamedGroup).build());

        actual = updateGroup(groupPatch).getEntity();
        assertNotNull(actual);
        assertEquals(renamedGroup, actual.getName());
    }

    @Test
    public void issueSYNCOPE632() {
        DerSchemaTO orig = schemaService.read(SchemaType.DERIVED, "displayProperty");
        DerSchemaTO modified = SerializationUtils.clone(orig);
        modified.setExpression("icon + '_' + show");

        GroupTO groupTO = GroupITCase.getSampleTO("lastGroup");
        try {
            schemaService.update(SchemaType.DERIVED, modified);

            // 0. create group
            groupTO.getPlainAttrs().add(attrTO("icon", "anIcon"));
            groupTO.getPlainAttrs().add(attrTO("show", "true"));
            groupTO.getDerAttrs().add(attrTO("displayProperty", null));
            groupTO.getResources().clear();

            groupTO = createGroup(groupTO).getEntity();
            assertNotNull(groupTO);

            // 1. create new LDAP resource having ConnObjectKey mapped to a derived attribute
            ResourceTO newLDAP = resourceService.read(RESOURCE_NAME_LDAP);
            newLDAP.setKey("new-ldap");
            newLDAP.setPropagationPriority(0);

            for (ProvisionTO provision : newLDAP.getProvisions()) {
                provision.getVirSchemas().clear();
            }

            MappingTO mapping = newLDAP.getProvision(AnyTypeKind.GROUP.name()).getMapping();

            MappingItemTO connObjectKey = mapping.getConnObjectKeyItem();
            connObjectKey.setIntAttrName("displayProperty");
            connObjectKey.setPurpose(MappingPurpose.PROPAGATION);
            mapping.setConnObjectKeyItem(connObjectKey);
            mapping.setConnObjectLink("'cn=' + displayProperty + ',ou=groups,o=isp'");

            MappingItemTO description = new MappingItemTO();
            description.setIntAttrName("key");
            description.setExtAttrName("description");
            description.setPurpose(MappingPurpose.PROPAGATION);
            mapping.add(description);

            newLDAP = createResource(newLDAP);
            assertNotNull(newLDAP);

            // 2. update group and give the resource created above
            GroupPatch patch = new GroupPatch();
            patch.setKey(groupTO.getKey());
            patch.getResources().add(new StringPatchItem.Builder().
                    operation(PatchOperation.ADD_REPLACE).
                    value("new-ldap").build());

            groupTO = updateGroup(patch).getEntity();
            assertNotNull(groupTO);

            // 3. update the group
            GroupPatch groupPatch = new GroupPatch();
            groupPatch.setKey(groupTO.getKey());
            groupPatch.getPlainAttrs().add(attrAddReplacePatch("icon", "anotherIcon"));

            groupTO = updateGroup(groupPatch).getEntity();
            assertNotNull(groupTO);

            // 4. check that a single group exists in LDAP for the group created and updated above
            int entries = 0;
            DirContext ctx = null;
            try {
                ctx = getLdapResourceDirContext(null, null);

                SearchControls ctls = new SearchControls();
                ctls.setReturningAttributes(new String[] { "*", "+" });
                ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

                NamingEnumeration<SearchResult> result =
                        ctx.search("ou=groups,o=isp", "(description=" + groupTO.getKey() + ")", ctls);
                while (result.hasMore()) {
                    result.next();
                    entries++;
                }
            } catch (Exception e) {
                // ignore
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (NamingException e) {
                        // ignore
                    }
                }
            }

            assertEquals(1, entries);
        } finally {
            schemaService.update(SchemaType.DERIVED, orig);
            if (groupTO.getKey() != null) {
                groupService.delete(groupTO.getKey());
            }
            resourceService.delete("new-ldap");
        }
    }

    @Test
    public void issueSYNCOPE717() {
        String doubleSchemaName = "double" + getUUIDString();

        // 1. create double schema without conversion pattern
        PlainSchemaTO schema = new PlainSchemaTO();
        schema.setKey(doubleSchemaName);
        schema.setType(AttrSchemaType.Double);

        schema = createSchema(SchemaType.PLAIN, schema);
        assertNotNull(schema);
        assertNull(schema.getConversionPattern());

        AnyTypeClassTO minimalGroup = anyTypeClassService.read("minimal group");
        assertNotNull(minimalGroup);
        minimalGroup.getPlainSchemas().add(doubleSchemaName);
        anyTypeClassService.update(minimalGroup);

        // 2. create group, provide valid input value
        GroupTO groupTO = GroupITCase.getBasicSampleTO("syncope717");
        groupTO.getPlainAttrs().add(attrTO(doubleSchemaName, "11.23"));

        groupTO = createGroup(groupTO).getEntity();
        assertNotNull(groupTO);
        assertEquals("11.23", groupTO.getPlainAttr(doubleSchemaName).getValues().get(0));

        // 3. update schema, set conversion pattern
        schema = schemaService.read(SchemaType.PLAIN, schema.getKey());
        schema.setConversionPattern("0.000");
        schemaService.update(SchemaType.PLAIN, schema);

        // 4. re-read group, verify that pattern was applied
        groupTO = groupService.read(groupTO.getKey());
        assertNotNull(groupTO);
        assertEquals("11.230", groupTO.getPlainAttr(doubleSchemaName).getValues().get(0));

        // 5. modify group with new double value
        GroupPatch patch = new GroupPatch();
        patch.setKey(groupTO.getKey());
        patch.getPlainAttrs().add(new AttrPatch.Builder().attrTO(attrTO(doubleSchemaName, "11.257")).build());

        groupTO = updateGroup(patch).getEntity();
        assertNotNull(groupTO);
        assertEquals("11.257", groupTO.getPlainAttr(doubleSchemaName).getValues().get(0));

        // 6. update schema, unset conversion pattern
        schema.setConversionPattern(null);
        schemaService.update(SchemaType.PLAIN, schema);

        // 7. modify group with new double value, verify that no pattern is applied
        patch = new GroupPatch();
        patch.setKey(groupTO.getKey());
        patch.getPlainAttrs().add(new AttrPatch.Builder().attrTO(attrTO(doubleSchemaName, "11.23")).build());

        groupTO = updateGroup(patch).getEntity();
        assertNotNull(groupTO);
        assertEquals("11.23", groupTO.getPlainAttr(doubleSchemaName).getValues().get(0));
    }

}
