/**
 *
 * Energy Cost Calculator
 *
 * Copyright 2022 Ryan Elliott
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * v0.1		RLE		Creation
 * v0.2		RLE		Substantial updates to the UI along with functionality. 
 * v1.0		RLE		Further UI overhauls. Added rate scheduling.
 */
 
definition(
    name: "Energy Cost Calculator",
    namespace: "rle",
    author: "Ryan Elliott",
    description: "Creates a table to track the cost of energy meters.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
	page(name: "mainPage")
	page(name: "pageSelectVariables")
	page(name: "pageSetRateSchedule")
}

def mainPage() {
    
	if(state.energies == null) state.energies = [:]
	if(state.energiesList == null) state.energiesList = []
	if(!state.energyRate) state.energyRate = 1
	dynamicPage(name: "mainPage", uninstall: true, install: true) {
		section(getFormat("header","<b>App Name</b>"),hideable: true, hidden: true) {
            label title: getFormat("important","<b>Enter a name for this app.</b>"), required:true, width: 4, submitOnChange: true
        }

		section(getFormat("header","<b>Device Selection</b>"),hideable: true, hidden: true) {
			input "energies", "capability.energyMeter", title: getFormat("important","Select Energy Devices to Measure Cost"), multiple: true, submitOnChange: true, width: 4
			
		}
		if(app.getInstallationState() == "COMPLETE") {
			section{
				href(name: "hrefSetRateSchedule", title: getFormat("important","Click here to update the rate information"),description: "", page: "pageSetRateSchedule", width:4,newLineAfter:true)
				href(name: "hrefSelectVariables", title: getFormat("important","Click here to add and/or remove variable links"),description: "", page: "pageSelectVariables", width:4)
			}

			section{
				energies.each {dev ->
					if(!state.energies["$dev.id"]) {
						state.energies["$dev.id"] = [todayEnergy: 0, dayStart: dev.currentEnergy, lastEnergy: dev.currentEnergy, var: "",thisWeekEnergy: 0,thisMonthEnergy: 0,lastWeekEnergy: 0,lastMonthEnergy: 0,todayCost: 0, thisWeekCost: 0, thisMonthCost: 0]
						state.energiesList += dev.id
					}
				}
				if(energies) {
					if(energies.id.sort() != state.energiesList.sort()) { //something was removed
						state.energiesList = energies.id
						Map newState = [:]
						energies.each{d ->  newState["$d.id"] = state.energies["$d.id"]}
						state.energies = newState
					}
					updated()
					if(energyRate) {
						String pattern = /(\d*[0-9]\d*(\.\d+)?|0*\.\d*[0-9]\d*)/
						java.util.regex.Matcher matching = energyRate =~ pattern
						BigDecimal energyRate = matching[0][1].toBigDecimal()
						state.energyRate = energyRate
						app.removeSetting("energyRate")
					}
					newEnergy = state.energyRate
					if(symbol) {rateDisplayFormat = symbol+newEnergy} else {rateDisplayFormat = newEnergy}
					paragraph getFormat("rateDisplay","Current Pricing is ${rateDisplayFormat} per KWH")
					paragraph displayTable()
					input "refresh", "button", title: "Refresh Table", width: 2
					paragraph ""
					input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: false, displayDuringSetup: false, required: false, width: 2
					input "traceOutput", "bool", title: "Enable trace logging?", defaultValue: false, displayDuringSetup: false, required: false, width: 2
				}
			}
		} else {
			section(getFormat("header","<b>CLICK DONE TO INSTALL APP AFTER SELECTING DEVICES</b>")) {
				paragraph ""
			}
		}
	}
}

