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
 * The AssignmentSupplementItem is to store additional information for the assignment. Candidates include model answers, instructor notes, grading guidelines, etc.
 * @author zqian
 *
 */
public abstract class AssignmentAllPurposeItem {
	
	private Long id;
	private String assignmentId;
	private String title;
	private String instruction;
	private Date showStartDate;	// the start showing date
	private Date showEndDate; // the end showing date
	private boolean hide;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getAssignmentId() {
		return assignmentId;
	}
	public void setAssignmentId(String assignmentId) {
		this.assignmentId = assignmentId;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getInstruction() {
		return instruction;
	}
	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}
	public Date getShowStartDate() {
		return showStartDate;
	}
	public void setShowStartDate(Date showStartDate) {
		this.showStartDate = showStartDate;
	}
	public Date getShowEndDate() {
		return showEndDate;
	}
	public void setShowEndDate(Date showEndDate) {
		this.showEndDate = showEndDate;
	}
	public boolean isHide() {
		return hide;
	}
	public void setHide(boolean hide) {
		this.hide = hide;
	}
	
}
