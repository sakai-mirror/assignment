package org.sakaiproject.assignment.impl;

import org.sakaiproject.assignment.api.AssignmentEvent;
import org.sakaiproject.event.api.Event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The implementation of posting assignment event 
 * @author zqian
 *
 */
public class AssignmentEventImpl implements AssignmentEvent {
	
	private static Log log = LogFactory.getLog(AssignmentEventImpl.class);
    
    private org.sakaiproject.event.api.EventTrackingService eventTrackingService;
    public void setEventTrackingService(org.sakaiproject.event.api.EventTrackingService eventTrackingService) {
    	this.eventTrackingService = eventTrackingService;
    }

    public void init() {
    	if (log.isDebugEnabled()) log.debug("init");
    }
    
    public void postEvent(String message, String objectReference) {
        Event event = eventTrackingService.newEvent(message, objectReference, true);
        eventTrackingService.post(event);
    }

}
