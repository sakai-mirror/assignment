/**********************************************************************************
 * $URL$
 * $Id$
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

package org.sakaiproject.assignment.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.sakaiproject.entity.api.AttachmentContainerEdit;
import org.sakaiproject.entity.api.Edit;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.user.api.User;

/**
 * <p>
 * Assignment is an interface for the Sakai assignments module. It represents a specific assignment (as for a specific section or class).
 * </p>
 */
public interface Assignment extends Edit, Comparable, Serializable, AttachmentContainerEdit
{
	/** Grade type not set */
	public static final int GRADE_TYPE_NOT_SET = -1;

	/** Ungraded grade type */
	public static final int UNGRADED_GRADE_TYPE = 1;

	/** Letter grade type */
	public static final int LETTER_GRADE_TYPE = 2;

	/** Score based grade type */
	public static final int SCORE_GRADE_TYPE = 3;

	/** Pass/fail grade type */
	public static final int PASS_FAIL_GRADE_TYPE = 4;

	/** Grade type that only requires a check */
	public static final int CHECK_GRADE_TYPE = 5;

	/** Ungraded grade type string */
	public static final String UNGRADED_GRADE_TYPE_STRING = "Ungraded";

	/** Letter grade type string */
	public static final String LETTER_GRADE_TYPE_STRING = "Letter Grade";

	/** Score based grade type string */
	public static final String SCORE_GRADE_TYPE_STRING = "Points";

	/** Pass/fail grade type string */
	public static final String PASS_FAIL_GRADE_TYPE_STRING = "Pass/Fail";

	/** Grade type that only requires a check string */
	public static final String CHECK_GRADE_TYPE_STRING = "Checkmark";

	/** Assignment type not yet set */
	public static final int ASSIGNMENT_SUBMISSION_TYPE_NOT_SET = -1;

	/** Text only assignment type */
	public static final int TEXT_ONLY_ASSIGNMENT_SUBMISSION = 1;

	/** Attachment only assignment type */
	public static final int ATTACHMENT_ONLY_ASSIGNMENT_SUBMISSION = 2;

	/** Text and/or attachment assignment type */
	public static final int TEXT_AND_ATTACHMENT_ASSIGNMENT_SUBMISSION = 3;
	
	/** Non-electronic assignment type */
	public static final int NON_ELECTRONIC_ASSIGNMENT_SUBMISSION = 4;
	
	public static final int[] SUBMISSION_TYPES = {TEXT_ONLY_ASSIGNMENT_SUBMISSION,ATTACHMENT_ONLY_ASSIGNMENT_SUBMISSION,TEXT_AND_ATTACHMENT_ASSIGNMENT_SUBMISSION,NON_ELECTRONIC_ASSIGNMENT_SUBMISSION};

	/** Honor Pledge not yet set */
	public static final int HONOR_PLEDGE_NOT_SET = -1;

	/** Honor Pledge not yet set */
	public static final int HONOR_PLEDGE_NONE = 1;

	/** Honor Pledge not yet set */
	public static final int HONOR_PLEDGE_ENGINEERING = 2;
	
	// the option setting per assignment
	public static final String ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE= "assignment_instructor_notifications_value";
	
	// no email to instructor
	public static final String ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_NONE = "assignment_instructor_notifications_none";
	
	// send every email to instructor
	public static final String ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_EACH = "assignment_instructor_notifications_each";
	
	// send email in digest form
	public static final String ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DIGEST = "assignment_instructor_notifications_digest";

	/**
	 * Access the Assignment of this Assignment.
	 * 
	 * @return The Assignment's Assignment.
	 * @deprecated The function is deprecated after Sakai 2.6
	 */
	public AssignmentContent getContent();

	/**
	 * Access the reference of the Assignment of this Assignment.
	 * 
	 * @return The Assignment's reference.
	 * @deprecated The function is deprecated after Sakai 2.6
	 */
	public String getContentReference();

	/**
	 * Access the first time at which the assignment can be viewed; may be null.
	 * 
	 * @return The Time at which the assignment is due, or null if unspecified.
	 */
	public Time getOpenTime();

