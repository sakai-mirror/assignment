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

package org.sakaiproject.assignment.api;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.user.api.User;
import org.w3c.dom.Element;

import org.sakaiproject.assignment.model.*;

/**
 * <p>
 * AssignmentService is the service that handles assignments.
 * </p>
 */
public interface AssignmentService extends EntityProducer
{
	/** The type string for this application: should not change over time as it may be stored in various parts of persistent entities. */
	static final String APPLICATION_ID = "sakai:assignment";

	/** This string starts the references to resources in this service. */
	public static final String REFERENCE_ROOT = "/assignment";

	/** Security function giving the user permission to receive assignment submission email */
	public static final String SECURE_ASSIGNMENT_RECEIVE_NOTIFICATIONS = "asn.receive.notifications";
	
	/** Security lock for adding an assignment. */
	public static final String SECURE_ADD_ASSIGNMENT = "asn.new";

	/** Security lock for adding an assignment. */
	public static final String SECURE_ADD_ASSIGNMENT_CONTENT = "asn.new";

	/** Security lock for adding an assignment submission. */
	public static final String SECURE_ADD_ASSIGNMENT_SUBMISSION = "asn.submit";

	/** Security lock for removing an assignment. */
	public static final String SECURE_REMOVE_ASSIGNMENT = "asn.delete";

	/** Security lock for removing an assignment content. */
	public static final String SECURE_REMOVE_ASSIGNMENT_CONTENT = "asn.delete";

	/** Security lock for removing an assignment submission. */
	public static final String SECURE_REMOVE_ASSIGNMENT_SUBMISSION = "asn.delete";

	/** Security lock for accessing an assignment. */
	public static final String SECURE_ACCESS_ASSIGNMENT = "asn.read";

	/** Security lock for accessing an assignment content. */
	public static final String SECURE_ACCESS_ASSIGNMENT_CONTENT = "asn.read";

	/** Security lock for accessing an assignment submission. */
	public static final String SECURE_ACCESS_ASSIGNMENT_SUBMISSION = "asn.submit";

	/** Security lock for updating an assignment. */
	public static final String SECURE_UPDATE_ASSIGNMENT = "asn.revise";

	/** Security lock for updating an assignment content. */
	public static final String SECURE_UPDATE_ASSIGNMENT_CONTENT = "asn.revise";

	/** Security lock for updating an assignment submission. */
	public static final String SECURE_UPDATE_ASSIGNMENT_SUBMISSION = "asn.submit";

	/** Security lock for grading submission */
	public static final String SECURE_GRADE_ASSIGNMENT_SUBMISSION = "asn.grade";

	/** Security function giving the user permission to all groups, if granted to at the site level. */
	public static final String SECURE_ALL_GROUPS = "asn.all.groups";
	
	/** Security function giving the user permission to share drafts within his/her role for a given site */
	public static final String SECURE_SHARE_DRAFTS = "asn.share.drafts";

	/** The Reference type for a site where site groups are to be considered in security computation. */
	public static final String REF_TYPE_SITE_GROUPS = "site-groups";

	/** The Reference type for an assignment. */
	public static final String REF_TYPE_ASSIGNMENT = "a";

	/** The Reference type for an assignment where site groups are to be considered in security computation. */
	public static final String REF_TYPE_ASSIGNMENT_GROUPS = "a-groups";

	/** The Reference type for a submission. */
	public static final String REF_TYPE_SUBMISSION = "s";

	/** The Reference type for a content. */
	public static final String REF_TYPE_CONTENT = "c";

	/** The Reference type for a grade spreadsheet. */
	public static final String REF_TYPE_GRADES = "grades";

	/** The Reference type for a submissions zip. */
	public static final String REF_TYPE_SUBMISSIONS = "submissions";
	
