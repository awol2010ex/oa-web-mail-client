package org.claros.intouch.webmail.adapters;

import java.io.PrintWriter;
import java.util.List;

import javax.mail.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.claros.intouch.webmail.services.ImapIdleCheckService;

/**
 * 
 * @author umut
 *
 */
public class IdleOutputThread extends Thread {
	private static Log log = LogFactory.getLog(IdleOutputThread.class);

	private PrintWriter out;
	private String username;
	
	public IdleOutputThread() {
		super();
	}
	
	public IdleOutputThread(PrintWriter out, String username) {
		super();
		this.out = out;
		this.username = username;
	}

	public void run() {
		while (true) {
			try {
				List notices = IdleMessageCountAdapter.getNotices(username);
				if (notices != null) {
					Message tmp = null;
					for (int i=0;i<notices.size();i++) {
						tmp = (Message)notices.get(i);
						if (out == null) {
							ImapIdleCheckService.removeListener(username, "INBOX");
							break;
						}
						out.print("alert('" + tmp.getMessageNumber() + ":" + tmp.getSubject() + "');");
						out.flush();
					}
					IdleMessageCountAdapter.resetUserNotices(username);
				}
				out.print(";");
				out.flush();
				Thread.sleep(4000);
			} catch (Throwable t) {
				log.warn(t);
			}
		}
	}
	
}