def pageSelectVariables() {
	logTrace "Loading variable table..."
	dynamicPage(name: "pageSelectVariables", uninstall: false, install: false, nextPage: "mainPage") {
		section(getFormat("header","<b>Link the Cost Value to a Hub Variable</b>")) {
			if(energies)
					paragraph getFormat("important","<b>The selected variable MUST be of type \"String\"</b>")
					paragraph displayVariableTable()
			if(state.newTodayVar) {
				logTrace "newTodayVar is ${state.newTodayVar}"
				List vars = getAllGlobalVars().findAll{it.value.type == "string"}.keySet().collect().sort{it.capitalize()}
				input "newVar", "enum", title: "Select Variable", submitOnChange: true, width: 4, options: vars, newLineAfter: true
				if(newVar) {
					addInUseGlobalVar(newVar)
					state.energies[state.newTodayVar].todayVar = newVar
					state.remove("newTodayVar")
					app.removeSetting("newVar")
					paragraph "<script>{changeSubmit(this)}</script>"
				}
			} else if(state.remTodayVar) {
				removeInUseGlobalVar(state.energies[state.remTodayVar].todayVar)
				state.energies[state.remTodayVar].todayVar = ""
				state.remove("remTodayVar")
				paragraph "<script>{changeSubmit(this)}</script>"
			} else if(state.newWeekVar) {
				logTrace "newWeekVar is ${state.newWeekVar}"
				List vars = getAllGlobalVars().findAll{it.value.type == "string"}.keySet().collect().sort{it.capitalize()}
				input "newVar", "enum", title: "Select Variable", submitOnChange: true, width: 4, options: vars, newLineAfter: true
				if(newVar) {
					addInUseGlobalVar(newVar)
					state.energies[state.newWeekVar].weekVar = newVar
					state.remove("newWeekVar")
					app.removeSetting("newVar")
					paragraph "<script>{changeSubmit(this)}</script>"
				}
			} else if(state.remWeekVar) {
				removeInUseGlobalVar(state.energies[state.remWeekVar].weekVar)
				state.energies[state.remWeekVar].weekVar = ""
				state.remove("remWeekVar")
				paragraph "<script>{changeSubmit(this)}</script>"
			} else if(state.newMonthVar) {
				logTrace "newMonthVar is ${state.newMonthVar}"
				List vars = getAllGlobalVars().findAll{it.value.type == "string"}.keySet().collect().sort{it.capitalize()}
				input "newVar", "enum", title: "Select Variable", submitOnChange: true, width: 4, options: vars, newLineAfter: true
				if(newVar) {
					addInUseGlobalVar(newVar)
					state.energies[state.newMonthVar].monthVar = newVar
					state.remove("newMonthVar")
					app.removeSetting("newVar")
					paragraph "<script>{changeSubmit(this)}</script>"
				}
			} else if(state.remMonthVar) {
				removeInUseGlobalVar(state.energies[state.remMonthVar].monthVar)
				state.energies[state.remMonthVar].monthVar = ""
				state.remove("remMonthVar")
				paragraph "<script>{changeSubmit(this)}</script>"
			} else if(state.newTodayTotalVar) {
				List vars = getAllGlobalVars().findAll{it.value.type == "string"}.keySet().collect().sort{it.capitalize()}
				input "newVar", "enum", title: "Select Variable", submitOnChange: true, width: 4, options: vars, newLineAfter: true
				if(newVar) {
					addInUseGlobalVar(newVar)
					state.todayTotalVar = newVar
					state.remove("newTodayTotalVar")
					app.removeSetting("newVar")
					paragraph "<script>{changeSubmit(this)}</script>"
				}
			} else if(state.remTodayTotalVar) {
				removeInUseGlobalVar(state.todayTotalVar)
				state.todayTotalVar = ""
				state.remove("remTodayTotalVar")
				paragraph "<script>{changeSubmit(this)}</script>"
			} else if(state.newWeekTotalVar) {
				List vars = getAllGlobalVars().findAll{it.value.type == "string"}.keySet().collect().sort{it.capitalize()}
				input "newVar", "enum", title: "Select Variable", submitOnChange: true, width: 4, options: vars, newLineAfter: true
				if(newVar) {
					addInUseGlobalVar(newVar)
					state.weekTotalVar = newVar
					state.remove("newWeekTotalVar")
					app.removeSetting("newVar")
					paragraph "<script>{changeSubmit(this)}</script>"
				}
			} else if(state.remWeekTotalVar) {
				removeInUseGlobalVar(state.weekTotalVar)
				state.weekTotalVar = ""
				state.remove("remWeekTotalVar")
				paragraph "<script>{changeSubmit(this)}</script>"
			} else if(state.newMonthTotalVar) {
				List vars = getAllGlobalVars().findAll{it.value.type == "string"}.keySet().collect().sort{it.capitalize()}
				input "newVar", "enum", title: "Select Variable", submitOnChange: true, width: 4, options: vars, newLineAfter: true
				if(newVar) {
					addInUseGlobalVar(newVar)
					state.monthTotalVar = newVar
					state.remove("newMonthTotalVar")
					app.removeSetting("newVar")
					paragraph "<script>{changeSubmit(this)}</script>"
				}
			} else if(state.remMonthTotalVar) {
				removeInUseGlobalVar(state.monthTotalVar)
				state.monthTotalVar = ""
				state.remove("remMonthTotalVar")
				paragraph "<script>{changeSubmit(this)}</script>"
			}
		}
	}
}

