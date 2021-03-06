package com.zizibujuan.dbaccess.mysql.util;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zizibujuan.dbaccess.mysql.DataAccessException;
import com.zizibujuan.drip.server.util.IdGenerator;
import com.zizibujuan.drip.server.util.PageInfo;

/**
 * 与数据库进行交互的工具集
 * @author jinzw
 * @since 0.0.1
 */
public abstract class DatabaseUtil {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);
	
	private static String DEFAULT_ID = "DBID";
	private static String SQL_DELETE_BY_IDENTITY = "DELETE FROM %s WHERE %s=?";
	private static String SQL_GET_BY_IDENTITY = "SELECT * FROM %s WHERE %s=?";
	
	public static List<Map<String, Object>> queryForList(Connection con, String sql, Object...params){
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		PreparedStatement stmt = null;
		ResultSet rst = null;
		try {
			stmt = con.prepareStatement(sql);
			addParams(stmt, params);
			
			rst = stmt.executeQuery();
			ResultSetMetaData meta = rst.getMetaData();
			int columnCount = meta.getColumnCount();
			while(rst.next())
			{
				Map<String,Object> map = new HashMap<String, Object>();
				for(int i = 0; i < columnCount; i++){
					int index = i + 1;
					String key = meta.getColumnLabel(index);
					Object value = getResultSetValue(rst, index);
					map.put(key, value);
				}
				result.add(map);
			}
		} catch (SQLException e) {
			throw new DataAccessException("执行失败：sql语句为:"+sql,e);
		}
		finally
		{
			DatabaseUtil.closeResultSet(rst);
			DatabaseUtil.closeStatement(stmt);
		}
		return result;
	}
	
	public static List<Map<String, Object>> queryForList(DataSource ds, String sql, Object... params) {
		
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		PreparedStatement stmt = null;
		ResultSet rst = null;
		Connection con= null;
		try {
			con = ds.getConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(sql);
			addParams(stmt, params);
			
			rst = stmt.executeQuery();
			ResultSetMetaData meta = rst.getMetaData();
			int columnCount = meta.getColumnCount();
			while(rst.next())
			{
				Map<String,Object> map = new HashMap<String, Object>();
				for(int i = 0; i < columnCount; i++){
					int index = i + 1;
					String key = meta.getColumnLabel(index);
					Object value = getResultSetValue(rst, index);
					map.put(key, value);
				}
				result.add(map);
			}
		} catch (SQLException e) {
			throw new DataAccessException("执行失败：sql语句为:"+sql,e);
		}
		finally
		{
			DatabaseUtil.safeClose(con, rst, stmt);
		}
		return result;
	}

	public static List<Map<String, Object>> queryForList(DataSource ds, String sql,PageInfo pageInfo, Object... params){
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		PreparedStatement stmt = null;
		ResultSet rst = null;
		Connection con= null;
		try {
			String sqlPage = sql;
			if(pageInfo != null){
				 sqlPage += "limit "+pageInfo.getStart()+" "+(pageInfo.getEnd()-pageInfo.getStart());
			}
			con = ds.getConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(sqlPage);
			addParams(stmt, params);
			
			rst = stmt.executeQuery();
			ResultSetMetaData meta = rst.getMetaData();
			int columnCount = meta.getColumnCount();
			while(rst.next())
			{
				Map<String,Object> map = new HashMap<String, Object>();
				for(int i = 0; i < columnCount; i++){
					int index = i + 1;
					String key = meta.getColumnLabel(index);
					Object value = getResultSetValue(rst, index);
					map.put(key, value);
				}
				result.add(map);
			}
			
		} catch (SQLException e) {
			throw new DataAccessException(e);
		}
		finally
		{
			DatabaseUtil.safeClose(con, rst, stmt);
		}
		
		if(pageInfo != null){
			int count = getCount(ds,sql,params);
			pageInfo.setCount(count);
		}
		return result;
	}
	
	private static int getCount(DataSource ds, String sql, Object... params){
		String sqlCount = "select count(*) from ( " + sql + " ) t";
		return queryForInt(ds, sqlCount, params);
	}
	
	private static Object getResultSetValue(ResultSet rst, int index)throws SQLException{
		Object obj = rst.getObject(index);
		
		if(obj instanceof Blob){
			obj = rst.getBytes(index);
		}
		else if(obj instanceof Clob){
			obj = rst.getString(index);
		}else if(obj !=null && obj instanceof java.sql.Date){
			if(rst.getMetaData().getColumnClassName(index).equals("java.sql.Timestamp")){
				obj = rst.getTimestamp(index);
			}
		}
		return obj;
	}
	
	/**
	 * FIXME:目前只支持传递字符串类型的参数
	 * @param sql
	 * @param params
	 * @return
	 * @throws SQLException 
	 */
	public static String queryForString(DataSource ds,String sql, Object... params){
		String result=null;
		PreparedStatement stmt = null;
		ResultSet rst = null;
		Connection con = null;
		try {
			con = ds.getConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(sql);
			addParams(stmt, params);
			
			rst = stmt.executeQuery();

			if(rst.next())
			{
				result = rst.getString(1);
			}
		} catch (SQLException e) {
			throw new DataAccessException(e);
		}
		finally
		{
			DatabaseUtil.safeClose(con, rst, stmt);
		}
		return result;
	}
	
	public static Long queryForLong(DataSource ds,
			String sql, Object... inParams) {
		Long result=null;
		PreparedStatement stmt = null;
		ResultSet rst = null;
		Connection con = null;
		try {
			con = ds.getConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(sql);
			addParams(stmt, inParams);
			
			rst = stmt.executeQuery();

			if(rst.next())
			{
				result = rst.getLong(1);
			}
		} catch (SQLException e) {
			throw new DataAccessException(e);
		}
		finally
		{
			DatabaseUtil.safeClose(con, rst, stmt);
		}
		return result;
	}
	
	public static Integer queryForInt(DataSource ds, String sql, Object... params) {
		Integer result = null;
		PreparedStatement stmt = null;
		ResultSet rst = null;
		Connection con = null;
		try {
			con = ds.getConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(sql);
			addParams(stmt, params);
			
			rst = stmt.executeQuery();

			if(rst.next())
			{
				result = rst.getInt(1);
			}
		} catch (SQLException e) {
			throw new DataAccessException(e);
		}
		finally
		{
			DatabaseUtil.safeClose(con, rst, stmt);
		}
		return result;
	}

	public static void addParams(PreparedStatement stmt, Object... params)
			throws SQLException {
		if(params != null){
			setParams(stmt, params);
		}
	}
	
	/**
	 * 约定：sql的select语句中的第一列为key值，第二列为value值
	 * @param sql
	 * @param params
	 * @return
	 * @throws SQLException
	 */
	public static List<Map<String,Object>> queryForKeyValuePair(DataSource ds,String sql,Object... params){
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		PreparedStatement stmt = null;
		ResultSet rst = null;
		Connection con = null;
		try {
			con = ds.getConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(sql);
			addParams(stmt, params);
			
			rst = stmt.executeQuery();
			
			while (rst.next())
			{
				Map<String,Object> map = new HashMap<String, Object>();
				String key = rst.getString(1);
				String value = rst.getString(2);
				map.put(key, value);
				result.add(map);
			}
		} catch (SQLException e) {
			throw new DataAccessException(e);
		}
		finally
		{
			DatabaseUtil.safeClose(con, rst, stmt);
		}
		return result;	
	}
	
	
	public static void safeClose(Connection con, ResultSet rst, Statement stmt){
		try {
			if (rst != null){
				rst.close();
			}
			if (stmt != null){
				stmt.close();
			}
			if (con != null){
				if (con.getAutoCommit() == false) {
					con.setAutoCommit(true);
				}
				con.close();
			}
		} catch (SQLException e) {
			//TODO:把这个异常类进一步具体话，以便获取更详细的信息
			throw new DataAccessException(e);
		}
	}
	
	public static void safeClose(Connection con, Statement stmt){
		try {
			if (stmt != null){
				stmt.close();
			}
			if (con != null){
				if (con.getAutoCommit() == false) {
					con.setAutoCommit(true);
				}
				con.close();
			}
		} catch (SQLException e) {
			throw new DataAccessException(e);
		}
	}

	public static void closeResultSet(ResultSet... params){
		if(params != null){
			for(ResultSet each : params){
				if (each != null){
					try {
						each.close();
					} catch (SQLException e) {
						throw new DataAccessException(e);
					}
				}
			}
		}
	}

	public static void closeStatement(Statement... params){
		if(params != null){
			for(Statement each : params){
				if (each != null){
					try {
						each.close();
					} catch (SQLException e) {
						throw new DataAccessException(e);
					}
				}
			}
		}
		
	}

	public static void closeConnection(Connection con) {
		if (con != null){
			try {
				if (con.getAutoCommit() == false) {
					con.setAutoCommit(true);
				}
				con.close();
			} catch (SQLException e) {
				throw new DataAccessException(e);
			}
		}
	}
	
	public static Map<String, Object> queryForMap(Connection con,String sql, Object... params){
		List<Map<String,Object>> list = queryForList(con,sql, params);
		int count = list.size();
		if(count==1){
			return list.get(0);
		}else if(count==0){
			return new HashMap<String, Object>();
		}else{
			throw new IllegalStateException("查询出来的记录数不允许大于1，结果却为"+count);
		}
	}

	public static Map<String, Object> queryForMap(DataSource ds,String sql, Object... params){
		List<Map<String,Object>> list = queryForList(ds,sql, params);
		int count = list.size();
		if(count==1){
			return list.get(0);
		}else if(count==0){
			return new HashMap<String, Object>();
		}else{
			throw new IllegalStateException("查询出来的记录数不允许大于1，结果却为"+count);
		}
	}
	
	/**
	 * 新增数据
	 * @param ds 数据源
	 * @param map 数据
	 * @param insertIdentity 是否在sql语句的最前面插入标识，即由程序生成主键标识
	 * @return 如果insertIdentity为<code>true</code>,则返回系统生成的主键;如果<code>false</code>,则返回null
	 */
	public static String insert(DataSource ds, Map<String,Object> map, boolean insertIdentity){
		String sql = SqlParser.getInsertSql(map.keySet(),insertIdentity);
		Object[] inParams = null;
		if(insertIdentity){
			String dbid = IdGenerator.uuid();
			inParams = SqlParser.wrapWithUUID(dbid, SqlParser.getInParams(map));
			update(ds, sql, inParams);
			return dbid;
		}else{
			inParams = SqlParser.getInParams(map);
			update(ds, sql, inParams);
			return null;
		}
	}
	
	public static String insert(DataSource ds, Map<String,Object> map){
		return insert(ds, map,true);
	}
	
	
	public static int update(DataSource ds, String sql, Object...inParams){
		Connection con = null;
		PreparedStatement pst = null;
		int result = 0;
		try {
			con = ds.getConnection();
			pst = con.prepareStatement(sql);
			con.setAutoCommit(false);
			int len = inParams.length;
			for(int i = 0; i < len; i++){
				pst.setObject(i+1, inParams[i]);
			}
			result = pst.executeUpdate();
			con.commit();
			return result;
			
		}catch(SQLException e){
			safeRollback(con);
			logger.error("执行更新操作失败", e);
			throw new DataAccessException(e);
		}finally{
			safeClose(con, null, pst);
		}
	}
	
	public static int update(Connection con, String sql, PreparedStatementSetter setter) throws SQLException{
		PreparedStatement pst = null;
		int result = 0;
		try {
			pst = con.prepareStatement(sql);
			setter.setValues(pst);
			result = pst.executeUpdate();
			return result;
		}finally{
			closeStatement(pst);
		}
	}
	
	public static int deleteByIdentity(DataSource ds, String tableName, String IdFieldName,
			String id) {
		String sql = String.format(SQL_DELETE_BY_IDENTITY, tableName,IdFieldName);
		return update(ds, sql, id);
	}
	
	public static int deleteByIdentity(DataSource ds, String tableName,
			String id) {
		return deleteByIdentity(ds, tableName, DEFAULT_ID,id);
	}
	
	public static Map<String,Object> getByIdentity(DataSource ds, String tableName, String IdFieldName,
			String id){
		String sql = String.format(SQL_GET_BY_IDENTITY, tableName, IdFieldName);
		return queryForMap(ds, sql, id);
	}
	
	public static Map<String, Object> getByIdentity(DataSource ds,
			String tableName, String id) {
		return getByIdentity(ds, tableName, DEFAULT_ID, id);
	}
	
	public static void safeRollback(Connection con) {
		if(con !=null){
			try {
				con.rollback();
			} catch (SQLException e1) {
				throw new DataAccessException(e1);
			}
		}
	}

	/**
	 * 新增记录，对mysql有效，因为getGeneratedKeys返回的正是自增列
	 * @param ds
	 * @param sql
	 * @param inParams
	 * @return 新增记录的标识。
	 */
	public static long insert(DataSource ds, String sql,  Object...inParams) {
		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rst = null;
		long result = 0;
		try {
			con = ds.getConnection();
			pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			con.setAutoCommit(false);
			int len = inParams.length;
			for(int i = 0; i < len; i++){
				pst.setObject(i+1, inParams[i]);
			}
			pst.executeUpdate();
			rst = pst.getGeneratedKeys();    
			rst.next();         
			result = rst.getLong(1); 
			con.commit();
			return result;
		}catch(SQLException e){
			safeRollback(con);
			throw new DataAccessException(e);
		}finally{
			safeClose(con, rst, pst);
		}
	}
	
	public static long insert(Connection con, String sql, Object... inParams) throws SQLException{
		if(inParams == null){
			inParams = new Object[]{};
		}
		PreparedStatement pst = null;
		ResultSet rst = null;
		try{
			pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			setParams(pst, inParams);
			pst.executeUpdate();
			rst = pst.getGeneratedKeys();    
			rst.next();         
			return rst.getLong(1);
		}catch(SQLException e){
			logger.error("insert sql出错，sql语句是:" + sql, e);
			throw e;
		}finally{
			DatabaseUtil.closeResultSet(rst);
			DatabaseUtil.closeStatement(pst);
		}
	}
	
	public static long insert(Connection con, String sql, PreparedStatementSetter pss) throws SQLException{
		PreparedStatement pst = null;
		ResultSet rst = null;
		Long result = null;
		try {
			pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			pss.setValues(pst);
			pst.executeUpdate();
			rst = pst.getGeneratedKeys();    
			rst.next();
			result = rst.getLong(1);
		}catch(SQLException e){
			logger.error("update sql出错，sql语句是:" + sql, e);
			throw e;
		}finally{
			DatabaseUtil.closeResultSet(rst);
			DatabaseUtil.closeStatement(pst);
		}
		return result;
	}
	

	public static void update(Connection con, String sql, Object... inParams) throws SQLException {
		if(inParams == null){
			inParams = new Object[]{};
		}
		PreparedStatement pst = null;
		ResultSet rst = null;
		try{
			pst = con.prepareStatement(sql);
			setParams(pst, inParams);
			pst.executeUpdate();
		}catch(SQLException e){
			logger.error("更新sql出错，sql语句是:" + sql, e);
			throw e;
		}finally{
			DatabaseUtil.closeResultSet(rst);
			DatabaseUtil.closeStatement(pst);
		}
	}

	private static void setParams(PreparedStatement pst, Object... inParams)
			throws SQLException {
		int len = inParams.length;
		for(int i = 0; i < len; i++){
			pst.setObject((i+1), inParams[i]);
		}
	}
	
	/**
	 * 批量更新数据库
	 * @param con 数据库链接
	 * @param sql sql脚本
	 * @param bpss 匿名函数，在其中设置传递的参数值
	 * @return 参照 {@link PreparedStatement#executeBatch()}的返回参数
	 * @throws SQLException 
	 */
	public static int[] batchUpdate(Connection con, String sql, BatchPreparedStatementSetter bpss) throws SQLException {
		logger.debug("Executing SQL batch update [" + sql + "]");
		
		PreparedStatement pst = null;
		try{
			pst = con.prepareStatement(sql);
			int count = bpss.getBatchSize();
			for(int i = 0; i < count; i++){
				bpss.setValues(pst, i);
				pst.addBatch();
			}
			return pst.executeBatch();
		}catch(SQLException e){
			logger.error("批量更新sql出错，sql语句是:" + sql, e);
			throw e;
		}finally{
			closeStatement(pst);
		}
	}
	
	/**
	 * 批量更新数据库
	 * @param ds 数据源对象
	 * @param sql sql脚本
	 * @param bpss 匿名函数，在其中设置传递的参数值
	 * @return 参照 {@link PreparedStatement#executeBatch()}的返回参数
	 */
	public static int[] batchUpdate(DataSource ds, String sql, BatchPreparedStatementSetter bpss) {
		logger.debug("Executing SQL batch update [" + sql + "]");
		int[] result = null;
		Connection con = null;
		PreparedStatement pst = null;
		try{
			con = ds.getConnection();
			pst = con.prepareStatement(sql);
			int count = bpss.getBatchSize();
			for(int i = 0; i < count; i++){
				bpss.setValues(pst, i);
				pst.addBatch();
			}
			result = pst.executeBatch();
			con.commit();
			return result;
		}catch(SQLException e){
			logger.error("批量更新sql出错，sql语句是:" + sql, e);
			throw new DataAccessException(e);
		}finally{
			closeStatement(pst);
			closeConnection(con);
		}
	}
	
	/**
	 * 查询出单条记录，并将其转换为pojo对象
	 * 
	 * @param ds 数据库
	 * @param sql sql语句
	 * @param rowMapper 返回列映射器
	 * @param inParams 输入参数
	 * @return pojo对象, 如果没有查到则返回null。
	 */
	public static <T> T queryForObject(DataSource ds, String sql, RowMapper<T> rowMapper, Object... inParams){
		logger.debug("Query for object [" + sql + "]");
		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rst = null;
		try{
			con = ds.getConnection();
			pst = con.prepareStatement(sql);
			setParams(pst, inParams);
			rst = pst.executeQuery();
			if(rst.next()){
				return rowMapper.mapRow(rst, 1);
			}else{
				return null;
			}
		}catch(SQLException e){
			logger.error("查询sql出错，sql语句是:" + sql, e);
			throw new DataAccessException(e);
		}finally{
			safeClose(con, rst, pst);
		}
	}
	
	/**
	 * 查询出单条记录，并将其转换为pojo对象
	 * 
	 * @param con 数据库连接
	 * @param sql sql语句
	 * @param rowMapper 返回列映射器
	 * @param inParams 输入参数
	 * @return pojo对象, 如果没有查到则返回null。
	 */
	public static <T> T queryForObject(Connection con, String sql, RowMapper<T> rowMapper, Object... inParams){
		logger.debug("Query for object [" + sql + "]");
		PreparedStatement pst = null;
		ResultSet rst = null;
		try{
			pst = con.prepareStatement(sql);
			setParams(pst, inParams);
			rst = pst.executeQuery();
			if(rst.next()){
				return rowMapper.mapRow(rst, 1);
			}else{
				return null;
			}
		}catch(SQLException e){
			logger.error("查询sql出错，sql语句是:" + sql, e);
		}finally{
			closeResultSet(rst);
			closeStatement(pst);
		}
		return null;
	}
	
	/**
	 * 查询多条记录，并将其转换为pojo对象，存放在{@link List}中
	 * @param ds 数据库
	 * @param sql sql语句
	 * @param pss 参数设置器
	 * @param rowMapper 返回列映射器
	 * @return pojo对象列表，如果没有值，则返回空的列表
	 */
	public static <T> List<T> query(DataSource ds, String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper){
		List<T> result = new ArrayList<T>();
		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rst = null;
		try {
			con = ds.getConnection();
			pst = con.prepareStatement(sql);
			pss.setValues(pst);
			rst = pst.executeQuery();
			while(rst.next()){
				T t = rowMapper.mapRow(rst, 1);
				result.add(t);
			}
		}catch(SQLException e){
			throw new DataAccessException(e);
		}finally{
			safeClose(con, rst, pst);
		}
		return result;
	}
	
	public static <T> List<T> query(Connection con, String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper){
		List<T> result = new ArrayList<T>();
		PreparedStatement pst = null;
		ResultSet rst = null;
		try {
			pst = con.prepareStatement(sql);
			pss.setValues(pst);
			rst = pst.executeQuery();
			while(rst.next()){
				T t = rowMapper.mapRow(rst, 1);
				result.add(t);
			}
		}catch(SQLException e){
			throw new DataAccessException(e);
		}finally{
			closeResultSet(rst);
			closeStatement(pst);
		}
		return result;
	}
	
	public static <T> List<T> query(DataSource ds, String sql, RowMapper<T> rowMapper, PageInfo pageInfo, Object... inParams){
		List<T> result = new ArrayList<T>();
		PreparedStatement stmt = null;
		ResultSet rst = null;
		Connection con= null;
		try {
			String sqlPage = sql;
			if(pageInfo != null){
				 sqlPage += " limit "+pageInfo.getStart()+","+(pageInfo.getEnd()-pageInfo.getStart());
			}
			con = ds.getConnection();
			con.setAutoCommit(false);
			stmt = con.prepareStatement(sqlPage);
			setParams(stmt, inParams);
			rst = stmt.executeQuery();
			while(rst.next()){
				T t = rowMapper.mapRow(rst, 1);
				result.add(t);
			}
			
		} catch (SQLException e) {
			throw new DataAccessException(e);
		}
		finally
		{
			DatabaseUtil.safeClose(con, rst, stmt);
		}
		
		if(pageInfo != null){
			int count = getCount(ds,sql,inParams);
			pageInfo.setCount(count);
		}
		return result;
	}
	
	/**
	 * 插入新的记录
	 * @param ds 数据库
	 * @param sql sql语句
	 * @param pss 参数设置器
	 * @return 新增记录的标识
	 */
	public static <T> Long insert(DataSource ds, String sql, PreparedStatementSetter pss){
		Connection con = null;
		PreparedStatement pst = null;
		ResultSet rst = null;
		Long result = null;
		try {
			con = ds.getConnection();
			con.setAutoCommit(false);
			pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			pss.setValues(pst);
			pst.executeUpdate();
			rst = pst.getGeneratedKeys();    
			rst.next();
			result = rst.getLong(1);
			con.commit();
		}catch(SQLException e){
			logger.error("update sql出错，sql语句是:" + sql, e);
			safeRollback(con);
			throw new DataAccessException(e);
		}finally{
			safeClose(con, rst, pst);
		}
		return result;
	}
	
	/**
	 * 更新或删除记录
	 * @param ds 数据库
	 * @param sql sql语句
	 * @param pss 参数设置器
	 * @return 影响的行数
	 */
	public static <T> int update(DataSource ds, String sql, PreparedStatementSetter pss){
		Connection con = null;
		PreparedStatement pst = null;
		int result = 0;
		try {
			con = ds.getConnection();
			con.setAutoCommit(false);
			pst = con.prepareStatement(sql);
			pss.setValues(pst);
			result = pst.executeUpdate();
			con.commit();
		}catch(SQLException e){
			logger.error("update sql出错，sql语句是:" + sql, e);
			safeRollback(con);
			throw new DataAccessException(e);
		}finally{
			safeClose(con, pst);
		}
		return result;
	}
}
