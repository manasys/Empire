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

import java.util.Arrays;
import java.util.Map;

import javax.persistence.NamedQuery;

import com.complexible.stardog.Stardog;
import com.complexible.stardog.api.admin.AdminConnection;
import com.complexible.stardog.api.ConnectionConfiguration;
import com.complexible.stardog.api.admin.AdminConnectionConfiguration;
import com.google.common.base.Throwables;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.EntityManagerTestSuite;
import com.clarkparsia.empire.api.nasa.MissionRole;
import com.clarkparsia.empire.api.nasa.Spacecraft;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.ds.DataSourceException;
import com.clarkparsia.empire.ds.DataSourceFactory;


/**
 * <p>Stardog based data source tests for backing an EntityManager in Empire</p>
 *
 * @author  Michael Grove
 * @version 0.9.0
 * @since   0.9.0
 */
public class StardogEntityManagerTestSuite extends EntityManagerTestSuite {
	private static Stardog aStardog = Stardog.builder().create();
	private final static String DB = "mem";
	@BeforeClass
	public static void beforeClass() {

		try {
			StardogEmpireAnnotationProvider.setAnnotatedClasses(NamedQuery.class, Arrays.<Class<?>>asList(MissionRole.class, Spacecraft.class));

			EmpireConfiguration config = new EmpireConfiguration();
			config.setAnnotationProvider(StardogEmpireAnnotationProvider.class);
			config.getGlobalConfig().put(StardogEmpireFactoryKeys.AUTO_COMMIT, "true");

			Empire.init(config);

			EntityManagerTestSuite.beforeClass();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@AfterClass
	public static void afterClass() {
		aStardog.shutdown();
	}
	
	@Before
	public void before() {
		try {
			try (AdminConnection aAdminConnection = AdminConnectionConfiguration.toEmbeddedServer()
			                                                                    .credentials("admin", "admin")
			                                                                    .connect()) {
				if (aAdminConnection.list().contains(DB)) {
					aAdminConnection.drop(DB);
				}
				aAdminConnection.newDatabase(DB).create();
			}
		}
		catch (Exception e) {
			Throwables.propagate(e);
		}
	}
	
	@Override
	protected DataSourceFactory createDataSourceFactory() {
		return new StardogEmpireDataSourceFactory() {
			@Override
			public StardogEmpireDataSource create(Map<String, Object> theMap) throws DataSourceException {
				ConnectionConfiguration connConf = ConnectionConfiguration.to(DB).credentials("admin", "admin");
				return new StardogEmpireDataSource(connConf);
			}
		};
	}
}