	// the three choices for Gradebook Integration
	public static final String GRADEBOOK_INTEGRATION_NO = "no";
	public static final String GRADEBOOK_INTEGRATION_ADD = "add";
	public static final String GRADEBOOK_INTEGRATION_ASSOCIATE = "associate";
	public static final String NEW_ASSIGNMENT_ADD_TO_GRADEBOOK = "new_assignment_add_to_gradebook";
	
	// and the prop name
	public static final String PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT = "prop_new_assignment_add_to_gradebook";
	
	/**
	 * Check permissions for receiving assignment submission notification email
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return True if the current User is allowed to receive the email, false if not.
	 */
	public boolean allowReceiveSubmissionNotification(String context);
	
	/**
	 * Get the List of Users who can add assignment
	 * 
	 * @param assignmentReference -
	 *        a reference to an assignment
	 * @return the List (User) of users who can addSubmission() for this assignment.
	 */
	public List allowReceiveSubmissionNotificationUsers(String context);
	
	/**
	 * Check permissions for adding an Assignment.
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return True if the current User is allowed to add an Assignment, false if not.
	 */
	public boolean allowAddAssignment(String context);

	/**
	 * Check if the user has permission to add a site-wide (not grouped) assignment.
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return true if the user has permission to add a channel-wide (not grouped) assignment.
	 */
	boolean allowAddSiteAssignment(String context);
	
	/**
	 * Check permissions for all.groups.
	 *
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return True if the current User is allowed all.groups, false if not.
	 */
	public boolean allowAllGroups(String context);

	/**
	 * Check permissions for reading an Assignment.
	 * 
	 * @param assignmentReference -
	 *        The Assignment's reference.
	 * @return True if the current User is allowed to get the Assignment, false if not.
	 */
	public boolean allowGetAssignment(String assignmentReference);

	/**
	 * Get the collection of Groups defined for the context of this site that the end user has add assignment permissions in.
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return The Collection (Group) of groups defined for the context of this site that the end user has add assignment permissions in, empty if none.
	 */
	Collection getGroupsAllowAddAssignment(String context);
	
	/**
	 * Get the collection of Groups defined for the context of this site that the end user has grade assignment permissions in.
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return The Collection (Group) of groups defined for the context of this site that the end user has grade assignment permissions in, empty if none.
	 */
	Collection getGroupsAllowGradeAssignment(String context, String assignmentReference);

	/**
	 * Check permissions for updating an Assignment.
	 * 
	 * @param assignmentReference -
	 *        The Assignment's reference.
	 * @return True if the current User is allowed to update the Assignment, false if not.
	 */
	public boolean allowUpdateAssignment(String assignmentReference);

	/**
	 * Check permissions for removing an Assignment.
	 * 
	 * @param assignmentReference -
	 *        The Assignment's reference.
	 * @return True if the current User is allowed to remove the Assignment, false if not.
	 */
	public boolean allowRemoveAssignment(String assignmentReference);

	/**
	 * Check permissions for add AssignmentSubmission
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return True if the current User is allowed to add an AssignmentSubmission, false if not.
	 */
	public boolean allowAddSubmission(String context);

	/**
	 * Get the List of Users who can addSubmission() for this assignment.
	 * 
	 * @param assignmentReference -
	 *        a reference to an assignment
	 * @return the List (User) of users who can addSubmission() for this assignment.
	 */
	public List allowAddSubmissionUsers(String assignmentReference);
	
	 /* Get the List of Users who can grade submission for this assignment.
	 * 
	 * @param assignmentReference -
	 *        a reference to an assignment
	 * @return the List (User) of users who can grade submission for this assignment.
	 */
	public List allowGradeAssignmentUsers(String assignmentReference);
	
	/**
	 * Get the list of users who can add submission for at lease one assignment within the context
	 * @param context the context string
	 * @return the list of user (ids)
	 */
	public List allowAddAnySubmissionUsers(String context);

	/**
	 * Get the List of Users who can add assignment
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return the List (User) of users who can add assignment
	 */
	public List allowAddAssignmentUsers(String context);

