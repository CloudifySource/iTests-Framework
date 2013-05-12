package iTests.framework.utils;

import org.openspaces.admin.gsa.GridServiceOptions;
import org.openspaces.admin.machine.Machine;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.*;


public class DBUtils {

    public static final String MY_SQL_HOST = "192.168.9.47";
	public static final String MY_SQL_PORT = "3306";

	/**
     * Loads 1 HSQL on the specified machine listening on the specified port.
     */
    public static int loadHSQLDB(Machine machine, String dbName, long port) {
        return loadHSQLDB(machine, dbName, port,null);
    }
    
    /**
     * Loads 1 HSQL on the specified machine listening on the specified port.
     * places the database file in the specified directory.
     * Use different directories to isolate tests.
     * @param machine - the machine to start the database
     * @param dbName - the database name
     * @param port - the database port
     * @param workDirectory - the directory to read/write the database file.
     * @return the database process id.
     */
    public static int loadHSQLDB(Machine machine, String dbName, long port, File workDirectory) {
    	String dbFilename = dbName;
    	if (workDirectory != null) {
    		dbFilename = "file:"+new File(workDirectory,dbName).getAbsolutePath();
    	}
        System.out.println("DEBUG: machine = "+machine.getHostName());
        System.out.println("DEBUG: dbFilename = "+dbFilename);
        System.out.println("DEBUG: port = "+port);
    	return machine.getGridServiceAgents().waitForAtLeastOne().startGridService(
    					new GridServiceOptions("hsqldb")
    						.argument("-database").argument(dbFilename)
    						.argument("-port").argument(String.valueOf(port)));
    }

    /**
     * return an available port at given machine
     */
    public static int getAvailablePort(Machine machine) {
        for (int i = 9000; i < 10000; i++) {
            try {
                ServerSocket srv = new ServerSocket(i);
                srv.close();
                srv = null;
                return i;
            } catch (IOException e) {
            }
        }
        throw new RuntimeException("Cant find open port at machine " + machine.getHostName());
    }

    /**
     * return an available port as java.lang.String at given machine
     */
    public static String getAvailablePortAsString(Machine machine) {
        return String.valueOf(getAvailablePort(machine));
    }

    public static ResultSet runSQLQuery(String sql, String ip, int port, String username, String passward) {
        try {
            Class.forName("org.hsqldb.jdbcDriver").newInstance();
            String url = "jdbc:hsqldb:hsql://" + ip + ":" + port;
            Connection connection = DriverManager.getConnection(url, username, passward);
            Statement statement = connection.createStatement();
            return statement.executeQuery(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute query:" + sql, e);
        }
    }

    public static ResultSet runMYSQLQuery(String sql, String ip, int port, String username, String passward) {
    	try {
    		Class.forName("com.mysql.jdbc.Driver").newInstance();
    		String url = "jdbc:mysql://" + ip + ":" + port;
    		Connection connection = DriverManager.getConnection(url, username, passward);
    		Statement statement = connection.createStatement();
    		return statement.executeQuery(sql);
    	} catch (Exception e) {
    		throw new RuntimeException("Failed to execute query:" + sql, e);
    	}
    }
    
    public static boolean runMYSQLExecute(String sql, String ip, int port, String username, String passward) {
    	try {
    		LogUtils.log("establishing connection to data base with details: [ip]=" + ip + "|[username]=" + username + "|[password]=" + passward);
    		Class.forName("com.mysql.jdbc.Driver").newInstance();
    		String url = "jdbc:mysql://" + ip + ":" + port;
    		Connection connection = DriverManager.getConnection(url, username, passward);
    		Statement statement = connection.createStatement();
    		return statement.execute(sql);
    	} catch (Exception e) {
    		throw new RuntimeException("Failed to execute query:" + sql, e);
    	}
    }
    
    public static Connection getMySQLConnection(String ip, int port, String username, String passward){
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			String url = "jdbc:mysql://" + ip + ":" + port;
			return DriverManager.getConnection(url, username, passward);
		} catch (Exception e) {
			
			throw new RuntimeException("failed to get MySQL connecteion", e);
		}
    }
    
    public static Connection getMySQLConnection(String ip, int port){
    	return getMySQLConnection(ip, port, "sa", null);
    }
    
    public static ResultSet runMYSQLQuery(String sql, String ip, int port){
        return runMYSQLQuery(sql, ip, port, "sa", null);
    }

    public static boolean runMYSQLExecute(String sql, String ip, int port){
    	return runMYSQLExecute(sql, ip, port, "sa", null);
    }
    
    public static ResultSet runMYSQLQuery(String sql, Connection connection){
        try {
			return connection.createStatement().executeQuery(sql);
		} catch (SQLException e) {
			throw new RuntimeException("failed to run MySQL query", e);
		}
    }

    public static boolean runMYSQLExecute(String sql, Connection connection){
    	try {
			return connection.createStatement().execute(sql);
		} catch (SQLException e) {
			throw new RuntimeException("failed to run MySQL query", e);
		}
    }
    
    public static ResultSet runSQLQuery(String sql, String ip, int port){
          return runSQLQuery(sql, ip, port, "sa", null);
    }
}