	/**
	 * Access the first time at which the assignment can be viewed; (String)
	 * 
	 * @return The Time at which the assignment is due, or "" if unspecified.
	 */
	public String getOpenTimeString();

	/**
	 * Access the time at which the assignment is due; may be null.
	 * 
	 * @return The Time at which the Assignment is due, or null if unspecified.
	 */
	public Time getDueTime();

	/**
	 * Access the time at which the assignment is due; (String)
	 * 
	 * @return The Time at which the Assignment is due,or "" if unspecified
	 */
	public String getDueTimeString();

	/**
	 * Access the drop dead time after which responses to this assignment are considered late; may be null.
	 * 
	 * @return The Time object representing the drop dead time, or null if unspecified.
	 */
	public Time getDropDeadTime();

	/**
	 * Access the drop dead time after which responses to this assignment are considered late; (String)
	 * 
	 * @return The Time object representing the drop dead time, or "" if unspecified.
	 */
	public String getDropDeadTimeString();

	/**
	 * Access the close time after which this assignment can no longer be viewed, and after which submissions will not be accepted. May be null.
	 * 
	 * @return The Time after which the Assignment is closed, or null if unspecified.
	 */
	public Time getCloseTime();

	/**
	 * Access the close time after which this assignment can no longer be viewed, and after which submissions will not be accepted. (String)
	 * 
	 * @return The Time after which the Assignment is closed, or "" if unspecified.
	 */
	public String getCloseTimeString();

	/**
	 * Access the section info.
	 * 
	 * @return The section id.
	 */
	public String getSection();

	/**
	 * Access the context at the time of creation.
	 * 
	 * @return String - the context string.
	 */
	public String getContext();

	/**
	 * Get whether this is a draft or final copy.
	 * 
	 * @return True if this is a draft, false if it is a final copy.
	 */
	public boolean getDraft();

	/**
	 * Access the creator of this object.
	 * 
	 * @return String - The id of the creator.
	 */
	public String getCreator();

	/**
	 * Access the time that this object was created.
	 * 
	 * @return The Time object representing the time of creation.
	 */
	public Time getTimeCreated();

	/**
	 * Access the list of authors.
	 * 
	 * @return List of authors as User objects.
	 */
	public List getAuthors();

	/**
	 * Access the time of last modificaiton.
	 * 
	 * @return The Time of last modification.
	 */
	public Time getTimeLastModified();

	/**
	 * Access the author of last modification
	 * 
	 * @return String - The id of the author.
	 */
	public String getAuthorLastModified();

	/**
	 * Access the title.
	 * 
	 * @return The Assignment's title.
	 */
	public String getTitle();
	
	/**
	 * Return string representation of assignment status
	 * 
	 * @return The Assignment's status
	 */
	public String getStatus();
	
	/**
	 * Access the position order field for the assignment.
     *
     * @return The Assignment's order.
     */
    public int getPosition_order();

	/**
	 * 
	 * Access the groups defined for this assignment.
	 * 
	 * @return A Collection (String) of group refs (authorization group ids) defined for this message; empty if none are defined.
	 */
	Collection getGroups();

	/**
	 * Access the access mode for the assignment - how we compute who has access to the assignment.
	 * 
	 * @return The AssignmentAccess access mode for the Assignment.
	 */
	AssignmentAccess getAccess();

	/**
	 * <p>
	 * AssignmentAccess enumerates different access modes for the assignment: site-wide or grouped.
	 * </p>
	 */
	public class AssignmentAccess
	{
		private final String m_id;

		private AssignmentAccess(String id)
		{
			m_id = id;
		}

		public String toString()
		{
			return m_id;
		}

		static public AssignmentAccess fromString(String access)
		{
			if (SITE.m_id.equals(access)) return SITE;
			if (GROUPED.m_id.equals(access)) return GROUPED;
			return null;
		}

		/** channel (site) level access to the message */
		public static final AssignmentAccess SITE = new AssignmentAccess("site");

		/** grouped access; only members of the getGroup() groups (authorization groups) have access */
		public static final AssignmentAccess GROUPED = new AssignmentAccess("grouped");
	}
	
