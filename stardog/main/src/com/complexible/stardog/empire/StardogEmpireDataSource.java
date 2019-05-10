/*
 * Copyright (c) 2009-2015 Clark & Parsia, LLC. <http://www.clarkparsia.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.complexible.stardog.empire;

import java.net.ConnectException;
import java.util.HashSet;
import java.util.Set;

import com.clarkparsia.empire.ds.DataSourceException;
import com.clarkparsia.empire.ds.MutableDataSource;
import com.clarkparsia.empire.ds.QueryException;
import com.clarkparsia.empire.ds.ResultSet;
import com.clarkparsia.empire.ds.SupportsTransactions;
import com.clarkparsia.empire.ds.impl.AbstractDataSource;
import com.clarkparsia.empire.impl.RdfQueryFactory;
import com.clarkparsia.empire.impl.sparql.SPARQLDialect;
import com.complexible.common.openrdf.model.Models2;
import com.complexible.common.openrdf.util.AdunaIterations;
import org.openrdf.model.Model;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.TupleQueryResult;
import com.complexible.stardog.StardogException;
import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.ConnectionConfiguration;
import com.complexible.stardog.sesame.SesameToStark;
import com.complexible.stardog.sesame.StarkToSesame;
import com.stardog.stark.Statement;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;

/**
 * <p></p>
 *
 * @author  Michael Grove
 * @since   0.9.0
 * @version 0.9.0
 */
public class StardogEmpireDataSource extends AbstractDataSource implements MutableDataSource, SupportsTransactions {
	private Connection mConnection;
	private final ConnectionConfiguration mConfig;

	public StardogEmpireDataSource(final ConnectionConfiguration theConfiguration) {
		mConfig = theConfiguration;

		setQueryFactory(new RdfQueryFactory(this, SPARQLDialect.instance()));
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void connect() throws ConnectException {
		if (mConnection == null) {
			try {
				mConnection = mConfig.connect();
				setConnected(true);
			}
			catch (StardogException e) {
				throw new ConnectException(e.getMessage());
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void disconnect() {
		if (mConnection != null) {
			try {
				mConnection.close();
				setConnected(false);
			}
			catch (StardogException e) {
				// TODO: log me
				System.err.println(e.getMessage());
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public ResultSet selectQuery(final String theQuery) throws QueryException {
		assertConnected();

		try {
			// impedance mismatching stark <=> sesame
			final TupleQueryResult aResults = StarkToSesame.toResult(mConnection.select(theQuery).execute());
			return new ResultSet() {
				@Override
				public void close() {
					AdunaIterations.closeQuietly(aResults);
				}

				@Override
				public boolean hasNext() {
					try {
						return aResults.hasNext();
					}
					catch (QueryEvaluationException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public BindingSet next() {
					try {
						return aResults.next();
					}
					catch (QueryEvaluationException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		catch (StardogException e) {
			throw new QueryException(e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Model graphQuery(final String theQuery) throws QueryException {
		assertConnected();

		GraphQueryResult aResult = null;

		try {
			// impedance mismatching stark <=> sesame
			aResult = StarkToSesame.toResult(mConnection.graph(theQuery).execute());
			return Models2.newModel(aResult);
		}
		catch (QueryEvaluationException | StardogException e) {
			throw new QueryException(e);
		}
		finally {
			if (aResult != null) {
				try {
					aResult.close();
				}
				catch (QueryEvaluationException e) {
					// TODO: log me
					System.err.println("There was an error closing a query result: " + e.getMessage());
				}
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean ask(final String theQuery) throws QueryException {
		assertConnected();

		try {
			return mConnection.ask(theQuery).execute();
		}
		catch (StardogException e) {
			throw new QueryException(e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Model describe(final String theQuery) throws QueryException {
		return graphQuery(theQuery);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void add(final Model theGraph) throws DataSourceException {
		assertConnected();
		// impedance mismatching stark <=> sesame
		Set<Statement> statements = new HashSet<Statement>();
		for (org.openrdf.model.Statement st: theGraph) {
			statements.add(SesameToStark.toStatement(st));
		}
		try {
			mConnection.add().graph(statements);
		}
		catch (StardogException e) {
			throw new DataSourceException(e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void remove(final Model theGraph) throws DataSourceException {
		assertConnected();
		// impedance mismatching stark <=> sesame
		Set<Statement> statements = new HashSet<Statement>();
		for (org.openrdf.model.Statement st: theGraph) {
			statements.add(SesameToStark.toStatement(st));
		}
		try {
			mConnection.remove().graph(statements);
		}
		catch (StardogException e) {
			throw new DataSourceException(e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void begin() throws DataSourceException {
		assertConnected();

		try {
			mConnection.begin();
		}
		catch (StardogException e) {
			throw new DataSourceException(e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void commit() throws DataSourceException {
		assertConnected();

		try {
			mConnection.commit();
		}
		catch (StardogException e) {
			throw new DataSourceException(e);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void rollback() throws DataSourceException {
		assertConnected();

		try {
			mConnection.rollback();
		}
		catch (StardogException e) {
			throw new DataSourceException(e);
		}
	}
}
