/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 The Sakai Foundation.
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

import java.util.List;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.Edit;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.user.api.User;

/**
 * <p>
 * AssignmentSubmission is the an interface for the Sakai assignments module. It represents student submissions for assignments.
 * </p>
 */
public interface AssignmentSubmission extends Entity, Edit
{	
	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Access the context at the time of creation.
	 * 
	 * @return String - the context string.
	 */
	public String getContext();

	/**
	 * Access the Assignment for this Submission
	 * 
	 * @return the Assignment
	 */
	public Assignment getAssignment();

	/**
	 * Access the ID for the Assignment for this Submission
	 * 
	 * @return String - the Assignment id
	 */
	public String getAssignmentId();

	/**
	 * Access the list of Users who submitted this response to the Assignment.
	 * 
	 * @return Array of user objects.
	 */
	public User[] getSubmitters();

	/**
	 * Access the list of Users who submitted this response to the Assignment.
	 * 
	 * @return List of user ids
	 */
	public List getSubmitterIds();
	
	/**
	 * Access the concat the submitter id together and form a String
	 * 
	 * @return List of user ids
	 */
	public String getSubmitterIdString();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Get whether this is a final submission.
	 * 
	 * @return True if a final submission, false if still a draft.
	 */
	public boolean getSubmitted();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set the time at which this response was submitted; null signifies the response is unsubmitted.
	 * 
	 * @return Time of submission.
	 */
	public Time getTimeSubmitted();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set the time at which this response was submitted; "" signifies the response is unsubmitted.
	 * 
	 * @return Time of submission (String)
	 */
	public String getTimeSubmittedString();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Text submitted in response to the Assignment.
	 * 
	 * @return The text of the submission.
	 */
	public String getSubmittedText();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Access the list of attachments to this response to the Assignment.
	 * 
	 * @return List of the list of attachments as Reference objects;
	 */
	public List getSubmittedAttachments();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Get the general comments by the grader
	 * 
	 * @return The text of the grader's comments; may be null.
	 */
	public String getFeedbackComment();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Access the text part of the instructors feedback; usually an annotated copy of the submittedText
	 * 
	 * @return The text of the grader's feedback.
	 */
	public String getFeedbackText();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Access the formatted text part of the instructors feedback; usually an annotated copy of the submittedText
	 * 
	 * @return The formatted text of the grader's feedback.
	 */
	public String getFeedbackFormattedText();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Access the list of attachments returned to the students in the process of grading this assignment; usually a modified or annotated version of the attachment submitted.
	 * 
	 * @return List of the Resource objects pointing to the attachments.
	 */
	public List getFeedbackAttachments();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Get whether this Submission was rejected by the grader.
	 * 
	 * @return True if this response was rejected by the grader, false otherwise.
	 */
	public boolean getReturned();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Get whether this Submission has been graded.
	 * 
	 * @return True if the submission has been graded, false otherwise.
	 */
	public boolean getGraded();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Get whether the grade has been released.
	 * 
	 * @return True if the Submissions's grade has been released, false otherwise.
	 */
	public boolean getGradeReleased();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Access the grade recieved.
	 * 
	 * @return The Submission's grade..
	 */
	public String getGrade();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Access the grade recieved. When points-type, format it to one decimal place
	 * 
	 * @return The Submission's grade..
	 */
	public String getGradeDisplay();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Get the time of last modification;
	 * 
	 * @return The time of last modification.
	 */
	public Time getTimeLastModified();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Get the time at which the graded submission was returned; null means the response is not yet graded.
	 * 
	 * @return the time (may be null)
	 */
	public Time getTimeReturned();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Access the checked status of the honor pledge flag.
	 * 
	 * @return True if the honor pledge is checked, false otherwise.
	 */
	public boolean getHonorPledgeFlag();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Returns the status of the submission : Not Started, submitted, returned or graded.
	 * 
	 * @return The Submission's status.
	 */
	public String getStatus();

	/**
	 * Method to get the number of allowed resubmission
	 */
	public int getResubmissionNum();
	
	/**
	 * Method to return the close time for the submission
	 */
	public Time getCloseTime();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	* Method to return the score from ContentReview Service
	*/
	public int getReviewScore();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	* Method to get the URL to the content Review Report
	*/
	public String getReviewReport();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	* Method to get the status of the review
	*/
	public String getReviewStatus();
 	
	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 *  the URL of the content review Icon associated with this submission
	 * @return
	 */
	public String getReviewIconUrl();
	
	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set the AssignmentSubmissions's context at the time of creation.
	 * 
	 * @param context -
	 *        The context string.
	 */
	public void setContext(String context);

