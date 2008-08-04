/**********************************************************************************
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

import java.util.Set ;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentSubmission;

/**
 * This is the interface for accessing assignment supplement item
 * @author zqian
 *
 */
public interface AssignmentSupplementItemService {
	
	/*************** model answer ******************/
	
	/**
	 * new ModelAnswer object
	 * @return 
	 */
	public AssignmentModelAnswerItem newModelAnswer();
	
	/**
	 * Save the ModelAnswer object
	 * @param mItem
	 * @return
	 */
	public boolean saveModelAnswer(AssignmentModelAnswerItem mItem);
	
	/**
	 * Remove the ModelAnswer object
	 * @param mItem
	 * @return
	 */
	public boolean removeModelAnswer(AssignmentModelAnswerItem mItem);
	
	/**
	 * Get the ModelAnswer object
	 * @param assignmentId
	 * @return
	 */
	public AssignmentModelAnswerItem getModelAnswer(String assignmentId);
	
	/******************* private note *******************/
	
	/**
	 * new AssignmentNoteItem object
	 */
	public AssignmentNoteItem newNoteItem();
	
	/**
	 * Save the AssignmentNoteItem object
	 * @param nItem
	 * @return
	 */
	public boolean saveNoteItem(AssignmentNoteItem nItem);
	
	/**
	 * Remove the AssignmentNoteItem object
	 * @param nItem
	 * @return
	 */
	public boolean removeNoteItem(AssignmentNoteItem nItem);
	
	/**
	 * Get the AssignmentNoteItem object
	 * @param assignmentId
	 * @return
	 */
	public AssignmentNoteItem getNoteItem(String assignmentId);
	
	/******************* all purpose *******************/
	
	/**
	 * new AssignmentAllPurposeItem object
	 */
	public AssignmentAllPurposeItem newAllPurposeItem();
	
	/**
	 * Save the AssignmentAllPurposeItem object
	 * @param nItem
	 * @return
	 */
	public boolean saveAllPurposeItem(AssignmentAllPurposeItem nItem);
	
	/**
	 * Remove the AssignmentAllPurposeItem object
	 * @param nItem
	 * @return
	 */
	public boolean removeAllPurposeItem(AssignmentAllPurposeItem nItem);
	
	/**
	 * Get the AssignmentAllPurposeItem object
	 * @param assignmentId
	 * @return
	 */
	public AssignmentAllPurposeItem getAllPurposeItem(String assignmentId);
	
	/**
	 * Can the current user see the model answer or not
	 * @param a
	 * @param s
	 * @return
	 */
	public boolean canViewModelAnswer(Assignment a, AssignmentSubmission s);
	
	/**
	 * Can current user read the AssignmentNoteItem?
	 * @param a
	 * @return
	 */
	public boolean canReadNoteItem(Assignment a);
	
	/**
	 * Can the current user modify the AssignmentNoteItem?
	 * @param a
	 * @return
	 */
	public boolean canEditNoteItem(Assignment a);

}