def pageSetRateSchedule() {
	dynamicPage(name: "pageSetRateSchedule",title:getFormat("header","<b>Set a Rate Schedule or Static Rate</b>"), uninstall: false, install: false, nextPage: "mainPage") {
		section{
			input "scheduleType", "bool", title: getFormat("important","Disabled: Use a static rate</br>Enabled: Use a rate schedule"), defaultValue: false, displayDuringSetup: false, required: false, width: 4, submitOnChange: true
			}
		if(scheduleType) {
			if(state.schedules == null) state.schedules = [:]
			if(state.schedulesList == null) state.schedulesList = []
			section(getFormat("header","<b>Set a dynamic rate schedule below</b>")){
				paragraph displayRateTable()
				logDebug "Schedules are ${state.schedules}"
				if(state.addNewRateSchedule) {
					input "newSchedule", "string", title: "What is the schedule name?", required: false, width: 4, submitOnChange: true, newLineAfter: true
					if(newSchedule) {
						if(!state.schedules["$newSchedule"]) {state.schedules["$newSchedule"] = [rateDayOfWeek: "",rateTimeOfDay: "", rateMonth:"",rateCost:""]}
						state.schedulesList.add(newSchedule)
						state.remove("addNewRateSchedule")
						app.removeSetting("newSchedule")
						logDebug "Schedules are ${state.schedules}"
						paragraph "<script>{changeSubmit(this)}</script>"
					}
				} else if(state.delRateSchedule) {
					input "delSchedule", "enum", title: "Which schedule should be removed?", options: state.schedulesList, submitOnChange: true, newLineAfter: true, width: 4
					if(delSchedule) {
						state.schedulesList.remove(delSchedule)
						state.schedules.remove(delSchedule)
						state.remove("delRateSchedule")
						app.removeSetting("delSchedule")
						paragraph "<script>{changeSubmit(this)}</script>"
					}
				} else if(state.newRateDay) {
					logTrace "newRateDay is ${state.newRateDay}"
					input "rateDayOfWeek", "enum", title: "What days of the week?", options: ["ALL","SUN","MON","TUE","WED","THU","FRI","SAT"], multiple:true, width: 4, submitOnChange: true, newLineAfter: true
					if(rateDayOfWeek) {
						if(rateDayOfWeek.contains("ALL")) rateDayOfWeek = "ALL"
						state.schedules[state.newRateDay].rateDayOfWeek = rateDayOfWeek
						state.remove("newRateDay")
						app.removeSetting("rateDayOfWeek")
						paragraph "<script>{changeSubmit(this)}</script>"
					}
				} else if(state.remRateDay) {
					state.schedules[state.remRateDay].rateDayOfWeek = ""
					state.remove("remRateDay")
					paragraph "<script>{changeSubmit(this)}</script>"
				} else if(state.newRateTime) {
					logTrace "newRateTime is ${state.newRateTime}"
					input "rateTimeOfDay", "enum", title: "What is the start time?", options: [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23], width: 4, submitOnChange: true, newLineAfter: true
					if(rateTimeOfDay) {
						state.schedules[state.newRateTime].rateTimeOfDay = rateTimeOfDay
						state.remove("newRateTime")
						app.removeSetting("rateTimeOfDay")
						paragraph "<script>{changeSubmit(this)}</script>"
					}
				} else if(state.remRateTime) {
					state.schedules[state.remRateTime].rateTimeOfDay = ""
					state.remove("remRateTime")
					paragraph "<script>{changeSubmit(this)}</script>"
				} else if(state.newRateMonths) {
					logTrace "newRateMonths is ${state.newRateMonths}"
					input "rateMonth", "enum", title: "Which months?", options: ["ALL","JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"], multiple:true, width: 4, submitOnChange: true, newLineAfter: true
					if(rateMonth) {
						if(rateMonth.contains("ALL")) rateMonth = "ALL"
						state.schedules[state.newRateMonths].rateMonth = rateMonth
						state.remove("newRateMonths")
						app.removeSetting("rateMonth")
						paragraph "<script>{changeSubmit(this)}</script>"
					}
				} else if(state.remRateMonths) {
					state.schedules[state.remRateMonths].rateMonth = ""
					state.remove("remRateMonths")
					paragraph "<script>{changeSubmit(this)}</script>"
				} else if(state.addRateAmount) {
					logTrace "addRateAmount is ${state.addRateAmount}"
					input "rateCost", "string", title: "What is your energy rate per kWh?<br>", required: false, default:"1", width: 4, submitOnChange: true
					if(rateCost) {
						String pattern = /(\d*[0-9]\d*(\.\d+)?|0*\.\d*[0-9]\d*)/
						java.util.regex.Matcher matching = rateCost =~ pattern
						BigDecimal rateCost = matching[0][1].toBigDecimal()
						state.schedules[state.addRateAmount].rateCost = rateCost
						state.remove("addRateAmount")
						app.removeSetting("rateCost")
						paragraph "<script>{changeSubmit(this)}</script>"
					}
				} else if(state.remRateAmount) {
					state.schedules[state.remRateAmount].rateCost = ""
					state.remove("remRateAmount")
					paragraph "<script>{changeSubmit(this)}</script>"
				}
			}
			section(getFormat("important","<b>Manual Override</b>"),hideable:true,hidden:true) {
				input "energyRateOverride", "string", title: getFormat("important","Enter a rate here to manually override the current rate:"),required: false, width: 4, submitOnChange: true
				if(energyRateOverride) {
					String pattern = /(\d*[0-9]\d*(\.\d+)?|0*\.\d*[0-9]\d*)/
					java.util.regex.Matcher matching = energyRateOverride =~ pattern
					BigDecimal energyRateOverride = matching[0][1].toBigDecimal()
					log.warn "Manually overriding current rate of ${state.energyRate} with ${energyRateOverride}"
					state.energyRate = energyRateOverride
					app.removeSetting("energyRateOverride")
				}
			} 
		} else {
			section{
			input "energyRate", "string", title: getFormat("header","What is your energy rate per kWh?"), required: false, default: 1, width: 4, submitOnChange: true
			}
		}
		section(getFormat("important","<b>Set the Currency Symbol</b>"),hideable:true,hidden:true) {
			input "symbol", "string", title: getFormat("important","What is your currency symbol?"),required: false, width: 4, submitOnChange: true
		}
	}
}