	/**
	 * Set the Assignment for this Submission
	 * 
	 * @param assignment -
	 *        the Assignment
	 */
	public void setAssignment(Assignment assignment);

	/**
	 * Add a User to the submitters list.
	 * 
	 * @param submitter -
	 *        the User to add.
	 */
	public void addSubmitter(User submitter);

	/**
	 * Remove an User from the submitter list
	 * 
	 * @param submitter -
	 *        the User to remove.
	 */
	public void removeSubmitter(User submitter);

	/**
	 * Remove all user from the submitter list
	 */
	public void clearSubmitters();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set whether this is a final submission.
	 * 
	 * @param submitted -
	 *        True if a final submission, false if still a draft.
	 */
	public void setSubmitted(boolean submitted);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set the time at which this response was submitted; setting it to null signifies the response is unsubmitted.
	 * 
	 * @param timeSubmitted -
	 *        Time of submission.
	 */
	public void setTimeSubmitted(Time timeSubmitted);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Text submitted in response to the Assignment.
	 * 
	 * @param submissionText -
	 *        The text of the submission.
	 */
	public void setSubmittedText(String submissionText);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Add an attachment to the list of submitted attachments.
	 * 
	 * @param attachment -
	 *        The Reference object pointing to the attachment.
	 */
	public void addSubmittedAttachment(Reference attachment);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Remove an attachment from the list of submitted attachments
	 * 
	 * @param attachment -
	 *        The Reference object pointing to the attachment.
	 */
	public void removeSubmittedAttachment(Reference attachment);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Remove all submitted attachments.
	 */
	public void clearSubmittedAttachments();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set the general comments by the grader.
	 * 
	 * @param comment -
	 *        the text of the grader's comments; may be null.
	 */
	public void setFeedbackComment(String comment);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set the text part of the instructors feedback; usually an annotated copy of the submittedText
	 * 
	 * @param feedback -
	 *        The text of the grader's feedback.
	 */
	public void setFeedbackText(String feedback);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Add an attachment to the list of feedback attachments.
	 * 
	 * @param attachment -
	 *        The Resource object pointing to the attachment.
	 */
	public void addFeedbackAttachment(Reference attachment);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Remove an attachment from the list of feedback attachments.
	 * 
	 * @param attachment -
	 *        The Resource pointing to the attachment to remove.
	 */
	public void removeFeedbackAttachment(Reference attachment);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Remove all feedback attachments.
	 */
	public void clearFeedbackAttachments();

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set whether this Submission was rejected by the grader.
	 * 
	 * @param returned -
	 *        true if this response was rejected by the grader, false otherwise.
	 */
	public void setReturned(boolean returned);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set whether this Submission has been graded.
	 * 
	 * @param graded -
	 *        true if the submission has been graded, false otherwise.
	 */
	public void setGraded(boolean graded);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set whether the grade has been released.
	 * 
	 * @param released -
	 *        True if the Submissions's grade has been released, false otherwise.
	 */
	public void setGradeReleased(boolean released);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Sets the grade for the Submisssion.
	 * 
	 * @param grade -
	 *        The Submission's grade.
	 */
	public void setGrade(String grade);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set the time at which the graded Submission was returned; setting it to null means it is not yet graded.
	 * 
	 * @param timeReturned -
	 *        The time at which the graded Submission was returned.
	 */
	public void setTimeReturned(Time timeReturned);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set the checked status of the honor pledge flag.
	 * 
	 * @param honorPledgeFlag -
	 *        True if the honor pledge is checked, false otherwise.
	 */
	public void setHonorPledgeFlag(boolean honorPledgeFlag);

	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Set the time last modified.
	 * 
	 * @param lastmod -
	 *        The Time at which the Submission was last modified.
	 */
	public void setTimeLastModified(Time lastmod);
	
	/**
	 * @deprecated
	 * See AssignmentSubmissionVersion api
	 * Post attachments to the content review service
	 * @param attachments
	 */
	public void postAttachment(List attachments);
	
	/**
	 * Get the list of associated SubmissionVersion object
	 * @return
	 */
	public List<AssignmentSubmissionVersion> getSubmissionVersionList();
	
	/**
	 * Set the list of associated SubmissionVersion object
	 * @param submissionVersionList
	 */
	public void setSubmissionVersionList(List<AssignmentSubmissionVersion> submissionVersionList);
	
	/**
	 * Get the id of last/latest version id
	 * @return
	 */
	public String getLastVersionId();
	
	/**
	 * Set the id of last/latest version id
	 * @param
	 */
	public void setLastVersionId(String lastVersionId);
}