	/**
	 * Check permissions for reading a Submission.
	 * 
	 * @param submissionReference -
	 *        The Submission's reference.
	 * @return True if the current User is allowed to get the AssignmentSubmission, false if not.
	 */
	public boolean allowGetSubmission(String submissionReference);

	/**
	 * Check permissions for updating Submission.
	 * 
	 * @param submissionReference -
	 *        The Submission's reference.
	 * @return True if the current User is allowed to update the AssignmentSubmission, false if not.
	 */
	public boolean allowUpdateSubmission(String submissionReference);

	/**
	 * Check permissions for remove Submission
	 * 
	 * @param submissionReference -
	 *        The Submission's reference.
	 * @return True if the current User is allowed to remove the AssignmentSubmission, false if not.
	 */
	public boolean allowRemoveSubmission(String submissionReference);

	/**
	 * Check permissions for grading Submission
	 * 
	 * @param submissionReference -
	 *        The Submission's reference.
	 * @return True if the current User is allowed to grade the AssignmentSubmission, false if not.
	 */
	public boolean allowGradeSubmission(String submissionReference);

	/**
	 * Creates and adds a new Assignment to the service.
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return AssignmentEdit The new Assignment object.
	 * @throws IdInvalidException
	 *         if the id contains prohibited characers.
	 * @throws IdUsedException
	 *         if the id is already used in the service.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public Assignment addAssignment(String context) throws PermissionException;

	/**
	 * Add a new assignment to the directory, from a definition in XML. Must commitEdit() to make official, or cancelEdit() when done!
	 * 
	 * @param el
	 *        The XML DOM Element defining the assignment.
	 * @return A locked Assignment object (reserving the id).
	 * @exception IdInvalidException
	 *            if the assignment id is invalid.
	 * @exception IdUsedException
	 *            if the assignment id is already used.
	 * @exception PermissionException
	 *            if the current user does not have permission to add an assignnment.
	 */
	public Assignment mergeAssignment(Element el) throws IdInvalidException, IdUsedException, PermissionException;

	/**
	 * Creates and adds a new Assignment to the service which is a copy of an existing Assignment.
	 * 
	 * @param context -
	 *        From DefaultId.getChannel(RunData)
	 * @param assignmentReference -
	 *        The reference of the Assignment to be duplicated.
	 * @return The new Assignment object, or null if the original Assignment does not exist.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public Assignment addDuplicateAssignment(String context, String assignmentReference) throws IdInvalidException,
			PermissionException, IdUsedException, IdUnusedException;

	/**
	 * Removes this Assignment and all references to it.
	 * 
	 * @param assignment -
	 *        The Assignment to remove.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public void removeAssignment(Assignment assignment) throws PermissionException;

	/**
	 * Get a locked assignment object for editing. Must commitEdit() to make official, or cancelEdit() when done!
	 * 
	 * @param id
	 *        The assignment id string.
	 * @return A Assignment object for editing.
	 * @exception IdUnusedException
	 *            if not found, or if not an Assignment object
	 * @exception PermissionException
	 *            if the current user does not have permission to edit this assignment.
	 * @exception InUseException
	 *            if the Assignment object is locked by someone else.
	 */
	public Assignment editAssignment(String id) throws IdUnusedException, PermissionException, InUseException;

	/**
	 * Commit the changes made to a Assignment object, and release the lock. The Assignment is disabled, and not to be used after this call.
	 * 
	 * @param assignment
	 *        The Assignment object to commit.
	 */
	public void commitEdit(Assignment assignment);

	/**
	 * Cancel the changes made to a Assignment object, and release the lock. The Assignment is disabled, and not to be used after this call.
	 * 
	 * @param assignment
	 *        The Assignment object to commit.
	 */
	public void cancelEdit(Assignment assignment);

	/**
	 * Adds an AssignmentSubmission
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @param assignmentId The assignment id
	 * @param submitterId The submitter id
	 * @return The new AssignmentSubmission.
	 * @exception IdInvalidException
	 *            if the submission id is invalid.
	 * @exception IdUsedException
	 *            if the submission id is already used.
	 * @throws PermissionException
	 *         if the current User does not have permission to do this.
	 */
	public AssignmentSubmission addSubmission(String context, String assignmentId, String submitter) throws PermissionException;

