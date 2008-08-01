package org.sakaiproject.assignment.impl;

import java.util.List;
import java.util.Date;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.api.AssignmentConstants;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.model.AssignmentModelAnswerItem;
import org.sakaiproject.assignment.api.model.AssignmentNoteItem;
import org.sakaiproject.assignment.api.model.AssignmentAllPurposeItem;
import org.sakaiproject.assignment.api.model.AssignmentSupplementItemService;
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
}
