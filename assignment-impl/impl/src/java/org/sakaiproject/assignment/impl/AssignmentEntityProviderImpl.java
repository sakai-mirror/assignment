package org.sakaiproject.assignment.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentEntityProvider;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.PropertyProvideable;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.SiteService;

public class AssignmentEntityProviderImpl implements AssignmentEntityProvider,
CoreEntityProvider, AutoRegisterEntityProvider, PropertyProvideable {

  private AssignmentService assignmentService;
  private SiteService siteService;
  
  public void setAssignmentService(AssignmentService assignmentService) {
    this.assignmentService = assignmentService;
  }

  public boolean entityExists(String id) {
    boolean rv = false;

    try {
      Assignment assignment = assignmentService.getAssignment(id);
      if (assignment != null) {
        rv = true;
      }
    }
    catch (Exception e) {}
    return rv;
  }

  public String getEntityPrefix() {
    return ENTITY_PREFIX;
  }

  public List<String> findEntityRefs(String[] prefixes, String[] name, String[] searchValue, boolean exactMatch) {
    String siteId = null;
    String userId = null;
    List<String> rv = new ArrayList<String>();

    if (ENTITY_PREFIX.equals(prefixes[0])) {

      for (int i = 0; i < name.length; i++) {
        if ("context".equalsIgnoreCase(name[i]) || "site".equalsIgnoreCase(name[i]))
          siteId = searchValue[i];
        else if ("user".equalsIgnoreCase(name[i]) || "userId".equalsIgnoreCase(name[i]))
          userId = searchValue[i];
      }

      if (siteId != null && userId != null) {
        Iterator assignmentSorter = assignmentService.getAssignmentsForContext(siteId, userId);
        // filter to obtain only grade-able assignments
        while (assignmentSorter.hasNext()) {
          Assignment a = (Assignment) assignmentSorter.next();
          if (assignmentService.allowGradeSubmission(a.getReference())) {
            rv.add(Entity.SEPARATOR + ENTITY_PREFIX + Entity.SEPARATOR + a.getId());
          }
        }
      }
    }
    return rv;
  }

  public Map<String, String> getProperties(String reference) {
    Map<String, String> props = new HashMap<String, String>();
    String parsedRef = reference;
    String defaultView = "doView_submission";
    String[] refParts = reference.split(Entity.SEPARATOR);
    String submissionId = "";
    
    if (refParts.length >= 4) {
    	parsedRef = refParts[0] + Entity.SEPARATOR + refParts[1] + Entity.SEPARATOR + refParts[2];
    	defaultView = refParts[3];
    	if (refParts.length >= 5) {
    		submissionId = refParts[4].replaceAll("_", Entity.SEPARATOR);
    	}
    }
    
    String assignmentId = parsedRef;
    try {
      Assignment assignment = assignmentService.getAssignment(assignmentId);
      props.put("title", assignment.getTitle());
      props.put("author", assignment.getCreator());
      if (assignment.getTimeCreated() != null)
        props.put("created_time", assignment.getTimeCreated().getDisplay());
        if (assignment.getAuthorLastModified() != null)
          props.put("modified_by", assignment.getAuthorLastModified());
        if (assignment.getTimeLastModified() != null)
          props.put("modified_time", assignment.getTimeLastModified().getDisplay());

      String placement = siteService.getSite(assignment.getContext()).getToolForCommonId("sakai.assignment.grades").getId();
      props.put("url", "/portal/tool/" + placement + "?assignmentId=" + assignment.getId() + 
    		  "&submissionId=" + submissionId +
    		  "&assignmentReference=" + assignment.getReference() + 
    		  "&panel=Main&sakai_action=" + defaultView);
      props.put("status", assignment.getStatus());
      props.put("due_time", assignment.getDueTimeString());
      props.put("open_time", assignment.getOpenTimeString());
      if (assignment.getDropDeadTime() != null)
        props.put("retract_time", assignment.getDropDeadTime().getDisplay());
      props.put("description", assignment.getContentReference());
      props.put("draft", "" + assignment.getDraft());
      props.put("siteId", assignment.getContext());
      props.put("section", assignment.getSection());
    }
    catch (IdUnusedException e) {
      e.printStackTrace();
    }
    catch (PermissionException e) {
      e.printStackTrace();
    }
    return props;
  }

  public String getPropertyValue(String reference, String name) {
    String rv = null;
    //lazy code, if any of the parts of getProperties is found to be slow this should be changed.
    Map<String, String> props = getProperties(reference);
    if (props != null && props.containsKey(name)) {
      rv = props.get(name);
    }
    return rv;
  }

  public void setPropertyValue(String reference, String name, String value) {
    // TODO: add ability to set properties of an assignment
  }

  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

}
