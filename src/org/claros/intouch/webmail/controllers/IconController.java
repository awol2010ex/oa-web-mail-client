package org.claros.intouch.webmail.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Properties;

import org.claros.commons.configuration.Paths;

public class IconController {
	private static Properties prop = new Properties();
	private static String defaultIcon = "binary.png";
	private static Locale loc = new Locale("en", "US");

	static {
        try {
        	FileInputStream is = new FileInputStream(new File(Paths.getCfgFolder() + "/mime.properties"));
			prop.load(is);
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        
	}

	/**
	 * 
	 * @param mime
	 * @return
	 */
	public static String findIcon(String mime) {
		if (mime == null) {
			return defaultIcon;
		}
		mime = mime.toLowerCase(loc).trim();
		if (mime.indexOf(";") > 0) {
			mime = mime.substring(0, mime.indexOf(";"));
		}
		if (mime.indexOf(" ") > 0) {
			mime = mime.substring(0, mime.indexOf(" "));
		}
		
		String ico = prop.getProperty(mime);
		if (ico == null) {
			if (mime.indexOf("/") > 0) {
				String mimeType = mime.substring(0, mime.indexOf("/"));
				ico = prop.getProperty(mimeType + "/*", defaultIcon);
			}
		}
		return ico;
	}
}
