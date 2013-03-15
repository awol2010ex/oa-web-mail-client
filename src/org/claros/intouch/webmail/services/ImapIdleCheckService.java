package org.claros.intouch.webmail.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.claros.commons.auth.models.AuthProfile;
import org.claros.commons.mail.models.ConnectionProfile;
import org.claros.commons.mail.utility.Constants;
import org.claros.intouch.common.services.BaseService;
import org.claros.intouch.webmail.adapters.IdleMessageCountAdapter;
import org.claros.intouch.webmail.adapters.IdleOutputThread;
import org.claros.intouch.webmail.controllers.ImapFolderControllerImpl;
import org.claros.intouch.webmail.factory.FolderControllerFactory;

import com.sun.mail.imap.IMAPFolder;

public class ImapIdleCheckService extends BaseService {
	private static final long serialVersionUID = 6209371120941300627L;
	private static HashMap listeners = new HashMap();
	private static Log log = LogFactory.getLog(ImapIdleCheckService.class);

	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();

		String f = request.getParameter("folder");

		ConnectionProfile profile = getConnectionProfile(request);
		AuthProfile auth = getAuthProfile(request);

		if (profile.getProtocol().equals(Constants.IMAP)) {
			FolderControllerFactory foldFact = new FolderControllerFactory(auth, profile, getConnectionHandler(request));
			ImapFolderControllerImpl imapFold = (ImapFolderControllerImpl)foldFact.getFolderController();

/*			
 			if (listenerExists(auth.getUsername(), f)) {
				// listener already exists so no need to do any action. 
				return;
			}
*/
			Folder fold = null;
			try {
				log.debug("getting the folder");
				fold = imapFold.getFolderObj(f, false);
				log.debug("got folder:" + f);
			} catch (Exception e) {
				log.error(e);
			}
			if (fold != null) {
				try {
					log.debug("monitoring....");
					addListener(auth.getUsername(), f);

					IdleMessageCountAdapter adp = new IdleMessageCountAdapter(auth.getUsername());
					
					IdleOutputThread ot = new IdleOutputThread(response.getWriter(), auth.getUsername());
					ot.start();

					monitorNewMail(auth.getUsername(), f, (IMAPFolder)fold, adp);
				} catch (Exception e) {
					log.error(e);
				}
			}
		}
		out.write("alert('FINITO');");
		out.flush();
	}

	/**
	 * 
	 * @param username
	 * @param folder
	 */
	public static void addListener(String username, String folder) {
		List userListener = (List)listeners.get(username);
		if (userListener == null) {
			userListener = new ArrayList();
		}
		if (!userListener.contains(folder)) {
			userListener.add(folder);
		}
		listeners.put(username, userListener);
	}

	/**
	 * 
	 * @param username
	 * @param folder
	 */
	public static boolean listenerExists(String username, String folder) {
		List userListener = (List)listeners.get(username);
		if (userListener == null) {
			userListener = new ArrayList();
		}
		return userListener.contains(folder);
	}

	/**
	 * 
	 * @param username
	 * @param folder
	 */
	public static void removeListener(String username, String folder) {
		List userListener = (List)listeners.get(username);
		if (userListener == null) {
			userListener = new ArrayList();
		}
		if (!userListener.contains(folder)) {
			userListener.remove(folder);
		}
		listeners.put(username, userListener);
	}
	
	/**
	 * 
	 * @param fold
	 * @param out
	 * @throws Exception
	 */
	public void monitorNewMail(String username, String folderName, IMAPFolder fold, IdleMessageCountAdapter adapter) throws Exception {
        // Add messageCountListener to listen for new messages
        fold.addMessageCountListener(adapter);

        // Check mail once in "freq" MILLIseconds
        try {
			int freq = 5000;
			boolean supportsIdle = false;
			try {
			    if (fold instanceof IMAPFolder) {
			        IMAPFolder f = (IMAPFolder)fold;
			        f.idle(); 
			        supportsIdle = true;
			    }
			} catch (FolderClosedException fex) {
			    throw fex;
			} catch (MessagingException mex) {
			    supportsIdle = false;
			}
			while (listenerExists(username, folderName)) {
			    if (supportsIdle && fold instanceof IMAPFolder) {
			        IMAPFolder f = (IMAPFolder)fold;
			        f.idle();
			        log.debug("IDLE done");
			    } else {
			        Thread.sleep(freq); // sleep for freq milliseconds
			        // This is to force the IMAP server to send us
			        // EXISTS notifications.
			        fold.getMessageCount();
			    }
			}
		} catch (FolderClosedException e) {
			log.error(e);
		} catch (MessagingException e) {
			log.error(e);
		} catch (InterruptedException e) {
			log.error(e);
		}
	}
}