String displayTable() {
	logDebug "Table display called"
	String str = "<script src='https://code.iconify.design/iconify-icon/1.0.0/iconify-icon.min.js'></script>"
	str += "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px }" +
		"</style><div style='overflow-x:auto'><table class='mdl-data-table tstat-col' style=';border:2px solid black'>" +
		"<thead><tr style='border-bottom:2px solid black'><th style='border-right:2px solid black'>Meter</th>" +
		"<th>Energy Use Today</th>" +
		"<th style='border-right:2px solid black'>Today's Cost</th>" +
		"<th>Energy Use This Week</th>" +
		"<th>Energy Cost This Week</th>" +
		"<th style='border-right:2px solid black'>Energy Use Last Week</th>" +
		"<th>Energy Use This Month</th>" +
		"<th>Energy Cost This Month</th>" +
		"<th>Energy Use Last Month</th></tr></thead>"
	updateDeviceEnergy()
	energies.sort{it.displayName.toLowerCase()}.each {dev ->
		device = state.energies["$dev.id"]

		//Get energy values for each device.
		todayEnergy = device.todayEnergy
		thisWeekEnergy = device.thisWeekEnergy
		thisMonthEnergy = device.thisMonthEnergy
		lastWeekEnergy = device.lastWeekEnergy ?: 0
		lastMonthEnergy = device.lastMonthEnergy ?: 0

		//Get cost values, round, and add symbol

		todayCost = device.todayCost.setScale(2, BigDecimal.ROUND_HALF_DOWN)
		thisWeekCost = device.thisWeekCost.setScale(2, BigDecimal.ROUND_HALF_DOWN)
		thisMonthCost = device.thisMonthCost.setScale(2, BigDecimal.ROUND_HALF_DOWN)

		if(symbol) {todayCost = symbol+todayCost.toString()}
		if(symbol) {thisWeekCost = symbol+thisWeekCost.toString()}
		if(symbol) {thisMonthCost = symbol+thisMonthCost.toString()}

		//Build display strings
		String devLink = "<a href='/device/edit/$dev.id' target='_blank' title='Open Device Page for $dev'>$dev"
		str += "<tr style='color:black'><td style='border-right:2px solid black'>$devLink</td>" +
			"<td style='color:#be05f5'><b>$todayEnergy</b></td>" +
			"<td style='border-right:2px solid black; color:#be05f5' title='Money spent running ${dev}'><b>$todayCost</b></td>" +
			"<td style='color:#007cbe'><b>$thisWeekEnergy</b></td>" +
			"<td title='Money spent running ${dev}' style='color:#007cbe'><b>$thisWeekCost</b></td>" +
			"<td style='border-right:2px solid black; color:#007cbe'><b>$lastWeekEnergy</b></td>" +
			"<td style='color:#5a8200'><b>$thisMonthEnergy</b></td>" +
			"<td title='Money spent running $dev' style='color:#5a8200'><b>$thisMonthCost</b></td>" +
			"<td style='color:#5a8200'><b>$lastMonthEnergy</b></td></tr>"
	}
	//Get total energy values
	todayTotalEnergy = state.todayTotalEnergy
	thisWeekTotal = state.thisWeekTotal
	thisMonthTotal = state.thisMonthTotal
	lastWeekTotal = state.lastWeekTotal ?: 0
	lastMonthTotal = state.lastMonthTotal ?: 0

	//Get cost values, round, and add symbol
	totalCostToday = state.totalCostToday.setScale(2, BigDecimal.ROUND_HALF_DOWN)
	totalCostWeek = state.totalCostWeek.setScale(2, BigDecimal.ROUND_HALF_DOWN)
	totalCostMonth = state.totalCostMonth.setScale(2, BigDecimal.ROUND_HALF_DOWN)

	if(symbol) {totalCostToday = symbol+totalCostToday.toString()}
	if(symbol) {totalCostWeek = symbol+totalCostWeek.toString()} 
	if(symbol) {totalCostMonth = symbol+totalCostMonth.toString()}

	//Build display string
    str += "<tr style='border-top:2px solid black'><td style='border-right:2px solid black'>Total</td>" +
			"<td style='color:#be05f5'><b>$todayTotalEnergy</b></td>" +
			"<td style='border-right:2px solid black; color:#be05f5' title='Money spent running $dev'><b>$totalCostToday</b></td>" +
			"<td style='color:#007cbe'><b>$thisWeekTotal</b></td>" +
			"<td title='Money spent running $dev' style='color:#007cbe'><b>$totalCostWeek</b></td>" +
			"<td style='border-right:2px solid black; color:#007cbe'><b>$lastWeekTotal</b></td>" +
			"<td style='color:#5a8200'><b>$thisMonthTotal</b></td>" +
			"<td title='Money spent running $dev' style='color:#5a8200'><b>$totalCostMonth</b></td>" +
			"<td style='color:#5a8200'><b>$lastMonthTotal</b></td></tr>"
	str += "</table></div>"
	str
}

