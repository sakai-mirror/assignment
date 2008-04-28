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

package org.sakaiproject.assignment.tool;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.nio.channels.*;
import java.nio.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.announcement.api.AnnouncementChannel;
import org.sakaiproject.announcement.api.AnnouncementMessage;
import org.sakaiproject.announcement.api.AnnouncementMessageEdit;
import org.sakaiproject.announcement.api.AnnouncementMessageHeaderEdit;
import org.sakaiproject.announcement.api.AnnouncementService;
import org.sakaiproject.assignment.model.Assignment;
import org.sakaiproject.assignment.model.AssignmentGroup;
import org.sakaiproject.assignment.model.AssignmentSubmission;
import org.sakaiproject.assignment.model.AssignmentSubmissionVersion;
import org.sakaiproject.assignment.model.constants.AssignmentConstants;

import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer;
import org.sakaiproject.taggable.api.TaggingHelperInfo;
import org.sakaiproject.taggable.api.TaggingManager;
import org.sakaiproject.taggable.api.TaggingProvider;
import org.sakaiproject.assignment.taggable.tool.DecoratedTaggingProvider;
import org.sakaiproject.assignment.taggable.tool.DecoratedTaggingProvider.Pager;
import org.sakaiproject.assignment.taggable.tool.DecoratedTaggingProvider.Sort;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.PermissionsHelper;
import org.sakaiproject.authz.cover.AuthzGroupService;
import org.sakaiproject.calendar.api.Calendar;
import org.sakaiproject.calendar.api.CalendarEvent;
import org.sakaiproject.calendar.api.CalendarEventEdit;
import org.sakaiproject.calendar.api.CalendarService;
import org.sakaiproject.cheftool.Context;
import org.sakaiproject.cheftool.JetspeedRunData;
import org.sakaiproject.cheftool.PagedResourceActionII;
import org.sakaiproject.cheftool.PortletConfig;
import org.sakaiproject.cheftool.RunData;
import org.sakaiproject.cheftool.VelocityPortlet;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.content.api.ContentTypeImageService;
import org.sakaiproject.content.api.FilePickerHelper;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.entity.cover.EntityManager;
import org.sakaiproject.event.api.SessionState;
import org.sakaiproject.event.cover.EventTrackingService;
import org.sakaiproject.event.cover.NotificationService;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.javax.PagingPosition;
import org.sakaiproject.service.gradebook.shared.AssignmentHasIllegalPointsException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.service.gradebook.shared.AssignmentHasIllegalPointsException;
import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.ConflictingExternalIdException;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeBreakdown;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolSession;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.FileItem;
import org.sakaiproject.util.FormattedText;
import org.sakaiproject.util.ParameterParser;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.SortedIterator;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Validator;
import org.sakaiproject.contentreview.service.ContentReviewService;
import org.sakaiproject.genericdao.api.CompleteGenericDao;

/**
 * <p>
 * AssignmentAction is the action class for the assignment tool.
 * </p>
 */
public class AssignmentAction extends PagedResourceActionII
{
	/** Our logger. */
	private static Log log = LogFactory.getLog(AssignmentAction.class);
	
	private static ResourceLoader rb = new ResourceLoader("assignment");

	private static final String ASSIGNMENT_TOOL_ID = "sakai.assignment.grades";
	
	private static final Boolean allowReviewService = ServerConfigurationService.getBoolean("assignment.useContentReview", false);
	
	/** Is the review service available? */
	private static final String ALLOW_REVIEW_SERVICE = "allow_review_service";
	
	private static final String NEW_ASSIGNMENT_USE_REVIEW_SERVICE = "new_assignment_use_review_service";
	
	private static final String NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW = "new_assignment_allow_student_view";
	
	
	
	
	/** The attachments */
	private static final String ATTACHMENTS = "AssignmentConstants.attachments";

	/** The content type image lookup service in the State. */
	private static final String STATE_CONTENT_TYPE_IMAGE_SERVICE = "AssignmentConstants.content_type_image_service";

	/** The calendar service in the State. */
	private static final String STATE_CALENDAR_SERVICE = "AssignmentConstants.calendar_service";

	/** The announcement service in the State. */
	private static final String STATE_ANNOUNCEMENT_SERVICE = "AssignmentConstants.announcement_service";

	/** The calendar object */
	private static final String CALENDAR = "calendar";
	
	/** The calendar tool */
	private static final String CALENDAR_TOOL_EXIST = "calendar_tool_exisit";

	/** The announcement tool */
	private static final String ANNOUNCEMENT_TOOL_EXIST = "announcement_tool_exist";
	
	/** The announcement channel */
	private static final String ANNOUNCEMENT_CHANNEL = "announcement_channel";

	/** The state mode */
	private static final String STATE_MODE = "AssignmentConstants.mode";

	/** The context string */
	private static final String STATE_CONTEXT_STRING = "AssignmentConstants.context_string";

	/** The user */
	private static final String STATE_USER = "AssignmentConstants.user";

	// SECTION MOD
	/** Used to keep track of the section info not currently being used. */
	private static final String STATE_SECTION_STRING = "AssignmentConstants.section_string";

	/** **************************** sort assignment ********************** */
	/** state sort * */
	private static final String SORTED_BY = "AssignmentConstants.sorted_by";

	/** state sort ascendingly * */
	private static final String SORTED_ASC = "AssignmentConstants.sorted_asc";
	
	/** default sorting */
	private static final String SORTED_BY_DEFAULT = "default";

	/** sort by assignment title */
	private static final String SORTED_BY_TITLE = "title";

	/** sort by assignment section */
	private static final String SORTED_BY_SECTION = "section";

	/** sort by assignment due date */
	private static final String SORTED_BY_DUEDATE = "duedate";

	/** sort by assignment open date */
	private static final String SORTED_BY_OPENDATE = "opendate";

	/** sort by assignment status */
	private static final String SORTED_BY_ASSIGNMENT_STATUS = "assignment_status";

	/** sort by assignment submission status */
	private static final String SORTED_BY_SUBMISSION_STATUS = "submission_status";

	/** sort by assignment number of submissions */
	private static final String SORTED_BY_NUM_SUBMISSIONS = "num_submissions";

	/** sort by assignment number of ungraded submissions */
	private static final String SORTED_BY_NUM_UNGRADED = "num_ungraded";

	/** sort by assignment submission grade */
	private static final String SORTED_BY_GRADE = "grade";

	/** sort by assignment maximun grade available */
	private static final String SORTED_BY_MAX_GRADE = "max_grade";

	/** sort by assignment range */
	private static final String SORTED_BY_FOR = "for";

	/** sort by group title */
	private static final String SORTED_BY_GROUP_TITLE = "group_title";

	/** sort by group description */
	private static final String SORTED_BY_GROUP_DESCRIPTION = "group_description";

	/** *************************** sort submission in instructor grade view *********************** */
	/** state sort submission* */
	private static final String SORTED_GRADE_SUBMISSION_BY = "AssignmentConstants.grade_submission_sorted_by";

	/** state sort submission ascendingly * */
	private static final String SORTED_GRADE_SUBMISSION_ASC = "AssignmentConstants.grade_submission_sorted_asc";

	/** state sort submission by submitters last name * */
	private static final String SORTED_GRADE_SUBMISSION_BY_LASTNAME = "sorted_grade_submission_by_lastname";

	/** state sort submission by submit time * */
	private static final String SORTED_GRADE_SUBMISSION_BY_SUBMIT_TIME = "sorted_grade_submission_by_submit_time";

	/** state sort submission by submission status * */
	private static final String SORTED_GRADE_SUBMISSION_BY_STATUS = "sorted_grade_submission_by_status";

	/** state sort submission by submission grade * */
	private static final String SORTED_GRADE_SUBMISSION_BY_GRADE = "sorted_grade_submission_by_grade";

	/** state sort submission by submission released * */
	private static final String SORTED_GRADE_SUBMISSION_BY_RELEASED = "sorted_grade_submission_by_released";
	
	/** state sort submissuib by content review score **/
	private static final String SORTED_GRADE_SUBMISSION_CONTENTREVIEW = "sorted_grade_submission_by_contentreview";

	/** *************************** sort submission *********************** */
	/** state sort submission* */
	private static final String SORTED_SUBMISSION_BY = "AssignmentConstants.submission_sorted_by";

	/** state sort submission ascendingly * */
	private static final String SORTED_SUBMISSION_ASC = "AssignmentConstants.submission_sorted_asc";

	/** state sort submission by submitters last name * */
	private static final String SORTED_SUBMISSION_BY_LASTNAME = "sorted_submission_by_lastname";

	/** state sort submission by submit time * */
	private static final String SORTED_SUBMISSION_BY_SUBMIT_TIME = "sorted_submission_by_submit_time";

	/** state sort submission by submission grade * */
	private static final String SORTED_SUBMISSION_BY_GRADE = "sorted_submission_by_grade";

	/** state sort submission by submission status * */
	private static final String SORTED_SUBMISSION_BY_STATUS = "sorted_submission_by_status";

	/** state sort submission by submission released * */
	private static final String SORTED_SUBMISSION_BY_RELEASED = "sorted_submission_by_released";

	/** state sort submission by assignment title */
	private static final String SORTED_SUBMISSION_BY_ASSIGNMENT = "sorted_submission_by_assignment";

	/** state sort submission by max grade */
	private static final String SORTED_SUBMISSION_BY_MAX_GRADE = "sorted_submission_by_max_grade";
	
	/*********************** Sort by user sort name *****************************************/
	private static final String SORTED_USER_BY_SORTNAME = "sorted_user_by_sortname";

	/** ******************** student's view assignment submission ****************************** */
	/** the assignment object been viewing * */
	private static final String VIEW_SUBMISSION_ASSIGNMENT_REFERENCE = "view_submission_assignment_reference";
	
	/** the submission object been viewing */
	private static final String VIEW_SUBMISSION_SUBMISSION_REFERENCE = "view_submission_submission_reference";

	/** the submission text to the assignment * */
	private static final String VIEW_SUBMISSION_TEXT = "view_submission_text";

	/** the submission answer to Honor Pledge * */
	private static final String VIEW_SUBMISSION_HONOR_PLEDGE_YES = "view_submission_honor_pledge_yes";

	/** ***************** student's preview of submission *************************** */
	/** the assignment id * */
	private static final String PREVIEW_SUBMISSION_ASSIGNMENT_REFERENCE = "preview_submission_assignment_reference";

	/** the submission text * */
	private static final String PREVIEW_SUBMISSION_TEXT = "preview_submission_text";

	/** the submission honor pledge answer * */
	private static final String PREVIEW_SUBMISSION_HONOR_PLEDGE_YES = "preview_submission_honor_pledge_yes";

	/** the submission attachments * */
	private static final String PREVIEW_SUBMISSION_ATTACHMENTS = "preview_attachments";

	/** the flag indicate whether the to show the student view or not */
	private static final String PREVIEW_ASSIGNMENT_STUDENT_VIEW_HIDE_FLAG = "preview_assignment_student_view_hide_flag";

	/** the flag indicate whether the to show the assignment info or not */
	private static final String PREVIEW_ASSIGNMENT_ASSIGNMENT_HIDE_FLAG = "preview_assignment_assignment_hide_flag";

	/** the assignment id */
	private static final String PREVIEW_ASSIGNMENT_ASSIGNMENT_ID = "preview_assignment_assignment_id";

	/** ************** view assignment ***************************************** */
	/** the hide assignment flag in the view assignment page * */
	private static final String VIEW_ASSIGNMENT_HIDE_ASSIGNMENT_FLAG = "view_assignment_hide_assignment_flag";

	/** the hide student view flag in the view assignment page * */
	private static final String VIEW_ASSIGNMENT_HIDE_STUDENT_VIEW_FLAG = "view_assignment_hide_student_view_flag";

	/** ******************* instructor's view assignment ***************************** */
	private static final String VIEW_ASSIGNMENT_ID = "view_assignment_id";

	/** ******************* instructor's edit assignment ***************************** */
	private static final String EDIT_ASSIGNMENT_ID = "edit_assignment_id";

	/** ******************* instructor's delete assignment ids ***************************** */
	private static final String DELETE_ASSIGNMENT_IDS = "delete_assignment_ids";

	/** ******************* flags controls the grade assignment page layout ******************* */
	private static final String GRADE_ASSIGNMENT_EXPAND_FLAG = "grade_assignment_expand_flag";

	private static final String GRADE_SUBMISSION_EXPAND_FLAG = "grade_submission_expand_flag";
	
	private static final String GRADE_NO_SUBMISSION_DEFAULT_GRADE = "grade_no_submission_default_grade";

	/** ******************* instructor's grade submission ***************************** */
	private static final String GRADE_SUBMISSION_ASSIGNMENT_ID = "grade_submission_assignment_id";

	private static final String GRADE_SUBMISSION_SUBMISSION_ID = "grade_submission_submission_id";

	private static final String GRADE_SUBMISSION_FEEDBACK_COMMENT = "grade_submission_feedback_comment";

	private static final String GRADE_SUBMISSION_FEEDBACK_TEXT = "grade_submission_feedback_text";

	private static final String GRADE_SUBMISSION_FEEDBACK_ATTACHMENT = "grade_submission_feedback_attachment";

	private static final String GRADE_SUBMISSION_GRADE = "grade_submission_grade";

	private static final String GRADE_SUBMISSION_ASSIGNMENT_EXPAND_FLAG = "grade_submission_assignment_expand_flag";

	private static final String GRADE_SUBMISSION_ALLOW_RESUBMIT = "grade_submission_allow_resubmit";
	
	/** ******************* instructor's export assignment ***************************** */
	private static final String EXPORT_ASSIGNMENT_REF = "export_assignment_ref";

	/**
	 * Is review service enabled? 
	 */
	private static final String ENABLE_REVIEW_SERVICE = "enable_review_service";

	private static final String EXPORT_ASSIGNMENT_ID = "export_assignment_id";

	/** ****************** instructor's new assignment ****************************** */
	private static final String NEW_ASSIGNMENT_TITLE = "new_assignment_title";
	
	// assignment order for default view
	private static final String NEW_ASSIGNMENT_ORDER = "new_assignment_order";

	// open date
	private static final String NEW_ASSIGNMENT_OPENMONTH = "new_assignment_openmonth";

	private static final String NEW_ASSIGNMENT_OPENDAY = "new_assignment_openday";

	private static final String NEW_ASSIGNMENT_OPENYEAR = "new_assignment_openyear";

	private static final String NEW_ASSIGNMENT_OPENHOUR = "new_assignment_openhour";

	private static final String NEW_ASSIGNMENT_OPENMIN = "new_assignment_openmin";

	private static final String NEW_ASSIGNMENT_OPENAMPM = "new_assignment_openampm";

	// due date
	private static final String NEW_ASSIGNMENT_DUEMONTH = "new_assignment_duemonth";

	private static final String NEW_ASSIGNMENT_DUEDAY = "new_assignment_dueday";

	private static final String NEW_ASSIGNMENT_DUEYEAR = "new_assignment_dueyear";

	private static final String NEW_ASSIGNMENT_DUEHOUR = "new_assignment_duehour";

	private static final String NEW_ASSIGNMENT_DUEMIN = "new_assignment_duemin";

	private static final String NEW_ASSIGNMENT_DUEAMPM = "new_assignment_dueampm";
	
	private static final String NEW_ASSIGNMENT_DUEDATE_CALENDAR_ASSIGNMENT_ID = "new_assignment_duedate_calendar_assignment_id";

	private static final String NEW_ASSIGNMENT_PAST_DUE_DATE = "new_assignment_past_due_date";
	
	// close date
	private static final String NEW_ASSIGNMENT_ENABLECLOSEDATE = "new_assignment_enableclosedate";

	private static final String NEW_ASSIGNMENT_CLOSEMONTH = "new_assignment_closemonth";

	private static final String NEW_ASSIGNMENT_CLOSEDAY = "new_assignment_closeday";

	private static final String NEW_ASSIGNMENT_CLOSEYEAR = "new_assignment_closeyear";

	private static final String NEW_ASSIGNMENT_CLOSEHOUR = "new_assignment_closehour";

	private static final String NEW_ASSIGNMENT_CLOSEMIN = "new_assignment_closemin";

	private static final String NEW_ASSIGNMENT_CLOSEAMPM = "new_assignment_closeampm";

	private static final String NEW_ASSIGNMENT_ATTACHMENT = "new_assignment_attachment";

	private static final String NEW_ASSIGNMENT_SECTION = "new_assignment_section";

	private static final String NEW_ASSIGNMENT_SUBMISSION_TYPE = "new_assignment_submission_type";

	private static final String NEW_ASSIGNMENT_GRADE_TYPE = "new_assignment_grade_type";

	private static final String NEW_ASSIGNMENT_GRADE_POINTS = "new_assignment_grade_points";

	private static final String NEW_ASSIGNMENT_DESCRIPTION = "new_assignment_instructions";

	private static final String NEW_ASSIGNMENT_DUE_DATE_SCHEDULED = "new_assignment_due_date_scheduled";

	private static final String NEW_ASSIGNMENT_OPEN_DATE_ANNOUNCED = "new_assignment_open_date_announced";

	private static final String NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE = "new_assignment_check_add_honor_pledge";

	private static final String NEW_ASSIGNMENT_HIDE_OPTION_FLAG = "new_assignment_hide_option_flag";

	private static final String NEW_ASSIGNMENT_FOCUS = "new_assignment_focus";

	private static final String NEW_ASSIGNMENT_DESCRIPTION_EMPTY = "new_assignment_description_empty";

	private static final String NEW_ASSIGNMENT_ADD_TO_GRADEBOOK = "new_assignment_add_to_gradebook";

	private static final String NEW_ASSIGNMENT_RANGE = "new_assignment_range";

	private static final String NEW_ASSIGNMENT_GROUPS = "new_assignment_groups";
	
	private static final String NEW_ASSIGNMENT_PAST_CLOSE_DATE = "new_assignment_past_close_date";
	
	/**************************** assignment year range *************************/
	private static final String NEW_ASSIGNMENT_YEAR_RANGE_FROM = "new_assignment_year_range_from";
	private static final String NEW_ASSIGNMENT_YEAR_RANGE_TO = "new_assignment_year_range_to";
	
	// submission level of resubmit due time
	private static final String ALLOW_RESUBMIT_CLOSEMONTH = "allow_resubmit_closeMonth";
	private static final String ALLOW_RESUBMIT_CLOSEDAY = "allow_resubmit_closeDay";
	private static final String ALLOW_RESUBMIT_CLOSEYEAR = "allow_resubmit_closeYear";
	private static final String ALLOW_RESUBMIT_CLOSEHOUR = "allow_resubmit_closeHour";
	private static final String ALLOW_RESUBMIT_CLOSEMIN = "allow_resubmit_closeMin";
	private static final String ALLOW_RESUBMIT_CLOSEAMPM = "allow_resubmit_closeAMPM";
	
	private static final String ATTACHMENTS_MODIFIED = "attachments_modified";

	/** **************************** instructor's view student submission ***************** */
	// the show/hide table based on member id
	private static final String STUDENT_LIST_SHOW_TABLE = "STUDENT_LIST_SHOW_TABLE";

	/** **************************** student view grade submission id *********** */
	private static final String VIEW_GRADE_SUBMISSION_ID = "view_grade_submission_id";
	
	// alert for grade exceeds max grade setting
	private static final String GRADE_GREATER_THAN_MAX_ALERT = "grade_greater_than_max_alert";

	/** **************************** modes *************************** */
	/** The list view of assignments */
   private static final String MODE_LIST_ASSIGNMENTS = "lisofass1"; // set in velocity template

	/** The student view of an assignment submission */
	private static final String MODE_STUDENT_VIEW_SUBMISSION = "AssignmentConstants.mode_view_submission";
	
	/** The student view of an assignment submission confirmation */
	private static final String MODE_STUDENT_VIEW_SUBMISSION_CONFIRMATION = "AssignmentConstants.mode_view_submission_confirmation";

	/** The student preview of an assignment submission */
	private static final String MODE_STUDENT_PREVIEW_SUBMISSION = "AssignmentConstants.mode_student_preview_submission";

	/** The student view of graded submission */
	private static final String MODE_STUDENT_VIEW_GRADE = "AssignmentConstants.mode_student_view_grade";

	/** The student view of assignments */
	private static final String MODE_STUDENT_VIEW_ASSIGNMENT = "AssignmentConstants.mode_student_view_assignment";

	/** The instructor view of creating a new assignment or editing an existing one */
	private static final String MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT = "AssignmentConstants.mode_instructor_new_edit_assignment";
	
	/** The instructor view to reorder assignments */
	private static final String MODE_INSTRUCTOR_REORDER_ASSIGNMENT = "reorder";

	/** The instructor view to delete an assignment */
	private static final String MODE_INSTRUCTOR_DELETE_ASSIGNMENT = "AssignmentConstants.mode_instructor_delete_assignment";

	/** The instructor view to grade an assignment */
	private static final String MODE_INSTRUCTOR_GRADE_ASSIGNMENT = "AssignmentConstants.mode_instructor_grade_assignment";

	/** The instructor view to grade a submission */
	private static final String MODE_INSTRUCTOR_GRADE_SUBMISSION = "AssignmentConstants.mode_instructor_grade_submission";

	/** The instructor view of preview grading a submission */
	private static final String MODE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION = "AssignmentConstants.mode_instructor_preview_grade_submission";

	/** The instructor preview of one assignment */
	private static final String MODE_INSTRUCTOR_PREVIEW_ASSIGNMENT = "AssignmentConstants.mode_instructor_preview_assignments";

	/** The instructor view of one assignment */
	private static final String MODE_INSTRUCTOR_VIEW_ASSIGNMENT = "AssignmentConstants.mode_instructor_view_assignments";

	/** The instructor view to list students of an assignment */
	private static final String MODE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT = "lisofass2"; // set in velocity template

	/** The instructor view of assignment submission report */
	private static final String MODE_INSTRUCTOR_REPORT_SUBMISSIONS = "grarep"; // set in velocity template
	
	/** The instructor view of uploading all from archive file */
	private static final String MODE_INSTRUCTOR_UPLOAD_ALL = "uploadAll"; 

	/** The student view of assignment submission report */
	private static final String MODE_STUDENT_VIEW = "stuvie"; // set in velocity template

	/** ************************* vm names ************************** */
	/** The list view of assignments */
	private static final String TEMPLATE_LIST_ASSIGNMENTS = "_list_assignments";

	/** The student view of assignment */
	private static final String TEMPLATE_STUDENT_VIEW_ASSIGNMENT = "_student_view_assignment";

	/** The student view of showing an assignment submission */
	private static final String TEMPLATE_STUDENT_VIEW_SUBMISSION = "_student_view_submission";
	
	/** The student view of an assignment submission confirmation */
	private static final String TEMPLATE_STUDENT_VIEW_SUBMISSION_CONFIRMATION = "_student_view_submission_confirmation";

	/** The student preview an assignment submission */
	private static final String TEMPLATE_STUDENT_PREVIEW_SUBMISSION = "_student_preview_submission";

	/** The student view of graded submission */
	private static final String TEMPLATE_STUDENT_VIEW_GRADE = "_student_view_grade";

	/** The instructor view to create a new assignment or edit an existing one */
	private static final String TEMPLATE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT = "_instructor_new_edit_assignment";
	
	/** The instructor view to reorder the default assignments */
	private static final String TEMPLATE_INSTRUCTOR_REORDER_ASSIGNMENT = "_instructor_reorder_assignment";

	/** The instructor view to edit assignment */
	private static final String TEMPLATE_INSTRUCTOR_DELETE_ASSIGNMENT = "_instructor_delete_assignment";

	/** The instructor view to edit assignment */
	private static final String TEMPLATE_INSTRUCTOR_GRADE_SUBMISSION = "_instructor_grading_submission";

	/** The instructor preview to edit assignment */
	private static final String TEMPLATE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION = "_instructor_preview_grading_submission";

	/** The instructor view to grade the assignment */
	private static final String TEMPLATE_INSTRUCTOR_GRADE_ASSIGNMENT = "_instructor_list_submissions";

	/** The instructor preview of assignment */
	private static final String TEMPLATE_INSTRUCTOR_PREVIEW_ASSIGNMENT = "_instructor_preview_assignment";

	/** The instructor view of assignment */
	private static final String TEMPLATE_INSTRUCTOR_VIEW_ASSIGNMENT = "_instructor_view_assignment";

	/** The instructor view to edit assignment */
	private static final String TEMPLATE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT = "_instructor_student_list_submissions";

	/** The instructor view to assignment submission report */
	private static final String TEMPLATE_INSTRUCTOR_REPORT_SUBMISSIONS = "_instructor_report_submissions";

	/** The instructor view to upload all information from archive file */
	private static final String TEMPLATE_INSTRUCTOR_UPLOAD_ALL = "_instructor_uploadAll";

	/** The opening mark comment */
	private static final String COMMENT_OPEN = "{{";

	/** The closing mark for comment */
	private static final String COMMENT_CLOSE = "}}";

	/** The selected view */
	private static final String STATE_SELECTED_VIEW = "state_selected_view";

	/** The configuration choice of with grading option or not */
	private static final String WITH_GRADES = "with_grades";

	/** The alert flag when doing global navigation from improper mode */
	private static final String ALERT_GLOBAL_NAVIGATION = "alert_global_navigation";

	/** The total list item before paging */
	private static final String STATE_PAGEING_TOTAL_ITEMS = "state_paging_total_items";

	/** is current user allowed to grade assignment? */
	private static final String STATE_ALLOW_GRADE_SUBMISSION = "state_allow_grade_submission";

	/** property for previous feedback attachments **/
	private static final String PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS = "prop_submission_previous_feedback_attachments";
	
	/** the user and submission list for list of submissions page */
	private static final String USER_SUBMISSIONS = "user_submissions";
	
	/** ************************* Taggable constants ************************** */
	/** identifier of tagging provider that will provide the appropriate helper */
	private static final String PROVIDER_ID = "providerId";

	/** Reference to an activity */
	private static final String ACTIVITY_REF = "activityRef";
	
	/** Reference to an item */
	private static final String ITEM_REF = "itemRef";
	
	/** session attribute for list of decorated tagging providers */
	private static final String PROVIDER_LIST = "providerList";
	
	// whether the choice of emails instructor submission notification is available in the installation
	private static final String ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS = "assignment.instructor.notifications";
	
	// default for whether or how the instructor receive submission notification emails, none(default)|each|digest
	private static final String ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DEFAULT = "assignment.instructor.notifications.default";
	
	/****************************** Upload all screen ***************************/
	private static final String UPLOAD_ALL_HAS_SUBMISSION_TEXT = "upload_all_has_submission_text";
	private static final String UPLOAD_ALL_HAS_SUBMISSION_ATTACHMENT = "upload_all_has_submission_attachment";
	private static final String UPLOAD_ALL_HAS_GRADEFILE = "upload_all_has_gradefile";
	private static final String UPLOAD_ALL_HAS_COMMENTS= "upload_all_has_comments";
	private static final String UPLOAD_ALL_HAS_FEEDBACK_TEXT= "upload_all_has_feedback_text";
	private static final String UPLOAD_ALL_HAS_FEEDBACK_ATTACHMENT = "upload_all_has_feedback_attachment";
	private static final String UPLOAD_ALL_RELEASE_GRADES = "upload_all_release_grades";
	
	// this is to track whether the site has multiple assignment, hence if true, show the reorder link
	private static final String HAS_MULTIPLE_ASSIGNMENTS = "has_multiple_assignments";
	
	// view all or grouped submission list
	private static final String VIEW_SUBMISSION_LIST_OPTION = "view_submission_list_option";
	
	private ContentHostingService m_contentHostingService = null;
	
	private AssignmentService assignmentService = (AssignmentService) ComponentManager.get("org.sakaiproject.assignment.api.AssignmentService");
	
	/**
	 * central place for dispatching the build routines based on the state name
	 */
	public String buildMainPanelContext(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		String template = null;

		context.put("tlang", rb);

		context.put("cheffeedbackhelper", this);

		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);

		// allow add assignment?
		boolean allowAddAssignment = assignmentService.allowAddAssignment(contextString);
		context.put("allowAddAssignment", Boolean.valueOf(allowAddAssignment));

		Object allowGradeSubmission = state.getAttribute(STATE_ALLOW_GRADE_SUBMISSION);

		// allow update site?
		context.put("allowUpdateSite", Boolean
						.valueOf(SiteService.allowUpdateSite((String) state.getAttribute(STATE_CONTEXT_STRING))));
		
		// allow all.groups?
		boolean allowAllGroups = assignmentService.allowAllGroups(contextString);
		context.put("allowAllGroups", Boolean.valueOf(allowAllGroups));
		
		//Is the review service allowed?
		Site s = null;
		try {
		 s = SiteService.getSite((String) state.getAttribute(STATE_CONTEXT_STRING));
		}
		catch (IdUnusedException iue) {
			log.warn(this + ":BuildMainPanelContext: Site not found!");
		}
		getContentReviewService();
		if (allowReviewService && contentReviewService.isSiteAcceptable(s)) {
			context.put("allowReviewService", allowReviewService);
		} else {
			context.put("allowReviewService", false);
		}

		// grading option
		context.put("withGrade", state.getAttribute(WITH_GRADES));
		
		// the grade type table
		context.put("gradeTypeTable", gradeTypeTable());

		String mode = (String) state.getAttribute(STATE_MODE);

		if (!mode.equals(MODE_LIST_ASSIGNMENTS))
		{
			// allow grade assignment?
			if (state.getAttribute(STATE_ALLOW_GRADE_SUBMISSION) == null)
			{
				state.setAttribute(STATE_ALLOW_GRADE_SUBMISSION, Boolean.FALSE);
			}
			context.put("allowGradeSubmission", state.getAttribute(STATE_ALLOW_GRADE_SUBMISSION));
		}

