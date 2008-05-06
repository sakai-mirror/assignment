/**********************************************************************************
 * $URL:  $
 * $Id:  $
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

package org.sakaiproject.assignment.impl;

import java.sql.SQLException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.sakaiproject.contentreview.exception.QueueException;
import org.sakaiproject.contentreview.service.ContentReviewService;

import org.sakaiproject.assignment.model.Assignment;
import org.sakaiproject.assignment.model.AssignmentSubmissionVersion;
import org.sakaiproject.assignment.model.constants.AssignmentConstants;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.model.AssignmentSubmission;
import org.sakaiproject.assignment.model.AssignmentSubmissionVersion;
import org.sakaiproject.assignment.util.AssignmentComparator;
import org.sakaiproject.taggable.api.TaggingManager;
import org.sakaiproject.taggable.api.TaggingProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzPermissionException;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.cover.AuthzGroupService;
import org.sakaiproject.authz.cover.FunctionManager;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.content.api.ResourceType;
import org.sakaiproject.content.api.GroupAwareEntity.AccessMode;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.email.cover.EmailService;
import org.sakaiproject.email.cover.DigestService;
import org.sakaiproject.entity.api.AttachmentContainer;
import org.sakaiproject.entity.api.Edit;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityAccessOverloadException;
import org.sakaiproject.entity.api.EntityCopyrightException;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityNotDefinedException;
import org.sakaiproject.entity.api.EntityPermissionException;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.EntityTransferrer;
import org.sakaiproject.entity.api.HttpAccess;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.cover.EventTrackingService;
import org.sakaiproject.event.cover.NotificationService;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.id.cover.IdManager;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.tool.api.SessionBindingEvent;
import org.sakaiproject.tool.api.SessionBindingListener;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.BaseResourcePropertiesEdit;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.util.Blob;
import org.sakaiproject.util.DefaultEntityHandler;
import org.sakaiproject.util.SAXEntityReader;
import org.sakaiproject.util.EmptyIterator;
import org.sakaiproject.util.EntityCollections;
import org.sakaiproject.util.FormattedText;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.SortedIterator;
import org.sakaiproject.util.StorageUser;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Validator;
import org.sakaiproject.util.Web;
import org.sakaiproject.util.Xml;
import org.sakaiproject.util.commonscodec.CommonsCodecBase64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
//import org.sakaiproject.genericdao.hibernate.HibernateCompleteGenericDao;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;


/**
 * <p>
 * AssignmentServiceImpl is the implementation service class for AssignmentService.
 * </p>
 */
public class AssignmentServiceImpl extends HibernateDaoSupport implements AssignmentService, EntityTransferrer
{
	/** Our logger. */
	private static Log log = LogFactory.getLog(AssignmentServiceImpl.class);

	/** the resource bundle */
	private static ResourceLoader rb;

	/** The access point URL. */
	protected String m_relativeAccessPoint = null;
	
	private static final String NEW_ASSIGNMENT_DUE_DATE_SCHEDULED = "new_assignment_due_date_scheduled";

	protected static final String GROUP_LIST = "group";

	protected static final String GROUP_NAME = "authzGroup";
	
	// the file types for zip download
	protected static final String ZIP_COMMENT_FILE_TYPE = ".txt";
	protected static final String ZIP_SUBMITTED_TEXT_FILE_TYPE = ".html";

	// the names of queries
	protected static final String GET_ASSIGNMENT_BY_ID = "getAssignmentById";
	protected static final String GET_ASSIGNMENTS_BY_CONTEXT = "getAssignmentsByContext";
	protected static final String GET_ALL_SUBMISSIONS_FOR_CONTEXT= "getAllSubmissionsForContext";
	protected static final String GET_SUBMISSION_BY_ASSIGNMENT_USER = "getSubmissionByAssignmentUser";
		
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
	
	public void setContentReviewService(ContentReviewService contentReviewService) {
		this.contentReviewService = contentReviewService;
	}

	/**
	 * Access the partial URL that forms the root of resource URLs.
	 * 
	 * @param relative -
	 *        if true, form within the access path only (i.e. starting with /msg)
	 * @return the partial URL that forms the root of resource URLs.
	 */
	protected String getAccessPoint(boolean relative)
	{
		return (relative ? "" : m_serverConfigurationService.getAccessUrl()) + m_relativeAccessPoint;

	} // getAccessPoint

	/**
	 * Access the internal reference which can be used to assess security clearance.
	 * 
	 * @param id
	 *        The assignment id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String assignmentReference(String context, String id)
	{
		String retVal = null;
		if (context == null)
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + id;
		else
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR + id;
		return retVal;

	} // assignmentReference
	
	/**
	 * Access the internal reference which can be used to assess security clearance.
	 * 
	 * @param id
	 *        The assignment id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String assignmentReference(Assignment a)
	{
		return assignmentReference(a.getContext(), String.valueOf(a.getId()));

	} // assignmentReference

	/**
	 * Access the internal reference which can be used to access the resource from within the system.
	 * 
	 * @param id
	 *        The submission id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String submissionReference(String context, String id, String assignmentId)
	{
		String retVal = null;
		if (context == null)
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "s" + Entity.SEPARATOR + id;
		else
			retVal = getAccessPoint(true) + Entity.SEPARATOR + "s" + Entity.SEPARATOR + context + Entity.SEPARATOR + assignmentId
					+ Entity.SEPARATOR + id;
		return retVal;

	} // submissionReference
	
	/**
	 * Access the internal reference which can be used to access the resource from within the system.
	 * 
	 * @param id
	 *        The submission id string.
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String submissionReference(AssignmentSubmission submission)
	{
		return submissionReference(submission.getAssignment().getContext(), String.valueOf(submission.getId()), String.valueOf(submission.getAssignment().getId()));

	} // submissionReference

	/**
	 * Access the assignment id extracted from an assignment reference.
	 * 
	 * @param ref
	 *        The assignment reference string.
	 * @return The the assignment id extracted from an assignment reference.
	 */
	protected String assignmentId(String ref)
	{
		int i = ref.lastIndexOf(Entity.SEPARATOR);
		if (i == -1) return ref;
		String id = ref.substring(i + 1);
		return id;

	} // assignmentId

	/**
	 * Access the submission id extracted from a submission reference.
	 * 
	 * @param ref
	 *        The submission reference string.
	 * @return The the submission id extracted from a submission reference.
	 */
	protected String submissionId(String ref)
	{
		int i = ref.lastIndexOf(Entity.SEPARATOR);
		if (i == -1) return ref;
		String id = ref.substring(i + 1);
		return id;

	} // submissionId

	/**
	 * Check security permission.
	 * 
	 * @param lock -
	 *        The lock id string.
	 * @param resource -
	 *        The resource reference string, or null if no resource is involved.
	 * @return true if allowed, false if not
	 */
	protected boolean unlockCheck(String lock, String resource)
	{
		if (!SecurityService.unlock(lock, resource))
		{
			return false;
		}

		return true;

	}// unlockCheck

	/**
	 * Check security permission.
	 * 
	 * @param lock1
	 *        The lock id string.
	 * @param lock2
	 *        The lock id string.
	 * @param resource
	 *        The resource reference string, or null if no resource is involved.
	 * @return true if either allowed, false if not
	 */
	protected boolean unlockCheck2(String lock1, String lock2, String resource)
	{
		// check the first lock
		if (SecurityService.unlock(lock1, resource)) return true;

		// if the second is different, check that
		if ((lock1 != lock2) && (SecurityService.unlock(lock2, resource))) return true;

		return false;

	} // unlockCheck2

	/**
	 * Check security permission.
	 * 
	 * @param lock -
	 *        The lock id string.
	 * @param resource -
	 *        The resource reference string, or null if no resource is involved.
	 * @exception PermissionException
	 *            Thrown if the user does not have access
	 */
	protected void unlock(String lock, String resource) throws PermissionException
	{
		if (!unlockCheck(lock, resource))
		{
			throw new PermissionException(SessionManager.getCurrentSessionUserId(), lock, resource);
		}

	} // unlock

	/**
	 * Check security permission.
	 * 
	 * @param lock1
	 *        The lock id string.
	 * @param lock2
	 *        The lock id string.
	 * @param resource
	 *        The resource reference string, or null if no resource is involved.
	 * @exception PermissionException
	 *            Thrown if the user does not have access to either.
	 */
	protected void unlock2(String lock1, String lock2, String resource) throws PermissionException
	{
		if (!unlockCheck2(lock1, lock2, resource))
		{
			throw new PermissionException(SessionManager.getCurrentSessionUserId(), lock1 + "/" + lock2, resource);
		}

	} // unlock2

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Dependencies and their setter methods
	 *********************************************************************************************************************************************************************************************************************************************************/
	
	/** Dependency: ContentHostingService. */
	protected ContentHostingService m_contentHostingService = null;

	/**
	 * Dependency:ContentHostingService.
	 * 
	 * @param service
	 *        The ContentHostingService.
	 */
	public void setContentHostingService(ContentHostingService service)
	{
		m_contentHostingService = service;
	}

	/** Dependency: EntityManager. */
	protected EntityManager m_entityManager = null;

	/**
	 * Dependency: EntityManager.
	 * 
	 * @param service
	 *        The EntityManager.
	 */
	public void setEntityManager(EntityManager service)
	{
		m_entityManager = service;
	}

	/** Dependency: ServerConfigurationService. */
	protected ServerConfigurationService m_serverConfigurationService = null;

	/**
	 * Dependency: ServerConfigurationService.
	 * 
	 * @param service
	 *        The ServerConfigurationService.
	 */
	public void setServerConfigurationService(ServerConfigurationService service)
	{
		m_serverConfigurationService = service;
	}

	/** Dependency: TaggingManager. */
	protected TaggingManager m_taggingManager = null;

	/**
	 * Dependency: TaggingManager.
	 * 
	 * @param manager
	 *        The TaggingManager.
	 */
	public void setTaggingManager(TaggingManager manager)
	{
		m_taggingManager = manager;
	}

	/** Dependency: allowGroupAssignments setting */
	protected boolean m_allowGroupAssignments = true;

	/**
	 * Dependency: allowGroupAssignments
	 * 
	 * @param allowGroupAssignments
	 *        the setting
	 */
	public void setAllowGroupAssignments(boolean allowGroupAssignments)
	{
		m_allowGroupAssignments = allowGroupAssignments;
	}
	/**
	 * Get
	 * 
	 * @return allowGroupAssignments
	 */
	public boolean getAllowGroupAssignments()
	{
		return m_allowGroupAssignments;
	}
	
	/** Dependency: allowGroupAssignmentsInGradebook setting */
	protected boolean m_allowGroupAssignmentsInGradebook = true;

	/**
	 * Dependency: allowGroupAssignmentsInGradebook
	 * 
	 * @param allowGroupAssignmentsInGradebook
	 */
	public void setAllowGroupAssignmentsInGradebook(boolean allowGroupAssignmentsInGradebook)
	{
		m_allowGroupAssignmentsInGradebook = allowGroupAssignmentsInGradebook;
	}
	/**
	 * Get
	 * 
	 * @return allowGroupAssignmentsGradebook
	 */
	public boolean getAllowGroupAssignmentsInGradebook()
	{
		return m_allowGroupAssignmentsInGradebook;
	}
	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		m_relativeAccessPoint = AssignmentConstants.REFERENCE_ROOT;
		log.info(this + " init()");

		// register as an entity producer
		m_entityManager.registerEntityProducer(this, AssignmentConstants.REFERENCE_ROOT);

		// register functions
		FunctionManager.registerFunction(SECURE_ALL_GROUPS);
		FunctionManager.registerFunction(SECURE_ADD_ASSIGNMENT);
		FunctionManager.registerFunction(SECURE_ADD_ASSIGNMENT_SUBMISSION);
		FunctionManager.registerFunction(SECURE_REMOVE_ASSIGNMENT);
		FunctionManager.registerFunction(SECURE_ACCESS_ASSIGNMENT);
		FunctionManager.registerFunction(SECURE_UPDATE_ASSIGNMENT);
		FunctionManager.registerFunction(SECURE_GRADE_ASSIGNMENT_SUBMISSION);
		FunctionManager.registerFunction(SECURE_ASSIGNMENT_RECEIVE_NOTIFICATIONS);
		FunctionManager.registerFunction(SECURE_SHARE_DRAFTS);
		
 		//if no contentReviewService was set try discovering it
 		if (contentReviewService == null)
 		{
 			contentReviewService = (ContentReviewService) ComponentManager.get(ContentReviewService.class.getName());
 		}
 		
