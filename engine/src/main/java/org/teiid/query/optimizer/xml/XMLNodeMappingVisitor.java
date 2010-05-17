/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.query.optimizer.xml;

import java.util.Collection;

import org.teiid.api.exception.query.QueryPlannerException;
import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidRuntimeException;
import org.teiid.query.execution.QueryExecPlugin;
import org.teiid.query.mapping.xml.MappingDocument;
import org.teiid.query.mapping.xml.MappingNode;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.sql.LanguageObject;
import org.teiid.query.sql.lang.Criteria;
import org.teiid.query.sql.navigator.PreOrderNavigator;
import org.teiid.query.sql.symbol.ElementSymbol;
import org.teiid.query.sql.symbol.Symbol;
import org.teiid.query.sql.visitor.AbstractSymbolMappingVisitor;
import org.teiid.query.util.ErrorMessageKeys;


/**
 * This visitor is able to map symbols based on the XML document model:
 * given a symbol representing one of the nodes of the document which is
 * mapped, this object will find the Symbol representing the relational
 * element it is mapped to.
 */
public class XMLNodeMappingVisitor extends AbstractSymbolMappingVisitor {

	private MappingNode rootNode;
    private QueryMetadataInterface metadata;

    /**
     * Constructor for XMLNodeMappingVisitor.
     */
    public XMLNodeMappingVisitor(MappingDocument rootNode, QueryMetadataInterface metadata) {
        this.rootNode = rootNode;
        this.metadata = metadata;
    }
    
    /**
     * @see AbstractSymbolMappingVisitor#getMappedSymbol(Symbol)
     */
    protected Symbol getMappedSymbol(Symbol symbol) {
    	if(!(symbol instanceof ElementSymbol)) {
    		return null;
    	}

		// Lookup full path to mapping node from symbol
		ElementSymbol element = (ElementSymbol) symbol;
        
        try {
            String path = metadata.getFullName(element.getMetadataID()).toUpperCase();
		
    		// Find mapping node for specified path
    		MappingNode elementNode = MappingNode.findNode(rootNode, path); 
    		if(elementNode == null) { 
    			return null;
    		}
    		
    		// Construct a new element node based on mapping node reference
    		String symbolName = elementNode.getNameInSource();
    		if (symbolName == null){
    			return null;
    		}
			return elementNode.getSourceNode().getMappedSymbol(new ElementSymbol(symbolName));
        } catch (TeiidComponentException err) {
            throw new TeiidRuntimeException(err);
        } 
    }
    
    /**
     * Convert the criteria from names using document identifiers to names using
     * result set (i.e. mapping class) identifiers.
     * @param simpleCrit Criteria to convert
     * @param rootNode Root of mapping node tree
     * @return Criteria Converted criteria
     * @throws QueryPlannerException if simpleCrit has a XML document model element
     * that is not mapped to data in a mapping class
     */
    public static Criteria convertCriteria(Criteria simpleCrit, MappingDocument rootNode, QueryMetadataInterface metadata)
    throws QueryPlannerException, TeiidComponentException{
        return (Criteria)convertObject(simpleCrit, rootNode, metadata);
    }

    public static LanguageObject convertObject(LanguageObject object, MappingDocument rootNode, QueryMetadataInterface metadata)
    throws QueryPlannerException, TeiidComponentException{
        LanguageObject copy = (LanguageObject)object.clone();

        //Don't want to do deep visiting
        XMLNodeMappingVisitor mappingVisitor = new XMLNodeMappingVisitor(rootNode, metadata);
        try {
            PreOrderNavigator.doVisit(copy, mappingVisitor);
        } catch (TeiidRuntimeException e) {
            Throwable child = e.getChild();
            
            if (child instanceof TeiidComponentException) {
                throw (TeiidComponentException)child;
            }
            
            throw new TeiidComponentException(child);
        }

        Collection unmappedSymbols = mappingVisitor.getUnmappedSymbols();
        if (unmappedSymbols != null && unmappedSymbols.size() > 0){
            throw new QueryPlannerException(ErrorMessageKeys.OPTIMIZER_0046, QueryExecPlugin.Util.getString(ErrorMessageKeys.OPTIMIZER_0046, new Object[] {unmappedSymbols, object}));
        }

        return copy;
    }
        
}