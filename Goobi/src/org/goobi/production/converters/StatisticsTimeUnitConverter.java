package org.goobi.production.converters;
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
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.goobi.production.flow.statistics.enums.TimeUnit;

/**
 * TimeUnitConverter for statistics TimeUnits as select-items in jsf-guis
 * 
 * @author Steffen Hankiewicz
 * @version 21.05.2009
 **************************************************************************************/
public class StatisticsTimeUnitConverter implements Converter {
	public static final String CONVERTER_ID = "StatisticsTimeUnitConverter";

	/**
	 * convert String to TimeUnit 
	 **************************************************************************************/
	public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
		if (value==null){
			return TimeUnit.days;
		}else {
			return TimeUnit.getById(value);
		}
	}
	
	/**
	 * convert TimeUnit to String
	 **************************************************************************************/
	public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
		if (value == null || !(value instanceof TimeUnit)) {
			return TimeUnit.days.getId();
		} else{
			return ((TimeUnit) value).getId();
		}
	}

}