String displayVariableTable() {
	logDebug "Variable table display called"
	String str = "<script src='https://code.iconify.design/iconify-icon/1.0.0/iconify-icon.min.js'></script>"
	str += "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px }" +
		"</style><div style='overflow-x:auto'><table class='mdl-data-table tstat-col' style=';border:2px solid black'>" +
		"<thead><tr style='border-bottom:2px solid black'><th style='border-right:2px solid black'>Meter</th>" +
		"<th>Today's Cost Variable</th>" +
		"<th>This Week Cost Variable</th>" +
		"<th>This Month Cost Variable</th></tr></thead>"
	energies.sort{it.displayName.toLowerCase()}.each {dev ->
		device = state.energies["$dev.id"]
		String todayVar = device.todayVar
		String weekVar = device.weekVar
		String monthVar = device.monthVar
		String devLink = "<a href='/device/edit/$dev.id' target='_blank' title='Open Device Page for $dev'>$dev"
		String todaysVar = todayVar ? buttonLink("noToday$dev.id", todayVar, "purple") : buttonLink("today$dev.id", "Select", "red")
		String weeksVar = weekVar ? buttonLink("noWeek$dev.id", weekVar, "purple") : buttonLink("week$dev.id", "Select", "red")
		String monthsVar = monthVar ? buttonLink("noMonth$dev.id", monthVar, "purple") : buttonLink("month$dev.id", "Select", "red")
		str += "<tr style='color:black'><td style='border-right:2px solid black'>$devLink</td>" +
		"<td title='${todayVar ? "Deselect $todayVar" : "Set a string hub variable to todays cost value"}'>$todaysVar</td>" +
		"<td title='${weekVar ? "Deselect $weekVar" : "Set a string hub variable to this weeks cost value"}'>$weeksVar</td>" +
		"<td title='${monthVar ? "Deselect $monthVar" : "Set a string hub variable to this months cost value"}'>$monthsVar</td></tr>"
	}
	String todayTotalVar = state.todayTotalVar
	String weekTotalVar = state.weekTotalVar
	String monthTotalVar = state.monthTotalVar
	String todaysTotalVar = todayTotalVar ? buttonLink("noVarTodayTotal", todayTotalVar, "purple") : buttonLink("varTodayTotal", "Select", "red")
	String weeksTotalVar = weekTotalVar ? buttonLink("noVarWeekTotal", weekTotalVar, "purple") : buttonLink("varWeekTotal", "Select", "red")
	String monthsTotalVar = monthTotalVar ? buttonLink("noVarMonthTotal", monthTotalVar, "purple") : buttonLink("varMonthTotal", "Select", "red")
	str += "<tr style='color:black'><td style='border-right:2px solid black'>Totals</td>" +
		"<td title='${todayTotalVar ? "Deselect $todayTotalVar" : "Set a string hub variable to todays cost value"}'>$todaysTotalVar</td>" +
		"<td title='${weekTotalVar ? "Deselect $weekTotalVar" : "Set a string hub variable to this weeks cost value"}'>$weeksTotalVar</td>" +
		"<td title='${monthTotalVar ? "Deselect $monthTotalVar" : "Set a string hub variable to this months cost value"}'>$monthsTotalVar</td></tr>"
	str += "</table></div>"
	str
}

