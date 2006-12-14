/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006 The Sakai Foundation.
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.assignment.taggable.api;

import java.util.List;

/**
 * A specialized list that can give column names for the data that the objects
 * in the list can provide.
 * 
 * @author The Sakai Foundation.
 * @see Tag
 */
public interface TagList extends List<Tag> {

	/**
	 * Method to get a list of the column names that the objects in this tag
	 * list can provide data for.
	 * 
	 * @return A list of column names that the objects in this tag list can
	 *         provide data for.
	 * @see Tag#getFieldData()
	 */
	public List<String> getColumns();
}
