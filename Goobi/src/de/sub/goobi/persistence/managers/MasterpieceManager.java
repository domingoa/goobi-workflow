package de.sub.goobi.persistence.managers;
/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *          - http://www.intranda.com
 *          - http://digiverso.com 
 *          - http://www.goobi.org
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
 */
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.goobi.beans.Masterpiece;

public class MasterpieceManager implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -8123847555204812322L;
    private static final Logger logger = Logger.getLogger(MasterpieceManager.class);

    public static List<Masterpiece> getMasterpiecesForProcess(int processId) {
        List<Masterpiece> list = new ArrayList<Masterpiece>();
        try {
            list = MasterpieceMysqlHelper.getMasterpiecesForProcess(processId);
        } catch (SQLException e) {
            logger.error(e);
        }

        return list;
    }

    public static Masterpiece getMasterpieceForTemplateID(int id) {
        try {
            return MasterpieceMysqlHelper.getMasterpieceForTemplateID(id);
        } catch (SQLException e) {
            logger.error(e);
        }
        return null;
    }

    public static int countMasterpieces() {
        try {
            return MasterpieceMysqlHelper.countMasterpieces();
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }

    public static void saveMasterpiece(Masterpiece object) {

        try {
            MasterpieceMysqlHelper.saveMasterpiece(object);
        } catch (SQLException e) {
            logger.error(e);
        }
    }

    public static void deleteMasterpiece(Masterpiece object) {
        try {
            MasterpieceMysqlHelper.deleteMasterpiece(object);
        } catch (SQLException e) {
            logger.error(e);
        }

    }

}