		if (mode.equals(MODE_LIST_ASSIGNMENTS))
		{
			// build the context for the student assignment view
			template = build_list_assignments_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_STUDENT_VIEW_ASSIGNMENT))
		{
			// the student view of assignment
			template = build_student_view_assignment_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_STUDENT_VIEW_SUBMISSION))
		{
			// disable auto-updates while leaving the list view
			justDelivered(state);

			// build the context for showing one assignment submission
			template = build_student_view_submission_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_STUDENT_VIEW_SUBMISSION_CONFIRMATION))
		{
			// build the context for showing one assignment submission confirmation
			template = build_student_view_submission_confirmation_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_STUDENT_PREVIEW_SUBMISSION))
		{
			// build the context for showing one assignment submission
			template = build_student_preview_submission_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_STUDENT_VIEW_GRADE))
		{
			// disable auto-updates while leaving the list view
			justDelivered(state);

			// build the context for showing one graded submission
			template = build_student_view_grade_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT))
		{
			// allow add assignment?
			boolean allowAddSiteAssignment = assignmentService.allowAddSiteAssignment(contextString);
			context.put("allowAddSiteAssignment", Boolean.valueOf(allowAddSiteAssignment));

			// disable auto-updates while leaving the list view
			justDelivered(state);

			// build the context for the instructor's create new assignment view
			template = build_instructor_new_edit_assignment_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_INSTRUCTOR_DELETE_ASSIGNMENT))
		{
			if (state.getAttribute(DELETE_ASSIGNMENT_IDS) != null)
			{
				// disable auto-updates while leaving the list view
				justDelivered(state);

				// build the context for the instructor's delete assignment
				template = build_instructor_delete_assignment_context(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_GRADE_ASSIGNMENT))
		{
			if (allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, build the context for the instructor's grade assignment
				template = build_instructor_grade_assignment_context(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_GRADE_SUBMISSION))
		{
			if (allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, disable auto-updates while leaving the list view
				justDelivered(state);

				// build the context for the instructor's grade submission
				template = build_instructor_grade_submission_context(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION))
		{
			if ( allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, build the context for the instructor's preview grade submission
				template = build_instructor_preview_grade_submission_context(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_PREVIEW_ASSIGNMENT))
		{
			// build the context for preview one assignment
			template = build_instructor_preview_assignment_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_INSTRUCTOR_VIEW_ASSIGNMENT))
		{
			// disable auto-updates while leaving the list view
			justDelivered(state);

			// build the context for view one assignment
			template = build_instructor_view_assignment_context(portlet, context, data, state);
		}
		else if (mode.equals(MODE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT))
		{
			if ( allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, build the context for the instructor's create new assignment view
				template = build_instructor_view_students_assignment_context(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_REPORT_SUBMISSIONS))
		{
			if ( allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, build the context for the instructor's view of report submissions
				template = build_instructor_report_submissions(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_UPLOAD_ALL))
		{
			if ( allowGradeSubmission != null && ((Boolean) allowGradeSubmission).booleanValue())
			{
				// if allowed for grading, build the context for the instructor's view of uploading all info from archive file
				template = build_instructor_upload_all(portlet, context, data, state);
			}
		}
		else if (mode.equals(MODE_INSTRUCTOR_REORDER_ASSIGNMENT))
		{
			// disable auto-updates while leaving the list view
			justDelivered(state);

			// build the context for the instructor's create new assignment view
			template = build_instructor_reorder_assignment_context(portlet, context, data, state);
		}

		if (template == null)
		{
			// default to student list view
			state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
			template = build_list_assignments_context(portlet, context, data, state);
		}

		// this is a check for seeing if there are any assignments.  The check is used to see if we display a Reorder link in the vm files
		if (state.getAttribute(HAS_MULTIPLE_ASSIGNMENTS) != null)
		{
			context.put("assignmentscheck", state.getAttribute(HAS_MULTIPLE_ASSIGNMENTS));
		}
		
		return template;

	} // buildNormalContext

		
	/**
	 * build the student view of showing an assignment submission
	 */
	protected String build_student_view_submission_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		context.put("context", contextString);

		User user = (User) state.getAttribute(STATE_USER);
		String currentAssignmentReference = (String) state.getAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE);
		Assignment assignment = null;
		try
		{
			assignment = assignmentService.getAssignment(currentAssignmentReference);
			context.put("assignment", assignment);
			context.put("canSubmit", Boolean.valueOf(assignmentService.canSubmit(contextString, assignment)));
			if (assignment.getTypeOfSubmission() == AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
			{
				context.put("nonElectronicType", Boolean.TRUE);
			}
			AssignmentSubmission s = assignmentService.getSubmission(currentAssignmentReference, user);
			if (s != null)
			{
				context.put("submission", s);
				/*TODO:zqian
				 * ResourceProperties p = s.getProperties();
				if (p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT) != null)
				{
					context.put("prevFeedbackText", p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT));
				}

				if (p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT) != null)
				{
					context.put("prevFeedbackComment", p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT));
				}
				
				if (p.getProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS) != null)
				{
					context.put("prevFeedbackAttachments", getPrevFeedbackAttachments(p));
				}*/
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannot_find_assignment"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot16"));
		}

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.taggable.api.TaggingManager");
		if (taggingManager.isTaggable() && assignment != null)
		{
			addProviders(context, state);
			addActivity(context, assignment);
			context.put("taggable", Boolean.valueOf(true));
		}

		// name value pairs for the vm
		context.put("name_submission_text", VIEW_SUBMISSION_TEXT);
		context.put("value_submission_text", state.getAttribute(VIEW_SUBMISSION_TEXT));
		context.put("name_submission_honor_pledge_yes", VIEW_SUBMISSION_HONOR_PLEDGE_YES);
		context.put("value_submission_honor_pledge_yes", state.getAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES));
		context.put("attachments", state.getAttribute(ATTACHMENTS));
		
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		context.put("currentTime", TimeService.newTime());

		boolean allowSubmit = assignmentService.allowAddSubmission((String) state.getAttribute(STATE_CONTEXT_STRING));
		if (!allowSubmit)
		{
			addAlert(state, rb.getString("not_allowed_to_submit"));
		}
		context.put("allowSubmit", new Boolean(allowSubmit));

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_STUDENT_VIEW_SUBMISSION;

	} // build_student_view_submission_context

	/**
	 * build the student view of showing an assignment submission confirmation
	 */
	protected String build_student_view_submission_confirmation_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		context.put("context", contextString);
		
		// get user information
		User user = (User) state.getAttribute(STATE_USER);
		context.put("user_name", user.getDisplayName());
		context.put("user_id", user.getDisplayId());
		if (StringUtil.trimToNull(user.getEmail()) != null)
			context.put("user_email", user.getEmail());
		
		// get site information
		try
		{
			// get current site
			Site site = SiteService.getSite(contextString);
			context.put("site_title", site.getTitle());
		}
		catch (Exception ignore)
		{
			log.warn(this + ignore.getMessage() + " siteId= " + contextString);
		}
		
		// get assignment and submission information
		String currentAssignmentReference = (String) state.getAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE);
		try
		{
			Assignment currentAssignment = assignmentService.getAssignment(currentAssignmentReference);
			context.put("assignment_title", currentAssignment.getTitle());
			AssignmentSubmission s = assignmentService.getSubmission(currentAssignmentReference, user);
			if (s != null)
			{
				context.put("submitted", Boolean.valueOf(!s.getCurrentSubmissionVersion().isDraft()));
				context.put("submission_id", s.getId());
				if (s.getCurrentSubmissionVersion().getTimeSubmitted() != null)
				{
					context.put("submit_time", s.getCurrentSubmissionVersion().getTimeSubmitted().toString());
				}
				List attachments = s.getCurrentSubmissionVersion().getSubmittedAttachments();
				if (attachments != null && attachments.size()>0)
				{
					context.put("submit_attachments", s.getCurrentSubmissionVersion().getSubmittedAttachments());
				}
				context.put("submit_text", StringUtil.trimToNull(s.getCurrentSubmissionVersion().getSubmittedText()));
				context.put("email_confirmation", Boolean.valueOf(ServerConfigurationService.getBoolean("assignment.submission.confirmation.email", true)));
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannot_find_assignment"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot16"));
		}	

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_STUDENT_VIEW_SUBMISSION_CONFIRMATION;

	} // build_student_view_submission_confirmation_context
	
	/**
	 * build the student view of assignment
	 */
	protected String build_student_view_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		context.put("context", state.getAttribute(STATE_CONTEXT_STRING));

		String aId = (String) state.getAttribute(VIEW_ASSIGNMENT_ID);

		Assignment assignment = null;
		
		try
		{
			assignment = assignmentService.getAssignment(aId);
			context.put("assignment", assignment);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannot_find_assignment"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.taggable.api.TaggingManager");
		if (taggingManager.isTaggable() && assignment != null)
		{
			addProviders(context, state);
			addActivity(context, assignment);
			context.put("taggable", Boolean.valueOf(true));
		}

		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		context.put("userDirectoryService", UserDirectoryService.getInstance());

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_STUDENT_VIEW_ASSIGNMENT;

	} // build_student_view_submission_context

	/**
	 * build the student preview of showing an assignment submission
	 */
	protected String build_student_preview_submission_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		User user = (User) state.getAttribute(STATE_USER);
		String aReference = (String) state.getAttribute(PREVIEW_SUBMISSION_ASSIGNMENT_REFERENCE);

		try
		{
			context.put("assignment", assignmentService.getAssignment(aReference));
			context.put("submission", assignmentService.getSubmission(aReference, user));
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot16"));
		}

		context.put("text", state.getAttribute(PREVIEW_SUBMISSION_TEXT));
		context.put("honor_pledge_yes", state.getAttribute(PREVIEW_SUBMISSION_HONOR_PLEDGE_YES));
		context.put("attachments", state.getAttribute(PREVIEW_SUBMISSION_ATTACHMENTS));
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_STUDENT_PREVIEW_SUBMISSION;

	} // build_student_preview_submission_context

	/**
	 * build the student view of showing a graded submission
	 */
	protected String build_student_view_grade_context(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));

		AssignmentSubmission submission = null;
		try
		{
			submission = assignmentService.getSubmission((String) state.getAttribute(VIEW_GRADE_SUBMISSION_ID));
			Assignment assignment = submission.getAssignment();
			context.put("assignment", assignment);
			if (assignment.getTypeOfSubmission() == AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
			{
				context.put("nonElectronicType", Boolean.TRUE);
			}
			context.put("submission", submission);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_get_submission"));
		}

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.taggable.api.TaggingManager");
		if (taggingManager.isTaggable() && submission != null)
		{
			AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
					.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");
			List<DecoratedTaggingProvider> providers = addProviders(context, state);
			List<TaggingHelperInfo> itemHelpers = new ArrayList<TaggingHelperInfo>();
			for (DecoratedTaggingProvider provider : providers)
			{
				TaggingHelperInfo helper = provider.getProvider()
						.getItemHelperInfo(
								assignmentActivityProducer.getItem(
										submission,
										UserDirectoryService.getCurrentUser()
												.getId()).getReference());
				if (helper != null)
				{
					itemHelpers.add(helper);
				}
			}
			addItem(context, submission, UserDirectoryService.getCurrentUser().getId());
			addActivity(context, submission.getAssignment());
			context.put("itemHelpers", itemHelpers);
			context.put("taggable", Boolean.valueOf(true));
		}

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_STUDENT_VIEW_GRADE;

	} // build_student_view_grade_context

	/**
	 * build the view of assignments list
	 */
	protected String build_list_assignments_context(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.taggable.api.TaggingManager");
		if (taggingManager.isTaggable())
		{
			context.put("producer", ComponentManager
					.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer"));
			context.put("providers", taggingManager.getProviders());
			context.put("taggable", Boolean.valueOf(true));
		}
		
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		context.put("contextString", contextString);
		context.put("user", state.getAttribute(STATE_USER));
		context.put("service", assignmentService);
		context.put("TimeService", TimeService.getInstance());
		context.put("LongObject", new Long(TimeService.newTime().getTime()));
		context.put("currentTime", TimeService.newTime());
		String sortedBy = (String) state.getAttribute(SORTED_BY);
		String sortedAsc = (String) state.getAttribute(SORTED_ASC);
		// clean sort criteria
		if (sortedBy.equals(SORTED_BY_GROUP_TITLE) || sortedBy.equals(SORTED_BY_GROUP_DESCRIPTION))
		{
			sortedBy = SORTED_BY_DUEDATE;
			sortedAsc = Boolean.TRUE.toString();
			state.setAttribute(SORTED_BY, sortedBy);
			state.setAttribute(SORTED_ASC, sortedAsc);
		}
		context.put("sortedBy", sortedBy);
		context.put("sortedAsc", sortedAsc);
		if (state.getAttribute(STATE_SELECTED_VIEW) != null)
		{
			context.put("view", state.getAttribute(STATE_SELECTED_VIEW));
		}

		Hashtable assignments_submissions = new Hashtable();
		List assignments = prepPage(state);

		context.put("assignments", assignments.iterator());
	
		// allow get assignment
		context.put("allowGetAssignment", Boolean.valueOf(assignmentService.allowGetAssignment(contextString)));
		
		// test whether user user can grade at least one assignment
		// and update the state variable.
		boolean allowGradeSubmission = false;
		for (Iterator aIterator=assignments.iterator(); !allowGradeSubmission && aIterator.hasNext(); )
		{
			if (assignmentService.allowGradeSubmission(((Assignment) aIterator.next()).getReference()))
			{
				allowGradeSubmission = true;
			}
		}
		state.setAttribute(STATE_ALLOW_GRADE_SUBMISSION, new Boolean(allowGradeSubmission));
		context.put("allowGradeSubmission", state.getAttribute(STATE_ALLOW_GRADE_SUBMISSION));

		// allow remove assignment?
		boolean allowRemoveAssignment = false;
		for (Iterator aIterator=assignments.iterator(); !allowRemoveAssignment && aIterator.hasNext(); )
		{
			if (assignmentService.allowRemoveAssignment(((Assignment) aIterator.next()).getReference()))
			{
				allowRemoveAssignment = true;
			}
		}
		context.put("allowRemoveAssignment", Boolean.valueOf(allowRemoveAssignment));

		add2ndToolbarFields(data, context);

		// inform the observing courier that we just updated the page...
		// if there are pending requests to do so they can be cleared
		justDelivered(state);

		pagingInfoToContext(state, context);

		// put site object into context
		try
		{
			// get current site
			Site site = SiteService.getSite(contextString);
			context.put("site", site);
			// any group in the site?
			Collection groups = site.getGroups();
			context.put("groups", (groups != null && groups.size()>0)?Boolean.TRUE:Boolean.FALSE);

			// add active user list
			AuthzGroup realm = AuthzGroupService.getAuthzGroup(SiteService.siteReference(contextString));
			if (realm != null)
			{
				context.put("activeUserIds", realm.getUsers());
			}
		}
		catch (Exception ignore)
		{
			log.warn(this + ignore.getMessage() + " siteId= " + contextString);
		}

		boolean allowSubmit = assignmentService.allowAddSubmission(contextString);
		context.put("allowSubmit", new Boolean(allowSubmit));
		
		// related to resubmit settings
		context.put("allowResubmitNumberProp", AssignmentConstants.ALLOW_RESUBMIT_NUMBER);
		context.put("allowResubmitCloseTimeProp", AssignmentConstants.ALLOW_RESUBMIT_CLOSETIME);
		
		// the type int for non-electronic submission
		context.put("typeNonElectronic", Integer.valueOf(AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION));

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_LIST_ASSIGNMENTS;

	} // build_list_assignments_context
	
	private HashSet<String> getSubmittersIdSet(List submissions)
	{
		HashSet<String> rv = new HashSet<String>();
		for (Iterator iSubmissions=submissions.iterator(); iSubmissions.hasNext();)
		{
			rv.add(((AssignmentSubmission) iSubmissions.next()).getSubmitterId());
		}
		return rv;
	}
	
	private HashSet<String> getAllowAddSubmissionUsersIdSet(List users)
	{
		HashSet<String> rv = new HashSet<String>();
		for (Iterator iUsers=users.iterator(); iUsers.hasNext();)
		{
			rv.add(((User) iUsers.next()).getId());
		}
		return rv;
	}

	/**
	 * build the instructor view of creating a new assignment or editing an existing one
	 */
	protected String build_instructor_new_edit_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		// is the assignment an new assignment
		String assignmentId = (String) state.getAttribute(EDIT_ASSIGNMENT_ID);
		if (assignmentId != null)
		{
			try
			{
				Assignment a = assignmentService.getAssignment(assignmentId);
				context.put("assignment", a);
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin3") + ": " + assignmentId);
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot14") + ": " + assignmentId);
			}
		}

		// set up context variables
		setAssignmentFormContext(state, context);

		context.put("fField", state.getAttribute(NEW_ASSIGNMENT_FOCUS));

		String sortedBy = (String) state.getAttribute(SORTED_BY);
		String sortedAsc = (String) state.getAttribute(SORTED_ASC);
		context.put("sortedBy", sortedBy);
		context.put("sortedAsc", sortedAsc);

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT;

	} // build_instructor_new_assignment_context

	protected void setAssignmentFormContext(SessionState state, Context context)
	{
		// put the names and values into vm file
		
		
		context.put("name_UseReviewService", NEW_ASSIGNMENT_USE_REVIEW_SERVICE);
		context.put("name_AllowStudentView", NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW);
		
		context.put("name_title", NEW_ASSIGNMENT_TITLE);
		context.put("name_order", NEW_ASSIGNMENT_ORDER);

		// set open time context variables
		putTimePropertiesInContext(context, state, "Open", NEW_ASSIGNMENT_OPENMONTH, NEW_ASSIGNMENT_OPENDAY, NEW_ASSIGNMENT_OPENYEAR, NEW_ASSIGNMENT_OPENHOUR, NEW_ASSIGNMENT_OPENMIN, NEW_ASSIGNMENT_OPENAMPM);
		
		// set due time context variables
		putTimePropertiesInContext(context, state, "Due", NEW_ASSIGNMENT_DUEMONTH, NEW_ASSIGNMENT_DUEDAY, NEW_ASSIGNMENT_DUEYEAR, NEW_ASSIGNMENT_DUEHOUR, NEW_ASSIGNMENT_DUEMIN, NEW_ASSIGNMENT_DUEAMPM);

		context.put("name_EnableCloseDate", NEW_ASSIGNMENT_ENABLECLOSEDATE);
		// set close time context variables
		putTimePropertiesInContext(context, state, "Close", NEW_ASSIGNMENT_CLOSEMONTH, NEW_ASSIGNMENT_CLOSEDAY, NEW_ASSIGNMENT_CLOSEYEAR, NEW_ASSIGNMENT_CLOSEHOUR, NEW_ASSIGNMENT_CLOSEMIN, NEW_ASSIGNMENT_CLOSEAMPM);

		context.put("name_Section", NEW_ASSIGNMENT_SECTION);
		context.put("name_SubmissionType", NEW_ASSIGNMENT_SUBMISSION_TYPE);
		context.put("name_GradeType", NEW_ASSIGNMENT_GRADE_TYPE);
		context.put("name_GradePoints", NEW_ASSIGNMENT_GRADE_POINTS);
		context.put("name_Description", NEW_ASSIGNMENT_DESCRIPTION);
		// do not show the choice when there is no Schedule tool yet
		if (state.getAttribute(CALENDAR) != null)
			context.put("name_CheckAddDueDate", ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE);
		//don't show the choice when there is no Announcement tool yet
		if (state.getAttribute(ANNOUNCEMENT_CHANNEL) != null)
			context.put("name_CheckAutoAnnounce", ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE);
		context.put("name_CheckAddHonorPledge", NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE);
		
		// number of resubmissions allowed
		context.put("name_allowResubmitNumber", AssignmentConstants.ALLOW_RESUBMIT_NUMBER);
		
		// set the values
		context.put("value_year_from", state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_FROM));
		context.put("value_year_to", state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_TO));
		context.put("value_title", state.getAttribute(NEW_ASSIGNMENT_TITLE));
		context.put("value_position_order", state.getAttribute(NEW_ASSIGNMENT_ORDER));

		context.put("value_EnableCloseDate", state.getAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE));

		context.put("value_Sections", state.getAttribute(NEW_ASSIGNMENT_SECTION));
		context.put("value_SubmissionType", state.getAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE));
		context.put("value_totalSubmissionTypes", AssignmentConstants.SUBMISSION_TYPES.length);
		context.put("value_GradeType", state.getAttribute(NEW_ASSIGNMENT_GRADE_TYPE));
		// format to show one decimal place
		String maxGrade = (String) state.getAttribute(NEW_ASSIGNMENT_GRADE_POINTS);
		context.put("value_GradePoints", displayGrade(state, maxGrade));
		context.put("value_Description", state.getAttribute(NEW_ASSIGNMENT_DESCRIPTION));
		
		
		// Keep the use review service setting
		context.put("value_UseReviewService", state.getAttribute(NEW_ASSIGNMENT_USE_REVIEW_SERVICE));
		context.put("value_AllowStudentView", state.getAttribute(NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW));
		
		// don't show the choice when there is no Schedule tool yet
		if (state.getAttribute(CALENDAR) != null)
		context.put("value_CheckAddDueDate", state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE));
		
		// don't show the choice when there is no Announcement tool yet
		if (state.getAttribute(ANNOUNCEMENT_CHANNEL) != null)
				context.put("value_CheckAutoAnnounce", state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE));
		
		String s = (String) state.getAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE);
		if (s == null) s = "1";
		context.put("value_CheckAddHonorPledge", s);
		// number of resubmissions allowed
		if (state.getAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER) != null)
		{
			context.put("value_allowResubmitNumber", Integer.valueOf((String) state.getAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER)));
		}
		else
		{
			// defaults to 0
			context.put("value_allowResubmitNumber", Integer.valueOf(0));
		}

		// get all available assignments from Gradebook tool except for those created from
		boolean gradebookExists = isGradebookDefined();
		if (gradebookExists)
		{
			GradebookService g = (GradebookService) (org.sakaiproject.service.gradebook.shared.GradebookService) ComponentManager.get("org.sakaiproject.service.gradebook.GradebookService");
			String gradebookUid = ToolManager.getInstance().getCurrentPlacement().getContext();

			try
			{
				// get all assignments in Gradebook
				List gradebookAssignments = g.getAssignments(gradebookUid);
				List gradebookAssignmentsExceptSamigo = new Vector();
	
				// filtering out those from Samigo
				for (Iterator i=gradebookAssignments.iterator(); i.hasNext();)
				{
					org.sakaiproject.service.gradebook.shared.Assignment gAssignment = (org.sakaiproject.service.gradebook.shared.Assignment) i.next();
					if (!gAssignment.isExternallyMaintained() || gAssignment.isExternallyMaintained() && gAssignment.getExternalAppName().equals(getToolTitle()))
					{
						gradebookAssignmentsExceptSamigo.add(gAssignment);
					}
				}
				context.put("gradebookAssignments", gradebookAssignmentsExceptSamigo);
				if (StringUtil.trimToNull((String) state.getAttribute(assignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK)) == null)
				{
					state.setAttribute(assignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, assignmentService.GRADEBOOK_INTEGRATION_NO);
				}
				
				context.put("withGradebook", Boolean.TRUE);
				
				// offer the gradebook integration choice only in the Assignments with Grading tool
				boolean withGrade = ((Boolean) state.getAttribute(WITH_GRADES)).booleanValue();
				if (withGrade)
				{
					context.put("name_Addtogradebook", assignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK);
					context.put("name_AssociateGradebookAssignment", assignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);
				}
				
				context.put("gradebookChoice", state.getAttribute(assignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK));
				context.put("gradebookChoice_no", assignmentService.GRADEBOOK_INTEGRATION_NO);
				context.put("gradebookChoice_add", assignmentService.GRADEBOOK_INTEGRATION_ADD);
				context.put("gradebookChoice_associate", assignmentService.GRADEBOOK_INTEGRATION_ASSOCIATE);
				String associateGradebookAssignment = (String) state.getAttribute(assignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);
				if (associateGradebookAssignment != null)
				{
					context.put("associateGradebookAssignment", associateGradebookAssignment);
					String assignmentId = (String) state.getAttribute(EDIT_ASSIGNMENT_ID);
					if (assignmentId != null)
					{
						try
						{
							Assignment a = assignmentService.getAssignment(assignmentId);
							context.put("noAddToGradebookChoice", Boolean.valueOf(associateGradebookAssignment.equals(a.getReference())));
						}
						catch (Exception ee)
						{
							// ignore
						}
					}
				}
			}
			catch (Exception e)
			{
				// not able to link to Gradebook
				log.warn(this + e.getMessage());
			}
			
			if (StringUtil.trimToNull((String) state.getAttribute(NEW_ASSIGNMENT_ADD_TO_GRADEBOOK)) == null)
			{
				state.setAttribute(NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, assignmentService.GRADEBOOK_INTEGRATION_NO);
			}
		}

		context.put("monthTable", monthTable());
		context.put("submissionTypeTable", submissionTypeTable());
		context.put("hide_assignment_option_flag", state.getAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG));
		context.put("attachments", state.getAttribute(ATTACHMENTS));
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));

		String range = StringUtil.trimToNull((String) state.getAttribute(NEW_ASSIGNMENT_RANGE));
		if (range != null)
		{
			context.put("range", range);
		}
		
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		// put site object into context
		try
		{
			// get current site
			Site site = SiteService.getSite((String) state.getAttribute(STATE_CONTEXT_STRING));
			context.put("site", site);
		}
		catch (Exception ignore)
		{
		}

		if (assignmentService.getAllowGroupAssignments())
		{
			Collection groupsAllowAddAssignment = assignmentService.getGroupsAllowAddAssignment(contextString);
			
			if (range == null)
			{
				if (assignmentService.allowAddSiteAssignment(contextString))
				{
					// default to make site selection
					context.put("range", "site");
				}
				else if (groupsAllowAddAssignment.size() > 0)
				{
					// to group otherwise
					context.put("range", "groups");
				}
			}
			
			// group list which user can add message to
			if (groupsAllowAddAssignment.size() > 0)
			{
				String sort = (String) state.getAttribute(SORTED_BY);
				String asc = (String) state.getAttribute(SORTED_ASC);
				if (sort == null || (!sort.equals(SORTED_BY_GROUP_TITLE) && !sort.equals(SORTED_BY_GROUP_DESCRIPTION)))
				{
					sort = SORTED_BY_GROUP_TITLE;
					asc = Boolean.TRUE.toString();
					state.setAttribute(SORTED_BY, sort);
					state.setAttribute(SORTED_ASC, asc);
				}
				context.put("groups", new SortedIterator(groupsAllowAddAssignment.iterator(), new AssignmentComparator(state, sort, asc)));
				context.put("assignmentGroups", state.getAttribute(NEW_ASSIGNMENT_GROUPS));
			}
		}

		context.put("allowGroupAssignmentsInGradebook", new Boolean(assignmentService.getAllowGroupAssignmentsInGradebook()));

		// the notification email choices
		if (state.getAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS) != null && ((Boolean) state.getAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS)).booleanValue())
		{
			context.put("name_assignment_instructor_notifications", ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS);
			if (state.getAttribute(AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE) == null)
			{
				// set the notification value using site default
				state.setAttribute(AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE, state.getAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DEFAULT));
			}
			context.put("value_assignment_instructor_notifications", state.getAttribute(AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE));
			// the option values
			context.put("value_assignment_instructor_notifications_none", AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_NONE);
			context.put("value_assignment_instructor_notifications_each", AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_EACH);
			context.put("value_assignment_instructor_notifications_digest", AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DIGEST);
		}
	} // setAssignmentFormContext

	/**
	 * build the instructor view of create a new assignment
	 */
	protected String build_instructor_preview_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		context.put("time", TimeService.newTime());

		context.put("user", UserDirectoryService.getCurrentUser());

		context.put("value_Title", (String) state.getAttribute(NEW_ASSIGNMENT_TITLE));
		context.put("name_order", NEW_ASSIGNMENT_ORDER);
		context.put("value_position_order", (String) state.getAttribute(NEW_ASSIGNMENT_ORDER));

		Date openTime = getTime(state, NEW_ASSIGNMENT_OPENYEAR, NEW_ASSIGNMENT_OPENMONTH, NEW_ASSIGNMENT_OPENDAY, NEW_ASSIGNMENT_OPENHOUR, NEW_ASSIGNMENT_OPENMIN, NEW_ASSIGNMENT_OPENAMPM);
		context.put("value_OpenDate", openTime);

		// due time
		int dueMonth = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEMONTH)).intValue();
		int dueDay = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEDAY)).intValue();
		int dueYear = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEYEAR)).intValue();
		int dueHour = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEHOUR)).intValue();
		int dueMin = ((Integer) state.getAttribute(NEW_ASSIGNMENT_DUEMIN)).intValue();
		String dueAMPM = (String) state.getAttribute(NEW_ASSIGNMENT_DUEAMPM);
		if ((dueAMPM.equals("PM")) && (dueHour != 12))
		{
			dueHour = dueHour + 12;
		}
		if ((dueHour == 12) && (dueAMPM.equals("AM")))
		{
			dueHour = 0;
		}
		Time dueTime = TimeService.newTimeLocal(dueYear, dueMonth, dueDay, dueHour, dueMin, 0, 0);
		context.put("value_DueDate", dueTime);

		// close time
		Time closeTime = TimeService.newTime();
		Boolean enableCloseDate = (Boolean) state.getAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE);
		context.put("value_EnableCloseDate", enableCloseDate);
		if ((enableCloseDate).booleanValue())
		{
			int closeMonth = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEMONTH)).intValue();
			int closeDay = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEDAY)).intValue();
			int closeYear = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEYEAR)).intValue();
			int closeHour = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEHOUR)).intValue();
			int closeMin = ((Integer) state.getAttribute(NEW_ASSIGNMENT_CLOSEMIN)).intValue();
			String closeAMPM = (String) state.getAttribute(NEW_ASSIGNMENT_CLOSEAMPM);
			if ((closeAMPM.equals("PM")) && (closeHour != 12))
			{
				closeHour = closeHour + 12;
			}
			if ((closeHour == 12) && (closeAMPM.equals("AM")))
			{
				closeHour = 0;
			}
			closeTime = TimeService.newTimeLocal(closeYear, closeMonth, closeDay, closeHour, closeMin, 0, 0);
			context.put("value_CloseDate", closeTime);
		}

		context.put("value_Sections", state.getAttribute(NEW_ASSIGNMENT_SECTION));
		context.put("value_SubmissionType", state.getAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE));
		context.put("value_GradeType", state.getAttribute(NEW_ASSIGNMENT_GRADE_TYPE));
		String maxGrade = (String) state.getAttribute(NEW_ASSIGNMENT_GRADE_POINTS);
		context.put("value_GradePoints", displayGrade(state, maxGrade));
		context.put("value_Description", state.getAttribute(NEW_ASSIGNMENT_DESCRIPTION));
		context.put("value_CheckAddDueDate", state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE));
		context.put("value_CheckAutoAnnounce", state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE));
		context.put("value_CheckAddHonorPledge", state.getAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE));

		// get all available assignments from Gradebook tool except for those created from
		if (isGradebookDefined())
		{
			context.put("gradebookChoice", state.getAttribute(NEW_ASSIGNMENT_ADD_TO_GRADEBOOK));
			context.put("associateGradebookAssignment", state.getAttribute(assignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT));
		}

		context.put("monthTable", monthTable());
		context.put("submissionTypeTable", submissionTypeTable());
		context.put("hide_assignment_option_flag", state.getAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG));
		context.put("attachments", state.getAttribute(ATTACHMENTS));

		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));

		context.put("preview_assignment_assignment_hide_flag", state.getAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_HIDE_FLAG));
		context.put("preview_assignment_student_view_hide_flag", state.getAttribute(PREVIEW_ASSIGNMENT_STUDENT_VIEW_HIDE_FLAG));
		String assignmentId = StringUtil.trimToNull((String) state.getAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_ID));
		if (assignmentId != null)
		{
			// editing existing assignment
			context.put("value_assignment_id", assignmentId);
			try
			{
				Assignment a = assignmentService.getAssignment(assignmentId);
				context.put("isDraft", Boolean.valueOf(a.isDraft()));
			}
			catch (Exception e)
			{
				log.warn(this + e.getMessage() + assignmentId);
			}
		}
		else
		{
			// new assignment
			context.put("isDraft", Boolean.TRUE);
		}

		context.put("currentTime", TimeService.newTime());

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_PREVIEW_ASSIGNMENT;

	} // build_instructor_preview_assignment_context

	/**
	 * build the instructor view to delete an assignment
	 */
	protected String build_instructor_delete_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		Vector assignments = new Vector();
		Vector assignmentIds = (Vector) state.getAttribute(DELETE_ASSIGNMENT_IDS);
		for (int i = 0; i < assignmentIds.size(); i++)
		{
			try
			{
				Assignment a = assignmentService.getAssignment((String) assignmentIds.get(i));

				Iterator submissions = assignmentService.getSubmissions(a).iterator();
				if (submissions.hasNext())
				{
					// if there is submission to the assignment, show the alert
					addAlert(state, rb.getString("areyousur") + " \"" + a.getTitle() + "\" " + rb.getString("whihassub") + "\n");
				}
				assignments.add(a);
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin3"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot14"));
			}
		}
		context.put("assignments", assignments);
		context.put("service", assignmentService);
		context.put("currentTime", TimeService.newTime());

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_DELETE_ASSIGNMENT;

	} // build_instructor_delete_assignment_context

	/**
	 * build the instructor view to grade an submission
	 */
	protected String build_instructor_grade_submission_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		int gradeType = -1;

		// need to show the alert for grading drafts?
		boolean addGradeDraftAlert = false;
		
		// assignment
		Assignment a = null;
		try
		{
			a = assignmentService.getAssignment((String) state.getAttribute(GRADE_SUBMISSION_ASSIGNMENT_ID));
			context.put("assignment", a);
			gradeType = a.getTypeOfGrade();
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}

		// assignment submission
		try
		{
			AssignmentSubmission s = assignmentService.getSubmission((String) state.getAttribute(GRADE_SUBMISSION_SUBMISSION_ID));
			if (s != null)
			{
				context.put("submission", s);
				
				// show alert if student is working on a draft
				if (!s.getCurrentSubmissionVersion().isDraft() // not submitted
					&& ((s.getCurrentSubmissionVersion().getSubmittedText() != null && s.getCurrentSubmissionVersion().getSubmittedText().length()> 0) // has some text
						|| (s.getCurrentSubmissionVersion().getSubmittedAttachments() != null && s.getCurrentSubmissionVersion().getSubmittedAttachments().size() > 0))) // has some attachment
				{
					// TODO:zqian
					/*if (s.getCurrentSubmissionVersion().get.after(TimeService.newTime()))	 
					{
						// not pass the close date yet
						addGradeDraftAlert = true;
					}
					else
					{
						// passed the close date already
						addGradeDraftAlert = false;
					}*/
				}
			
				// TODO:zqian
				/*ResourceProperties p = s.getProperties();
				if (p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT) != null)
				{
					context.put("prevFeedbackText", p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT));
				}

				if (p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT) != null)
				{
					context.put("prevFeedbackComment", p.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT));
				}
				
				if (p.getProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS) != null)
				{
					context.put("prevFeedbackAttachments", getPrevFeedbackAttachments(p));
				}
				
				if (state.getAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER) != null)
				{
					context.put("value_allowResubmitNumber", Integer.valueOf((String) state.getAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER)));
					String allowResubmitTimeString =p.getProperty(AssignmentConstants.ALLOW_RESUBMIT_CLOSETIME);
					if (allowResubmitTimeString == null)
					{
						allowResubmitTimeString = (String) state.getAttribute(AssignmentConstants.ALLOW_RESUBMIT_CLOSETIME);
					}
					Time allowResubmitTime = null;
					if (allowResubmitTimeString != null)
					{
						// if there is a local setting
						allowResubmitTime = TimeService.newTime(Long.parseLong(allowResubmitTimeString));
					}
					else if (a != null)
					{
						// if there is no local setting, default to assignment close time
						allowResubmitTime = a.getCloseTime();
					}
					
					// set up related state variables
					putTimePropertiesInState(state, allowResubmitTime, ALLOW_RESUBMIT_CLOSEMONTH, ALLOW_RESUBMIT_CLOSEDAY, ALLOW_RESUBMIT_CLOSEYEAR, ALLOW_RESUBMIT_CLOSEHOUR, ALLOW_RESUBMIT_CLOSEMIN, ALLOW_RESUBMIT_CLOSEAMPM);
					
					// put allow resubmit time information into context
					putTimePropertiesInContext(context, state, "Resubmit", ALLOW_RESUBMIT_CLOSEMONTH, ALLOW_RESUBMIT_CLOSEDAY, ALLOW_RESUBMIT_CLOSEYEAR, ALLOW_RESUBMIT_CLOSEHOUR, ALLOW_RESUBMIT_CLOSEMIN, ALLOW_RESUBMIT_CLOSEAMPM);
				}*/
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}

		context.put("user", state.getAttribute(STATE_USER));
		context.put("submissionTypeTable", submissionTypeTable());
		context.put("instructorAttachments", state.getAttribute(ATTACHMENTS));
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		context.put("service", assignmentService);

		// names
		context.put("name_grade_assignment_id", GRADE_SUBMISSION_ASSIGNMENT_ID);
		context.put("name_feedback_comment", GRADE_SUBMISSION_FEEDBACK_COMMENT);
		context.put("name_feedback_text", GRADE_SUBMISSION_FEEDBACK_TEXT);
		context.put("name_feedback_attachment", GRADE_SUBMISSION_FEEDBACK_ATTACHMENT);
		context.put("name_grade", GRADE_SUBMISSION_GRADE);
		context.put("name_allowResubmitNumber", AssignmentConstants.ALLOW_RESUBMIT_NUMBER);

		// values
		context.put("value_year_from", state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_FROM));
		context.put("value_year_to", state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_TO));
		context.put("value_grade_assignment_id", state.getAttribute(GRADE_SUBMISSION_ASSIGNMENT_ID));
		context.put("value_feedback_comment", state.getAttribute(GRADE_SUBMISSION_FEEDBACK_COMMENT));
		context.put("value_feedback_text", state.getAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT));
		context.put("value_feedback_attachment", state.getAttribute(ATTACHMENTS));

		// format to show one decimal place in grade
		context.put("value_grade", (gradeType == 3) ? displayGrade(state, (String) state.getAttribute(GRADE_SUBMISSION_GRADE))
				: state.getAttribute(GRADE_SUBMISSION_GRADE));

		context.put("assignment_expand_flag", state.getAttribute(GRADE_SUBMISSION_ASSIGNMENT_EXPAND_FLAG));
		context.put("gradingAttachments", state.getAttribute(ATTACHMENTS));

		// is this a non-electronic submission type of assignment
		context.put("nonElectronic", (a!=null && a.getTypeOfSubmission() == AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)?Boolean.TRUE:Boolean.FALSE);
		
		if (addGradeDraftAlert)
		{
			addAlert(state, rb.getString("grading.alert.draft.beforeclosedate"));
		}
		context.put("alertGradeDraft", Boolean.valueOf(addGradeDraftAlert));
		
		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_GRADE_SUBMISSION;

	} // build_instructor_grade_submission_context

	/**
	 * Parse time value and put corresponding values into state
	 * @param context
	 * @param state
	 * @param a
	 * @param timeValue
	 * @param timeName
	 * @param month
	 * @param day
	 * @param year
	 * @param hour
	 * @param min
	 * @param ampm
	 */
	private void putTimePropertiesInState(SessionState state, Time timeValue,
											String month, String day, String year, String hour, String min, String ampm) {
		TimeBreakdown bTime = timeValue.breakdownLocal();
		state.setAttribute(month, new Integer(bTime.getMonth()));
		state.setAttribute(day, new Integer(bTime.getDay()));
		state.setAttribute(year, new Integer(bTime.getYear()));
		int bHour = bTime.getHour();
		if (bHour >= 12)
		{
			state.setAttribute(ampm, "PM");
		}
		else
		{		
			state.setAttribute(ampm, "AM");
		}
		if (bHour == 0)
		{
			// for midnight point, we mark it as 12AM
			bHour = 12;
		}		
		state.setAttribute(hour, new Integer((bHour > 12) ? bHour - 12 : bHour));
		state.setAttribute(min, new Integer(bTime.getMin()));
	}

	/**
	 * put related time information into context variable
	 * @param context
	 * @param state
	 * @param timeName
	 * @param month
	 * @param day
	 * @param year
	 * @param hour
	 * @param min
	 * @param ampm
	 */
	private void putTimePropertiesInContext(Context context, SessionState state, String timeName,
													String month, String day, String year, String hour, String min, String ampm) {
		// get the submission level of close date setting
		context.put("name_" + timeName + "Month", month);
		context.put("name_" + timeName + "Day", day);
		context.put("name_" + timeName + "Year", year);
		context.put("name_" + timeName + "Hour", hour);
		context.put("name_" + timeName + "Min", min);
		context.put("name_" + timeName + "AMPM", ampm);
		context.put("value_" + timeName + "Month", (Integer) state.getAttribute(month));
		context.put("value_" + timeName + "Day", (Integer) state.getAttribute(day));
		context.put("value_" + timeName + "Year", (Integer) state.getAttribute(year));
		context.put("value_" + timeName + "AMPM", (String) state.getAttribute(ampm));
		context.put("value_" + timeName + "Hour", (Integer) state.getAttribute(hour));
		context.put("value_" + timeName + "Min", (Integer) state.getAttribute(min));
	}

	private List getPrevFeedbackAttachments(ResourceProperties p) {
		String attachmentsString = p.getProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS);
		String[] attachmentsReferences = attachmentsString.split(",");
		List prevFeedbackAttachments = EntityManager.newReferenceList();
		for (int k =0; k < attachmentsReferences.length; k++)
		{
			prevFeedbackAttachments.add(EntityManager.newReference(attachmentsReferences[k]));
		}
		return prevFeedbackAttachments;
	}

	/**
	 * build the instructor preview of grading submission
	 */
	protected String build_instructor_preview_grade_submission_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{

		// assignment
		int gradeType = -1;
		try
		{
			Assignment a = assignmentService.getAssignment((String) state.getAttribute(GRADE_SUBMISSION_ASSIGNMENT_ID));
			context.put("assignment", a);
			gradeType = a.getTypeOfGrade();
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		// submission
		try
		{
			context.put("submission", assignmentService.getSubmission((String) state.getAttribute(GRADE_SUBMISSION_SUBMISSION_ID)));
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}

		User user = (User) state.getAttribute(STATE_USER);
		context.put("user", user);
		context.put("submissionTypeTable", submissionTypeTable());
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		context.put("service", assignmentService);

		// filter the feedback text for the instructor comment and mark it as red
		String feedbackText = (String) state.getAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT);
		context.put("feedback_comment", state.getAttribute(GRADE_SUBMISSION_FEEDBACK_COMMENT));
		context.put("feedback_text", feedbackText);
		context.put("feedback_attachment", state.getAttribute(GRADE_SUBMISSION_FEEDBACK_ATTACHMENT));

		// format to show one decimal place
		String grade = (String) state.getAttribute(GRADE_SUBMISSION_GRADE);
		if (gradeType == 3)
		{
			grade = displayGrade(state, grade);
		}
		context.put("grade", grade);

		context.put("comment_open", COMMENT_OPEN);
		context.put("comment_close", COMMENT_CLOSE);

		context.put("allowResubmitNumber", state.getAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER));
		String closeTimeString =(String) state.getAttribute(AssignmentConstants.ALLOW_RESUBMIT_CLOSETIME);
		if (closeTimeString != null)
		{
			// close time for resubmit
			Time time = TimeService.newTime(Long.parseLong(closeTimeString));
			context.put("allowResubmitCloseTime", time.toStringLocalFull());
		}

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION;

	} // build_instructor_preview_grade_submission_context

	/**
	 * build the instructor view to grade an assignment
	 */
	protected String build_instructor_grade_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		context.put("user", state.getAttribute(STATE_USER));

		// sorting related fields
		context.put("sortedBy", state.getAttribute(SORTED_GRADE_SUBMISSION_BY));
		context.put("sortedAsc", state.getAttribute(SORTED_GRADE_SUBMISSION_ASC));
		context.put("sort_lastName", SORTED_GRADE_SUBMISSION_BY_LASTNAME);
		context.put("sort_submitTime", SORTED_GRADE_SUBMISSION_BY_SUBMIT_TIME);
		context.put("sort_submitStatus", SORTED_GRADE_SUBMISSION_BY_STATUS);
		context.put("sort_submitGrade", SORTED_GRADE_SUBMISSION_BY_GRADE);
		context.put("sort_submitReleased", SORTED_GRADE_SUBMISSION_BY_RELEASED);
		context.put("sort_submitReview", SORTED_GRADE_SUBMISSION_CONTENTREVIEW);

		Assignment assignment = null;
		try
		{
			assignment = assignmentService.getAssignment((String) state.getAttribute(EXPORT_ASSIGNMENT_REF));
			context.put("assignment", assignment);
			state.setAttribute(EXPORT_ASSIGNMENT_ID, assignment.getId());
			
			// ever set the default grade for no-submissions
			String defaultGrade = assignment.getNoSubmissionDefaultGrade();
			if (defaultGrade != null)
			{
				context.put("defaultGrade", defaultGrade);
			}
			
			// groups
			if (state.getAttribute(VIEW_SUBMISSION_LIST_OPTION) == null)
			{
				state.setAttribute(VIEW_SUBMISSION_LIST_OPTION, rb.getString("gen.viewallgroupssections"));
			}
			String view = (String)state.getAttribute(VIEW_SUBMISSION_LIST_OPTION);
			context.put("view", view);
			// access point url for zip file download
			String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
			String accessPointUrl = ServerConfigurationService.getAccessUrl().concat(assignmentService.submissionsZipReference(
					contextString, (String) state.getAttribute(EXPORT_ASSIGNMENT_REF)));
			if (!view.equals(rb.getString("gen.viewallgroupssections")))
			{
				// append the group info to the end
				accessPointUrl = accessPointUrl.concat(view);
			}
			context.put("accessPointUrl", accessPointUrl);
				
			if (assignmentService.getAllowGroupAssignments())
			{
				Collection groupsAllowGradeAssignment = assignmentService.getGroupsAllowGradeAssignment((String) state.getAttribute(STATE_CONTEXT_STRING), assignment.getReference());
				
				// group list which user can add message to
				if (groupsAllowGradeAssignment.size() > 0)
				{
					String sort = (String) state.getAttribute(SORTED_BY);
					String asc = (String) state.getAttribute(SORTED_ASC);
					if (sort == null || (!sort.equals(SORTED_BY_GROUP_TITLE) && !sort.equals(SORTED_BY_GROUP_DESCRIPTION)))
					{
						sort = SORTED_BY_GROUP_TITLE;
						asc = Boolean.TRUE.toString();
						state.setAttribute(SORTED_BY, sort);
						state.setAttribute(SORTED_ASC, asc);
					}
					context.put("groups", new SortedIterator(groupsAllowGradeAssignment.iterator(), new AssignmentComparator(state, sort, asc)));
				}
			}
			
			// for non-electronic assignment
			if (assignment.getTypeOfSubmission() == AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
			{
				updateNonElectronicSubmissions(state, assignment);
			}
			
			List userSubmissions = prepPage(state);
			state.setAttribute(USER_SUBMISSIONS, userSubmissions);
			context.put("userSubmissions", state.getAttribute(USER_SUBMISSIONS));
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.taggable.api.TaggingManager");
		if (taggingManager.isTaggable() && assignment != null)
		{
			context.put("producer", ComponentManager
					.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer"));
			addProviders(context, state);
			addActivity(context, assignment);
			context.put("taggable", Boolean.valueOf(true));
		}

		context.put("submissionTypeTable", submissionTypeTable());
		context.put("attachments", state.getAttribute(ATTACHMENTS));
		
		
		// Get turnitin results for instructors
		
		
		
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		context.put("service", assignmentService);

		context.put("assignment_expand_flag", state.getAttribute(GRADE_ASSIGNMENT_EXPAND_FLAG));
		context.put("submission_expand_flag", state.getAttribute(GRADE_SUBMISSION_EXPAND_FLAG));

		// the user directory service
		context.put("userDirectoryService", UserDirectoryService.getInstance());
		add2ndToolbarFields(data, context);

		pagingInfoToContext(state, context);
		
		String template = (String) getContext(data).get("template");
		
		return template + TEMPLATE_INSTRUCTOR_GRADE_ASSIGNMENT;

	} // build_instructor_grade_assignment_context


	/**
	 * Synchronize the submissions for non electronic assignment with the current user set
	 * @param state
	 * @param assignment
	 */
	private void updateNonElectronicSubmissions(SessionState state, Assignment assignment) {
		List submissions = assignmentService.getSubmissions(assignment);
		// the following operation is accessible for those with add assignment right
		List allowAddSubmissionUsers = assignmentService.allowAddSubmissionUsers(assignment.getReference());
		
		HashSet<String> submittersIdSet = getSubmittersIdSet(submissions);
		HashSet<String> allowAddSubmissionUsersIdSet = getAllowAddSubmissionUsersIdSet(allowAddSubmissionUsers);
		
		if (!submittersIdSet.equals(allowAddSubmissionUsersIdSet))
		{
			// get the difference between two sets
			try
			{
				HashSet<String> addSubmissionUserIdSet = (HashSet<String>) allowAddSubmissionUsersIdSet.clone();
				addSubmissionUserIdSet.removeAll(submittersIdSet);
				HashSet<String> removeSubmissionUserIdSet = (HashSet<String>) submittersIdSet.clone();
				removeSubmissionUserIdSet.removeAll(allowAddSubmissionUsersIdSet);
		        
				try
				{
					addRemoveSubmissionsForNonElectronicAssignment(state, submissions, addSubmissionUserIdSet, removeSubmissionUserIdSet, assignment); 
				}
				catch (Exception ee)
				{
					log.warn(this + ee.getMessage());
				}
			}
			catch (Exception e)
			{
				log.warn(this + e.getMessage());
			}
		}
	}

	/**
	 * build the instructor view of an assignment
	 */
	protected String build_instructor_view_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		context.put("tlang", rb);
		
		Assignment assignment = null;
		try
		{
			assignment = assignmentService.getAssignment((String) state.getAttribute(VIEW_ASSIGNMENT_ID));
			context.put("assignment", assignment);
			
			// the creator 
			String creatorId = assignment.getCreator();
			try
			{
				User creator = UserDirectoryService.getUser(creatorId);
				context.put("creator", creator.getDisplayName());
			}
			catch (Exception ee)
			{
				context.put("creator", creatorId);
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.taggable.api.TaggingManager");
		if (taggingManager.isTaggable() && assignment != null)
		{
			List<DecoratedTaggingProvider> providers = addProviders(context, state);
			List<TaggingHelperInfo> activityHelpers = new ArrayList<TaggingHelperInfo>();
			AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
					.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");
			for (DecoratedTaggingProvider provider : providers)
			{
				TaggingHelperInfo helper = provider.getProvider()
						.getActivityHelperInfo(
								assignmentActivityProducer.getActivity(
										assignment).getReference());
				if (helper != null)
				{
					activityHelpers.add(helper);
				}
			}
			addActivity(context, assignment);
			context.put("activityHelpers", activityHelpers);
			context.put("taggable", Boolean.valueOf(true));
		}

		context.put("currentTime", TimeService.newTime());
		context.put("submissionTypeTable", submissionTypeTable());
		context.put("hideAssignmentFlag", state.getAttribute(VIEW_ASSIGNMENT_HIDE_ASSIGNMENT_FLAG));
		context.put("hideStudentViewFlag", state.getAttribute(VIEW_ASSIGNMENT_HIDE_STUDENT_VIEW_FLAG));
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		
		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_VIEW_ASSIGNMENT;

	} // build_instructor_view_assignment_context

	/**
	 * build the instructor view of reordering assignments
	 */
	protected String build_instructor_reorder_assignment_context(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		context.put("context", state.getAttribute(STATE_CONTEXT_STRING));
		
		List assignments = prepPage(state);
		
		context.put("assignments", assignments.iterator());
		context.put("assignmentsize", assignments.size());
		
		String sortedBy = (String) state.getAttribute(SORTED_BY);
		String sortedAsc = (String) state.getAttribute(SORTED_ASC);
		context.put("sortedBy", sortedBy);
		context.put("sortedAsc", sortedAsc);
		
		//		 put site object into context
		try
		{
			// get current site
			Site site = SiteService.getSite((String) state.getAttribute(STATE_CONTEXT_STRING));
			context.put("site", site);
		}
		catch (Exception ignore)
		{
		}
	
		context.put("contentTypeImageService", state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE));
		context.put("userDirectoryService", UserDirectoryService.getInstance());
	
		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_REORDER_ASSIGNMENT;
	
	} // build_instructor_reorder_assignment_context

	/**
	 * build the instructor view to view the list of students for an assignment
	 */
	protected String build_instructor_view_students_assignment_context(VelocityPortlet portlet, Context context, RunData data,
			SessionState state)
	{
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);

		// get the realm and its member
		List studentMembers = new Vector();
		List allowSubmitMembers = assignmentService.allowAddAnySubmissionUsers(contextString);
		for (Iterator allowSubmitMembersIterator=allowSubmitMembers.iterator(); allowSubmitMembersIterator.hasNext();)
		{
			// get user
			try
			{
				String userId = (String) allowSubmitMembersIterator.next();
				User user = UserDirectoryService.getUser(userId);
				studentMembers.add(user);
			}
			catch (Exception ee)
			{
				log.warn(this + ee.getMessage());
			}
		}
		
		context.put("studentMembers", new SortedIterator(studentMembers.iterator(), new AssignmentComparator(state, SORTED_USER_BY_SORTNAME, Boolean.TRUE.toString())));
		context.put("assignmentService", assignmentService);
		
		Hashtable showStudentAssignments = new Hashtable();
		if (state.getAttribute(STUDENT_LIST_SHOW_TABLE) != null)
		{
			Set showStudentListSet = (Set) state.getAttribute(STUDENT_LIST_SHOW_TABLE);
			context.put("studentListShowSet", showStudentListSet);
			for (Iterator showStudentListSetIterator=showStudentListSet.iterator(); showStudentListSetIterator.hasNext();)
			{
				// get user
				try
				{
					String userId = (String) showStudentListSetIterator.next();
					User user = UserDirectoryService.getUser(userId);
					
					// sort the assignments into the default order before adding
					Iterator assignmentSorter = assignmentService.getAssignmentsForContext(contextString, userId);
					// filter to obtain only grade-able assignments
					List rv = new Vector();
					while (assignmentSorter.hasNext())
					{
						Assignment a = (Assignment) assignmentSorter.next();
						if (assignmentService.allowGradeSubmission(a.getReference()))
						{
							rv.add(a);
						}
					}
					Iterator assignmentSortFinal = new SortedIterator(rv.iterator(), new AssignmentComparator(state, SORTED_BY_DEFAULT, Boolean.TRUE.toString()));

					showStudentAssignments.put(user, assignmentSortFinal);
				}
				catch (Exception ee)
				{
					log.warn(this + ee.getMessage());
				}
			}
			
		}

		context.put("studentAssignmentsTable", showStudentAssignments);

		add2ndToolbarFields(data, context);

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT;

	} // build_instructor_view_students_assignment_context

	/**
	 * build the instructor view to report the submissions
	 */
	protected String build_instructor_report_submissions(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		context.put("submissions", prepPage(state));

		context.put("sortedBy", (String) state.getAttribute(SORTED_SUBMISSION_BY));
		context.put("sortedAsc", (String) state.getAttribute(SORTED_SUBMISSION_ASC));
		context.put("sortedBy_lastName", SORTED_SUBMISSION_BY_LASTNAME);
		context.put("sortedBy_submitTime", SORTED_SUBMISSION_BY_SUBMIT_TIME);
		context.put("sortedBy_grade", SORTED_SUBMISSION_BY_GRADE);
		context.put("sortedBy_status", SORTED_SUBMISSION_BY_STATUS);
		context.put("sortedBy_released", SORTED_SUBMISSION_BY_RELEASED);
		context.put("sortedBy_assignment", SORTED_SUBMISSION_BY_ASSIGNMENT);
		context.put("sortedBy_maxGrade", SORTED_SUBMISSION_BY_MAX_GRADE);

		add2ndToolbarFields(data, context);

		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		context.put("accessPointUrl", ServerConfigurationService.getAccessUrl()
				+ assignmentService.gradesSpreadsheetReference(contextString, null));

		pagingInfoToContext(state, context);

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_REPORT_SUBMISSIONS;

	} // build_instructor_report_submissions
	
	// Is Gradebook defined for the site?
	protected boolean isGradebookDefined()
	{
		boolean rv = false;
		try
		{
			GradebookService g = (GradebookService) (org.sakaiproject.service.gradebook.shared.GradebookService) ComponentManager
					.get("org.sakaiproject.service.gradebook.GradebookService");
			String gradebookUid = ToolManager.getInstance().getCurrentPlacement().getContext();
			if (g.isGradebookDefined(gradebookUid))
			{
				rv = true;
			}
		}
		catch (Exception e)
		{
			log.debug(this + rb.getString("addtogradebook.alertMessage") + "\n" + e.getMessage());
		}

		return rv;

	} // isGradebookDefined()
	
	/**
	 * build the instructor view to upload information from archive file
	 */
	protected String build_instructor_upload_all(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		context.put("hasSubmissionText", state.getAttribute(UPLOAD_ALL_HAS_SUBMISSION_TEXT));
		context.put("hasSubmissionAttachment", state.getAttribute(UPLOAD_ALL_HAS_SUBMISSION_ATTACHMENT));
		context.put("hasGradeFile", state.getAttribute(UPLOAD_ALL_HAS_GRADEFILE));
		context.put("hasComments", state.getAttribute(UPLOAD_ALL_HAS_COMMENTS));
		context.put("hasFeedbackText", state.getAttribute(UPLOAD_ALL_HAS_FEEDBACK_TEXT));
		context.put("hasFeedbackAttachment", state.getAttribute(UPLOAD_ALL_HAS_FEEDBACK_ATTACHMENT));
		context.put("releaseGrades", state.getAttribute(UPLOAD_ALL_RELEASE_GRADES));
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		context.put("accessPointUrl", (ServerConfigurationService.getAccessUrl()).concat(assignmentService.submissionsZipReference(
				contextString, (String) state.getAttribute(EXPORT_ASSIGNMENT_REF))));

		String template = (String) getContext(data).get("template");
		return template + TEMPLATE_INSTRUCTOR_UPLOAD_ALL;

	} // build_instructor_upload_all
	
   /**
    ** Retrieve tool title from Tool configuration file or use default
    ** (This should return i18n version of tool title if available)
    **/
   private String getToolTitle()
   {
      Tool tool = ToolManager.getTool(ASSIGNMENT_TOOL_ID);
      String toolTitle = null;

      if (tool == null)
        toolTitle = "Assignments";
      else
        toolTitle = tool.getTitle();

      return toolTitle;
   }

	/**
	 * integration with gradebook
	 *
	 * @param state
	 * @param assignmentRef Assignment reference
	 * @param associateGradebookAssignment The title for the associated GB assignment
	 * @param addUpdateRemoveAssignment "add" for adding the assignment; "update" for updating the assignment; "remove" for remove assignment
	 * @param oldAssignment_title The original assignment title
	 * @param newAssignment_title The updated assignment title
	 * @param newAssignment_maxPoints The maximum point of the assignment
	 * @param newAssignment_dueTime The due time of the assignment
	 * @param submissionRef Any submission grade need to be updated? Do bulk update if null
	 * @param updateRemoveSubmission "update" for update submission;"remove" for remove submission
	 */
	protected void integrateGradebook (SessionState state, String assignmentRef, String associateGradebookAssignment, String addUpdateRemoveAssignment, String oldAssignment_title, String newAssignment_title, int newAssignment_maxPoints, Date newAssignment_dueTime, String submissionRef, String updateRemoveSubmission)
	{
		associateGradebookAssignment = StringUtil.trimToNull(associateGradebookAssignment);

		// add or remove external grades to gradebook
		// a. if Gradebook does not exists, do nothing, 'cos setting should have been hidden
		// b. if Gradebook exists, just call addExternal and removeExternal and swallow any exception. The
		// exception are indication that the assessment is already in the Gradebook or there is nothing
		// to remove.
		String assignmentToolTitle = getToolTitle();

		GradebookService g = (GradebookService) (org.sakaiproject.service.gradebook.shared.GradebookService) ComponentManager.get("org.sakaiproject.service.gradebook.GradebookService");
		String gradebookUid = ToolManager.getInstance().getCurrentPlacement().getContext();
		if (g.isGradebookDefined(gradebookUid))
		{
			boolean isExternalAssignmentDefined=g.isExternalAssignmentDefined(gradebookUid, assignmentRef);
			boolean isExternalAssociateAssignmentDefined = g.isExternalAssignmentDefined(gradebookUid, associateGradebookAssignment);
			boolean isAssignmentDefined = g.isAssignmentDefined(gradebookUid, associateGradebookAssignment);

			if (addUpdateRemoveAssignment != null)
			{
				// add an entry into Gradebook for newly created assignment or modified assignment, and there wasn't a correspond record in gradebook yet
				if ((addUpdateRemoveAssignment.equals(assignmentService.GRADEBOOK_INTEGRATION_ADD) || (addUpdateRemoveAssignment.equals("update") && !isExternalAssignmentDefined)) && associateGradebookAssignment == null)
				{
					// add assignment into gradebook
					try
					{
						// add assignment to gradebook
						g.addExternalAssessment(gradebookUid, assignmentRef, null, newAssignment_title,
								newAssignment_maxPoints/10.0, new Date(newAssignment_dueTime.getTime()), assignmentToolTitle);
					}
					catch (AssignmentHasIllegalPointsException e)
					{
						addAlert(state, rb.getString("addtogradebook.illegalPoints"));
					}
					catch (ConflictingAssignmentNameException e)
					{
						// add alert prompting for change assignment title
						addAlert(state, rb.getString("addtogradebook.nonUniqueTitle"));
					}
					catch (ConflictingExternalIdException e)
					{
						// this shouldn't happen, as we have already checked for assignment reference before. Log the error
						log.warn(this + e.getMessage());
					}
					catch (GradebookNotFoundException e)
					{
						// this shouldn't happen, as we have checked for gradebook existence before
						log.warn(this + e.getMessage());
					}
					catch (Exception e)
					{
						// ignore
						log.warn(this + e.getMessage());
					}
				}
				else if (addUpdateRemoveAssignment.equals("update"))
				{
					if (associateGradebookAssignment != null && isExternalAssociateAssignmentDefined)
					{
						// if there is an external entry created in Gradebook based on this assignment, update it
						try
						{
						    Assignment a = assignmentService.getAssignment(associateGradebookAssignment);

						    // update attributes if the GB assignment was created for the assignment
						    g.updateExternalAssessment(gradebookUid, associateGradebookAssignment, null, newAssignment_title, newAssignment_maxPoints/10.0, new Date(newAssignment_dueTime.getTime()));
						}
					    catch(Exception e)
				        {
				        		log.warn(rb.getString("cannot_find_assignment") + assignmentRef + ": " + e.getMessage());
				        }
					}
				}	// addUpdateRemove != null
				else if (addUpdateRemoveAssignment.equals("remove"))
				{
					// remove assignment and all submission grades
					removeNonAssociatedExternalGradebookEntry((String) state.getAttribute(STATE_CONTEXT_STRING), assignmentRef, associateGradebookAssignment, g, gradebookUid);
				}
			}

			if (updateRemoveSubmission != null)
			{
				try
				{
					Assignment a = assignmentService.getAssignment(assignmentRef);

					if (updateRemoveSubmission.equals("update")
							&& a.getGradableObjectId() != null
							&& a.getTypeOfGrade() == AssignmentConstants.SCORE_GRADE_TYPE)
					{
						if (submissionRef == null)
						{
							// bulk add all grades for assignment into gradebook
							Iterator submissions = assignmentService.getSubmissions(a).iterator();

							Map m = new HashMap();

							// any score to copy over? get all the assessmentGradingData and copy over
							while (submissions.hasNext())
							{
								AssignmentSubmission aSubmission = (AssignmentSubmission) submissions.next();
								if (aSubmission.getCurrentSubmissionVersion().isReturned())
								{
									String submitterId = aSubmission.getSubmitterId();
									String gradeString = StringUtil.trimToNull(aSubmission.getCurrentSubmissionVersion().getGrade());
									Double grade = gradeString != null ? Double.valueOf(displayGrade(state,gradeString)) : null;
									m.put(submitterId, grade);
								}
							}

							// need to update only when there is at least one submission
							if (m.size()>0)
							{
								if (associateGradebookAssignment != null)
								{
									if (isExternalAssociateAssignmentDefined)
									{
										// the associated assignment is externally maintained
										g.updateExternalAssessmentScores(gradebookUid, associateGradebookAssignment, m);
									}
									else if (isAssignmentDefined)
									{
										// the associated assignment is internal one, update records one by one
										submissions = assignmentService.getSubmissions(a).iterator();
										while (submissions.hasNext())
										{
											AssignmentSubmission aSubmission = (AssignmentSubmission) submissions.next();
											String submitterId = aSubmission.getSubmitterId();
											String gradeString = StringUtil.trimToNull(aSubmission.getCurrentSubmissionVersion().getGrade());
											Double grade = (gradeString != null && aSubmission.getCurrentSubmissionVersion().isReturned()) ? Double.valueOf(displayGrade(state,gradeString)) : null;
											g.setAssignmentScore(gradebookUid, associateGradebookAssignment, submitterId, grade, assignmentToolTitle);
										}
									}
								}
								else if (isExternalAssignmentDefined)
								{
									g.updateExternalAssessmentScores(gradebookUid, assignmentRef, m);
								}
							}
						}
						else
						{
							try
							{
								// only update one submission
								AssignmentSubmission aSubmission = (AssignmentSubmission) assignmentService.getSubmission(submissionRef);
								String submitterId = aSubmission.getSubmitterId();
								String gradeString = StringUtil.trimToNull(aSubmission.getCurrentSubmissionVersion().getGrade());

								if (associateGradebookAssignment != null)
								{
									if (g.isExternalAssignmentDefined(gradebookUid, associateGradebookAssignment))
									{
										// the associated assignment is externally maintained
										g.updateExternalAssessmentScore(gradebookUid, associateGradebookAssignment, submitterId,
												(gradeString != null && aSubmission.getCurrentSubmissionVersion().isReturned()) ? Double.valueOf(displayGrade(state,gradeString)) : null);
									}
									else if (g.isAssignmentDefined(gradebookUid, associateGradebookAssignment))
									{
										// the associated assignment is internal one, update records
										g.setAssignmentScore(gradebookUid, associateGradebookAssignment, submitterId,
												(gradeString != null && aSubmission.getCurrentSubmissionVersion().isReturned()) ? Double.valueOf(displayGrade(state,gradeString)) : null, assignmentToolTitle);
									}
								}
								else
								{
									g.updateExternalAssessmentScore(gradebookUid, assignmentRef, submitterId,
											(gradeString != null && aSubmission.getCurrentSubmissionVersion().isReturned()) ? Double.valueOf(displayGrade(state,gradeString)) : null);
								}
							}
							catch (Exception e)
							{
								log.warn("Cannot find submission " + submissionRef + ": " + e.getMessage());
							}
						}

					}
					else if (updateRemoveSubmission.equals("remove"))
					{
						if (submissionRef == null)
						{
							// remove all submission grades (when changing the associated entry in Gradebook)
							Iterator submissions = assignmentService.getSubmissions(a).iterator();

							// any score to copy over? get all the assessmentGradingData and copy over
							while (submissions.hasNext())
							{
								AssignmentSubmission aSubmission = (AssignmentSubmission) submissions.next();
								String submitterId = aSubmission.getSubmitterId();
								if (isExternalAssociateAssignmentDefined)
								{
									// if the old associated assignment is an external maintained one
									g.updateExternalAssessmentScore(gradebookUid, associateGradebookAssignment, submitterId, null);
								}
								else if (isAssignmentDefined)
								{
									g.setAssignmentScore(gradebookUid, associateGradebookAssignment, submitterId, null, assignmentToolTitle);
								}
							}
						}
						else
						{
							// remove only one submission grade
							try
							{
								AssignmentSubmission aSubmission = (AssignmentSubmission) assignmentService.getSubmission(submissionRef);
								g.updateExternalAssessmentScore(gradebookUid, assignmentRef, aSubmission.getSubmitterId(), null);
							}
							catch (Exception e)
							{
								log.warn("Cannot find submission " + submissionRef + ": " + e.getMessage());
							}
						}
					}
				}
				catch (Exception e)
				{
					log.warn(rb.getString("cannot_find_assignment") + assignmentRef + ": " + e.getMessage());
				}
			} // updateRemoveSubmission != null
		}
	} // integrateGradebook

	/**
	 * Go to the instructor view
	 */
	public void doView_instructor(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		state.setAttribute(SORTED_BY, SORTED_BY_DEFAULT);
		state.setAttribute(SORTED_ASC, Boolean.TRUE.toString());

	} // doView_instructor

	/**
	 * Go to the student view
	 */
	public void doView_student(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// to the student list of assignment view
		state.setAttribute(SORTED_BY, SORTED_BY_DEFAULT);
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doView_student

	/**
	 * Action is to view the content of one specific assignment submission
	 */
	public void doView_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the submission context
		resetViewSubmission(state);

		ParameterParser params = data.getParameters();
		String assignmentReference = params.getString("assignmentReference");
		state.setAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE, assignmentReference);

		User u = (User) state.getAttribute(STATE_USER);

		try
		{
			AssignmentSubmission submission = assignmentService.getSubmission(assignmentReference, u);

			if (submission != null)
			{
				state.setAttribute(VIEW_SUBMISSION_TEXT, submission.getCurrentSubmissionVersion().getSubmittedText());
			//TODO:zqian	state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, (new Boolean(submission.getCurrentSubmissionVersion().getHonorPledgeFlag())).toString());
				List v = EntityManager.newReferenceList();
				Iterator l = submission.getCurrentSubmissionVersion().getSubmittedAttachments().iterator();
				while (l.hasNext())
				{
					v.add(l.next());
				}
				state.setAttribute(ATTACHMENTS, v);		
			}
			else
			{
				state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, "false");
				state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());	
			}

			state.setAttribute(STATE_MODE, MODE_STUDENT_VIEW_SUBMISSION);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		} // try

	} // doView_submission
	
	/**
	 * Action is to view the content of one specific assignment submission
	 */
	public void doView_submission_list_option(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		
		ParameterParser params = data.getParameters();
		String view = params.getString("view");
		state.setAttribute(VIEW_SUBMISSION_LIST_OPTION, view);

	} // doView_submission_list_option

	/**
	 * Preview of the submission
	 */
	public void doPreview_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		ParameterParser params = data.getParameters();
		// String assignmentId = params.getString(assignmentId);
		state.setAttribute(PREVIEW_SUBMISSION_ASSIGNMENT_REFERENCE, state.getAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE));

		// retrieve the submission text (as formatted text)
		boolean checkForFormattingErrors = true; // the student is submitting something - so check for errors
		String text = processFormattedTextFromBrowser(state, params.getCleanString(VIEW_SUBMISSION_TEXT), checkForFormattingErrors);

		state.setAttribute(PREVIEW_SUBMISSION_TEXT, text);
		state.setAttribute(VIEW_SUBMISSION_TEXT, text);

		// assign the honor pledge attribute
		String honor_pledge_yes = params.getString(VIEW_SUBMISSION_HONOR_PLEDGE_YES);
		if (honor_pledge_yes == null)
		{
			honor_pledge_yes = "false";
		}
		state.setAttribute(PREVIEW_SUBMISSION_HONOR_PLEDGE_YES, honor_pledge_yes);
		state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, honor_pledge_yes);
		
		state.setAttribute(PREVIEW_SUBMISSION_ATTACHMENTS, state.getAttribute(ATTACHMENTS));
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_STUDENT_PREVIEW_SUBMISSION);
		}
	} // doPreview_submission

	/**
	 * Preview of the grading of submission
	 */
	public void doPreview_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// read user input
		readGradeForm(data, state, "read");

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION);
		}

	} // doPreview_grade_submission

	/**
	 * Action is to end the preview submission process
	 */
	public void doDone_preview_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_STUDENT_VIEW_SUBMISSION);

	} // doDone_preview_submission

	/**
	 * Action is to end the view assignment process
	 */
	public void doDone_view_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doDone_view_assignments

	/**
	 * Action is to end the preview new assignment process
	 */
	public void doDone_preview_new_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the new assignment page
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT);

	} // doDone_preview_new_assignment

	/**
	 * Action is to end the user view assignment process and redirect him to the assignment list view
	 */
	public void doCancel_student_view_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the view assignment
		state.setAttribute(VIEW_ASSIGNMENT_ID, "");

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doCancel_student_view_assignment

	/**
	 * Action is to end the show submission process
	 */
	public void doCancel_show_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the view assignment
		state.setAttribute(VIEW_ASSIGNMENT_ID, "");

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doCancel_show_submission

	/**
	 * Action is to cancel the delete assignment process
	 */
	public void doCancel_delete_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the show assignment object
		state.setAttribute(DELETE_ASSIGNMENT_IDS, new Vector());

		// back to the instructor list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doCancel_delete_assignment

	/**
	 * Action is to end the show submission process
	 */
	public void doCancel_edit_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		
		// reset sorting
		setDefaultSort(state);

	} // doCancel_edit_assignment

	/**
	 * Action is to end the show submission process
	 */
	public void doCancel_new_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the assignment object
		resetAssignment(state);

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		
		// reset sorting
		setDefaultSort(state);

	} // doCancel_new_assignment

	/**
	 * Action is to cancel the grade submission process
	 */
	public void doCancel_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the grading page
		resetGradeSubmission(state);

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);

	} // doCancel_grade_submission

	/**
	 * clean the state variables related to grading page
	 * @param state
	 */
	private void resetGradeSubmission(SessionState state) {
		// reset the grade parameters
		state.removeAttribute(GRADE_SUBMISSION_FEEDBACK_COMMENT);
		state.removeAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT);
		state.removeAttribute(GRADE_SUBMISSION_FEEDBACK_ATTACHMENT);
		state.removeAttribute(GRADE_SUBMISSION_GRADE);
		state.removeAttribute(GRADE_SUBMISSION_SUBMISSION_ID);
		state.removeAttribute(GRADE_GREATER_THAN_MAX_ALERT);
		state.removeAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER);
		state.removeAttribute(AssignmentConstants.ALLOW_RESUBMIT_CLOSETIME);
		state.removeAttribute(ALLOW_RESUBMIT_CLOSEMONTH);
		state.removeAttribute(ALLOW_RESUBMIT_CLOSEDAY);
		state.removeAttribute(ALLOW_RESUBMIT_CLOSEYEAR);
		state.removeAttribute(ALLOW_RESUBMIT_CLOSEHOUR);
		state.removeAttribute(ALLOW_RESUBMIT_CLOSEMIN);
		state.removeAttribute(ALLOW_RESUBMIT_CLOSEAMPM);
	}

	/**
	 * Action is to cancel the preview grade process
	 */
	public void doCancel_preview_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the instructor view of grading a submission
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_SUBMISSION);

	} // doCancel_preview_grade_submission
	
	/**
	 * Action is to cancel the reorder process
	 */
	public void doCancel_reorder(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		
		// back to the list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doCancel_reorder
	
	/**
	 * Action is to cancel the preview grade process
	 */
	public void doCancel_preview_to_list_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		
		// back to the instructor view of grading a submission
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);

	} // doCancel_preview_to_list_submission
	
	/**
	 * Action is to return to the view of list assignments
	 */
	public void doList_assignments(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		state.setAttribute(SORTED_BY, SORTED_BY_DEFAULT);
		state.setAttribute(SORTED_ASC, Boolean.TRUE.toString());

	} // doList_assignments

	/**
	 * Action is to cancel the student view grade process
	 */
	public void doCancel_view_grade(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the view grade submission id
		state.setAttribute(VIEW_GRADE_SUBMISSION_ID, "");

		// back to the student list view of assignments
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);

	} // doCancel_view_grade

	/**
	 * Action is to save the grade to submission
	 */
	public void doSave_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		readGradeForm(data, state, "save");
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			grade_submission_option(data, "save");
		}

	} // doSave_grade_submission

	/**
	 * Action is to release the grade to submission
	 */
	public void doRelease_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		readGradeForm(data, state, "release");
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			grade_submission_option(data, "release");
		}

	} // doRelease_grade_submission

	/**
	 * Action is to return submission with or without grade
	 */
	public void doReturn_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		readGradeForm(data, state, "return");
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			grade_submission_option(data, "return");
		}

	} // doReturn_grade_submission

	/**
	 * Action is to return submission with or without grade from preview
	 */
	public void doReturn_preview_grade_submission(RunData data)
	{
		grade_submission_option(data, "return");

	} // doReturn_grade_preview_submission

	/**
	 * Action is to save submission with or without grade from preview
	 */
	public void doSave_preview_grade_submission(RunData data)
	{
		grade_submission_option(data, "save");

	} // doSave_grade_preview_submission

	/**
	 * Common grading routine plus specific operation to differenciate cases when saving, releasing or returning grade.
	 */
	private void grade_submission_option(RunData data, String gradeOption)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		boolean withGrade = state.getAttribute(WITH_GRADES) != null ? ((Boolean) state.getAttribute(WITH_GRADES)).booleanValue(): false;

		String sId = (String) state.getAttribute(GRADE_SUBMISSION_SUBMISSION_ID);

		try
		{
			// for points grading, one have to enter number as the points
			String grade = (String) state.getAttribute(GRADE_SUBMISSION_GRADE);

			AssignmentSubmissionVersion s = assignmentService.getSubmissionVersion(sId);
			Assignment a = s.getAssignmentSubmission().getAssignment();
			int typeOfGrade = a.getTypeOfGrade();

			if (!withGrade)
			{
				// no grade input needed for the without-grade version of assignment tool
				if (gradeOption.equals("return") || gradeOption.equals("release"))
				{
					s.setReturned(true);
				}
			}
			else if (grade == null)
			{
				s.setGrade("");
				s.setReturned(false);
			}
			else
			{
				if (typeOfGrade == 1)
				{
					s.setGrade(rb.getString("gen.nograd"));
				}
				else
				{
					s.setGrade(grade);
				}
			}

			if (gradeOption.equals("release"))
			{
				s.setReturned(true);
				s.setTimeReleased(new Date());
			}
			else if (gradeOption.equals("save"))
			{
				s.setReturned(false);
				s.setTimeReleased(null);
			}

			if (state.getAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER) != null)
			{
				// get resubmit number
				/*TODO:zqian
				 * ResourcePropertiesEdit pEdit = sEdit.getPropertiesEdit();
				pEdit.addProperty(AssignmentConstants.ALLOW_RESUBMIT_NUMBER, (String) state.getAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER));
			
				if (state.getAttribute(ALLOW_RESUBMIT_CLOSEYEAR) != null)
				{
					// get resubmit time
					Time closeTime = getTime(state, ALLOW_RESUBMIT_CLOSEYEAR, ALLOW_RESUBMIT_CLOSEMONTH, ALLOW_RESUBMIT_CLOSEDAY, ALLOW_RESUBMIT_CLOSEHOUR, ALLOW_RESUBMIT_CLOSEMIN, ALLOW_RESUBMIT_CLOSEAMPM);
		
					pEdit.addProperty(AssignmentConstants.ALLOW_RESUBMIT_CLOSETIME, String.valueOf(closeTime.getTime()));
				}
				else
				{
					pEdit.removeProperty(AssignmentConstants.ALLOW_RESUBMIT_CLOSETIME);
				}*/
			}

			// the instructor comment
			String feedbackCommentString = StringUtil
					.trimToNull((String) state.getAttribute(GRADE_SUBMISSION_FEEDBACK_COMMENT));
			if (feedbackCommentString != null)
			{
				s.setFeedbackComment(feedbackCommentString);
			}

			// the instructor inline feedback
			String feedbackTextString = (String) state.getAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT);
			if (feedbackTextString != null)
			{
				s.setFeedbackText(feedbackTextString);
			}

			List v = (List) state.getAttribute(GRADE_SUBMISSION_FEEDBACK_ATTACHMENT);
			if (v != null)
			{
				
				s.setFeedbackAttachments(v);
			}

			assignmentService.saveSubmissionVersion(s);

			// update grades in gradebook
			String aReference = a.getReference();
			String associateGradebookAssignment = StringUtil.trimToNull(a.getGradableObjectId().toString());
			String sReference = s.getReference();

			if (gradeOption.equals("release") || gradeOption.equals("return"))
			{
				// update grade in gradebook
				integrateGradebook(state, aReference, associateGradebookAssignment, null, null, null, -1, null, sReference, "update");
			}
			else
			{
				// remove grade from gradebook
				integrateGradebook(state, aReference, associateGradebookAssignment, null, null, null, -1, null, sReference, "remove");
			}
		}
		catch (IdUnusedException e)
		{
		}
		catch (PermissionException e)
		{
		}
		catch (InUseException e)
		{
			addAlert(state, rb.getString("somelsis") + " " + rb.getString("submiss"));
		} // try

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);
			state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());
		}

	} // grade_submission_option

	/**
	 * Action is to save the submission as a draft
	 */
	public void doSave_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		// retrieve the submission text (as formatted text)
		boolean checkForFormattingErrors = true; // the student is submitting something - so check for errors
		String text = processFormattedTextFromBrowser(state, params.getCleanString(VIEW_SUBMISSION_TEXT), checkForFormattingErrors);

		if (text == null)
		{
			text = (String) state.getAttribute(VIEW_SUBMISSION_TEXT);
		}

		String honorPledgeYes = params.getString(VIEW_SUBMISSION_HONOR_PLEDGE_YES);
		if (honorPledgeYes == null)
		{
			honorPledgeYes = "false";
		}

		String aReference = (String) state.getAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE);
		User u = (User) state.getAttribute(STATE_USER);

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			try
			{
				Assignment a = assignmentService.getAssignment(aReference);
				String assignmentId = Long.toString(a.getId());

				AssignmentSubmission submission = assignmentService.getSubmission(aReference, u);
				if (submission != null)
				{
					// the submission already exists, change the text and honor pledge value, save as draft
					try
					{
						AssignmentSubmissionVersion edit = assignmentService.getSubmissionVersion(submission.getReference());
						edit.setSubmittedText(text);
						edit.setHonorPledgeFlag(Boolean.valueOf(honorPledgeYes).booleanValue());
						edit.setDraft(true);

						// add attachments
						List attachments = (List) state.getAttribute(ATTACHMENTS);
						if (attachments != null)
						{
							edit.setSubmittedAttachments(attachments);
						}
						assignmentService.saveSubmissionVersion(edit);
					}
					catch (IdUnusedException e)
					{
						addAlert(state, rb.getString("cannotfin2") + " " + a.getTitle());
					}
					catch (PermissionException e)
					{
						addAlert(state, rb.getString("youarenot12"));
					}
					catch (InUseException e)
					{
						addAlert(state, rb.getString("somelsis") + " " + rb.getString("submiss"));
					}
				}
				else
				{
					// new submission, save as draft
					try
					{
						submission = assignmentService.getSubmission(assignmentId, UserDirectoryService.getCurrentUser());
						if (submission == null)
						{
							submission = assignmentService.newSubmission(assignmentId, UserDirectoryService.getCurrentUser().getId());
						}
						AssignmentSubmissionVersion edit = assignmentService.newSubmissionVersion(submission.getReference());
						edit.setSubmittedText(text);
						edit.setHonorPledgeFlag(Boolean.valueOf(honorPledgeYes).booleanValue());
						edit.setDraft(true);

						// add attachments
						List attachments = (List) state.getAttribute(ATTACHMENTS);
						if (attachments != null)
						{
							edit.setSubmittedAttachments(attachments);
						}
						assignmentService.saveSubmissionVersion(edit);
					}
					catch (PermissionException e)
					{
						addAlert(state, rb.getString("youarenot4"));
					}
				}
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin5"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("not_allowed_to_view"));
			}
		}

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_STUDENT_VIEW_SUBMISSION_CONFIRMATION);
		}

	} // doSave_submission

	/**
	 * Action is to post the submission
	 */
	public void doPost_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		String aReference = (String) state.getAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE);
		Assignment a = null;
		try
		{
			a = assignmentService.getAssignment(aReference);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin2"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}
		
		if (assignmentService.canSubmit(contextString, a))
		{
			ParameterParser params = data.getParameters();
	
			// retrieve the submission text (as formatted text)
			boolean checkForFormattingErrors = true; // the student is submitting something - so check for errors
			String text = processFormattedTextFromBrowser(state, params.getCleanString(VIEW_SUBMISSION_TEXT), checkForFormattingErrors);
	
			if (text == null)
			{
				text = (String) state.getAttribute(VIEW_SUBMISSION_TEXT);
			}
			else
			{
				state.setAttribute(VIEW_SUBMISSION_TEXT, text);
			}
	
			String honorPledgeYes = params.getString(VIEW_SUBMISSION_HONOR_PLEDGE_YES);
			if (honorPledgeYes == null)
			{
				honorPledgeYes = (String) state.getAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES);
			}
	
			if (honorPledgeYes == null)
			{
				honorPledgeYes = "false";
			}
	
			User u = (User) state.getAttribute(STATE_USER);
			String assignmentId = "";
			if (state.getAttribute(STATE_MESSAGE) == null)
			{
				assignmentId = Long.toBinaryString(a.getId());
	
				if (a.getHonorPledge())
				{
					if (!Boolean.valueOf(honorPledgeYes).booleanValue())
					{
						addAlert(state, rb.getString("youarenot18"));
					}
					state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, honorPledgeYes);
				}

				// check the submission inputs based on the submission type
				int submissionType = a.getTypeOfSubmission();
				if (submissionType == 1)
				{
					// for the inline only submission
					if (text.length() == 0)
					{
						addAlert(state, rb.getString("youmust7"));
					}
				}
				else if (submissionType == 2)
				{
					// for the attachment only submission
					Vector v = (Vector) state.getAttribute(ATTACHMENTS);
					if ((v == null) || (v.size() == 0))
					{
						addAlert(state, rb.getString("youmust1"));
					}
				}
				else if (submissionType == 3)
				{
					// for the inline and attachment submission
					Vector v = (Vector) state.getAttribute(ATTACHMENTS);
					if ((text.length() == 0) && ((v == null) || (v.size() == 0)))
					{
						addAlert(state, rb.getString("youmust2"));
					}
				}
			}
	
			if ((state.getAttribute(STATE_MESSAGE) == null) && (a != null))
			{
				try
				{
					AssignmentSubmission submission = assignmentService.getSubmission(a.getReference(), u);
					if (submission != null)
					{
						// the submission already exists, change the text and honor pledge value, post it
						try
						{
							AssignmentSubmissionVersion sEdit = assignmentService.getSubmissionVersion(submission.getReference());
							sEdit.setSubmittedText(text);
							sEdit.setHonorPledgeFlag(Boolean.valueOf(honorPledgeYes).booleanValue());
							sEdit.setTimeSubmitted(new Date());
							sEdit.setDraft(false);
	
							// for resubmissions
							// when resubmit, keep the Returned flag on till the instructor grade again.
							Time now = TimeService.newTime();
							//ResourcePropertiesEdit sPropertiesEdit = sEdit.getPropertiesEdit();
							if (sEdit.getGrade() != null)
							{
								// add the current grade into previous grade histroy
								/*String previousGrades = (String) sEdit.getProperties().getProperty(
										ResourceProperties.PROP_SUBMISSION_SCALED_PREVIOUS_GRADES);
								if (previousGrades == null)
								{
									previousGrades = (String) sEdit.getProperties().getProperty(
											ResourceProperties.PROP_SUBMISSION_PREVIOUS_GRADES);
									if (previousGrades != null)
									{
										int typeOfGrade = a.getTypeOfGrade();
										if (typeOfGrade == 3)
										{
											// point grade assignment type
											// some old unscaled grades, need to scale the number and remove the old property
											String[] grades = StringUtil.split(previousGrades, " ");
											String newGrades = "";
											for (int jj = 0; jj < grades.length; jj++)
											{
												String grade = grades[jj];
												if (grade.indexOf(".") == -1)
												{
													// show the grade with decimal point
													grade = grade.concat(".0");
												}
												newGrades = newGrades.concat(grade + " ");
											}
											previousGrades = newGrades;
										}
										sPropertiesEdit.removeProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_GRADES);
									}
									else
									{
										previousGrades = "";
									}
								}
								previousGrades =  "<tr><td style=\"padding:0 1em 0 0\">" + now.toStringLocalFull() +  "</td><td><span class=\"highlight\"><strong>" + sEdit.getGradeDisplay() + "</strong></span></td></tr>" +previousGrades;

								sPropertiesEdit.addProperty(ResourceProperties.PROP_SUBMISSION_SCALED_PREVIOUS_GRADES,
										previousGrades);
								*/
								// clear the current grade and make the submission ungraded
								sEdit.setGrade("");
								sEdit.setReturned(false);
	
								/*// keep the history of assignment feed back text
								String feedbackTextHistory = sPropertiesEdit
										.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT) != null ? sPropertiesEdit
										.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT)
										: "";
								feedbackTextHistory =  "<h4>" + now.toStringLocalFull() + "</h4>" + "<div style=\"margin:0;padding:0\">" + sEdit.getFeedbackText() + "</div>" + feedbackTextHistory;
								sPropertiesEdit.addProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_TEXT,
										feedbackTextHistory);
	
								// keep the history of assignment feed back comment
								String feedbackCommentHistory = sPropertiesEdit
										.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT) != null ? sPropertiesEdit
										.getProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT)
										: "";
								feedbackCommentHistory = "<h4>" + now.toStringLocalFull() + "</h4>" + "<div style=\"margin:0;padding:0\">" + sEdit.getFeedbackComment() + "</div>" + feedbackCommentHistory;
								sPropertiesEdit.addProperty(ResourceProperties.PROP_SUBMISSION_PREVIOUS_FEEDBACK_COMMENT,
										feedbackCommentHistory);
								
								// keep the history of assignment feed back comment
								String feedbackAttachmentHistory = sPropertiesEdit
										.getProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS) != null ? sPropertiesEdit
										.getProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS)
										: "";
										
								feedbackAttachmentHistory = att + feedbackAttachmentHistory;
									
								sPropertiesEdit.addProperty(PROP_SUBMISSION_PREVIOUS_FEEDBACK_ATTACHMENTS,
										feedbackAttachmentHistory);*/
	

								List feedbackAttachments = sEdit.getFeedbackAttachments();
								String att = "<h5>" +  now.toStringLocalFull() + "</h5>";
								for (int k = 0; k<feedbackAttachments.size();k++)
								{
									att = att + ((Reference) feedbackAttachments.get(k)).getReference() + "<br />";
								}
								
								// reset the previous grading context
								sEdit.setFeedbackText("");
								sEdit.setFeedbackComment("");
								sEdit.setFeedbackAttachments(new Vector());
	
								// decrease the allow_resubmit_number
								int number = submission.getNumSubmissionsAllowed().intValue();
								// minus 1 from the submit number
								if (number>=1)
								{
									submission.setNumSubmissionsAllowed(Integer.valueOf(number-1));
								}
								else if (number == -1)
								{
									submission.setNumSubmissionsAllowed(Integer.valueOf(-1));
								}
							}
	
							// add attachments
							List attachments = (List) state.getAttribute(ATTACHMENTS);
							if (attachments != null)
							{
								
								//Post the attachments before clearing so that we don't submit duplicate attachments
								//Check if we need to post the attachments
								if (a.getAllowReviewService().booleanValue()) {
									if (!attachments.isEmpty()) { 
										//TODO:zian
										//sEdit.postAttachment(attachments);
									}
								}
																 
								// clear the old attachments first
								sEdit.setSubmittedAttachments(attachments);
							}
	
							assignmentService.saveSubmissionVersion(sEdit);
						}
						catch (IdUnusedException e)
						{
							addAlert(state, rb.getString("cannotfin2") + " " + a.getTitle());
						}
						catch (PermissionException e)
						{
							addAlert(state, rb.getString("no_permissiion_to_edit_submission"));
						}
						catch (InUseException e)
						{
							addAlert(state, rb.getString("somelsis") + " " + rb.getString("submiss"));
						}
					}
					else
					{
						// new submission, post it
						try
						{
							// add submission object
							submission = assignmentService.newSubmission(assignmentId, SessionManager.getCurrentSessionUserId());
							submission.setAssignment(a);
							submission.setNumSubmissionsAllowed(a.getNumSubmissionsAllowed());
							assignmentService.saveSubmission(submission);
							
							AssignmentSubmissionVersion edit = assignmentService.newSubmissionVersion(submission.getReference());
							edit.setSubmittedText(text);
							edit.setHonorPledgeFlag(Boolean.valueOf(honorPledgeYes).booleanValue());
							edit.setTimeSubmitted(new Date());
							edit.setDraft(false);
	
							// add attachments
							List attachments = (List) state.getAttribute(ATTACHMENTS);
							if (attachments != null)
							{
	 							// add each attachment
								if ((!attachments.isEmpty()) && a.getAllowReviewService().booleanValue()) 
									edit.postAttachment(attachments);								
								
								// add each attachment
								edit.setSubmittedAttachments(attachments);
							}
	
							assignmentService.saveSubmissionVersion(edit);
						}
						catch (PermissionException e)
						{
							addAlert(state, rb.getString("youarenot13"));
						}
					} // if -else
				}
				catch (IdUnusedException e)
				{
					addAlert(state, rb.getString("cannotfin5"));
				}
				catch (PermissionException e)
				{
					addAlert(state, rb.getString("not_allowed_to_view"));
				}
	
			} // if
	
			if (state.getAttribute(STATE_MESSAGE) == null)
			{
				state.setAttribute(STATE_MODE, MODE_STUDENT_VIEW_SUBMISSION_CONFIRMATION);
			}
		}	// if

	} // doPost_submission
	
	/**
	 * Action is to confirm the submission and return to list view
	 */
	public void doConfirm_assignment_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());
	}
	/**
	 * Action is to show the new assignment screen
	 */
	public void doNew_assignment(RunData data, Context context)
	{
		
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		if (!alertGlobalNavigation(state, data))
		{
			if (assignmentService.allowAddAssignment((String) state.getAttribute(STATE_CONTEXT_STRING)))
			{
				resetAssignment(state);
				
				state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());
				state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT);
			}
			else
			{
				addAlert(state, rb.getString("youarenot2"));
				state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
			}
		}

	} // doNew_Assignment
	
	/**
	 * Action is to show the reorder assignment screen
	 */
	public void doReorder(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		
		// this insures the default order is loaded into the reordering tool
		state.setAttribute(SORTED_BY, SORTED_BY_DEFAULT);
		state.setAttribute(SORTED_ASC, Boolean.TRUE.toString());

		if (!alertGlobalNavigation(state, data))
		{
			if (assignmentService.allowAllGroups((String) state.getAttribute(STATE_CONTEXT_STRING)))
			{
				resetAssignment(state);
				
				state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());
				state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_REORDER_ASSIGNMENT);
			}
			else
			{
				addAlert(state, rb.getString("youarenot19"));
				state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
			}
		}

	} // doReorder

	/**
	 * Action is to save the input infos for assignment fields
	 *
	 * @param validify
	 *        Need to validify the inputs or not
	 */
	protected void setNewAssignmentParameters(RunData data, boolean validify)
	{
		// read the form inputs
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		// put the input value into the state attributes
		String title = params.getString(NEW_ASSIGNMENT_TITLE);
		state.setAttribute(NEW_ASSIGNMENT_TITLE, title);
		
		String order = params.getString(NEW_ASSIGNMENT_ORDER);
		state.setAttribute(NEW_ASSIGNMENT_ORDER, order);

		if (title.length() == 0)
		{
			// empty assignment title
			addAlert(state, rb.getString("plespethe1"));
		}

		// open time
		int openMonth = (new Integer(params.getString(NEW_ASSIGNMENT_OPENMONTH))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_OPENMONTH, new Integer(openMonth));
		int openDay = (new Integer(params.getString(NEW_ASSIGNMENT_OPENDAY))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_OPENDAY, new Integer(openDay));
		int openYear = (new Integer(params.getString(NEW_ASSIGNMENT_OPENYEAR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_OPENYEAR, new Integer(openYear));
		int openHour = (new Integer(params.getString(NEW_ASSIGNMENT_OPENHOUR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_OPENHOUR, new Integer(openHour));
		int openMin = (new Integer(params.getString(NEW_ASSIGNMENT_OPENMIN))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_OPENMIN, new Integer(openMin));
		String openAMPM = params.getString(NEW_ASSIGNMENT_OPENAMPM);
		state.setAttribute(NEW_ASSIGNMENT_OPENAMPM, openAMPM);
		if ((openAMPM.equals("PM")) && (openHour != 12))
		{
			openHour = openHour + 12;
		}
		if ((openHour == 12) && (openAMPM.equals("AM")))
		{
			openHour = 0;
		}
		Time openTime = TimeService.newTimeLocal(openYear, openMonth, openDay, openHour, openMin, 0, 0);
		// validate date
		if (!Validator.checkDate(openDay, openMonth, openYear))
		{
			addAlert(state, rb.getString("date.invalid") + rb.getString("date.opendate") + ".");
		}

		// due time
		int dueMonth = (new Integer(params.getString(NEW_ASSIGNMENT_DUEMONTH))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_DUEMONTH, new Integer(dueMonth));
		int dueDay = (new Integer(params.getString(NEW_ASSIGNMENT_DUEDAY))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_DUEDAY, new Integer(dueDay));
		int dueYear = (new Integer(params.getString(NEW_ASSIGNMENT_DUEYEAR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_DUEYEAR, new Integer(dueYear));
		int dueHour = (new Integer(params.getString(NEW_ASSIGNMENT_DUEHOUR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_DUEHOUR, new Integer(dueHour));
		int dueMin = (new Integer(params.getString(NEW_ASSIGNMENT_DUEMIN))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_DUEMIN, new Integer(dueMin));
		String dueAMPM = params.getString(NEW_ASSIGNMENT_DUEAMPM);
		state.setAttribute(NEW_ASSIGNMENT_DUEAMPM, dueAMPM);
		if ((dueAMPM.equals("PM")) && (dueHour != 12))
		{
			dueHour = dueHour + 12;
		}
		if ((dueHour == 12) && (dueAMPM.equals("AM")))
		{
			dueHour = 0;
		}
		Time dueTime = TimeService.newTimeLocal(dueYear, dueMonth, dueDay, dueHour, dueMin, 0, 0);
		
		// show alert message when due date is in past. Remove it after user confirms the choice.
		if (dueTime.before(TimeService.newTime()) && state.getAttribute(NEW_ASSIGNMENT_PAST_DUE_DATE) == null)
		{
			state.setAttribute(NEW_ASSIGNMENT_PAST_DUE_DATE, Boolean.TRUE);
		}
		else
		{
			// clean the attribute after user confirm
			state.removeAttribute(NEW_ASSIGNMENT_PAST_DUE_DATE);
		}
		if (state.getAttribute(NEW_ASSIGNMENT_PAST_DUE_DATE) != null)
		{
			addAlert(state, rb.getString("assig4"));
		}
		
		if (!dueTime.after(openTime))
		{
			addAlert(state, rb.getString("assig3"));
		}
		if (!Validator.checkDate(dueDay, dueMonth, dueYear))
		{
			addAlert(state, rb.getString("date.invalid") + rb.getString("date.duedate") + ".");
		}

		state.setAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE, new Boolean(true));

		// close time
		int closeMonth = (new Integer(params.getString(NEW_ASSIGNMENT_CLOSEMONTH))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_CLOSEMONTH, new Integer(closeMonth));
		int closeDay = (new Integer(params.getString(NEW_ASSIGNMENT_CLOSEDAY))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_CLOSEDAY, new Integer(closeDay));
		int closeYear = (new Integer(params.getString(NEW_ASSIGNMENT_CLOSEYEAR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_CLOSEYEAR, new Integer(closeYear));
		int closeHour = (new Integer(params.getString(NEW_ASSIGNMENT_CLOSEHOUR))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_CLOSEHOUR, new Integer(closeHour));
		int closeMin = (new Integer(params.getString(NEW_ASSIGNMENT_CLOSEMIN))).intValue();
		state.setAttribute(NEW_ASSIGNMENT_CLOSEMIN, new Integer(closeMin));
		String closeAMPM = params.getString(NEW_ASSIGNMENT_CLOSEAMPM);
		state.setAttribute(NEW_ASSIGNMENT_CLOSEAMPM, closeAMPM);
		if ((closeAMPM.equals("PM")) && (closeHour != 12))
		{
			closeHour = closeHour + 12;
		}
		if ((closeHour == 12) && (closeAMPM.equals("AM")))
		{
			closeHour = 0;
		}
		Time closeTime = TimeService.newTimeLocal(closeYear, closeMonth, closeDay, closeHour, closeMin, 0, 0);
		// validate date
		if (!Validator.checkDate(closeDay, closeMonth, closeYear))
		{
			addAlert(state, rb.getString("date.invalid") + rb.getString("date.closedate") + ".");
		}
		if (!closeTime.after(openTime))
		{
			addAlert(state, rb.getString("acesubdea3"));
		}
		if (closeTime.before(dueTime))
		{
			addAlert(state, rb.getString("acesubdea2"));
		}

		// SECTION MOD
		String sections_string = "";
		String mode = (String) state.getAttribute(STATE_MODE);
		if (mode == null) mode = "";

		state.setAttribute(NEW_ASSIGNMENT_SECTION, sections_string);
		state.setAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE, new Integer(params.getString(NEW_ASSIGNMENT_SUBMISSION_TYPE)));

		int gradeType = -1;

		// grade type and grade points
		if (state.getAttribute(WITH_GRADES) != null && ((Boolean) state.getAttribute(WITH_GRADES)).booleanValue())
		{
			gradeType = Integer.parseInt(params.getString(NEW_ASSIGNMENT_GRADE_TYPE));
			state.setAttribute(NEW_ASSIGNMENT_GRADE_TYPE, new Integer(gradeType));
		}

		
		String r = params.getString(NEW_ASSIGNMENT_USE_REVIEW_SERVICE);
		String b;
		// set whether we use the review service or not
		if (r == null) b = Boolean.FALSE.toString();
		else b = Boolean.TRUE.toString();
		state.setAttribute(NEW_ASSIGNMENT_USE_REVIEW_SERVICE, b);
		
		//set whether students can view the review service results
		r = params.getString(NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW);
		if (r == null) b = Boolean.FALSE.toString();
		else b = Boolean.TRUE.toString();
		state.setAttribute(NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW, b);
		
		// treat the new assignment description as formatted text
		boolean checkForFormattingErrors = true; // instructor is creating a new assignment - so check for errors
		String description = processFormattedTextFromBrowser(state, params.getCleanString(NEW_ASSIGNMENT_DESCRIPTION),
				checkForFormattingErrors);
		state.setAttribute(NEW_ASSIGNMENT_DESCRIPTION, description);

		if (state.getAttribute(CALENDAR) != null)
		{
			// calendar enabled for the site
			if (params.getString(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE) != null
					&& params.getString(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE).equalsIgnoreCase(Boolean.TRUE.toString()))
			{
				state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE, Boolean.TRUE.toString());
			}
			else
			{
				state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE, Boolean.FALSE.toString());
			}
		}
		else
		{
			// no calendar yet for the site
			state.removeAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE);
		}

		if (params.getString(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE) != null
				&& params.getString(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE)
						.equalsIgnoreCase(Boolean.TRUE.toString()))
		{
			state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE, Boolean.TRUE.toString());
		}
		else
		{
			state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE, Boolean.FALSE.toString());
		}

		String s = params.getString(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE);

		// set the honor pledge to be "no honor pledge"
		if (s == null) s = "1";
		state.setAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE, s);

		String grading = params.getString(NEW_ASSIGNMENT_ADD_TO_GRADEBOOK);
		state.setAttribute(NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, grading);

		// only when choose to associate with assignment in Gradebook
		String associateAssignment = params.getString(assignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);

		if (grading != null)
		{
			if (grading.equals(assignmentService.GRADEBOOK_INTEGRATION_ASSOCIATE))
			{
				state.setAttribute(assignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT, associateAssignment);
			}
			else
			{
				state.setAttribute(assignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT, "");
			}

			if (!grading.equals(assignmentService.GRADEBOOK_INTEGRATION_NO))
			{
				// gradebook integration only available to point-grade assignment
				if (gradeType != AssignmentConstants.SCORE_GRADE_TYPE)
				{
					addAlert(state, rb.getString("addtogradebook.wrongGradeScale"));
				}

				// if chosen as "associate", have to choose one assignment from Gradebook
				if (grading.equals(assignmentService.GRADEBOOK_INTEGRATION_ASSOCIATE) && StringUtil.trimToNull(associateAssignment) == null)
				{
					addAlert(state, rb.getString("grading.associate.alert"));
				}
			}
		}

		List attachments = (List) state.getAttribute(ATTACHMENTS);
		state.setAttribute(NEW_ASSIGNMENT_ATTACHMENT, attachments);

		if (validify)
		{
			if (((description == null) || (description.length() == 0)) && ((attachments == null || attachments.size() == 0)))
			{
				// if there is no description nor an attachment, show the following alert message.
				// One could ignore the message and still post the assignment
				if (state.getAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY) == null)
				{
					state.setAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY, Boolean.TRUE.toString());
				}
				else
				{
					state.removeAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY);
				}
			}
			else
			{
				state.removeAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY);
			}
		}

		if (validify && state.getAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY) != null)
		{
			addAlert(state, rb.getString("thiasshas"));
		}
		
		// assignment range?
		String range = data.getParameters().getString("range");
		state.setAttribute(NEW_ASSIGNMENT_RANGE, range);
		if (range.equals("groups"))
		{
			String[] groupChoice = data.getParameters().getStrings("selectedGroups");
			if (groupChoice != null && groupChoice.length != 0)
			{
				state.setAttribute(NEW_ASSIGNMENT_GROUPS, new ArrayList(Arrays.asList(groupChoice)));
			}
			else
			{
				state.setAttribute(NEW_ASSIGNMENT_GROUPS, null);
				addAlert(state, rb.getString("java.alert.youchoosegroup"));
			}
		}
		else
		{
			state.removeAttribute(NEW_ASSIGNMENT_GROUPS);
		}

		if (state.getAttribute(WITH_GRADES) != null && ((Boolean) state.getAttribute(WITH_GRADES)).booleanValue())
		{
			// the grade point
			String gradePoints = params.getString(NEW_ASSIGNMENT_GRADE_POINTS);
			state.setAttribute(NEW_ASSIGNMENT_GRADE_POINTS, gradePoints);
			if (gradePoints != null)
			{
				if (gradeType == 3)
				{
					if ((gradePoints.length() == 0))
					{
						// in case of point grade assignment, user must specify maximum grade point
						addAlert(state, rb.getString("plespethe3"));
					}
					else
					{
						validPointGrade(state, gradePoints);
						// when scale is points, grade must be integer and less than maximum value
						if (state.getAttribute(STATE_MESSAGE) == null)
						{
							gradePoints = scalePointGrade(state, gradePoints);
						}
						state.setAttribute(NEW_ASSIGNMENT_GRADE_POINTS, gradePoints);
					}
				}
			}
		}
		
		// allow resubmission numbers
		String nString = params.getString(AssignmentConstants.ALLOW_RESUBMIT_NUMBER);
		if (nString != null)
		{
			state.setAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER, nString);
		}
		
		// assignment notification option
		String notiOption = params.getString(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS);
		if (notiOption != null)
		{
			state.setAttribute(AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE, notiOption);
		}
	} // setNewAssignmentParameters

	/**
	 * Action is to hide the preview assignment student view
	 */
	public void doHide_submission_assignment_instruction(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_SUBMISSION_ASSIGNMENT_EXPAND_FLAG, new Boolean(false));

		// save user input
		readGradeForm(data, state, "read");

	} // doHide_preview_assignment_student_view

	/**
	 * Action is to show the preview assignment student view
	 */
	public void doShow_submission_assignment_instruction(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_SUBMISSION_ASSIGNMENT_EXPAND_FLAG, new Boolean(true));

		// save user input
		readGradeForm(data, state, "read");

	} // doShow_submission_assignment_instruction

	/**
	 * Action is to hide the preview assignment student view
	 */
	public void doHide_preview_assignment_student_view(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(PREVIEW_ASSIGNMENT_STUDENT_VIEW_HIDE_FLAG, new Boolean(true));

	} // doHide_preview_assignment_student_view

	/**
	 * Action is to show the preview assignment student view
	 */
	public void doShow_preview_assignment_student_view(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(PREVIEW_ASSIGNMENT_STUDENT_VIEW_HIDE_FLAG, new Boolean(false));

	} // doShow_preview_assignment_student_view

	/**
	 * Action is to hide the preview assignment assignment infos
	 */
	public void doHide_preview_assignment_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_HIDE_FLAG, new Boolean(true));

	} // doHide_preview_assignment_assignment

	/**
	 * Action is to show the preview assignment assignment info
	 */
	public void doShow_preview_assignment_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_HIDE_FLAG, new Boolean(false));

	} // doShow_preview_assignment_assignment

	/**
	 * Action is to hide the assignment option
	 */
	public void doHide_assignment_option(RunData data)
	{
		setNewAssignmentParameters(data, false);
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG, new Boolean(true));
		state.setAttribute(NEW_ASSIGNMENT_FOCUS, "eventSubmit_doShow_assignment_option");

	} // doHide_assignment_option

	/**
	 * Action is to show the assignment option
	 */
	public void doShow_assignment_option(RunData data)
	{
		setNewAssignmentParameters(data, false);
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG, new Boolean(false));
		state.setAttribute(NEW_ASSIGNMENT_FOCUS, NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE);

	} // doShow_assignment_option

	/**
	 * Action is to hide the assignment content in the view assignment page
	 */
	public void doHide_view_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_ASSIGNMENT_FLAG, new Boolean(true));

	} // doHide_view_assignment

	/**
	 * Action is to show the assignment content in the view assignment page
	 */
	public void doShow_view_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_ASSIGNMENT_FLAG, new Boolean(false));

	} // doShow_view_assignment

	/**
	 * Action is to hide the student view in the view assignment page
	 */
	public void doHide_view_student_view(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_STUDENT_VIEW_FLAG, new Boolean(true));

	} // doHide_view_student_view

	/**
	 * Action is to show the student view in the view assignment page
	 */
	public void doShow_view_student_view(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_STUDENT_VIEW_FLAG, new Boolean(false));

	} // doShow_view_student_view

	/**
	 * Action is to post assignment
	 */
	public void doPost_assignment(RunData data)
	{
		// post assignment
		postOrSaveAssignment(data, "post");

	} // doPost_assignment

	/**
	 * Action is to tag items via an items tagging helper
	 */
	public void doHelp_items(RunData data) {
		SessionState state = ((JetspeedRunData) data)
				.getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.taggable.api.TaggingManager");
		TaggingProvider provider = taggingManager.findProviderById(params
				.getString(PROVIDER_ID));

		String activityRef = params.getString(ACTIVITY_REF);

		TaggingHelperInfo helperInfo = provider
				.getItemsHelperInfo(activityRef);

		// get into helper mode with this helper tool
		startHelper(data.getRequest(), helperInfo.getHelperId());

		Map<String, ? extends Object> helperParms = helperInfo
				.getParameterMap();

		for (Iterator<String> keys = helperParms.keySet().iterator(); keys
				.hasNext();) {
			String key = keys.next();
			state.setAttribute(key, helperParms.get(key));
		}
	} // doHelp_items
	
	/**
	 * Action is to tag an individual item via an item tagging helper
	 */
	public void doHelp_item(RunData data) {
		SessionState state = ((JetspeedRunData) data)
				.getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.taggable.api.TaggingManager");
		TaggingProvider provider = taggingManager.findProviderById(params
				.getString(PROVIDER_ID));

		String itemRef = params.getString(ITEM_REF);

		TaggingHelperInfo helperInfo = provider
				.getItemHelperInfo(itemRef);

		// get into helper mode with this helper tool
		startHelper(data.getRequest(), helperInfo.getHelperId());

		Map<String, ? extends Object> helperParms = helperInfo
				.getParameterMap();

		for (Iterator<String> keys = helperParms.keySet().iterator(); keys
				.hasNext();) {
			String key = keys.next();
			state.setAttribute(key, helperParms.get(key));
		}
	} // doHelp_item
	
	/**
	 * Action is to tag an activity via an activity tagging helper
	 */
	public void doHelp_activity(RunData data) {
		SessionState state = ((JetspeedRunData) data)
				.getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.taggable.api.TaggingManager");
		TaggingProvider provider = taggingManager.findProviderById(params
				.getString(PROVIDER_ID));

		String activityRef = params.getString(ACTIVITY_REF);

		TaggingHelperInfo helperInfo = provider
				.getActivityHelperInfo(activityRef);

		// get into helper mode with this helper tool
		startHelper(data.getRequest(), helperInfo.getHelperId());

		Map<String, ? extends Object> helperParms = helperInfo
				.getParameterMap();

		for (String key : helperParms.keySet()) {
			state.setAttribute(key, helperParms.get(key));
		}
	} // doHelp_activity
	
	/**
	 * post or save assignment
	 */
	private void postOrSaveAssignment(RunData data, String postOrSave)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		
		ParameterParser params = data.getParameters();
		
		String siteId = (String) state.getAttribute(STATE_CONTEXT_STRING);
		
		boolean post = (postOrSave != null) && postOrSave.equals("post");

		// assignment old title
		String aOldTitle = null;

		// assignment old associated Gradebook entry if any
		String oAssociateGradebookAssignment = null;

		String mode = (String) state.getAttribute(STATE_MODE);
		if (!mode.equals(MODE_INSTRUCTOR_PREVIEW_ASSIGNMENT))
		{
			// read input data if the mode is not preview mode
			setNewAssignmentParameters(data, true);
		}
		
		String assignmentId = params.getString("assignmentId");
		
		// whether this is an editing which changes non-electronic assignment to any other type?
		boolean bool_change_from_non_electronic = false;

		if (state.getAttribute(STATE_MESSAGE) == null)
		{

			// Assignment
			Assignment a = getAssignment(state, assignmentId);
			
			bool_change_from_non_electronic = change_from_non_electronic(state, a);

			// put the names and values into vm file
			String title = (String) state.getAttribute(NEW_ASSIGNMENT_TITLE);
			String order = (String) state.getAttribute(NEW_ASSIGNMENT_ORDER);

			// open time
			Date openTime = getTime(state, NEW_ASSIGNMENT_OPENYEAR, NEW_ASSIGNMENT_OPENMONTH, NEW_ASSIGNMENT_OPENDAY, NEW_ASSIGNMENT_OPENHOUR, NEW_ASSIGNMENT_OPENMIN, NEW_ASSIGNMENT_OPENAMPM);

			// due time
			Date dueTime = getTime(state, NEW_ASSIGNMENT_DUEYEAR, NEW_ASSIGNMENT_DUEMONTH, NEW_ASSIGNMENT_DUEDAY, NEW_ASSIGNMENT_DUEHOUR, NEW_ASSIGNMENT_DUEMIN, NEW_ASSIGNMENT_DUEAMPM);
			

			// close time
			Date closeTime = dueTime;
			boolean enableCloseDate = ((Boolean) state.getAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE)).booleanValue();
			if (enableCloseDate)
			{
				closeTime = getTime(state, NEW_ASSIGNMENT_CLOSEYEAR, NEW_ASSIGNMENT_CLOSEMONTH, NEW_ASSIGNMENT_CLOSEDAY, NEW_ASSIGNMENT_CLOSEHOUR, NEW_ASSIGNMENT_CLOSEMIN, NEW_ASSIGNMENT_CLOSEAMPM);
				
			}

			// sections
			String section = (String) state.getAttribute(NEW_ASSIGNMENT_SECTION);

			int submissionType = ((Integer) state.getAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE)).intValue();

			int gradeType = ((Integer) state.getAttribute(NEW_ASSIGNMENT_GRADE_TYPE)).intValue();

			String gradePoints = (String) state.getAttribute(NEW_ASSIGNMENT_GRADE_POINTS);

			String description = (String) state.getAttribute(NEW_ASSIGNMENT_DESCRIPTION);

			String checkAddDueTime = state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE)!=null?(String) state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE):null;

			String checkAutoAnnounce = (String) state.getAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE);

			String checkAddHonorPledge = (String) state.getAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE);

			String addtoGradebook = state.getAttribute(NEW_ASSIGNMENT_ADD_TO_GRADEBOOK) != null?(String) state.getAttribute(NEW_ASSIGNMENT_ADD_TO_GRADEBOOK):"" ;

			String associateGradebookAssignment = (String) state.getAttribute(assignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);
			
			String allowResubmitNumber = state.getAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER) != null?(String) state.getAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER):null;
			
			boolean useReviewService = "true".equalsIgnoreCase((String) state.getAttribute(NEW_ASSIGNMENT_USE_REVIEW_SERVICE));
			
			boolean allowStudentViewReport = "true".equalsIgnoreCase((String) state.getAttribute(NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW));
			
			// the attachments
			List attachments = (List) state.getAttribute(ATTACHMENTS);
			List attachments1 = EntityManager.newReferenceList(attachments);
			
			// set group property
			String range = (String) state.getAttribute(NEW_ASSIGNMENT_RANGE);
			List<AssignmentGroup> groups = new Vector<AssignmentGroup>();
			try
			{
				Site site = SiteService.getSite(siteId);
				Collection groupChoice = (Collection) state.getAttribute(NEW_ASSIGNMENT_GROUPS);
				if (range.equals(AssignmentConstants.GROUPED) && (groupChoice == null || groupChoice.size() == 0))
				{
					// show alert if no group is selected for the group access assignment
					addAlert(state, rb.getString("java.alert.youchoosegroup"));
				}
				else if (groupChoice != null)
				{
					for (Iterator iGroups = groupChoice.iterator(); iGroups.hasNext();)
					{
						String groupId = (String) iGroups.next();
						AssignmentGroup aGroup = new AssignmentGroup();
						aGroup.setAssignment(a);
						aGroup.setGroupId(groupId);
						groups.add(aGroup);
					}
				}
			}
			catch (Exception e)
			{
				log.warn(this + e.getMessage());
			}


			if ((state.getAttribute(STATE_MESSAGE) == null) && (a != null))
			{
				aOldTitle = a.getTitle();
				// old open time
				Date oldOpenTime = a.getOpenTime();
				// old due time
				Date oldDueTime = a.getDueTime();
				
				// set the Assignment Properties object
				oAssociateGradebookAssignment = Long.toString(a.getGradableObjectId());
				editAssignmentProperties(a, checkAddDueTime, checkAutoAnnounce, addtoGradebook, associateGradebookAssignment, allowResubmitNumber, post);
				// the notification option
				if (state.getAttribute(AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE) != null)
				{
					a.setNotificationType(new Integer((String) state.getAttribute(AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE)));
				}
				
				// comment the changes to Assignment object
				commitAssignment(state, post, a, title, openTime, dueTime, closeTime, enableCloseDate, section, range, groups);
	
				if (state.getAttribute(STATE_MESSAGE) == null)
				{
					state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
					state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());
					resetAssignment(state);
				}

				if (post)
				{
					// only if user is posting the assignment
					if (a.getTypeOfSubmission() == AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
					{
						// update submissions
						updateNonElectronicSubmissions(state, a);
					}
					else if (bool_change_from_non_electronic)
					{
						// not non_electronic type any more
						List submissions = assignmentService.getSubmissions(a);
						if (submissions != null && submissions.size() >0)
						{
							// assignment already exist and with submissions
							for (Iterator iSubmissions = submissions.iterator(); iSubmissions.hasNext();)
							{
								AssignmentSubmission s = (AssignmentSubmission) iSubmissions.next();
								try
								{
									AssignmentSubmissionVersion sVersion = assignmentService.getSubmission(s.getReference()).getCurrentSubmissionVersion();
									sVersion.setDraft(true);
									sVersion.setTimeSubmitted(null);
									assignmentService.saveSubmissionVersion(sVersion);
								}
								catch (Exception e)
								{
									log.debug(this + e.getMessage() + s.getReference());
								}
							}
						}
								
					}
						

					// add the due date to schedule if the schedule exists
					integrateWithCalendar(state, a, title, dueTime, checkAddDueTime, oldDueTime);

					// the open date been announced
					integrateWithAnnouncement(state, aOldTitle, a, title, openTime, checkAutoAnnounce, oldOpenTime);

					// integrate with Gradebook
					try
					{
						initIntegrateWithGradebook(state, siteId, aOldTitle, oAssociateGradebookAssignment, a, title, dueTime, gradeType, gradePoints, addtoGradebook, associateGradebookAssignment, range);
					}
					catch (AssignmentHasIllegalPointsException e)
					{
						addAlert(state, rb.getString("addtogradebook.illegalPoints"));
					}
	
				} //if

			} // if

		} // if
		
		// set default sorting
		setDefaultSort(state);
		
	} // doPost_assignment

	/**
	 * 
	 */
	private boolean change_from_non_electronic(SessionState state, Assignment a) 
	{
		// whether this is an editing which changes non-electronic assignment to any other type?
		if (a != null)
		{
			// editing
			if (a.getTypeOfSubmission() == AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION
					&& ((Integer) state.getAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE)).intValue() != AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
			{
				// changing from non-electronic type
				return true;
			}
		}
		return false;
	}

	/**
	 * default sorting
	 */
	private void setDefaultSort(SessionState state) {
		state.setAttribute(SORTED_BY, SORTED_BY_DEFAULT);
		state.setAttribute(SORTED_ASC, Boolean.TRUE.toString());
	}

	/**
	 * Add submission objects if necessary for non-electronic type of assignment
	 * @param state
	 * @param a
	 */
	private void addRemoveSubmissionsForNonElectronicAssignment(SessionState state, List submissions, HashSet<String> addSubmissionForUsers, HashSet<String> removeSubmissionForUsers, Assignment a) 
	{
		// create submission object for those user who doesn't have one yet
		for (Iterator iUserIds = addSubmissionForUsers.iterator(); iUserIds.hasNext();)
		{
			String userId = (String) iUserIds.next();
			try
			{
				User u = UserDirectoryService.getUser(userId);
				// only include those users that can submit to this assignment
				if (u != null)
				{
					// construct fake submissions for grading purpose
					AssignmentSubmission submission = assignmentService.newSubmission(Long.toString(a.getId()), userId);
					assignmentService.saveSubmission(submission);
					
					AssignmentSubmissionVersion submissionVersion = assignmentService.newSubmissionVersion(submission.getReference());
					submissionVersion.setTimeSubmitted(new Date());
					submissionVersion.setDraft(false);
					assignmentService.saveSubmissionVersion(submissionVersion);
				}
			}
			catch (Exception e)
			{
				log.warn(this + e.toString() + "error adding submission for userId = " + userId);
			}
		}
		
		// remove submission object for those who no longer in the site
		for (Iterator iUserIds = removeSubmissionForUsers.iterator(); iUserIds.hasNext();)
		{
			String userId = (String) iUserIds.next();
			String submissionRef = null;
			// TODO: we don't have an efficient way to retrieve specific user's submission now, so until then, we still need to iterate the whole submission list
			for (Iterator iSubmissions=submissions.iterator(); iSubmissions.hasNext() && submissionRef == null;)
			{
				AssignmentSubmission submission = (AssignmentSubmission) iSubmissions.next();
				assignmentService.removeSubmission(submission);
			}
		}
		
	}
	
	private void initIntegrateWithGradebook(SessionState state, String siteId, String aOldTitle, String oAssociateGradebookAssignment, Assignment a, String title, Date dueTime, int gradeType, String gradePoints, String addtoGradebook, String associateGradebookAssignment, String range) {

		String context = (String) state.getAttribute(STATE_CONTEXT_STRING);
		boolean gradebookExists = isGradebookDefined();

		// only if the gradebook is defined
		if (gradebookExists)
		{
			GradebookService g = (GradebookService) (org.sakaiproject.service.gradebook.shared.GradebookService) ComponentManager.get("org.sakaiproject.service.gradebook.GradebookService");
			String gradebookUid = ToolManager.getInstance().getCurrentPlacement().getContext();
			
			String aReference = a.getReference();
			String addUpdateRemoveAssignment = "remove";
			if (!addtoGradebook.equals(assignmentService.GRADEBOOK_INTEGRATION_NO))
			{
				// if integrate with Gradebook
				if (!assignmentService.getAllowGroupAssignmentsInGradebook() && (range.equals("groups")))
				{
					// if grouped assignment is not allowed to add into Gradebook
					addAlert(state, rb.getString("java.alert.noGroupedAssignmentIntoGB"));
					String ref = "";
					try
					{
						ref = a.getReference();
						a.setAddedToGradebook(Boolean.FALSE.toString());
						a.setGradableObjectId(null);
						assignmentService.saveAssignment(a);
					}
					catch (Exception ignore)
					{
						// ignore the exception
						log.warn(rb.getString("cannotfin2") + ref);
					}
					integrateGradebook(state, aReference, associateGradebookAssignment, "remove", null, null, -1, null, null, null);
				}
				else
				{
					if (addtoGradebook.equals(assignmentService.GRADEBOOK_INTEGRATION_ADD))
					{
						addUpdateRemoveAssignment = assignmentService.GRADEBOOK_INTEGRATION_ADD;
					}
					else if (addtoGradebook.equals(assignmentService.GRADEBOOK_INTEGRATION_ASSOCIATE))
					{
						addUpdateRemoveAssignment = "update";
					}
	
					if (!addUpdateRemoveAssignment.equals("remove") && gradeType == 3)
					{
						try
						{
							integrateGradebook(state, aReference, associateGradebookAssignment, addUpdateRemoveAssignment, aOldTitle, title, Integer.parseInt (gradePoints), dueTime, null, null);
	
							// add all existing grades, if any, into Gradebook
							integrateGradebook(state, aReference, associateGradebookAssignment, null, null, null, -1, null, null, "update");
	
							// if the assignment has been assoicated with a different entry in gradebook before, remove those grades from the entry in Gradebook
							if (StringUtil.trimToNull(oAssociateGradebookAssignment) != null && !oAssociateGradebookAssignment.equals(associateGradebookAssignment))
							{
								// if the old assoicated assignment entry in GB is an external one, but doesn't have anything assoicated with it in Assignment tool, remove it
								removeNonAssociatedExternalGradebookEntry(context, a.getReference(), oAssociateGradebookAssignment,g, gradebookUid);
							}
						}
						catch (NumberFormatException nE)
						{
							alertInvalidPoint(state, gradePoints);
						}
					}
					else
					{
						integrateGradebook(state, aReference, associateGradebookAssignment, "remove", null, null, -1, null, null, null);
					}
				}
			}
			else
			{
				// need to remove the associated gradebook entry if 1) it is external and 2) no other assignment are associated with it
				removeNonAssociatedExternalGradebookEntry(context, a.getReference(), oAssociateGradebookAssignment,g, gradebookUid);
					
			}
		}
	}

	private void removeNonAssociatedExternalGradebookEntry(String context, String assignmentReference, String associateGradebookAssignment, GradebookService g, String gradebookUid) {
		boolean isExternalAssignmentDefined=g.isExternalAssignmentDefined(gradebookUid, associateGradebookAssignment);
		if (isExternalAssignmentDefined)
		{
			// iterate through all assignments currently in the site, see if any is associated with this GB entry
			Iterator i = assignmentService.getAssignmentsForContext(context);
			boolean found = false;
			while (!found && i.hasNext())
			{
				Assignment aI = (Assignment) i.next();
				String gbEntry = Long.toString(aI.getGradableObjectId());
				if (!aI.isDeleted() && gbEntry.equals(associateGradebookAssignment) && !aI.getReference().equals(assignmentReference))
				{
					found = true;
				}
			}
			// so if none of the assignment in this site is associated with the entry, remove the entry
			if (!found)
			{
				g.removeExternalAssessment(gradebookUid, associateGradebookAssignment);
			}
		}
	}

	private void integrateWithAnnouncement(SessionState state, String aOldTitle, Assignment a, String title, Date openTime, String checkAutoAnnounce, Date oldOpenTime) 
	{
		if (checkAutoAnnounce.equalsIgnoreCase(Boolean.TRUE.toString()))
		{
			AnnouncementChannel channel = (AnnouncementChannel) state.getAttribute(ANNOUNCEMENT_CHANNEL);
			if (channel != null)
			{
				// whether the assignment's title or open date has been updated
				boolean updatedTitle = false;
				boolean updatedOpenDate = false;
				
				boolean openDateAnnounced = a.getHasAnnouncement().booleanValue();
				String openDateAnnouncementId = a.getAnnouncementId();
				if (openDateAnnounced && openDateAnnouncementId != null)
				{
					try
					{
						AnnouncementMessage message = channel.getAnnouncementMessage(openDateAnnouncementId);
						if (!message.getAnnouncementHeader().getSubject().contains(title))/*whether title has been changed*/
						{
							updatedTitle = true;
						}
						if (!message.getBody().contains(openTime.toString())) /*whether open date has been changed*/
						{
							updatedOpenDate = true;
						}
					}
					catch (IdUnusedException e)
					{
						log.warn(e.getMessage());
					}
					catch (PermissionException e)
					{
						log.warn(e.getMessage());
					}
				}

				// need to create announcement message if assignment is added or assignment has been updated
				if (!openDateAnnounced || updatedTitle || updatedOpenDate)
				{
					try
					{
						AnnouncementMessageEdit message = channel.addAnnouncementMessage();
						AnnouncementMessageHeaderEdit header = message.getAnnouncementHeaderEdit();
						header.setDraft(/* draft */false);
						header.replaceAttachments(/* attachment */EntityManager.newReferenceList());
	
						if (!openDateAnnounced)
						{
							// making new announcement
							header.setSubject(/* subject */rb.getString("assig6") + " " + title);
						}
						else
						{
							// updated title
							header.setSubject(/* subject */rb.getString("assig5") + " " + title);
						}
						
						if (updatedOpenDate)
						{
							// revised assignment open date
							message.setBody(/* body */rb.getString("newope") + " "
									+ FormattedText.convertPlaintextToFormattedText(title) + " " + rb.getString("is") + " "
									+ openTime.toString() + ". ");
						}
						else
						{
							// assignment open date
							message.setBody(/* body */rb.getString("opedat") + " "
									+ FormattedText.convertPlaintextToFormattedText(title) + " " + rb.getString("is") + " "
									+ openTime.toString() + ". ");
						}
	
						// group information
						List<AssignmentGroup> aGroups = a.getGroups();
						if (aGroups != null && aGroups.size() > 0)
						{
							try
							{
								// get the group ids selected
								Collection groupRefs = a.getGroups();
	
								// make a collection of Group objects
								Collection groups = new Vector();
	
								//make a collection of Group objects from the collection of group ref strings
								Site site = SiteService.getSite((String) state.getAttribute(STATE_CONTEXT_STRING));
								for (Iterator iGroupRefs = groupRefs.iterator(); iGroupRefs.hasNext();)
								{
									String groupRef = (String) iGroupRefs.next();
									groups.add(site.getGroup(groupRef));
								}
	
								// set access
								header.setGroupAccess(groups);
							}
							catch (Exception exception)
							{
								// log
								log.warn(exception.getMessage());
							}
						}
						else
						{
							// site announcement
							header.clearGroupAccess();
						}
	
	
						channel.commitMessage(message, NotificationService.NOTI_NONE);
	
						// commit related properties into Assignment object
						String ref = "";
						try
						{
							ref = a.getReference();
							a.setHasAnnouncement(Boolean.TRUE);
							if (message != null)
							{
								a.setAnnouncementId(message.getId());
							}
							assignmentService.saveAssignment(a);
						}
						catch (Exception ignore)
						{
							// ignore the exception
							log.warn(rb.getString("cannotfin2") + ref);
						}
	
					}
					catch (PermissionException ee)
					{
						log.warn(rb.getString("cannotmak"));
					}
				}
			}
		} // if
	}

	private void integrateWithCalendar(SessionState state, Assignment a, String title, Date dueTime, String checkAddDueTime, Date oldDueTime) 
	{
		Calendar c = (Calendar) state.getAttribute(CALENDAR);
		if (c != null)
		{
			String oldEventId = a.getScheduleEventId();
			CalendarEvent e = null;

			if (a.getAddedToSchedule().booleanValue() || oldEventId != null)
			{
				// find the old event
				boolean found = false;
				if (oldEventId != null)
				{
					try
					{
						e = c.getEvent(oldEventId);
						found = true;
					}
					catch (IdUnusedException ee)
					{
						log.warn("The old event has been deleted: event id=" + oldEventId + ". ");
					}
					catch (PermissionException ee)
					{
						log.warn("You do not have the permission to view the schedule event id= "
								+ oldEventId + ".");
					}
				}
				else
				{
					TimeBreakdown b = dateBreakDown(oldDueTime);
					// TODO: check- this was new Time(year...), not local! -ggolden
					Time startTime = TimeService.newTimeLocal(b.getYear(), b.getMonth(), b.getDay(), 0, 0, 0, 0);
					Time endTime = TimeService.newTimeLocal(b.getYear(), b.getMonth(), b.getDay(), 23, 59, 59, 999);
					try
					{
						Iterator events = c.getEvents(TimeService.newTimeRange(startTime, endTime), null)
								.iterator();

						while ((!found) && (events.hasNext()))
						{
							e = (CalendarEvent) events.next();
							if (((String) e.getDisplayName()).indexOf(rb.getString("assig1") + " " + title) != -1)
							{
								found = true;
							}
						}
					}
					catch (PermissionException ignore)
					{
						// ignore PermissionException
					}
				}

				if (found)
				{
					// remove the founded old event
					try
					{
						c.removeEvent(c.getEditEvent(e.getId(), CalendarService.EVENT_REMOVE_CALENDAR));
					}
					catch (PermissionException ee)
					{
						log.warn(rb.getString("cannotrem") + " " + title + ". ");
					}
					catch (InUseException ee)
					{
						log.warn(rb.getString("somelsis") + " " + rb.getString("calen"));
					}
					catch (IdUnusedException ee)
					{
						log.warn(rb.getString("cannotfin6") + e.getId());
					}
				}
			}

			if (checkAddDueTime.equalsIgnoreCase(Boolean.TRUE.toString()))
			{
				// commit related properties into Assignment object
				String ref = "";
				try
				{
					ref = a.getReference();

					try
					{
						e = null;
						CalendarEvent.EventAccess eAccess = CalendarEvent.EventAccess.SITE;
						Collection eGroups = new Vector();

						List<AssignmentGroup> groups = a.getGroups();
						if (groups != null && groups.size() > 0)
						{
							eAccess = CalendarEvent.EventAccess.GROUPED;

							// make a collection of Group objects from the collection of group ref strings
							Site site = SiteService.getSite((String) state.getAttribute(STATE_CONTEXT_STRING));
							for (Iterator<AssignmentGroup> iGroups = groups.iterator(); iGroups.hasNext();)
							{
								AssignmentGroup aGroup = iGroups.next();
								String groupRef = aGroup.getGroupId();
								eGroups.add(site.getGroup(groupRef));
							}
						}
						e = c.addEvent(/* TimeRange */TimeService.newTimeRange(dueTime.getTime(), /* 0 duration */0 * 60 * 1000),
								/* title */rb.getString("due") + " " + title,
								/* description */rb.getString("assig1") + " " + title + " " + "is due on "
										+ dueTime.toString() + ". ",
								/* type */rb.getString("deadl"),
								/* location */"",
								/* access */ eAccess,
								/* groups */ eGroups,
								/* attachments */EntityManager.newReferenceList());

						a.setAddedToSchedule(Boolean.TRUE);
						if (e != null)
						{
							a.setScheduleEventId(e.getId());
						}
						
						// edit the calendar object and add an assignment id field
						CalendarEventEdit edit = c.getEditEvent(e.getId(), org.sakaiproject.calendar.api.CalendarService.EVENT_ADD_CALENDAR);
								
						edit.setField(NEW_ASSIGNMENT_DUEDATE_CALENDAR_ASSIGNMENT_ID, Long.toString(a.getId()));
						
						c.commitEvent(edit);
						
					}
					catch (IdUnusedException ee)
					{
						log.warn(ee.getMessage());
					}
					catch (PermissionException ee)
					{
						log.warn(rb.getString("cannotfin1"));
					}
					catch (Exception ee)
					{
						log.warn(ee.getMessage());
					}
					// try-catch


					assignmentService.saveAssignment(a);
				}
				catch (Exception ignore)
				{
					// ignore the exception
					log.warn(rb.getString("cannotfin2") + ref);
				}
			} // if
		}
	}
	
	private TimeBreakdown dateBreakDown(Date d)
	{
		return (TimeService.newTime(d.getTime())).breakdownLocal();
	}

	private void commitAssignment(SessionState state, boolean post, Assignment a, String title, Date openTime, Date dueTime, Date closeTime, boolean enableCloseDate, String s, String range, List<AssignmentGroup> groups) 
	{
		a.setTitle(title);
		a.setContext((String) state.getAttribute(STATE_CONTEXT_STRING));
		a.setOpenTime(openTime);
		a.setDueTime(dueTime);
		// set the drop dead date as the due date
		a.setCloseTime(dueTime);
		if (enableCloseDate)
		{
			a.setCloseTime(closeTime);
		}
		else
		{
			// if editing an old assignment with close date
			if (a.getCloseTime() != null)
			{
				a.setCloseTime(null);
			}
		}

		// post the assignment
		a.setDraft(!post);

		try
		{
			if (range.equals("site"))
			{
				a.setGroups(null);
			}
			else if (range.equals("groups"))
			{
				a.setGroups(groups);
			}
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot1"));
		}

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			// commit assignment first
			assignmentService.saveAssignment(a);
		}
	}

	private void editAssignmentProperties(Assignment a, String checkAddDueTime, String checkAutoAnnounce, String addtoGradebook, String associateGradebookAssignment, String allowResubmitNumber, boolean post) 
	{
		a.setAddedToSchedule(checkAddDueTime != null?Boolean.TRUE:Boolean.FALSE);
		a.setHasAnnouncement(new Boolean(checkAutoAnnounce));
		//TODO: zqiana.setAddedToGradebook(B)
		//aPropertiesEdit.addProperty(NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, addtoGradebook);
		a.setGradableObjectId(Long.valueOf(associateGradebookAssignment));
		if (post && addtoGradebook.equals(assignmentService.GRADEBOOK_INTEGRATION_ADD))
		{
			// if the choice is to add an entry into Gradebook, let just mark it as associated with such new entry then
			a.setAddedToGradebook(assignmentService.GRADEBOOK_INTEGRATION_ASSOCIATE);
			a.setGradableObjectId(Long.valueOf(a.getReference()));

		}
		
		// allow resubmit number
		if (allowResubmitNumber != null)
		{
			a.setResubmissionNumber(Integer.getInteger(allowResubmitNumber));
		}
	}

	/*private void commitAssignmentContentEdit(SessionState state, AssignmentContentEdit ac, String title, int submissionType,boolean useReviewService, boolean allowStudentViewReport, int gradeType, String gradePoints, String description, String checkAddHonorPledge, List attachments1) 
	{
		ac.setTitle(title);
		ac.setInstructions(description);
		ac.setHonorPledge(Integer.parseInt(checkAddHonorPledge));
		ac.setTypeOfSubmission(submissionType);
		ac.setAllowReviewService(useReviewService);
		ac.setAllowStudentViewReport(allowStudentViewReport);
		ac.setTypeOfGrade(gradeType);
		if (gradeType == 3)
		{
			try
			{
				ac.setMaxGradePoint(Integer.parseInt(gradePoints));
			}
			catch (NumberFormatException e)
			{
				alertInvalidPoint(state, gradePoints);
			}
		}
		ac.setGroupProject(true);
		ac.setIndividuallyGraded(false);

		if (submissionType != 1)
		{
			ac.setAllowAttachments(true);
		}
		else
		{
			ac.setAllowAttachments(false);
		}

		// clear attachments
		ac.clearAttachments();

		// add each attachment
		Iterator it = EntityManager.newReferenceList(attachments1).iterator();
		while (it.hasNext())
		{
			Reference r = (Reference) it.next();
			ac.addAttachment(r);
		}
		state.setAttribute(ATTACHMENTS_MODIFIED, new Boolean(false));

		// commit the changes
		assignmentService.commitEdit(ac);
	}*/
	
	/**
	 * reorderAssignments
	 */
	private void reorderAssignments(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();
		
		List assignments = prepPage(state);
		
		Iterator it = assignments.iterator();
		
		// temporarily allow the user to read and write from assignments (asn.revise permission)
        SecurityService.pushAdvisor(new SecurityAdvisor()
            {
                public SecurityAdvice isAllowed(String userId, String function, String reference)
                {
                    return SecurityAdvice.ALLOWED;
                }
            });
        
        while (it.hasNext()) // reads and writes the parameter for default ordering
        {
            Assignment a = (Assignment) it.next();
            String assignmentid = Long.toString(a.getId());
            String assignmentposition = params.getString("position_" + assignmentid);
            a.setPositionOrder((new Integer(assignmentposition)).intValue());
            assignmentService.saveAssignment(a);
        }
        
        // clear the permission
        SecurityService.clearAdvisors();
		
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
			state.setAttribute(ATTACHMENTS, EntityManager.newReferenceList());
			//resetAssignment(state);
		}
	} // reorderAssignments

	private Assignment getAssignment(SessionState state, String assignmentId) 
	{
		Assignment a = null;
		if (assignmentId.length() == 0)
		{
			// create a new assignment
			try
			{
				a = assignmentService.newAssignment((String) state.getAttribute(STATE_CONTEXT_STRING));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot1"));
			}
		}
		return a;
	}

	private Date getTime(SessionState state, String yearString, String monthString, String dayString, String hourString, String minString, String ampmString) 
	{
		int openMonth = ((Integer) state.getAttribute(monthString)).intValue();
		int openDay = ((Integer) state.getAttribute(dayString)).intValue();
		int openYear = ((Integer) state.getAttribute(yearString)).intValue();
		int openHour = ((Integer) state.getAttribute(hourString)).intValue();
		int openMin = ((Integer) state.getAttribute(minString)).intValue();
		String openAMPM = (String) state.getAttribute(ampmString);
		if ((openAMPM.equals("PM")) && (openHour != 12))
		{
			openHour = openHour + 12;
		}
		if ((openHour == 12) && (openAMPM.equals("AM")))
		{
			openHour = 0;
		}

		//Setting up Dates
    	java.util.Calendar cal = java.util.Calendar.getInstance();
    	cal.set(openYear, openMonth, openDay, openHour, openMin);
		return cal.getTime();
	}

	/**
	 * Action is to post new assignment
	 */
	public void doSave_assignment(RunData data)
	{
		postOrSaveAssignment(data, "save");

	} // doSave_assignment
	
	/**
	 * Action is to reorder assignments
	 */
	public void doReorder_assignment(RunData data)
	{
		reorderAssignments(data);
	} // doReorder_assignments

	/**
	 * Action is to preview the selected assignment
	 */
	public void doPreview_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		setNewAssignmentParameters(data, false);

		String assignmentId = data.getParameters().getString("assignmentId");
		state.setAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_ID, assignmentId);

		state.setAttribute(PREVIEW_ASSIGNMENT_ASSIGNMENT_HIDE_FLAG, new Boolean(false));
		state.setAttribute(PREVIEW_ASSIGNMENT_STUDENT_VIEW_HIDE_FLAG, new Boolean(true));
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_PREVIEW_ASSIGNMENT);
		}

	} // doPreview_assignment

	/**
	 * Action is to view the selected assignment
	 */
	public void doView_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		// show the assignment portion
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_ASSIGNMENT_FLAG, new Boolean(false));
		// show the student view portion
		state.setAttribute(VIEW_ASSIGNMENT_HIDE_STUDENT_VIEW_FLAG, new Boolean(true));

		String assignmentId = params.getString("assignmentId");
		state.setAttribute(VIEW_ASSIGNMENT_ID, assignmentId);

		try
		{
			Assignment a = assignmentService.getAssignment(assignmentId);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_VIEW_ASSIGNMENT);
		}

	} // doView_Assignment

	/**
	 * Action is for student to view one assignment content
	 */
	public void doView_assignment_as_student(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		String assignmentId = params.getString("assignmentId");
		state.setAttribute(VIEW_ASSIGNMENT_ID, assignmentId);

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_STUDENT_VIEW_ASSIGNMENT);
		}

	} // doView_assignment_as_student

	/**
	 * Action is to show the edit assignment screen
	 */
	public void doEdit_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		String assignmentId = StringUtil.trimToNull(params.getString("assignmentId"));
		// whether the user can modify the assignment
		state.setAttribute(EDIT_ASSIGNMENT_ID, assignmentId);

		try
		{
			Assignment a = assignmentService.getAssignment(assignmentId);
			// for the non_electronice assignment, submissions are auto-generated by the time that assignment is created;
			// don't need to go through the following checkings.
			if (a.getTypeOfSubmission() != AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
			{
				Iterator submissions = assignmentService.getSubmissions(a).iterator();
				if (submissions.hasNext())
				{
					// any submitted?
					boolean anySubmitted = false;
					for (;submissions.hasNext() && !anySubmitted;)
					{
						AssignmentSubmission s = (AssignmentSubmission) submissions.next();
						AssignmentSubmissionVersion sVersion = s.getCurrentSubmissionVersion();
						if (!sVersion.isDraft() && sVersion.getTimeSubmitted() != null)
						{
							anySubmitted = true;
						}
					}
					
					// any draft submission
					boolean anyDraft = false;
					for (;submissions.hasNext() && !anyDraft;)
					{
						AssignmentSubmission s = (AssignmentSubmission) submissions.next();
						AssignmentSubmissionVersion sVersion = s.getCurrentSubmissionVersion();
						if (sVersion.isDraft())
						{
							anyDraft = true;
						}
					}
					if (anySubmitted)
					{
						// if there is any submitted submission to this assignment, show alert
						addAlert(state, rb.getString("assig1") + " " + a.getTitle() + " " + rb.getString("hassum"));
					}
					
					if (anyDraft)
					{
						// otherwise, show alert about someone has started working on the assignment, not necessarily submitted
						addAlert(state, rb.getString("hasDraftSum"));
					}
				}
			}

			// put the names and values into vm file
			state.setAttribute(NEW_ASSIGNMENT_TITLE, a.getTitle());
			state.setAttribute(NEW_ASSIGNMENT_ORDER, a.getPositionOrder());
			TimeBreakdown openTime = dateBreakDown(a.getOpenTime());
			state.setAttribute(NEW_ASSIGNMENT_OPENMONTH, new Integer(openTime.getMonth()));
			state.setAttribute(NEW_ASSIGNMENT_OPENDAY, new Integer(openTime.getDay()));
			state.setAttribute(NEW_ASSIGNMENT_OPENYEAR, new Integer(openTime.getYear()));
			int openHour = openTime.getHour();
			if (openHour >= 12)
			{
				state.setAttribute(NEW_ASSIGNMENT_OPENAMPM, "PM");
			}
			else
			{
				state.setAttribute(NEW_ASSIGNMENT_OPENAMPM, "AM");
			}
			if (openHour == 0)
			{
				// for midnight point, we mark it as 12AM
				openHour = 12;
			}
			state.setAttribute(NEW_ASSIGNMENT_OPENHOUR, new Integer((openHour > 12) ? openHour - 12 : openHour));
			state.setAttribute(NEW_ASSIGNMENT_OPENMIN, new Integer(openTime.getMin()));

			TimeBreakdown dueTime = dateBreakDown(a.getDueTime());
			state.setAttribute(NEW_ASSIGNMENT_DUEMONTH, new Integer(dueTime.getMonth()));
			state.setAttribute(NEW_ASSIGNMENT_DUEDAY, new Integer(dueTime.getDay()));
			state.setAttribute(NEW_ASSIGNMENT_DUEYEAR, new Integer(dueTime.getYear()));
			int dueHour = dueTime.getHour();
			if (dueHour >= 12)
			{
				state.setAttribute(NEW_ASSIGNMENT_DUEAMPM, "PM");
			}
			else
			{
				state.setAttribute(NEW_ASSIGNMENT_DUEAMPM, "AM");
			}
			if (dueHour == 0)
			{
				// for midnight point, we mark it as 12AM
				dueHour = 12;
			}
			state.setAttribute(NEW_ASSIGNMENT_DUEHOUR, new Integer((dueHour > 12) ? dueHour - 12 : dueHour));
			state.setAttribute(NEW_ASSIGNMENT_DUEMIN, new Integer(dueTime.getMin()));
			// generate alert when editing an assignment past due date
			if (a.getDueTime().before(new Date()))
			{
				addAlert(state, rb.getString("youarenot17"));
			}

			if (a.getCloseTime() != null)
			{
				state.setAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE, new Boolean(true));
				TimeBreakdown closeTime = dateBreakDown(a.getCloseTime());
				state.setAttribute(NEW_ASSIGNMENT_CLOSEMONTH, new Integer(closeTime.getMonth()));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEDAY, new Integer(closeTime.getDay()));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEYEAR, new Integer(closeTime.getYear()));
				int closeHour = closeTime.getHour();
				if (closeHour >= 12)
				{
					state.setAttribute(NEW_ASSIGNMENT_CLOSEAMPM, "PM");
				}
				else
				{
					state.setAttribute(NEW_ASSIGNMENT_CLOSEAMPM, "AM");
				}
				if (closeHour == 0)
				{
					// for the midnight point, we mark it as 12 AM
					closeHour = 12;
				}
				state.setAttribute(NEW_ASSIGNMENT_CLOSEHOUR, new Integer((closeHour > 12) ? closeHour - 12 : closeHour));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEMIN, new Integer(closeTime.getMin()));
			}
			else
			{
				state.setAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE, new Boolean(false));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEMONTH, state.getAttribute(NEW_ASSIGNMENT_DUEMONTH));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEDAY, state.getAttribute(NEW_ASSIGNMENT_DUEDAY));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEYEAR, state.getAttribute(NEW_ASSIGNMENT_DUEYEAR));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEHOUR, state.getAttribute(NEW_ASSIGNMENT_DUEHOUR));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEMIN, state.getAttribute(NEW_ASSIGNMENT_DUEMIN));
				state.setAttribute(NEW_ASSIGNMENT_CLOSEAMPM, state.getAttribute(NEW_ASSIGNMENT_DUEAMPM));
			}

			state.setAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE, new Integer(a.getTypeOfSubmission()));
			int typeOfGrade = a.getTypeOfGrade();
			state.setAttribute(NEW_ASSIGNMENT_GRADE_TYPE, new Integer(typeOfGrade));
			if (typeOfGrade == 3)
			{
				state.setAttribute(NEW_ASSIGNMENT_GRADE_POINTS, a.getMaxGradePoint());
			}
			state.setAttribute(NEW_ASSIGNMENT_DESCRIPTION, a.getInstruction());
			
			state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE, a.getAddedToSchedule());
			state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE, a.getHasAnnouncement());
			state.setAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE, a.getHonorPledge());
			
			state.setAttribute(assignmentService.NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, a.getAddedToGradebook());
			state.setAttribute(assignmentService.PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT, a.getGradableObjectId());
			state.setAttribute(ATTACHMENTS, a.getAttachments());
			
			// notification option
			state.setAttribute(AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_VALUE, a.getNotificationType());

			// group setting
			if (a.getGroups() == null && a.getGroups().size() == 0)
			{
				state.setAttribute(NEW_ASSIGNMENT_RANGE, AssignmentConstants.SITE);
			}
			else
			{
				state.setAttribute(NEW_ASSIGNMENT_RANGE, AssignmentConstants.GROUPED);
			}
				
			state.setAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER, a.getResubmissionNumber() != null?a.getResubmissionNumber():"0");
			
			// set whether we use the review service or not
			state.setAttribute(NEW_ASSIGNMENT_USE_REVIEW_SERVICE, new Boolean(a.getAllowReviewService()).toString());
			
			//set whether students can view the review service results
			state.setAttribute(NEW_ASSIGNMENT_ALLOW_STUDENT_VIEW, new Boolean(a.getAllowStudentViewReport()).toString());
			
			
			state.setAttribute(NEW_ASSIGNMENT_GROUPS, a.getGroups());
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}

		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT);

	} // doEdit_Assignment

	/**
	 * Action is to show the delete assigment confirmation screen
	 */
	public void doDelete_confirm_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		String[] assignmentIds = params.getStrings("selectedAssignments");

		if (assignmentIds != null)
		{
			Vector ids = new Vector();
			for (int i = 0; i < assignmentIds.length; i++)
			{
				String id = (String) assignmentIds[i];
				if (!assignmentService.allowRemoveAssignment(id))
				{
					addAlert(state, rb.getString("youarenot9") + " " + id + ". ");
				}
				ids.add(id);
			}

			if (state.getAttribute(STATE_MESSAGE) == null)
			{
				// can remove all the selected assignments
				state.setAttribute(DELETE_ASSIGNMENT_IDS, ids);
				state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_DELETE_ASSIGNMENT);
			}
		}
		else
		{
			addAlert(state, rb.getString("youmust6"));
		}

	} // doDelete_confirm_Assignment

	/**
	 * Action is to delete the confirmed assignments
	 */
	public void doDelete_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// get the delete assignment ids
		Vector ids = (Vector) state.getAttribute(DELETE_ASSIGNMENT_IDS);
		for (int i = 0; i < ids.size(); i++)
		{

			String assignmentId = (String) ids.get(i);
			try
			{
				Assignment a = assignmentService.getAssignment(assignmentId);
				String associateGradebookAssignment = Long.toString(a.getGradableObjectId());
				String title = a.getTitle();

				// remove releted event if there is one
				if (a.getAddedToSchedule().booleanValue())
				{
					removeCalendarEvent(state, a, title);
				} // if-else

				if (a.getTypeOfSubmission() == AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
				{
					// if this is non-electronic submission, remove all the submissions
					List submissions = assignmentService.getSubmissions(a);
					if (submissions != null)
					{
						for (Iterator sIterator=submissions.iterator(); sIterator.hasNext();)
						{
							AssignmentSubmission s = (AssignmentSubmission) sIterator.next();
							try
							{
								assignmentService.removeSubmission(s);
							}
							catch (Exception eee)
							{
								addAlert(state, rb.getString("youarenot11_s") + " " + s.getReference() + ". ");
							}
						}
					}
					
					try
					{
						// remove the assignment afterwards
						assignmentService.removeAssignment(a);
					}
					catch (Exception ee)
					{
						addAlert(state, rb.getString("youarenot11") + " " + a.getTitle() + ". ");
					}

				}
				else
				{
					// for assignment with other type of submission
					if (!assignmentService.getSubmissions(a).iterator().hasNext())
					{	
						try
						{
							TaggingManager taggingManager = (TaggingManager) ComponentManager
									.get("org.sakaiproject.taggable.api.TaggingManager");
	
							AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
									.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");
	
							if (taggingManager.isTaggable()) {
								for (TaggingProvider provider : taggingManager
										.getProviders()) {
									provider.removeTags(assignmentActivityProducer
											.getActivity(a));
								}
							}
							
							assignmentService.removeAssignment(a);
						}
						catch (PermissionException ee)
						{
							addAlert(state, rb.getString("youarenot11") + " " + a.getTitle() + ". ");
						}
					}
					else
					{
						// remove the assignment by marking the remove status property true
						a.setDeleted(true);
	
						assignmentService.saveAssignment(a);
					}
				}

				// remove from Gradebook
				integrateGradebook(state, (String) ids.get (i), associateGradebookAssignment, "remove", null, null, -1, null, null, null);
			}
			catch (InUseException e)
			{
				addAlert(state, rb.getString("somelsis") + " " + rb.getString("assig2"));
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin3"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot6"));
			}
		} // for

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(DELETE_ASSIGNMENT_IDS, new Vector());

			state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
			
			// reset paging information after the assignment been deleted
			resetPaging(state);
		}

	} // doDelete_Assignment

	private void removeCalendarEvent(SessionState state, Assignment a, String title) throws PermissionException 
	{
		// remove the associated calender event
		Calendar c = (Calendar) state.getAttribute(CALENDAR);
		if (c != null)
		{
			// already has calendar object
			// get the old event
			CalendarEvent e = null;
			boolean found = false;
			String oldEventId = a.getScheduleEventId();
			if (oldEventId != null)
			{
				try
				{
					e = c.getEvent(oldEventId);
					found = true;
				}
				catch (IdUnusedException ee)
				{
					// no action needed for this condition
				}
				catch (PermissionException ee)
				{
				}
			}
			else
			{
				TimeBreakdown b = dateBreakDown(a.getDueTime());
				// TODO: check- this was new Time(year...), not local! -ggolden
				Time startTime = TimeService.newTimeLocal(b.getYear(), b.getMonth(), b.getDay(), 0, 0, 0, 0);
				Time endTime = TimeService.newTimeLocal(b.getYear(), b.getMonth(), b.getDay(), 23, 59, 59, 999);
				Iterator events = c.getEvents(TimeService.newTimeRange(startTime, endTime), null).iterator();
				while ((!found) && (events.hasNext()))
				{
					e = (CalendarEvent) events.next();
					if (((String) e.getDisplayName()).indexOf(rb.getString("assig1") + " " + title) != -1)
					{
						found = true;
					}
				}
			}
			// remove the founded old event
			if (found)
			{
				// found the old event delete it
				try
				{
					c.removeEvent(c.getEditEvent(e.getId(), CalendarService.EVENT_REMOVE_CALENDAR));
					a.setAddedToSchedule(false);
					a.setScheduleEventId(null);
				}
				catch (PermissionException ee)
				{
					log.warn(rb.getString("cannotrem") + " " + title + ". ");
				}
				catch (InUseException ee)
				{
					log.warn(rb.getString("somelsis") + " " + rb.getString("calen"));
				}
				catch (IdUnusedException ee)
				{
					log.warn(rb.getString("cannotfin6") + e.getId());
				}
			}
		}
	}

	/**
	 * Action is to delete the assignment and also the related AssignmentSubmission
	 */
	public void doDeep_delete_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// get the delete assignment ids
		Vector ids = (Vector) state.getAttribute(DELETE_ASSIGNMENT_IDS);
		for (int i = 0; i < ids.size(); i++)
		{
			String currentId = (String) ids.get(i);
			try
			{
				Assignment a = assignmentService.getAssignment(currentId);
				try
				{
					TaggingManager taggingManager = (TaggingManager) ComponentManager
							.get("org.sakaiproject.taggable.api.TaggingManager");

					AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
					.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");

					if (taggingManager.isTaggable()) {
						for (TaggingProvider provider : taggingManager
								.getProviders()) {
							provider.removeTags(assignmentActivityProducer
									.getActivity(a));
						}
					}
			
					assignmentService.removeAssignment(a);
				}
				catch (PermissionException e)
				{
					addAlert(state, rb.getString("youarenot11") + " " + a.getTitle() + ". ");
				}
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin3"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot14"));
			}
			catch (InUseException e)
			{
				addAlert(state, rb.getString("somelsis") + " " +  rb.getString("assig2"));
			}
		}
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(DELETE_ASSIGNMENT_IDS, new Vector());
			state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		}

	} // doDeep_delete_Assignment

	/**
	 * Action is to show the duplicate assignment screen
	 */
	public void doDuplicate_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// we are changing the view, so start with first page again.
		resetPaging(state);

		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		ParameterParser params = data.getParameters();
		String assignmentId = StringUtil.trimToNull(params.getString("assignmentId"));

		if (assignmentId != null)
		{
			try
			{
				Assignment a = assignmentService.addDuplicateAssignment(contextString, assignmentId);

				// clean the duplicate's property
				/*TODO:zqian
				 * ResourcePropertiesEdit aPropertiesEdit = a.getPropertiesEdit();
				aPropertiesEdit.removeProperty(NEW_ASSIGNMENT_DUE_DATE_SCHEDULED);
				aPropertiesEdit.removeProperty(ResourceProperties.PROP_ASSIGNMENT_DUEDATE_CALENDAR_EVENT_ID);
				aPropertiesEdit.removeProperty(NEW_ASSIGNMENT_OPEN_DATE_ANNOUNCED);
				aPropertiesEdit.removeProperty(ResourceProperties.PROP_ASSIGNMENT_OPENDATE_ANNOUNCEMENT_MESSAGE_ID);
				*/
				assignmentService.saveAssignment(a);
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot5"));
			}
			catch (IdInvalidException e)
			{
				addAlert(state, rb.getString("theassiid") + " " + assignmentId + " " + rb.getString("isnotval"));
			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("theassiid") + " " + assignmentId + " " + rb.getString("hasnotbee"));
			}
			catch (Exception e)
			{
			}

		}

	} // doDuplicate_Assignment

	/**
	 * Action is to show the grade submission screen
	 */
	public void doGrade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// reset the submission context
		resetViewSubmission(state);
		
		ParameterParser params = data.getParameters();

		// reset the grade assignment id
		state.setAttribute(GRADE_SUBMISSION_ASSIGNMENT_ID, params.getString("assignmentId"));
		state.setAttribute(GRADE_SUBMISSION_SUBMISSION_ID, params.getString("submissionId"));
		
		// allow resubmit number
		int allowResubmitNumber = 0;
		Assignment a = null;
		try
		{
			a = assignmentService.getAssignment((String) state.getAttribute(GRADE_SUBMISSION_ASSIGNMENT_ID));
			allowResubmitNumber= a.getResubmissionNumber();
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}

		try
		{
			AssignmentSubmission s = assignmentService.getSubmission((String) state.getAttribute(GRADE_SUBMISSION_SUBMISSION_ID));
			AssignmentSubmissionVersion sVersion = s.getCurrentSubmissionVersion();

			if ((sVersion.getFeedbackText() == null) || (sVersion.getFeedbackText().length() == 0))
			{
				state.setAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT, sVersion.getSubmittedText());
			}
			else
			{
				state.setAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT, sVersion.getFeedbackText());
			}
			state.setAttribute(GRADE_SUBMISSION_FEEDBACK_COMMENT, sVersion.getFeedbackComment());

			List v = EntityManager.newReferenceList();
			Iterator attachments = sVersion.getFeedbackAttachments().iterator();
			while (attachments.hasNext())
			{
				v.add(attachments.next());
			}
			state.setAttribute(ATTACHMENTS, v);

			state.setAttribute(GRADE_SUBMISSION_GRADE, sVersion.getGrade());
			
			allowResubmitNumber = s.getNumSubmissionsAllowed().intValue();
			
			state.setAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER, Integer.valueOf(allowResubmitNumber));
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			state.setAttribute(GRADE_SUBMISSION_ASSIGNMENT_EXPAND_FLAG, new Boolean(false));
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_SUBMISSION);
		}

	} // doGrade_submission

	/**
	 * Action is to release all the grades of the submission
	 */
	public void doRelease_grades(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		ParameterParser params = data.getParameters();

		try
		{
			// get the assignment
			Assignment a = assignmentService.getAssignment(params.getString("assignmentId"));

			String aReference = a.getReference();

			Iterator submissions = assignmentService.getSubmissions(a).iterator();
			while (submissions.hasNext())
			{
				AssignmentSubmission s = (AssignmentSubmission) submissions.next();
				AssignmentSubmissionVersion sVersion = s.getCurrentSubmissionVersion();
				if (sVersion.getGrade() != null)
				{
					String grade = sVersion.getGrade();
					
					boolean withGrade = state.getAttribute(WITH_GRADES) != null ? ((Boolean) state.getAttribute(WITH_GRADES))
							.booleanValue() : false;
					if (withGrade)
					{
						// for the assignment tool with grade option, a valide grade is needed
						if (grade != null && !grade.equals(""))
						{
							sVersion.setReturned(true);
						}
					}
					else
					{
						// for the assignment tool without grade option, no grade is needed
						sVersion.setReturned(true);
					}
					
					// also set the return status
					sVersion.setTimeReleased(new Date());
					sVersion.setHonorPledgeFlag(Boolean.FALSE.booleanValue());
					
					assignmentService.saveSubmissionVersion(sVersion);
				}

			} // while

			// add grades into Gradebook
			String integrateWithGradebook = a.getAddedToGradebook();
			if (integrateWithGradebook != null && !integrateWithGradebook.equals(assignmentService.GRADEBOOK_INTEGRATION_NO))
			{
				// integrate with Gradebook
				String associateGradebookAssignment = StringUtil.trimToNull(String.valueOf(a.getGradableObjectId()));

				integrateGradebook(state, aReference, associateGradebookAssignment, null, null, null, -1, null, null, "update");
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}
		catch (InUseException e)
		{
			addAlert(state, rb.getString("somelsis") + " " + rb.getString("submiss"));
		}

	} // doRelease_grades

	/**
	 * Action is to show the assignment in grading page
	 */
	public void doExpand_grade_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_ASSIGNMENT_EXPAND_FLAG, new Boolean(true));

	} // doExpand_grade_assignment

	/**
	 * Action is to hide the assignment in grading page
	 */
	public void doCollapse_grade_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_ASSIGNMENT_EXPAND_FLAG, new Boolean(false));

	} // doCollapse_grade_assignment

	/**
	 * Action is to show the submissions in grading page
	 */
	public void doExpand_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_SUBMISSION_EXPAND_FLAG, new Boolean(true));

	} // doExpand_grade_submission

	/**
	 * Action is to hide the submissions in grading page
	 */
	public void doCollapse_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(GRADE_SUBMISSION_EXPAND_FLAG, new Boolean(false));

	} // doCollapse_grade_submission

	/**
	 * Action is to show the grade assignment
	 */
	public void doGrade_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		// clean state attribute
		state.removeAttribute(USER_SUBMISSIONS);
		
		state.setAttribute(EXPORT_ASSIGNMENT_REF, params.getString("assignmentId"));

		try
		{
			Assignment a = assignmentService.getAssignment((String) state.getAttribute(EXPORT_ASSIGNMENT_REF));
			state.setAttribute(EXPORT_ASSIGNMENT_ID, a.getId());
			state.setAttribute(GRADE_ASSIGNMENT_EXPAND_FLAG, new Boolean(false));
			state.setAttribute(GRADE_SUBMISSION_EXPAND_FLAG, new Boolean(true));
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);

			// we are changing the view, so start with first page again.
			resetPaging(state);
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin3"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("youarenot14"));
		}
	} // doGrade_assignment

	/**
	 * Action is to show the View Students assignment screen
	 */
	public void doView_students_assignment(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT);

	} // doView_students_Assignment

	/**
	 * Action is to show the student submissions
	 */
	public void doShow_student_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		Set t = (Set) state.getAttribute(STUDENT_LIST_SHOW_TABLE);
		ParameterParser params = data.getParameters();

		String id = params.getString("studentId");
		// add the student id into the table
		t.add(id);

		state.setAttribute(STUDENT_LIST_SHOW_TABLE, t);

	} // doShow_student_submission

	/**
	 * Action is to hide the student submissions
	 */
	public void doHide_student_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		Set t = (Set) state.getAttribute(STUDENT_LIST_SHOW_TABLE);
		ParameterParser params = data.getParameters();

		String id = params.getString("studentId");
		// remove the student id from the table
		t.remove(id);

		state.setAttribute(STUDENT_LIST_SHOW_TABLE, t);

	} // doHide_student_submission

	/**
	 * Action is to show the graded assignment submission
	 */
	public void doView_grade(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		ParameterParser params = data.getParameters();

		state.setAttribute(VIEW_GRADE_SUBMISSION_ID, params.getString("submissionId"));

		state.setAttribute(STATE_MODE, MODE_STUDENT_VIEW_GRADE);

	} // doView_grade

	/**
	 * Action is to show the student submissions
	 */
	public void doReport_submissions(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_REPORT_SUBMISSIONS);
		state.setAttribute(SORTED_BY, SORTED_SUBMISSION_BY_LASTNAME);
		state.setAttribute(SORTED_ASC, Boolean.TRUE.toString());

	} // doReport_submissions

	/**
	 *
	 *
	 */
	public void doAssignment_form(RunData data)
	{
		ParameterParser params = data.getParameters();

		String option = (String) params.getString("option");
		if (option != null)
		{
			if (option.equals("post"))
			{
				// post assignment
				doPost_assignment(data);
			}
			else if (option.equals("save"))
			{
				// save assignment
				doSave_assignment(data);
			}
			else if (option.equals("reorder"))
			{
				// reorder assignments
				doReorder_assignment(data);
			}
			else if (option.equals("preview"))
			{
				// preview assignment
				doPreview_assignment(data);
			}
			else if (option.equals("cancel"))
			{
				// cancel creating assignment
				doCancel_new_assignment(data);
			}
			else if (option.equals("canceledit"))
			{
				// cancel editing assignment
				doCancel_edit_assignment(data);
			}
			else if (option.equals("attach"))
			{
				// attachments
				doAttachments(data);
			}
			else if (option.equals("view"))
			{
				// view
				doView(data);
			}
			else if (option.equals("permissions"))
			{
				// permissions
				doPermissions(data);
			}
			else if (option.equals("returngrade"))
			{
				// return grading
				doReturn_grade_submission(data);
			}
			else if (option.equals("savegrade"))
			{
				// save grading
				doSave_grade_submission(data);
			}
			else if (option.equals("previewgrade"))
			{
				// preview grading
				doPreview_grade_submission(data);
			}
			else if (option.equals("cancelgrade"))
			{
				// cancel grading
				doCancel_grade_submission(data);
			}
			else if (option.equals("cancelreorder"))
			{
				// cancel reordering
				doCancel_reorder(data);
			}
			else if (option.equals("sortbygrouptitle"))
			{
				// read input data
				setNewAssignmentParameters(data, true);

				// sort by group title
				doSortbygrouptitle(data);
			}
			else if (option.equals("sortbygroupdescription"))
			{
				// read input data
				setNewAssignmentParameters(data, true);

				// sort group by description
				doSortbygroupdescription(data);
			}
			else if (option.equals("hide_instruction"))
			{
				// hide the assignment instruction
				doHide_submission_assignment_instruction(data);
			}
			else if (option.equals("show_instruction"))
			{
				// show the assignment instruction
				doShow_submission_assignment_instruction(data);
			}
			else if (option.equals("sortbygroupdescription"))
			{
				// show the assignment instruction
				doShow_submission_assignment_instruction(data);
			}
			else if (option.equals("revise") || option.equals("done"))
			{
				// back from the preview mode
				doDone_preview_new_assignment(data);
			}


		}
	}

	/**
	 * Action is to use when doAattchmentsadding requested, corresponding to chef_Assignments-new "eventSubmit_doAattchmentsadding" when "add attachments" is clicked
	 */
	public void doAttachments(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();

		String mode = (String) state.getAttribute(STATE_MODE);
		if (mode.equals(MODE_STUDENT_VIEW_SUBMISSION))
		{
			// retrieve the submission text (as formatted text)
			boolean checkForFormattingErrors = true; // the student is submitting something - so check for errors
			String text = processFormattedTextFromBrowser(state, params.getCleanString(VIEW_SUBMISSION_TEXT),
					checkForFormattingErrors);

			state.setAttribute(VIEW_SUBMISSION_TEXT, text);
			if (params.getString(VIEW_SUBMISSION_HONOR_PLEDGE_YES) != null)
			{
				state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, "true");
			}
			// TODO: file picker to save in dropbox? -ggolden
			// User[] users = { UserDirectoryService.getCurrentUser() };
			// state.setAttribute(ResourcesAction.STATE_SAVE_ATTACHMENT_IN_DROPBOX, users);
		}
		else if (mode.equals(MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT))
		{
			setNewAssignmentParameters(data, false);
		}
		else if (mode.equals(MODE_INSTRUCTOR_GRADE_SUBMISSION))
		{
			readGradeForm(data, state, "read");
		}

		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			// get into helper mode with this helper tool
			startHelper(data.getRequest(), "sakai.filepicker");

			state.setAttribute(FilePickerHelper.FILE_PICKER_TITLE_TEXT, rb.getString("gen.addatttoassig"));
			state.setAttribute(FilePickerHelper.FILE_PICKER_INSTRUCTION_TEXT, rb.getString("gen.addatttoassiginstr"));

			// use the real attachment list
			state.setAttribute(FilePickerHelper.FILE_PICKER_ATTACHMENTS, state.getAttribute(ATTACHMENTS));
		}
	}

	/**
	 * readGradeForm
	 */
	public void readGradeForm(RunData data, SessionState state, String gradeOption)
	{

		ParameterParser params = data.getParameters();
		int typeOfGrade = -1;

		boolean withGrade = state.getAttribute(WITH_GRADES) != null ? ((Boolean) state.getAttribute(WITH_GRADES)).booleanValue()
				: false;

		boolean checkForFormattingErrors = false; // so that grading isn't held up by formatting errors
		String feedbackComment = processFormattedTextFromBrowser(state, params.getCleanString(GRADE_SUBMISSION_FEEDBACK_COMMENT),
				checkForFormattingErrors);
		if (feedbackComment != null)
		{
			state.setAttribute(GRADE_SUBMISSION_FEEDBACK_COMMENT, feedbackComment);
		}

		String feedbackText = processAssignmentFeedbackFromBrowser(state, params.getCleanString(GRADE_SUBMISSION_FEEDBACK_TEXT));
		if (feedbackText != null)
		{
			state.setAttribute(GRADE_SUBMISSION_FEEDBACK_TEXT, feedbackText);
		}
		
		state.setAttribute(GRADE_SUBMISSION_FEEDBACK_ATTACHMENT, state.getAttribute(ATTACHMENTS));

		String g = params.getCleanString(GRADE_SUBMISSION_GRADE);
		if (g != null)
		{
			state.setAttribute(GRADE_SUBMISSION_GRADE, g);
		}

		String sId = (String) state.getAttribute(GRADE_SUBMISSION_SUBMISSION_ID);

		try
		{
			// for points grading, one have to enter number as the points
			String grade = (String) state.getAttribute(GRADE_SUBMISSION_GRADE);

			Assignment a = assignmentService.getSubmission(sId).getAssignment();
			typeOfGrade = a.getTypeOfGrade();

			if (withGrade)
			{
				// do grade validation only for Assignment with Grade tool
				if (typeOfGrade == AssignmentConstants.SCORE_GRADE_TYPE)
				{
					if ((grade.length() == 0))
					{
						state.setAttribute(GRADE_SUBMISSION_GRADE, grade);
					}
					else
					{
						// the preview grade process might already scaled up the grade by 10
						if (!((String) state.getAttribute(STATE_MODE)).equals(MODE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION))
						{
							validPointGrade(state, grade);
							
							if (state.getAttribute(STATE_MESSAGE) == null)
							{
								int maxGrade = a.getMaxGradePoint();
								try
								{
									if (Integer.parseInt(scalePointGrade(state, grade)) > maxGrade)
									{
										if (state.getAttribute(GRADE_GREATER_THAN_MAX_ALERT) == null)
										{
											// alert user first when he enters grade bigger than max scale
											addAlert(state, rb.getString("grad2"));
											state.setAttribute(GRADE_GREATER_THAN_MAX_ALERT, Boolean.TRUE);
										}
										else
										{
											// remove the alert once user confirms he wants to give student higher grade
											state.removeAttribute(GRADE_GREATER_THAN_MAX_ALERT);
										}
									}
								}
								catch (NumberFormatException e)
								{
									alertInvalidPoint(state, grade);
								}
							}
							
							state.setAttribute(GRADE_SUBMISSION_GRADE, grade);
						}
					}
				}

				// if ungraded and grade type is not "ungraded" type
				if ((grade == null || grade.equals("ungraded")) && (typeOfGrade != AssignmentConstants.UNGRADED_GRADE_TYPE) && gradeOption.equals("release"))
				{
					addAlert(state, rb.getString("plespethe2"));
				}
			}
		}
		catch (IdUnusedException e)
		{
			addAlert(state, rb.getString("cannotfin5"));
		}
		catch (PermissionException e)
		{
			addAlert(state, rb.getString("not_allowed_to_view"));
		}
		
		// allow resubmit number and due time
		if (params.getString(AssignmentConstants.ALLOW_RESUBMIT_NUMBER) != null)
		{
			String allowResubmitNumberString = params.getString(AssignmentConstants.ALLOW_RESUBMIT_NUMBER);
			state.setAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER, params.getString(AssignmentConstants.ALLOW_RESUBMIT_NUMBER));
		
			if (Integer.parseInt(allowResubmitNumberString) != 0)
			{
				int closeMonth = (new Integer(params.getString(ALLOW_RESUBMIT_CLOSEMONTH))).intValue();
				state.setAttribute(ALLOW_RESUBMIT_CLOSEMONTH, new Integer(closeMonth));
				int closeDay = (new Integer(params.getString(ALLOW_RESUBMIT_CLOSEDAY))).intValue();
				state.setAttribute(ALLOW_RESUBMIT_CLOSEDAY, new Integer(closeDay));
				int closeYear = (new Integer(params.getString(ALLOW_RESUBMIT_CLOSEYEAR))).intValue();
				state.setAttribute(ALLOW_RESUBMIT_CLOSEYEAR, new Integer(closeYear));
				int closeHour = (new Integer(params.getString(ALLOW_RESUBMIT_CLOSEHOUR))).intValue();
				state.setAttribute(ALLOW_RESUBMIT_CLOSEHOUR, new Integer(closeHour));
				int closeMin = (new Integer(params.getString(ALLOW_RESUBMIT_CLOSEMIN))).intValue();
				state.setAttribute(ALLOW_RESUBMIT_CLOSEMIN, new Integer(closeMin));
				String closeAMPM = params.getString(ALLOW_RESUBMIT_CLOSEAMPM);
				state.setAttribute(ALLOW_RESUBMIT_CLOSEAMPM, closeAMPM);
				if ((closeAMPM.equals("PM")) && (closeHour != 12))
				{
					closeHour = closeHour + 12;
				}
				if ((closeHour == 12) && (closeAMPM.equals("AM")))
				{
					closeHour = 0;
				}
				Time closeTime = TimeService.newTimeLocal(closeYear, closeMonth, closeDay, closeHour, closeMin, 0, 0);
				state.setAttribute(AssignmentConstants.ALLOW_RESUBMIT_CLOSETIME, String.valueOf(closeTime.getTime()));
				// validate date
				if (closeTime.before(TimeService.newTime()) && state.getAttribute(NEW_ASSIGNMENT_PAST_CLOSE_DATE) == null)
				{
					state.setAttribute(NEW_ASSIGNMENT_PAST_CLOSE_DATE, Boolean.TRUE);
				}
				else
				{
					// clean the attribute after user confirm
					state.removeAttribute(NEW_ASSIGNMENT_PAST_CLOSE_DATE);
				}
				if (state.getAttribute(NEW_ASSIGNMENT_PAST_CLOSE_DATE) != null)
				{
					addAlert(state, rb.getString("acesubdea4"));
				}
				if (!Validator.checkDate(closeDay, closeMonth, closeYear))
				{
					addAlert(state, rb.getString("date.invalid") + rb.getString("date.closedate") + ".");
				}
			}
			else
			{
				// reset the state attributes
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEMONTH);
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEDAY);
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEYEAR);
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEHOUR);
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEMIN);
				state.removeAttribute(ALLOW_RESUBMIT_CLOSEAMPM);
				state.removeAttribute(AssignmentConstants.ALLOW_RESUBMIT_CLOSETIME);
			}
		}
		
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			String grade = (String) state.getAttribute(GRADE_SUBMISSION_GRADE);
			grade = (typeOfGrade == AssignmentConstants.SCORE_GRADE_TYPE)?scalePointGrade(state, grade):grade;
			state.setAttribute(GRADE_SUBMISSION_GRADE, grade);
		}
	}

	/**
	 * Populate the state object, if needed - override to do something!
	 */
	protected void initState(SessionState state, VelocityPortlet portlet, JetspeedRunData data)
	{
		super.initState(state, portlet, data);
		
		if (m_contentHostingService == null)
		{
			m_contentHostingService = (ContentHostingService) ComponentManager.get("org.sakaiproject.content.api.ContentHostingService");
		}

		String siteId = ToolManager.getCurrentPlacement().getContext();

		// show the list of assignment view first
		if (state.getAttribute(STATE_SELECTED_VIEW) == null)
		{
			state.setAttribute(STATE_SELECTED_VIEW, MODE_LIST_ASSIGNMENTS);
		}

		if (state.getAttribute(STATE_USER) == null)
		{
			state.setAttribute(STATE_USER, UserDirectoryService.getCurrentUser());
		}

		/** The content type image lookup service in the State. */
		ContentTypeImageService iService = (ContentTypeImageService) state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE);
		if (iService == null)
		{
			iService = org.sakaiproject.content.cover.ContentTypeImageService.getInstance();
			state.setAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE, iService);
		} // if

		/** The calendar tool  */
		if (state.getAttribute(CALENDAR_TOOL_EXIST) == null)
		{
			if (!siteHasTool(siteId, "sakai.schedule"))
			{
				state.setAttribute(CALENDAR_TOOL_EXIST, Boolean.FALSE);
				state.removeAttribute(CALENDAR);
			}
			else
			{
				state.setAttribute(CALENDAR_TOOL_EXIST, Boolean.TRUE);
				if (state.getAttribute(CALENDAR) == null )
				{
					state.setAttribute(CALENDAR_TOOL_EXIST, Boolean.TRUE);

					CalendarService cService = org.sakaiproject.calendar.cover.CalendarService.getInstance();
					state.setAttribute(STATE_CALENDAR_SERVICE, cService);
	
					String calendarId = ServerConfigurationService.getString("calendar", null);
					if (calendarId == null)
					{
						calendarId = cService.calendarReference(siteId, SiteService.MAIN_CONTAINER);
						try
						{
							state.setAttribute(CALENDAR, cService.getCalendar(calendarId));
						}
						catch (IdUnusedException e)
						{
							log.info(this + "No calendar found for site " + siteId);
							state.removeAttribute(CALENDAR);
						}
						catch (PermissionException e)
						{
							log.info(this + "No permission to get the calender. ");
							state.removeAttribute(CALENDAR);
						}
						catch (Exception ex)
						{
							log.info(this + "Assignment : Action : init state : calendar exception : " + ex);
							state.removeAttribute(CALENDAR);
						}
					}
				}
			}
		}
			

		/** The Announcement tool  */
		if (state.getAttribute(ANNOUNCEMENT_TOOL_EXIST) == null)
		{
			if (!siteHasTool(siteId, "sakai.announcements"))
			{
				state.setAttribute(ANNOUNCEMENT_TOOL_EXIST, Boolean.FALSE);
				state.removeAttribute(ANNOUNCEMENT_CHANNEL);
			}
			else
			{
				state.setAttribute(ANNOUNCEMENT_TOOL_EXIST, Boolean.TRUE);
				if (state.getAttribute(ANNOUNCEMENT_CHANNEL) == null )
				{
					/** The announcement service in the State. */
					AnnouncementService aService = (AnnouncementService) state.getAttribute(STATE_ANNOUNCEMENT_SERVICE);
					if (aService == null)
					{
						aService = org.sakaiproject.announcement.cover.AnnouncementService.getInstance();
						state.setAttribute(STATE_ANNOUNCEMENT_SERVICE, aService);
			
						String channelId = ServerConfigurationService.getString("channel", null);
						if (channelId == null)
						{
							channelId = aService.channelReference(siteId, SiteService.MAIN_CONTAINER);
							try
							{
								state.setAttribute(ANNOUNCEMENT_CHANNEL, aService.getAnnouncementChannel(channelId));
							}
							catch (IdUnusedException e)
							{
								log.warn("No announcement channel found. ");
								state.removeAttribute(ANNOUNCEMENT_CHANNEL);
							}
							catch (PermissionException e)
							{
								log.warn("No permission to annoucement channel. ");
							}
							catch (Exception ex)
							{
								log.warn("Assignment : Action : init state : calendar exception : " + ex);
							}
						}
					}
				}
			}
		} // if

		if (state.getAttribute(STATE_CONTEXT_STRING) == null)
		{
			state.setAttribute(STATE_CONTEXT_STRING, siteId);
		} // if context string is null

		if (state.getAttribute(SORTED_BY) == null)
		{
			setDefaultSort(state);
		}

		if (state.getAttribute(SORTED_GRADE_SUBMISSION_BY) == null)
		{
			state.setAttribute(SORTED_GRADE_SUBMISSION_BY, SORTED_GRADE_SUBMISSION_BY_LASTNAME);
		}

		if (state.getAttribute(SORTED_GRADE_SUBMISSION_ASC) == null)
		{
			state.setAttribute(SORTED_GRADE_SUBMISSION_ASC, Boolean.TRUE.toString());
		}

		if (state.getAttribute(SORTED_SUBMISSION_BY) == null)
		{
			state.setAttribute(SORTED_SUBMISSION_BY, SORTED_SUBMISSION_BY_LASTNAME);
		}

		if (state.getAttribute(SORTED_SUBMISSION_ASC) == null)
		{
			state.setAttribute(SORTED_SUBMISSION_ASC, Boolean.TRUE.toString());
		}

		if (state.getAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG) == null)
		{
			resetAssignment(state);
		}

		if (state.getAttribute(STUDENT_LIST_SHOW_TABLE) == null)
		{
			state.setAttribute(STUDENT_LIST_SHOW_TABLE, new HashSet());
		}

		if (state.getAttribute(ATTACHMENTS_MODIFIED) == null)
		{
			state.setAttribute(ATTACHMENTS_MODIFIED, new Boolean(false));
		}

		// SECTION MOD
		if (state.getAttribute(STATE_SECTION_STRING) == null)
		{
			
			state.setAttribute(STATE_SECTION_STRING, "001");
		}

		// // setup the observer to notify the Main panel
		// if (state.getAttribute(STATE_OBSERVER) == null)
		// {
		// // the delivery location for this tool
		// String deliveryId = clientWindowId(state, portlet.getID());
		//
		// // the html element to update on delivery
		// String elementId = mainPanelUpdateId(portlet.getID());
		//
		// // the event resource reference pattern to watch for
		// String pattern = assignmentService.assignmentReference((String) state.getAttribute (STATE_CONTEXT_STRING), "");
		//
		// state.setAttribute(STATE_OBSERVER, new MultipleEventsObservingCourier(deliveryId, elementId, pattern));
		// }

		if (state.getAttribute(STATE_MODE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_LIST_ASSIGNMENTS);
		}

		if (state.getAttribute(STATE_TOP_PAGE_MESSAGE) == null)
		{
			state.setAttribute(STATE_TOP_PAGE_MESSAGE, new Integer(0));
		}

		if (state.getAttribute(WITH_GRADES) == null)
		{
			PortletConfig config = portlet.getPortletConfig();
			String withGrades = StringUtil.trimToNull(config.getInitParameter("withGrades"));
			if (withGrades == null)
			{
				withGrades = Boolean.FALSE.toString();
			}
			state.setAttribute(WITH_GRADES, new Boolean(withGrades));
		}
		
		// whether the choice of emails instructor submission notification is available in the installation
		if (state.getAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS) == null)
		{
			state.setAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS, Boolean.valueOf(ServerConfigurationService.getBoolean(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS, true)));
		}
		
		// whether or how the instructor receive submission notification emails, none(default)|each|digest
		if (state.getAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DEFAULT) == null)
		{
			state.setAttribute(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DEFAULT, ServerConfigurationService.getString(ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DEFAULT, AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_NONE));
		}
		
		if (state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_FROM) == null)
		{
			state.setAttribute(NEW_ASSIGNMENT_YEAR_RANGE_FROM, new Integer(2002));
		}
		
		if (state.getAttribute(NEW_ASSIGNMENT_YEAR_RANGE_TO) == null)
		{
			state.setAttribute(NEW_ASSIGNMENT_YEAR_RANGE_TO, new Integer(2012));
		}
	} // initState


	/**
	 * whether the site has the specified tool
	 * @param siteId
	 * @return
	 */
	private boolean siteHasTool(String siteId, String toolId) {
		boolean rv = false;
		try
		{
			Site s = SiteService.getSite(siteId);
			if (s.getToolForCommonId(toolId) != null)
			{
				rv = true;
			}
		}
		catch (Exception e)
		{
			log.info(this + e.getMessage() + siteId);
		}
		return rv;
	}

	/**
	 * reset the attributes for view submission
	 */
	private void resetViewSubmission(SessionState state)
	{
		state.removeAttribute(VIEW_SUBMISSION_ASSIGNMENT_REFERENCE);
		state.removeAttribute(VIEW_SUBMISSION_TEXT);
		state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, "false");
		state.removeAttribute(GRADE_GREATER_THAN_MAX_ALERT);

	} // resetViewSubmission

	/**
	 * reset the attributes for view submission
	 */
	private void resetAssignment(SessionState state)
	{
		// put the input value into the state attributes
		state.setAttribute(NEW_ASSIGNMENT_TITLE, "");

		// get current time
		Time t = TimeService.newTime();
		TimeBreakdown tB = t.breakdownLocal();
		int month = tB.getMonth();
		int day = tB.getDay();
		int year = tB.getYear();

		// set the open time to be 12:00 PM
		state.setAttribute(NEW_ASSIGNMENT_OPENMONTH, new Integer(month));
		state.setAttribute(NEW_ASSIGNMENT_OPENDAY, new Integer(day));
		state.setAttribute(NEW_ASSIGNMENT_OPENYEAR, new Integer(year));
		state.setAttribute(NEW_ASSIGNMENT_OPENHOUR, new Integer(12));
		state.setAttribute(NEW_ASSIGNMENT_OPENMIN, new Integer(0));
		state.setAttribute(NEW_ASSIGNMENT_OPENAMPM, "PM");

		// due date is shifted forward by 7 days
		t.setTime(t.getTime() + 7 * 24 * 60 * 60 * 1000);
		tB = t.breakdownLocal();
		month = tB.getMonth();
		day = tB.getDay();
		year = tB.getYear();

		// set the due time to be 5:00pm
		state.setAttribute(NEW_ASSIGNMENT_DUEMONTH, new Integer(month));
		state.setAttribute(NEW_ASSIGNMENT_DUEDAY, new Integer(day));
		state.setAttribute(NEW_ASSIGNMENT_DUEYEAR, new Integer(year));
		state.setAttribute(NEW_ASSIGNMENT_DUEHOUR, new Integer(5));
		state.setAttribute(NEW_ASSIGNMENT_DUEMIN, new Integer(0));
		state.setAttribute(NEW_ASSIGNMENT_DUEAMPM, "PM");

		// enable the close date by default
		state.setAttribute(NEW_ASSIGNMENT_ENABLECLOSEDATE, new Boolean(true));
		// set the close time to be 5:00 pm, same as the due time by default
		state.setAttribute(NEW_ASSIGNMENT_CLOSEMONTH, new Integer(month));
		state.setAttribute(NEW_ASSIGNMENT_CLOSEDAY, new Integer(day));
		state.setAttribute(NEW_ASSIGNMENT_CLOSEYEAR, new Integer(year));
		state.setAttribute(NEW_ASSIGNMENT_CLOSEHOUR, new Integer(5));
		state.setAttribute(NEW_ASSIGNMENT_CLOSEMIN, new Integer(0));
		state.setAttribute(NEW_ASSIGNMENT_CLOSEAMPM, "PM");

		state.setAttribute(NEW_ASSIGNMENT_SECTION, "001");
		state.setAttribute(NEW_ASSIGNMENT_SUBMISSION_TYPE, new Integer(AssignmentConstants.TEXT_AND_ATTACHMENT_ASSIGNMENT_SUBMISSION));
		state.setAttribute(NEW_ASSIGNMENT_GRADE_TYPE, new Integer(AssignmentConstants.UNGRADED_GRADE_TYPE));
		state.setAttribute(NEW_ASSIGNMENT_GRADE_POINTS, "");
		state.setAttribute(NEW_ASSIGNMENT_DESCRIPTION, "");
		state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_ADD_DUE_DATE, Boolean.FALSE.toString());
		state.setAttribute(ResourceProperties.NEW_ASSIGNMENT_CHECK_AUTO_ANNOUNCE, Boolean.FALSE.toString());
		// make the honor pledge not include as the default
		state.setAttribute(NEW_ASSIGNMENT_CHECK_ADD_HONOR_PLEDGE, (new Integer(AssignmentConstants.HONOR_PLEDGE_NONE)).toString());

		state.setAttribute(NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, assignmentService.GRADEBOOK_INTEGRATION_NO);

		state.setAttribute(NEW_ASSIGNMENT_ATTACHMENT, EntityManager.newReferenceList());

		state.setAttribute(NEW_ASSIGNMENT_HIDE_OPTION_FLAG, new Boolean(false));

		state.setAttribute(NEW_ASSIGNMENT_FOCUS, NEW_ASSIGNMENT_TITLE);

		state.removeAttribute(NEW_ASSIGNMENT_DESCRIPTION_EMPTY);

		// reset the global navigaion alert flag
		if (state.getAttribute(ALERT_GLOBAL_NAVIGATION) != null)
		{
			state.removeAttribute(ALERT_GLOBAL_NAVIGATION);
		}

		state.removeAttribute(NEW_ASSIGNMENT_RANGE);
		state.removeAttribute(NEW_ASSIGNMENT_GROUPS);

		// remove the edit assignment id if any
		state.removeAttribute(EDIT_ASSIGNMENT_ID);
		
		// remove the resubmit number
		state.removeAttribute(AssignmentConstants.ALLOW_RESUBMIT_NUMBER);

	} // resetNewAssignment

	/**
	 * construct a Hashtable using integer as the key and three character string of the month as the value
	 */
	private Hashtable monthTable()
	{
		Hashtable n = new Hashtable();
		n.put(new Integer(1), rb.getString("jan"));
		n.put(new Integer(2), rb.getString("feb"));
		n.put(new Integer(3), rb.getString("mar"));
		n.put(new Integer(4), rb.getString("apr"));
		n.put(new Integer(5), rb.getString("may"));
		n.put(new Integer(6), rb.getString("jun"));
		n.put(new Integer(7), rb.getString("jul"));
		n.put(new Integer(8), rb.getString("aug"));
		n.put(new Integer(9), rb.getString("sep"));
		n.put(new Integer(10), rb.getString("oct"));
		n.put(new Integer(11), rb.getString("nov"));
		n.put(new Integer(12), rb.getString("dec"));
		return n;

	} // monthTable

	/**
	 * construct a Hashtable using the integer as the key and grade type String as the value
	 */
	private Hashtable gradeTypeTable()
	{
		Hashtable n = new Hashtable();
		n.put(new Integer(2), rb.getString("letter"));
		n.put(new Integer(3), rb.getString("points"));
		n.put(new Integer(4), rb.getString("pass"));
		n.put(new Integer(5), rb.getString("check"));
		n.put(new Integer(1), rb.getString("ungra"));
		return n;

	} // gradeTypeTable

	/**
	 * construct a Hashtable using the integer as the key and submission type String as the value
	 */
	private Hashtable submissionTypeTable()
	{
		Hashtable n = new Hashtable();
		n.put(new Integer(1), rb.getString("inlin"));
		n.put(new Integer(2), rb.getString("attaonly"));
		n.put(new Integer(3), rb.getString("inlinatt"));
		n.put(new Integer(4), rb.getString("nonelec"));
		return n;

	} // submissionTypeTable

	/**
	 * Sort based on the given property
	 */
	public void doSort(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// we are changing the sort, so start from the first page again
		resetPaging(state);

		setupSort(data, data.getParameters().getString("criteria"));
	}

	/**
	 * setup sorting parameters
	 *
	 * @param criteria
	 *        String for sortedBy
	 */
	private void setupSort(RunData data, String criteria)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// current sorting sequence
		String asc = "";
		if (!criteria.equals(state.getAttribute(SORTED_BY)))
		{
			state.setAttribute(SORTED_BY, criteria);
			asc = Boolean.TRUE.toString();
			state.setAttribute(SORTED_ASC, asc);
		}
		else
		{
			// current sorting sequence
			asc = (String) state.getAttribute(SORTED_ASC);

			// toggle between the ascending and descending sequence
			if (asc.equals(Boolean.TRUE.toString()))
			{
				asc = Boolean.FALSE.toString();
			}
			else
			{
				asc = Boolean.TRUE.toString();
			}
			state.setAttribute(SORTED_ASC, asc);
		}

	} // doSort

	/**
	 * Do sort by group title
	 */
	public void doSortbygrouptitle(RunData data)
	{
		setupSort(data, SORTED_BY_GROUP_TITLE);

	} // doSortbygrouptitle

	/**
	 * Do sort by group description
	 */
	public void doSortbygroupdescription(RunData data)
	{
		setupSort(data, SORTED_BY_GROUP_DESCRIPTION);

	} // doSortbygroupdescription

	/**
	 * Sort submission based on the given property
	 */
	public void doSort_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// we are changing the sort, so start from the first page again
		resetPaging(state);

		// get the ParameterParser from RunData
		ParameterParser params = data.getParameters();

		String criteria = params.getString("criteria");

		// current sorting sequence
		String asc = "";

		if (!criteria.equals(state.getAttribute(SORTED_SUBMISSION_BY)))
		{
			state.setAttribute(SORTED_SUBMISSION_BY, criteria);
			asc = Boolean.TRUE.toString();
			state.setAttribute(SORTED_SUBMISSION_ASC, asc);
		}
		else
		{
			// current sorting sequence
			state.setAttribute(SORTED_SUBMISSION_BY, criteria);
			asc = (String) state.getAttribute(SORTED_SUBMISSION_ASC);

			// toggle between the ascending and descending sequence
			if (asc.equals(Boolean.TRUE.toString()))
			{
				asc = Boolean.FALSE.toString();
			}
			else
			{
				asc = Boolean.TRUE.toString();
			}
			state.setAttribute(SORTED_SUBMISSION_ASC, asc);
		}
	} // doSort_submission


	
	
	/**
	 * Sort submission based on the given property in instructor grade view
	 */
	public void doSort_grade_submission(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// we are changing the sort, so start from the first page again
		resetPaging(state);

		// get the ParameterParser from RunData
		ParameterParser params = data.getParameters();

		String criteria = params.getString("criteria");

		// current sorting sequence
		String asc = "";

		if (!criteria.equals(state.getAttribute(SORTED_GRADE_SUBMISSION_BY)))
		{
			state.setAttribute(SORTED_GRADE_SUBMISSION_BY, criteria);
			//for content review default is desc
			if (criteria.equals(SORTED_GRADE_SUBMISSION_CONTENTREVIEW))
				asc = Boolean.FALSE.toString();
			else
				asc = Boolean.TRUE.toString();
			
			state.setAttribute(SORTED_GRADE_SUBMISSION_ASC, asc);
		}
		else
		{
			// current sorting sequence
			state.setAttribute(SORTED_GRADE_SUBMISSION_BY, criteria);
			asc = (String) state.getAttribute(SORTED_GRADE_SUBMISSION_ASC);

			// toggle between the ascending and descending sequence
			if (asc.equals(Boolean.TRUE.toString()))
			{
				asc = Boolean.FALSE.toString();
			}
			else
			{
				asc = Boolean.TRUE.toString();
			}
			state.setAttribute(SORTED_GRADE_SUBMISSION_ASC, asc);
		}
	} // doSort_grade_submission

	public void doSort_tags(RunData data) {
		SessionState state = ((JetspeedRunData) data)
				.getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		ParameterParser params = data.getParameters();

		String criteria = params.getString("criteria");
		String providerId = params.getString(PROVIDER_ID);
		
		String savedText = params.getString("savedText");		
		state.setAttribute(VIEW_SUBMISSION_TEXT, savedText);

		String mode = (String) state.getAttribute(STATE_MODE);
		
		List<DecoratedTaggingProvider> providers = (List) state
				.getAttribute(mode + PROVIDER_LIST);

		for (DecoratedTaggingProvider dtp : providers) {
			if (dtp.getProvider().getId().equals(providerId)) {
				Sort sort = dtp.getSort();
				if (sort.getSort().equals(criteria)) {
					sort.setAscending(sort.isAscending() ? false : true);
				} else {
					sort.setSort(criteria);
					sort.setAscending(true);
				}
				break;
			}
		}
	}
	
	public void doPage_tags(RunData data) {
		SessionState state = ((JetspeedRunData) data)
				.getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		ParameterParser params = data.getParameters();

		String page = params.getString("page");
		String pageSize = params.getString("pageSize");
		String providerId = params.getString(PROVIDER_ID);

		String savedText = params.getString("savedText");		
		state.setAttribute(VIEW_SUBMISSION_TEXT, savedText);

		String mode = (String) state.getAttribute(STATE_MODE);
		
		List<DecoratedTaggingProvider> providers = (List) state
				.getAttribute(mode + PROVIDER_LIST);

		for (DecoratedTaggingProvider dtp : providers) {
			if (dtp.getProvider().getId().equals(providerId)) {
				Pager pager = dtp.getPager();
				pager.setPageSize(Integer.valueOf(pageSize));
				if (Pager.FIRST.equals(page)) {
					pager.setFirstItem(0);
				} else if (Pager.PREVIOUS.equals(page)) {
					pager.setFirstItem(pager.getFirstItem()
							- pager.getPageSize());
				} else if (Pager.NEXT.equals(page)) {
					pager.setFirstItem(pager.getFirstItem()
							+ pager.getPageSize());
				} else if (Pager.LAST.equals(page)) {
					pager.setFirstItem((pager.getTotalItems() / pager
							.getPageSize())
							* pager.getPageSize());
				}
				break;			
			}
		}
	}
	
	/**
	 * the UserSubmission clas
	 */
	public class UserSubmission
	{
		/**
		 * the User object
		 */
		User m_user = null;

		/**
		 * the AssignmentSubmission object
		 */
		AssignmentSubmission m_submission = null;

		public UserSubmission(User u, AssignmentSubmission s)
		{
			m_user = u;
			m_submission = s;
		}

		/**
		 * Returns the AssignmentSubmission object
		 */
		public AssignmentSubmission getSubmission()
		{
			return m_submission;
		}

		/**
		 * Returns the User object
		 */
		public User getUser()
		{
			return m_user;
		}
	}

	/**
	 * the AssignmentComparator clas
	 */
	private class AssignmentComparator implements Comparator
	{
		Collator collator = Collator.getInstance();
		
		/**
		 * the SessionState object
		 */
		SessionState m_state = null;

		/**
		 * the criteria
		 */
		String m_criteria = null;

		/**
		 * the criteria
		 */
		String m_asc = null;

		/**
		 * the user
		 */
		User m_user = null;

		/**
		 * constructor
		 *
		 * @param state
		 *        The state object
		 * @param criteria
		 *        The sort criteria string
		 * @param asc
		 *        The sort order string. TRUE_STRING if ascending; "false" otherwise.
		 */
		public AssignmentComparator(SessionState state, String criteria, String asc)
		{
			m_state = state;
			m_criteria = criteria;
			m_asc = asc;

		} // constructor

		/**
		 * constructor
		 *
		 * @param state
		 *        The state object
		 * @param criteria
		 *        The sort criteria string
		 * @param asc
		 *        The sort order string. TRUE_STRING if ascending; "false" otherwise.
		 * @param user
		 *        The user object
		 */
		public AssignmentComparator(SessionState state, String criteria, String asc, User user)
		{
			m_state = state;
			m_criteria = criteria;
			m_asc = asc;
			m_user = user;
		} // constructor

		/**
		 * caculate the range string for an assignment
		 */
		private String getAssignmentRange(Assignment a)
		{
			String rv = "";
			List<AssignmentGroup> groups = a.getGroups();
			if (a.getGroups().equals(AssignmentConstants.SITE))
			{
				// site assignment
				rv = rb.getString("range.allgroups");
			}
			else
			{
				try
				{
					// get current site
					Site site = SiteService.getSite(ToolManager.getCurrentPlacement().getContext());
					for (Iterator k = a.getGroups().iterator(); k.hasNext();)
					{
						// announcement by group
						rv = rv.concat(site.getGroup((String) k.next()).getTitle());
					}
				}
				catch (Exception ignore)
				{
				}
			}

			return rv;

		} // getAssignmentRange

		/**
		 * implementing the compare function
		 *
		 * @param o1
		 *        The first object
		 * @param o2
		 *        The second object
		 * @return The compare result. 1 is o1 < o2; -1 otherwise
		 */
		public int compare(Object o1, Object o2)
		{
			int result = -1;
			
			if (m_criteria == null)
			{
				m_criteria = SORTED_BY_DEFAULT;
			}

			/** *********** for sorting assignments ****************** */
			if (m_criteria.equals(SORTED_BY_DEFAULT))
			{
				int s1 = ((Assignment) o1).getPositionOrder();
				int s2 = ((Assignment) o2).getPositionOrder();
				
				if ( s1 == s2 ) // we either have 2 assignments with no existing postion_order or a numbering error, so sort by duedate
				{
					// sorted by the assignment due date
					Date t1 = ((Assignment) o1).getDueTime();
					Date t2 = ((Assignment) o2).getDueTime();

					if (t1 == null)
					{
						result = -1;
					}
					else if (t2 == null)
					{
						result = 1;
					}
					else if (t1.equals(t2))
					{
						t1 = ((Assignment) o1).getCreatedTime();
						t2 = ((Assignment) o2).getCreatedTime();
					}
					
					if (t1!=null && t2!=null && t1.before(t2))
					{
						result = -1;
					}
					else
					{
						result = 1;
					}
				}				
				else if ( s1 == 0 && s2 > 0 ) // order has not been set on this object, so put it at the bottom of the list
				{
					result = 1;
				}
				else if ( s2 == 0 && s1 > 0 ) // making sure assignments with no position_order stay at the bottom
				{
					result = -1;
				}
				else // 2 legitimate postion orders
				{
					result = (s1 < s2) ? -1 : 1;
				}
			}
			if (m_criteria.equals(SORTED_BY_TITLE))
			{
				// sorted by the assignment title
				String s1 = ((Assignment) o1).getTitle();
				String s2 = ((Assignment) o2).getTitle();
				result = compareString(s1, s2);
			}
			else if (m_criteria.equals(SORTED_BY_DUEDATE))
			{
				// sorted by the assignment due date
				Date t1 = ((Assignment) o1).getDueTime();
				Date t2 = ((Assignment) o2).getDueTime();

				if (t1 == null)
				{
					result = -1;
				}
				else if (t2 == null)
				{
					result = 1;
				}
				else if (t1.before(t2))
				{
					result = -1;
				}
				else
				{
					result = 1;
				}
			}
			else if (m_criteria.equals(SORTED_BY_OPENDATE))
			{
				// sorted by the assignment open
				Date t1 = ((Assignment) o1).getOpenTime();
				Date t2 = ((Assignment) o2).getOpenTime();

				if (t1 == null)
				{
					result = -1;
				}
				else if (t2 == null)
				{
					result = 1;
				}
				if (t1.before(t2))
				{
					result = -1;
				}
				else
				{
					result = 1;
				}
			}
			else if (m_criteria.equals(SORTED_BY_ASSIGNMENT_STATUS))
			{
				// TODO:
				result = compareString(assignmentService.getAssignmentStatus((Assignment) o1), assignmentService.getAssignmentStatus((Assignment) o2));
			}
			else if (m_criteria.equals(SORTED_BY_NUM_SUBMISSIONS))
			{
				// sort by numbers of submissions

				// initialize
				int subNum1 = 0;
				int subNum2 = 0;

				Iterator submissions1 = assignmentService.getSubmissions((Assignment) o1).iterator();
				while (submissions1.hasNext())
				{
					AssignmentSubmission submission1 = (AssignmentSubmission) submissions1.next();
					if (!submission1.getCurrentSubmissionVersion().isDraft()) subNum1++;
				}

				Iterator submissions2 = assignmentService.getSubmissions((Assignment) o2).iterator();
				while (submissions2.hasNext())
				{
					AssignmentSubmission submission2 = (AssignmentSubmission) submissions2.next();
					if (!submission2.getCurrentSubmissionVersion().isDraft()) subNum2++;
				}

				result = (subNum1 > subNum2) ? 1 : -1;

			}
			else if (m_criteria.equals(SORTED_BY_NUM_UNGRADED))
			{
				// sort by numbers of ungraded submissions

				// initialize
				int ungraded1 = 0;
				int ungraded2 = 0;

				Iterator submissions1 = assignmentService.getSubmissions((Assignment) o1).iterator();
				while (submissions1.hasNext())
				{
					AssignmentSubmission submission1 = (AssignmentSubmission) submissions1.next();
					AssignmentSubmissionVersion sVersion1 = submission1.getCurrentSubmissionVersion();
					if (!sVersion1.isDraft() && sVersion1.getGrade() == null) ungraded1++;
				}

				Iterator submissions2 = assignmentService.getSubmissions((Assignment) o2).iterator();
				while (submissions2.hasNext())
				{
					AssignmentSubmission submission2 = (AssignmentSubmission) submissions2.next();
					AssignmentSubmissionVersion sVersion2 = submission2.getCurrentSubmissionVersion();
					if (!sVersion2.isDraft() && sVersion2.getGrade() == null) ungraded2++;
				}

				result = (ungraded1 > ungraded2) ? 1 : -1;

			}
			else if (m_criteria.equals(SORTED_BY_SUBMISSION_STATUS))
			{
				try
				{
					AssignmentSubmission submission1 = assignmentService.getSubmission(String.valueOf(((Assignment) o1).getId()), m_user);
					AssignmentSubmission submission2 = assignmentService.getSubmission(String.valueOf(((Assignment) o2).getId()), m_user);
					result = compareString(assignmentService.getSubmissionStatus(submission1.getCurrentSubmissionVersion()),
							assignmentService.getSubmissionStatus(submission2.getCurrentSubmissionVersion()));
				}
				catch (IdUnusedException e)
				{
					return 1;
				}
				catch (PermissionException e)
				{
					return 1;
				}
			}
			else if (m_criteria.equals(SORTED_BY_GRADE))
			{
				try
				{
					AssignmentSubmission submission1 = assignmentService.getSubmission(String.valueOf(((Assignment) o1).getId()), m_user);
					AssignmentSubmissionVersion submissionVersion1 = submission1.getCurrentSubmissionVersion();
					String grade1 = " ";
					if (submissionVersion1 != null && submissionVersion1.getGrade() != null && submissionVersion1.isReturned())
					{
						grade1 = submissionVersion1.getGrade();
					}

					AssignmentSubmission submission2 = assignmentService.getSubmission(String.valueOf(((Assignment) o2).getId()), m_user);
					AssignmentSubmissionVersion submissionVersion2 = submission2.getCurrentSubmissionVersion();
					String grade2 = " ";
					if (submissionVersion2 != null && submissionVersion2.getGrade() != null && submissionVersion2.isReturned())
					{
						grade2 = submissionVersion2.getGrade();
					}

					result = compareString(grade1, grade2);
				}
				catch (IdUnusedException e)
				{
					return 1;
				}
				catch (PermissionException e)
				{
					return 1;
				}
			}
			else if (m_criteria.equals(SORTED_BY_MAX_GRADE))
			{
				String maxGrade1 = maxGrade(((Assignment) o1).getTypeOfGrade(), (Assignment) o1);
				String maxGrade2 = maxGrade(((Assignment) o2).getTypeOfGrade(), (Assignment) o2);

				try
				{
					// do integer comparation inside point grade type
					int max1 = Integer.parseInt(maxGrade1);
					int max2 = Integer.parseInt(maxGrade2);
					result = (max1 < max2) ? -1 : 1;
				}
				catch (NumberFormatException e)
				{
					// otherwise do an alpha-compare
					result = compareString(maxGrade1, maxGrade2);
				}
			}
			// group related sorting
			else if (m_criteria.equals(SORTED_BY_FOR))
			{
				// sorted by the public view attribute
				String factor1 = getAssignmentRange((Assignment) o1);
				String factor2 = getAssignmentRange((Assignment) o2);
				result = compareString(factor1, factor2);
			}
			else if (m_criteria.equals(SORTED_BY_GROUP_TITLE))
			{
				// sorted by the group title
				String factor1 = ((Group) o1).getTitle();
				String factor2 = ((Group) o2).getTitle();
				result = compareString(factor1, factor2);
			}
			else if (m_criteria.equals(SORTED_BY_GROUP_DESCRIPTION))
			{
				// sorted by the group description
				String factor1 = ((Group) o1).getDescription();
				String factor2 = ((Group) o2).getDescription();
				if (factor1 == null)
				{
					factor1 = "";
				}
				if (factor2 == null)
				{
					factor2 = "";
				}
				result = compareString(factor1, factor2);
			}
			/** ***************** for sorting submissions in instructor grade assignment view ************* */
			else if(m_criteria.equals(SORTED_GRADE_SUBMISSION_CONTENTREVIEW))
			{
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;
				if (u1 == null || u2 == null || u1.getUser() == null || u2.getUser() == null )
				{
					result = 1;
				}
				else
				{	
					AssignmentSubmission s1 = u1.getSubmission();
					AssignmentSubmission s2 = u2.getSubmission();


					if (s1 == null)
					{
						result = -1;
					}
					else if (s2 == null )
					{
						result = 1;
					} 
					else
					{
						int score1 = u1.getSubmission().getCurrentSubmissionVersion().getReviewScore();
						int score2 = u2.getSubmission().getCurrentSubmissionVersion().getReviewScore();
						result = (new Integer(score1)).intValue() > (new Integer(score2)).intValue() ? 1 : -1;
					}
				}
				
			}
			else if (m_criteria.equals(SORTED_GRADE_SUBMISSION_BY_LASTNAME))
			{
				// sorted by the submitters sort name
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;

				if (u1 == null || u2 == null || u1.getUser() == null || u2.getUser() == null )
				{
					result = 1;
				}
				else
				{
					String lName1 = u1.getUser().getSortName();
					String lName2 = u2.getUser().getSortName();
					result = compareString(lName1, lName2);
				}
			}
			else if (m_criteria.equals(SORTED_GRADE_SUBMISSION_BY_SUBMIT_TIME))
			{
				// sorted by submission time
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;

				if (u1 == null || u2 == null)
				{
					result = -1;
				}
				else
				{
					AssignmentSubmissionVersion s1 = u1.getSubmission().getCurrentSubmissionVersion();
					AssignmentSubmissionVersion s2 = u2.getSubmission().getCurrentSubmissionVersion();


					if (s1 == null || s1.getTimeSubmitted() == null)
					{
						result = -1;
					}
					else if (s2 == null || s2.getTimeSubmitted() == null)
					{
						result = 1;
					}
					else if (s1.getTimeSubmitted().before(s2.getTimeSubmitted()))
					{
						result = -1;
					}
					else
					{
						result = 1;
					}
				}
			}
			else if (m_criteria.equals(SORTED_GRADE_SUBMISSION_BY_STATUS))
			{
				// sort by submission status
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;

				String status1 = "";
				String status2 = "";
				
				if (u1 == null)
				{
					status1 = rb.getString("listsub.nosub");
				}
				else
				{
					AssignmentSubmissionVersion s1 = u1.getSubmission().getCurrentSubmissionVersion();
					if (s1 == null)
					{
						status1 = rb.getString("listsub.nosub");
					}
					else
					{
						status1 = assignmentService.getSubmissionStatus(s1);
					}
				}
				
				if (u2 == null)
				{
					status2 = rb.getString("listsub.nosub");
				}
				else
				{
					AssignmentSubmissionVersion s2 = u2.getSubmission().getCurrentSubmissionVersion();
					if (s2 == null)
					{
						status2 = rb.getString("listsub.nosub");
					}
					else
					{
						status2 = assignmentService.getSubmissionStatus(s2);
					}
				}
				
				result = compareString(status1, status2);
			}
			else if (m_criteria.equals(SORTED_GRADE_SUBMISSION_BY_GRADE))
			{
				// sort by submission status
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;

				if (u1 == null || u2 == null)
				{
					result = -1;
				}
				else
				{
					AssignmentSubmissionVersion s1 = u1.getSubmission().getCurrentSubmissionVersion();
					AssignmentSubmissionVersion s2 = u2.getSubmission().getCurrentSubmissionVersion();

					//sort by submission grade
					if (s1 == null)
					{
						result = -1;
					}
					else if (s2 == null)
					{
						result = 1;
					}
					else
					{
						String grade1 = s1.getGrade();
						String grade2 = s2.getGrade();
						if (grade1 == null)
						{
							grade1 = "";
						}
						if (grade2 == null)
						{
							grade2 = "";
						}

						// if scale is points
						if ((s1.getAssignmentSubmission().getAssignment().getTypeOfGrade() == 3)
								&& ((s2.getAssignmentSubmission().getAssignment().getTypeOfGrade() == 3)))
						{
							if (grade1.equals(""))
							{
								result = -1;
							}
							else if (grade2.equals(""))
							{
								result = 1;
							}
							else
							{
								result = compareDouble(grade1, grade2);
							}
						}
						else
						{
							result = compareString(grade1, grade2);
						}
					}
				}
			}
			else if (m_criteria.equals(SORTED_GRADE_SUBMISSION_BY_RELEASED))
			{
				// sort by submission status
				UserSubmission u1 = (UserSubmission) o1;
				UserSubmission u2 = (UserSubmission) o2;

				if (u1 == null || u2 == null)
				{
					result = -1;
				}
				else
				{
					AssignmentSubmissionVersion s1 = u1.getSubmission().getCurrentSubmissionVersion();
					AssignmentSubmissionVersion s2 = u2.getSubmission().getCurrentSubmissionVersion();

					if (s1 == null)
					{
						result = -1;
					}
					else if (s2 == null)
					{
						result = 1;
					}
					else
					{
						// sort by submission released
						String released1 = (new Boolean(s1.isReturned())).toString();
						String released2 = (new Boolean(s2.isReturned())).toString();

						result = compareString(released1, released2);
					}
				}
			}
			/****** for other sort on submissions **/
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_LASTNAME))
			{
				// sorted by the submitters sort name
				String submitters1 = ((AssignmentSubmission) o1).getSubmitterId();
				String submitters2 = ((AssignmentSubmission) o2).getSubmitterId();

				result = compareString(submitters1, submitters2);
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_SUBMIT_TIME))
			{
				// sorted by submission time
				Date t1 = ((AssignmentSubmission) o1).getCurrentSubmissionVersion().getTimeSubmitted();
				Date t2 = ((AssignmentSubmission) o2).getCurrentSubmissionVersion().getTimeSubmitted();

				if (t1 == null)
				{
					result = -1;
				}
				else if (t2 == null)
				{
					result = 1;
				}
				else if (t1.before(t2))
				{
					result = -1;
				}
				else
				{
					result = 1;
				}
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_STATUS))
			{
				// sort by submission status
				result = compareString(assignmentService.getSubmissionStatus(((AssignmentSubmission) o1).getCurrentSubmissionVersion()), assignmentService.getSubmissionStatus(((AssignmentSubmission) o2).getCurrentSubmissionVersion()));
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_GRADE))
			{
				// sort by submission grade
				String grade1 = ((AssignmentSubmission) o1).getCurrentSubmissionVersion().getGrade();
				String grade2 = ((AssignmentSubmission) o2).getCurrentSubmissionVersion().getGrade();
				if (grade1 == null)
				{
					grade1 = "";
				}
				if (grade2 == null)
				{
					grade2 = "";
				}

				// if scale is points
				if ((((AssignmentSubmission) o1).getAssignment().getTypeOfGrade() == 3)
						&& ((((AssignmentSubmission) o2).getAssignment().getTypeOfGrade() == 3)))
				{
					if (grade1.equals(""))
					{
						result = -1;
					}
					else if (grade2.equals(""))
					{
						result = 1;
					}
					else
					{
						result = compareDouble(grade1, grade2);
					}
				}
				else
				{
					result = compareString(grade1, grade2);
				}
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_GRADE))
			{
				// sort by submission grade
				String grade1 = ((AssignmentSubmission) o1).getCurrentSubmissionVersion().getGrade();
				String grade2 = ((AssignmentSubmission) o2).getCurrentSubmissionVersion().getGrade();
				if (grade1 == null)
				{
					grade1 = "";
				}
				if (grade2 == null)
				{
					grade2 = "";
				}

				// if scale is points
				if ((((AssignmentSubmission) o1).getAssignment().getTypeOfGrade() == 3)
						&& ((((AssignmentSubmission) o2).getAssignment().getTypeOfGrade() == 3)))
				{
					if (grade1.equals(""))
					{
						result = -1;
					}
					else if (grade2.equals(""))
					{
						result = 1;
					}
					else
					{
						result = compareDouble(grade1, grade2);
					}
				}
				else
				{
					result = compareString(grade1, grade2);
				}
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_MAX_GRADE))
			{
				Assignment a1 = ((AssignmentSubmission) o1).getAssignment();
				Assignment a2 = ((AssignmentSubmission) o2).getAssignment();
				String maxGrade1 = maxGrade(a1.getTypeOfGrade(), a1);
				String maxGrade2 = maxGrade(a2.getTypeOfGrade(), a2);

				try
				{
					// do integer comparation inside point grade type
					int max1 = Integer.parseInt(maxGrade1);
					int max2 = Integer.parseInt(maxGrade2);
					result = (max1 < max2) ? -1 : 1;
				}
				catch (NumberFormatException e)
				{
					// otherwise do an alpha-compare
					result = maxGrade1.compareTo(maxGrade2);
				}
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_RELEASED))
			{
				// sort by submission released
				String released1 = (new Boolean(((AssignmentSubmission) o1).getCurrentSubmissionVersion().isReturned())).toString();
				String released2 = (new Boolean(((AssignmentSubmission) o2).getCurrentSubmissionVersion().isReturned())).toString();

				result = compareString(released1, released2);
			}
			else if (m_criteria.equals(SORTED_SUBMISSION_BY_ASSIGNMENT))
			{
				// sort by submission's assignment
				String title1 = ((AssignmentSubmission) o1).getAssignment().getTitle();
				String title2 = ((AssignmentSubmission) o2).getAssignment().getTitle();

				result = compareString(title1, title2);
			}
			/*************** sort user by sort name ***************/
			else if (m_criteria.equals(SORTED_USER_BY_SORTNAME))
			{
				// sort by user's sort name
				String name1 = ((User) o1).getSortName();
				String name2 = ((User) o2).getSortName();

				result = compareString(name1, name2);
			}

			// sort ascending or descending
			if (!Boolean.valueOf(m_asc))
			{
				result = -result;
			}
			return result;
		}

		/**
		 * Compare two strings as double values. Deal with the case when either of the strings cannot be parsed as double value.
		 * @param grade1
		 * @param grade2
		 * @return
		 */
		private int compareDouble(String grade1, String grade2) {
			int result;
			try
			{
				result = (new Double(grade1)).doubleValue() > (new Double(grade2)).doubleValue() ? 1 : -1;
			}
			catch (Exception formatException)
			{
				// in case either grade1 or grade2 cannot be parsed as Double
				result = compareString(grade1, grade2);
			}
			return result;
		} // compareDouble

		private int compareString(String s1, String s2) 
		{
			int result;
			if (s1 == null && s2 == null) {
				result = 0;
			} else if (s2 == null) {
				result = 1;
			} else if (s1 == null) {
				result = -1;
			} else {
				result = collator.compare(s1.toLowerCase(), s2.toLowerCase());
			}
			return result;
		}

		/**
		 * get submission status
		 */
		private String getSubmissionStatus(AssignmentSubmission submission, Assignment assignment)
		{
			String status = "";

			if (submission != null)
			{	
				AssignmentSubmissionVersion sVersion = submission.getCurrentSubmissionVersion();
				if (!sVersion.isDraft())
					if (sVersion.getGrade() != null && sVersion.isReturned())
						status = rb.getString("grad3");
					else if (sVersion.isReturned())
						status = rb.getString("return") + " " + sVersion.getTimeReleased().toString();
					else
					{
						status = rb.getString("submitt") + sVersion.getTimeSubmitted().toString();
						if (sVersion.getTimeSubmitted().after(assignment.getDueTime())) status = status + rb.getString("late");
					}
				else
					status = rb.getString("inpro");
			}
			else
				status = rb.getString("notsta");

			return status;

		} // getSubmissionStatus

		/**
		 * get assignment maximun grade available based on the assignment grade type
		 *
		 * @param gradeType
		 *        The int value of grade type
		 * @param a
		 *        The assignment object
		 * @return The max grade String
		 */
		private String maxGrade(int gradeType, Assignment a)
		{
			String maxGrade = "";

			if (gradeType == -1)
			{
				// Grade type not set
				maxGrade = rb.getString("granotset");
			}
			else if (gradeType == 1)
			{
				// Ungraded grade type
				maxGrade = rb.getString("nogra");
			}
			else if (gradeType == 2)
			{
				// Letter grade type
				maxGrade = "A";
			}
			else if (gradeType == 3)
			{
				// Score based grade type
				maxGrade = Integer.toString(a.getMaxGradePoint());
			}
			else if (gradeType == 4)
			{
				// Pass/fail grade type
				maxGrade = rb.getString("pass2");
			}
			else if (gradeType == 5)
			{
				// Grade type that only requires a check
				maxGrade = rb.getString("check2");
			}

			return maxGrade;

		} // maxGrade

	} // DiscussionComparator

	/**
	 * Fire up the permissions editor
	 */
	public void doPermissions(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		if (!alertGlobalNavigation(state, data))
		{
			// we are changing the view, so start with first page again.
			resetPaging(state);

			// clear search form
			doSearch_clear(data, null);

			if (SiteService.allowUpdateSite((String) state.getAttribute(STATE_CONTEXT_STRING)))
			{
				// get into helper mode with this helper tool
				startHelper(data.getRequest(), "sakai.permissions.helper");

				String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
				String siteRef = SiteService.siteReference(contextString);

				// setup for editing the permissions of the site for this tool, using the roles of this site, too
				state.setAttribute(PermissionsHelper.TARGET_REF, siteRef);

				// ... with this description
				state.setAttribute(PermissionsHelper.DESCRIPTION, rb.getString("setperfor") + " "
						+ SiteService.getSiteDisplay(contextString));

				// ... showing only locks that are prpefixed with this
				state.setAttribute(PermissionsHelper.PREFIX, "asn.");

				// disable auto-updates while leaving the list view
				justDelivered(state);
			}

			// reset the global navigaion alert flag
			if (state.getAttribute(ALERT_GLOBAL_NAVIGATION) != null)
			{
				state.removeAttribute(ALERT_GLOBAL_NAVIGATION);
			}

			// switching back to assignment list view
			state.setAttribute(STATE_SELECTED_VIEW, MODE_LIST_ASSIGNMENTS);
			doList_assignments(data);
		}

	} // doPermissions

	/**
	 * transforms the Iterator to Vector
	 */
	private Vector iterator_to_vector(Iterator l)
	{
		Vector v = new Vector();
		while (l.hasNext())
		{
			v.add(l.next());
		}
		return v;
	} // iterator_to_vector

	/**
	 * Implement this to return alist of all the resources that there are to page. Sort them as appropriate.
	 */
	protected List readResourcesPage(SessionState state, int first, int last)
	{

		List returnResources = (List) state.getAttribute(STATE_PAGEING_TOTAL_ITEMS);

		PagingPosition page = new PagingPosition(first, last);
		page.validate(returnResources.size());
		returnResources = returnResources.subList(page.getFirst() - 1, page.getLast());

		return returnResources;

	} // readAllResources

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sakaiproject.cheftool.PagedResourceActionII#sizeResources(org.sakaiproject.service.framework.session.SessionState)
	 */
	protected int sizeResources(SessionState state)
	{
		String mode = (String) state.getAttribute(STATE_MODE);
		String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
		// all the resources for paging
		List returnResources = new Vector();

		boolean allowAddAssignment = assignmentService.allowAddAssignment(contextString);
		if (mode.equalsIgnoreCase(MODE_LIST_ASSIGNMENTS))
		{
			String view = "";
			if (state.getAttribute(STATE_SELECTED_VIEW) != null)
			{
				view = (String) state.getAttribute(STATE_SELECTED_VIEW);
			}

			if (allowAddAssignment && view.equals(MODE_LIST_ASSIGNMENTS))
			{
				// read all Assignments
				returnResources = assignmentService.getListAssignmentsForContext((String) state
						.getAttribute(STATE_CONTEXT_STRING));
			}
			else if (allowAddAssignment && view.equals(MODE_STUDENT_VIEW)
					|| (!allowAddAssignment && assignmentService.allowAddSubmission((String) state
						.getAttribute(STATE_CONTEXT_STRING))))
			{
				// in the student list view of assignments
				Iterator assignments = assignmentService
						.getAssignmentsForContext(contextString);
				Date currentTime = new Date();
				while (assignments.hasNext())
				{
					Assignment a = (Assignment) assignments.next();
					try
					{
						if (!a.isDeleted())
						{
							// show not deleted assignments
							Date openTime = a.getOpenTime();
							if (openTime != null && currentTime.after(openTime) && !a.isDraft())
							{
								returnResources.add(a);
							}
						}
						else if (a.isDeleted() && (a.getTypeOfSubmission() != AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION) && assignmentService.getSubmission(a.getReference(), (User) state
								.getAttribute(STATE_USER)) != null)
						{
							// and those deleted but not non-electronic assignments but the user has made submissions to them
							returnResources.add(a);
						}
					}
					catch (IdUnusedException e)
					{
						addAlert(state, rb.getString("cannotfin3"));
					}
					catch (PermissionException e)
					{
						addAlert(state, rb.getString("youarenot14"));
					}
				}
			}
			else
			{
				// read all Assignments
				returnResources = assignmentService.getListAssignmentsForContext((String) state
						.getAttribute(STATE_CONTEXT_STRING));
			}
			
			state.setAttribute(HAS_MULTIPLE_ASSIGNMENTS, Boolean.valueOf(returnResources.size() > 1));
		}
		else if (mode.equalsIgnoreCase(MODE_INSTRUCTOR_REORDER_ASSIGNMENT))
		{
			returnResources = assignmentService.getListAssignmentsForContext((String) state
					.getAttribute(STATE_CONTEXT_STRING));
		}
		else if (mode.equalsIgnoreCase(MODE_INSTRUCTOR_REPORT_SUBMISSIONS))
		{
			Vector submissions = new Vector();

			Vector assignments = iterator_to_vector(assignmentService.getAssignmentsForContext((String) state
					.getAttribute(STATE_CONTEXT_STRING)));
			if (assignments.size() > 0)
			{
				// users = assignmentService.allowAddSubmissionUsers (((Assignment)assignments.get(0)).getReference ());
			}

			for (int j = 0; j < assignments.size(); j++)
			{
				Assignment a = (Assignment) assignments.get(j);
				
				//get the list of users which are allowed to grade this assignment
				List allowGradeAssignmentUsers = assignmentService.allowGradeAssignmentUsers(a.getReference());
				
				if (!a.isDeleted() && (!a.isDraft()) && assignmentService.allowGradeSubmission(a.getReference()))
				{
					try
					{
						List assignmentSubmissions = assignmentService.getSubmissions(a);
						for (int k = 0; k < assignmentSubmissions.size(); k++)
						{
							AssignmentSubmission s = (AssignmentSubmission) assignmentSubmissions.get(k);
							AssignmentSubmissionVersion sVersion = s.getCurrentSubmissionVersion();
							if (s != null && (!sVersion.isDraft() || (sVersion.isReturned() && (sVersion.getLastModifiedTime().before(sVersion.getTimeReleased())))))
							{
								// has been subitted or has been returned and not work on it yet
								if (!allowGradeAssignmentUsers.contains(s.getSubmitterId()))
								{
									// only include the student submission
									submissions.add(s);
								}
							} // if-else
						}
					}
					catch (Exception e)
					{
					}
				}
			}

			returnResources = submissions;
		}
		else if (mode.equalsIgnoreCase(MODE_INSTRUCTOR_GRADE_ASSIGNMENT))
		{
			// range
			Collection groups = new Vector();
			try
			{
				Assignment a = assignmentService.getAssignment((String) state.getAttribute(EXPORT_ASSIGNMENT_REF));
				
				// all submissions
				List submissions = assignmentService.getSubmissions(a);
				
				// now are we view all sections/groups or just specific one?
				String allOrOneGroup = (String) state.getAttribute(VIEW_SUBMISSION_LIST_OPTION);
				if (allOrOneGroup.equals(rb.getString("gen.viewallgroupssections")))
				{
					if (assignmentService.allowAllGroups(contextString))
					{
						// site range
						groups.add(SiteService.getSite(contextString));
					}
					else
					{
						// get all groups user can grade
						groups = assignmentService.getGroupsAllowGradeAssignment(contextString, a.getReference());
					}
				}
				else
				{
					// filter out only those submissions from the selected-group members
					try
					{
						Group group = SiteService.getSite(contextString).getGroup(allOrOneGroup);
						groups.add(group);
					}
					catch (Exception e)
					{
						log.warn(e.getMessage() + " groupId=" + allOrOneGroup);
					}
				}

				// all users that can submit
				List allowAddSubmissionUsers = assignmentService.allowAddSubmissionUsers((String) state.getAttribute(EXPORT_ASSIGNMENT_REF));

				HashSet userIdSet = new HashSet();
				for (Iterator iGroup=groups.iterator(); iGroup.hasNext();)
				{
					Object nGroup = iGroup.next();
					String authzGroupRef = (nGroup instanceof Group)? ((Group) nGroup).getReference():((nGroup instanceof Site))?((Site) nGroup).getReference():null;
					if (authzGroupRef != null)
					{
						try
						{
							AuthzGroup group = AuthzGroupService.getAuthzGroup(authzGroupRef);
							Set grants = group.getUsers();
							for (Iterator iUserIds = grants.iterator(); iUserIds.hasNext();)
							{
								String userId = (String) iUserIds.next();
								
								// don't show user multiple times
								if (!userIdSet.contains(userId))
								{
									try
									{
										User u = UserDirectoryService.getUser(userId);
										// only include those users that can submit to this assignment
										if (u != null && allowAddSubmissionUsers.contains(u))
										{
											boolean found = false;
											for (int i = 0; !found && i<submissions.size();i++)
											{
												AssignmentSubmission s = (AssignmentSubmission) submissions.get(i);
												if (s.getSubmitterId().equals(userId))
												{
													returnResources.add(new UserSubmission(u, s));
													found = true;
												}
											}
											
		
											// add those users who haven't made any submissions
											if (!found)
											{
												// construct fake submissions for grading purpose if the user has right for grading
												if (assignmentService.allowGradeSubmission(a.getReference()))
												{
												
													// temporarily allow the user to read and write from assignments (asn.revise permission)
											        SecurityService.pushAdvisor(new SecurityAdvisor()
											            {
											                public SecurityAdvice isAllowed(String userId, String function, String reference)
											                {
											                    return SecurityAdvice.ALLOWED;
											                }
											            });
											        
													// construct fake submissions for grading purpose
													AssignmentSubmission submission = assignmentService.newSubmission(Long.toString(a.getId()), userId);
													assignmentService.saveSubmission(submission);
													
													AssignmentSubmissionVersion submissionVersion = assignmentService.newSubmissionVersion(submission.getReference());
													submissionVersion.setTimeSubmitted(new Date());
													submissionVersion.setDraft(false);
													assignmentService.saveSubmissionVersion(submissionVersion);
													
													// update the UserSubmission list by adding newly created Submission object
													returnResources.add(new UserSubmission(u, submission));
			
											        // clear the permission
											        SecurityService.clearAdvisors();
												}
											}
										}
									}
									catch (Exception e)
									{
										log.warn(this + e.toString() + " here userId = " + userId);
									}
									
									// add userId into set to prevent showing user multiple times
									userIdSet.add(userId);
								}
							}
							
						}
						catch (Exception eee)
						{
							log.warn(eee.getMessage() + " authGroupId=" + authzGroupRef);
						}
					}
				}

			}
			catch (IdUnusedException e)
			{
				addAlert(state, rb.getString("cannotfin3"));
			}
			catch (PermissionException e)
			{
				addAlert(state, rb.getString("youarenot14"));
			}

		}

		// sort them all
		String ascending = "true";
		String sort = "";
		ascending = (String) state.getAttribute(SORTED_ASC);
		sort = (String) state.getAttribute(SORTED_BY);
		if (mode.equalsIgnoreCase(MODE_INSTRUCTOR_GRADE_ASSIGNMENT) && (sort == null || !sort.startsWith("sorted_grade_submission_by")))
		{
			ascending = (String) state.getAttribute(SORTED_GRADE_SUBMISSION_ASC);
			sort = (String) state.getAttribute(SORTED_GRADE_SUBMISSION_BY);
		}
		else if (mode.equalsIgnoreCase(MODE_INSTRUCTOR_REPORT_SUBMISSIONS) && (sort == null || sort.startsWith("sorted_submission_by")))
		{
			ascending = (String) state.getAttribute(SORTED_SUBMISSION_ASC);
			sort = (String) state.getAttribute(SORTED_SUBMISSION_BY);
		}
		else
		{
			ascending = (String) state.getAttribute(SORTED_ASC);
			sort = (String) state.getAttribute(SORTED_BY);
		}
		
		if ((returnResources.size() > 1) && !mode.equalsIgnoreCase(MODE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT))
		{
			Collections.sort(returnResources, new AssignmentComparator(state, sort, ascending));
		}

		// record the total item number
		state.setAttribute(STATE_PAGEING_TOTAL_ITEMS, returnResources);
		
		return returnResources.size();
	}

	public void doView(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		if (!alertGlobalNavigation(state, data))
		{
			// we are changing the view, so start with first page again.
			resetPaging(state);

			// clear search form
			doSearch_clear(data, null);

			String viewMode = data.getParameters().getString("view");
			state.setAttribute(STATE_SELECTED_VIEW, viewMode);

			if (viewMode.equals(MODE_LIST_ASSIGNMENTS))
			{
				doList_assignments(data);
			}
			else if (viewMode.equals(MODE_INSTRUCTOR_VIEW_STUDENTS_ASSIGNMENT))
			{
				doView_students_assignment(data);
			}
			else if (viewMode.equals(MODE_INSTRUCTOR_REPORT_SUBMISSIONS))
			{
				doReport_submissions(data);
			}
			else if (viewMode.equals(MODE_STUDENT_VIEW))
			{
				doView_student(data);
			}

			// reset the global navigaion alert flag
			if (state.getAttribute(ALERT_GLOBAL_NAVIGATION) != null)
			{
				state.removeAttribute(ALERT_GLOBAL_NAVIGATION);
			}
		}

	} // doView

	/**
	 * put those variables related to 2ndToolbar into context
	 */
	private void add2ndToolbarFields(RunData data, Context context)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		context.put("totalPageNumber", new Integer(totalPageNumber(state)));
		context.put("form_search", FORM_SEARCH);
		context.put("formPageNumber", FORM_PAGE_NUMBER);
		context.put("prev_page_exists", state.getAttribute(STATE_PREV_PAGE_EXISTS));
		context.put("next_page_exists", state.getAttribute(STATE_NEXT_PAGE_EXISTS));
		context.put("current_page", state.getAttribute(STATE_CURRENT_PAGE));
		context.put("selectedView", state.getAttribute(STATE_MODE));

	} // add2ndToolbarFields

	/**
	 * valid grade for point based type
	 */
	private void validPointGrade(SessionState state, String grade)
	{
		if (grade != null && !grade.equals(""))
		{
			if (grade.startsWith("-"))
			{
				// check for negative sign
				addAlert(state, rb.getString("plesuse3"));
			}
			else
			{
				int index = grade.indexOf(".");
				if (index != -1)
				{
					// when there is decimal points inside the grade, scale the number by 10
					// but only one decimal place is supported
					// for example, change 100.0 to 1000
					if (!grade.equals("."))
					{
						if (grade.length() > index + 2)
						{
							// if there are more than one decimal point
							addAlert(state, rb.getString("plesuse2"));
						}
						else
						{
							// decimal points is the only allowed character inside grade
							// replace it with '1', and try to parse the new String into int
							String gradeString = (grade.endsWith(".")) ? grade.substring(0, index).concat("0") : grade.substring(0,
									index).concat(grade.substring(index + 1));
							try
							{
								Integer.parseInt(gradeString);
							}
							catch (NumberFormatException e)
							{
								alertInvalidPoint(state, gradeString);
							}
						}
					}
					else
					{
						// grade is "."
						addAlert(state, rb.getString("plesuse1"));
					}
				}
				else
				{
					// There is no decimal point; should be int number
					String gradeString = grade + "0";
					try
					{
						Integer.parseInt(gradeString);
					}
					catch (NumberFormatException e)
					{
						alertInvalidPoint(state, gradeString);
					}
				}
			}
		}

	} // validPointGrade
	
	/**
	 * valid grade for point based type
	 */
	private void validLetterGrade(SessionState state, String grade)
	{
		String VALID_CHARS_FOR_LETTER_GRADE = " ABCDEFGHIJKLMNOPQRSTUVWXYZ+-";
		boolean invalid = false;
		if (grade != null)
		{
			grade = grade.toUpperCase();
			for (int i = 0; i < grade.length() && !invalid; i++)
			{
				char c = grade.charAt(i);
				if (VALID_CHARS_FOR_LETTER_GRADE.indexOf(c) == -1)
				{
					invalid = true;
				}
			}
			if (invalid)
			{
				addAlert(state, rb.getString("plesuse0"));
			}
		}
	}

	private void alertInvalidPoint(SessionState state, String grade)
	{
		String VALID_CHARS_FOR_INT = "-01234567890";

		boolean invalid = false;
		// case 1: contains invalid char for int
		for (int i = 0; i < grade.length() && !invalid; i++)
		{
			char c = grade.charAt(i);
			if (VALID_CHARS_FOR_INT.indexOf(c) == -1)
			{
				invalid = true;
			}
		}
		if (invalid)
		{
			addAlert(state, rb.getString("plesuse1"));
		}
		else
		{
			int maxInt = Integer.MAX_VALUE / 10;
			int maxDec = Integer.MAX_VALUE - maxInt * 10;
			// case 2: Due to our internal scaling, input String is larger than Integer.MAX_VALUE/10
			addAlert(state, grade.substring(0, grade.length()-1) + "." + grade.substring(grade.length()-1) + " " + rb.getString("plesuse4") + maxInt + "." + maxDec + ".");
		}
	}

	/**
	 * display grade properly
	 */
	private String displayGrade(SessionState state, String grade)
	{
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			if (grade != null && (grade.length() >= 1))
			{
				if (grade.indexOf(".") != -1)
				{
					if (grade.startsWith("."))
					{
						grade = "0".concat(grade);
					}
					else if (grade.endsWith("."))
					{
						grade = grade.concat("0");
					}
				}
				else
				{
					try
					{
						Integer.parseInt(grade);
						grade = grade.substring(0, grade.length() - 1) + "." + grade.substring(grade.length() - 1);
					}
					catch (NumberFormatException e)
					{
						alertInvalidPoint(state, grade);
					}
				}
			}
			else
			{
				grade = "";
			}
		}
		return grade;

	} // displayGrade

	/**
	 * scale the point value by 10 if there is a valid point grade
	 */
	private String scalePointGrade(SessionState state, String point)
	{
		validPointGrade(state, point);
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			if (point != null && (point.length() >= 1))
			{
				// when there is decimal points inside the grade, scale the number by 10
				// but only one decimal place is supported
				// for example, change 100.0 to 1000
				int index = point.indexOf(".");
				if (index != -1)
				{
					if (index == 0)
					{
						// if the point is the first char, add a 0 for the integer part
						point = "0".concat(point.substring(1));
					}
					else if (index < point.length() - 1)
					{
						// use scale integer for gradePoint
						point = point.substring(0, index) + point.substring(index + 1);
					}
					else
					{
						// decimal point is the last char
						point = point.substring(0, index) + "0";
					}
				}
				else
				{
					// if there is no decimal place, scale up the integer by 10
					point = point + "0";
				}

				// filter out the "zero grade"
				if (point.equals("00"))
				{
					point = "0";
				}
			}
		}
		return point;

	} // scalePointGrade

	/**
	 * Processes formatted text that is coming back from the browser (from the formatted text editing widget).
	 *
	 * @param state
	 *        Used to pass in any user-visible alerts or errors when processing the text
	 * @param strFromBrowser
	 *        The string from the browser
	 * @param checkForFormattingErrors
	 *        Whether to check for formatted text errors - if true, look for errors in the formatted text. If false, accept the formatted text without looking for errors.
	 * @return The formatted text
	 */
	private String processFormattedTextFromBrowser(SessionState state, String strFromBrowser, boolean checkForFormattingErrors)
	{
		StringBuilder alertMsg = new StringBuilder();
		try
		{
			boolean replaceWhitespaceTags = true;
			String text = FormattedText.processFormattedText(strFromBrowser, alertMsg, checkForFormattingErrors,
					replaceWhitespaceTags);
			if (alertMsg.length() > 0) addAlert(state, alertMsg.toString());
			return text;
		}
		catch (Exception e)
		{
			log.warn(this + ": ", e);
			return strFromBrowser;
		}
	}

	/**
	 * Processes the given assignmnent feedback text, as returned from the user's browser. Makes sure that the Chef-style markup {{like this}} is properly balanced.
	 */
	private String processAssignmentFeedbackFromBrowser(SessionState state, String strFromBrowser)
	{
		if (strFromBrowser == null || strFromBrowser.length() == 0) return strFromBrowser;

		StringBuilder buf = new StringBuilder(strFromBrowser);
		int pos = -1;
		int numopentags = 0;

		while ((pos = buf.indexOf("{{")) != -1)
		{
			buf.replace(pos, pos + "{{".length(), "<ins>");
			numopentags++;
		}

		while ((pos = buf.indexOf("}}")) != -1)
		{
			buf.replace(pos, pos + "}}".length(), "</ins>");
			numopentags--;
		}

		while (numopentags > 0)
		{
			buf.append("</ins>");
			numopentags--;
		}

		boolean checkForFormattingErrors = false; // so that grading isn't held up by formatting errors
		buf = new StringBuilder(processFormattedTextFromBrowser(state, buf.toString(), checkForFormattingErrors));

		while ((pos = buf.indexOf("<ins>")) != -1)
		{
			buf.replace(pos, pos + "<ins>".length(), "{{");
		}

		while ((pos = buf.indexOf("</ins>")) != -1)
		{
			buf.replace(pos, pos + "</ins>".length(), "}}");
		}

		return buf.toString();
	}

	/**
	 * Called to deal with old Chef-style assignment feedback annotation, {{like this}}.
	 *
	 * @param value
	 *        A formatted text string that may contain {{}} style markup
	 * @return HTML ready to for display on a browser
	 */
	public static String escapeAssignmentFeedback(String value)
	{
		if (value == null || value.length() == 0) return value;

		value = fixAssignmentFeedback(value);

		StringBuilder buf = new StringBuilder(value);
		int pos = -1;

		while ((pos = buf.indexOf("{{")) != -1)
		{
			buf.replace(pos, pos + "{{".length(), "<span class='highlight'>");
		}

		while ((pos = buf.indexOf("}}")) != -1)
		{
			buf.replace(pos, pos + "}}".length(), "</span>");
		}

		return FormattedText.escapeHtmlFormattedText(buf.toString());
	}

	/**
	 * Escapes the given assignment feedback text, to be edited as formatted text (perhaps using the formatted text widget)
	 */
	public static String escapeAssignmentFeedbackTextarea(String value)
	{
		if (value == null || value.length() == 0) return value;

		value = fixAssignmentFeedback(value);

		return FormattedText.escapeHtmlFormattedTextarea(value);
	}

	/**
	 * Apply the fix to pre 1.1.05 assignments submissions feedback.
	 */
	private static String fixAssignmentFeedback(String value)
	{
		if (value == null || value.length() == 0) return value;

		StringBuilder buf = new StringBuilder(value);
		int pos = -1;

		// <br/> -> \n
		while ((pos = buf.indexOf("<br/>")) != -1)
		{
			buf.replace(pos, pos + "<br/>".length(), "\n");
		}

		// <span class='chefAlert'>( -> {{
		while ((pos = buf.indexOf("<span class='chefAlert'>(")) != -1)
		{
			buf.replace(pos, pos + "<span class='chefAlert'>(".length(), "{{");
		}

		// )</span> -> }}
		while ((pos = buf.indexOf(")</span>")) != -1)
		{
			buf.replace(pos, pos + ")</span>".length(), "}}");
		}

		while ((pos = buf.indexOf("<ins>")) != -1)
		{
			buf.replace(pos, pos + "<ins>".length(), "{{");
		}

		while ((pos = buf.indexOf("</ins>")) != -1)
		{
			buf.replace(pos, pos + "</ins>".length(), "}}");
		}

		return buf.toString();

	} // fixAssignmentFeedback

	/**
	 * Apply the fix to pre 1.1.05 assignments submissions feedback.
	 */
	public static String showPrevFeedback(String value)
	{
		if (value == null || value.length() == 0) return value;

		StringBuilder buf = new StringBuilder(value);
		int pos = -1;

		// <br/> -> \n
		while ((pos = buf.indexOf("\n")) != -1)
		{
			buf.replace(pos, pos + "\n".length(), "<br />");
		}

		return buf.toString();

	} // showPrevFeedback

	private boolean alertGlobalNavigation(SessionState state, RunData data)
	{
		String mode = (String) state.getAttribute(STATE_MODE);
		ParameterParser params = data.getParameters();

		if (mode.equals(MODE_STUDENT_VIEW_SUBMISSION) || mode.equals(MODE_STUDENT_PREVIEW_SUBMISSION)
				|| mode.equals(MODE_STUDENT_VIEW_GRADE) || mode.equals(MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT)
				|| mode.equals(MODE_INSTRUCTOR_DELETE_ASSIGNMENT) || mode.equals(MODE_INSTRUCTOR_GRADE_SUBMISSION)
				|| mode.equals(MODE_INSTRUCTOR_PREVIEW_GRADE_SUBMISSION) || mode.equals(MODE_INSTRUCTOR_PREVIEW_ASSIGNMENT)
				|| mode.equals(MODE_INSTRUCTOR_VIEW_ASSIGNMENT) || mode.equals(MODE_INSTRUCTOR_REORDER_ASSIGNMENT))
		{
			if (state.getAttribute(ALERT_GLOBAL_NAVIGATION) == null)
			{
				addAlert(state, rb.getString("alert.globalNavi"));
				state.setAttribute(ALERT_GLOBAL_NAVIGATION, Boolean.TRUE);

				if (mode.equals(MODE_STUDENT_VIEW_SUBMISSION))
				{
					// retrieve the submission text (as formatted text)
					boolean checkForFormattingErrors = true; // the student is submitting something - so check for errors
					String text = processFormattedTextFromBrowser(state, params.getCleanString(VIEW_SUBMISSION_TEXT),
							checkForFormattingErrors);

					state.setAttribute(VIEW_SUBMISSION_TEXT, text);
					if (params.getString(VIEW_SUBMISSION_HONOR_PLEDGE_YES) != null)
					{
						state.setAttribute(VIEW_SUBMISSION_HONOR_PLEDGE_YES, "true");
					}
					state.setAttribute(FilePickerHelper.FILE_PICKER_TITLE_TEXT, rb.getString("gen.addatt"));

					// TODO: file picker to save in dropbox? -ggolden
					// User[] users = { UserDirectoryService.getCurrentUser() };
					// state.setAttribute(ResourcesAction.STATE_SAVE_ATTACHMENT_IN_DROPBOX, users);
				}
				else if (mode.equals(MODE_INSTRUCTOR_NEW_EDIT_ASSIGNMENT))
				{
					setNewAssignmentParameters(data, false);
				}
				else if (mode.equals(MODE_INSTRUCTOR_GRADE_SUBMISSION))
				{
					readGradeForm(data, state, "read");
				}

				return true;
			}
		}

		return false;

	} // alertGlobalNavigation

	/**
	 * Dispatch function inside add submission page
	 */
	public void doRead_add_submission_form(RunData data)
	{
		String option = data.getParameters().getString("option");
		if (option.equals("cancel"))
		{
			// cancel
			doCancel_show_submission(data);
		}
		else if (option.equals("preview"))
		{
			// preview
			doPreview_submission(data);
		}
		else if (option.equals("save"))
		{
			// save draft
			doSave_submission(data);
		}
		else if (option.equals("post"))
		{
			// post
			doPost_submission(data);
		}
		else if (option.equals("revise"))
		{
			// done preview
			doDone_preview_submission(data);
		}
		else if (option.equals("attach"))
		{
			// attach
			ToolSession toolSession = SessionManager.getCurrentToolSession();
			String userId = SessionManager.getCurrentSessionUserId();
			String siteId = SiteService.getUserSiteId(userId);
	        String collectionId = m_contentHostingService.getSiteCollection(siteId);
	        toolSession.setAttribute(FilePickerHelper.DEFAULT_COLLECTION_ID, collectionId);
			doAttachments(data);
		}
	}
	
	/**
	 * Set default score for all ungraded non electronic submissions
	 * @param data
	 */
	public void doSet_defaultNotGradedNonElectronicScore(RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ()); 
		ParameterParser params = data.getParameters();
		
		String grade = StringUtil.trimToNull(params.getString("defaultGrade"));
		if (grade == null)
		{
			addAlert(state, rb.getString("plespethe2"));
		}
		
		String assignmentId = (String) state.getAttribute(EXPORT_ASSIGNMENT_REF);
		try
		{
			// record the default grade setting for no-submission
			Assignment a = assignmentService.getAssignment(assignmentId); 
			a.setNoSubmissionDefaultGrade(grade);
			assignmentService.saveAssignment(a);
			
			if (a.getTypeOfGrade() == AssignmentConstants.SCORE_GRADE_TYPE)
			{
				//for point-based grades
				validPointGrade(state, grade);
				
				if (state.getAttribute(STATE_MESSAGE) == null)
				{
					int maxGrade = a.getMaxGradePoint();
					try
					{
						if (Integer.parseInt(scalePointGrade(state, grade)) > maxGrade)
						{
							if (state.getAttribute(GRADE_GREATER_THAN_MAX_ALERT) == null)
							{
								// alert user first when he enters grade bigger than max scale
								addAlert(state, rb.getString("grad2"));
								state.setAttribute(GRADE_GREATER_THAN_MAX_ALERT, Boolean.TRUE);
							}
							else
							{
								// remove the alert once user confirms he wants to give student higher grade
								state.removeAttribute(GRADE_GREATER_THAN_MAX_ALERT);
							}
						}
					}
					catch (NumberFormatException e)
					{
						alertInvalidPoint(state, grade);
					}
				}
				
				if (state.getAttribute(STATE_MESSAGE) == null)
				{
					grade = scalePointGrade(state, grade);
				}
			}
			
			
			if (grade != null && state.getAttribute(STATE_MESSAGE) == null)
			{
				// get the user list
				List submissions = assignmentService.getSubmissions(a);
				
				for (int i = 0; i<submissions.size(); i++)
				{
					// get the submission object
					AssignmentSubmission submission = (AssignmentSubmission) submissions.get(i);
					AssignmentSubmissionVersion sVersion = submission.getCurrentSubmissionVersion();
					if (!sVersion.isDraft() && sVersion.getGrade() == null)
					{
						// update the grades for those existing non-submissions
						sVersion.setGrade(grade);
						assignmentService.saveSubmissionVersion(sVersion);
					}
				}
			}
			
		}
		catch (Exception e)
		{
			log.warn(e.toString());
		
		}
		
		
	}
	
	/**
	 * 
	 */
	public void doSet_defaultNoSubmissionScore(RunData data)
	{
		SessionState state = ((JetspeedRunData)data).getPortletSessionState (((JetspeedRunData)data).getJs_peid ()); 
		ParameterParser params = data.getParameters();
		
		String grade = StringUtil.trimToNull(params.getString("defaultGrade"));
		if (grade == null)
		{
			addAlert(state, rb.getString("plespethe2"));
		}
		
		String assignmentId = (String) state.getAttribute(EXPORT_ASSIGNMENT_REF);
		try
		{
			// record the default grade setting for no-submission
			Assignment a = assignmentService.getAssignment(assignmentId); 
			a.setNoSubmissionDefaultGrade(grade);
			assignmentService.saveAssignment(a);
			
			if (a.getTypeOfGrade() == AssignmentConstants.SCORE_GRADE_TYPE)
			{
				//for point-based grades
				validPointGrade(state, grade);
				
				if (state.getAttribute(STATE_MESSAGE) == null)
				{
					int maxGrade = a.getMaxGradePoint();
					try
					{
						if (Integer.parseInt(scalePointGrade(state, grade)) > maxGrade)
						{
							if (state.getAttribute(GRADE_GREATER_THAN_MAX_ALERT) == null)
							{
								// alert user first when he enters grade bigger than max scale
								addAlert(state, rb.getString("grad2"));
								state.setAttribute(GRADE_GREATER_THAN_MAX_ALERT, Boolean.TRUE);
							}
							else
							{
								// remove the alert once user confirms he wants to give student higher grade
								state.removeAttribute(GRADE_GREATER_THAN_MAX_ALERT);
							}
						}
					}
					catch (NumberFormatException e)
					{
						alertInvalidPoint(state, grade);
					}
				}
				
				if (state.getAttribute(STATE_MESSAGE) == null)
				{
					grade = scalePointGrade(state, grade);
				}
			}
			
			
			if (grade != null && state.getAttribute(STATE_MESSAGE) == null)
			{
				// get the user list
				List userSubmissions = new Vector();
				if (state.getAttribute(USER_SUBMISSIONS) != null)
				{
					userSubmissions = (List) state.getAttribute(USER_SUBMISSIONS);
				}
				
				// constructor a new UserSubmissions list
				List userSubmissionsNew = new Vector();
				
				for (int i = 0; i<userSubmissions.size(); i++)
				{
					// get the UserSubmission object
					UserSubmission us = (UserSubmission) userSubmissions.get(i);
					
					User u = us.getUser();
					AssignmentSubmission submission = us.getSubmission();
					
					// check whether there is a submission associated
					if (submission == null)
					{
						AssignmentSubmission s = assignmentService.newSubmission(String.valueOf(assignmentId), u.getId());
						assignmentService.saveSubmission(submission);
						
						AssignmentSubmissionVersion submissionVersion = assignmentService.newSubmissionVersion(submission.getReference());
						submissionVersion.setTimeSubmitted(new Date());
						submissionVersion.setDraft(false);
						submissionVersion.setGrade(grade);
						assignmentService.saveSubmissionVersion(submissionVersion);
						
						// update the UserSubmission list by adding newly created Submission object
						AssignmentSubmission sub = assignmentService.getSubmission(s.getReference());
						userSubmissionsNew.add(new UserSubmission(u, sub));
					}
					else 
					{
						AssignmentSubmissionVersion submissionVersion = submission.getCurrentSubmissionVersion();
						if (StringUtil.trimToNull(submissionVersion.getGrade()) == null)
						{
							// update the grades for those existing non-submissions
							submissionVersion.setGrade(grade);
							submissionVersion.setDraft(false);
							assignmentService.saveSubmissionVersion(submissionVersion);
							
							userSubmissionsNew.add(new UserSubmission(u, submission));
						}
						else
						{
							// no change for this user
							userSubmissionsNew.add(us);
						}
					}
				}
				
				state.setAttribute(USER_SUBMISSIONS, userSubmissionsNew);
			}
			
		}
		catch (Exception e)
		{
			log.warn(e.toString());
		
		}
		
		
	}
	
	/**
	 * 
	 * @return
	 */
	public void doUpload_all(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();
		String flow = params.getString("flow");
		if (flow.equals("upload"))
		{
			// upload
			doUpload_all_upload(data);
		}
		else if (flow.equals("cancel"))
		{
			// cancel
			doCancel_upload_all(data);
		}
	}
	
	public void doUpload_all_upload(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();
		
		String contextString = ToolManager.getCurrentPlacement().getContext();
		String toolTitle = ToolManager.getTool(ASSIGNMENT_TOOL_ID).getTitle();
		String aReference = (String) state.getAttribute(EXPORT_ASSIGNMENT_REF);
		String associateGradebookAssignment = null;
		
		boolean hasSubmissionText = false;
		boolean hasSubmissionAttachment = false;
		boolean hasGradeFile = false;
		boolean hasFeedbackText = false;
		boolean hasComment = false;
		boolean hasFeedbackAttachment = false;
		boolean releaseGrades = false;
		
		// check against the content elements selection
		if (params.getString("studentSubmissionText") != null)
		{
			// should contain student submission text information
			hasSubmissionText = true;
		}
		if (params.getString("studentSubmissionAttachment") != null)
		{
			// should contain student submission attachment information
			hasSubmissionAttachment = true;
		}
		if (params.getString("gradeFile") != null)
		{
			// should contain grade file
			hasGradeFile = true;	
		}
		if (params.getString("feedbackTexts") != null)
		{
			// inline text
			hasFeedbackText = true;
		}
		if (params.getString("feedbackComments") != null)
		{
			// comments.txt should be available
			hasComment = true;
		}
		if (params.getString("feedbackAttachments") != null)
		{
			// feedback attachment
			hasFeedbackAttachment = true;
		}
		if (params.getString("release") != null)
		{
			// comments.xml should be available
			releaseGrades = params.getBoolean("release");
		}
		state.setAttribute(UPLOAD_ALL_HAS_SUBMISSION_TEXT, Boolean.valueOf(hasSubmissionText));
		state.setAttribute(UPLOAD_ALL_HAS_SUBMISSION_ATTACHMENT, Boolean.valueOf(hasSubmissionAttachment));
		state.setAttribute(UPLOAD_ALL_HAS_GRADEFILE, Boolean.valueOf(hasGradeFile));
		state.setAttribute(UPLOAD_ALL_HAS_COMMENTS, Boolean.valueOf(hasComment));
		state.setAttribute(UPLOAD_ALL_HAS_FEEDBACK_TEXT, Boolean.valueOf(hasFeedbackText));
		state.setAttribute(UPLOAD_ALL_HAS_FEEDBACK_ATTACHMENT, Boolean.valueOf(hasFeedbackAttachment));
		state.setAttribute(UPLOAD_ALL_RELEASE_GRADES, Boolean.valueOf(releaseGrades));
		
		if (!hasSubmissionText && !hasSubmissionAttachment && !hasFeedbackText && !hasGradeFile && !hasComment && !hasFeedbackAttachment)
		{
			// has to choose one upload feature
			addAlert(state, rb.getString("uploadall.alert.choose.element"));
		}
		else
		{
			// constructor the hashtable for all submission objects
			Hashtable submissionTable = new Hashtable();
			Assignment assignment = null;
			List submissions = null;
			try
			{
				assignment = assignmentService.getAssignment(aReference);
				associateGradebookAssignment = StringUtil.trimToNull(String.valueOf(assignment.getGradableObjectId()));
				submissions =  assignmentService.getSubmissions(assignment);
				if (submissions != null)
				{
					Iterator sIterator = submissions.iterator();
					while (sIterator.hasNext())
					{
						AssignmentSubmission s = (AssignmentSubmission) sIterator.next();
						AssignmentSubmissionVersion sVersion = s.getCurrentSubmissionVersion();
						submissionTable.put(s.getSubmitterId(), new UploadGradeWrapper(sVersion.getGrade(), sVersion.getSubmittedText(), sVersion.getFeedbackComment(), sVersion.getSubmittedAttachments(), sVersion.getFeedbackAttachments(), (!sVersion.isDraft() && sVersion.getTimeSubmitted() != null)?sVersion.getTimeSubmitted().toString():"", sVersion.getFeedbackText()));
					}
				}
			}
			catch (Exception e)
			{
				log.warn(e.toString());
			}
			
			// see if the user uploaded a file
		    FileItem fileFromUpload = null;
		    String fileName = null;
		    fileFromUpload = params.getFileItem("file");
			    
		    String max_file_size_mb = ServerConfigurationService.getString("content.upload.max", "1");
		    int max_bytes = 1024 * 1024;
		    try
		    {
		    		max_bytes = Integer.parseInt(max_file_size_mb) * 1024 * 1024;
		    	}
			catch(Exception e)
			{
				// if unable to parse an integer from the value
				// in the properties file, use 1 MB as a default
				max_file_size_mb = "1";
				max_bytes = 1024 * 1024;
			}
			
			if(fileFromUpload == null)
			{
				// "The user submitted a file to upload but it was too big!"
				addAlert(state, rb.getString("uploadall.size") + " " + max_file_size_mb + "MB " + rb.getString("uploadall.exceeded"));
			}
			else if (fileFromUpload.getFileName() == null || fileFromUpload.getFileName().length() == 0)
			{
				// no file
				addAlert(state, rb.getString("uploadall.alert.zipFile"));
			}
			else
			{
				byte[] fileData = fileFromUpload.get();
				    
				if(fileData.length >= max_bytes)
				{
					addAlert(state, rb.getString("uploadall.size") + " " + max_file_size_mb + "MB " + rb.getString("uploadall.exceeded"));
				}
				else if(fileData.length > 0)
				{	
					ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(fileData));
					ZipEntry entry;
					
					try
					{
						while ((entry=zin.getNextEntry()) != null)
						{
							String entryName = entry.getName();
							if (!entry.isDirectory() && entryName.indexOf("/.") == -1)
							{
								if (entryName.endsWith("grades.csv"))
								{
									if (hasGradeFile)
									{
										// read grades.cvs from zip
								        String result = StringUtil.trimToZero(readIntoString(zin));
								        String[] lines=null;
								        if (result.indexOf("\r\n") != -1)
								        	lines = result.split("\r\n");
								        else if (result.indexOf("\r") != -1)
								        		lines = result.split("\r");
								        else if (result.indexOf("\n") != -1)
							        			lines = result.split("\n");
								        for (int i = 3; i<lines.length; i++)
								        {
								        		// escape the first three header lines
								        		String[] items = lines[i].split(",");
								        		if (items.length > 4)
								        		{
								        			// has grade information
									        		try
									        		{
									        			User u = UserDirectoryService.getUserByEid(items[1]/*user eid*/);
									        			if (u != null)
									        			{
										        			UploadGradeWrapper w = (UploadGradeWrapper) submissionTable.get(u.getDisplayId());
										        			if (w != null)
										        			{
										        				String itemString = items[4];
										        				int gradeType = assignment.getTypeOfGrade();
										        				if (gradeType == AssignmentConstants.SCORE_GRADE_TYPE)
										        				{
										        					validPointGrade(state, itemString);
										        				}
										        				else
										        				{
										        					validLetterGrade(state, itemString);
										        				}
										        				if (state.getAttribute(STATE_MESSAGE) == null)
										        				{
											        				w.setGrade(gradeType == AssignmentConstants.SCORE_GRADE_TYPE?scalePointGrade(state, itemString):itemString);
											        				submissionTable.put(u.getDisplayId(), w);
										        				}
										        			}
									        			}
									        		}
									        		catch (Exception e )
									        		{
									        			log.warn(e.toString());
									        		}
								        		}
								        }
									}
								}
								else 
								{
									// get user eid part
									String userEid = "";
									if (entryName.indexOf("/") != -1)
									{
										// remove the part of zip name
										userEid = entryName.substring(entryName.indexOf("/")+1);
										// get out the user name part
										if (userEid.indexOf("/") != -1)
										{
											userEid = userEid.substring(0, userEid.indexOf("/"));
										}
										// get the eid part
										if (userEid.indexOf("(") != -1)
										{
											userEid = userEid.substring(userEid.indexOf("(")+1, userEid.indexOf(")"));
										}
										userEid=StringUtil.trimToNull(userEid);
									}
									if (submissionTable.containsKey(userEid))
									{
										if (hasComment && entryName.indexOf("comments") != -1)
										{
											// read the comments file
											String comment = getBodyTextFromZipHtml(zin);
									        if (comment != null)
									        {
									        		UploadGradeWrapper r = (UploadGradeWrapper) submissionTable.get(userEid);
									        		r.setComment(comment);
									        		submissionTable.put(userEid, r);
									        }
										}
										if (hasFeedbackText && entryName.indexOf("feedbackText") != -1)
										{
											// upload the feedback text
											String text = getBodyTextFromZipHtml(zin);
											if (text != null)
									        {
									        		UploadGradeWrapper r = (UploadGradeWrapper) submissionTable.get(userEid);
									        		r.setFeedbackText(text);
									        		submissionTable.put(userEid, r);
									        }
										}
										if (hasSubmissionText && entryName.indexOf("_submissionText") != -1)
										{
											// upload the student submission text
											String text = getBodyTextFromZipHtml(zin);
											if (text != null)
									        {
									        		UploadGradeWrapper r = (UploadGradeWrapper) submissionTable.get(userEid);
									        		r.setText(text);
									        		submissionTable.put(userEid, r);
									        }
										}
										if (hasSubmissionAttachment)
										{
											// upload the submission attachment
											String submissionFolder = "/" + rb.getString("download.submission.attachment") + "/";
											if ( entryName.indexOf(submissionFolder) != -1)
											{
												// clear the submission attachment first
												UploadGradeWrapper r = (UploadGradeWrapper) submissionTable.get(userEid);
												r.setSubmissionAttachments(new Vector());
												submissionTable.put(userEid, r);
												uploadZipAttachments(state, submissionTable, zin, entry, entryName, userEid, "submission");
											}
										}
										if (hasFeedbackAttachment)
										{
											// upload the feedback attachment
											String submissionFolder = "/" + rb.getString("download.feedback.attachment") + "/";
											if ( entryName.indexOf(submissionFolder) != -1)
											{
												// clear the submission attachment first
												UploadGradeWrapper r = (UploadGradeWrapper) submissionTable.get(userEid);
												r.setFeedbackAttachments(new Vector());
												submissionTable.put(userEid, r);
												uploadZipAttachments(state, submissionTable, zin, entry, entryName, userEid, "feedback");
											}
										}
										
										// if this is a timestamp file
										if (entryName.indexOf("timestamp") != -1)
										{
											byte[] timeStamp = readIntoBytes(zin, entryName, entry.getSize());
											UploadGradeWrapper r = (UploadGradeWrapper) submissionTable.get(userEid);
							        		r.setSubmissionTimestamp(new String(timeStamp));
							        		submissionTable.put(userEid, r);
										}
									}
								}
							}
						}
					}
					catch (IOException e) 
					{
						// uploaded file is not a valid archive
						addAlert(state, rb.getString("uploadall.alert.zipFile"));
						
					}
				}
			}
			
			if (state.getAttribute(STATE_MESSAGE) == null)
			{
				// update related submissions
				if (assignment != null && submissions != null)
				{
					Iterator sIterator = submissions.iterator();
					while (sIterator.hasNext())
					{
						AssignmentSubmission s = (AssignmentSubmission) sIterator.next();
						//TODO:zqian
						// should use displayname
						String uName = s.getSubmitterId();
						if (submissionTable.containsKey(uName))
						{
							// update the AssignmetnSubmission record
							try
							{
								AssignmentSubmissionVersion sEdit = s.getCurrentSubmissionVersion();
								
								UploadGradeWrapper w = (UploadGradeWrapper) submissionTable.get(uName);
								
								// the submission text
								if (hasSubmissionText)
								{
									sEdit.setSubmittedText(w.getText());
								}
								
								// the feedback text
								if (hasFeedbackText)
								{
									sEdit.setFeedbackText(w.getFeedbackText());
								}
								
								// the submission attachment
								if (hasSubmissionAttachment)
								{
									sEdit.setSubmittedAttachments(w.getSubmissionAttachments());
								}
								
								// the feedback attachment
								if (hasFeedbackAttachment)
								{
									sEdit.setFeedbackAttachments(w.getFeedbackAttachments());
								}
								
								// the feedback comment
								if (hasComment)
								{
									sEdit.setFeedbackComment(w.getComment());
								}
								
								// the grade file
								if (hasGradeFile)
								{
									// set grade
									String grade = StringUtil.trimToNull(w.getGrade());
									sEdit.setGrade(grade);
									/*if (grade != null && !grade.equals(rb.getString("gen.nograd")) && !grade.equals("ungraded"))
										sEdit.setGraded(true);*/
								}
								
								// release or not
								if (sEdit.getGrade() != null)
								{
									sEdit.setReturned(releaseGrades);
								}
								else
								{
									sEdit.setReturned(false);
								}
								
								if (releaseGrades && sEdit.getGrade() != null)
								{
									sEdit.setTimeReleased(new Date());
								}
								
								// if the current submission lacks timestamp while the timestamp exists inside the zip file
								if (StringUtil.trimToNull(w.getSubmissionTimeStamp()) != null && sEdit.getTimeSubmitted() == null)
								{
									sEdit.setTimeSubmitted(new Date(Long.valueOf(w.getSubmissionTimeStamp())));
								}
								
								// for further information
								boolean graded = sEdit.getGrade() != null;
								String sReference = sEdit.getReference();
								
								// commit
								assignmentService.saveSubmissionVersion(sEdit);
								
								if (releaseGrades && graded)
								{
									// update grade in gradebook
									if (associateGradebookAssignment != null)
									{
										integrateGradebook(state, aReference, associateGradebookAssignment, null, null, null, -1, null, sReference, "update");
									}
								}
								
							}
							catch (Exception ee)
							{
								log.debug(ee.toString());
							}
						}	
					}
				}
			}
		}
		
		if (state.getAttribute(STATE_MESSAGE) == null)
		{
			// go back to the list of submissions view
			cleanUploadAllContext(state);
			state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);
		}
	}


	/**
	 * This is to get the submission or feedback attachment from the upload zip file into the submission object
	 * @param state
	 * @param submissionTable
	 * @param zin
	 * @param entry
	 * @param entryName
	 * @param userEid
	 * @param submissionOrFeedback
	 */
	private void uploadZipAttachments(SessionState state, Hashtable submissionTable, ZipInputStream zin, ZipEntry entry, String entryName, String userEid, String submissionOrFeedback) {
		// upload all the files as instructor attachments to the submission for grading purpose
		String fName = entryName.substring(entryName.lastIndexOf("/") + 1, entryName.length());
		ContentTypeImageService iService = (ContentTypeImageService) state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE);
		try
		{
				// get file extension for detecting content type
				// ignore those hidden files
				String extension = "";
				if(!fName.contains(".") || (fName.contains(".") && fName.indexOf(".") != 0))
				{
					// add the file as attachment
					ResourceProperties properties = m_contentHostingService.newResourceProperties();
					properties.addProperty(ResourceProperties.PROP_DISPLAY_NAME, fName);
					
					String[] parts = fName.split("\\.");
					if(parts.length > 1)
					{
						extension = parts[parts.length - 1];
					}
					String contentType = ((ContentTypeImageService) state.getAttribute(STATE_CONTENT_TYPE_IMAGE_SERVICE)).getContentType(extension);
					ContentResourceEdit attachment = m_contentHostingService.addAttachmentResource(fName);
					attachment.setContent(readIntoBytes(zin, entryName, entry.getSize()));
					attachment.setContentType(contentType);
					attachment.getPropertiesEdit().addAll(properties);
					m_contentHostingService.commitResource(attachment);
					
		    		UploadGradeWrapper r = (UploadGradeWrapper) submissionTable.get(userEid);
		    		List attachments = submissionOrFeedback.equals("submission")?r.getSubmissionAttachments():r.getFeedbackAttachments();
		    		attachments.add(EntityManager.newReference(attachment.getReference()));
		    		if (submissionOrFeedback.equals("submission"))
		    		{
		    			r.setSubmissionAttachments(attachments);
		    		}
		    		else
		    		{
		    			r.setFeedbackAttachments(attachments);
		    		}
		    		submissionTable.put(userEid, r);
				}
		}
		catch (Exception ee)
		{
			log.warn(ee.toString());
		}
	}

	private String getBodyTextFromZipHtml(ZipInputStream zin)
	{
		String rv = "";
		try
		{
			rv = StringUtil.trimToNull(readIntoString(zin));
		}
		catch (IOException e)
		{
			log.debug(this + " " + e.toString());
		}
		if (rv != null)
		{
			int start = rv.indexOf("<body>");
			int end = rv.indexOf("</body>");
			if (start != -1 && end != -1)
			{
				// get the text in between
				rv = rv.substring(start+6, end);
			}
		}
		return rv;
	}
		private byte[] readIntoBytes(ZipInputStream zin, String fName, long length) throws IOException {
		
			StringBuilder b = new StringBuilder();
			
			byte[] buffer = new byte[4096];
			
			File f = File.createTempFile("asgnup", "tmp");
			
			FileOutputStream fout = new FileOutputStream(f);
			int len;
			while ((len = zin.read(buffer)) > 0)
			{
				fout.write(buffer, 0, len);
			}
			zin.closeEntry();
			fout.close();
			
			FileInputStream fis = new FileInputStream(f);
			FileChannel fc = fis.getChannel();
			byte[] data = new byte[(int)(fc.size())];   // fc.size returns the size of the file which backs the channel
			ByteBuffer bb = ByteBuffer.wrap(data);
			fc.read(bb);
			
			//remove the file
			fc.close(); // The file channel needs to be closed before the deletion.
			f.delete();
			
			return data;
	}
	
	private String readIntoString(ZipInputStream zin) throws IOException 
	{
		StringBuilder buffer = new StringBuilder();
		int size = 2048;
		byte[] data = new byte[2048];
		while (true)
		{
			try
			{
				size = zin.read(data, 0, data.length);
				if (size > 0)
				{
					buffer.append(new String(data, 0, size));
	             }
	             else
	             {
	                 break;
	             }
			}
			catch (IOException e)
			{
				log.debug("readIntoString " + e.toString());
			}
         }
		return buffer.toString();
	}
	/**
	 * 
	 * @return
	 */
	public void doCancel_upload_all(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_GRADE_ASSIGNMENT);
		cleanUploadAllContext(state);
	}
	
	/**
	 * clean the state variabled used by upload all process
	 */
	private void cleanUploadAllContext(SessionState state)
	{
		state.removeAttribute(UPLOAD_ALL_HAS_SUBMISSION_TEXT);
		state.removeAttribute(UPLOAD_ALL_HAS_SUBMISSION_ATTACHMENT);
		state.removeAttribute(UPLOAD_ALL_HAS_FEEDBACK_ATTACHMENT);
		state.removeAttribute(UPLOAD_ALL_HAS_FEEDBACK_TEXT);
		state.removeAttribute(UPLOAD_ALL_HAS_GRADEFILE);
		state.removeAttribute(UPLOAD_ALL_HAS_COMMENTS);
		state.removeAttribute(UPLOAD_ALL_RELEASE_GRADES);
		
	}
	

	/**
	 * Action is to preparing to go to the upload files
	 */
	public void doPrep_upload_all(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		ParameterParser params = data.getParameters();
		state.setAttribute(STATE_MODE, MODE_INSTRUCTOR_UPLOAD_ALL);

	} // doPrep_upload_all
	
	/**
	 * the UploadGradeWrapper class to be used for the "upload all" feature
	 */
	public class UploadGradeWrapper
	{
		/**
		 * the grade 
		 */
		String m_grade = null;
		
		/**
		 * the text
		 */
		String m_text = null;
		
		/**
		 * the submission attachment list
		 */
		List m_submissionAttachments = EntityManager.newReferenceList();
		
		/**
		 * the comment
		 */
		String m_comment = "";
		
		/**
		 * the timestamp
		 */
		String m_timeStamp="";
		
		/**
		 * the feedback text
		 */
		String m_feedbackText="";
		
		/**
		 * the feedback attachment list
		 */
		List m_feedbackAttachments = EntityManager.newReferenceList();

		public UploadGradeWrapper(String grade, String text, String comment, List submissionAttachments, List feedbackAttachments, String timeStamp, String feedbackText)
		{
			m_grade = grade;
			m_text = text;
			m_comment = comment;
			m_submissionAttachments = submissionAttachments;
			m_feedbackAttachments = feedbackAttachments;
			m_feedbackText = feedbackText;
			m_timeStamp = timeStamp;
		}

		/**
		 * Returns grade string
		 */
		public String getGrade()
		{
			return m_grade;
		}
		
		/**
		 * Returns the text
		 */
		public String getText()
		{
			return m_text;
		}

		/**
		 * Returns the comment string
		 */
		public String getComment()
		{
			return m_comment;
		}
		
		/**
		 * Returns the submission attachment list
		 */
		public List getSubmissionAttachments()
		{
			return m_submissionAttachments;
		}
		
		/**
		 * Returns the feedback attachment list
		 */
		public List getFeedbackAttachments()
		{
			return m_feedbackAttachments;
		}
		
		/**
		 * submission timestamp
		 * @return
		 */
		public String getSubmissionTimeStamp()
		{
			return m_timeStamp;
		}
		
		/**
		 * feedback text/incline comment
		 * @return
		 */
		public String getFeedbackText()
		{
			return m_feedbackText;
		}
		
		/**
		 * set the grade string
		 */
		public void setGrade(String grade)
		{
			m_grade = grade;
		}
		
		/**
		 * set the text
		 */
		public void setText(String text)
		{
			m_text = text;
		}
		
		/**
		 * set the comment string
		 */
		public void setComment(String comment)
		{
			m_comment = comment;
		}
		
		/**
		 * set the submission attachment list
		 */
		public void setSubmissionAttachments(List attachments)
		{
			m_submissionAttachments = attachments;
		}
		
		/**
		 * set the attachment list
		 */
		public void setFeedbackAttachments(List attachments)
		{
			m_feedbackAttachments = attachments;
		}
		
		/**
		 * set the submission timestamp
		 */
		public void setSubmissionTimestamp(String timeStamp)
		{
			m_timeStamp = timeStamp;
		}
		
		/**
		 * set the feedback text
		 */
		public void setFeedbackText(String feedbackText)
		{
			m_feedbackText = feedbackText;
		}
	}
	
	private List<DecoratedTaggingProvider> initDecoratedProviders() {
		TaggingManager taggingManager = (TaggingManager) ComponentManager
				.get("org.sakaiproject.taggable.api.TaggingManager");
		List<DecoratedTaggingProvider> providers = new ArrayList<DecoratedTaggingProvider>();
		for (TaggingProvider provider : taggingManager.getProviders())
		{
			providers.add(new DecoratedTaggingProvider(provider));
		}
		return providers;
	}
	
	private List<DecoratedTaggingProvider> addProviders(Context context, SessionState state)
	{
		String mode = (String) state.getAttribute(STATE_MODE);
		List<DecoratedTaggingProvider> providers = (List) state
				.getAttribute(mode + PROVIDER_LIST);
		if (providers == null)
		{
			providers = initDecoratedProviders();
			state.setAttribute(mode + PROVIDER_LIST, providers);
		}
		context.put("providers", providers);
		return providers;
	}
	
	private void addActivity(Context context, Assignment assignment)
	{
		AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");
		context.put("activity", assignmentActivityProducer
				.getActivity(assignment));
	}
	
	private void addItem(Context context, AssignmentSubmission submission, String userId)
	{
		AssignmentActivityProducer assignmentActivityProducer = (AssignmentActivityProducer) ComponentManager
				.get("org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer");
		context.put("item", assignmentActivityProducer
				.getItem(submission, userId));
	}
	
	private ContentReviewService contentReviewService;
	public String getReportURL(Long score) {
		getContentReviewService();
		return contentReviewService.getIconUrlforScore(score);
	}
	
	private void getContentReviewService() {
		if (contentReviewService == null)
		{
			contentReviewService = (ContentReviewService) ComponentManager.get(ContentReviewService.class.getName());
		}
	}
}	
