package org.goobi.managedbeans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.apache.deltaspike.core.api.scope.WindowScoped;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.statistics.enums.CalculationUnit;
import org.goobi.production.flow.statistics.enums.TimeUnit;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.IPushPlugin;
import org.goobi.production.plugin.interfaces.IStatisticPlugin;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

import lombok.Getter;
import lombok.Setter;

@Named("StatisticalQuestionBean")
@WindowScoped
public class StatisticalQuestionBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -5418439937138681611L;

    @Getter
    private List<String> possiblePluginNames;

    @Getter
    private String currentPluginName = "";

    @Getter
    @Setter
    private IStatisticPlugin currentPlugin;

    @Getter
    @Setter
    private Date sourceDateFrom;
    @Getter
    @Setter
    private Date sourceDateTo = new Date();

    private Date calculatedStartDate = new Date();
    private Date calculatedEndDate = new Date();

    @Getter
    @Setter
    protected Date timeFilterFrom;
    @Getter
    @Setter
    protected Date timeFilterTo;

    @Getter
    @Setter
    private TimeUnit sourceTimeUnit;
    private int sourceNumberOfTimeUnits = 0;

    @Inject
    @Push
    PushContext statisticsPluginPush;

    public StatisticalQuestionBean() {
        possiblePluginNames = PluginLoader.getListOfPlugins(PluginType.Statistics);
        Collections.sort(possiblePluginNames);
    }

    public String setStatisticalQuestion(String pluginName) {
        currentPluginName = pluginName;
        currentPlugin = (IStatisticPlugin) PluginLoader.getPluginByTitle(PluginType.Statistics, currentPluginName);
        if (currentPlugin instanceof IPushPlugin) {
            ((IPushPlugin) currentPlugin).setPushContext(statisticsPluginPush);
        }
        currentPlugin.setFilter("");
        return "statistics";
    }

    /**
     * Get all {@link TimeUnit} from enum
     * 
     * @return all timeUnits
     *************************************************************************************/
    public List<TimeUnit> getAllTimeUnits() {
        return Arrays.asList(TimeUnit.values());
    }

    public void calculate() {
        if (this.sourceNumberOfTimeUnits > 0) {
            setTimeFrameToStatisticalQuestion();
            currentPlugin.setStartDate(calculatedStartDate);
            currentPlugin.setEndDate(calculatedEndDate);
        } else {
            currentPlugin.setStartDate(timeFilterFrom);
            currentPlugin.setEndDate(timeFilterTo);
        }
        currentPlugin.calculate();
    }

    /**
     * Get all {@link CalculationUnit} from enum
     * 
     * @return all calculationUnit
     *************************************************************************************/
    public List<CalculationUnit> getAllCalculationUnits() {
        return Arrays.asList(CalculationUnit.values());
    }

    public String getData() {
        return currentPlugin.getData();
    }

    public void setData(String data) {
    }

    /**
     * @return the sourceNumberOfTimeUnitsAsString
     *************************************************************************************/
    public String getSourceNumberOfTimeUnitsAsString() {
        if (sourceNumberOfTimeUnits == 0) {
            return "";
        } else {
            return String.valueOf(sourceNumberOfTimeUnits);
        }
    }

    /**
     * @param inUnits the sourceNumberOfTimeUnits to set given as String to show an empty text field in gui
     *************************************************************************************/
    public void setSourceNumberOfTimeUnitsAsString(String inUnits) {
        if (StringUtils.isNotBlank(inUnits) && StringUtils.isNumericSpace(inUnits)) {
            sourceNumberOfTimeUnits = Integer.parseInt(inUnits);
        } else {
            sourceNumberOfTimeUnits = 0;
        }
    }

    private void setTimeFrameToStatisticalQuestion() {
        /* only add a date, if correct interface is implemented here */

        /* if a timeunit is selected calculate the dates */
        Calendar cl = Calendar.getInstance();

        if (sourceNumberOfTimeUnits > 0) {

            cl.setFirstDayOfWeek(Calendar.MONDAY);
            cl.setTime(new Date());

            switch (sourceTimeUnit) {
                case days:
                    calculatedEndDate = calulateStartDateForTimeFrame(cl);
                    calculatedStartDate = calculateEndDateForTimeFrame(cl, Calendar.DATE, -1);
                    break;

                case weeks:
                    cl.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    calculatedEndDate = calulateStartDateForTimeFrame(cl);
                    calculatedStartDate = calculateEndDateForTimeFrame(cl, Calendar.DATE, -7);
                    break;

                case months:
                    cl.set(Calendar.DAY_OF_MONTH, 1);
                    calculatedEndDate = calulateStartDateForTimeFrame(cl);
                    calculatedStartDate = calculateEndDateForTimeFrame(cl, Calendar.MONTH, -1);
                    break;

                case quarters:
                    cl.set(Calendar.DAY_OF_MONTH, 1);
                    setCalendarToMidnight(cl);
                    while ((cl.get(Calendar.MONTH) + 0) % 3 > 0) {
                        cl.add(Calendar.MONTH, -1);
                    }
                    cl.add(Calendar.MILLISECOND, -1);
                    calculatedEndDate = cl.getTime();
                    calculatedStartDate = calculateEndDateForTimeFrame(cl, Calendar.MONTH, -3);
                    break;

                case years:
                    cl.set(Calendar.DAY_OF_YEAR, 1);
                    calculatedEndDate = calulateStartDateForTimeFrame(cl);
                    calculatedStartDate = calculateEndDateForTimeFrame(cl, Calendar.YEAR, -1);
                    break;
                default:
                    break;
            }

        } else {
            /* take start and end date */
            cl.setTime(sourceDateTo);
            cl.add(Calendar.DATE, 1);
            setCalendarToMidnight(cl);
            cl.add(Calendar.MILLISECOND, -1);
            calculatedStartDate = sourceDateFrom;
            calculatedEndDate = cl.getTime();
        }

    }

    private void setCalendarToMidnight(Calendar cl) {
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
    }

    private Date calculateEndDateForTimeFrame(Calendar cl, int calenderUnit, int faktor) {
        cl.add(Calendar.MILLISECOND, 1);
        cl.add(calenderUnit, sourceNumberOfTimeUnits * faktor);
        return cl.getTime();
    }

    private Date calulateStartDateForTimeFrame(Calendar cl) {
        setCalendarToMidnight(cl);
        cl.add(Calendar.MILLISECOND, -1);
        return cl.getTime();
    }

}
