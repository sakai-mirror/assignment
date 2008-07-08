package org.sakaiproject.assignment.impl;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.util.FormattedText;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.commonscodec.CommonsCodecBase64;
import org.xml.sax.Attributes;

public class AssignmentUtil {


	/** Our logger. */
	private static Log M_log = LogFactory.getLog(AssignmentUtil.class);
	
	/**
	 * Utility function which converts a string into a chef time object.
	 * 
	 * @param timeString -
	 *        String version of a time in long format, representing the standard ms since the epoch, Jan 1, 1970 00:00:00.
	 * @return A chef Time object.
	 */
	public static Time getTimeObject(String timeString)
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
				M_log.warn("AssignmentUtil:geTimeObject " + e.getMessage());
				try
				{
					long longTime = Long.parseLong(timeString);
					aTime = TimeService.newTime(longTime);
				}
				catch (Exception ee)
				{
					M_log.warn("AssignmentUtil:getTimeObject Base Exception creating time object from xml file : " + ee.getMessage() + " timeString=" + timeString);
				}
			}
		}
		return aTime;
	}
	
	/**
	 * Utility function which converts a string into a Date object.
	 * 
	 * @param dateString -
	 *        String version of a date in long format, representing the standard ms since the epoch, Jan 1, 1970 00:00:00.
	 * @return A chef Time object.
	 */
	public static Date getDateObject(String dateString)
	{
		Date aDate = null;
		dateString = StringUtil.trimToNull(dateString);
		if (dateString != null)
		{
			try
			{
				aDate = new Date(Long.parseLong(dateString));
			}
			catch (Exception e)
			{
				M_log.warn("AssignmentUtil:geDateObject " + e.getMessage());
			}
		}
		return aDate;
	}
	
	/**
	 * Utility function which returns a boolean value from a string.
	 * 
	 * @param s -
	 *        The input string.
	 * @return the boolean true if the input string is "true", false otherwise.
	 */
	public static boolean getBool(String s)
	{
		boolean retVal = false;
		if (s != null)
		{
			if (s.equalsIgnoreCase("true")) retVal = true;
		}
		return retVal;
	}
	

	/**
	 * Utility function which returns a string from a boolean value.
	 * 
	 * @param b -
	 *        the boolean value.
	 * @return - "True" if the input value is true, "false" otherwise.
	 */
	public static String getBoolString(boolean b)
	{
		if (b)
			return "true";
		else
			return "false";
	}
	
	/**
	 * Update the live properties for an object when modified.
	 */
	public static void addLiveUpdateProperties(ResourcePropertiesEdit props)
	{
		props.addProperty(ResourceProperties.PROP_MODIFIED_BY, SessionManager.getCurrentSessionUserId());

		props.addProperty(ResourceProperties.PROP_MODIFIED_DATE, TimeService.newTime().toString());

	} // addLiveUpdateProperties

	/**
	 * Create the live properties for the object.
	 */
	public static void addLiveProperties(ResourcePropertiesEdit props)
	{
		String current = SessionManager.getCurrentSessionUserId();
		props.addProperty(ResourceProperties.PROP_CREATOR, current);
		props.addProperty(ResourceProperties.PROP_MODIFIED_BY, current);

		String now = TimeService.newTime().toString();
		props.addProperty(ResourceProperties.PROP_CREATION_DATE, now);
		props.addProperty(ResourceProperties.PROP_MODIFIED_DATE, now);

	} // addLiveProperties
	

	/**
	 * This is to mimic the FormattedText.decodeFormattedTextAttribute but use SAX serialization instead
	 * @return
	 */
	public static String FormattedTextDecodeFormattedTextAttribute(Attributes attributes, String baseAttributeName)
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
	public static String XmlDecodeAttribute(Attributes attributes, String tag)
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
				M_log.warn("AssignmentUtil XmlDecodeAttribute: " + e.getMessage() + " tag=" + tag);
			}
		}

		if (body == null) body = "";

		return body;
	}
}
