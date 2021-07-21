/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.server.core.normalization;

// Remove this class as soon as upgrade to ApacheDS 2.0.0.AM27 is available

// CHECKSTYLE:OFF

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.LeafNode;
import org.apache.directory.api.ldap.model.filter.NotNode;
import org.apache.directory.api.ldap.model.filter.ObjectClassNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.filter.UndefinedNode;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.api.ldap.model.schema.normalizers.NameComponentNormalizer;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.normalization.FilterNormalizingVisitor;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A name normalization service.  This service makes sure all relative and distinguished
 * names are normalized before calls are made against the respective interface methods
 * on DefaultPartitionNexus.
 *
 * The Filters are also normalized.
 *
 * If the Rdn AttributeTypes are not present in the entry for an Add request,
 * they will be added.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NormalizationInterceptor extends BaseInterceptor
{
    /** logger used by this class */
    private static final Logger LOG = LoggerFactory.getLogger( NormalizationInterceptor.class );

    /** a filter node value normalizer and undefined node remover */
    private FilterNormalizingVisitor normVisitor;


    /**
     * Creates a new instance of a NormalizationInterceptor.
     */
    public NormalizationInterceptor()
    {
        super( InterceptorEnum.NORMALIZATION_INTERCEPTOR );
    }


    /**
     * Initialize the registries, normalizers.
     */
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        LOG.debug( "Initialiazing the NormalizationInterceptor" );

        super.init( directoryService );

        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        normVisitor = new FilterNormalizingVisitor( ncn, schemaManager );
    }


    /**
     * The destroy method does nothing
     */
    @Override
    public void destroy()
    {
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for ContextPartition interface operations
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        Dn addDn = addContext.getDn();
        
        if ( !addDn.isSchemaAware() )
        {
            addContext.setDn( new Dn( schemaManager, addDn ) );
        }
        
        Dn entryDn = addContext.getEntry().getDn();
        
        if ( !entryDn.isSchemaAware() )
        {
            addContext.getEntry().setDn( new Dn( schemaManager, entryDn ) );
        }
        
        addRdnAttributesToEntry( addContext.getDn(), addContext.getEntry() );
        
        next( addContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        Dn dn = compareContext.getDn();
        
        if ( !dn.isSchemaAware() )
        {
            compareContext.setDn( new Dn( schemaManager, dn ) );
        }

        // Get the attributeType from the OID
        try
        {
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( compareContext.getOid() );

            // Translate the value from binary to String if the AT is HR
            if ( attributeType.getSyntax().isHumanReadable() && ( !compareContext.getValue().isHumanReadable() ) )
            {
                compareContext.setValue( compareContext.getValue() );
            }

            compareContext.setAttributeType( attributeType );
        }
        catch ( LdapException le )
        {
            throw new LdapInvalidAttributeTypeException( I18n.err( I18n.ERR_266, compareContext.getOid() ) );
        }

        return next( compareContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        Dn dn = deleteContext.getDn();
        
        if ( !dn.isSchemaAware() )
        {
            deleteContext.setDn( new Dn( schemaManager, dn ) );
        }

        next( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        Dn dn = hasEntryContext.getDn();
        
        if ( !dn.isSchemaAware() )
        {
            hasEntryContext.setDn( new Dn( schemaManager, dn ) );
        }

        return next( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        Dn dn = lookupContext.getDn();
        
        if ( !dn.isSchemaAware() )
        {
            lookupContext.setDn( new Dn( schemaManager, dn ) );
        }

        return next( lookupContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        Dn dn = modifyContext.getDn();
        
        if ( !dn.isSchemaAware() )
        {
            modifyContext.setDn( new Dn( schemaManager, dn ) );
        }

        if ( modifyContext.getModItems() != null )
        {
            for ( Modification modification : modifyContext.getModItems() )
            {
                AttributeType attributeType = schemaManager.getAttributeType( modification.getAttribute().getId() );
                modification.apply( attributeType );
            }
        }

        next( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        Dn moveDn = moveContext.getDn();
        
        if ( !moveDn.isSchemaAware() )
        {
            moveContext.setDn( new Dn( schemaManager, moveDn ) );
        }

        Dn oldSuperiorDn = moveContext.getOldSuperior();
        
        if ( !oldSuperiorDn.isSchemaAware() )
        {
            moveContext.setOldSuperior( new Dn( schemaManager, oldSuperiorDn ) );
        }

        Dn newSuperiorDn = moveContext.getNewSuperior();
        
        if ( !newSuperiorDn.isSchemaAware() )
        {
            moveContext.setNewSuperior( new Dn( schemaManager, newSuperiorDn ) );
        }
        
        Dn newDn = moveContext.getNewDn();
        
        if ( !newDn.isSchemaAware() )
        {
            moveContext.setNewDn( new Dn( schemaManager, newDn ) );
        }

        Rdn rdn = moveContext.getRdn();
        
        if ( !rdn.isSchemaAware() )
        {
            moveContext.setRdn( new Rdn( schemaManager, rdn ) );
        }

        next( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Rdn newRdn = moveAndRenameContext.getNewRdn();
        
        if ( !newRdn.isSchemaAware() )
        {
            moveAndRenameContext.setNewRdn( new Rdn( schemaManager, newRdn ) );
        }
        
        Dn dn = moveAndRenameContext.getDn();
        
        if ( !dn.isSchemaAware() )
        {
            moveAndRenameContext.setDn( new Dn( schemaManager, dn ) );
        }
        
        Dn newDn = moveAndRenameContext.getNewDn();
        
        if ( !newDn.isSchemaAware() )
        {
            moveAndRenameContext.setNewDn( new Dn( schemaManager, newDn ) );
        }
        
        Dn newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();
        
        if ( !newSuperiorDn.isSchemaAware() )
        {
            moveAndRenameContext.setNewSuperiorDn( new Dn( schemaManager, newSuperiorDn ) );
        }

        next( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        // Normalize the new Rdn and the Dn if needed
        Dn dn = renameContext.getDn();
        
        if ( !dn.isSchemaAware() )
        {
            renameContext.setDn( new Dn( schemaManager, dn ) );
        }
        
        Rdn newRdn = renameContext.getNewRdn();
        
        if ( !newRdn.isSchemaAware() )
        {
            renameContext.setNewRdn( new Rdn( schemaManager, newRdn ) );
        }
        
        Dn newDn = renameContext.getNewDn();
        
        if ( !newDn.isSchemaAware() )
        {
            renameContext.setNewDn( new Dn( schemaManager, newDn ) );
        }

        // Push to the next interceptor
        next( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        Dn dn = searchContext.getDn();
        
        if ( !dn.isSchemaAware() )
        {
            searchContext.setDn( new Dn( schemaManager, dn ) );
        }

        ExprNode filter = searchContext.getFilter();

        if ( filter == null )
        {
            LOG.warn( "undefined filter based on undefined attributeType not evaluted at all.  Returning empty enumeration." );
            return new EntryFilteringCursorImpl(new EmptyCursor<>(), searchContext, schemaManager );
        }

        // Normalize the filter
        filter = ( ExprNode ) filter.accept( normVisitor );

        if ( filter == null )
        {
            LOG.warn( "undefined filter based on undefined attributeType not evaluted at all.  Returning empty enumeration." );
            return new EntryFilteringCursorImpl(new EmptyCursor<>(), searchContext, schemaManager );
        }

        // We now have to remove the (ObjectClass=*) filter if it's present, and to add the scope filter
        ExprNode modifiedFilter = removeObjectClass( filter );

        searchContext.setFilter( modifiedFilter );

        // TODO Normalize the returned Attributes, storing the UP attributes to format the returned values.
        return next( searchContext );
    }


    /**
     * Remove the (ObjectClass=*) node from an AndNode, if we have one.
     */
    private ExprNode handleAndNode( ExprNode node )
    {
        int nbNodes = 0;
        AndNode newAndNode = new AndNode();

        for ( ExprNode child : ( ( BranchNode ) node ).getChildren() )
        {
            ExprNode modifiedNode = removeObjectClass( child );

            if ( !( modifiedNode instanceof ObjectClassNode ) )
            {
                newAndNode.addNode( modifiedNode );
                nbNodes++;
            }

            if ( modifiedNode instanceof UndefinedNode )
            {
                // We can just return an Undefined node as nothing will get selected
                return UndefinedNode.UNDEFINED_NODE;
            }
        }

        switch ( nbNodes )
        {
            case 0:
                // Unlikely... But (&(ObjectClass=*)) or (|(ObjectClass=*)) are still an option
                return ObjectClassNode.OBJECT_CLASS_NODE;

            case 1:
                // We can safely remove the AND/OR node and replace it with its first child
                return newAndNode.getFirstChild();

            default:
                return newAndNode;
        }
    }


    /**
     * Remove the (ObjectClass=*) node from a NotNode, if we have one.
     */
    private ExprNode handleNotNode( ExprNode node )
    {
        NotNode newNotNode = new NotNode();

        for ( ExprNode child : ( ( BranchNode ) node ).getChildren() )
        {
            ExprNode modifiedNode = removeObjectClass( child );

            if ( modifiedNode instanceof ObjectClassNode )
            {
                // We don't want any entry which has an ObjectClass, return an undefined node
                return UndefinedNode.UNDEFINED_NODE;
            }

            if ( modifiedNode instanceof UndefinedNode )
            {
                // Here, we will select everything
                return ObjectClassNode.OBJECT_CLASS_NODE;
            }
            
            newNotNode.addNode( modifiedNode );

        }

        return newNotNode;
    }


    /**
     * Remove the (ObjectClass=*) node from an OrNode, if we have one.
     */
    private ExprNode handleOrNode( ExprNode node )
    {
        OrNode newOrNode = new OrNode();

        for ( ExprNode child : ( ( BranchNode ) node ).getChildren() )
        {
            ExprNode modifiedNode = removeObjectClass( child );

            if ( modifiedNode instanceof ObjectClassNode )
            {
                // We can return immediately with an ObjectClass node
                return ObjectClassNode.OBJECT_CLASS_NODE;
            }
            
            newOrNode.addNode( modifiedNode );
        }

        return newOrNode;
    }


    /**
     * Remove the (ObjectClass=*) and ( ObjectClass=top) nodes from the filter, if we have one.
     */
    private ExprNode removeObjectClass( ExprNode node )
    {
        if ( node instanceof LeafNode )
        {
            LeafNode leafNode = ( LeafNode ) node;

            if ( leafNode.getAttributeType() == directoryService.getAtProvider().getObjectClass() )
            {
                if ( leafNode instanceof PresenceNode )
                {
                    // We can safely remove the node and return an undefined node
                    return ObjectClassNode.OBJECT_CLASS_NODE;
                }
                else if ( leafNode instanceof EqualityNode )
                {
                    @SuppressWarnings("unchecked")
                    Value value = ( ( EqualityNode<String> ) leafNode ).getValue();

                    if ( value.equals( SchemaConstants.TOP_OC ) )
                    {
                        // Here too we can safely remove the node and return an undefined node
                        return ObjectClassNode.OBJECT_CLASS_NODE;
                    }
                }
            }
        }

        // --------------------------------------------------------------------
        //                 H A N D L E   B R A N C H   N O D E S
        // --------------------------------------------------------------------

        if ( node instanceof AndNode )
        {
            return handleAndNode( node );
        }
        else if ( node instanceof OrNode )
        {
            return handleOrNode( node );
        }
        else if ( node instanceof NotNode )
        {
            return handleNotNode( node );
        }
        else
        {
            // Failover : we return the initial node as is
            return node;
        }
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for other interface operations
    // ------------------------------------------------------------------------
    /**
     * Adds missing Rdn's attributes and values to the entry.
     *
     * @param dn the Dn
     * @param entry the entry
     */
    private void addRdnAttributesToEntry( Dn dn, Entry entry ) throws LdapException
    {
        if ( dn == null || entry == null )
        {
            return;
        }

        Rdn rdn = dn.getRdn();

        // Loop on all the AVAs
        for ( Ava ava : rdn )
        {
            Value value = ava.getValue();
            String upValue = ava.getValue().getString();
            String upId = ava.getType();

            // Check that the entry contains this Ava
            if ( !entry.contains( upId, value ) )
            {
                String message = "The Rdn '" + upId + "=" + upValue + "' is not present in the entry";
                LOG.warn( message );

                // We don't have this attribute : add it.
                // Two cases :
                // 1) The attribute does not exist
                if ( !entry.containsAttribute( upId ) )
                {
                    entry.add( upId, upValue );
                }
                // 2) The attribute exists
                else
                {
                    AttributeType at = schemaManager.lookupAttributeTypeRegistry( upId );

                    // 2.1 if the attribute is single valued, replace the value
                    if ( at.isSingleValued() )
                    {
                        entry.removeAttributes( upId );
                        entry.add( upId, upValue );
                    }
                    // 2.2 the attribute is multi-valued : add the missing value
                    else
                    {
                        entry.add( upId, upValue );
                    }
                }
            }
        }
    }
}
