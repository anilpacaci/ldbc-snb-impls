/* 
 * Copyright (C) 2015-2016 Stanford University
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.ellitron.ldbcsnbimpls.interactive.neo4j.util;

import java.util.ArrayList;
import java.util.List;

import net.ellitron.ldbcsnbimpls.interactive.core.Entity;

/**
 * A collection of static methods used as helper methods in the implementation
 * of the LDBC SNB interactive workload for Neo4j.
 *
 * @author Jonathan Ellithorpe (jde@cs.stanford.edu)
 */
public class DbHelper {

	/**
	 * Take a list of Objects and serialize it to a JSON formatted array of
	 * strings.
	 *
	 * @param list
	 *            List of objects.
	 *
	 * @return Serialized JSON formatted string representing this list of
	 *         objects.
	 */
	public static String listToJsonArray(List<?> list) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				sb.append(", \"").append(list.get(i).toString()).append("\"");
			} else {
				sb.append("\"").append(list.get(i).toString()).append("\"");
			}
		}
		sb.append("]");

		return sb.toString();
	}

	/*
	 * Returns the original LDBC SNB assigned 64-bit ID of the given vertex
	 * (this is not the ID that is assigned to the vertex by TitanDB during the
	 * data loading phase).
	 */
	public static Long getSNBId(String v) {
		return Long.decode(v.split(":")[1]);
	}

	/*
	 * Return a String representing the globally unique Iid property on all
	 * vertices in the graph. This Iid property is a function of both the Entity
	 * type and the 64-bit LDBC SNB assigned ID to the node (which is only
	 * unique across vertices of that type).
	 */
	public static String makeIid(Entity type, long id) {
		return type.getName() + ":" + String.valueOf(id);
	}

	/*
	 * Return String representing the globally unique Iid property on all
	 * vertices in the graph. This Iid property is a function of both the Entity
	 * type and the 64-bit LDBC SNB assigned ID to the node (which is only
	 * unique across vertices of that type).
	 */
	public static List<String> makeIid(Entity type, List<Long> ids) {
		String name = type.getName();
		List<String> stringIds = new ArrayList<>(ids.size());

		ids.forEach(id -> {
			stringIds.add(name + ":" + String.valueOf(id));
		});

		return stringIds;
	}
}
