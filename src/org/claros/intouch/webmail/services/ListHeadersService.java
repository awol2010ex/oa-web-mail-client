package org.claros.intouch.webmail.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.claros.commons.auth.models.AuthProfile;
import org.claros.commons.mail.comparator.ComparatorDate;
import org.claros.commons.mail.comparator.ComparatorFrom;
import org.claros.commons.mail.comparator.ComparatorSize;
import org.claros.commons.mail.comparator.ComparatorSubject;
import org.claros.commons.mail.comparator.ComparatorTo;
import org.claros.commons.mail.models.ConnectionMetaHandler;
import org.claros.commons.mail.models.ConnectionProfile;
import org.claros.commons.mail.models.EmailHeader;
import org.claros.commons.mail.utility.Constants;
import org.claros.intouch.common.services.BaseService;
import org.claros.intouch.webmail.controllers.FolderController;
import org.claros.intouch.webmail.controllers.InboxController;
import org.claros.intouch.webmail.factory.FolderControllerFactory;
import org.claros.intouch.webmail.factory.InboxControllerFactory;
import org.claros.intouch.webmail.models.FolderDbObject;
import org.claros.intouch.common.utility.Utility;

public class ListHeadersService extends BaseService {

	/**
	 * 
	 */
	private static final long serialVersionUID = -38711675148470029L;
	private static Log log = LogFactory.getLog(ListHeadersService.class);
	private static Locale loc;
	
	static {
		if (org.claros.intouch.common.utility.Constants.charset.indexOf("_") < 0) {
			loc = new Locale(org.claros.intouch.common.utility.Constants.charset);
		} else {
			StringTokenizer token = new StringTokenizer(org.claros.intouch.common.utility.Constants.charset, "_");
			loc = new Locale(token.nextToken(), token.nextToken());
		}
	}

