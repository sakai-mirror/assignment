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
package org.sakaiproject.assignment.model;

public class AssignmentGroup {
	private Long id;
	
	/**
	 * get the AssignmentGroup id
	 * @return
	 */
	public Long getId() {
		return id;
	}
	
	/**
	 * set the AssignmentGroup id
	 * @param id
	 */
	public void setId(Long id) {
		this.id = id;
	}
	
	private Assignment assignment;
	
	/**
	 * get the associated Assignment object
	 * @return
	 */
	public Assignment getAssignment() {
		return assignment;
	}
	
	/**
	 * set the associated Assignment object
	 * @param assignment
	 */
	public void setAssignment(Assignment assignment) {
		this.assignment = assignment;
	}
	
	private String groupId;
	
	/**
	 * get group id
	 * @return
	 */
	public String getGroupId() {
		return groupId;
	}
	
	/**
	 * set group id
	 * @param groupId
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	/** the Hibernate version number */
    private int hibernateVersion;

	public int getHibernateVersion() {
		return hibernateVersion;
	}

	public void setHibernateVersion(int hibernateVersion) {
		this.hibernateVersion = hibernateVersion;
	}

	public AssignmentGroup(Long id, Assignment assignment, String groupId,
			int hibernateVersion) {
		super();
		this.id = id;
		this.assignment = assignment;
		this.groupId = groupId;
	}
	
	/**
	 * the default constructor
	 */
	public AssignmentGroup(){
		
	}

}