String displayRateTable() {
    def schedules = state.schedulesList
    String str = "<script src='https://code.iconify.design/iconify-icon/1.0.0/iconify-icon.min.js'></script>"
	str += "<style>.mdl-data-table tbody tr:hover{background-color:inherit} .tstat-col td,.tstat-col th { padding:8px 8px;text-align:center;font-size:12px} .tstat-col td {font-size:15px }" +
		"</style><div style='overflow-x:auto'><table class='mdl-data-table tstat-col' style=';border:2px solid black'>" +
		"<thead><tr style='border-bottom:2px solid black'><th style='border-right:2px solid black'><b>Rate Schedule Name</b></th>" +
		"<th><b>Days of the Week</b></th>" +
		"<th><b>Start Time</b></th>" +
		"<th><b>Months</b></th>" +
		"<th><b>Rate Cost</b></th></tr></thead>"
	schedules.each {sched ->
		scheds = state.schedules["$sched"]
		String rateDow = scheds.rateDayOfWeek.toString().replace("[","").replace("]","").replace(" ","")
		String rateTod = scheds.rateTimeOfDay
		String rateMon = scheds.rateMonth.toString().replace("[","").replace("]","").replace(" ","")
		String rateC = scheds.rateCost
		String rateDay = rateDow ? buttonLink("noRateDay$sched", rateDow, "purple") : buttonLink("addRateDay$sched", "Select", "red")
		String rateTime = rateTod ? buttonLink("noRateTime$sched", rateTod, "purple") : buttonLink("addRateTime$sched", "Select", "red")
		String rateMonths = rateMon ? buttonLink("noRateMonths$sched", rateMon, "purple") : buttonLink("addRateMonths$sched", "Select", "red")
		String rateAmount = rateC ? buttonLink("noRateAmount$sched", rateC, "purple") : buttonLink("addRateAmount$sched", "Select", "red")
		str += "<tr style='color:#0000EE''><td style='border-right:2px solid black'>$sched</td>" +
            "<td title='${rateDow ? "Click to remove days of week" : "Click to set days of week"}'>$rateDay</td>" +
			"<td title='${rateTod ? "Click to remove start time" : "Click to set start time"}'>$rateTime</td>" +
			"<td title='${rateMon ? "Click to remove months list" : "Click to set months list"}'>$rateMonths</td>" + 
			"<td title='${rateC ? "Click to remove rate cost" : "Click to set rate cost"}'>$rateAmount</td></tr>"
	}
	str += "</table>"
	String addRateSchedule = buttonLink("createRateSchedule", "<iconify-icon icon='mdi:calendar-add'></iconify-icon>", "#660000", "25px")
	String remRateSchedule = buttonLink ("removeRateSchedule", "<iconify-icon icon='mdi:calendar-remove-outline'></iconify-icon", "#660000", "25px")
	str += "<table class='mdl-data-table tstat-col' style=';border:none'><thead><tr>" +
		"<th style='border-bottom:2px solid black;border-right:2px solid black;border-left:2px solid black;width:50px' title='Create a New Rate Schedule'>$addRateSchedule</th>" +
		"<th style='border-bottom:2px solid black;border-right:2px solid black;border-left:2px solid black;width:50px' title='Remove a Rate Schedule'>$remRateSchedule</th>" +
		"<th style='border:none;color:#660000;font-size:1.125rem'><b><i class='he-arrow-left2' style='vertical-align:middle'></i>Click here to add or remove a rate schedule</b></th>" +
		"</tr></thead></table>"
    str += "</div>"
    
    return str
}

String buttonLink(String btnName, String linkText, color = "#1A77C9", font = "15px") {
	"<div class='form-group'><input type='hidden' name='${btnName}.type' value='button'></div><div><div class='submitOnChange' onclick='buttonClick(this)' style='color:$color;cursor:pointer;font-size:$font'>$linkText</div></div><input type='hidden' name='settings[$btnName]' value=''>"
}

void appButtonHandler(btn) {
	logDebug "btn is ${btn}"
	if(btn == "varTodayTotal") state.newTodayTotalVar = btn
	else if(btn == "noVarTodayTotal") state.remTodayTotalVar = btn
    else if(btn == "varWeekTotal") state.newWeekTotalVar = btn
	else if(btn == "noVarWeekTotal") state.remWeekTotalVar = btn
    else if(btn == "varMonthTotal") state.newMonthTotalVar = btn
	else if(btn == "noVarMonthTotal") state.remMonthTotalVar = btn
	else if(btn.startsWith("today")) state.newTodayVar = btn.minus("today")
	else if(btn.startsWith("noToday")) state.remTodayVar = btn.minus("noToday")
    else if(btn.startsWith("week")) state.newWeekVar = btn.minus("week")
	else if(btn.startsWith("noWeek")) state.remWeekVar = btn.minus("noWeek")
    else if(btn.startsWith("month")) state.newMonthVar = btn.minus("month")
	else if(btn.startsWith("noMonth")) state.remMonthVar = btn.minus("noMonth")
	else if(btn == "createRateSchedule") state.addNewRateSchedule = btn
	else if(btn == "removeRateSchedule") state.delRateSchedule = btn
	else if(btn.startsWith("noRateDay")) state.remRateDay = btn.minus("noRateDay")
	else if(btn.startsWith("addRateDay")) state.newRateDay = btn.minus("addRateDay")
	else if(btn.startsWith("noRateMonths")) state.remRateMonths = btn.minus("noRateMonths")
	else if(btn.startsWith("addRateMonths")) state.newRateMonths = btn.minus("addRateMonths")
	else if(btn.startsWith("noRateTime")) state.remRateTime = btn.minus("noRateTime")
	else if(btn.startsWith("addRateTime")) state.newRateTime = btn.minus("addRateTime")
	else if(btn.startsWith("noRateAmount")) state.remRateAmount = btn.minus("noRateAmount")
	else if(btn.startsWith("addRateAmount")) state.addRateAmount = btn.minus("addRateAmount")
}

void updated() {
	logTrace "Updated app"
	unsubscribe()
	unschedule()
	initialize()
}

void installed() {
	log.warn "Installed app"
	state.onceReset = 2
	initialize()
}

void uninstalled() {
	log.warn "Uninstalling app"
	removeAllInUseGlobalVar()
}

void initialize() {
	logTrace "Initialized app"
	if(scheduleType) rateScheduler()
	schedule("11 0 0 * * ?",resetDaily)
	schedule("7 0 0 ? * SUN *",resetWeekly)
	schedule("8 0 0 1 * ? *",resetMonthly)
	subscribe(energies, "energy", energyHandler)
	resetForTest()
}

void energyHandler(evt) {
    logDebug "Energy change for ${evt.device}"
	updateDeviceEnergy()
}

