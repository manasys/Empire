/*
 * Copyright (c) 2009-2010 Clark & Parsia, LLC. <http://www.clarkparsia.com>
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

package com.clarkparsia.empire.config;

import com.clarkparsia.empire.util.EmpireAnnotationProvider;
import com.clarkparsia.empire.util.PropertiesAnnotationProvider;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.name.Names;

import java.util.Map;
import java.util.HashMap;
import java.io.File;

/**
 * <p>A simple container class for EmpireConfiguration information.</p>
 *
 * @author Michael Grove
 * @since 0.6.2
 * @version 0.6.2
 */
public class EmpireConfiguration {
	private Class<? extends EmpireAnnotationProvider> mAnnotationProvider = PropertiesAnnotationProvider.class;

	private Map<String, String> mGeneralConfiguration = new HashMap<String, String>();

	private Map<String, Map<String, String>> mUnitConfiguration = new HashMap<String, Map<String, String>>();

//	public EmpireConfiguration(final Map<String, Map<String, String>> theConfiguration) {
//		mConfiguration = theConfiguration;
//	}
//
//	public EmpireConfiguration(final String theUnit, final Map<String, String> theConfig) {
//		mConfiguration.put(theUnit, theConfig);
//	}
//
//	public EmpireConfiguration() {
//	}
//
//	public Map<String, String> get(String theUnitName) {
//		return mConfiguration.get(theUnitName);
//	}

	public EmpireConfiguration() {
	}

	public EmpireConfiguration(final Map<String, String> theGeneralConfiguration,
							   final Map<String, Map<String, String>> theUnitConfiguration) {
		
		mGeneralConfiguration = theGeneralConfiguration;
		mUnitConfiguration = theUnitConfiguration;
	}

	public Class<? extends EmpireAnnotationProvider> getAnnotationProvider() {
		return mAnnotationProvider;
	}

	public void setAnnotationProvider(final Class<EmpireAnnotationProvider> theAnnotationProvider) {
		mAnnotationProvider = theAnnotationProvider;
	}

	public Map<String, String> getUnitConfig(final String theUnitName) {
		return mUnitConfiguration.get(theUnitName);
	}

	public String get(String theKey) {
		return mGeneralConfiguration.get(theKey);
	}

	public void installBindings(final Binder theBinder) {
		// TODO: is there a more general binding scheme that makes more sense?
		// TODO: or is this overkill and we should just not worry about context specific bindings?
		theBinder.bind(File.class)
				.annotatedWith(Names.named("annotation.index"))
				.toProvider(new Provider<File>() {
					public File get() {
						if (mGeneralConfiguration.containsKey(ConfigKeys.ANNOTATION_INDEX)) {
							return new File(EmpireConfiguration.this.get(ConfigKeys.ANNOTATION_INDEX));
						}
						else {
							return new File("empire.annotation.index");
						}
					}
				});
	}

	public boolean hasUnit(final String theUnitName) {
		return mUnitConfiguration.containsKey(theUnitName);
	}
}
