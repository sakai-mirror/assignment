package org.sakaiproject.assignment.tool;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.assignment.api.AssignmentEntityProvider;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.entitybroker.EntityBroker;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.access.HttpServletAccessProvider;
import org.sakaiproject.entitybroker.access.HttpServletAccessProviderManager;

/**
 * Does a redirect to allow basic DirectServlet access to old assignment Entities
 * 
 * @author Joshua Ryan  josh@asu.edu  alt^I
 *
 */

public class RedirectingAssignmentEntityServlet extends HttpServlet
  implements HttpServletAccessProvider {

  private static final long serialVersionUID = 0L;
  private EntityBroker entityBroker;
  private HttpServletAccessProviderManager accessProviderManager;
  
  /**
   * Initialize the servlet.
   * 
   * @param config
   *        The servlet config.
   * @throws ServletException
   */
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    entityBroker = (EntityBroker) ComponentManager
        .get("org.sakaiproject.entitybroker.EntityBroker");
    accessProviderManager = (HttpServletAccessProviderManager) ComponentManager
        .get("org.sakaiproject.entitybroker.access.HttpServletAccessProviderManager");
    if (accessProviderManager != null)
      accessProviderManager.registerProvider(AssignmentEntityProvider.ENTITY_PREFIX, this);
  }
  
  public void handleAccess(HttpServletRequest req, HttpServletResponse res, EntityReference ref) {    
    String target = entityBroker.getPropertyValue(req.getPathInfo(), "url");
    try {
      res.sendRedirect(target);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return;
  }
}
