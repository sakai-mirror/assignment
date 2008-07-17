package org.sakaiproject.assignment.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.api.model.AssignmentSupplementItemService;
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
   
   

}