	/**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to post.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setHeader("Expires","-1");
		response.setHeader("Pragma","no-cache");
		response.setHeader("Cache-control","no-cache");
		response.setHeader("Content-Type", "text/html; charset=utf-8");

		PrintWriter out = response.getWriter();

		AuthProfile auth = getAuthProfile(request);
		// get folder and set it into sesssion
		String sFolder = URLDecoder.decode((String)getVariable(request, "folder"), "UTF-8");

		// prepare variables
		List headers = null;
		ConnectionMetaHandler handler = getConnectionHandler(request);
		ConnectionProfile profile = getConnectionProfile(request);

		FolderControllerFactory foldFact = null;
		FolderController folderCont = null;
		String currFolder = org.claros.commons.mail.utility.Constants.FOLDER_INBOX(profile);

		try {
			if (auth == null) {
				throw new org.claros.commons.exception.NoPermissionException();
			}
			// if folder name is empty or it is inbox then do mail filtering. It is done by inbox controller
			if (sFolder == null || sFolder.equals("") || sFolder.equals(currFolder)) {
				try {
					InboxControllerFactory inFact = new InboxControllerFactory(auth, profile, handler);
					InboxController inCont = inFact.getInboxController();
					handler = inCont.checkEmail();
					request.getSession().setAttribute("handler", handler);
					foldFact = new FolderControllerFactory(auth, profile, handler);
					folderCont = foldFact.getFolderController();
				} catch (Exception e) {
					log.debug("minor error while checking new mail", e);
				}

				// get the id(pop3) or the mail folder name (imap)
				if (profile.getProtocol().equals(org.claros.commons.mail.utility.Constants.POP3)) {
					currFolder = folderCont.getInboxFolder().getId().toString();
				} else {
					currFolder = folderCont.getInboxFolder().getFolderName();
				}
			} else {
				currFolder = sFolder;
				handler = (ConnectionMetaHandler)request.getSession().getAttribute("handler");
				foldFact = new FolderControllerFactory(auth, profile, handler);
				folderCont = foldFact.getFolderController();
			}
			request.getSession().setAttribute("folder", currFolder);

			// time to fetch the headers
			headers = folderCont.getHeadersByFolder(currFolder);

			// get info about the current folder
			FolderDbObject myFolder = folderCont.getFolder(currFolder);

			// get and set sort parameters
			String mailSort = (String)getVariable(request, "mailSort");
			if(null==mailSort) mailSort = "date";
			String mailSortDirection = (String)getVariable(request, "mailSortDirection");
			if(null==mailSortDirection) mailSortDirection = "desc";
			request.getSession().setAttribute("mailSort", mailSort);
			request.getSession().setAttribute("mailSortDirection", mailSortDirection);
			
			// get and set pageNo
			int pageNo = 1;
			try {pageNo = Integer.parseInt(getVariable(request, "pageNo").toString());}
			catch (Exception e) {}
			request.getSession().setAttribute("pageNo", new Integer(pageNo));
			
			Cookie c1 = new Cookie("mailSort", mailSort);
			c1.setMaxAge(Integer.MAX_VALUE);
			Cookie c2 = new Cookie("mailSortDirection", mailSortDirection);
			c2.setMaxAge(Integer.MAX_VALUE);
			Cookie c3 = new Cookie("pageNo", String.valueOf(pageNo));
			c3.setMaxAge(Integer.MAX_VALUE);
			response.addCookie(c1);
			response.addCookie(c2);
			response.addCookie(c3);
			
			boolean isAscending = false;
			if (mailSortDirection != null && mailSortDirection.equals("asc")) {
				isAscending = true;
			}
			
			// organize em
			String fromSort = "";
			String dateSort = "";
			String sizeSort = "";
			String subjectSort = "";
			
			if (mailSort == null || mailSort.equals("date")) {
				if (isAscending) dateSort = "asc"; else dateSort = "desc";
			} else if (mailSort.equals("from")) {
				if (isAscending) fromSort = "asc"; else fromSort = "desc";
			} else if (mailSort.equals("subject")) {
				if (isAscending) subjectSort = "asc"; else subjectSort = "desc";
			} else if (mailSort.equals("to")) {
				if (isAscending) fromSort = "asc"; else fromSort = "desc";
			} else if (mailSort.equals("size")) {
				if (isAscending) sizeSort = "asc"; else sizeSort = "desc";
			}
			
			out.print("<div id=\"ieHolder\" class=\"ieHolder\"><div class=\"ie7Holder\" id=\"ie7Holder\">");
			
			// at least show the title bar also with sort arrows
			if (profile.getProtocol().equals(Constants.IMAP)) {
				out.print("<p class='title' id='mailtitle' displayName='" + myFolder.getFolderName() + "' folderid='" + myFolder.getId() + "' foldername='mailFolder" + myFolder.getFolderName() + "' type='" + myFolder.getFolderType() + "'>" + 
				  "	<span class='flag' style='background-image:url(images/apply.gif);background-repeat:no-repeat;' onclick='clickAll();'><a href='javascript:;'>&nbsp;</a></span>");
			} else {
				out.print("<p class='title' id='mailtitle' displayName='" + myFolder.getFolderName() + "' folderid='" + myFolder.getId() + "' foldername='mailFolder" + myFolder.getId() + "' type='" + myFolder.getFolderType() + "'>" + 
				  "	<span class='flag' style='background-image:url(images/apply.gif);background-repeat:no-repeat;' onclick='clickAll();'><a href='javascript:;'>&nbsp;</a></span>");
			}
			out.print("<span class='attributes'><a href='javascript:;'><span style='cursor:hand'><img alt='' src='images/priority-title.gif' align='absmiddle'><img alt='' src='images/sensitivity-title.gif' align='absmiddle'></span></a></span>");
			
			if (myFolder.getFolderType().equals(org.claros.intouch.common.utility.Constants.FOLDER_TYPE_SENT)) {
				  out.print("  <span class='from " + fromSort + "' onclick=\"sortColumn(this, 'to');\" sort='" + fromSort + "'><a href='javascript:;'><span style='cursor:hand'>" + getText(request, "to") + "</span></a></span>");
				  if (mailSort != null && mailSort.equals("from")) {
					  mailSort = "to";
				  }
			} else {
				  out.print("  <span class='from " + fromSort + "' onclick=\"sortColumn(this, 'from');\" sort='" + fromSort + "'><a href='javascript:;'><span style='cursor:hand'>" + getText(request, "from") + "</span></a></span>"); 
				  if (mailSort != null && mailSort.equals("to")) {
					  mailSort = "from";
				  }
			}
			out.print("	<span class='subject " + subjectSort + "' onclick=\"sortColumn(this, 'subject');\" sort='" + subjectSort + "'><a href='javascript:;'><span style='cursor:hand'>" + getText(request, "subject") + "</span></a></span>" + 
					  "	<span class='date " + dateSort + "' onclick=\"sortColumn(this, 'date');\" sort='" + dateSort + "'><a href='javascript:;'><span style='cursor:hand'>" + getText(request, "date") + "</span></a></span>" + 
					  "	<span class='size " + sizeSort + "' onclick=\"sortColumn(this, 'size');\" sort='" + sizeSort + "'><a href='javascript:;'><span style='cursor:hand'>" + getText(request, "size") + "</span></a></span>" + 
					  "	<span class='attach'><a href='javascript:;'>&nbsp;</a></span>" + 
			  		  "</p>");
			// sort the headers
			if (mailSort == null || mailSort.equals("date")) {
				Collections.sort(headers, new ComparatorDate(isAscending));
			} else if (mailSort.equals("from")) {
				Collections.sort(headers, new ComparatorFrom(isAscending, loc));
			} else if (mailSort.equals("subject")) {
				Collections.sort(headers, new ComparatorSubject(isAscending, loc));
			} else if (mailSort.equals("to")) {
				Collections.sort(headers, new ComparatorTo(isAscending, loc));
			} else if (mailSort.equals("size")) {
				Collections.sort(headers, new ComparatorSize(isAscending));
			}
			
			// organize and generate XML from the headers.
			if (headers != null) {
				EmailHeader tmp = null;
				String subject = null;
				String from = null;
				int pageSize = 25;
				int messageCount = headers.size();
				int pageCount = messageCount/pageSize;
				if((messageCount%pageSize)>0) pageCount++;
				if(pageNo > pageCount) pageNo = pageCount;
				int startIdx = (pageNo-1)*pageSize;
				if (startIdx < 0) startIdx = 0;
				int endIdx = startIdx+pageSize;
				if(endIdx > messageCount) endIdx = messageCount;
				
				out.print("<span id=\"pagerParams\" style=\"display:none\">"+pageNo+":"+pageCount+":"+messageCount+"</span>");
				
				for (int i=startIdx;i<endIdx;i++) {
					tmp = (EmailHeader)headers.get(i);
					subject = tmp.getSubject();
					if(null == subject || 0 == subject.length())
						subject = getText(request, "no.subject");
					from = tmp.getFromShown();
					if(null == from || 0 == from.length())
						from = getText(request, "unknown.sender");
					String priority = org.claros.commons.mail.models.EmailPriority.toStringValue(tmp.getPriority());
					short sensitivity = tmp.getSensitivity();
					
					out.print("<p " + ((tmp.isMultipart()) ? " class='attach' ": "") + "id='mail" + tmp.getMessageId() + "' " + ((tmp.getUnread().booleanValue()) ? "style='font-weight:bold;'" : "") + ">" + 
							  "<span class='flag' onclick='mailListClick(event, this, false, true);'>&nbsp;</span>");
					out.print("<span class='attributes' onclick='mailListClick(event, this, true, false);'><img alt='' src='images/priority-"+ priority.toLowerCase() +".gif' align='absmiddle'><img alt='' src='images/sensitivity-"+ sensitivity +".gif' align='absmiddle'></span>"); 
					if (myFolder.getFolderType().equals(org.claros.intouch.common.utility.Constants.FOLDER_TYPE_SENT)) {
						  out.print("<span class='from' onclick='mailListClick(event, this, true, false);'>" + org.claros.commons.utility.Utility.convertTRCharsToHtmlSafe(Utility.htmlCheck(sbjTrim(tmp.getToShown()))) + "</span>"); 
					} else {
						  out.print("<span class='from' onclick='mailListClick(event, this, true, false);'>" + org.claros.commons.utility.Utility.convertTRCharsToHtmlSafe(Utility.htmlCheck(sbjTrim(from))) + "</span>"); 
					}
					out.print("<span class='subject' onclick='mailListClick(event, this, true, false);' title=\"" + org.claros.commons.utility.Utility.convertTRCharsToHtmlSafe(Utility.htmlCheck(subject)) + "\">" + org.claros.commons.utility.Utility.convertTRCharsToHtmlSafe(Utility.htmlCheck(sbjTrim(subject))) + "</span>" + 
							  "<span class='date' onclick='mailListClick(event, this, true, false);'>" + Utility.htmlCheck(tmp.getDateShown()) + "</span>" + 
							  "<span class='size' onclick='mailListClick(event, this, true, false);'>" + Utility.htmlCheck(tmp.getSizeShown()) + "</span>" + 
							  "<span class='attach' onclick='mailListClick(event, this, true, false);'>&nbsp;</span>" + 
							  "</p>");
				}
			}

			out.print("</div></div>");
		} catch (Exception e) {
			out.print("<p class='title'>" + 
					  "<span class='flag' style='background-image:url(images/apply.gif);background-repeat:no-repeat;' onclick='clickAll();'><a href='javascript:;'>&nbsp;</a></span>" + 
					  "<span class='attributes'><a href='javascript:;'><span>&nbsp;</span></a></span>" + 
					  "<span class='from asc'><a href='javascript:;'><span>" + getText(request, "from") + "</span></a></span>" + 
					  "<span class='subject'><a href='javascript:;'><span>" + getText(request, "subject") + "</span></a></span>" + 
					  "<span class='date'><a href='javascript:;'><span>" + getText(request, "date") + "</span></a></span>" + 
					  "<span class='size'><a href='javascript:;'><span>" + getText(request, "size") + "</span></a></span>" + 
					  "<span class='attach'><a href='javascript:;'>&nbsp;</a></span>" + 
					  "</p>");

		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
	
	private String sbjTrim(String str) {
		if (str != null) {
			if (str.length() > 40) {
				str = str.substring(0,40) + "...";
			}
		}
		return str;
	}
}
