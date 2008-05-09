/**********************************************************************************
 * $URL:  $
 * $Id:  $
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
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

package org.sakaiproject.assignment.api;

/**
 * The base class for all attachment type
 * @author zqian
 *
 */
public class AttachmentBase {
	protected Long id;
	protected String attachmentReference;
	protected int hibernateVersion;  //for optimistic locking

	/**
	 * @return the id of this assignment attachment
	 */
	public Long getId() {
		return id;
	}

	/**
	 * set the the id of this assignment attachment
	 * @param id
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * 
	 * @return the reference to this attachment
	 */
	public String getAttachmentReference() {
		return attachmentReference;
	}

	/**
	 * set the reference to this attachment
	 * @param attachmentReference
	 */
	public void setAttachmentReference(String attachmentReference) {
		this.attachmentReference = attachmentReference;
	}

	public int getHibernateVersion() {
		return hibernateVersion;
	}

	public void setHibernateVersion(int hibernateVersion) {
		this.hibernateVersion = hibernateVersion;
	}
}