	/**
	 * Set the reference of the Assignment of this Assignment.
	 * 
	 * @param String -
	 *        the reference of the Assignment.
	 */
	public void setContentReference(String contentReference);

	/**
	 * Set the Assignment of this Assignment.
	 * 
	 * @param content -
	 *        the Assignment's Assignment.
	 */
	public void setContent(AssignmentContent content);

	/**
	 * Set the first time at which the assignment can be viewed; may be null.
	 * 
	 * @param openTime -
	 *        The Time at which the Assignment opens.
	 */
	public void setOpenTime(Time openTime);

	/**
	 * Set the time at which the assignment is due; may be null.
	 * 
	 * @param dueTime -
	 *        The Time at which the Assignment is due.
	 */
	public void setDueTime(Time dueTime);

	/**
	 * Set the drop dead time after which responses to this assignment are considered late; may be null.
	 * 
	 * @param dropDeadTime -
	 *        The Time object representing the drop dead time.
	 */
	public void setDropDeadTime(Time dropDeadTime);

	/**
	 * Set the time after which this assignment can no longer be viewed, and after which submissions will not be accepted. May be null.
	 * 
	 * @param closeTime -
	 *        The Time after which the Assignment is closed, or null if unspecified.
	 */
	public void setCloseTime(Time closeTime);

	/**
	 * Set the section info
	 * 
	 * @param sectionId -
	 *        The section id.
	 */
	public void setSection(String sectionId);

	/**
	 * Set the Assignment's context at the time of creation.
	 * 
	 * @param context -
	 *        The context string.
	 */
	public void setContext(String context);

	/**
	 * Set whether this is a draft or final copy.
	 * 
	 * @param draft -
	 *        true if this is a draft, false if it is a final copy.
	 */
	public void setDraft(boolean draft);

	/**
	 * Add an author to the author list.
	 * 
	 * @param author -
	 *        The User to add to the author list.
	 */
	public void addAuthor(User author);

	/**
	 * Remove an author from the author list.
	 * 
	 * @param author -
	 *        the User to remove from the author list.
	 */
	public void removeAuthor(User author);

	/**
	 * Set the title.
	 * 
	 * @param title -
	 *        The Assignment's title.
	 */
	public void setTitle(String title);

	/**
	 * Set these as the message's groups, replacing the access and groups already defined.
	 * 
	 * @param Collection
	 *        groups The colelction of Group objects to use for this message.
	 * @throws PermissionException
	 *         if the end user does not have permission to remove from the groups that would be removed or add to the groups that would be added.
	 */
	void setGroupAccess(Collection groups) throws PermissionException;

	/**
	 * Remove any grouping for this message; the access mode reverts to channel and any groups are removed.
	 * 
	 * @throws PermissionException
	 *         if the end user does not have permission to do this.
	 */
	void clearGroupAccess() throws PermissionException;

	/**
	 * Set the access mode for the assignment - how we compute who has access to the assignment.
	 * 
	 * @param access
	 *        The AssignmentAccess access mode for the message.
	 */
	void setAccess(AssignmentAccess access);
	
    /**
	 * Set the position order field for the assignment.
	 *
	 * @param position_order -
	 *        The Assignment's order.
	 */
	public void setPosition_order(int position_order);
	
	/****************** from previous Assignment interface *********************/

	/**
	 * Access the instructions for the assignment
	 * 
	 * @return The Assignment's instructions.
	 */
	public String getInstructions();

	/**
	 * Access the type of submission.
	 * 
	 * @return An integer representing the type of submission.
	 */
	public int getTypeOfSubmission();

	/**
	 * Access the grade type
	 * 
	 * @return The integer representing the type of grade.
	 */
	public int getTypeOfGrade();

	/**
	 * Access a string describing the type of grade.
	 * 
	 * @param gradeType -
	 *        The integer representing the type of grade.
	 * @return Description of the type of grade.
	 */
	public String getTypeOfGradeString(int gradeType);

	/**
	 * Gets the maximum grade if grade type is SCORE_GRADE_TYPE(3)
	 * 
	 * @return int The maximum grade score, or zero if the grade type is not SCORE_GRADE_TYPE(3).
	 */
	public int getMaxGradePoint();

