/**   
 * License Agreement for QWAZR
 *
 * Copyright (C) 2014-2015 OpenSearchServer Inc.
 * 
 * http://www.qwazr.com
 * 
 * This file is part of QWAZR.
 *
 * QWAZR is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * QWAZR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QWAZR. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.qwazr.connectors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ConnectorContextAbstract implements ConnectorContext {

	private final Map<String, AbstractConnector> connectors;

	protected ConnectorContextAbstract() {
		connectors = new ConcurrentHashMap<String, AbstractConnector>();
	}

	void add(AbstractConnector connector) {
		connectors.put(connector.name, connector);
	}

	protected void unload() {
		if (connectors == null)
			return;
		for (AbstractConnector connector : connectors.values()) {
			try {
				connector.unload(this);
			} catch (Exception e) {
				// This should never happen
				System.err.println(e);
			}
		}
		// Paranoid free
		connectors.clear();
	}

	@Override
	public ConnectorMap getReadOnlyMap() {
		return new ConnectorMap();
	}

	public class ConnectorMap {

		public AbstractConnector get(String name) {
			return connectors.get(name);
		}
	}
}
