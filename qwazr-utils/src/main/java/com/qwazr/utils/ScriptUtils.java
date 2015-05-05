/**
 * Copyright 2014-2015 OpenSearchServer Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.utils;

import java.io.Reader;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class ScriptUtils {

	public static class RestrictedAccessControlContext {
		public static final AccessControlContext INSTANCE;

		static {
			INSTANCE = new AccessControlContext(
					new ProtectionDomain[] { new ProtectionDomain(null, null) });
		}
	}

	public static void evalScript(final ScriptEngine scriptEngine,
			final AccessControlContext controlContext, final Reader reader,
			final Bindings bindings) throws ScriptException,
			PrivilegedActionException {
		AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
			@Override
			public Object run() throws ScriptException {
				scriptEngine.eval(reader, bindings);
				return null;
			}
		}, controlContext);

	}
}
