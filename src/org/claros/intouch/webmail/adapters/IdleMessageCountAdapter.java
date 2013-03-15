package org.claros.intouch.webmail.adapters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IdleMessageCountAdapter extends MessageCountAdapter {
	private static Log log = LogFactory.getLog(IdleMessageCountAdapter.class);
	private static HashMap notices = new HashMap();
	private String username;

	public IdleMessageCountAdapter() {
		super();
	}
	
	public IdleMessageCountAdapter(String username) {
		this.username = username;
	}
	
    public void messagesAdded(MessageCountEvent ev) {
        Message[] msgs = ev.getMessages();
        
        log.debug("Got " + msgs.length + " new messages in folder");

        // Just dump out the new messages
        for (int i = 0; i < msgs.length; i++) {
            try {
            	log.debug("Message " + msgs[i].getMessageNumber() + ":");
            	ByteArrayOutputStream bs = new ByteArrayOutputStream();
                msgs[i].writeTo(bs);
                log.debug(new String(bs.toByteArray()));
                bs.close();
                
                addNotice(msgs[i]);
            } catch (IOException ioex) {
            	log.error(ioex);
                ioex.printStackTrace();
            } catch (MessagingException mex) {
            	log.error(mex);
                mex.printStackTrace();
            }
        }
    }

    /**
     * 
     * @param username2
     * @param message
     */
	private synchronized void addNotice(Message message) {
        List userNotices = (List)notices.get(username);
        if (userNotices == null) {
        	userNotices = new ArrayList();
        }
        userNotices.add(message);
        notices.put(username, userNotices);
	}

	/**
	 * 
	 * @param username
	 * @return
	 */
	public synchronized static List getNotices(String username) {
		List userNotices = null;
		if (notices != null) {
	        userNotices = (List)notices.get(username);
		}
        return userNotices;
	}

	/**
	 * 
	 * @param username
	 */
	public synchronized static void resetUserNotices(String username) {
		notices.put(username, null);
	}
	
}
