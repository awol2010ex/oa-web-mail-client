package org.claros.intouch.notes.controllers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.claros.commons.auth.models.AuthProfile;
import org.claros.commons.db.DbConfigList;
import org.claros.commons.exception.NoPermissionException;
import org.claros.intouch.common.utility.Constants;
import org.claros.intouch.common.utility.Utility;
import org.claros.intouch.notes.models.NotesFolder;

import com.jenkov.mrpersister.impl.mapping.AutoGeneratedColumnsMapper;
import com.jenkov.mrpersister.itf.IGenericDao;
import com.jenkov.mrpersister.itf.mapping.IObjectMappingKey;
import com.jenkov.mrpersister.util.JdbcUtil;

/**
 * @author Umut Gokbayrak
 */
public class NotesFolderController {
	private static Log log = LogFactory.getLog(NotesFolderController.class);
	
	/**
	 * 
	 * @param auth
	 * @return
	 * @throws Exception
	 */
	public static List getFolders(AuthProfile auth) throws Exception {
		IGenericDao dao = null;
		List folders = null;
		try {
			dao = Utility.getDbConnection();
			String username = auth.getUsername();
			
			String sql = "SELECT * FROM NOTES_FOLDERS WHERE USERNAME = ?";

			folders = dao.readList(NotesFolder.class, sql, new Object[] {username});
		} finally {
			JdbcUtil.close(dao);
			dao = null;
		}
		return folders;
	}
	
	/**
	 * 
	 * @param auth
	 * @param folder
	 * @throws Exception
	 */
	public static Long saveFolder(AuthProfile auth, NotesFolder folder) throws Exception {
		IGenericDao dao = null;
		Long resId = new Long(-1);
		try {
			dao = Utility.getDbConnection();
			
			Long id = folder.getId();
			if (id == null) {
				// it is an insert
				IObjectMappingKey myObj = Constants.persistMan.getObjectMappingFactory().createInstance(NotesFolder.class, new AutoGeneratedColumnsMapper(true));
				dao.insert(myObj, folder);

				QueryRunner run = new QueryRunner(DbConfigList.getDataSourceById("file"));
				String username = auth.getUsername();
				String sql = "SELECT MAX(ID) AS NUMBER FROM NOTES_FOLDERS WHERE USERNAME=?";
				HashMap result = (HashMap)run.query(sql, new Object[] {username}, new MapHandler());
				Object hbl = result.get("number");
				if (hbl instanceof Long) {
					resId = new Long(((Long)hbl).longValue());
				} else if (hbl instanceof Integer) {
					resId = new Long(((Integer)hbl).longValue());
				} else if (hbl instanceof BigDecimal) {
					resId = new Long(((BigDecimal)hbl).longValue());
				} else if (hbl instanceof BigInteger) {
					resId = new Long(((BigInteger)hbl).longValue());
				}
			} else {
				// it is an update
				NotesFolder tmp = getFolderById(auth, folder.getId());
				String user = tmp.getUsername();
				if (!auth.getUsername().equals(user)) {
					throw new NoPermissionException();
				}
				dao.update(folder);
			}
		} finally {
			JdbcUtil.close(dao);
			dao = null;
		}
		return resId;
	}

	/**
	 * 
	 * @param auth
	 * @param folderId
	 * @throws Exception
	 */
	public static void deleteFolder(AuthProfile auth, Long folderId) throws Exception {
		IGenericDao dao = null;
		try {
			dao = Utility.getTxnDbConnection();
			String username = auth.getUsername();

			NotesFolder folder = getFolderById(auth, folderId);
			if (!folder.getUsername().equals(auth.getUsername())) {
				throw new NoPermissionException();
			}
			
			String sql = "DELETE FROM NOTES WHERE USERNAME=? AND FOLDER_ID = ?";
			// delete the notes under folder
			dao.executeUpdate(sql, new Object[] {username, folderId});
			// delete the folder
			dao.deleteByPrimaryKey(NotesFolder.class, folderId);
			dao.commit();
		} catch (Exception e) {
//			dao.rollback();
//			throw e;
			log.warn("error while deleting folder", e);
		} finally {
			JdbcUtil.close(dao);
			dao = null;
		}
	}

	/**
	 * @param auth
	 * @param folderId
	 * @return
	 */
	public static NotesFolder getFolderById(AuthProfile auth, Long folderId) throws Exception {
		IGenericDao dao = null;
		NotesFolder folder = null;
		try {
			dao = Utility.getDbConnection();
			String username = auth.getUsername();
			
			String sql = "SELECT * FROM NOTES_FOLDERS WHERE USERNAME=? AND ID = ?";

			folder = (NotesFolder)dao.read(NotesFolder.class, sql, new Object[] {username, folderId});
		} finally {
			JdbcUtil.close(dao);
			dao = null;
		}
		return folder;
	}

	/**
	 * 
	 * @param auth
	 * @param folderId
	 * @throws Exception
	 */
	public static void emptyFolder(AuthProfile auth, Long folderId) throws Exception {
		QueryRunner run = new QueryRunner(DbConfigList.getDataSourceById("file"));
		String username = auth.getUsername();
		String sql = "DELETE FROM NOTES WHERE USERNAME=? AND FOLDER_ID=?";
		run.update(sql, new Object[] {username, folderId});
	}
}
