package de.sub.goobi.persistence.managers;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.goobi.beans.DatabaseObject;
import org.goobi.beans.Process;

import de.sub.goobi.helper.exceptions.DAOException;

public class ProcessManager implements IManager, Serializable {

    private static final long serialVersionUID = 3898081063234221110L;

    private static final Logger logger = Logger.getLogger(ProcessManager.class);

    @Override
    public int getHitSize(String order, String filter) throws DAOException {
        try {
            return ProcessMysqlHelper.getProcessCount(order, filter);
        } catch (SQLException e) {
            logger.error(e);
            return 0;
        }
    }

    @Override
    public List<? extends DatabaseObject> getList(String order, String filter, Integer start, Integer count) {
        return (List<? extends DatabaseObject>) getProcesses(order, filter, start, count);
    }

    public static List<Process> getProcesses(String order, String filter, Integer start, Integer count) {
        List<Process> answer = new ArrayList<Process>();
        try {
            answer = ProcessMysqlHelper.getProcesses(order, filter, start, count);
        } catch (SQLException e) {
            logger.error("error while getting process list", e);
        }
        return answer;
    }
    
    public static List<Process> getProcesses(String order, String filter) {
        return getProcesses(order, filter, 0, Integer.MAX_VALUE);
    }

    public static Process getProcessById(int id) {
        Process p = null;
        try {
            p = ProcessMysqlHelper.getProcessById(id);
        } catch (SQLException e) {
            logger.error(e);
        }

        return p;
    }

    public static void saveProcess(Process o) throws DAOException {
        ProcessMysqlHelper.saveProcess(o, false);

    }

    public static void saveProcessInformation(Process o) {
        try {
            ProcessMysqlHelper.saveProcess(o, true);
        } catch (DAOException e) {
            logger.error(e);
        }
    }

    public static void deleteProcess(Process o) {
        try {
            ProcessMysqlHelper.deleteProcess(o);
        } catch (SQLException e) {
            logger.error(e);
        }
    }

    public static List<Process> getAllProcesses() {
        List<Process> answer = new ArrayList<Process>();
        try {
            answer = ProcessMysqlHelper.getAllProcesses();
        } catch (SQLException e) {
            logger.error("error while getting process list", e);
        }
        return answer;
    }

    public static void updateBatchList(List<Process> processList) {
        try {
            ProcessMysqlHelper.updateBatchList(processList);
        } catch (SQLException e) {
            logger.error(e);
        }
    }

    public static void insertBatchProcessList(List<Process> processList) {
        try {
            ProcessMysqlHelper.insertBatchProcessList(processList);
        } catch (SQLException e) {
            logger.error(e);
        }
    }

    public static int countProcessTitle(String title) {
        try {
            return ProcessMysqlHelper.getProcessCount(null, " title = '" + StringEscapeUtils.escapeSql(title) + "'");
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }

    public static int getMaxBatchNumber() {

        try {
            return ProcessMysqlHelper.getMaxBatchNumber();
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }

    public static int countProcesses(String filter) {
        try {
            return ProcessMysqlHelper.countProcesses(filter);
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }

    public static List<Integer> getIDList(String filter) {

        try {
            return ProcessMysqlHelper.getIDList(filter);
        } catch (SQLException e) {
            logger.error(e);
        }
        return new ArrayList<Integer>();
    }

    public static List<Integer> getBatchIds(int limit) {
        
        try {
            return ProcessMysqlHelper.getBatchIds(limit);
        } catch (SQLException e) {
            logger.error(e);
        }
        return new ArrayList<Integer>();
    }
    
    public static List runSQL(String sql) {
        
        try {
            return ProcessMysqlHelper.runSQL(sql);
        } catch (SQLException e) {
            logger.error(e);
        }
        return new ArrayList();
    }
}
