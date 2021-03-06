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

package org.teiid.query.processor;

import static org.junit.Assert.*;

import java.util.List;

import org.teiid.cache.DefaultCacheFactory;
import org.teiid.common.buffer.BufferManager;
import org.teiid.common.buffer.BufferManagerFactory;
import org.teiid.dqp.internal.process.CachedResults;
import org.teiid.dqp.internal.process.SessionAwareCache;
import org.teiid.dqp.service.TransactionContext;
import org.teiid.metadata.FunctionMethod.Determinism;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.metadata.TempMetadataAdapter;
import org.teiid.query.optimizer.capabilities.CapabilitiesFinder;
import org.teiid.query.optimizer.capabilities.DefaultCapabilitiesFinder;
import org.teiid.query.tempdata.TempTableDataManager;
import org.teiid.query.tempdata.TempTableStore;
import org.teiid.query.tempdata.TempTableStore.TransactionMode;
import org.teiid.query.util.CommandContext;

@SuppressWarnings("nls")
public class TempTableTestHarness {
	
	protected TempMetadataAdapter metadata;
	protected TempTableDataManager dataManager;
	protected TempTableStore tempStore;
	
	protected TransactionContext tc;

	public ProcessorPlan execute(String sql, List<?>[] expectedResults, CapabilitiesFinder finder) throws Exception {
		CommandContext cc = TestProcessor.createCommandContext();
		ProcessorPlan plan = TestProcessor.helpGetPlan(TestProcessor.helpParse(sql), metadata, finder, cc);
		cc.setTransactionContext(tc);
		cc.setMetadata(metadata);
		cc.setTempTableStore(tempStore);
		TestProcessor.doProcess(plan, dataManager, expectedResults, cc);
		assertTrue(Determinism.SESSION_DETERMINISTIC.compareTo(cc.getDeterminismLevel()) <= 0);
		return plan;
	}
	
	public ProcessorPlan execute(String sql, List<?>[] expectedResults) throws Exception {
		return execute(sql, expectedResults, DefaultCapabilitiesFinder.INSTANCE);
	}
	
	public void setUp(QueryMetadataInterface qmi, ProcessorDataManager dm) {
		tempStore = new TempTableStore("1", TransactionMode.ISOLATE_WRITES); //$NON-NLS-1$
		metadata = new TempMetadataAdapter(qmi, tempStore.getMetadataStore());
		metadata.setSession(true);
	    BufferManager bm = BufferManagerFactory.getStandaloneBufferManager();
	    
	    SessionAwareCache<CachedResults> cache = new SessionAwareCache<CachedResults>("resultset", DefaultCacheFactory.INSTANCE, SessionAwareCache.Type.RESULTSET, 0);
	    cache.setTupleBufferCache(bm);
		dataManager = new TempTableDataManager(dm, bm, cache);
	}

}