 		/*if (rb==null)
 			rb = new ResourceLoader("assignment");*/
	} // init

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		log.info(this + " destroy()");
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * AssignmentService implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * new assignment
	 */
	public Assignment newAssignment(String context) throws PermissionException
	{
		Assignment a = new Assignment();
		a.setContext(context);
		return a;

	} // addAssignment

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
	public Assignment mergeAssignment(Element el) throws IdInvalidException, IdUsedException, PermissionException
	{		
		// TODO:zqian
		return null;
	}

	/**
	 * Creates and adds a new Assignment to the service which is a copy of an existing Assignment.
	 * 
	 * @param assignmentId -
	 *        The Assignment to be duplicated.
	 * @return The new Assignment object, or null if the original Assignment does not exist.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public Assignment addDuplicateAssignment(String context, String assignmentReference) throws PermissionException,
			IdInvalidException, IdUsedException, IdUnusedException
	{
		if (log.isDebugEnabled())
			log.debug(this + " ENTERING ADD DUPLICATE ASSIGNMENT WITH ID : " + assignmentReference);

		/*Assignment retVal = null;

		if (assignmentReference != null)
		{
			String assignmentId = assignmentId(assignmentReference);
			if (!m_assignmentStorage.check(assignmentId))
				throw new IdUnusedException(assignmentId);
			else
			{
				if (log.isDebugEnabled())
					log.debug(this + " addDuplicateAssignment : assignment exists - will copy");

				Assignment existingAssignment = getAssignment(assignmentReference);

				retVal = addAssignment(context);
				retVal.setTitle(existingAssignment.getTitle() + " - Copy");
				retVal.setOpenTime(existingAssignment.getOpenTime());
				retVal.setDueTime(existingAssignment.getDueTime());
				retVal.setDropDeadTime(existingAssignment.getDropDeadTime());
				retVal.setCloseTime(existingAssignment.getCloseTime());
				retVal.setDraft(true);
				ResourcePropertiesEdit pEdit = (BaseResourcePropertiesEdit) retVal.getProperties();
				pEdit.addAll(existingAssignment.getProperties());
				addLiveProperties(pEdit);
			}
		}

		if (log.isDebugEnabled())
			log.debug(this + " ADD DUPLICATE ASSIGNMENT : LEAVING ADD DUPLICATE ASSIGNMENT WITH ID : "
					+ retVal.getId());

		return retVal;*/
		//TODO:zqian
		return null;
	}

	/**
	 * Access the Assignment with the specified reference.
	 * 
	 * @param assignmentReference -
	 *        The reference of the Assignment.
	 * @return The Assignment corresponding to the reference, or null if it does not exist.
	 * @throws IdUnusedException
	 *         if there is no object with this reference.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public Assignment getAssignment(String assignmentReference) throws IdUnusedException, PermissionException
	{
		if (log.isDebugEnabled()) log.debug(this + " GET ASSIGNMENT : REF : " + assignmentReference);

		// check security on the assignment
		unlockCheck(SECURE_ACCESS_ASSIGNMENT, assignmentReference);
		
		Assignment assignment = findAssignment(assignmentReference);
		
		if (assignment == null) throw new IdUnusedException(assignmentReference);

		return assignment;

	}// getAssignment
	
	protected Assignment findAssignment(String assignmentReference)
	{
		Assignment assignment = null;

		String assignmentId = assignmentId(assignmentReference);

		List<Assignment> rvList = (getHibernateTemplate().findByNamedQueryAndNamedParam(GET_ASSIGNMENT_BY_ID, "id", assignmentId));
		if (rvList != null && rvList.size() == 1)
		{
			return rvList.get(0);
		}
		return null;
	}

	/**
	 * Access all assignment objects - known to us (not from external providers).
	 * 
	 * @return A list of assignment objects.
	 */
	protected List getAssignments(String context)
	{
		return assignments(context, null);

	} // getAssignments
	
	/**
	 * Access all assignment objects - known to us (not from external providers) and accessible by the user
	 * 
	 * @return A list of assignment objects.
	 */
	protected List getAssignments(String context, String userId)
	{
		return assignments(context, userId);

	} // getAssignments

	//
	private List assignments(String context, String userId) 
	{
		if (userId == null)
		{
			userId = SessionManager.getCurrentSessionUserId();
		}
		List<Assignment> assignments = getHibernateTemplate().findByNamedQueryAndNamedParam(GET_ALL_SUBMISSIONS_FOR_CONTEXT, "context", context);
		if (assignments == null)
		{
			return new Vector<Assignment>();
		}

		List rv = new Vector();
		
		// check for the allowed groups of the current end use if we need it, and only once
		Collection allowedGroups = null;
		Site site = null;
		try
		{
			site = SiteService.getSite(context);
		}
		catch (IdUnusedException e)
		{
			log.warn(this + " assignments(String, String) " + e.getMessage() + " context=" + context);
		}
		
		for (int x = 0; x < assignments.size(); x++)
		{
			Assignment tempAssignment = (Assignment) assignments.get(x);
			// check the assignment's groups to the allowed (get) groups for the current user
			Collection asgGroups = tempAssignment.getGroups();
			
			if (asgGroups != null && asgGroups.size() > 0)
			{
				
				// Can at least one of the designated groups been found
				boolean groupFound = false;
				
				// if grouped, check that the end user has get access to any of this assignment's groups; reject if not

				for (Iterator iAsgGroups=asgGroups.iterator(); site!=null && !groupFound && iAsgGroups.hasNext();)
				{
					String groupId = (String) iAsgGroups.next();
					try
					{
						if (site.getGroup(groupId) != null)
						{
							groupFound = true;
						}
					}
					catch (Exception ee)
					{
						log.warn(this + " assignments(String, String) " + ee.getMessage() + " groupId = " + groupId);
					}
					
				}
				
				if (!groupFound)
				{
					// if none of the group exists, mark the assignment as draft and list it
					String assignmentId = String.valueOf(tempAssignment.getId());
					try
					{
						Assignment a = getAssignment(assignmentId);
						a.setDraft(true);
						saveAssignment(a);
						rv.add(getAssignment(assignmentId));
					}
					catch (Exception e)
					{
						log.warn(this + " assignments(String, String) " + e.getMessage() + " assignment id =" + assignmentId);
						continue;
					}
				}
				else
				{
					// we need the allowed groups, so get it if we have not done so yet
					if (allowedGroups == null)
					{
						allowedGroups = getGroupsAllowGetAssignment(context, userId);
					}
					
					// reject if there is no intersection
					if (!isIntersectionGroupRefsToGroups(asgGroups, allowedGroups)) continue;
					
					rv.add(tempAssignment);
				}
			}
			else
			{
				/// if not reject, add it
				rv.add(tempAssignment);
			}
		}

		return rv;
	}

	/**
	 * See if the collection of group reference strings has at least one group that is in the collection of Group objects.
	 * 
	 * @param groupRefs
	 *        The collection (String) of group references.
	 * @param groups
	 *        The collection (Group) of group objects.
	 * @return true if there is interesection, false if not.
	 */
	protected boolean isIntersectionGroupRefsToGroups(Collection groupRefs, Collection groups)
	{	
		for (Iterator iRefs = groupRefs.iterator(); iRefs.hasNext();)
		{
			String findThisGroupRef = (String) iRefs.next();
			for (Iterator iGroups = groups.iterator(); iGroups.hasNext();)
			{
				String thisGroupRef = ((Group) iGroups.next()).getReference();
				if (thisGroupRef.equals(findThisGroupRef))
				{
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Save the assignment
	 */
	public void saveAssignment(Assignment assignment)
	{
		try 
		{
			getHibernateTemplate().saveOrUpdate(assignment);
		}
		catch (DataAccessException e)
		{
			e.printStackTrace();
			log.warn(this + ".saveAssignment() Hibernate could not save assignment=" + assignment.getId());
		}

	} // saveAssignment

	/**
	 * Removes this Assignment and all references to it.
	 * 
	 * @param assignment -
	 *        The Assignment to remove.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public void removeAssignment(Assignment assignment) throws PermissionException
	{
		if (assignment != null)
		{
			if (log.isDebugEnabled()) log.debug(this + " removeAssignment with id : " + assignment.getId());

			// CHECK PERMISSION
			unlock(SECURE_REMOVE_ASSIGNMENT, assignmentReference(assignment));

			try {
				getHibernateTemplate().delete(assignment);
			} catch (DataAccessException e) {
				log.warn(this + ".removeAssignment(): Hibernate could not delete: " + e.toString());
				e.printStackTrace();
			}
			log.info(this + ".removeAssignment(): Assignment id " + assignment.getId() + " deleted");
		}

	}// removeAssignment

	/**
	 * {@inheritDoc}
	 */
	public AssignmentSubmission newSubmission(String assignmentId, String submitterId) throws PermissionException
	{
		AssignmentSubmission submission = new AssignmentSubmission();
		
		try
		{
			Assignment a = getAssignment(assignmentId);
			
			String key = submissionReference(submission);
			if (log.isDebugEnabled()) log.debug(this + ".newSubmission() : SUB REF : " + key);

			unlock(SECURE_ADD_ASSIGNMENT_SUBMISSION, key);
			submission.setAssignment(a);
			return submission;
		}
		catch (Exception e)
		{
			if (log.isDebugEnabled()) log.debug(this + ".newSubmission() : exception : assignment id=" + assignmentId);
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public AssignmentSubmissionVersion newSubmissionVersion(String submissionReference) throws PermissionException
	{
		AssignmentSubmissionVersion submissionVersion = new AssignmentSubmissionVersion();
		
		try
		{
			AssignmentSubmission submission = getSubmission(submissionReference);

			unlock(SECURE_ADD_ASSIGNMENT_SUBMISSION, submissionReference);
			submissionVersion.setAssignmentSubmission(submission);
			return submissionVersion;
		}
		catch (Exception e)
		{
			if (log.isDebugEnabled()) log.debug(this + ".newSubmissionVersion() : exception : submission id=" + submissionReference);
		}
		
		return null;
	}

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
	public AssignmentSubmission mergeSubmission(Element el) throws IdInvalidException, IdUsedException, PermissionException
	{
		//TODO: zqian
		// construct from the XML
		/*AssignmentSubmission submissionFromXml = new AssignmentSubmission(el);

		// check for a valid submission name
		if (!Validator.checkResourceId(submissionFromXml.getId())) throw new IdInvalidException(submissionFromXml.getId());

		// check security (throws if not permitted)
		unlock(SECURE_ADD_ASSIGNMENT_SUBMISSION, submissionFromXml.getReference());

		// reserve a submission with this id from the info store - if it's in use, this will return null
		AssignmentSubmission submission = m_submissionStorage.put(	submissionFromXml.getId(), 
																		submissionFromXml.getAssignmentId(),
																		submissionFromXml.getSubmitterIdString(),
																		(submissionFromXml.getSubmittedTime() != null)?String.valueOf(submissionFromXml.getSubmittedTime().getTime()):null,
																		Boolean.valueOf(submissionFromXml.getSubmitted()).toString(),
																		Boolean.valueOf(submissionFromXml.getGraded()).toString());
		if (submission == null)
		{
			throw new IdUsedException(submissionFromXml.getId());
		}

		// transfer from the XML read submission object to the SubmissionEdit
		((BaseAssignmentSubmission) submission).set(submissionFromXml);
		*/
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void saveSubmission(AssignmentSubmission submission)
	{
		try 
		{
			getHibernateTemplate().saveOrUpdate(submission);
		}
		catch (DataAccessException e)
		{
			e.printStackTrace();
			log.warn(this + ".saveAssignment() Hibernate could not save submission=" + submission.getId());
		}
	}	// saveSubmission
	

	/**
	 * {@inheritDoc}
	 */
	public void saveSubmissionVersion(AssignmentSubmissionVersion submissionVersion)
	{
		try 
		{
			getHibernateTemplate().saveOrUpdate(submissionVersion);
		}
		catch (DataAccessException e)
		{
			e.printStackTrace();
			log.warn(this + ".saveAssignment() Hibernate could not save submission=" + submissionVersion.getId());
		}
		try
		{
			Assignment a = submissionVersion.getAssignmentSubmission().getAssignment();
			
			Date returnedTime = submissionVersion.getReleasedTime();
			Date submittedTime = submissionVersion.getSubmittedTime();
			
			// track it
			if (submissionVersion.isDraft())
			{
				// saving a submission
				//EventTrackingService.post(EventTrackingService.newEvent(EVENT_SAVE_ASSIGNMENT_SUBMISSION, submissionRef, true));
			}
			else if (returnedTime == null && !submissionVersion.isReturned() && (submittedTime == null /*grading non-submissions*/
																|| (submittedTime != null && (submissionVersion.getLastModifiedTime().getTime() - submittedTime.getTime()) > 1000*60 /*make sure the last modified time is at least one minute after the submit time*/)))
			{
				// graded and saved before releasing it
				//EventTrackingService.post(EventTrackingService.newEvent(EVENT_GRADE_ASSIGNMENT_SUBMISSION, submissionRef, true));
			}
			else if (returnedTime != null && submissionVersion.getGrade() != null && (submittedTime == null/*returning non-submissions*/ 
											|| (submittedTime != null && returnedTime.after(submittedTime))/*returning normal submissions*/ 
											|| (submittedTime != null && submittedTime.after(returnedTime) && submissionVersion.getLastModifiedTime().after(submittedTime))/*grading the resubmitted assignment*/))
			{
				// releasing a submitted assignment or releasing grade to an unsubmitted assignment
				//EventTrackingService.post(EventTrackingService.newEvent(EVENT_GRADE_ASSIGNMENT_SUBMISSION, submissionRef, true));
			}
			else if (submittedTime == null) /*grading non-submission*/
			{
				// releasing a submitted assignment or releasing grade to an unsubmitted assignment
				//EventTrackingService.post(EventTrackingService.newEvent(EVENT_GRADE_ASSIGNMENT_SUBMISSION, submissionRef, true));
			}
			else
			{
				// submitting a submission
				//EventTrackingService.post(EventTrackingService.newEvent(EVENT_SUBMIT_ASSIGNMENT_SUBMISSION, submissionRef, true));
			
				// only doing the notification for real online submissions
				if (a.getTypeOfSubmission() != AssignmentConstants.NON_ELECTRONIC_ASSIGNMENT_SUBMISSION)
				{
					// instructor notification
					notificationToInstructors(submissionVersion, a);
					
					// student notification, whether the student gets email notification once he submits an assignment
					notificationToStudent(submissionVersion);
				}
			}
				
			
		}
		catch (Exception e)
		{
			log.warn(this + " commitEdit(), submissionId=" + submissionVersion.getId(), e);
		}

	} // saveSubmissionVersion

	/**
	 * send notification to instructor type of users if necessary
	 * @param s
	 * @param a
	 */
	private void notificationToInstructors(AssignmentSubmissionVersion s, Assignment a) 
	{
		String notiOption = String.valueOf(a.getNotificationType());
		if (notiOption != null && !notiOption.equals(AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_NONE))
		{
			// need to send notification email
			String context = a.getContext();
			
			// compare the list of users with the receive.notifications and list of users who can actually grade this assignment
			List receivers = allowReceiveSubmissionNotificationUsers(context);
			List allowGradeAssignmentUsers = allowGradeAssignmentUsers(assignmentReference(a));
			receivers.retainAll(allowGradeAssignmentUsers);
			
			String submitterId = s.getCreatedBy();
			
			// filter out users who's not able to grade this submission
			List finalReceivers = new Vector();
			
			HashSet receiverSet = new HashSet();
			Collection groups = a.getGroups();
			if (groups != null && groups.size() > 0)
			{
				for (Iterator gIterator = groups.iterator(); gIterator.hasNext();)
				{
					String g = (String) gIterator.next();
					try
					{
						AuthzGroup aGroup = AuthzGroupService.getAuthzGroup(g);
						if (aGroup.isAllowed(submitterId, AssignmentService.SECURE_ADD_ASSIGNMENT_SUBMISSION))
						{
							for (Iterator rIterator = receivers.iterator(); rIterator.hasNext();)
							{
								User rUser = (User) rIterator.next();
								String rUserId = rUser.getId();
								if (!receiverSet.contains(rUserId) && aGroup.isAllowed(rUserId, AssignmentService.SECURE_GRADE_ASSIGNMENT_SUBMISSION))
								{
									finalReceivers.add(rUser);
									receiverSet.add(rUserId);
								}
							}
						}
					}
					catch (Exception e)
					{
						log.warn(this + " notificationToInstructors, group id =" + g + " " + e.getMessage());
					}
				}
			}
			else
			{
				finalReceivers.addAll(receivers);
			}
			
			String messageBody = getNotificationMessage(s);
			
			if (notiOption.equals(AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_EACH))
			{
				// send the message immediately
				EmailService.sendToUsers(finalReceivers, getHeaders(null), messageBody);
			}
			else if (notiOption.equals(AssignmentConstants.ASSIGNMENT_INSTRUCTOR_NOTIFICATIONS_DIGEST))
			{
				// digest the message to each user
				for (Iterator iReceivers = finalReceivers.iterator(); iReceivers.hasNext();)
				{
					User user = (User) iReceivers.next();
					DigestService.digest(user.getId(), getSubject(), messageBody);
				}
			}
		}
	}

	/**
	 * send notification to student if necessary
	 * @param s
	 */
	private void notificationToStudent(AssignmentSubmissionVersion s) 
	{
		if (m_serverConfigurationService.getBoolean("assignment.submission.confirmation.email", true))
		{
			//send notification
			User u = UserDirectoryService.getCurrentUser();
			
			if (StringUtil.trimToNull(u.getEmail()) != null)
			{
				List receivers = new Vector();
				receivers.add(u);
				
				EmailService.sendToUsers(receivers, getHeaders(u.getEmail()), getNotificationMessage(s));
			}
		}
	}
	
	protected List<String> getHeaders(String receiverEmail)
	{
		List<String> rv = new Vector<String>();
		
		rv.add("MIME-Version: 1.0");
		rv.add("Content-Type: multipart/alternative; boundary=\""+MULTIPART_BOUNDARY+"\"");
		// set the subject
		rv.add(getSubject());

		// from
		rv.add(getFrom());
		
		// to
		if (StringUtil.trimToNull(receiverEmail) != null)
		{
			rv.add("To: " + receiverEmail);
		}
		
		return rv;
	}
	
	protected String getSubject()
	{
		return rb.getString("noti.subject.label") + " " + rb.getString("noti.subject.content");
	}
	
	protected String getFrom()
	{
		return "From: " + "\"" + m_serverConfigurationService.getString("ui.service", "Sakai") + "\"<no-reply@"+ m_serverConfigurationService.getServerName() + ">";
	}
	
	private final String MULTIPART_BOUNDARY = "======sakai-multi-part-boundary======";
	private final String BOUNDARY_LINE = "\n\n--"+MULTIPART_BOUNDARY+"\n";
	private final String TERMINATION_LINE = "\n\n--"+MULTIPART_BOUNDARY+"--\n\n";
	private final String MIME_ADVISORY = "This message is for MIME-compliant mail readers.";
	
	/**
	 * Get the message for the email.
	 * 
	 * @param event
	 *        The event that matched criteria to cause the notification.
	 * @return the message for the email.
	 */
	protected String getNotificationMessage(AssignmentSubmissionVersion s)
	{	
		StringBuilder message = new StringBuilder();
		message.append(MIME_ADVISORY);
		message.append(BOUNDARY_LINE);
		message.append(plainTextHeaders());
		message.append(plainTextContent(s));
		message.append(BOUNDARY_LINE);
		message.append(htmlHeaders());
		message.append(htmlPreamble());
		message.append(htmlContent(s));
		message.append(htmlEnd());
		message.append(TERMINATION_LINE);
		return message.toString();
	}
	
	protected String plainTextHeaders() {
		return "Content-Type: text/plain\n\n";
	}
	
	protected String plainTextContent(AssignmentSubmissionVersion s) {
		return htmlContent(s);
	}
	
	protected String htmlHeaders() {
		return "Content-Type: text/html\n\n";
	}
	
	protected String htmlPreamble() {
		StringBuilder buf = new StringBuilder();
		buf.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n");
		buf.append("    \"http://www.w3.org/TR/html4/loose.dtd\">\n");
		buf.append("<html>\n");
		buf.append("  <head><title>");
		buf.append(getSubject());
		buf.append("</title></head>\n");
		buf.append("  <body>\n");
		return buf.toString();
	}
	
	protected String htmlEnd() {
		return "\n  </body>\n</html>\n";
	}

	private String htmlContent(AssignmentSubmissionVersion s) 
	{
		String newline = "<br />\n";
		
		Assignment a = s.getAssignmentSubmission().getAssignment();
		
		String context = a.getContext();
		
		String siteTitle = "";
		String siteId = "";
		try
		{
			Site site = SiteService.getSite(context);
			siteTitle = site.getTitle();
			siteId = site.getId();
		}
		catch (Exception ee)
		{
			log.warn(this + " htmlContent(), site id =" + context + " " + ee.getMessage());
		}
		
		StringBuilder buffer = new StringBuilder();
		// site title and id
		buffer.append(rb.getString("noti.site.title") + " " + siteTitle + newline);
		buffer.append(rb.getString("noti.site.id") + " " + siteId +newline + newline);
		// assignment title and due date
		buffer.append(rb.getString("noti.assignment") + " " + a.getTitle()+newline);
		buffer.append(rb.getString("noti.assignment.duedate") + " " + a.getDueTime().toString()+newline + newline);
		// submitter name and id
		String submitterId = s.getCreatedBy();
		buffer.append(rb.getString("noti.student"));
		try
		{
			User u = UserDirectoryService.getUser(submitterId);
			buffer.append(" " + u.getDisplayName());
		}
		catch (Exception e)
		{

		}
		buffer.append("( " + submitterId + " )");
		
		buffer.append(newline + newline);
		
		// submit time
		buffer.append(rb.getString("noti.submit.id") + " " + s.getId() + newline);
		
		// submit time 
		buffer.append(rb.getString("noti.submit.time") + " " + s.getSubmittedTime().toString() + newline + newline);
		
		// submit text
		String text = StringUtil.trimToNull(s.getSubmittedText());
		if ( text != null)
		{
			buffer.append(rb.getString("noti.submit.text") + newline + newline + Validator.escapeHtmlFormattedText(text) + newline + newline);
		}
		
		// attachment if any
		List attachments = s.getSubmittedAttachments();
		if (attachments != null && attachments.size() >0)
		{
			buffer.append(rb.getString("noti.submit.attachments") + newline + newline);
			for (int j = 0; j<attachments.size(); j++)
			{
				Reference r = (Reference) attachments.get(j);
				buffer.append(r.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME) + "(" + r.getProperties().getPropertyFormatted(ResourceProperties.PROP_CONTENT_LENGTH)+ ")\n");
			}
		}
		
		return buffer.toString();
	}

	/**
	 * Removes an AssignmentSubmission and all references to it
	 * 
	 * @param submission -
	 *        the AssignmentSubmission to remove.
	 * @throws PermissionException
	 *         if current User does not have permission to do this.
	 */
	public void removeSubmission(AssignmentSubmission submission) throws PermissionException
	{
		if (submission != null)
		{
			String submissionReference = submissionReference(submission);
			// check security
			unlock(SECURE_REMOVE_ASSIGNMENT_SUBMISSION, submissionReference);

			try {
				getHibernateTemplate().delete(submission);
			} catch (DataAccessException e) {
				log.warn(this + ".removeSubmission(): Hibernate could not delete: " + e.toString());
				e.printStackTrace();
			}
			log.info(this + ".removeSubmission(): submission id " + submission.getId() + " deleted");

			// track event
			//EventTrackingService.post(EventTrackingService.newEvent(EVENT_REMOVE_ASSIGNMENT_SUBMISSION, submission.getReference(), true));

			// remove any realm defined for this resource
			try
			{
				AuthzGroupService.removeAuthzGroup(AuthzGroupService.getAuthzGroup(submissionReference));
			}
			catch (AuthzPermissionException e)
			{
				log.warn(this + " removeSubmission: removing realm for : " + submissionReference + " : " + e.getMessage());
			}
			catch (GroupNotDefinedException ignore)
			{
			}
		}
	}// removeSubmission

	/**
	 *@inheritDoc
	 */
	public int getSubmissionsSize(String context)
	{
		int size = 0;
		
		List submissions = getSubmissions(context);
		if (submissions != null)
		{
			size = submissions.size();
		}
		return size;
	}
	
	/**
	 * Access all AssignmentSubmission objects - known to us (not from external providers).
	 * 
	 * @return A list of AssignmentSubmission objects.
	 */
	protected List getSubmissions(String context)
	{
		List<AssignmentSubmission> submissions = (getHibernateTemplate().findByNamedQueryAndNamedParam(GET_ALL_SUBMISSIONS_FOR_CONTEXT, "context", context));

		return submissions;

	} // getAssignmentSubmissions

	/**
	 * Access all the Assignemnts associated with the context
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return Iterator over all the Assignments associated with the context and the user.
	 */
	public Iterator getAssignmentsForContext(String context)
	{
		if (log.isDebugEnabled()) log.debug(this + " GET ASSIGNMENTS FOR CONTEXT : CONTEXT : " + context);
		
		return assignmentsForContextAndUser(context, null);

	}
	
	/**
	 * Access all the Assignemnts associated with the context and the user
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel()
	 * @return Iterator over all the Assignments associated with the context and the user
	 */
	public Iterator getAssignmentsForContext(String context, String userId)
	{
		if (log.isDebugEnabled()) log.debug(this + " GET ASSIGNMENTS FOR CONTEXT : CONTEXT : " + context);
		
		return assignmentsForContextAndUser(context, userId);

	}

	/**
	 * get proper assignments for specified context and user
	 * @param context
	 * @param user
	 * @return
	 */
	private Iterator assignmentsForContextAndUser(String context, String userId) 
	{
		Assignment tempAssignment = null;
		Vector retVal = new Vector();
		List allAssignments = null;

		if (context != null)
		{
			allAssignments = getAssignments(context, userId);
			
			for (int x = 0; x < allAssignments.size(); x++)
			{
				tempAssignment = (Assignment) allAssignments.get(x);

				if ((context.equals(tempAssignment.getContext()))
						|| (context.equals(getGroupNameFromContext(tempAssignment.getContext()))))
				{
					retVal.add(tempAssignment);
				}
			}
		}

		if (retVal.isEmpty())
			return new EmptyIterator();
		else
			return retVal.iterator();
	}

	/**
	 * @inheritDoc
	 */
	public List getListAssignmentsForContext(String context)
	{
		if (log.isDebugEnabled()) log.debug(this + " getListAssignmetsForContext : CONTEXT : " + context);
		Assignment tempAssignment = null;
		Vector retVal = new Vector();
		List allAssignments = new Vector();

		if (context != null)
		{
			allAssignments = getAssignments(context);
			for (int x = 0; x < allAssignments.size(); x++)
			{
				tempAssignment = (Assignment) allAssignments.get(x);
				
				if ((context.equals(tempAssignment.getContext()))
						|| (context.equals(getGroupNameFromContext(tempAssignment.getContext()))))
				{
					if (!tempAssignment.isDeleted())
					{
						// not deleted, show it
						if (tempAssignment.isDraft())
						{
							// who can see the draft assignment
							if (isDraftAssignmentVisible(tempAssignment, context))
							{
								retVal.add(tempAssignment);
							}
						}
						else
						{
							retVal.add(tempAssignment);
						}
					}
				}
			}
		}

		return retVal;

	}
	
	/**
	 * who can see the draft assignment
	 * @param assignment
	 * @param context
	 * @return
	 */
	private boolean isDraftAssignmentVisible(Assignment assignment, String context) 
	{
		return SecurityService.isSuperUser() // super user can always see it
			|| assignment.getCreator().equals(UserDirectoryService.getCurrentUser().getId()) // the creator can see it
			|| (unlockCheck(SECURE_SHARE_DRAFTS, SiteService.siteReference(context)) && isCurrentUserInSameRoleAsCreator(assignment.getCreator(), context)); // same role user with share draft permission
	}
	
	/**
	 * is current user has same role as the specified one?
	 * @param assignment
	 * @param context
	 * @return
	 */
	private boolean isCurrentUserInSameRoleAsCreator(String creatorUserId, String context) 
	{	
		try {
			User currentUser = UserDirectoryService.getCurrentUser();
			
			AuthzGroup group = AuthzGroupService.getAuthzGroup(SiteService.siteReference(context));
			
			Member currentUserMember = group.getMember(currentUser.getId());
			Member creatorMember = group.getMember(creatorUserId);
			Role role = currentUserMember.getRole();
		
			return role != null && role.getId().equals(creatorMember.getRole().getId());
		
		} catch (GroupNotDefinedException gnde) {
			log.warn("No group defined for this site " + context);
		}
		
		return false;
	}
	
	/**
	 * Access a User's AssignmentSubmission to a particular Assignment.
	 * 
	 * @param assignmentReference
	 *        The reference of the assignment.
	 * @param person -
	 *        The User who's Submission you would like.
	 * @return AssignmentSubmission The user's submission for that Assignment.
	 * @throws IdUnusedException
	 *         if there is no object with this id.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public AssignmentSubmission getSubmission(String assignmentReference, User person)
	{
		AssignmentSubmission submission = null;

		String assignmentId = assignmentId(assignmentReference);
		
		if ((assignmentReference != null) && (person != null))
		{
			/*List<AssignmentSubmission> rvList = (getHibernateTemplate().findByNamedQueryAndNamedParam(GET_SUBMISSION_BY_ASSIGNMENT_USER, "assignmentId", assignmentId, "userId", person.getId()));
			if (rvList != null && rvList.size() == 1)
			{
				submission = rvList.get(0);
			}*/
		}

		if (submission != null)
		{
			try
			{
				Assignment a = getAssignment(assignmentId);
					
				String key = submissionReference(submission);
				unlock2(SECURE_ACCESS_ASSIGNMENT_SUBMISSION, SECURE_ACCESS_ASSIGNMENT, key);
			}
			catch (Exception e)
			{
				return null;
			}
		}

		return submission;
	}

	/**
	 * @inheritDoc
	 */
	public AssignmentSubmission getSubmission(List submissions, User person) 
	{
		AssignmentSubmission retVal = null;
		
		for (int z = 0; z < submissions.size(); z++)
		{
			AssignmentSubmission sub = (AssignmentSubmission) submissions.get(z);
			if (sub != null)
			{
				String aUserId = sub.getSubmitterId();
				if (log.isDebugEnabled())
					log.debug(this + " getSubmission(List, User) comparing aUser id : " + aUserId + " and chosen user id : "
							+ person.getId());
				if (aUserId.equals(person.getId()))
				{
					if (log.isDebugEnabled())
						log.debug(this + " getSubmission(List, User) found a match : return value is " + sub.getId());
					retVal = sub;
				}
			}
		}

		return retVal;
	}
	
	/**
	 * Get the submissions for an assignment.
	 * 
	 * @param assignment -
	 *        the Assignment who's submissions you would like.
	 * @return Iterator over all the submissions for an Assignment.
	 */
	public List getSubmissions(Assignment assignment)
	{
		List retVal = new Vector();

		if (assignment != null)
		{
			retVal = getSubmissions(String.valueOf(assignment.getId()));
		}
		
		return retVal;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getSubmittedSubmissionsCount(String assignmentId)
	{
		//TODO:zqian return m_submissionStorage.getSubmittedSubmissionsCount(assignmentId);
		return -1;
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public int getUngradedSubmissionsCount(String assignmentId)
	{
		return -1;
		//TODO: zqian return m_submissionStorage.getUngradedSubmissionsCount(assignmentId);
		
	}

	/**
	 * Access the AssignmentSubmission with the specified id.
	 * 
	 * @param submissionReference -
	 *        The reference of the AssignmentSubmission.
	 * @return The AssignmentSubmission corresponding to the id, or null if it does not exist.
	 * @throws IdUnusedException
	 *         if there is no object with this id.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public AssignmentSubmission getSubmission(String submissionReference) throws IdUnusedException, PermissionException
	{
		if (log.isDebugEnabled()) log.debug(this + " GET SUBMISSION : REF : " + submissionReference);
		
		// check permission
		unlock2(SECURE_ACCESS_ASSIGNMENT_SUBMISSION, SECURE_ACCESS_ASSIGNMENT, submissionReference);

		AssignmentSubmission submission = null;

		String submissionId = submissionId(submissionReference);

		submission = (AssignmentSubmission) getHibernateTemplate().get(AssignmentSubmission.class, submissionId);
		
		if (submission == null) throw new IdUnusedException(submissionId);

		// track event
		// EventTrackingService.post(EventTrackingService.newEvent(EVENT_ACCESS_ASSIGNMENT_SUBMISSION, submission.getReference(), false));

		return submission;

	}// getAssignmentSubmission
	
	/**
	 * {@inheritDoc}
	 */
	public AssignmentSubmissionVersion getSubmissionVersion(String submissionVersionReference) throws IdUnusedException, PermissionException
	{
		if (log.isDebugEnabled()) log.debug(this + " GET SUBMISSION : REF : " + submissionVersionReference);
		
		// check permission
		unlock2(SECURE_ACCESS_ASSIGNMENT_SUBMISSION, SECURE_ACCESS_ASSIGNMENT, submissionVersionReference);

		AssignmentSubmissionVersion submissionVersion = null;

		String submissionVersionId = submissionId(submissionVersionReference);

		submissionVersion = (AssignmentSubmissionVersion) getHibernateTemplate().get(AssignmentSubmissionVersion.class, submissionVersionId);
		
		if (submissionVersion == null) throw new IdUnusedException(submissionVersionId);

		// track event
		// EventTrackingService.post(EventTrackingService.newEvent(EVENT_ACCESS_ASSIGNMENT_SUBMISSION, submission.getReference(), false));

		return submissionVersion;

	}// getAssignmentSubmissionVersion

	/**
	 * Return the reference root for use in resource references and urls.
	 * 
	 * @return The reference root for use in resource references and urls.
	 */
	protected String getReferenceRoot()
	{
		return AssignmentConstants.REFERENCE_ROOT;
	}

	/**
	 * Update the live properties for an object when modified.
	 */
	protected void addLiveUpdateProperties(ResourcePropertiesEdit props)
	{
		props.addProperty(ResourceProperties.PROP_MODIFIED_BY, SessionManager.getCurrentSessionUserId());

		props.addProperty(ResourceProperties.PROP_MODIFIED_DATE, TimeService.newTime().toString());

	} // addLiveUpdateProperties

	/**
	 * Create the live properties for the object.
	 */
	protected void addLiveProperties(ResourcePropertiesEdit props)
	{
		String current = SessionManager.getCurrentSessionUserId();
		props.addProperty(ResourceProperties.PROP_CREATOR, current);
		props.addProperty(ResourceProperties.PROP_MODIFIED_BY, current);

		String now = TimeService.newTime().toString();
		props.addProperty(ResourceProperties.PROP_CREATION_DATE, now);
		props.addProperty(ResourceProperties.PROP_MODIFIED_DATE, now);

	} // addLiveProperties

	/**
	 * check permissions for addAssignment().
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel()
	 * @return true if the user is allowed to addAssignment(...), false if not.
	 */
	public boolean allowAddGroupAssignment(String context)
	{
		// base the check for SECURE_ADD on the site, any of the site's groups, and the channel
		// if the user can SECURE_ADD anywhere in that mix, they can add an assignment
		// this stack is not the normal azg set for channels, so use a special refernce to get this behavior
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + REF_TYPE_ASSIGNMENT_GROUPS + Entity.SEPARATOR + "a"
				+ Entity.SEPARATOR + context + Entity.SEPARATOR;

		if (log.isDebugEnabled())
		{
			log.debug(this + " allowAddGroupAssignment with resource string : " + resourceString);
			log.debug("                                    context string : " + context);
		}

		// check security on the channel (throws if not permitted)
		return unlockCheck(SECURE_ADD_ASSIGNMENT, resourceString);

	} // allowAddGroupAssignment

	/**
	 * @inheritDoc
	 */
	public boolean allowReceiveSubmissionNotification(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;

		if (log.isDebugEnabled())
		{
			log.debug(this + " allowReceiveSubmissionNotification with resource string : " + resourceString);
		}

		// checking allow at the site level
		if (unlockCheck(SECURE_ASSIGNMENT_RECEIVE_NOTIFICATIONS, resourceString)) return true;
		
		return false;
	}
	
	/**
	 * @inheritDoc
	 */
	public List allowReceiveSubmissionNotificationUsers(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;
		if (log.isDebugEnabled())
		{
			log.debug(this + " allowReceiveSubmissionNotificationUsers with resource string : " + resourceString);
			log.debug("                                   				 	context string : " + context);
		}
		return SecurityService.unlockUsers(SECURE_ASSIGNMENT_RECEIVE_NOTIFICATIONS, resourceString);

	} // allowAddAssignmentUsers
	
	/**
	 * @inheritDoc
	 */
	public boolean allowAddAssignment(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;
		// base the check for SECURE_ADD_ASSIGNMENT on the site and any of the site's groups
		// if the user can SECURE_ADD_ASSIGNMENT anywhere in that mix, they can add an assignment
		// this stack is not the normal azg set for site, so use a special refernce to get this behavior

		if (log.isDebugEnabled())
		{
			log.debug(this + " allowAddAssignment with resource string : " + resourceString);
		}

		// checking allow at the site level
		if (unlockCheck(SECURE_ADD_ASSIGNMENT, resourceString)) return true;

		// if not, see if the user has any groups to which adds are allowed
		return (!getGroupsAllowAddAssignment(context).isEmpty());
	}

	/**
	 * @inheritDoc
	 */
	public boolean allowAddSiteAssignment(String context)
	{
		// check for assignments that will be site-wide:
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context  + Entity.SEPARATOR;

		if (log.isDebugEnabled())
		{
			log.debug(this + " allowAddSiteAssignment with resource string : " + resourceString);
		}

		// check security on the channel (throws if not permitted)
		return unlockCheck(SECURE_ADD_ASSIGNMENT, resourceString);
	}

	/**
	 * @inheritDoc
	 */
	public boolean allowAllGroups(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;

		if (log.isDebugEnabled())
		{
			log.debug(this + " allowAllGroups with resource string : " + resourceString);
		}

		// checking all.groups
		if (unlockCheck(SECURE_ALL_GROUPS, resourceString)) return true;

		// if not
		return false;
	}
	
	/**
	 * @inheritDoc
	 */
	public Collection getGroupsAllowAddAssignment(String context)
	{
		return getGroupsAllowFunction(SECURE_ADD_ASSIGNMENT, context, null);
	}
	
	/**
	 * @inheritDoc
	 */
	public Collection getGroupsAllowGradeAssignment(String context, String assignmentReference)
	{
		Collection rv = new Vector();
		try
		{
			Assignment a = getAssignment(assignmentReference);
			if (allowGradeSubmission(a))
			{
				// only if the user is allowed to group at all
				Collection allAllowedGroups = getGroupsAllowFunction(SECURE_GRADE_ASSIGNMENT_SUBMISSION, context, null);
				if (a.getGroups() == null || a.getGroups().size() == 0)
				{
					// for site-scope assignment, return all groups
					rv = allAllowedGroups;
				}
				else
				{
					Collection aGroups = a.getGroups();
					// for grouped assignment, return only those also allowed for grading
					for (Iterator i = allAllowedGroups.iterator(); i.hasNext();)
					{
						Group g = (Group) i.next();
						if (aGroups.contains(g.getReference()))
						{
							rv.add(g);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			log.info(this + " getGroupsAllowGradeAssignment " + e.getMessage() + assignmentReference);
		}
			
		return rv;
	}

	/** 
	 * @inherit
	 */
	public boolean allowGetAssignment(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;

		if (log.isDebugEnabled())
		{
			log.debug(this + " allowGetAssignment with resource string : " + resourceString);
		}

		return unlockCheck(SECURE_ACCESS_ASSIGNMENT, resourceString);
	}

	/**
	 * @inheritDoc
	 */
	public Collection getGroupsAllowGetAssignment(String context)
	{
		return getGroupsAllowFunction(SECURE_ACCESS_ASSIGNMENT, context, null);
	}
	
	// for specified user
	private Collection getGroupsAllowGetAssignment(String context, String userId)
	{
		return getGroupsAllowFunction(SECURE_ACCESS_ASSIGNMENT, context, userId);
	}

	/**
	 * Check permissions for updateing an Assignment.
	 * 
	 * @param assignmentReference -
	 *        The Assignment's reference.
	 * @return True if the current User is allowed to update the Assignment, false if not.
	 */
	public boolean allowUpdateAssignment(String assignmentReference)
	{
		if (log.isDebugEnabled()) log.debug(this + " allowUpdateAssignment with resource string : " + assignmentReference);

		return unlockCheck(SECURE_UPDATE_ASSIGNMENT, assignmentReference);
	}

	/**
	 * Check permissions for removing an Assignment.
	 * 
	 * @return True if the current User is allowed to remove the Assignment, false if not.
	 */
	public boolean allowRemoveAssignment(String assignmentReference)
	{
		if (log.isDebugEnabled()) log.debug(this + " allowRemoveAssignment " + assignmentReference);

		// check security (throws if not permitted)
		return unlockCheck(SECURE_REMOVE_ASSIGNMENT, assignmentReference);
	}

	/**
	 * @inheritDoc
	 */
	public Collection getGroupsAllowRemoveAssignment(String context)
	{
		return getGroupsAllowFunction(SECURE_REMOVE_ASSIGNMENT, context, null);
	}

	/**
	 * Get the groups of this channel's contex-site that the end user has permission to "function" in.
	 * 
	 * @param function
	 *        The function to check
	 */
	protected Collection getGroupsAllowFunction(String function, String context, String userId)
	{	
		Collection rv = new Vector();
		try
		{
			// get the site groups
			Site site = SiteService.getSite(context);
			Collection groups = site.getGroups();

			if (SecurityService.isSuperUser())
			{
				// for super user, return all groups
				return groups;
			}
			else if (userId == null)
			{
				// for current session user
				userId = SessionManager.getCurrentSessionUserId();
			}
			
			// if the user has SECURE_ALL_GROUPS in the context (site), select all site groups
			if (AuthzGroupService.isAllowed(userId, SECURE_ALL_GROUPS, SiteService.siteReference(context)) && unlockCheck(function, SiteService.siteReference(context)))
			{
				return groups;
			}

			// otherwise, check the groups for function

			// get a list of the group refs, which are authzGroup ids
			Collection groupRefs = new Vector();
			for (Iterator i = groups.iterator(); i.hasNext();)
			{
				Group group = (Group) i.next();
				groupRefs.add(group.getReference());
			}

			// ask the authzGroup service to filter them down based on function
			groupRefs = AuthzGroupService.getAuthzGroupsIsAllowed(userId,
					function, groupRefs);

			// pick the Group objects from the site's groups to return, those that are in the groupRefs list
			for (Iterator i = groups.iterator(); i.hasNext();)
			{
				Group group = (Group) i.next();
				if (groupRefs.contains(group.getReference()))
				{
					rv.add(group);
				}
			}
		}
		catch (IdUnusedException e)
		{
		}

		return rv;
		
	}

	/**
	 * Check permissions for add AssignmentSubmission
	 * 
	 * @param context -
	 *        Describes the portlet context - generated with DefaultId.getChannel().
	 * @return True if the current User is allowed to add an AssignmentSubmission, false if not.
	 */
	public boolean allowAddSubmission(String context)
	{
		// check security (throws if not permitted)
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "s" + Entity.SEPARATOR + context + Entity.SEPARATOR;

		if (log.isDebugEnabled()) log.debug(this + " allowAddSubmission with resource string : " + resourceString);

		return unlockCheck(SECURE_ADD_ASSIGNMENT_SUBMISSION, resourceString);
	}
	
	/**
	 * Get the list of Users who can do certain function for this assignment
	 * @inheritDoc
	 */
	public List allowAssignmentFunctionUsers(String assignmentReference, String function)
	{
		List rv = new Vector();
		
		rv = SecurityService.unlockUsers(function, assignmentReference);
		
		// get the list of users who have SECURE_ALL_GROUPS
		List allGroupUsers = new Vector();
		try
		{
			String contextRef = SiteService.siteReference(getAssignment(assignmentReference).getContext());
			allGroupUsers = SecurityService.unlockUsers(SECURE_ALL_GROUPS, contextRef);
			// remove duplicates
			allGroupUsers.removeAll(rv);
		}
		catch (Exception e)
		{
			log.warn(this + "allowAssignmentFunctionUsers " + e.getMessage() + " assignmentReference=" + assignmentReference + " function=" + function);
		}
		
		// combine two lists together
		rv.addAll(allGroupUsers);
		
		return rv;
	}
	
	/**
	 * Get the List of Users who can addSubmission() for this assignment.
	 * 
	 * @param assignmentReference -
	 *        a reference to an assignment
	 * @return the List (User) of users who can addSubmission() for this assignment.
	 */
	public List allowAddSubmissionUsers(String assignmentReference)
	{
		return allowAssignmentFunctionUsers(assignmentReference, SECURE_ADD_ASSIGNMENT_SUBMISSION);

	} // allowAddSubmissionUsers
	
	/**
	 * Get the List of Users who can grade submission for this assignment.
	 * 
	 * @param assignmentReference -
	 *        a reference to an assignment
	 * @return the List (User) of users who can grade submission for this assignment.
	 */
	public List allowGradeAssignmentUsers(String assignmentReference)
	{
		return allowAssignmentFunctionUsers(assignmentReference, SECURE_GRADE_ASSIGNMENT_SUBMISSION);

	} // allowGradeAssignmentUsers
	
	/**
	 * @inheritDoc
	 * @param context
	 * @return
	 */
	public List allowAddAnySubmissionUsers(String context)
	{
		List rv = new Vector();
		
		try
		{
			AuthzGroup group = AuthzGroupService.getAuthzGroup(SiteService.siteReference(context));
			
			// get the roles which are allowed for submission but not for all_site control
			Set rolesAllowSubmission = group.getRolesIsAllowed(SECURE_ADD_ASSIGNMENT_SUBMISSION);
			Set rolesAllowAllSite = group.getRolesIsAllowed(SECURE_ALL_GROUPS);
			rolesAllowSubmission.removeAll(rolesAllowAllSite);
			
			for (Iterator iRoles = rolesAllowSubmission.iterator(); iRoles.hasNext(); )
			{
				rv.addAll(group.getUsersHasRole((String) iRoles.next()));
			}
		}
		catch (Exception e)
		{
			log.warn(this + " allowAddAnySubmissionUsers " + e.getMessage() + " context=" + context);
		}
		
		return rv;
		
	}

	/**
	 * Get the List of Users who can add assignment
	 * 
	 * @param assignmentReference -
	 *        a reference to an assignment
	 * @return the List (User) of users who can addSubmission() for this assignment.
	 */
	public List allowAddAssignmentUsers(String context)
	{
		String resourceString = getAccessPoint(true) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + context + Entity.SEPARATOR;
		if (log.isDebugEnabled())
		{
			log.debug(this + " allowAddAssignmentUsers with resource string : " + resourceString);
			log.debug("                                    	context string : " + context);
		}
		return SecurityService.unlockUsers(SECURE_ADD_ASSIGNMENT, resourceString);

	} // allowAddAssignmentUsers

	/**
	 * Check permissions for accessing a Submission.
	 * 
	 * @param submissionReference -
	 *        The Submission's reference.
	 * @return True if the current User is allowed to get the AssignmentSubmission, false if not.
	 */
	public boolean allowGetSubmission(String submissionReference)
	{
		if (log.isDebugEnabled()) log.debug(this + " allowGetSubmission with resource string : " + submissionReference);

		return unlockCheck2(SECURE_ACCESS_ASSIGNMENT_SUBMISSION, SECURE_ACCESS_ASSIGNMENT, submissionReference);
	}

	/**
	 * Check permissions for updating Submission.
	 * 
	 * @param submissionReference -
	 *        The Submission's reference.
	 * @return True if the current User is allowed to update the AssignmentSubmission, false if not.
	 */
	public boolean allowUpdateSubmission(String submissionReference)
	{
		if (log.isDebugEnabled()) log.debug(this + " allowUpdateSubmission with resource string : " + submissionReference);

		return unlockCheck2(SECURE_UPDATE_ASSIGNMENT_SUBMISSION, SECURE_UPDATE_ASSIGNMENT, submissionReference);
	}

	/**
	 * Check permissions for remove Submission
	 * 
	 * @param submissionReference -
	 *        The Submission's reference.
	 * @return True if the current User is allowed to remove the AssignmentSubmission, false if not.
	 */
	public boolean allowRemoveSubmission(String submissionReference)
	{
		if (log.isDebugEnabled()) log.debug(this + " allowRemoveSubmission with resource string : " + submissionReference);

		// check security (throws if not permitted)
		return unlockCheck(SECURE_REMOVE_ASSIGNMENT_SUBMISSION, submissionReference);
	}

	public boolean allowGradeSubmission(Assignment assignment)
	{
		return allowGradeSubmission(assignmentReference(assignment));	
	}
	
	public boolean allowGradeSubmission(String assignmentReference)
	{
		if (log.isDebugEnabled())
		{
			log.debug(this + " allowGradeSubmission with resource string : " + assignmentReference);
		}
		return unlockCheck(SECURE_GRADE_ASSIGNMENT_SUBMISSION, assignmentReference);
	}

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
	public byte[] getGradesSpreadsheet(String ref) throws IdUnusedException, PermissionException
	{
		/*String typeGradesString = new String(REF_TYPE_GRADES + Entity.SEPARATOR);
		String context = ref.substring(ref.indexOf(typeGradesString) + typeGradesString.length());

		// get site title for display purpose
		String siteTitle = "";
		try
		{
			Site s = SiteService.getSite(context);
			siteTitle = s.getTitle();
		}
		catch (Exception e)
		{
			// ignore exception
		}
		
		// does current user allowed to grade any assignment?
		boolean allowGradeAny = false;
		List assignmentsList = getListAssignmentsForContext(context);
		for (int iAssignment = 0; !allowGradeAny && iAssignment<assignmentsList.size(); iAssignment++)
		{
			Assignment assignment = (Assignment) assignmentsList.get(iAssignment);
			if (allowGradeSubmission(assignmentReference(assignment)))
			{
				allowGradeAny = true;
			}
		}
		
		if (!allowGradeAny)
		{
			// not permitted to download the spreadsheet
			return null;
		}
		else
		{
			short rowNum = 0;
			HSSFWorkbook wb = new HSSFWorkbook();
			HSSFSheet sheet = wb.createSheet(Validator.escapeZipEntry(siteTitle));
	
			// Create a row and put some cells in it. Rows are 0 based.
			HSSFRow row = sheet.createRow(rowNum++);
	
			row.createCell((short) 0).setCellValue(rb.getString("download.spreadsheet.title"));
	
			// empty line
			row = sheet.createRow(rowNum++);
			row.createCell((short) 0).setCellValue("");
	
			// site title
			row = sheet.createRow(rowNum++);
			row.createCell((short) 0).setCellValue(rb.getString("download.spreadsheet.site") + siteTitle);
	
			// download time
			row = sheet.createRow(rowNum++);
			row.createCell((short) 0).setCellValue(
					rb.getString("download.spreadsheet.date") + TimeService.newTime().toStringLocalFull());
	
			// empty line
			row = sheet.createRow(rowNum++);
			row.createCell((short) 0).setCellValue("");
	
			HSSFCellStyle style = wb.createCellStyle();
	
			// this is the header row number
			short headerRowNumber = rowNum;
			// set up the header cells
			row = sheet.createRow(rowNum++);
			short cellNum = 0;
			
			// user enterprise id column
			HSSFCell cell = row.createCell(cellNum++);
			cell.setCellStyle(style);
			cell.setCellValue(rb.getString("download.spreadsheet.column.name"));
	
			// user name column
			cell = row.createCell(cellNum++);
			cell.setCellStyle(style);
			cell.setCellValue(rb.getString("download.spreadsheet.column.userid"));
			
			// starting from this row, going to input user data
			Iterator assignments = new SortedIterator(assignmentsList.iterator(), new AssignmentComparator("duedate", "true"));
	
			// site members excluding those who can add assignments
			List members = new Vector();
			// hashtable which stores the Excel row number for particular user
			Hashtable user_row = new Hashtable();
			
			List allowAddAnySubmissionUsers = allowAddAnySubmissionUsers(context);
			for (Iterator iUserIds = new SortedIterator(allowAddAnySubmissionUsers.iterator(), new AssignmentComparator("sortname", "true")); iUserIds.hasNext();)
			{
				String userId = (String) iUserIds.next();
				try
				{
					User u = UserDirectoryService.getUser(userId);
					members.add(u);
					// create the column for user first
					row = sheet.createRow(rowNum);
					// update user_row Hashtable
					user_row.put(u.getId(), new Integer(rowNum));
					// increase row
					rowNum++;
					// put user displayid and sortname in the first two cells
					cellNum = 0;
					row.createCell(cellNum++).setCellValue(u.getSortName());
					row.createCell(cellNum).setCellValue(u.getDisplayId());
				}
				catch (Exception e)
				{
					log.warn(this + " getGradesSpreadSheet " + e.getMessage() + " userId = " + userId);
				}
			}
				
			int index = 0;
			// the grade data portion starts from the third column, since the first two are used for user's display id and sort name
			while (assignments.hasNext())
			{
				Assignment a = (Assignment) assignments.next();
				
				int assignmentType = a.getTypeOfGrade();
				
				// for column header, check allow grade permission based on each assignment
				if(!a.isDraft() && allowGradeSubmission(assigmentReference(a)))
				{
					// put in assignment title as the column header
					rowNum = headerRowNumber;
					row = sheet.getRow(rowNum++);
					cellNum = (short) (index + 2);
					cell = row.createCell(cellNum); // since the first two column is taken by student id and name
					cell.setCellStyle(style);
					cell.setCellValue(a.getTitle());
					
					for (int loopNum = 0; loopNum < members.size(); loopNum++)
					{
						// prepopulate the column with the "no submission" string
						row = sheet.getRow(rowNum++);
						cell = row.createCell(cellNum);
						cell.setCellType(1);
						cell.setCellValue(rb.getString("listsub.nosub"));
					}

					// begin to populate the column for this assignment, iterating through student list
					for (Iterator sIterator=getSubmissions(a).iterator(); sIterator.hasNext();)
					{
						AssignmentSubmission submission = (AssignmentSubmission) sIterator.next();
						
						String userId = (String) submission.getSubmitterId();
						
						if (user_row.containsKey(userId))
						{	
							// find right row
							row = sheet.getRow(((Integer)user_row.get(userId)).intValue());
						
							if (submission.getGraded() && submission.getGradeReleased() && submission.getGrade() != null)
							{
								// graded and released
								if (assignmentType == 3)
								{
									try
									{
										// numeric cell type?
										String grade = submission.getGradeDisplay();
										Float.parseFloat(grade);
			
										// remove the String-based cell first
										cell = row.getCell(cellNum);
										row.removeCell(cell);
										// add number based cell
										cell=row.createCell(cellNum);
										cell.setCellType(0);
										cell.setCellValue(Float.parseFloat(grade));
			
										style = wb.createCellStyle();
										style.setDataFormat(wb.createDataFormat().getFormat("#,##0.0"));
										cell.setCellStyle(style);
									}
									catch (Exception e)
									{
										// if the grade is not numeric, let's make it as String type
										row.removeCell(cell);
										cell=row.createCell(cellNum);
										cell.setCellType(1);
										cell.setCellValue(submission.getGrade());
									}
								}
								else
								{
									// String cell type
									cell = row.getCell(cellNum);
									cell.setCellValue(submission.getGrade());
								}
							}
							else
							{
								// no grade available yet
								cell = row.getCell(cellNum);
								cell.setCellValue("");
							}
						} // if
					}
				}
				
				index++;
				
			}
			
			// output
			Blob b = new Blob();
			try
			{
				wb.write(b.outputStream());
			}
			catch (IOException e)
			{
				log.debug(this + " getGradesSpreadsheet Can not output the grade spread sheet for reference= " + ref);
			}
			
			return b.getBytes();
		}*/
		return null;

	} // getGradesSpreadsheet

	/**
	 * Access the submissions zip for the assignment reference.
	 * 
	 * @param ref
	 *        The assignment reference.
	 * @return The submissions zip bytes.
	 * @throws IdUnusedException
	 *         if there is no object with this id.
	 * @throws PermissionException
	 *         if the current user is not allowed to access this.
	 */
	public void getSubmissionsZip(OutputStream outputStream, String ref) throws IdUnusedException, PermissionException
 	{
		if (log.isDebugEnabled()) log.debug(this + ": getSubmissionsZip reference=" + ref);

		byte[] rv = null;

		try
		{
			Assignment a = getAssignment(assignmentReferenceFromSubmissionsZipReference(ref));
			String contextString = a.getContext();
			String groupReference = groupReferenceFromSubmissionsZipReference(ref);
			List allSubmissions = getSubmissions(a);
			List submissions = new Vector();
			
			// group or site
			String authzGroupId = "";
			if (groupReference == null)
			{
				// view all groups
				if (allowAllGroups(contextString))
				{
					// if have site level control
					submissions = allSubmissions;
				}
				else
				{
					// iterate through all allowed-grade-group
					Collection gCollection = getGroupsAllowGradeAssignment(contextString, assignmentReference(a));
					// prevent multiple entries
					HashSet userIdSet = new HashSet();
					for (Iterator iGCollection = gCollection.iterator(); iGCollection.hasNext();)
					{
						Group g = (Group) iGCollection.next();
						String gReference = g.getReference();
						try
						{
							AuthzGroup group = AuthzGroupService.getAuthzGroup(gReference);
							Set grants = group.getUsers();
							for (int i = 0; i<allSubmissions.size();i++)
							{
								// see if the submitters is in the group
								AssignmentSubmission s = (AssignmentSubmission) allSubmissions.get(i);
								String submitterId = s.getSubmitterId();
								if (!userIdSet.contains(submitterId) && grants.contains(submitterId))
								{
									submissions.add(s);
									userIdSet.add(submitterId);
								}
							}
						}
						catch (Exception ee)
						{
							log.info(this + " getSubmissionsZip " + ee.getMessage() + " group reference=" + gReference);
						}
					}
					
				}
			}
			else
			{
				// just one group
				try
				{
					AuthzGroup group = AuthzGroupService.getAuthzGroup(groupReference);
					Set grants = group.getUsers();
					for (int i = 0; i<allSubmissions.size();i++)
					{
						// see if the submitters is in the group
						AssignmentSubmission s = (AssignmentSubmission) allSubmissions.get(i);
						if (grants.contains(s.getSubmitterId()))
						{
							submissions.add(s);
						}
					}
				}
				catch (Exception ee)
				{
					log.info(this +  " getSubmissionsZip " + ee.getMessage() + " group reference=" + groupReference);
				}
				
			}

			StringBuilder exceptionMessage = new StringBuilder();

			if (allowGradeSubmission(a))
			{
				zipSubmissions(assignmentReference(a), a.getTitle(), String.valueOf(a.getTypeOfGrade()), a.getTypeOfSubmission(), submissions.iterator(), outputStream, exceptionMessage);

				if (exceptionMessage.length() > 0)
				{
					// log any error messages
					if (log.isDebugEnabled())
						log.debug(this + " getSubmissionsZip ref=" + ref + exceptionMessage.toString());
				}
			}
		}
		catch (IdUnusedException e)
		{
			if (log.isDebugEnabled())
				log.debug(this + "getSubmissionsZip -IdUnusedException Unable to get assignment " + ref);
			throw new IdUnusedException(ref);
		}
		catch (PermissionException e)
		{
			log.debug(this + " getSubmissionsZip -PermissionException Not permitted to get assignment " + ref);
			throw new PermissionException(SessionManager.getCurrentSessionUserId(), SECURE_ACCESS_ASSIGNMENT, ref);
		}

	} // getSubmissionsZip

	protected void zipSubmissions(String assignmentReference, String assignmentTitle, String gradeTypeString, int typeOfSubmission, Iterator submissions, OutputStream outputStream, StringBuilder exceptionMessage) 
	{
		/*try
		{
			ZipOutputStream out = new ZipOutputStream(outputStream);

			// create the folder structor - named after the assignment's title
			String root = Validator.escapeZipEntry(assignmentTitle) + Entity.SEPARATOR;

			String submittedText = "";
			if (!submissions.hasNext())
			{
				exceptionMessage.append("There is no submission yet. ");
			}
			
			// the buffer used to store grade information
			StringBuilder gradesBuffer = new StringBuilder(assignmentTitle + "," + gradeTypeString + "\n\n");
			gradesBuffer.append(rb.getString("grades.id") + "," + rb.getString("grades.eid") + "," + rb.getString("grades.lastname") + "," + rb.getString("grades.firstname") + "," + rb.getString("grades.grade") + "\n");

			// allow add assignment members
			List allowAddSubmissionUsers = allowAddSubmissionUsers(assignmentReference);
			
			// Create the ZIP file
			String submittersName = "";
			int count = 1;
			while (submissions.hasNext())
			{
				AssignmentSubmission s = (AssignmentSubmission) submissions.next();
				
				//TODO:zqian
				if (true)
				{
					// get the submission user id and see if the user is still in site
					String userId = (String) s.getSubmitterId();
					try
					{
						User u = UserDirectoryService.getUser(userId);
						if (allowAddSubmissionUsers.contains(u))
						{
							count = 1;
							submittersName = root;
							
							String submittersString = s.getSubmitterId();
							try
							{
								User submitter = UserDirectoryService.getUser(s.getSubmitterId());
								String fullName = submitter.getSortName();
								// in case the user doesn't have first name or last name
								if (fullName.indexOf(",") == -1)
								{
									fullName=fullName.concat(",");
								}
								submittersString = submittersString.concat(fullName);
								// add the eid to the end of it to guarantee folder name uniqness
								submittersString = submittersString + "(" + submitter.getEid() + ")";
								gradesBuffer.append(submitter.getDisplayId() + "," + submitter.getEid() + "," + fullName + ","  + s.getGradeDisplay() + "\n");
							} catch (Exception e)
							{
								
							}
							
							if (StringUtil.trimToNull(submittersString) != null)
							{
								submittersName = submittersName.concat(StringUtil.trimToNull(submittersString));
								submittedText = s.getSubmittedText();
		
								boolean added = false;
								while (!added)
								{
									try
									{
										submittersName = submittersName.concat("/");
										// create the folder structure - named after the submitter's name
										if (typeOfSubmission != Assignment.ATTACHMENT_ONLY_ASSIGNMENT_SUBMISSION)
										{
											// create the text file only when a text submission is allowed
											ZipEntry textEntry = new ZipEntry(submittersName + submittersString + "_submissionText" + ZIP_SUBMITTED_TEXT_FILE_TYPE);
											out.putNextEntry(textEntry);
											byte[] text = submittedText.getBytes();
											out.write(text);
											textEntry.setSize(text.length);
											out.closeEntry();
										}
										
										// record submission timestamp
										if (s.getSubmitted() && s.getSubmittedTime() != null)
										{
											ZipEntry textEntry = new ZipEntry(submittersName + "timestamp.txt");
											out.putNextEntry(textEntry);
											byte[] b = (s.getSubmittedTime().toString()).getBytes();
											out.write(b);
											textEntry.setSize(b.length);
											out.closeEntry();
										}
										// create a feedbackText file into zip
										ZipEntry fTextEntry = new ZipEntry(submittersName + "feedbackText.html");
										out.putNextEntry(fTextEntry);
										byte[] fText = s.getFeedbackText().getBytes();
										out.write(fText);
										fTextEntry.setSize(fText.length);
										out.closeEntry();
										
										// the comments.txt file to show instructor's comments
										ZipEntry textEntry = new ZipEntry(submittersName + "comments" + ZIP_COMMENT_FILE_TYPE);
										out.putNextEntry(textEntry);
										byte[] b = FormattedText.encodeUnicode(s.getFeedbackComment()).getBytes();
										out.write(b);
										textEntry.setSize(b.length);
										out.closeEntry();
										
										// create an attachment folder for the feedback attachments
										String feedbackSubAttachmentFolder = submittersName + rb.getString("download.feedback.attachment") + "/";
										ZipEntry feedbackSubAttachmentFolderEntry = new ZipEntry(feedbackSubAttachmentFolder);
										out.putNextEntry(feedbackSubAttachmentFolderEntry);
										out.closeEntry();
		
										// create a attachment folder for the submission attachments
										String sSubAttachmentFolder = submittersName + rb.getString("download.submission.attachment") + "/";
										ZipEntry sSubAttachmentFolderEntry = new ZipEntry(sSubAttachmentFolder);
										out.putNextEntry(sSubAttachmentFolderEntry);
										out.closeEntry();
										// add all submission attachment into the submission attachment folder
										zipAttachments(out, submittersName, sSubAttachmentFolder, s.getSubmittedAttachments());
										// add all feedback attachment folder
										zipAttachments(out, submittersName, feedbackSubAttachmentFolder, s.getFeedbackAttachments());
		
										added = true;
									}
									catch (IOException e)
									{
										exceptionMessage.append("Can not establish the IO to create zip file for user "
												+ submittersName);
										log.debug(this + " zipSubmissions --IOException unable to create the zip file for user"
												+ submittersName);
										submittersName = submittersName.substring(0, submittersName.length() - 1) + "_" + count++;
									}
								}	//while
							} // if
						}
					}
					catch (Exception e)
					{
						log.warn(this + " zipSubmissions " + e.getMessage() + " userId = " + userId);
					}
				} // if the user is still in site

			} // while -- there is submission

			// create a grades.csv file into zip
			ZipEntry gradesCSVEntry = new ZipEntry(root + "grades.csv");
			out.putNextEntry(gradesCSVEntry);
			byte[] grades = gradesBuffer.toString().getBytes();
			out.write(grades);
			gradesCSVEntry.setSize(grades.length);
			out.closeEntry();
			
			// Complete the ZIP file
			out.finish();
			out.flush();
			out.close();
		}
		catch (IOException e)
		{
			exceptionMessage.append("Can not establish the IO to create zip file. ");
			log.debug(this + " zipSubmissions IOException unable to create the zip file for assignment "
					+ assignmentTitle);
		}*/
	}



	private void zipAttachments(ZipOutputStream out, String submittersName, String sSubAttachmentFolder, List attachments) {
		int attachedUrlCount = 0;
		for (int j = 0; j < attachments.size(); j++)
		{
			Reference r = (Reference) attachments.get(j);
			try
			{
				ContentResource resource = m_contentHostingService.getResource(r.getId());

				String contentType = resource.getContentType();
				
				ResourceProperties props = r.getProperties();
				String displayName = props.getPropertyFormatted(props.getNamePropDisplayName());

				// for URL content type, encode a redirect to the body URL
				if (contentType.equalsIgnoreCase(ResourceProperties.TYPE_URL))
				{
					displayName = "attached_URL_" + attachedUrlCount;
					attachedUrlCount++;
				}

				// buffered stream input
				InputStream content = resource.streamContent();
				byte data[] = new byte[1024 * 10];
				BufferedInputStream bContent = new BufferedInputStream(content, data.length);
				
				ZipEntry attachmentEntry = new ZipEntry(sSubAttachmentFolder + displayName);
				out.putNextEntry(attachmentEntry);
				int bCount = -1;
				while ((bCount = bContent.read(data, 0, data.length)) != -1) 
				{
					out.write(data, 0, bCount);
				}
				out.closeEntry();
				content.close();
			}
			catch (PermissionException e)
			{
				log.debug(this + " zipAttachments--PermissionException submittersName="
						+ submittersName + " attachment reference=" + r);
			}
			catch (IdUnusedException e)
			{
				log.debug(this + " zipAttachments--IdUnusedException submittersName="
						+ submittersName + " attachment reference=" + r);
			}
			catch (TypeException e)
			{
				log.debug(this + " zipAttachments--TypeException: submittersName="
						+ submittersName + " attachment reference=" + r);
			}
			catch (IOException e)
			{
				log.debug(this + " zipAttachments--IOException: Problem in creating the attachment file: submittersName="
								+ submittersName + " attachment reference=" + r);
			}
			catch (ServerOverloadException e)
			{
				log.debug(this + " zipAttachments--ServerOverloadException: submittersName="
						+ submittersName + " attachment reference=" + r);
			}
		} // for
	}

	/**
	 * Get the string to form an assignment grade spreadsheet
	 * 
	 * @param context
	 *        The assignment context String
	 * @param assignmentId
	 *        The id for the assignment object; when null, indicates all assignment in that context
	 */
	public String gradesSpreadsheetReference(String context, String assignmentId)
	{
		// based on all assignment in that context
		String s = AssignmentConstants.REFERENCE_ROOT + Entity.SEPARATOR + REF_TYPE_GRADES + Entity.SEPARATOR + context;
		if (assignmentId != null)
		{
			// based on the specified assignment only
			s = s.concat(Entity.SEPARATOR + assignmentId);
		}

		return s;

	} // gradesSpreadsheetReference

	/**
	 * Get the string to form an assignment submissions zip file
	 * 
	 * @param context
	 *        The assignment context String
	 * @param assignmentReference
	 *        The reference for the assignment object;
	 */
	public String submissionsZipReference(String context, String assignmentReference)
	{
		// based on the specified assignment
		return AssignmentConstants.REFERENCE_ROOT + Entity.SEPARATOR + REF_TYPE_SUBMISSIONS + Entity.SEPARATOR + context + Entity.SEPARATOR
				+ assignmentReference;

	} // submissionsZipReference

	/**
	 * Decode the submissionsZipReference string to get the assignment reference String
	 * 
	 * @param sReference
	 *        The submissionZipReference String
	 * @return The assignment reference String
	 */
	private String assignmentReferenceFromSubmissionsZipReference(String sReference)
	{
		// remove the String part relating to submissions zip reference
		if (sReference.indexOf(Entity.SEPARATOR +"site") == -1)
		{
			return sReference.substring(sReference.lastIndexOf(Entity.SEPARATOR + "assignment"));
		}
		else
		{
			return sReference.substring(sReference.lastIndexOf(Entity.SEPARATOR + "assignment"), sReference.indexOf(Entity.SEPARATOR +"site"));
		}

	} // assignmentReferenceFromSubmissionsZipReference
	
	/**
	 * Decode the submissionsZipReference string to get the group reference String
	 * 
	 * @param sReference
	 *        The submissionZipReference String
	 * @return The group reference String
	 */
	private String groupReferenceFromSubmissionsZipReference(String sReference)
	{
		// remove the String part relating to submissions zip reference
		if (sReference.indexOf(Entity.SEPARATOR +"site") != -1)
		{
			return sReference.substring(sReference.lastIndexOf(Entity.SEPARATOR + "site"));
		}
		else
		{
			return null;
		}

	} // assignmentReferenceFromSubmissionsZipReference

	/**********************************************************************************************************************************************************************************************************************************************************
	 * ResourceService implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * {@inheritDoc}
	 */
	public String getLabel()
	{
		return "assignment";
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean willArchiveMerge()
	{
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public HttpAccess getHttpAccess()
	{
		return new HttpAccess()
		{
			public void handleAccess(HttpServletRequest req, HttpServletResponse res, Reference ref,
					Collection copyrightAcceptedRefs) throws EntityPermissionException, EntityNotDefinedException,
					EntityAccessOverloadException, EntityCopyrightException
			{
				if (SessionManager.getCurrentSessionUserId() == null)
				{
					// fail the request, user not logged in yet.
				}
				else
				{
					try
					{
						if (REF_TYPE_SUBMISSIONS.equals(ref.getSubType()))
						{
							res.setContentType("application/zip");
							res.setHeader("Content-Disposition", "attachment; filename = bulk_download.zip");
							 
							OutputStream out = null;
							try
							{
							    out = res.getOutputStream();
							    
							    // get the submissions zip blob
							    getSubmissionsZip(out, ref.getReference());
							    
							    out.flush();
							    out.close();
							}
							catch (Throwable ignore)
							{
							    log.error(this + " getHttpAccess handleAccess " + ignore.getMessage() + " ref=" + ref.getReference());
							}
							finally
							{
							    if (out != null)
							    {
							        try
							        {
							            out.close();
							        }
							        catch (Throwable ignore)
							        {
							        }
							    }
							}
						}
	
						else if (REF_TYPE_GRADES.equals(ref.getSubType()))
						{
							// get the grades spreadsheet blob
							byte[] spreadsheet = getGradesSpreadsheet(ref.getReference());
	
							if (spreadsheet != null)
							{
								res.setContentType("application/vnd.ms-excel");
								res.setHeader("Content-Disposition", "attachment; filename = export_grades_file.xls");
	
								OutputStream out = null;
								try
								{
									out = res.getOutputStream();
									out.write(spreadsheet);
									out.flush();
									out.close();
								}
								catch (Throwable ignore)
								{
								}
								finally
								{
									if (out != null)
									{
										try
										{
											out.close();
										}
										catch (Throwable ignore)
										{
										}
									}
								}
							}
						}
						else
						{
							throw new IdUnusedException(ref.getReference());
						}
					}
					catch (Throwable t)
					{
						throw new EntityNotDefinedException(ref.getReference());
					}
				}
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean parseEntityReference(String reference, Reference ref)
	{
		if (reference.startsWith(AssignmentConstants.REFERENCE_ROOT))
		{
			String id = null;
			String subType = null;
			String container = null;
			String context = null;

			String[] parts = StringUtil.split(reference, Entity.SEPARATOR);
			// we will get null, assignment, [a|c|s|grades|submissions], context, [auid], id

			if (parts.length > 2)
			{
				subType = parts[2];

				if (parts.length > 3)
				{
					// context is the container
					context = parts[3];

					// submissions have the assignment unique id as a container
					if ("s".equals(subType))
					{
						if (parts.length > 5)
						{
							container = parts[4];
							id = parts[5];
						}
					}

					// others don't
					else
					{
						if (parts.length > 4)
						{
							id = parts[4];
						}
					}
				}
			}

			ref.set(APPLICATION_ID, subType, id, container, context);

			return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public Entity getEntity(Reference ref)
	{
		Entity rv = null;

		try
		{
			// is it an Assignment object
			if (REF_TYPE_ASSIGNMENT.equals(ref.getSubType()))
			{
				//rv = getAssignment(ref.getReference());
			}
			// is it an AssignmentSubmission object
			else if (REF_TYPE_SUBMISSION.equals(ref.getSubType()))
			{
				//rv = getSubmission(ref.getReference());
			}
			else
				log.warn(this + "getEntity(): unknown message ref subtype: " + ref.getSubType() + " in ref: " + ref.getReference());
		}
		catch (Exception e)
		{
			log.warn(this + "getEntity(): " + e + " ref=" + ref.getReference());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection getEntityAuthzGroups(Reference ref, String userId)
	{
		Collection rv = new Vector();

		// for AssignmentService assignments:
		// if access set to SITE, use the assignment and site authzGroups.
		// if access set to GROUPED, use the assignment, and the groups, but not the site authzGroups.
		// if the user has SECURE_ALL_GROUPS in the context, ignore GROUPED access and treat as if SITE

		try
		{
			// for assignment
			if (REF_TYPE_ASSIGNMENT.equals(ref.getSubType()))
			{
				// assignment
				rv.add(ref.getReference());
				
				boolean grouped = false;
				Collection groups = null;

				// check SECURE_ALL_GROUPS - if not, check if the assignment has groups or not
				// TODO: the last param needs to be a ContextService.getRef(ref.getContext())... or a ref.getContextAuthzGroup() -ggolden
				if ((userId == null) || ((!SecurityService.isSuperUser(userId)) && (!AuthzGroupService.isAllowed(userId, SECURE_ALL_GROUPS, SiteService.siteReference(ref.getContext())))))
				{
					// get the channel to get the message to get group information
					if (ref.getId() != null)
					{
						Assignment a = findAssignment(ref.getReference());
						if (a != null)
						{
							groups = a.getGroups();
							grouped = (groups != null || groups.size() > 0)?true:false;
						}
					}
				}

				if (grouped)
				{
					// groups
					rv.addAll(groups);
				}

				// not grouped
				else
				{
					// site
					ref.addSiteContextAuthzGroup(rv);
				}
			}
			else
			{
				rv.add(ref.getReference());
				
				// for content and submission, use site security setting
				ref.addSiteContextAuthzGroup(rv);
			}
		}
		catch (Throwable e)
		{
			log.warn(this + " getEntityAuthzGroups(): " + e.getMessage() + " ref=" + ref.getReference());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getEntityUrl(Reference ref)
	{
		String rv = null;

		try
		{
			// is it an Assignment object
			if (REF_TYPE_ASSIGNMENT.equals(ref.getSubType()))
			{
				Assignment a = getAssignment(ref.getReference());
				//rv = a.getUrl();
			}
			// is it an AssignmentSubmission object
			else if (REF_TYPE_SUBMISSION.equals(ref.getSubType()))
			{
				AssignmentSubmission s = getSubmission(ref.getReference());
				//rv = s.getUrl();
			}
			else
				log.warn(this + " getEntityUrl(): unknown message ref subtype: " + ref.getSubType() + " in ref: " + ref.getReference());
		}
		catch (PermissionException e)
		{
			log.warn(this + "getEntityUrl(): " + e + " ref=" + ref.getReference());
		}
		catch (IdUnusedException e)
		{
			log.warn(this + "getEntityUrl(): " + e + " ref=" + ref.getReference());
		}
		catch (NullPointerException e)
		{
			log.warn(this + "getEntityUrl(): " + e + " ref=" + ref.getReference());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments)
	{
		// prepare the buffer for the results log
		/*StringBuilder results = new StringBuilder();

		// String assignRef = assignmentReference(siteId, SiteService.MAIN_CONTAINER);
		results.append("archiving " + getLabel() + " context " + Entity.SEPARATOR + siteId + Entity.SEPARATOR
				+ SiteService.MAIN_CONTAINER + ".\n");

		// start with an element with our very own (service) name
		Element element = doc.createElement(AssignmentService.class.getName());
		((Element) stack.peek()).appendChild(element);
		stack.push(element);

		Iterator assignmentsIterator = getAssignmentsForContext(siteId);

		while (assignmentsIterator.hasNext())
		{
			Assignment assignment = (Assignment) assignmentsIterator.next();

			// archive this assignment
			Element el = assignment.toXml(doc, stack);
			element.appendChild(el);

			// then archive the related content
			AssignmentContent content = (AssignmentContent) assignment.getContent();
			if (content != null)
			{
				Element contentEl = content.toXml(doc, stack);

				// assignment node has already kept the context info
				contentEl.removeAttribute("context");

				// collect attachments
				List atts = content.getAttachments();

				for (int i = 0; i < atts.size(); i++)
				{
					Reference ref = (Reference) atts.get(i);
					// if it's in the attachment area, and not already in the list
					if ((ref.getReference().startsWith("/content/attachment/")) && (!attachments.contains(ref)))
					{
						attachments.add(ref);
					}

					// in order to make assignment.xml has the consistent format with the other xml files
					// move the attachments to be the children of the content, instead of the attributes
					String attributeString = "attachment" + i;
					String attRelUrl = contentEl.getAttribute(attributeString);
					contentEl.removeAttribute(attributeString);
					Element attNode = doc.createElement("attachment");
					attNode.setAttribute("relative-url", attRelUrl);
					contentEl.appendChild(attNode);

				} // for

				// make the content a childnode of the assignment node
				el.appendChild(contentEl);

				Iterator submissionsIterator = getSubmissions(assignment).iterator();
				while (submissionsIterator.hasNext())
				{
					AssignmentSubmission submission = (AssignmentSubmission) submissionsIterator.next();

					// archive this assignment
					Element submissionEl = submission.toXml(doc, stack);
					el.appendChild(submissionEl);

				}
			} // if //TODO: zqian
		} // while
		stack.pop();

		return results.toString();*/
		return null;

	} // archive

	/**
	 * Replace the WT user id with the new qualified id
	 * 
	 * @param el
	 *        The XML element holding the perproties
	 * @param useIdTrans
	 *        The HashMap to track old WT id to new CTools id
	 */
	protected void WTUserIdTrans(Element el, Map userIdTrans)
	{
		NodeList children4 = el.getChildNodes();
		int length4 = children4.getLength();
		for (int i4 = 0; i4 < length4; i4++)
		{
			Node child4 = children4.item(i4);
			if (child4.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element4 = (Element) child4;
				if (element4.getTagName().equals("property"))
				{
					String creatorId = "";
					String modifierId = "";
					if (element4.hasAttribute("CHEF:creator"))
					{
						if ("BASE64".equalsIgnoreCase(element4.getAttribute("enc")))
						{
							creatorId = Xml.decodeAttribute(element4, "CHEF:creator");
						}
						else
						{
							creatorId = element4.getAttribute("CHEF:creator");
						}
						String newCreatorId = (String) userIdTrans.get(creatorId);
						if (newCreatorId != null)
						{
							Xml.encodeAttribute(element4, "CHEF:creator", newCreatorId);
							element4.setAttribute("enc", "BASE64");
						}
					}
					else if (element4.hasAttribute("CHEF:modifiedby"))
					{
						if ("BASE64".equalsIgnoreCase(element4.getAttribute("enc")))
						{
							modifierId = Xml.decodeAttribute(element4, "CHEF:modifiedby");
						}
						else
						{
							modifierId = element4.getAttribute("CHEF:modifiedby");
						}
						String newModifierId = (String) userIdTrans.get(modifierId);
						if (newModifierId != null)
						{
							Xml.encodeAttribute(element4, "CHEF:creator", newModifierId);
							element4.setAttribute("enc", "BASE64");
						}
					}
				}
			}
		}

	} // WTUserIdTrans

	/**
	 * {@inheritDoc}
	 */
	public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans,
			Set userListAllowImport)
	{
		// prepare the buffer for the results log
		StringBuilder results = new StringBuilder();

		int count = 0;

		try
		{
			// pass the DOM to get new assignment ids, and adjust attachments
			NodeList children2 = root.getChildNodes();

			int length2 = children2.getLength();
			for (int i2 = 0; i2 < length2; i2++)
			{
				Node child2 = children2.item(i2);
				if (child2.getNodeType() == Node.ELEMENT_NODE)
				{
					Element element2 = (Element) child2;

					if (element2.getTagName().equals("assignment"))
					{
						// a flag showing if continuing merging the assignment
						boolean goAhead = true;

						// element2 now - assignment node
						// adjust the id of this assignment
						// String newId = IdManager.createUuid();
						element2.setAttribute("id", IdManager.createUuid());
						element2.setAttribute("context", siteId);

						// cloneNode(false) - no children cloned
						Element el2clone = (Element) element2.cloneNode(false);

						// traverse this assignment node first to check if the person who last modified, has the right role.
						// if no right role, mark the flag goAhead to be false.
						NodeList children3 = element2.getChildNodes();
						int length3 = children3.getLength();
						for (int i3 = 0; i3 < length3; i3++)
						{
							Node child3 = children3.item(i3);
							if (child3.getNodeType() == Node.ELEMENT_NODE)
							{
								Element element3 = (Element) child3;

								// add the properties childnode to the clone of assignment node
								if (element3.getTagName().equals("properties"))
								{
									NodeList children6 = element3.getChildNodes();
									int length6 = children6.getLength();
									for (int i6 = 0; i6 < length6; i6++)
									{
										Node child6 = children6.item(i6);
										if (child6.getNodeType() == Node.ELEMENT_NODE)
										{
											Element element6 = (Element) child6;

											if (element6.getTagName().equals("property"))
											{
												if (element6.getAttribute("name").equalsIgnoreCase("CHEF:modifiedby"))
												{
													if ("BASE64".equalsIgnoreCase(element6.getAttribute("enc")))
													{
														String creatorId = Xml.decodeAttribute(element6, "value");
														if (!userListAllowImport.contains(creatorId)) goAhead = false;
													}
													else
													{
														String creatorId = element6.getAttribute("value");
														if (!userListAllowImport.contains(creatorId)) goAhead = false;
													}
												}
											}
										}
									}
								}
							}
						} // for

						// then, go ahead to merge the content and assignment
						if (goAhead)
						{
							for (int i3 = 0; i3 < length3; i3++)
							{
								Node child3 = children3.item(i3);
								if (child3.getNodeType() == Node.ELEMENT_NODE)
								{
									Element element3 = (Element) child3;

									// add the properties childnode to the clone of assignment node
									if (element3.getTagName().equals("properties"))
									{
										// add the properties childnode to the clone of assignment node
										el2clone.appendChild(element3.cloneNode(true));
									}
									else if (element3.getTagName().equals("content"))
									{
										// element3 now- content node
										// adjust the id of this content
										String newContentId = IdManager.createUuid();
										element3.setAttribute("id", newContentId);
										element3.setAttribute("context", siteId);

										// clone the content node without the children of <properties>
										Element el3clone = (Element) element3.cloneNode(false);

										// update the assignmentcontent id in assignment node
										String assignContentId = "/assignment/c/" + siteId + "/" + newContentId;
										el2clone.setAttribute("assignmentcontent", assignContentId);

										// for content node, process the attachment or properties kids
										NodeList children5 = element3.getChildNodes();
										int length5 = children5.getLength();
										int attCount = 0;
										for (int i5 = 0; i5 < length5; i5++)
										{
											Node child5 = children5.item(i5);
											if (child5.getNodeType() == Node.ELEMENT_NODE)
											{
												Element element5 = (Element) child5;

												// for the node of "properties"
												if (element5.getTagName().equals("properties"))
												{
													// for the file from WT, preform userId translation when needed
													if (!userIdTrans.isEmpty())
													{
														WTUserIdTrans(element3, userIdTrans);
													}
												} // for the node of properties
												el3clone.appendChild(element5.cloneNode(true));

												// for "attachment" children
												if (element5.getTagName().equals("attachment"))
												{
													// map the attachment area folder name
													// filter out the invalid characters in the attachment id
													// map the attachment area folder name
													String oldUrl = element5.getAttribute("relative-url");
													if (oldUrl.startsWith("/content/attachment/"))
													{
														String newUrl = (String) attachmentNames.get(oldUrl);
														if (newUrl != null)
														{
															if (newUrl.startsWith("/attachment/"))
																newUrl = "/content".concat(newUrl);

															element5.setAttribute("relative-url", Validator
																	.escapeQuestionMark(newUrl));
														}
													}

													// map any references to this site to the new site id
													else if (oldUrl.startsWith("/content/group/" + fromSiteId + "/"))
													{
														String newUrl = "/content/group/" + siteId
																+ oldUrl.substring(15 + fromSiteId.length());
														element5.setAttribute("relative-url", Validator.escapeQuestionMark(newUrl));
													}
													// put the attachment back to the attribute field of content
													// to satisfy the input need of mergeAssignmentContent
													String attachmentString = "attachment" + attCount;
													el3clone.setAttribute(attachmentString, element5.getAttribute("relative-url"));
													attCount++; 

												} // if
											} // if
										} // for
									}
								}
							} // for

 							// when importing, refer to property to determine draft status
							if ("false".equalsIgnoreCase(m_serverConfigurationService.getString("import.importAsDraft")))
							{
								String draftAttribute = el2clone.getAttribute("draft");
								if (draftAttribute.equalsIgnoreCase("true") || draftAttribute.equalsIgnoreCase("false"))
									el2clone.setAttribute("draft", draftAttribute);
								else
									el2clone.setAttribute("draft", "true");
							}
							else
							{
								el2clone.setAttribute("draft", "true");
							}

							// merge in this assignment
							Assignment mAssignment = mergeAssignment(el2clone);
							try 
							{
								getHibernateTemplate().saveOrUpdate(mAssignment);
							}
							catch (DataAccessException e)
							{
								e.printStackTrace();
								log.warn(this + ".saveAssignment() Hibernate could not save assignment=" + mAssignment.getId());
							}

							count++;
						} // if goAhead
					} // if
				} // if
			} // for
		}
		catch (Exception any)
		{
			log.warn(this + " merge(): exception: " + any.getMessage() + " siteId=" + siteId + " from site id=" + fromSiteId);
		}

		results.append("merging assignment " + siteId + " (" + count + ") assignments.\n");
		return results.toString();

	} // merge

	/**
	 * {@inheritDoc}
	 */
	public String[] myToolIds()
	{
		String[] toolIds = { "sakai.assignment", "sakai.assignment.grades" };
		return toolIds;
	}

	/**
	 * {@inheritDoc}
	 */
	public void transferCopyEntities(String fromContext, String toContext, List resourceIds)
	{
		// import Assignment objects
	//TODO zqian
		/*	Iterator oAssignments = getAssignmentsForContext(fromContext);
		while (oAssignments.hasNext())
		{
			Assignment oAssignment = (Assignment) oAssignments.next();
			String oAssignmentId = oAssignment.getId();

			boolean toBeImported = true;
			if (resourceIds != null && resourceIds.size() > 0)
			{
				// if there is a list for import assignments, only import those assignments and relative submissions
				toBeImported = false;
				for (int m = 0; m < resourceIds.size() && !toBeImported; m++)
				{
					if (((String) resourceIds.get(m)).equals(oAssignmentId))
					{
						toBeImported = true;
					}
				}
			}

			if (toBeImported)
			{
				Assignment nAssignment = null;
				AssignmentContentEdit nContent = null;

				if (!m_assignmentStorage.check(oAssignmentId))
				{

				}
				else
				{
					try
					{
						// add new Assignment content
						String oContentReference = oAssignment.getContentReference();
						String oContentId = contentId(oContentReference);
						if (!m_contentStorage.check(oContentId))
							throw new IdUnusedException(oContentId);
						else
						{
							AssignmentContent oContent = getAssignmentContent(oContentReference);
							nContent = addAssignmentContent(toContext);
							// attributes

							nContent.setAllowAttachments(oContent.getAllowAttachments());
							nContent.setContext(toContext);
							nContent.setGroupProject(oContent.getGroupProject());
							nContent.setHonorPledge(oContent.getHonorPledge());
							nContent.setIndividuallyGraded(oContent.individuallyGraded());
							nContent.setInstructions(oContent.getInstructions());
							nContent.setMaxGradePoint(oContent.getMaxGradePoint());
							nContent.setReleaseGrades(oContent.releaseGrades());
							nContent.setTimeLastModified(oContent.getTimeLastModified());
							nContent.setTitle(oContent.getTitle());
							nContent.setTypeOfGrade(oContent.getTypeOfGrade());
							nContent.setTypeOfSubmission(oContent.getTypeOfSubmission());
							// properties
							ResourcePropertiesEdit p = nContent.getPropertiesEdit();
							p.clear();
							p.addAll(oContent.getProperties());
							// update live properties
							addLiveProperties(p);
							// attachment
							List oAttachments = oContent.getAttachments();
							List nAttachments = m_entityManager.newReferenceList();
							for (int n = 0; n < oAttachments.size(); n++)
							{
								Reference oAttachmentRef = (Reference) oAttachments.get(n);
								String oAttachmentId = ((Reference) oAttachments.get(n)).getId();
								if (oAttachmentId.indexOf(fromContext) != -1)
								{
									// replace old site id with new site id in attachments
									String nAttachmentId = oAttachmentId.replaceAll(fromContext, toContext);
									try
									{
										ContentResource attachment = m_contentHostingService.getResource(nAttachmentId);
										nAttachments.add(m_entityManager.newReference(attachment.getReference()));
									}
									catch (IdUnusedException e)
									{
										try
										{
											ContentResource oAttachment = m_contentHostingService.getResource(oAttachmentId);
											try
											{
												if (m_contentHostingService.isAttachmentResource(nAttachmentId))
												{
													// add the new resource into attachment collection area
													ContentResource attachment = m_contentHostingService.addAttachmentResource(
															Validator.escapeResourceName(oAttachment.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME)), 
															ToolManager.getCurrentPlacement().getContext(), 
															ToolManager.getTool("sakai.assignment.grades").getTitle(), 
															oAttachment.getContentType(), 
															oAttachment.getContent(), 
															oAttachment.getProperties());
													// add to attachment list
													nAttachments.add(m_entityManager.newReference(attachment.getReference()));
												}
												else
												{
													// add the new resource into resource area
													ContentResource attachment = m_contentHostingService.addResource(
															Validator.escapeResourceName(oAttachment.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME)),
															ToolManager.getCurrentPlacement().getContext(), 
															1, 
															oAttachment.getContentType(), 
															oAttachment.getContent(), 
															oAttachment.getProperties(), 
															NotificationService.NOTI_NONE);
													// add to attachment list
													nAttachments.add(m_entityManager.newReference(attachment.getReference()));
												}
											}
											catch (Exception eeAny)
											{
												// if the new resource cannot be added
												log.warn(this + " transferCopyEntities: cannot add new attachment with id=" + nAttachmentId + " " + eeAny.getMessage());
											}
										}
										catch (Exception eAny)
										{
											// if cannot find the original attachment, do nothing.
											log.warn(this + " transferCopyEntities: cannot find the original attachment with id=" + oAttachmentId + " " + eAny.getMessage());
										}
									}
									catch (Exception any)
									{
										log.warn(this + " transferCopyEntities" + any.getMessage() + " oAttachmentId=" + oAttachmentId + " nAttachmentId=" + nAttachmentId);
									}
								}
								else
								{
									nAttachments.add(oAttachmentRef);
								}
							}
							nContent.replaceAttachments(nAttachments);
							// complete the edit
							m_contentStorage.commit(nContent);
							((BaseAssignmentContentEdit) nContent).closeEdit();
						}
					}
					catch (Exception e)
					{
						if (log.isWarnEnabled()) log.warn(this + " transferCopyEntities " + e.toString()  + " oAssignmentId=" + oAssignmentId);
					}

					if (nContent != null)
					{
						try
						{
							// add new assignment
							nAssignment = addAssignment(toContext);
							// attribute
							nAssignment.setCloseTime(oAssignment.getCloseTime());
							nAssignment.setContentReference(nContent.getReference());
							nAssignment.setContext(toContext);
							
 							// when importing, refer to property to determine draft status
							if ("false".equalsIgnoreCase(m_serverConfigurationService.getString("import.importAsDraft")))
							{
								nAssignment.setDraft(oAssignment.isDraft());
							}
							else
							{
								nAssignment.setDraft(true);
							}
							
							nAssignment.setDropDeadTime(oAssignment.getDropDeadTime());
							nAssignment.setDueTime(oAssignment.getDueTime());
							nAssignment.setOpenTime(oAssignment.getOpenTime());
							nAssignment.setSection(oAssignment.getSection());
							nAssignment.setTitle(oAssignment.getTitle());
							// properties
							ResourcePropertiesEdit p = nAssignment.getPropertiesEdit();
							p.clear();
							p.addAll(oAssignment.getProperties());
							
							// one more touch on the gradebook-integration link
							if (StringUtil.trimToNull(p.getProperty(PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT)) != null)
							{
								// assignments are imported as drafts;
								// mark the integration with "add" for now, later when user posts the assignment, the corresponding assignment will be created in gradebook.
								p.removeProperty(PROP_ASSIGNMENT_ASSOCIATE_GRADEBOOK_ASSIGNMENT);
								p.addProperty(NEW_ASSIGNMENT_ADD_TO_GRADEBOOK, GRADEBOOK_INTEGRATION_ADD);
							}
							
							// update live properties
							addLiveProperties(p);
							// complete the edit
							m_assignmentStorage.commit(nAssignment);
							((BaseAssignment) nAssignment).closeEdit();
							
							try {
								if (m_taggingManager.isTaggable()) {
									for (TaggingProvider provider : m_taggingManager
											.getProviders()) {
										provider
												.transferCopyTags(
														m_assignmentActivityProducer
																.getActivity(oAssignment),
														m_assignmentActivityProducer
																.getActivity(nAssignment));
									}
								}
							} catch (PermissionException pe) {
								log.error(this + " transferCopyEntities " + pe.toString()  + " oAssignmentId=" + oAssignment.getId() + " nAssignmentId=" + nAssignment.getId());
							}
						}
						catch (Exception ee)
						{
							log.error(this + " transferCopyEntities " + ee.toString() + " oAssignmentId=" + oAssignment.getId() + " nAssignmentId=" + nAssignment.getId());
						}
					}
				} // if-else
			} // if
		} // for*/
	} // importResources

	/**
	 * {@inheritDoc}
	 */
	public String getEntityDescription(Reference ref)
	{
		String rv = "Assignment: " + ref.getReference();
		
		try
		{
			
			// is it an Assignment object
			if (REF_TYPE_ASSIGNMENT.equals(ref.getSubType()))
			{
				Assignment a = getAssignment(ref.getReference());
				rv = "Assignment: " + a.getId() + " (" + a.getContext() + ")";
			}
			// is it an AssignmentSubmission object
			else if (REF_TYPE_SUBMISSION.equals(ref.getSubType()))
			{
				AssignmentSubmission s = getSubmission(ref.getReference());
				rv = "AssignmentSubmission: " + s.getId() + " (" + s.getAssignment().getContext() + ")";
			}
			else
				log.warn(this + " getEntityDescription(): unknown message ref subtype: " + ref.getSubType() + " in ref: " + ref.getReference());
		}
		catch (PermissionException e)
		{
			log.warn(this + " getEntityDescription(): " + e.getMessage() + " ref=" + ref.getReference());
		}
		catch (IdUnusedException e)
		{
			log.warn(this + " getEntityDescription(): " + e.getMessage() + " ref=" + ref.getReference());
		}
		catch (NullPointerException e)
		{
			log.warn(this + " getEntityDescription(): " + e.getMessage() + " ref=" + ref.getReference());
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public ResourceProperties getEntityResourceProperties(Reference ref)
	{
		ResourceProperties rv = null;

		try
		{
			// is it an Assignment object
			if (REF_TYPE_ASSIGNMENT.equals(ref.getSubType()))
			{
				Assignment a = getAssignment(ref.getReference());
			//	rv = a.getProperties();
			}
			// is it an AssignmentSubmission object
			else if (REF_TYPE_SUBMISSION.equals(ref.getSubType()))
			{
				AssignmentSubmission s = getSubmission(ref.getReference());
				//rv = s.getProperties();
			}
			else
				log.warn(this + " getEntityResourceProperties: unknown message ref subtype: " + ref.getSubType() + " in ref: " + ref.getReference());
		}
		catch (PermissionException e)
		{
			log.warn(this + " getEntityResourceProperties(): " + e.getMessage() + " ref=" + ref.getReference());
		}
		catch (IdUnusedException e)
		{
			log.warn(this + " getEntityResourceProperties(): " + e.getMessage() + " ref=" + ref.getReference());
		}
		catch (NullPointerException e)
		{
			log.warn(this + " getEntityResourceProperties(): " + e.getMessage() + " ref=" + ref.getReference());
		}

		return rv;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean canSubmit(String context, Assignment a)
	{
		/*TODO:zqian
		// return false if not allowed to submit at all
		if (!allowAddSubmission(context)) return false;
		
		String userId = SessionManager.getCurrentSessionUserId();
		try
		{
			// get user
			User u = UserDirectoryService.getUser(userId);
			
			Date currentTime = new Date();
			
			// return false if the assignment is draft or is not open yet
			Date openTime = a.getOpenTime();
			if (a.isDraft() || (openTime != null && openTime.after(currentTime)))
			{
				return false;
			}
			
			// return false if the current time has passed the assignment close time
			Date closeTime = a.getCloseTime();
			
			// get user's submission
			AssignmentSubmission submission = null;
			
			submission = getSubmission(assignmentReference(a), u);
			if (submission != null)
			{
				closeTime = submission.getResubmitCloseTime();
			}
			
			if (submission == null || (submission != null && s == null))
			{
				// if there is no submission yet
				if (closeTime != null && currentTime.after(closeTime))
				{
					return false;
				}
				else
				{
					return true;
				}
			}
			else
			{
				if (!submission.getSubmitted() && !(closeTime != null && currentTime.after(closeTime)))
				{
					// return true for drafted submissions
					return true;
				}
				else
				{
					// returned 
					if (submission.getResubmissionNum()!=0 && currentTime.before(closeTime))
					{
						// return true for returned submission but allow for resubmit and before the close time
						return true;
					}
					else
					{
						// return false otherwise
						return false;
					}
				}
			}
		}
		catch (UserNotDefinedException e)
		{
			// cannot find user
			log.warn(this + " canSubmit(String, Assignment) " + e.getMessage() + " assignment ref=" + a.getReference());
			return false;
		}*/
		return false;
	}
	
	/**
	 * Utility function which returns the string representation of the long value of the time object.
	 * 
	 * @param t -
	 *        the Time object.
	 * @return A String representation of the long value of the time object.
	 */
	protected String getTimeString(Time t)
	{
		String retVal = "";
		if (t != null) retVal = t.toString();
		return retVal;
	}

	/**
	 * Utility function which returns a string from a boolean value.
	 * 
	 * @param b -
	 *        the boolean value.
	 * @return - "True" if the input value is true, "false" otherwise.
	 */
	protected String getBoolString(boolean b)
	{
		if (b)
			return "true";
		else
			return "false";
	}

	/**
	 * Utility function which returns a boolean value from a string.
	 * 
	 * @param s -
	 *        The input string.
	 * @return the boolean true if the input string is "true", false otherwise.
	 */
	protected boolean getBool(String s)
	{
		boolean retVal = false;
		if (s != null)
		{
			if (s.equalsIgnoreCase("true")) retVal = true;
		}
		return retVal;
	}

	/**
	 * Utility function which converts a string into a chef time object.
	 * 
	 * @param timeString -
	 *        String version of a time in long format, representing the standard ms since the epoch, Jan 1, 1970 00:00:00.
	 * @return A chef Time object.
	 */
	protected Time getTimeObject(String timeString)
	{
		Time aTime = null;
		timeString = StringUtil.trimToNull(timeString);
		if (timeString != null)
		{
			try
			{
				aTime = TimeService.newTimeGmt(timeString);
			}
			catch (Exception e)
			{
				try
				{
					long longTime = Long.parseLong(timeString);
					aTime = TimeService.newTime(longTime);
				}
				catch (Exception ee)
				{
					log.warn(this + " getTimeObject Base Exception creating time object from xml file : " + ee.getMessage() + " timeString=" + timeString);
				}
			}
		}
		return aTime;
	}

	protected String getGroupNameFromContext(String context)
	{
		String retVal = "";

		if (context != null)
		{
			int index = context.indexOf("group-");
			if (index != -1)
			{
				String[] parts = StringUtil.splitFirst(context, "-");
				if (parts.length > 1)
				{
					retVal = parts[1];
				}
			}
			else
			{
				retVal = context;
			}
		}

		return retVal;
	}
	
	public void transferCopyEntities(String fromContext, String toContext, List ids, boolean cleanup)
	{	
		try
		{
			if(cleanup == true)
			{
				SecurityService.pushAdvisor(new SecurityAdvisor() 
				{
					public SecurityAdvice isAllowed(String userId, String function, String reference)       
					{    
						return SecurityAdvice.ALLOWED;       
					} 
				});

				String toSiteId = toContext;
				Iterator assignmentsIter = getAssignmentsForContext(toSiteId);
				while (assignmentsIter.hasNext())
				{
					try 
					{
						Assignment assignment = (Assignment) assignmentsIter.next();
						String assignmentId = String.valueOf(assignment.getId());
						Assignment aEdit = (Assignment) getHibernateTemplate().get(Assignment.class, assignmentId);
						try
						{
							removeAssignment(aEdit);
						}
						catch (Exception eeee)
						{
							log.debug("removeAssignment error:" + eeee);
						}
					}
					catch(Exception ee)
					{
						log.debug("removeAssignment process error:" + ee);
					}
				}
				   
			}
			transferCopyEntities(fromContext, toContext, ids);
		}
		catch (Exception e)
		{
			log.info("transferCopyEntities: End removing Assignmentt data" + e);
		}
		finally
		{
			SecurityService.popAdvisor();
		}
	}

	/**
	 * This is to mimic the FormattedText.decodeFormattedTextAttribute but use SAX serialization instead
	 * @return
	 */
	protected String FormattedTextDecodeFormattedTextAttribute(Attributes attributes, String baseAttributeName)
	{
		String ret;

		// first check if an HTML-encoded attribute exists, for example "foo-html", and use it if available
		ret = StringUtil.trimToNull(XmlDecodeAttribute(attributes, baseAttributeName + "-html"));
		if (ret != null) return ret;

		// next try the older kind of formatted text like "foo-formatted", and convert it if found
		ret = StringUtil.trimToNull(XmlDecodeAttribute(attributes, baseAttributeName + "-formatted"));
		ret = FormattedText.convertOldFormattedText(ret);
		if (ret != null) return ret;

		// next try just a plaintext attribute and convert the plaintext to formatted text if found
		// convert from old plaintext instructions to new formatted text instruction
		ret = XmlDecodeAttribute(attributes, baseAttributeName);
		ret = FormattedText.convertPlaintextToFormattedText(ret);
		return ret;
	}
	
	/**
	 * this is to mimic the Xml.decodeAttribute
	 * @param el
	 * @param tag
	 * @return
	 */
	protected String XmlDecodeAttribute(Attributes attributes, String tag)
	{
		String charset = StringUtil.trimToNull(attributes.getValue("charset"));
		if (charset == null) charset = "UTF-8";

		String body = StringUtil.trimToNull(attributes.getValue(tag));
		if (body != null)
		{
			try
			{
				byte[] decoded = CommonsCodecBase64.decodeBase64(body.getBytes("UTF-8"));
				body = new String(decoded, charset);
			}
			catch (Exception e)
			{
				log.warn(this + " XmlDecodeAttribute: " + e.getMessage() + " tag=" + tag);
			}
		}

		if (body == null) body = "";

		return body;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getAssignmentStatus(Assignment a)
	{
		//TODO:zqian
		return "";
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getSubmissionStatus(AssignmentSubmissionVersion sVersion)
	{
		//TODO:zqian
		return "";
	}

} // AssignmentServiceImpl