	/**
	 * Add a new AssignmentSubmission to the directory, from a definition in XML. Must commitEdit() to make official, or cancelEdit() when done!
	 * 
	 * @param el
	 *        The XML DOM Element defining the submission.
	 * @return A locked AssignmentSubmission object (reserving the id).
	 * @exception IdInvalidException
	 *            if the submission id is invalid.
	 * @exception IdUsedException
	 *            if the submission id is already used.
	 * @exception PermissionException
	 *            if the current user does not have permission to add a submission.
	 */
	public AssignmentSubmission mergeSubmission(Element el) throws IdInvalidException, IdUsedException, PermissionException;

	/**
	 * Removes an AssignmentSubmission and all references to it
	 * 
	 * @param submission -
	 *        the AssignmentSubmission to remove.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public void removeSubmission(AssignmentSubmission submission) throws PermissionException;

	/**
	 * Get a locked AssignmentSubmission object for editing. Must commitEdit() to make official, or cancelEdit() when done!
	 * 
	 * @param id
	 *        The submission id string.
	 * @return An AssignmentSubmission object for editing.
	 * @exception IdUnusedException
	 *            if not found, or if not an AssignmentSubmission object
	 * @exception PermissionException
	 *            if the current user does not have permission to edit this submission.
	 * @exception InUseException
	 *            if the AssignmentSubmission object is locked by someone else.
	 */
	public AssignmentSubmission editSubmission(String id) throws IdUnusedException, PermissionException, InUseException;

	/**
	 * Commit the changes made to a AssignmentSubmission object, and release the lock. The AssignmentSubmission is disabled, and not to be used after this call.
	 * 
	 * @param submission
	 *        The AssignmentSubmission object to commit.
	 */
	public void commitEdit(AssignmentSubmission submission);

	/**
	 * Cancel the changes made to a AssignmentSubmission object, and release the lock. The AssignmentSubmission is disabled, and not to be used after this call.
	 * 
	 * @param submission
	 *        The AssignmentSubmission object to commit.
	 */
	public void cancelEdit(AssignmentSubmission submission);

	/**
	 * Access the Assignment with the specified id.
	 * 
	 * @param assignmentId -
	 *        The id of the Assignment.
	 * @return The Assignment corresponding to the id, or null if it does not exist.
	 * @throws IdUnusedException
	 *         if there is no object with this id.
	 * @throws PermissionException
	 *         if the current user is not allowed to read this.
	 */
	public Assignment getAssignment(String assignmentId) throws IdUnusedException, PermissionException;
	
	/**
	 * Access the AssignmentSubmission with the specified id.
	 * 
	 * @param submissionId -
	 *        The id of the AssignmentSubmission.
	 * @return The AssignmentSubmission corresponding to the id, or null if it does not exist.
	 * @throws IdUnusedException
	 *         if there is no object with this id.
	 * @throws PermissionException
	 *         if the current user is not allowed to read this.
	 */
	public AssignmentSubmission getSubmission(String submissionId) throws IdUnusedException, PermissionException;

	/**
	 * Access all the Assignemnts associated with the context.
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return Iterator over all the Assignments associated with a group.
	 */
	public Iterator getAssignmentsForContext(String context);
	
	/**
	 * Access all the Assignemnts associated with the context and accesible by the user
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @param userId
	 * 		 The user id
	 * @return Iterator over all the Assignments associated with a group.
	 */
	public Iterator getAssignmentsForContext(String context, String userId);
	
	/**
	 * Access all the Assignemnts that are not deleted and self-drafted ones
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return List All the Assignments will be listed
	 */
	public List getListAssignmentsForContext(String context);

