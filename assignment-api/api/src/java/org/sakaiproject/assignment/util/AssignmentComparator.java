package org.sakaiproject.assignment.util;

import java.util.Comparator;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

public class AssignmentComparator implements Comparator 
{
	
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssignmentComparator.class);
	
	/**
	 * the criteria
	 */
	String m_criteria = null;

	/**
	 * the criteria
	 */
	String m_asc = null;

	/**
	 * constructor
	 * @param criteria
	 *        The sort criteria string
	 * @param asc
	 *        The sort order string. TRUE_STRING if ascending; "false" otherwise.
	 */
	public AssignmentComparator(String criteria, String asc)
	{
		m_criteria = criteria;
		m_asc = asc;
	} // constructor

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

		/** *********** fo sorting assignments ****************** */
		if (m_criteria.equals("duedate"))
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
		else if (m_criteria.equals("sortname"))
		{
			// sorted by the user's display name
			String s1 = null;
			String userId1 = (String) o1;
			if (userId1 != null)
			{
				try
				{
					User u1 = UserDirectoryService.getUser(userId1);
					s1 = u1!=null?u1.getSortName():null;
				}
				catch (Exception e)
				{
					if (M_log.isDebugEnabled()) M_log.debug(this + " AssignmentComparator.compare " + e.getMessage() + " id=" + userId1);
				}
			}
				
			String s2 = null;
			String userId2 = (String) o2;
			if (userId2 != null)
			{
				try
				{
					User u2 = UserDirectoryService.getUser(userId2);
					s2 = u2!=null?u2.getSortName():null;
				}
				catch (Exception e)
				{
					if (M_log.isDebugEnabled()) M_log.debug(this + " AssignmentComparator.compare " + e.getMessage() + " id=" + userId2);
				}
			}

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
				result = s1.compareTo(s2);
			}
		}
		
		// sort ascending or descending
		if (m_asc.equals(Boolean.FALSE.toString()))
		{
			result = -result;
		}
		return result;
	}
}