	/**
	 * Get the maximum grade for grade type = SCORE_GRADE_TYPE(3) Formated to show one decimal place
	 * 
	 * @return The maximum grade score.
	 */
	public String getMaxGradePointDisplay();

	/**
	 * Get whether this project can be a group project.
	 * 
	 * @return True if this can be a group project, false otherwise.
	 */
	public boolean getGroupProject();

	/**
	 * Access whether group projects should be individually graded.
	 * 
	 * @return true if projects are individually graded, false if grades are given to the group.
	 */
	public boolean individuallyGraded();

	/**
	 * Access whether grades can be released once submissions are graded.
	 * 
	 * @return True if grades can be released once submission are graded, false if they must be released manually.
	 */
	public boolean releaseGrades();

	/**
	 * Access the Honor Pledge type; values are NONE and ENGINEERING_HONOR_PLEDGE.
	 * 
	 * @return The type of pledge.
	 */
	public int getHonorPledge();

	/**
	 * Access whether this Assignment allows attachments.
	 * 
	 * @return true if the Assignment allows attachments, false otherwise.
	 */
	public boolean getAllowAttachments();
	
	
	
	/**
	 * Access whether this Assignment allows review service.
	 * 
	 * @return true if the Assignment allows review service, false otherwise.
	 */
	public boolean getAllowReviewService();

	/**
	 * Access whether this Assignment allows students to view review service reports.
	 * 
	 * @return true if the Assignment allows students to view review service reports, false otherwise.
	 */
	
	public boolean getAllowStudentViewReport();
	
	/********************* from previous AssignmentEdit interface ******************/

	/**
	 * Set the instructions for the Assignment.
	 * 
	 * @param instructions -
	 *        The Assignment's instructions.
	 */
	public void setInstructions(String instructions);
	/**
	 * Set the type of submission.
	 * 
	 * @param subType -
	 *        The type of submission.
	 */
	public void setTypeOfSubmission(int subType);

	/**
	 * Set the grade type.
	 * 
	 * @param gradeType -
	 *        The type of grade.
	 */
	public void setTypeOfGrade(int gradeType);

	/**
	 * Set the maximum grade for grade type = SCORE_GRADE_TYPE(3)
	 * 
	 * @param maxPoints -
	 *        The maximum grade score.
	 */
	public void setMaxGradePoint(int maxPoints);

	/**
	 * Set whether this project can be a group project.
	 * 
	 * @param groupProject -
	 *        True if this can be a group project, false otherwise.
	 */
	public void setGroupProject(boolean groupProject);

	/**
	 * Set whether group projects should be individually graded.
	 * 
	 * @param individGraded -
	 *        true if projects are individually graded, false if grades are given to the group.
	 */
	public void setIndividuallyGraded(boolean individGraded);

	/**
	 * Sets whether grades can be released once submissions are graded.
	 * 
	 * @param release -
	 *        true if grades can be released once submission are graded, false if they must be released manually.
	 */
	public void setReleaseGrades(boolean release);

	/**
	 * Set the Honor Pledge type; values are NONE and ENGINEERING_HONOR_PLEDGE.
	 * 
	 * @param pledgeType -
	 *        the Honor Pledge value.
	 */
	public void setHonorPledge(int pledgeType);

	/**
	 * Does this Assignment allow attachments?
	 * 
	 * @param allow -
	 *        true if the Assignment allows attachments, false otherwise?
	 */
	public void setAllowAttachments(boolean allow);

	/**
	 * Does this Assignment allow using the review service?
	 * 
	 * @param allow -
	 *        true if the Assignment allows review service, false otherwise?
	 */
	public void setAllowReviewService(boolean allow);
	
	/**
	 * Set whether this sssignment allow students to view review service reports?
	 * 
	 * @param allow -
	 *        true if the Assignment allows review service, false otherwise?
	 */
	public void setAllowStudentViewReport(boolean allow);

	/**
	 * Set the time last modified.
	 * 
	 * @param lastmod -
	 *        The Time at which the Content was last modified.
	 */
	public void setTimeLastModified(Time lastmod);
}
