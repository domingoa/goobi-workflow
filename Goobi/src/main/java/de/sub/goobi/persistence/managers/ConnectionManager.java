package de.sub.goobi.persistence.managers;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *     		- https://goobi.io
 * 			- https://www.intranda.com
 * 			- https://github.com/intranda/goobi-workflow
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import lombok.extern.log4j.Log4j2;

/**
 * 
 * @author Robert Sehr
 * 
 */
@Log4j2
public class ConnectionManager {

    private DataSource ds = null;
    @SuppressWarnings("rawtypes")
    private static GenericObjectPool _pool = null;

    /**
     * @param config configuration from an XML file.
     */
    public ConnectionManager() {
        try {
            //			connectToDB(config);
            connectToDB();
        } catch (Exception e) {
            log.error("Failed to construct ConnectionManager", e);
        }
    }

    private void connectToDB() {
        if (log.isTraceEnabled()) {
            log.trace("Trying to connect to database...");
        }
        try {
            Context ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:comp/env/goobi");
            if (log.isTraceEnabled()) {
                log.trace("Connection attempt to database succeeded.");
            }
        } catch (Exception e) {
            log.error("Error when attempting to connect to DB ", e);
        }
    }

    @SuppressWarnings("rawtypes")
    public static void printDriverStats() throws Exception {
        ObjectPool connectionPool = ConnectionManager._pool;
        if (log.isTraceEnabled()) {
            log.trace("NumActive: " + connectionPool.getNumActive());
            log.trace("NumIdle: " + connectionPool.getNumIdle());
        }
    }

    /**
     * getNumLockedProcesses - gets the number of currently locked processes on the MySQL db
     * 
     * @return Number of locked processes
     */
    public int getNumLockedProcesses() {
        int num_locked_connections = 0;
        Connection con = null;
        PreparedStatement p_stmt = null;
        ResultSet rs = null;
        try {
            con = this.ds.getConnection();
            p_stmt = con.prepareStatement("SHOW PROCESSLIST");
            rs = p_stmt.executeQuery();
            while (rs.next()) {
                if (rs.getString("State") != null && rs.getString("State").equals("Locked")) {
                    num_locked_connections++;
                }
            }
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("Failed to get get Locked Connections - Exception: " + e.toString());
            }
        } finally {
            try {
                rs.close();
                p_stmt.close();
                con.close();
            } catch (java.sql.SQLException ex) {
                log.error(ex.toString());
            }
        }
        return num_locked_connections;
    }

    public DataSource getDataSource() {
        return this.ds;
    }

}
