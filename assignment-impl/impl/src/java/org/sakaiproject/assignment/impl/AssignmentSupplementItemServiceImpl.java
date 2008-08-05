package org.sakaiproject.assignment.impl;

import java.util.List;
import java.util.Date;
import java.util.Set;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.api.AssignmentConstants;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.model.AssignmentModelAnswerItem;
import org.sakaiproject.assignment.api.model.AssignmentNoteItem;
import org.sakaiproject.assignment.api.model.AssignmentAllPurposeItem;
import org.sakaiproject.assignment.api.model.AssignmentAllPurposeItemAccess;
import org.sakaiproject.assignment.api.model.AssignmentSupplementItemAttachment;
import org.sakaiproject.assignment.api.model.AssignmentSupplementItemService;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class AssignmentSupplementItemServiceImpl extends HibernateDaoSupport implements AssignmentSupplementItemService {
	

	private final static Log Log = LogFactory.getLog(AssignmentSupplementItemServiceImpl.class);
	
	/**
	 * Init
	 */
   public void init()
   {
      Log.info("init()");
   }
   
   /**
    * Destroy
    */
   public void destroy()
   {
      Log.info("destroy()");
   }
   
   /** Dependency: UserDirectoryService */
	protected UserDirectoryService m_userDirectoryService = null;

	/**
	 * Dependency: UserDirectoryService.
	 * 
	 * @param service
	 *        The UserDirectoryService.
	 */
	public void setUserDirectoryService(UserDirectoryService service)
	{
		m_userDirectoryService = service;
	}
	
	   /** Dependency: AssignmentService */
	protected AssignmentService m_assignmentService = null;

	/**
	 * Dependency: AssignmentService.
	 * 
	 * @param service
	 *        The AssignmentService.
	 */
	public void setAssignmentService(AssignmentService service)
	{
		m_assignmentService = service;
	}
	
	/** Dependency: AuthzGroupService */
	protected AuthzGroupService m_authzGroupService = null;

	/**
	 * Dependency: AuthzGroupService.
	 * 
	 * @param service
	 *        The AuthzGroupService.
	 */
	public void setAuthzGroupService(AuthzGroupService authzService)
	{
		m_authzGroupService = authzService;
	}
	
	/** Dependency: SiteService */
	protected SiteService m_siteService = null;

	/**
	 * Dependency: SiteService.
	 * 
	 * @param service
	 *        The SiteService.
	 */
	public void setSiteService(SiteService siteService)
	{
		m_siteService = siteService;
	}
	
	/********************** attachment   ************************/
	/**
	 * {@inheritDoc}
	 */
	public AssignmentSupplementItemAttachment newAttachment()
	{
		return new AssignmentSupplementItemAttachment();
	}
	
	public boolean saveAttachment(AssignmentSupplementItemAttachment attachment)
	{
		try 
		{
			getHibernateTemplate().saveOrUpdate(attachment);
			return true;
		}
		catch (DataAccessException e)
		{
			e.printStackTrace();
			Log.warn(this + ".saveModelAnswerQuestion() Hibernate could not save attachment " + attachment.getId());
			return false;
		}
	}
	
   /*********************** model answer ************************/
	/**
	 * {@inheritDoc}}
	 */
	public AssignmentModelAnswerItem newModelAnswer()
	{
		return new AssignmentModelAnswerItem();
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public boolean saveModelAnswer(AssignmentModelAnswerItem mItem)
	{
		try 
		{
			getHibernateTemplate().saveOrUpdate(mItem);
			return true;
		}
		catch (DataAccessException e)
		{
			e.printStackTrace();
			Log.warn(this + ".saveModelAnswerQuestion() Hibernate could not save model answer for assignment " + mItem.getAssignmentId());
			return false;
		}
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public boolean removeModelAnswer(AssignmentModelAnswerItem mItem)
	{

		try 
		{
			getHibernateTemplate().delete(mItem);
			return true;
		}
		catch (DataAccessException e)
		{
			e.printStackTrace();
			Log.warn(this + ".removeModelAnswer() Hibernate could not delete ModelAnswer for assignment " + mItem.getAssignmentId());
			return false;
		}
		
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public AssignmentModelAnswerItem getModelAnswer(String assignmentId)
	{
		List<AssignmentModelAnswerItem> rvList = (getHibernateTemplate().findByNamedQueryAndNamedParam("findModelAnswerByAssignmentId", "id", assignmentId));
		if (rvList != null && rvList.size() == 1)
		{
			return rvList.get(0);
		}
		return null;
	}
	
	/*********************** private note *****************/
	/**
	 * {@inheritDoc}}
	 */
	public AssignmentNoteItem newNoteItem()
	{
		return new AssignmentNoteItem();
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public boolean saveNoteItem(AssignmentNoteItem nItem)
	{
		try 
		{
			getHibernateTemplate().saveOrUpdate(nItem);
			return true;
		}
		catch (DataAccessException e)
		{
			e.printStackTrace();
			Log.warn(this + ".saveNoteItem() Hibernate could not save private note for assignment " + nItem.getAssignmentId());
			return false;
		}
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public boolean removeNoteItem(AssignmentNoteItem mItem)
	{

		try 
		{
			getHibernateTemplate().delete(mItem);
			return true;
		}
		catch (DataAccessException e)
		{
			e.printStackTrace();
			Log.warn(this + ".removeNoteItem() Hibernate could not delete NoteItem for assignment " + mItem.getAssignmentId());
			return false;
		}
		
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public AssignmentNoteItem getNoteItem(String assignmentId)
	{
		List<AssignmentNoteItem> rvList = (getHibernateTemplate().findByNamedQueryAndNamedParam("findNoteItemByAssignmentId", "id", assignmentId));
		if (rvList != null && rvList.size() == 1)
		{
			return rvList.get(0);
		}
		return null;
	}
	
	
	/*********************** all purpose item *****************/
	/**
	 * {@inheritDoc}}
	 */
	public AssignmentAllPurposeItem newAllPurposeItem()
	{
		return new AssignmentAllPurposeItem();
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public boolean saveAllPurposeItem(AssignmentAllPurposeItem nItem)
	{
		try 
		{
			getHibernateTemplate().saveOrUpdate(nItem);
			return true;
		}
		catch (DataAccessException e)
		{
			e.printStackTrace();
			Log.warn(this + ".saveAllPurposeItem() Hibernate could not save private AllPurpose for assignment " + nItem.getAssignmentId());
			return false;
		}
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public boolean removeAllPurposeItem(AssignmentAllPurposeItem mItem)
	{

		try 
		{
			getHibernateTemplate().delete(mItem);
			return true;
		}
		catch (DataAccessException e)
		{
			e.printStackTrace();
			Log.warn(this + ".removeAllPurposeItem() Hibernate could not delete AllPurposeItem for assignment " + mItem.getAssignmentId());
			return false;
		}
		
	}
	
	/**
	 * {@inheritDoc}}
	 */
	public AssignmentAllPurposeItem getAllPurposeItem(String assignmentId)
	{
		List<AssignmentAllPurposeItem> rvList = (getHibernateTemplate().findByNamedQueryAndNamedParam("findAllPurposeItemByAssignmentId", "id", assignmentId));
		if (rvList != null && rvList.size() == 1)
		{
			return rvList.get(0);
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canViewModelAnswer(Assignment a, AssignmentSubmission s)
	{
		if (a != null)
		{
			AssignmentModelAnswerItem m = getModelAnswer(a.getId());
			if (m != null)
			{
				int show = m.getShowTo();
				if (show == AssignmentConstants.MODEL_ANSWER_SHOW_TO_STUDENT_BEFORE_STARTS)
				{
					return true;
				}
				else if (show == AssignmentConstants.MODEL_ANSWER_SHOW_TO_STUDENT_AFTER_SUBMIT && s != null && s.getSubmitted())
				{
					return true;
				}
				else if (show == AssignmentConstants.MODEL_ANSWER_SHOW_TO_STUDENT_AFTER_GRADE_RETURN && s!= null && s.getGradeReleased())
				{
					return true;
				}
				else if (show == AssignmentConstants.MODEL_ANSWER_SHOW_TO_STUDENT_AFTER_ACCEPT_UTIL && (a.getCloseTime().before(TimeService.newTime())))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean canReadNoteItem(Assignment a)
	{
		if (a != null)
		{
			AssignmentNoteItem note = getNoteItem(a.getId());
			if (note != null)
			{
				User u = m_userDirectoryService.getCurrentUser();
				String noteCreatorId = note.getCreatorId();
				if (noteCreatorId.equals(u.getId()))
				{
					return true;
				}
				else if (m_assignmentService.allowAddSubmission(a.getContext()))
				{
					// check whether the instructor type can view the note
					int share = note.getShareWith();
					if (share == AssignmentConstants.NOTE_READ_BY_OTHER || share == AssignmentConstants.NOTE_READ_AND_WRITE_BY_OTHER)
					{
						return true;
					}	
				}
			}
		}
		
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean canEditNoteItem(Assignment a)
	{
		if (a != null)
		{
			AssignmentNoteItem note = getNoteItem(a.getId());
			if (note != null)
			{
				if (note.getShareWith() == AssignmentConstants.NOTE_READ_AND_WRITE_BY_OTHER)
				{
					return true;
				}	
			}
		}
		
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean canViewAllPurposeItem(Assignment a)
	{
		boolean rv = false;
		
		if (a != null)
		{
			AssignmentAllPurposeItem aItem = getAllPurposeItem(a.getId());
			if (aItem != null)
			{
				if (!aItem.getHide())
				{
					Time now = TimeService.newTime();
					Date releaseDate = aItem.getReleaseDate();
					Date retractDate = aItem.getRetractDate();
					
					if (releaseDate == null && retractDate == null)
					{
						// no time limitation on showing the item
						rv = true;
					}
					else if (releaseDate != null && retractDate == null)
					{
						// has relase date but not retract date
						rv = now.getTime() > releaseDate.getTime();
					}
					else if (releaseDate == null && retractDate != null)
					{
						// has retract date but not release date
						rv = now.getTime() < retractDate.getTime();
					}
					else
					{
						// has both release and retract dates
						rv = now.getTime() > releaseDate.getTime() && now.getTime() < retractDate.getTime();
					}
				}
				else
				{
					rv = false;
				}
			}
			
			if (rv)
			{
				// need to check role/user permission only if the above time test returns true
				Set<AssignmentAllPurposeItemAccess> access = aItem.getAccessSet();
				User u = m_userDirectoryService.getCurrentUser();
				if ( u != null)
				{
					if (access.contains(u.getId()))
						rv = true;
					else 
					{
						try
						{
							String role = m_authzGroupService.getUserRole(u.getId(), m_siteService.siteReference(a.getContext()));
							if (access.contains(role))
								rv = true;
						}
						catch (Exception e)
						{
							Log.warn(this + ".callViewAllPurposeItem() Hibernate cannot access user role for user id= " + u.getId());
							return rv;
						}
						
					}
						
				}
			}
		}
		
		return rv;
	}
}