void updateDeviceEnergy() {
	logDebug "Start energy update"
	def todayTotalEnergy = 0
	def thisWeekTotal = 0
	def thisMonthTotal = 0
	energies.each {dev ->
		device = state.energies["$dev.id"]
		currentEnergy = dev.currentEnergy ?: 0
		lastEnergy = device.lastEnergy ?: currentEnergy
		if (lastEnergy != currentEnergy) {
			logTrace "${dev} changed from ${lastEnergy} to ${currentEnergy}"
		}
		energyChange = currentEnergy - lastEnergy
		if(energyChange != 0) logTrace "Energy change for ${dev} is ${energyChange}"
		String thisVar = device.var
		BigDecimal start = device.dayStart ?: 0
		BigDecimal thisWeek = device.thisWeekEnergy ?: 0
		BigDecimal thisWeekStart = device.weekStart ?: 0
		BigDecimal thisMonth = device.thisMonthEnergy ?: 0
		BigDecimal thisMonthStart = device.monthStart ?: 0
		energyCheck = currentEnergy - start
		if(energyCheck < 0) {
			log.info "Energy for ${dev} is less than day start; energy was reset; setting day start to 0"
			device.dayStart = 0
			todayEnergy = currentEnergy - device.dayStart
		} else {todayEnergy = energyCheck}
		logTrace "${dev} energy today is ${todayEnergy}"
		device.lastEnergy = currentEnergy
		device.energyChange = energyChange
		device.todayEnergy = todayEnergy
		todayTotalEnergy = todayTotalEnergy + todayEnergy
		thisWeek = thisWeekStart + todayEnergy
		thisWeekTotal = thisWeekTotal + thisWeek
		device.thisWeekEnergy = thisWeek
		thisMonth = thisMonthStart + todayEnergy
		thisMonthTotal = thisMonthTotal + thisMonth
		device.thisMonthEnergy = thisMonth
	}
	state.todayTotalEnergy = todayTotalEnergy
	state.thisWeekTotal = thisWeekTotal
	state.thisMonthTotal = thisMonthTotal
	logDebug "Energy update done"
	updateCost()
}

void updateCost() {
	logDebug "Start cost update"
	def totalCostToday = 0
	def totalCostWeek = 0
	def totalCostMonth = 0
	tempRate = new BigDecimal(state.energyRate)
	logTrace "Current rate is ${tempRate}"
	energies.each {dev ->
		device = state.energies["$dev.id"]

		tempTodayCost = new BigDecimal(device.todayCost)
		tempWeekCost = device.thisWeekCost
		tempMonthCost = device.thisMonthCost
		
		thisWeek = device.thisWeekEnergy
		thisMonth = device.thisMonthEnergy

		tempEnergy = new BigDecimal(device.energyChange)
		
		costCheck = (tempEnergy*tempRate)
		if(costCheck >= 0) {
			tempCost = costCheck
			} else {
				tempCost = 0
				log.info "Cost change for ${dev} is a negative; energy was reset"}
		if(tempCost > 0) logTrace "Price change for ${dev} is ${tempEnergy} * ${tempRate} = ${tempCost}"
		
		tempTodayCost += tempCost
		tempWeekCost += tempCost
		tempMonthCost += tempCost
		totalCostToday += tempTodayCost
		totalCostWeek += tempWeekCost
		totalCostMonth += tempMonthCost
		device.todayCost = tempTodayCost
		device.thisWeekCost = tempWeekCost 
		device.thisMonthCost = tempMonthCost 
		logTrace "New cost for ${dev.displayName} is ${device.todayCost}"
		//Set variables for devices
		todayVar = device.todayVar
		weekVar = device.weekVar
		monthVar = device.monthVar
		if(todayVar) {
			tempTodayCost = tempTodayCost.setScale(2, BigDecimal.ROUND_HALF_DOWN)
			if(symbol) {setGlobalVar(todayVar, symbol+tempTodayCost.toString())} else {setGlobalVar(todayVar,tempTodayCost.toString())}
		}
		if(weekVar) {
			tempWeekCost = tempWeekCost.setScale(2, BigDecimal.ROUND_HALF_DOWN)
			if(symbol) {setGlobalVar(weekVar, symbol+tempWeekCost.toString())} else {setGlobalVar(weekVar,tempWeekCost.toString())}
		}
		if(monthVar) {
			tempMonthCost = tempMonthCost.setScale(2, BigDecimal.ROUND_HALF_DOWN)
			if(symbol) {setGlobalVar(monthVar, symbol+tempMonthCost.toString())} else {setGlobalVar(monthVar,tempMonthCost.toString())}
		}
	}
	state.totalCostToday = totalCostToday
	state.totalCostWeek = totalCostWeek
	state.totalCostMonth = totalCostMonth
	//Set variables for 'totals'
	todayTotalVar = state.todayTotalVar
	weekTotalVar = state.weekTotalVar
	monthTotalVar = state.monthTotalVar

	if(todayTotalVar) {
		totalCostToday = totalCostToday.setScale(2, BigDecimal.ROUND_HALF_DOWN)
		if(symbol) {setGlobalVar(todayTotalVar, symbol+totalCostToday.toString())} else {setGlobalVar(todayTotalVar,totalCostToday.toString())}
	}
	if(weekTotalVar) {
		totalCostWeek = totalCostWeek.setScale(2, BigDecimal.ROUND_HALF_DOWN)
		if(symbol) {setGlobalVar(weekTotalVar, symbol+totalCostWeek.toString())} else {setGlobalVar(weekTotalVar,totalCostWeek.toString())}
	}
	if(monthTotalVar) {
		totalCostMonth = totalCostMonth.setScale(2, BigDecimal.ROUND_HALF_DOWN)
		if(symbol) {setGlobalVar(monthTotalVar, symbol+totalCostMonth.toString())} else {setGlobalVar(monthTotalVar,totalCostMonth.toString())}
	}
	logDebug "Cost update done"
}

