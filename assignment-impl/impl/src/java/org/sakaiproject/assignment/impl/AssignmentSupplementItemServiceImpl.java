package org.sakaiproject.assignment.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.api.model.AssignmentModelAnswerItem;
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


}