	/**
	 * Access a User's AssignmentSubmission to a particular Assignment.
	 * 
	 * @param assignmentId -
	 *        The id of the assignment.
	 * @param person -
	 *        The User who's Submission you would like.
	 * @return AssignmentSubmission The user's submission for that Assignment, or null if one does not exist.
	 * @throws IdUnusedException
	 *         if the assignmentId does not correspond to an existing Assignment.
	 * @throws PermissionException
	 *         if the current user is not allowed to read this.
	 */
	public AssignmentSubmission getSubmission(String assignmentId, User person);
	
	/**
	 * Access a User's AssignmentSubmission inside a list of AssignmentSubmission object.
	 * 
	 * @param  - submissions
	 *        The list of submissions
	 * @param person -
	 *        The User who's Submission you would like.
	 * @return AssignmentSubmission The user's submission for that Assignment, or null if one does not exist.
	 */
	public AssignmentSubmission getSubmission(List submissions, User person);

	/**
	 * Get the submissions for an assignment.
	 * 
	 * @param assignment -
	 *        the Assignment who's submissions you would like.
	 * @return List over all the submissions for an Assignment.
	 */
	public List getSubmissions(Assignment assignment);
	
	/**
	 * Get the number of submissions which has been submitted.
	 * 
	 * @param assignmentId -
	 *        the id of Assignment who's submissions you would like.
	 * @return List over all the submissions for an Assignment.
	 */
	public int getSubmittedSubmissionsCount(String assignmentId);
	
	/**
	 * Get the number of submissions which has not been submitted and graded.
	 * 
	 * @param assignmentId -
	 *        the id of Assignment who's submissions you would like.
	 * @return List over all the submissions for an Assignment.
	 */
	public int getUngradedSubmissionsCount(String assignmentId);

	/**
	 * Access the grades spreadsheet for the reference, either for an assignment or all assignments in a context.
	 * 
	 * @param ref
	 *        The reference, either to a specific assignment, or just to an assignment context.
	 * @return The grades spreadsheet bytes.
	 * @throws IdUnusedException
	 *         if there is no object with this id.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public byte[] getGradesSpreadsheet(String ref) throws IdUnusedException, PermissionException;

	/**
	 * Access the submissions zip for the assignment reference.
	 * 
	 * @param ref
	 *        The assignment reference.
	 * @param out
	 * 		  	The outputStream to stream the zip file into
	 * @return The submissions zip bytes.
	 * @throws IdUnusedException
	 *         if there is no object with this id.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public void getSubmissionsZip(OutputStream out, String ref) throws IdUnusedException, PermissionException;
	
	/**
	 * Access the internal reference which can be used to assess security clearance.
	 * 
	 * @param id
	 *        The assignment id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String assignmentReference(String context, String id);

	/**
	 * Access the internal reference which can be used to access the resource from within the system.
	 * 
	 * @param id
	 *        The content id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String contentReference(String context, String id);

	/**
	 * Access the internal reference which can be used to access the resource from within the system.
	 * 
	 * @param id
	 *        The submission id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String submissionReference(String context, String id, String assignmentId);

	/**
	 * Get the String to form an assignment grade spreadsheet
	 * 
	 * @param context
	 *        The assignment context String
	 * @param assignmentId
	 *        The id for the assignment object; when null, indicates all assignment in that context
	 */
	public String gradesSpreadsheetReference(String context, String assignmentId);

	/**
	 * Get the string to form an assignment submissions zip file
	 * 
	 * @param context
	 *        The assignment context String
	 * @param assignmentId
	 *        The id for the assignment object;
	 */
	public String submissionsZipReference(String context, String assignmentId);
	
	/**
	 * Whether assignment could be assigned to groups
	 */
	public boolean getAllowGroupAssignments();
	
	/**
	 * Whether assignment with group access can be added to gradebook
	 */
	public boolean getAllowGroupAssignmentsInGradebook();
	
	/**
	 * Whether the current user can submit
	 */
	public boolean canSubmit(String context, Assignment a);
	
}