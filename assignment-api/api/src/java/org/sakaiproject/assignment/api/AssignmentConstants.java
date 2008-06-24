package org.sakaiproject.assignment.api;

public class AssignmentConstants {
	
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

	public static final String GROUP_LIST = "group";

	public static final String GROUP_NAME = "authzGroup";
	
	/** number of times that the submission is allowed to resubmit */
	public static final String ALLOW_RESUBMIT_NUMBER = "allow_resubmit_number";
	
	/** submission level of close time*/
	public static final String ALLOW_RESUBMIT_CLOSETIME = "allow_resubmit_closeTime";

}
