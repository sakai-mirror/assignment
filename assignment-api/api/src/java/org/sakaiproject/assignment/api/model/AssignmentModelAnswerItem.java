/**********************************************************************************
 * $URL:  $
 * $Id:  $
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
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
package org.sakaiproject.assignment.api.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * To provide sample answers to student
 * @author zqian
 *
 */
public class AssignmentModelAnswerItem extends AssignmentSupplementItem {

	public AssignmentModelAnswerItem() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	private int showToStudent;

	public int getShowToStudent() {
		return showToStudent;
	}

	public void setShowToStudent(int showToStudent) {
		this.showToStudent = showToStudent;
	}
	
	
	
	
}