void rateScheduler() {
	def schedules = state.schedulesList
	schedules.each {sched ->
	scheds = state.schedules["$sched"]
	String rateDow = scheds.rateDayOfWeek.toString().replace("[","").replace("]","").replace(" ","") ?: "*"
	if(rateDow == "ALL") rateDow = "*"
	String rateTod = scheds.rateTimeOfDay ?: "*"
	String rateMon = scheds.rateMonth.toString().replace("[","").replace("]","").replace(" ","") ?: "*"
	if(rateMon == "ALL") rateMon = "*"
	String makingCron = "1 0 ${rateTod} ? ${rateMon} ${rateDow} *"
	logTrace "${sched} cron is ${makingCron}"
	schedule(makingCron,setRate, [data:sched,overwrite:false])
	}
}

void setRate(data) {
	logTrace "Old rate is ${state.energyRate}"
	state.energyRate = state.schedules["$data"].rateCost ?: 1
	logTrace "New rate is ${state.energyRate}"
}

void resetDaily() {
	logDebug "Daily reset"
	energies.each {dev ->
		device = state.energies["$dev.id"]
		device.dayStart = dev.currentEnergy ?: 0
		device.todayCost = 0
		device.weekStart = device.thisWeekEnergy
		device.monthStart = device.thisMonthEnergy
		logTrace "${dev} starting energy is ${device.dayStart}"
	}
}

void resetWeekly() {
	logDebug "Weekly reset"
	energies.each {dev ->
	device = state.energies["$dev.id"]
	device.lastWeekEnergy = 0
	device.lastWeekEnergy = device.thisWeekEnergy
	device.thisWeekEnergy = 0
	device.thisWeekCost = 0
	}
	state.lastWeekTotal = state.thisWeekTotal
	state.thisWeekTotal = 0
}

void resetMonthly() {
	logDebug "Monthly reset"
	energies.each {dev ->
	device = state.energies["$dev.id"]
	device.lastMonthEnergy = 0
	device.lastMonthEnergy = device.thisMonthEnergy
	device.thisMonthEnergy = 0
	device.thisMonthCost = 0
	}
	state.lastMonthTotal = state.thisMonthTotal
	state.thisMonthTotal = 0
}

def resetForTest(yes) {
	toReset = yes ?: state.onceReset
	logTrace "state.onceReset is ${toReset}"
	String pattern = /.*?(\d+\.\d+).*/
	if(toReset != 2) {
		log.warn "Converting cost variables to decimal"
		tempRate = new BigDecimal(state.energyRate)
		energies.each {dev ->
			device = state.energies["$dev.id"]
			todayCost = device.todayCost
			thisWeekCost = device.thisWeekCost
			thisMonthCost = device.thisMonthCost
			
			
			java.util.regex.Matcher matching = todayCost =~ pattern
			BigDecimal newTodayCost = matching[0][1].toBigDecimal()
			log.warn "${dev} converted today value of ${newTodayCost}"
			device.todayCost = newTodayCost

			java.util.regex.Matcher matchingWeek = thisWeekCost =~ pattern
			BigDecimal newThisWeekCost = matchingWeek[0][1].toBigDecimal()
			log.warn "${dev} converted this week value of ${newThisWeekCost}"
			device.thisWeekCost = newThisWeekCost

			java.util.regex.Matcher matchingMonth = thisMonthCost =~ pattern
			BigDecimal newthisMonthCost = matchingMonth[0][1].toBigDecimal()
			log.warn "${dev} converted this month value of ${newthisMonthCost}"
			device.thisMonthCost = newthisMonthCost


		}
		toReset = 2
		logTrace "state.onceReset updated to ${toReset}"
		state.onceReset = toReset
    } else if(toReset == 2) {
	logTrace "Once reset skipped"
    }
}

def getFormat(type, myText="") {
	if(type == "header") return "<div style='color:#660000;font-weight: bold'>${myText}</div>"
	if(type == "important") return "<div style='color:#32a4be'>${myText}</div>"
	if(type == "rateDisplay") return "<div style='color:green; text-align: center;font-weight: bold'>${myText}</div>"
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
    }
}

def logTrace(msg) {
    if (settings?.traceOutput) {
		log.trace msg
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    app.updateSetting("debugOutput",[value:"false",type:"bool"])
}