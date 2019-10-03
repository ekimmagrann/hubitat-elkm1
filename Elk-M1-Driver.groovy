/***********************************************************************************************************************
 *
 *  A Hubitat Driver using Telnet on the local network to connect to the Elk M1 via the M1XEP or C1M1
 *
 *  License:
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 *  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  Name: Elk M1 Driver
 *
 *  A Special Thanks to Doug Beard for the framework of this driver!
 *
 *  I am not a programmer so alot of this work is through trial and error. I also spent a good amount of time looking
 *  at other integrations on various platforms. I know someone else was working on an Elk driver that involved an ESP
 *  setup. This is a more direct route using equipment I already owned.
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.1.29" }

public static boolean isDebug() { return true }
import groovy.transform.Field

//import java.util.regex.Matcher
//import java.util.regex.Pattern


metadata {
	definition(name: "Elk M1 Driver", namespace: "belk", author: "Mike Magrann") {
		capability "Switch"
		capability "Initialize"
		capability "Telnet"
		capability "Thermostat"
		capability "ContactSensor"
		command "Disarm"
		command "ArmAway"
		command "ArmHome"
		command "ArmStayInstant"
		command "ArmNight"
		command "ArmNightInstant"
		command "ArmVacation"
		command "RequestArmStatus"
		command "ControlOutputOn", ["number", "time"]
		command "ControlOutputOff", ["number"]
		command "ControlOutputToggle", ["number"]
		command "TaskActivation", ["number"]
		command "RequestThermostatData"
		command "RequestTemperatureData"
//This is used to run the zone import script
//		command "RequestTextDescriptions"
		command "RequestZoneDefinitions"
		command "RequestZoneStatus"
		command "RequestOutputStatus"
		attribute "ArmStatus", "string"
		attribute "ArmState", "string"
		attribute "AlarmState", "string"
		attribute "LastUser", "string"
		attribute "contact", "string"
		attribute "switch", "string"
	}
	preferences {
		input name: "ip", type: "text", title: "IP Address", description: "ip", required: true
		input name: "port", type: "text", title: "Port", description: "port", required: true
		input name: "passwd", type: "text", title: "Password", description: "password", required: true
		input name: "code", type: "text", title: "Code", description: "code", required: true
		input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: false
		input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
	}
}

//general handlers
def installed() {
	log.warn "${device.label} installed..."
	initialize()
}

def updated() {
	log.info "${device.label} Updated..."
	if (dbgEnable)
		log.debug "${device.label}: Configuring IP: ${ip}, Port ${port}, Code: ${code}, Password: ${passwd}"
	initialize()
}

def initialize() {
	telnetClose()
	boolean success = true
	try {
		//open telnet connection
		telnetConnect([termChars: [13, 10]], ip, port.toInteger(), null, null)
		//give it a chance to start
		pauseExecution(1000)
		if (dbgEnable)
			log.debug "${device.label}: Telnet connection to Elk M1 established"
	} catch (e) {
		log.warn "${device.label}: initialize error: ${e.message}"
		success = false
	}
	if (success) {
		runIn(1, "poll")
	}
}

def uninstalled() {
	telnetClose()
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each { deleteChildDevice(it.deviceNetworkId) }
}

def poll() {
	runIn(1, "RequestThermostatData")
	pauseExecution(1000)
	runIn(1, "RequestTemperatureData")
	pauseExecution(1000)
	runIn(1, "RequestArmStatus")
	pauseExecution(1000)
	runIn(1, "RequestOutputStatus")
	pauseExecution(1000)
	runIn(1, "RequestZoneStatus")
}

//Elk M1 Command Line Request - Start of
hubitat.device.HubAction off() {
	Disarm()
}

hubitat.device.HubAction on() {
	ArmAway()
}

hubitat.device.HubAction Disarm() {
	if (dbgEnable)
		log.debug "${device.label}: Disarm()"
	String cmd = elkCommands["Disarm"]
	prepMsg(cmd)
}


hubitat.device.HubAction ArmAway() {
	if (dbgEnable)
		log.debug "${device.label}: ArmAway()"
	String cmd = elkCommands["ArmAway"]
	prepMsg(cmd)
}

hubitat.device.HubAction ArmHome() {
	if (dbgEnable)
		"armHome()"
	def cmd = elkCommands["ArmHome"]
	prepMsg(cmd)
}

hubitat.device.HubAction ArmStayInstant() {
	if (dbgEnable)
		log.debug "${device.label}: armStayInstant()"
	String cmd = elkCommands["ArmStayInstant"]
	prepMsg(cmd)
}

hubitat.device.HubAction ArmNight() {
	if (dbgEnable)
		log.debug "${device.label}: armNight()"
	String cmd = elkCommands["ArmNight"]
	prepMsg(cmd)
}

hubitat.device.HubAction ArmNightInstant() {
	if (dbgEnable)
		log.debug "${device.label}: armNightInstant()"
	String cmd = elkCommands["ArmNightInstant"]
	prepMsg(cmd)
}

hubitat.device.HubAction ArmVacation() {
	if (dbgEnable)
		log.debug "${device.label}: armVacation()"
	String cmd = elkCommands["ArmVacation"]
	prepMsg(cmd)
}

hubitat.device.HubAction RequestArmStatus() {
	if (dbgEnable)
		log.debug "${device.label}: requestArmStatus()"
	String cmd = elkCommands["RequestArmStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction RequestTemperatureData() {
	if (dbgEnable)
		log.debug "${device.label}: requestTemperatureData()"
	String cmd = elkCommands["RequestTemperatureData"]
	sendMsg(cmd)
}

//This for loop now works properly
def RequestTextDescriptions(String deviceType, int startDev) {
	if (dbgEnable)
		log.debug "${device.label}: Request Text Descriptions Type: ${deviceType} Zone: ${startDev}"
	String cmd = elkCommands["RequestTextDescriptions"] + deviceType + String.format("%03d", startDev) + "00"
	String msgStr = addChksum(Integer.toHexString(cmd.length() + 2).toUpperCase().padLeft(2, '0') + cmd)
	if (dbgEnable)
		log.debug "${device.label}: sending $msgStr"
	sendHubCommand(new hubitat.device.HubAction(msgStr, hubitat.device.Protocol.TELNET))
}

hubitat.device.HubAction ControlOutputOn(BigDecimal output = 0, String time = "0") {
	if (dbgEnable)
		log.debug "${device.label}: controlOutputOn()"
	String cmd = elkCommands["ControlOutputOn"]
	cmd = cmd + String.format("%03d", output.intValue()) + time.padLeft(5, '0')
	sendMsg(cmd)
}

hubitat.device.HubAction ControlOutputOff(BigDecimal output = 0) {
	if (dbgEnable)
		log.debug "${device.label}: controlOutputOff()"
	String cmd = elkCommands["ControlOutputOff"]
	cmd = cmd + String.format("%03d", output.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction ControlOutputToggle(BigDecimal output = 0) {
	if (dbgEnable)
		log.debug "${device.label}: controlOutputToggle()"
	String cmd = elkCommands["ControlOutputToggle"]
	cmd = cmd + String.format("%03d", output.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction TaskActivation(BigDecimal task = 0) {
	if (dbgEnable)
		log.debug "${device.label}: taskActivation()"
	String cmd = elkCommands["TaskActivation"]
	cmd = cmd + String.format("%03d", task.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction RequestThermostatData(String thermostat = "01") {
	if (dbgEnable)
		log.debug "${device.label}: requestThermostatData()"
	String cmd = elkCommands["RequestThermostatData"]
	cmd = cmd + thermostat
	sendMsg(cmd)
}

hubitat.device.HubAction setThermostatMode(String thermostatmode, String thermostat = "01") {
	String cmd = elkCommands["setThermostatMode"]
	String value = elkThermostatModeSet[thermostatmode]
	value = (value.padLeft(2, '0'))
	String element = "0"
	cmd = cmd + thermostat + value + element
	if (dbgEnable)
		log.debug "${device.label}: setThermostatMode()" + cmd + thermostatmode + thermostat
	sendMsg(cmd)
}

hubitat.device.HubAction auto(String thermostat = "01") {
	String cmd = elkCommands["auto"]
	String value = "03"
	String element = "0"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction heat(String thermostat = "01") {
	String cmd = elkCommands["heat"]
	String value = "01"
	String element = "0"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction emergencyHeat(String thermostat = "01") {
	String cmd = elkCommands["emergencyHeat"]
	String value = "01"
	String element = "0"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction cool(String thermostat = "01") {
	String cmd = elkCommands["cool"]
	String value = "02"
	String element = "0"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction off(String thermostat) {
	String cmd = elkCommands["off"]
	String value = "00"
	String element = "0"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setThermostatFanMode(String fanmode, String thermostat = "01") {
	String cmd = elkCommands["setThermostatFanMode"]
	String value = elkThermostatFanModeSet[fanmode]
	value = (value.padLeft(2, '0'))
	String element = "2"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction fanOn(String thermostat = "01") {
	String cmd = elkCommands["fanOn"]
	String value = "01"
	String element = "2"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction fanAuto(String thermostat = "01") {
	String cmd = elkCommands["fanAuto"]
	String value = "00"
	String element = "2"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction fanCirculate(String thermostat = "01") {
	String cmd = elkCommands["fanCirculate"]
	String value = "00"
	String element = "2"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setHeatingSetpoint(BigDecimal degrees, String thermostat = "01") {
	String cmd = elkCommands["setHeatingSetpoint"]
	String value = String.format("%02d", degrees.intValue())
	String element = "5"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setCoolingSetpoint(BigDecimal degrees, String thermostat = "01") {
	String cmd = elkCommands["setCoolingSetpoint"]
	String value = String.format("%02d", degrees.intValue())
	String element = "4"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

//hubitat.device.HubAction setThermostatSetpoint(String degrees, String thermostat = "01") {
//	String cmd = elkCommands["setHeatingSetpoint"]
//	String value = degrees
//	String element = "4"
//	cmd = cmd + thermostat + value + element
//	sendMsg(cmd)
//}

hubitat.device.HubAction RequestZoneDefinitions() {
	if (dbgEnable)
		log.debug "${device.label}: request Zone Definitions()"
	String cmd = elkCommands["RequestZoneDefinitions"]
	sendMsg(cmd)
}

hubitat.device.HubAction RequestZoneStatus() {
	if (dbgEnable)
		log.debug "${device.label}: RequestZoneStatus()"
	String cmd = elkCommands["RequestZoneStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction RequestOutputStatus() {
	if (dbgEnable)
		log.debug "${device.label}: RequestOutputStatus()"
	String cmd = elkCommands["RequestOutputStatus"]
	sendMsg(cmd)
}

//Elk M1 Command Line Request - End of


//Elk M1 Message Send Lines - Start of
hubitat.device.HubAction prepMsg(String cmd) {
	String area = "1"
	sendMsg(cmd + area + code.padLeft(6, '0'))
}

hubitat.device.HubAction sendMsg(String cmd) {
	String msg = cmd + "00"
	String msgStr = addChksum(Integer.toHexString(msg.length() + 2).toUpperCase().padLeft(2, '0') + msg)
	if (dbgEnable)
		log.debug "${device.label}: sendMsg $msgStr"
	return new hubitat.device.HubAction(msgStr, hubitat.device.Protocol.TELNET)
}

String addChksum(String msg) {
	char[] msgArray = msg.toCharArray()
	int msgSum = 0
	msgArray.each { (msgSum += (int) it) }
	String chkSumStr = msg + Integer.toHexString(256 - (msgSum % 256)).toUpperCase().padLeft(2, '0')
}

//Elk M1 Message Send Lines - End of


//Elk M1 Event Receipt Lines
private parse(String message) {
	if (dbgEnable)
		log.debug "${device.label}: Parsing Incoming message: " + message

	switch (message.substring(2, 4)) {
		case "ZC":
			zoneChange(message);
			break;
		case "CC":
			outputChange(message);
			break;
		case "TC":
			taskChange(message);
			break;
		case "EE":
			entryExitChange(message);
			break;
		case "AS":
			armStatusReport(message);
			break;
		case "IC":
			userCodeEntered(message);
			break;
		case "AR":
			alarmReporting(message);
			break;
		case "CS":
			outputStatus(message);
			break;
		case "DS":
			//lightingDeviceStatus(message);
			break;
		case "LD":
			logData(message);
			break;
		case "LW":
			temperatureData(message);
			break;
		case "SD":
			stringDescription(message);
			break;
		case "ST":
			//statusTemperature(message);
			break;
		case "TR":
			thermostatData(message);
			break;
		case "AM":
			//if (dbgEnable) log.debug "${device.label}: The event is unknown: ";
			break;
		case "XK":
			// Ethernet Test
			break;
		case "ZD":
			zoneDefinitionReport(message);
			break;
		case "ZS":
			zoneStatusReport(message);
			break;
		default:
			if (dbgEnable) log.debug "${device.label}: The command is unknown";
			break;
	}
}

def zoneChange(String message) {
	String zoneNumber = message.substring(4, 7)
	String zoneStatus = message.substring(7, 8)
	switch (zoneStatus) {
		case "1":
			if (dbgEnable) log.debug "${device.label}: ZoneChange: ${zoneNumber} - ${zoneStatus} - ${NormalOpen}";
			zoneNormal(message);
			break;
		case "2":
			if (dbgEnable) log.debug "${device.label}: ZoneChange: ${zoneNumber} - ${zoneStatus} - ${NormalEOL}";
			zoneNormal(message);
			break;
		case "3":
			if (dbgEnable) log.debug "${device.label}: ZoneChange: ${zoneNumber} - ${zoneStatus} - ${NormalShort}";
			zoneNormal(message);
			break;
		case "9":
			if (dbgEnable) log.debug "${device.label}: ZoneChange: ${zoneNumber} - ${zoneStatus} - ${ViolatedOpen}";
			zoneViolated(message);
			break;
		case "A":
			if (dbgEnable) log.debug "${device.label}: ZoneChange: ${zoneNumber} - ${zoneStatus} - ${ViolatedEOL}";
			zoneViolated(message);
			break;
		case "B":
			if (dbgEnable) log.debug "${device.label}: ZoneChange: ${zoneNumber} - ${zoneStatus} - ${ViolatedShort}";
			zoneViolated(message);
			break;
//		case "0":
//			if (dbgEnable) log.debug "${device.label}: ZoneChange: ${zoneNumber} - ${zoneStatus} - ${NormalUnconfigured}";
//			zoneNormal(message);
//			break;
//		case "5":
//			if (dbgEnable) log.debug "${device.label}: ZoneChange: ${zoneNumber} - ${zoneStatus} - ${TroubleOpen}";
//			break;
//		case "6":
//			zoneStatus = TroubleEOL;
//			if (dbgEnable) log.debug "${device.label}: ZoneChange: ${zoneNumber} - ${zoneStatus} - ${TroubleEOL}";
//			break;
//		case "7":
//			if (dbgEnable) log.debug "${device.label}: ZoneChange: ${zoneNumber} - ${zoneStatus} - ${TroubleShort}";
//			break;
//		case "8":
//			if (dbgEnable) log.debug "${device.label}: ZoneChange: ${zoneNumber} - ${zoneStatus} - ${NotUsed}";
//			break;
		default:
			if (dbgEnable) log.debug "${device.label}: Unknown zone status message $zoneStatus";
			break;
	}
}

def entryExitChange(String message) {
	String exitArea = message.substring(4, 5)
	String exitTime = message.substring(6, 12)
	String armStatus = elkArmStatuses[message.substring(12, 13)]
	if (dbgEnable)
		log.debug "${device.label}: Area: $exitArea, Time: $exitTime, ArmStatus: $armStatus"
	if (exitArea == "1" && exitTime == "000000") {
		String newArmMode
		String newSetArm
		if (armStatus == Disarmed) {
			newArmMode = "Home"
			newSetArm = "disarm"
		} else if (armStatus == ArmedStay || armStatus == ArmedStayInstant) {
			newArmMode = "Stay"
			newSetArm = "armHome"
		} else if (armStatus == ArmedtoNight || armStatus == ArmedtoNightInstant) {
			newArmMode = "Night"
			newSetArm = "armNight"
		} else if (armStatus == ArmedtoVacation) {
			newArmMode = "Vacation"
			newSetArm = "armAway"
		} else {
			newArmMode = "Away"
			newSetArm = "armAway"
		}
		setMode(newArmMode, newSetArm, armStatus)
	}
}

def armStatusReport(String message) {
	String armStatus = elkArmStatuses[message.substring(4, 5)]
	String armUpState = elkArmUpStates[message.substring(12, 13)]
	String alarmState = elkAlarmStates[message.substring(20, 21)]
	if (dbgEnable) {
		log.debug "${device.label}: ArmStatus: ${armStatus}"
		log.debug "${device.label}: ArmUpState: ${armUpState}"
		log.debug "${device.label}: AlarmState: ${alarmState}"
	}
	sendEvent(name: "ArmState", value: armUpState, displayed: false, isStateChange: true)
	if (state.alarmState != alarmState) {
		state.alarmState = alarmState
		sendEvent(name: "AlarmState", value: alarmState, displayed: false, isStateChange: true)
		if (txtEnable)
			log.info "${device.label}: AlarmState changed to ${alarmState}"
		if (alarmState == PoliceAlarm || alarmState == BurgularAlarm) {
			device.sendEvent(name: "contact", value: "open", isStateChange: true)
		} else {
			device.sendEvent(name: "contact", value: "closed", isStateChange: true)
		}
	}
}

def userCodeEntered(String message) {
	String userCode = message.substring(16, 19)
	if (txtEnable)
		log.info "${device.label} LastUser was ${userCode}"
	sendEvent(name: "LastUser", value: userCode, displayed: false, isStateChange: true)
}

def alarmReporting(String message) {
	log.warn "${device.label}: AlarmReporting"
	String accountNumber = message.substring(4, 10)
	String alarmCode = message.substring(10, 14)
	String area = message.substring(14, 16)
	String zone = message.substring(16, 19)
	String telIp = message.substring(19, 20)
}

def outputChange(String message) {
	String outputNumber = message.substring(4, 7)
	String outputState = (message.substring(7, 8) == "1") ? "on" : "off"
	def zoneDevice = getChildDevice("${device.deviceNetworkId}_O_${outputNumber}")
	if (zoneDevice != null) {
		if (state.outputReport != null) {
			state.outputReport = sendReport(state.outputReport, zoneDevice, outputNumber, outputState == "on")
		}
		if (dbgEnable)
			log.debug "${device.label}: Output Change Update: ${outputNumber} - ${outputState}"
		if (zoneDevice.hasCapability("Switch")) {
			zoneDevice.sendEvent(name: "switch", value: outputState, isStateChange: true)
		}
		if (zoneDevice.hasCommand("writeLog")) {
			zoneDevice.writeLog(outputState)
		}
	}
}

def outputStatus(String message) {
	String outputString = message.substring(4, 212)
	String outputState
	int i
	for (i = 1; i <= 208; i++) {
		outputState = outputString.substring(i - 1, i)
		//if (dbgEnable)
		//	log.debug "${device.label}: OutputStatus: Output " + i + " - " + elkOutputStates[outputState]
		outputChange("    " + String.format("%03d", i) + outputState)
		if (i <= 32) {
			taskStatus([Message: "    " + String.format("%03d", i), State: "off"])
		}
	}
}

def lightingDeviceStatus(String message) {
	if (dbgEnable)
		log.debug "${device.label}: LightStatus: "
	String deviceNumber = message.substring(4, 7)
	String deviceState = message.substring(7, 8)
}

def logData(String message) {
	String eventCode = message.substring(4, 8)
	String eventValue = elkResponses[eventCode]
	if (eventValue != null && dbgEnable) {
		log.debug "${device.label}: LogData: ${eventCode} - ${eventValue}"
	}
	if (eventCode == '1173' || eventCode == '1183' || eventCode == '1223' || eventCode == '1231') {
//		sendEvent(name:"Status", value: eventValue, displayed:false, isStateChange: true)
// 		systemArmed()
	} else if (eventCode == '1191' || eventCode == '1199') {
//		sendEvent(name:"Status", value: eventValue, displayed:false, isStateChange: true)
//		systemArmed()
	} else if (eventCode == '1174') {
//		sendEvent(name:"Status", value: eventValue, displayed:false, isStateChange: true)
//		disarming()
		setMode("Home", "disarm", "Disarmed")
	} else if ('1207' || eventCode == '1215') {
//		sendEvent(name:"Status", value: eventValue, displayed:false, isStateChange: true)
//		systemArmed()
	} else if (eventCode != '1000' && eventValue != null) {
		sendEvent(name: "Status", value: eventValue, displayed: false, isStateChange: true)
	}
}

def temperatureData(String message) {
	int number
	String zoneNumber
	def zoneDevice
	int i
	int zoneInt
	for (i = 4; i <= 50; i += 3) {
		number = message.substring(i, i + 3).toInteger()
		if (number > 0) {
			zoneInt = (i - 1) / 3
			zoneNumber = String.format("%03d", zoneInt)
			number = number - 40
			if (dbgEnable)
				log.debug "${device.label}: Keypad ${zoneNumber} temp: ${number}"
			zoneDevice = getChildDevice("${device.deviceNetworkId}_P_${zoneNumber}")
			if (zoneDevice?.capabilities?.find { it.name == "TemperatureMeasurement" } != null) {
				zoneDevice.sendEvent(name: "temperature", value: number)
			}
		}
	}

	for (i = 52; i <= 98; i += 3) {
		number = message.substring(i, i + 3).toInteger()
		if (number > 0) {
			zoneInt = (i - 49) / 3
			zoneNumber = String.format("%03d", zoneInt)
			number = number - 60
			if (dbgEnable)
				log.debug "${device.label}: Zone ${zoneNumber} temp: ${number}"
			zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}")
			if (zoneDevice?.capabilities?.find { it.name == "TemperatureMeasurement" } != null) {
				zoneDevice.sendEvent(name: "temperature", value: number)
			}
		}
	}
}

def stringDescription(String message) {
	String zoneNumber = message.substring(6, 9)
	if (zoneNumber != "000") {
		String zoneName
		String zoneType = message.substring(4, 6)
		byte firstText = message.substring(9, 10)
		String zoneText = (String) ((char) (firstText & 0b01111111)) + message.substring(10, 25).trim()
		// Mask high order "Show On Keypad" bit in first letter
		if (zoneText != "") {
			createZone([zoneNumber: zoneNumber, zoneName: zoneName, zoneType: zoneType, zoneText: zoneText])
		}
		int i = zoneNumber.toInteger() // Request next zone description
		if (i < 208) {
			RequestTextDescriptions(zoneType, i + 1)
		}
	}
}

def statusTemperature(String message) {
	if (dbgEnable)
		log.debug "${device.label}: ReplyRequestedTemp: "
	String group = message.substring(4, 5)
	group = elkTempTypes[group]
	String device = message.substring(6, 8)
	String temp = message.substring(8, 11)
}

def taskChange(String message) {
	taskStatus([Message: message, State: "on"])
	runIn(2, "taskStatus", [data: [Message: message, State: "off"]])
}

//Zone Status (Currently working on this)
def thermostatData(String message) {
	String deviceNetworkId
	def zoneDevice
	def ElkM1DNI = device.deviceNetworkId
	def tNumber = message.substring(4, 6)
	def mode = message.substring(6, 7)
	mode = elkThermostatMode[mode]
	def hold = message.substring(7, 8)
	hold = elkThermostatHold[hold]
	def fan = message.substring(8, 9)
	fan = elkThermostatFan[fan]
	def cTemp = message.substring(9, 11)
	def hSet = message.substring(11, 13)
	def cSet = message.substring(13, 15)
	def cHumid = message.substring(15, 17)
	if (dbgEnable)
		log.debug "${device.label}: Thermostat Data Check: " + tNumber + ", " + mode + " Mode, Hold temperature = " + hold + ", " + fan + ", Current Temperature = " + cTemp + ", Heat Setpoint = " + hSet + ", Cool Setpoint = " + cSet
	tNumber = (tNumber.padLeft(3, '0'))
	deviceNetworkId = ElkM1DNI + "_T_" + tNumber
//	if (dbgEnable)
//		log.debug "${device.label}: Thermostat Data Check: " + deviceNetworkId

	if (getChildDevice(deviceNetworkId) != null) {
		zoneDevice = getChildDevice(deviceNetworkId)
		if (zoneDevice.capabilities.find { item -> item.name.startsWith('Thermostat') }) {
			zoneDevice.sendEvent(name: "temperature", value: (cTemp))
			zoneDevice.sendEvent(name: "thermostatMode", value: (mode))
			zoneDevice.sendEvent(name: "thermostatFanMode", value: (fan))
			zoneDevice.sendEvent(name: "coolingSetpoint", value: (cSet))
			zoneDevice.sendEvent(name: "heatingSetpoint", value: (hSet))
		}
	}
}

def zoneDefinitionReport(String message) {
	String zoneString
	String zoneDefinitions = message.substring(4, 212)
	int i
	for (i = 1; i <= 208; i++) {
		zoneString = elkZoneDefinitions[zoneDefinitions.substring(i - 1, i)]
		if (zoneString != null && zoneString != Disabled && dbgEnable) {
			log.debug "${device.label}: ZoneDefinitions: Zone " + i + " - " + zoneString
		}
	}
}

def zoneStatusReport(String message) {
	String zoneString = message.substring(4, 212)
	String zoneStatus
	int i
	for (i = 1; i <= 208; i++) {
		zoneStatus = zoneString.substring(i - 1, i)
		if (zoneStatus != null && zoneStatus != "0") {
			//if (dbgEnable)
			//	log.debug "${device.label}: ZoneStatus: Zone " + i + " - " + elkZoneStatuses[zoneStatus]
			zoneChange("    " + String.format("%03d", i) + zoneStatus + "    ")
		}
	}
}

// Zone Status
def zoneViolated(String message) {
	String zoneNumber = message.substring(message.length() - 8).take(3)
	def zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}")
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_C_${zoneNumber}")
	}
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_M_${zoneNumber}")
	}

	if (zoneDevice != null) {
		if (state.zoneReport != null) {
			state.zoneReport = sendReport(state.zoneReport, zoneDevice, zoneNumber, true)
		}
		def cmdList = zoneDevice.supportedCommands
		if (cmdList.find { it.name == "open" } != null) {
			zoneDevice.open()
		} else if (cmdList.find { it.name == "active" } != null) {
			zoneDevice.active()
		} else if (cmdList.find { it.name == "on" } != null) {
			zoneDevice.on()
		} else if (cmdList.find { it.name == "arrived" } != null) {
			zoneDevice.arrived()
		} else if (cmdList.find { it.name == "detected" } != null) {
			zoneDevice.detected()
		} else {
			def capList = zoneDevice.capabilities
			if (capList.find { it.name == "ContactSensor" } != null) {
				zoneDevice.sendEvent(name: "contact", value: "open")
//				if (dbgEnable)
//					log.debug "${device.label}: Contact Open"
			} else if (capList.find { it.name == "MotionSensor" } != null) {
				zoneDevice.sendEvent(name: "motion", value: "active")
//				if (dbgEnable)
//					log.debug "${device.label}: Motion Active"
			} else if (capList.find { it.name == "SmokeDetector" } != null) {
				zoneDevice.sendEvent(name: "smoke", value: "detected")
//				if (dbgEnable)
//					log.debug "${device.label}: Smoke Detected"
			} else if (capList.find { it.name == "CarbonMonoxideDetector" } != null) {
				zoneDevice.sendEvent(name: "carbonMonoxide", value: "detected")
//				if (dbgEnable)
//					log.debug "${device.label}: Carbon Monoxide Detected"
			} else if (capList.find { it.name == "WaterSensor" } != null) {
				zoneDevice.sendEvent(name: "water", value: "wet")
//				if (dbgEnable)
//					log.debug "${device.label}: Water Wet"
			} else if (capList.find { it.name == "TamperAlert" } != null) {
				zoneDevice.sendEvent(name: "tamper", value: "detected")
//				if (dbgEnable)
//					log.debug "${device.label}: Tamper Alert Detected"
			} else if (capList.find { it.name == "AccelerationSensor" } != null) {
				zoneDevice.sendEvent(name: "acceleration", value: "active")
//				if (dbgEnable)
//					log.debug "${device.label}: Acceleration Sensor Active"
			} else if (capList.find { it.name == "Beacon" } != null) {
				zoneDevice.sendEvent(name: "presence", value: "present")
//				if (dbgEnable)
//					log.debug "${device.label}: Beacon Present"
			} else if (capList.find { it.name == "GarageDoorControl" } != null) {
				zoneDevice.sendEvent(name: "door", value: "open")
//				if (dbgEnable)
//					log.debug "${device.label}: GarageDoorControl Open"
			} else if (capList.find { it.name == "PresenceSensor" } != null) {
				zoneDevice.sendEvent(name: "presence", value: "present")
//				if (dbgEnable)
//					log.debug "${device.label}: Presence Present"
			} else if (capList.find { it.name == "RelaySwitch" } != null) {
				zoneDevice.sendEvent(name: "switch", value: "on")
//				if (dbgEnable)
//					log.debug "${device.label}: Relay Switch On"
			} else if (capList.find { it.name == "ShockSensor" } != null) {
				zoneDevice.sendEvent(name: "shock", value: "detected")
//				if (dbgEnable)
//					log.debug "${device.label}: Shock Sensor Detected"
			} else if (capList.find { it.name == "SleepSensor" } != null) {
				zoneDevice.sendEvent(name: "sleeping", value: "sleeping")
//				if (dbgEnable)
//					log.debug "${device.label}: Sleep Sensor Sleeping"
			} else if (capList.find { it.name == "SoundSensor" } != null) {
				zoneDevice.sendEvent(name: "sound", value: "detected")
//				if (dbgEnable)
//					log.debug "${device.label}: Sound Sensor Detected"
			} else if (capList.find { it.name == "Switch" } != null) {
				zoneDevice.sendEvent(name: "switch", value: "on")
//				if (dbgEnable)
//					log.debug "${device.label}: Switch On"
			} else if (capList.find { it.name == "TouchSensor" } != null) {
				zoneDevice.sendEvent(name: "touch", value: "touched")
//				if (dbgEnable)
//					log.debug "${device.label}: Touch Touched"
			} else if (capList.find { it.name == "Valve" } != null) {
				zoneDevice.sendEvent(name: "valve", value: "open")
//				if (dbgEnable)
//					log.debug "${device.label}: Valve Open"
			}
		}
	}
}

def zoneNormal(String message) {
	String zoneNumber = message.substring(message.length() - 8).take(3)
	def zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}")
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_C_${zoneNumber}")
	}
	if (zoneDevice == null) { // For backwards capability
		zoneDevice = getChildDevice("${device.deviceNetworkId}_M_${zoneNumber}")
	}
	if (zoneDevice != null) {
		if (state.zoneReport != null) {
			state.zoneReport = sendReport(state.zoneReport, zoneDevice, zoneNumber, false)
		}
		def cmdList = zoneDevice.supportedCommands
		if (cmdList.find { it.name == "close" } != null) {
			zoneDevice.close()
		}
		if (cmdList.find { it.name == "inactive" } != null) {
			zoneDevice.inactive()
		}
		if (cmdList.find { it.name == "off" } != null) {
			zoneDevice.off()
		} else if (cmdList.find { it.name == "departed" } != null) {
			zoneDevice.departed()
		} else if (cmdList.find { it.name == "clear" } != null) {
			zoneDevice.clear()
		} else {
			def capList = zoneDevice?.capabilities
			if (capList.find { it.name == "ContactSensor" } != null) {
				zoneDevice.sendEvent(name: "contact", value: "closed")
//				if (dbgEnable)
//					log.debug "${device.label}: Contact Closed"
			} else if (capList.find { it.name == "MotionSensor" } != null) {
				zoneDevice.sendEvent(name: "motion", value: "inactive")
//				if (dbgEnable)
//					log.debug "${device.label}: Motion Inactive"
			} else if (capList.find { it.name == "SmokeDetector" } != null) {
				zoneDevice.sendEvent(name: "smoke", value: "clear")
//				if (dbgEnable)
//					log.debug "${device.label}: Smoke Clear"
			} else if (capList.find { it.name == "CarbonMonoxideDetector" } != null) {
				zoneDevice.sendEvent(name: "carbonMonoxide", value: "clear")
//				if (dbgEnable)
//					log.debug "${device.label}: Carbon Monoxide Clear"
			} else if (capList.find { it.name == "WaterSensor" } != null) {
				zoneDevice.sendEvent(name: "water", value: "dry")
//				if (dbgEnable)
//					log.debug "${device.label}: Water Dry"
			} else if (capList.find { it.name == "TamperAlert" } != null) {
				zoneDevice.sendEvent(name: "tamper", value: "clear")
//				if (dbgEnable)
//					log.debug "${device.label}: Tamper Clear"
			} else if (capList.find { it.name == "AccelerationSensor" } != null) {
				zoneDevice.sendEvent(name: "acceleration", value: "inactive")
//				if (dbgEnable)
//					log.debug "${device.label}: Acceleration Sensor Inactive"
			} else if (capList.find { it.name == "Beacon" } != null) {
				zoneDevice.sendEvent(name: "presence", value: "not present")
//				if (dbgEnable)
//					log.debug "${device.label}: Beacon Not Present"
			} else if (capList.find { it.name == "GarageDoorControl" } != null) {
				zoneDevice.sendEvent(name: "door", value: "closed")
//				if (dbgEnable)
//					log.debug "${device.label}: GarageDoorControl Closed"
			} else if (capList.find { it.name == "PresenceSensor" } != null) {
				zoneDevice.sendEvent(name: "presence", value: "not present")
//				if (dbgEnable)
//					log.debug "${device.label}: Presence Not Present"
			} else if (capList.find { it.name == "RelaySwitch" } != null) {
				zoneDevice.sendEvent(name: "switch", value: "off")
//				if (dbgEnable)
//					log.debug "${device.label}: Relay Switch Off"
			} else if (capList.find { it.name == "ShockSensor" } != null) {
				zoneDevice.sendEvent(name: "shock", value: "clear")
//				if (dbgEnable)
//					log.debug "${device.label}: Shock Sensor Clear"
			} else if (capList.find { it.name == "SleepSensor" } != null) {
				zoneDevice.sendEvent(name: "sleeping", value: "not sleeping")
//				if (dbgEnable)
//					log.debug "${device.label}: Sleep Sensor Not Sleeping"
			} else if (capList.find { it.name == "SoundSensor" } != null) {
				zoneDevice.sendEvent(name: "sound", value: "not detected")
//				if (dbgEnable)
//					log.debug "${device.label}: Sound Sensor Not Detected"
			} else if (capList.find { it.name == "Switch" } != null) {
				zoneDevice.sendEvent(name: "switch", value: "off")
//				if (dbgEnable)
//					log.debug "${device.label}: Switch Off"
			} else if (capList.find { it.name == "Valve" } != null) {
				zoneDevice.sendEvent(name: "valve", value: "closed")
//				if (dbgEnable)
//					log.debug "${device.label}: Valve Closed"
			}
		}
	}
}

def taskStatus(data) {
	String taskNumber = data.Message.substring(4, 7)
	def zoneDevice = getChildDevice(device.deviceNetworkId + "_K_" + taskNumber)
	if (zoneDevice != null) {
		if (dbgEnable)
			log.debug "${device.label}: Task Change Update: ${taskNumber} - ${data.State}"
		if (zoneDevice.hasCapability("Switch")) {
			if (data.State == "off") {
				zoneDevice.sendEvent(name: "switch", value: data.State)
			} else {
				zoneDevice.sendEvent(name: "switch", value: data.State, isStateChange: true)
				if (state.taskReport != null) {
					state.taskReport = sendReport(state.taskReport, zoneDevice, taskNumber, true)
				}
			}
		}
		if (data.State == "on" && zoneDevice.hasCommand("writeLog")) {
			zoneDevice.writeLog()
		}
	}
}

//NEW CODE
//Manage Zones
def createZone(zoneInfo) {
	String zoneNumber = zoneInfo.zoneNumber
	String zoneName = zoneInfo.zoneName
	String deviceNetworkId
	//if (dbgEnable)
	//	log.debug "${device.label}: zoneNumber: ${zoneNumber}, zoneName: ${zoneName}, zoneType: ${zoneInfo.zoneType}, zoneText: ${zoneInfo.zoneText}"
	if (zoneInfo.zoneType == "00") {
		if (zoneName == null) {
			zoneName = "Zone " + zoneNumber + " - " + zoneInfo.zoneText
		}
		if (getChildDevice("${device.deviceNetworkId}_C_${zoneNumber}") == null && getChildDevice("{$device.deviceNetworkId}_M_${zoneNumber}") == null &&
				getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}") == null) {
			deviceNetworkId = "${device.deviceNetworkId}_Z_${zoneNumber}"
			if (zoneName ==~ /(?i).*motion.*/) {
				if (txtEnable)
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Motion"
				addChildDevice("hubitat", "Virtual Motion Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
				def newDevice = getChildDevice(deviceNetworkId)
				newDevice.updateSetting("autoInactive", [type: "enum", value: 0])
				newDevice.updateSetting("txtEnable", [value: "false", type: "bool"])
			} else if (zoneName ==~ /(?i).*temperature.*/) {
				if (txtEnable)
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Temperature"
				addChildDevice("hubitat", "Virtual Temperature Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
				def newDevice = getChildDevice(deviceNetworkId)
				newDevice.updateSetting("txtEnable", [value: "false", type: "bool"])
			} else {
				if (txtEnable)
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Contact"
				addChildDevice("hubitat", "Virtual Contact Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
				def newDevice = getChildDevice(deviceNetworkId)
				newDevice.updateSetting("txtEnable", [value: "false", type: "bool"])
			}
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${device.deviceNetworkId}_Z_${zoneNumber} already exists"
		}
	} else if (zoneInfo.zoneType == "03") {
		if (zoneName == null) {
			zoneName = "Keypad ${zoneNumber.substring(1, 3)} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_P_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable)
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Keypad"
			addChildDevice("hubitat", "Virtual Temperature Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
			def newDevice = getChildDevice(deviceNetworkId)
			newDevice.updateSetting("txtEnable", [value: "false", type: "bool"])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "04") {
		if (zoneName == null) {
			zoneName = "Output ${zoneNumber} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_O_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable)
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Output"
			addChildDevice("belk", "Elk M1 Driver Outputs", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "05") {
		if (zoneName == null) {
			zoneName = "Task ${zoneNumber.substring(1, 3)} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_K_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable)
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Task"
			addChildDevice("belk", "Elk M1 Driver Tasks", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "11") {
		if (zoneName == null) {
			zoneName = "Thermostat ${zoneNumber} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_T_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable)
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Thermostat"
			addChildDevice("belk", "Elk M1 Driver Thermostat", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	}
}

def removeZone(zoneInfo) {
	if (txtEnable)
		log.info "${device.label}: Removing ${zoneInfo.zoneName} with deviceNetworkId = ${zoneInfo.deviceNetworkId}"
	deleteChildDevice(zoneInfo.deviceNetworkId)
}

def setMode(String armMode, String setArm, String armStatus) {
	if (state.armStatus != armStatus) {
		state.armStatus = armStatus
		sendEvent(name: "ArmStatus", value: armStatus, displayed: false, isStateChange: true)
		if (txtEnable)
			log.info "${device.label}: ArmStatus changed to ${armStatus}"
	}
	if (state.armState != armMode) {
		state.armState = armMode
		def allmodes = location.getModes()
		int idx = allmodes.findIndexOf { it.name == armMode }
		if (idx == -1 && armMode == "Vacation") {
			idx = allmodes.findIndexOf { it.name == "Away" }
		} else if (idx == -1 && armMode == "Stay") {
			idx = allmodes.findIndexOf { it.name == "Home" }
		}
		if (idx != -1) {
			String curmode = location.currentMode
			String newmode = allmodes[idx].getName()
			location.setMode(newmode)
			if (dbgEnable)
				log.debug "${device.label}: Location Mode changed from $curmode to $newmode"
		}
		if (setArm == "disarm") {
			parent.unlockIt()
			parent.speakDisarmed()
			sendEvent(name: "switch", value: "off")
		} else {
			parent.lockIt()
			parent.speakArmed()
			sendEvent(name: "switch", value: "on")
		}
		sendLocationEvent(name: "hsmSetArm", value: setArm)
		if (txtEnable)
			log.info "${device.label} changed to mode ${armMode}"
	}
}

//def armReady(){
//	if (state.armUpStates != "Ready To Arm"){
//		if (dbgEnable)
//			log.debug "${device.label}: ready to arm"
//		state.armUpStates = "Ready To Arm"
//		parent.lockIt()
//		parent.speakArmed()
//		if (location.hsmStatus == "disarmed") {
//			sendLocationEvent(name: "hsmSetArm", value: "armHome")
//		}
//	}
//}

def registerZoneReport(String deviceNetworkId, String zoneNumber) {
	if (dbgEnable)
		log.debug "${device.label}: registering zone ${zoneNumber} reporting for ${deviceNetworkId}"
	state.zoneReport = registerReport(state.zoneReport, deviceNetworkId, zoneNumber)
}

def unRegisterZoneReport(String deviceNetworkId, String zoneNumber = null) {
	if (dbgEnable)
		log.debug "${device.label}: unregistering zone ${zoneNumber} reporting for ${deviceNetworkId}"
	state.zoneReport = unRegisterReport(state.zoneReport, deviceNetworkId, zoneNumber)
}

def registerOutputReport(String deviceNetworkId, String outputNumber) {
	if (dbgEnable)
		log.debug "${device.label}: registering output ${outputNumber} reporting for ${deviceNetworkId}"
	state.outputReport = registerReport(state.outputReport, deviceNetworkId, outputNumber)
}

def unRegisterOutputReport(String deviceNetworkId, String outputNumber = null) {
	if (dbgEnable)
		log.debug "${device.label}: unregistering output ${outputNumber} reporting for ${deviceNetworkId}"
	state.outputReport = unRegisterReport(state.outputReport, deviceNetworkId, outputNumber)
}

def registerTaskReport(String deviceNetworkId, String taskNumber) {
	if (dbgEnable)
		log.debug "${device.label}: registering task ${taskNumber} reporting for ${deviceNetworkId}"
	state.taskReport = registerReport(state.taskReport, deviceNetworkId, taskNumber)
}

def unRegisterTaskReport(String deviceNetworkId, String taskNumber = null) {
	if (dbgEnable)
		log.debug "${device.label}: unregistering task ${taskNumber} reporting for ${deviceNetworkId}"
	state.taskReport = unRegisterReport(state.taskReport, deviceNetworkId, taskNumber)
}

HashMap registerReport(HashMap reportList, String deviceNetworkId, String deviceNumber) {
	if (reportList == null) {
		reportList = [:]
	}
	reportList[deviceNumber] = deviceNetworkId
	return reportList
}

HashMap unRegisterReport(HashMap reportList, String deviceNetworkId, String deviceNumber) {
	if (reportList == null) {
		reportList = [:]
	} else if (deviceNumber == null) {
		HashMap newreport = [:]
		reportList.each {
			if (it.value != deviceNetworkId) {
				newreport[it.key] = value
			}
		}
		reportList = newreport
	} else {
		reportList -= [deviceNumber: deviceNetworkId]
	}
	if (reportList.size() == 0)
		reportList = null
	return reportList
}

HashMap sendReport(HashMap reportList, zoneDevice, String deviceNumber, boolean violated) {
	String reportDNID = reportList[deviceNumber]
	if (reportDNID != null) {
		def otherChild = getChildDevice(reportDNID)
		if (otherChild != null && otherChild.hasCommand("report")) {
			otherChild.report(zoneDevice.deviceNetworkId, violated)
		} else {
			reportList = unRegisterReport(reportList, reportDNID, deviceNumber)
		}
	}
	return reportList
}

//Telnet
int getReTry(Boolean inc) {
	int reTry = (state.reTryCount ?: 0).toInteger()
	if (inc) reTry++
	state.reTryCount = reTry
	return reTry
}

def telnetStatus(String status) {
	log.warn "${device.label}: telnetStatus- error: ${status}"
	if (status != "receive error: Stream is closed") {
		getReTry(true)
		log.error "Telnet connection dropped..."
		initialize()
	} else {
		log.warn "${device.label}: Telnet is restarting..."
	}
}

////REFERENCES AND MAPPINGS////
// Event Mapping Readable Text
@Field static final String NoEvent = "No Event"
@Field static final String FIREALARM = "Fire Alarm"
@Field static final String FIRESUPERVISORYALARM = "Fire Supervisory Alarm"
@Field static final String BURGLARALARMANYAREA = "Burglar Alarm, Any Area"
@Field static final String MEDICALALARMANYAREA = "Medical Alarm, Any Area"
@Field static final String POLICEALARMANYAREA = "Police Alarm, Any Area"
@Field static final String AUX124HRANYAREA = "Aux1 24 Hr, Any Area"
@Field static final String AUX224HRANYAREA = "Aux2 24 Hr, Any Area"
@Field static final String CARBONMONOXIDEALARMANYAREA = "Carbon Monoxide Alarm, Any Area"
@Field static final String EMERGENCYALARMANYAREA = "Emergency Alarm, Any Area"
@Field static final String FREEZEALARMANYAREA = "Freeze Alarm, Any Area"
@Field static final String GASALARMANYAREA = "Gas Alarm, Any Area"
@Field static final String HEATALARMANYAREA = "Heat Alarm, Any Area"
@Field static final String WATERALARMANYAREA = "Water Alarm, Any Area"
@Field static final String ALARMANYAREA = "Alarm, Any Area"
@Field static final String CODELOCKOUTANYKEYPAD = "Code Lockout, Any Keypad"
@Field static final String FIRETROUBLEANYZONE = "Fire Trouble, Any Zone"
@Field static final String BURGLARTROUBLEANYZONE = "Burglar Trouble, Any Zone"
@Field static final String FAILTOCOMMUNICATETROUBLE = "Fail To Communicate Trouble"
@Field static final String RFSENSORLOWBATTERYTROUBLE = "Rf Sensor Low Battery Trouble"
@Field static final String LOSTANCMODULETROUBLE = "Lost Anc Module Trouble"
@Field static final String LOSTKEYPADTROUBLE = "Lost Keypad Trouble"
@Field static final String LOSTINPUTEXPANDERTROUBLE = "Lost Input Expander Trouble"
@Field static final String LOSTOUTPUTEXPANDERTROUBLE = "Lost Output Expander Trouble"
@Field static final String EEPROMMEMORYERRORTROUBLE = "Eeprom Memory Error Trouble"
@Field static final String FLASHMEMORYERRORTROUBLE = "Flash Memory Error Trouble"
@Field static final String ACFAILURETROUBLE = "Ac Failure Trouble"
@Field static final String CONTROLLOWBATTERYTROUBLE = "Control Low Battery Trouble"
@Field static final String CONTROLOVERCURRENTTROUBLE = "Control Over Current Trouble"
@Field static final String EXPANSIONMODULETROUBLE = "Expansion Module Trouble"
@Field static final String OUTPUT2SUPERVISORYTROUBLE = "Output 2 Supervisory Trouble"
@Field static final String TELEPHONELINEFAULTTROUBLE1 = "Telephone Line Fault Trouble1"
@Field static final String RESTOREFIREZONE = "Estore Fire Zone"
@Field static final String RESTOREFIRESUPERVISORYZONE = "Restore Fire Supervisory Zone"
@Field static final String RESTOREBURGLARZONE = "Restore Burglar Zone"
@Field static final String RESTOREMEDICALZONE = "Restore Medical Zone"
@Field static final String RESTOREPOLICEZONE = "Restore Police Zone"
@Field static final String RESTOREAUX124HRZONE = "Restore Aux1 24 Hr Zone"
@Field static final String RESTOREAUX224HRZONE = "Restore Aux2 24 Hr Zone"
@Field static final String RESTORECOZONE = "Restore Co Zone"
@Field static final String RESTOREEMERGENCYZONE = "Restore Emergency Zone"
@Field static final String RESTOREFREEZEZONE = "Restore Freeze Zone"
@Field static final String RESTOREGASZONE = "Restore Gas Zone"
@Field static final String RESTOREHEATZONE = "Restore Heat Zone"
@Field static final String RESTOREWATERZONE = "Restore Water Zone"
@Field static final String COMMUNICATIONFAILRESTORE = "Communication Fail Restore"
@Field static final String ACFAILRESTORE = "Ac Fail Restore"
@Field static final String LOWBATTERYRESTORE = "Low Battery Restore"
@Field static final String CONTROLOVERCURRENTRESTORE = "Control Over Current Restore"
@Field static final String EXPANSIONMODULERESTORE = "Expansion Module Restore"
@Field static final String OUTPUT2RESTORE = "Output2 Restore"
@Field static final String TELEPHONELINERESTORE = "Telephone Line Restore"
@Field static final String ALARMMEMORYANYAREA = "Alarm Memory, Any Area"
@Field static final String AREAARMED = "Area Armed"
@Field static final String AREADISARMED = "Area Disarmed"
@Field static final String AREA1ARMSTATE = "Area 1 Armed State"
@Field static final String AREA1ISARMEDAWAY = "Area 1 Is Armed Away"
@Field static final String AREA1ISARMEDSTAY = "Area 1 Is Armed Stay"
@Field static final String AREA1ISARMEDSTAYINSTANT = "Area 1 Is Armed Stay Instant"
@Field static final String AREA1ISARMEDNIGHT = "Area 1 Is Armed Night"
@Field static final String AREA1ISARMEDNIGHTINSTANT = "Area 1 Is Armed Night Instant"
@Field static final String AREA1ISARMEDVACATION = "Area 1 Is Armed Vacation"
@Field static final String AREA1ISFORCEARMED = "Area 1 Is Force Armed"
@Field static final String ZONEBYPASSED = "Zone Bypassed"
@Field static final String ZONEUNBYPASSED = "Zone Unbypassed"
@Field static final String ANYBURGLARZONEISFAULTED = "Any Burglar Zone Is Faulted"
@Field static final String BURGLARSTATUSOFALLAREAS = "Burglar Status Of All Areas"
@Field static final String AREA1CHIMEMODE = "Area 1 Chime Mode"
@Field static final String AREA1CHIMEALERT = "Area 1 Chime Alert"
@Field static final String ENTRYDELAYANYAREA = "Entry Delay, Any Area"
@Field static final String EXITDELAYANYAREA = "Exit Delay, Any Area"
@Field static final String AREA1EXITDELAYENDS = "Area 1 Exit Delay Ends"

// Event Mapping
@Field final Map elkResponses = [
		'1000': NoEvent,
		'1001': FIREALARM,
		'1002': FIRESUPERVISORYALARM,
		'1003': BURGLARALARMANYAREA,
		'1004': MEDICALALARMANYAREA,
		'1005': POLICEALARMANYAREA,
		'1006': AUX124HRANYAREA,
		'1007': AUX224HRANYAREA,
		'1008': CARBONMONOXIDEALARMANYAREA,
		'1009': EMERGENCYALARMANYAREA,
		'1010': FREEZEALARMANYAREA,
		'1011': GASALARMANYAREA,
		'1012': HEATALARMANYAREA,
		'1013': WATERALARMANYAREA,
		'1014': ALARMANYAREA,
		'1111': CODELOCKOUTANYKEYPAD,
		'1128': FIRETROUBLEANYZONE,
		'1129': BURGLARTROUBLEANYZONE,
		'1130': FAILTOCOMMUNICATETROUBLE,
		'1131': RFSENSORLOWBATTERYTROUBLE,
		'1132': LOSTANCMODULETROUBLE,
		'1133': LOSTKEYPADTROUBLE,
		'1134': LOSTINPUTEXPANDERTROUBLE,
		'1135': LOSTOUTPUTEXPANDERTROUBLE,
		'1136': EEPROMMEMORYERRORTROUBLE,
		'1137': FLASHMEMORYERRORTROUBLE,
		'1138': ACFAILURETROUBLE,
		'1139': CONTROLLOWBATTERYTROUBLE,
		'1140': CONTROLOVERCURRENTTROUBLE,
		'1141': EXPANSIONMODULETROUBLE,
		'1142': OUTPUT2SUPERVISORYTROUBLE,
		'1143': TELEPHONELINEFAULTTROUBLE1,
		'1144': RESTOREFIREZONE,
		'1145': RESTOREFIRESUPERVISORYZONE,
		'1146': RESTOREBURGLARZONE,
		'1147': RESTOREMEDICALZONE,
		'1148': RESTOREPOLICEZONE,
		'1149': RESTOREAUX124HRZONE,
		'1150': RESTOREAUX224HRZONE,
		'1151': RESTORECOZONE,
		'1152': RESTOREEMERGENCYZONE,
		'1153': RESTOREFREEZEZONE,
		'1154': RESTOREGASZONE,
		'1155': RESTOREHEATZONE,
		'1156': RESTOREWATERZONE,
		'1157': COMMUNICATIONFAILRESTORE,
		'1158': ACFAILRESTORE,
		'1159': LOWBATTERYRESTORE,
		'1160': CONTROLOVERCURRENTRESTORE,
		'1161': EXPANSIONMODULERESTORE,
		'1162': OUTPUT2RESTORE,
		'1163': TELEPHONELINERESTORE,
		'1164': ALARMMEMORYANYAREA,
		'1173': AREAARMED,
		'1174': AREADISARMED,
		'1175': AREA1ARMSTATE,
		'1183': AREA1ISARMEDAWAY,
		'1191': AREA1ISARMEDSTAY,
		'1199': AREA1ISARMEDSTAYINSTANT,
		'1207': AREA1ISARMEDNIGHT,
		'1215': AREA1ISARMEDNIGHTINSTANT,
		'1223': AREA1ISARMEDVACATION,
		'1231': AREA1ISFORCEARMED,
		'1239': ZONEBYPASSED,
		'1240': ZONEUNBYPASSED,
		'1241': ANYBURGLARZONEISFAULTED,
		'1242': BURGLARSTATUSOFALLAREAS,
		'1251': AREA1CHIMEMODE,
		'1259': AREA1CHIMEALERT,
		'1267': ENTRYDELAYANYAREA,
		'1276': EXITDELAYANYAREA,
		'1285': AREA1EXITDELAYENDS
]


@Field static final String Disarmed = "Disarmed"
@Field static final String ArmedAway = "Armed Away"
@Field static final String ArmedStay = "Armed Stay"
@Field static final String ArmedStayInstant = "Armed Stay Instant"
@Field static final String ArmedtoNight = "Armed To Night"
@Field static final String ArmedtoNightInstant = "Armed To Night Instance"
@Field static final String ArmedtoVacation = "Armed To Vacation"

@Field final Map elkArmStatuses = [
		'0': Disarmed,
		'1': ArmedAway,
		'2': ArmedStay,
		'3': ArmedStayInstant,
		'4': ArmedtoNight,
		'5': ArmedtoNightInstant,
		'6': ArmedtoVacation
]
@Field static final String NotReadytoArm = "Not Ready to Arm"
@Field static final String ReadytoArm = "Ready to Arm"
@Field static final String ReadytoArmBut = "Ready to Arm, but a zone is violated and can be force armed"
@Field static final String ArmedwithExit = "Armed with Exit Timer working"
@Field static final String ArmedFully = "Armed Fully"
@Field static final String ForceArmed = "Force Armed with a force arm zone violated"
@Field static final String ArmedwithaBypass = "Armed with a Bypass"

@Field final Map elkArmUpStates = [
		'0': NotReadytoArm,
		'1': ReadytoArm,
		'2': ReadytoArmBut,
		'3': ArmedwithExit,
		'4': ArmedFully,
		'5': ForceArmed,
		'6': ArmedwithaBypass
]

@Field static final String NoActiveAlarm = "No Active Alarm"
@Field static final String EntranceDelayisActive = "Entrance Delay is Active"
@Field static final String AlarmAbortDelayActive = "Alarm Abort Delay Active"
@Field static final String FireAlarm = "Fire Alarm"
@Field static final String MedicalAlarm = "Medical Alarm"
@Field static final String PoliceAlarm = "Police Alarm"
@Field static final String BurgularAlarm = "Burgular Alarm"
@Field static final String AuxAlarm1 = "Aux Alarm 1"
@Field static final String AuxAlarm2 = "Aux Alarm 2"
@Field static final String AuxAlarm3 = "Aux Alarm 3"
@Field static final String AuxAlarm4 = "Aux Alarm 4"
@Field static final String CarbonMonoxide = "Carbon Monoxide"
@Field static final String EmergencyAlarm = "Emergency Alarm"
@Field static final String FreezeAlarm = "Freeze Alarm"
@Field static final String GasAlarm = "Gas Alarm"
@Field static final String HeatAlarm = "Heat Alarm"
@Field static final String WaterAlarm = "Water Alarm"
@Field static final String FireSupervisory = "Fire Supervisory"
@Field static final String FireVerified = "Fire Verified"

@Field final Map elkAlarmStates = [
		'0': NoActiveAlarm,
		'1': EntranceDelayisActive,
		'2': AlarmAbortDelayActive,
		'3': FireAlarm,
		'4': MedicalAlarm,
		'5': PoliceAlarm,
		'6': BurgularAlarm,
		'7': AuxAlarm1,
		'8': AuxAlarm2,
		'9': AuxAlarm3,
		':': AuxAlarm4,
		';': CarbonMonoxide,
		'<': EmergencyAlarm,
		'=': FreezeAlarm,
		'>': GasAlarm,
		'?': HeatAlarm,
		'@': WaterAlarm,
		'A': FireSupervisory,
		'B': FireVerified
]

// Zone Status Mapping Readable Text
@Field static final String NormalUnconfigured = "Normal: Unconfigured"
@Field static final String NormalOpen = "Normal: Open"
@Field static final String NormalEOL = "Normal: EOL"
@Field static final String NormalShort = "Normal: Short"
@Field static final String TroubleOpen = "Trouble: Open"
@Field static final String TroubleEOL = "Trouble: EOL"
@Field static final String TroubleShort = "Trouble: Short"
@Field static final String notused = "not used"
@Field static final String ViolatedOpen = "Violated: Open"
@Field static final String ViolatedEOL = "Violated: EOL"
@Field static final String ViolatedShort = "Violated: Short"
@Field static final String SoftBypassed = "Soft Bypassed"
@Field static final String BypassedOpen = "Bypassed: Open"
@Field static final String BypassedEOL = "Bypassed: EOL"
@Field static final String BypassedShort = "Bypassed: Short"

// Zone Status Mapping
@Field final Map elkZoneStatuses = [
		'0': NormalUnconfigured,
		'1': NormalOpen,
		'2': NormalEOL,
		'3': NormalShort,
		'5': TroubleOpen,
		'6': TroubleEOL,
		'7': TroubleShort,
		'8': notused,
		'9': ViolatedOpen,
		'A': ViolatedEOL,
		'B': ViolatedShort,
		'C': SoftBypassed,
		'D': BypassedOpen,
		'E': BypassedEOL,
		'F': BypassedShort

]

@Field static final String Off = "off"
@Field static final String On = "on"

@Field final Map elkOutputStates = [
		"0": Off,
		"1": On
]

@Field static final String Fahrenheit = "Fahrenheit"
@Field static final String Celcius = "Celcius"

@Field final Map elkTemperatureModes = [
		F: Fahrenheit,
		C: Celcius
]

@Field static final String User = "User"

@Field final Map elkUserCodeTypes = [
		1: User,
]

@Field static final String ZoneName = "Zone Name"
@Field static final String AreaName = "Area Name"
@Field static final String UserName = "User Name"
@Field static final String Keypad = "Keypad"
@Field static final String OutputName = "Output Name"
@Field static final String TaskName = "Task Name"
@Field static final String TelephoneName = "Telephone Name"
@Field static final String LightName = "Light Name"
@Field static final String AlarmDurationName = "Alarm Duration Name"
@Field static final String CustomSettings = "Custom Settings"
@Field static final String CountersNames = "Counters Names"
@Field static final String ThermostatNames = "Thermostat Names"
@Field static final String FunctionKey1Name = "FunctionKey1 Name"
@Field static final String FunctionKey2Name = "FunctionKey2 Name"
@Field static final String FunctionKey3Name = "FunctionKey3 Name"
@Field static final String FunctionKey4Name = "FunctionKey4 Name"
@Field static final String FunctionKey5Name = "FunctionKey5 Name"
@Field static final String FunctionKey6Name = "FunctionKey6 Name"


@Field final Map elkTextDescriptionsTypes = [
		'0' : ZoneName,
		'1' : AreaName,
		'2' : UserName,
		'3' : Keypad,
		'4' : OutputName,
		'5' : TaskName,
		'6' : TelephoneName,
		'7' : LightName,
		'8' : AlarmDurationName,
		'9' : CustomSettings,
		'10': CountersNames,
		'11': ThermostatNames,
		'12': FunctionKey1Name,
		'13': FunctionKey2Name,
		'14': FunctionKey3Name,
		'15': FunctionKey4Name,
		'16': FunctionKey5Name,
		'17': FunctionKey6Name
]


@Field static final String TemperatureProbe = "Temperature Probe"
@Field static final String Keypads = "Keypads"
@Field static final String Thermostats = "Thermostats"

@Field final Map elkTempTypes = [
		0: TemperatureProbe,
		1: Keypads,
		2: Thermostats
]

//NEW CODE
@Field static final String off = "off"
@Field static final String heat = "heat"
@Field static final String cool = "cool"
@Field static final String auto = "auto"
@Field static final String circulate = "circulate"
@Field static final String emergencyHeat = "emergency Heat"
@Field static final String False = "False"
@Field static final String True = "True"
@Field static final String FanAuto = "Fan Auto"
@Field static final String Fanturnedon = "Fan turned on"


@Field final Map elkThermostatMode = ['0': off, '1': heat, '2': cool, '3': auto, '4': emergencyHeat]
@Field final Map elkThermostatHold = ['0': False, '1': True]
@Field final Map elkThermostatFan = ['0': FanAuto, '1': Fanturnedon]

@Field final Map elkThermostatModeSet = [off: '0', heat: '1', cool: '2', auto: '3', 'emergency heat': '4']
@Field final Map elkThermostatFanModeSet = [auto: '0', on: '1', circulate: '0']


@Field final Map elkCommands = [

		Disarm                    : "a0",
		ArmAway                   : "a1",
		ArmHome                   : "a2",
		ArmStayInstant            : "a3",
		ArmNight                  : "a4",
		ArmNightInstant           : "a5",
		ArmVacation               : "a6",
		ArmStepAway               : "a7",
		ArmStepStay               : "a8",
		RequestArmStatus          : "as",
		AlarmByZoneRequest        : "az",
		RequestTemperatureData    : "lw",
		RequestRealTimeClockRead  : "rr",
		RealTimeClockWrite        : "rw",
		RequestTextDescriptions   : "sd",
		Speakphrase               : "sp",
		RequestSystemTroubleStatus: "ss",
		Requesttemperature        : "st",
		Speakword                 : "sw",
		TaskActivation            : "tn",
		RequestThermostatData     : "tr",
		SetThermostatData         : "ts",
		Requestusercodeareas      : "ua",
		requestVersionNumberofM1  : "vn",
		ReplyfromEthernettest     : "xk",
		Zonebypassrequest         : "zb",
		RequestZoneDefinitions    : "zd",
		Zonepartitionrequest      : "zp",
		RequestZoneStatus         : "zs",
		RequestZoneanalogvoltage  : "zv",
		SetThermostatData         : "ts",
		setHeatingSetpoint        : "ts",
		setCoolingSetpoint        : "ts",
		setThermostatSetpoint     : "ts",
		setThermostatFanMode      : "ts",
		setThermostatMode         : "ts",
		auto                      : "ts",
		cool                      : "ts",
		emergencyHeat             : "ts",
		fanAuto                   : "ts",
		fanCirculate              : "ts",
		fanOn                     : "ts",
		heat                      : "ts",
		off                       : "ts",
		ControlOutputOn           : "cn",
		ControlOutputOff          : "cf",
		ControlOutputToggle       : "ct",
		RequestOutputStatus       : "cs"
]

@Field static final String Disabled = "Disabled"
@Field static final String BurglarEntryExit1 = "Burglar Entry/Exit 1"
@Field static final String BurglarEntryExit2 = "Burglar Entry/Exit 2"
@Field static final String BurglarPerimeterInstant = "Burglar Perimeter Instant"
@Field static final String BurglarInterior = "Burglar Interior"
@Field static final String BurglarInteriorFollower = "Burglar Interior Follower"
@Field static final String BurglarInteriorNight = "Burglar Interior Night"
@Field static final String BurglarInteriorNightDelay = "Burglar Interior Night Delay"
@Field static final String Burglar24Hour = "Burglar 24 Hour"
@Field static final String BurglarBoxTamper = "Burglar Box Tamper"
@Field static final String Keyfob = "Key fob"
@Field static final String NonAlarm = "Non Alarm"
@Field static final String PoliceNoIndication = "Police No Indication"
@Field static final String KeyMomentaryArmDisarm = "Key Momentary Arm / Disarm"
@Field static final String KeyMomentaryArmAway = "Key Momentary Arm Away"
@Field static final String KeyMomentaryArmStay = "Key Momentary Arm Stay"
@Field static final String KeyMomentaryDisarm = "Key Momentary Disarm"
@Field static final String KeyOnOff = "Key On/Off"
@Field static final String MuteAudibles = "Mute Audibles"
@Field static final String PowerSupervisory = "Power Supervisory"
@Field static final String Temperature = "Temperature"
@Field static final String AnalogZone = "Analog Zone"
@Field static final String PhoneKey = "Phone Key"
@Field static final String IntercomKey = "Intercom Key"

@Field final Map elkZoneDefinitions = [
		'0': Disabled,
		'1': BurglarEntryExit1,
		'2': BurglarEntryExit2,
		'3': BurglarPerimeterInstant,
		'4': BurglarInterior,
		'5': BurglarInteriorFollower,
		'6': BurglarInteriorNight,
		'7': BurglarInteriorNightDelay,
		'8': Burglar24Hour,
		'9': BurglarBoxTamper,
		':': FireAlarm,
		';': FireVerified,
		'<': FireSupervisory,
		'=': AuxAlarm1,
		'>': AuxAlarm2,
		'?': Keyfob,
		'@': NonAlarm,
		'A': CarbonMonoxide,
		'B': EmergencyAlarm,
		'C': FreezeAlarm,
		'D': GasAlarm,
		'E': HeatAlarm,
		'F': MedicalAlarm,
		'G': PoliceAlarm,
		'H': PoliceNoIndication,
		'I': WaterAlarm,
		'J': KeyMomentaryArmDisarm,
		'K': KeyMomentaryArmAway,
		'L': KeyMomentaryArmStay,
		'M': KeyMomentaryDisarm,
		'N': KeyOnOff,
		'O': MuteAudibles,
		'P': PowerSupervisory,
		'Q': Temperature,
		'R': AnalogZone,
		'S': PhoneKey,
		'T': IntercomKey
]

//Not currently using this
//@Field static final String Disabled = "Disabled"
@Field static final String ContactBurglarEntryExit1 = "Contact"
@Field static final String ContactBurglarEntryExit2 = "Contact"
@Field static final String ContactBurglarPerimeterInstant = "Contact"
@Field static final String MotionBurglarInterior = "Motion"
@Field static final String MotionBurglarInteriorFollower = "Motion"
@Field static final String MotionBurglarInteriorNight = "Motion"
@Field static final String MotionBurglarInteriorNightDelay = "Motion"
@Field static final String AlertBurglar24Hour = "Alert"
@Field static final String AlertBurglarBoxTamper = "Alert"
@Field static final String AlertFireAlarm = "Alert"
@Field static final String AlertFireVerified = "Alert"
@Field static final String AlertFireSupervisory = "Alert"
@Field static final String AlertAuxAlarm1 = "Alert"
@Field static final String AlertAuxAlarm2 = "Alert"
@Field static final String AlertCarbonMonoxide = "Alert"
@Field static final String AlertEmergencyAlarm = "Alert"
@Field static final String AlertFreezeAlarm = "Alert"
@Field static final String AlertGasAlarm = "Alert"
@Field static final String AlertHeatAlarm = "Alert"
@Field static final String AlertMedicalAlarm = "Alert"
@Field static final String AlertPoliceAlarm = "Alert"
@Field static final String AlertPoliceNoIndication = "Alert"
@Field static final String AlertWaterAlarm = "Alert"

//Not currently using this
@Field final Map elkZoneTypes = [
//'0': Disabled,
'1': ContactBurglarEntryExit1,
'2': ContactBurglarEntryExit2,
'3': ContactBurglarPerimeterInstant,
'4': MotionBurglarInterior,
'5': MotionBurglarInteriorFollower,
'6': MotionBurglarInteriorNight,
'7': MotionBurglarInteriorNightDelay,
'8': AlertBurglar24Hour,
'9': AlertBurglarBoxTamper,
':': AlertFireAlarm,
';': AlertFireVerified,
'<': AlertFireSupervisory,
'=': AlertAuxAlarm1,
'>': AlertAuxAlarm2,
'A': AlertCarbonMonoxide,
'B': AlertEmergencyAlarm,
'C': AlertFreezeAlarm,
'D': AlertGasAlarm,
'E': AlertHeatAlarm,
'F': AlertMedicalAlarm,
'G': AlertPoliceAlarm,
'H': AlertPoliceNoIndication,
'I': AlertWaterAlarm
]

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.1.30
 * Added polling of device status once connected or reconnected to the panel.
 * Added Enable debug logging and Enable descriptionText logging to the main device.  Debug is no longer set for the
 *   driver from within the application.  Info logging can now be turned on or off for the main device.
 * Added zone, output and task reporting so child devices can register to be updated via the report command when another
 *   device status has changed.
 *   I am using this for a door control driver assigned to an output that needs to be aware of the state of the contact
 *   attached to the door.
 *
 * 0.1.29
 * Strongly typed variables for performance
 *
 * 0.1.28
 * Added ability to handle a zone with a Temperature Probe attached to the main board. Automatically assigned when
 *   the zone's description has the word "Temperature" in it or zone device can be manually changed.
 * Added the device type Keypad for temperature readings.
 * Both devices are assigned the Virtual Temperature Sensor driver.  This is an experimental feature.  Since the panel
 *   does not volunteer temperature data, it must be requested either manually for all devices using the Request Temperature
 *   Data button on the main driver or by setting up a rule to periodically execute the RequestTemperatureData command.
 * Added "ContactSensor" as a capability for HSM monitoring.  The contact will open if a Burglar Alarm or Police Alarm is triggered.
 * Changed the method of importing devices to greatly improve performance and reduce panel communication.
 * Fixed an issue not deleting child devices when the main driver is uninstalled.
 * Fixed an issue with the name when importing a device with the "Show On Keypad" flag set.
 *
 * 0.1.27
 * You can now change the port on the main device page.  You must click "Initialize" after you save preferences to take effect
 * Changed info logging to only happen if Enable descriptionText logging is turned on the device
 * Reenabled Request Zone Definition and Request Zone Status
 * Added Request Output Status
 *
 * 0.1.26
 * Added info logging when output or task status changes or Arm Mode changes
 * Added switch capability to main Elk M1 driver
 * Improved ArmStatus and AlarmState events to fire only when changed
 * Adding missing AlarmStates
 * Small tweaks to improve performance
 *
 * 0.1.25
 * Added sync of Elk M1 modes to Location Modes: Disarmed, Armed Away, Night, Stay, Vacation synced to modes Home, Away, Night,
 *    Stay (if available and Home if not), Vacation (if available and Away if not), respectively.
 * Added sync of hsmSetArm modes of disarm, armAway, armNight, ArmHome
 * Retooled zone-device creation to always create a device of type Virtual Contact unless "motion" is in the description.
 * Fixed issue that would create a lot of bogus devices when you connect to the panel with the M1 Touch Pro app
 * Added auto zone-device capability detection to support virtual devices with the following capabilities:
 *     ContactSensor, MotionSensor, SmokeDetector, CarbonMonoxideDetector, WaterSensor, TamperAlert, AccelerationSensor,
 *     Beacon, PresenceSensor, RelaySwitch, ShockSensor, SleepSensor, SoundSensor, Switch, TouchSensor and Valve
 * Changed registration of arm mode event to happen upon entry/exit timer expiration
 * Fixed issue of extra arm mode events firing when the panel is armed
 * Fixed status of EOL terminated zones
 * Added ability to read task status from the panel and set Contact device to open temporarily.
 * Tested use of the Elk C1M1 dual path communicator over local ethernet instead of using the M1XEP
 * Added setting of LastUser event which contains the user number who last triggered armed or disarmed
 *
 * 0.1.24
 * Rerouted some code for efficiency
 * Turned off some of the extra debugging
 *
 * 0.1.23
 * Outputs are now functional with Rule Machine
 * Change switch case to else if statements
 *
 * 0.1.22
 * Fixed code to show operating state and thermostat setpoint on dashboard tile
 * Reorder some code to see if it helps with some delay issues
 * Consolidated code for zone open and zone closed to see if it helps with some delay issues (need to check if this has any other impact elsewhere)
 *
 * 0.1.21
 * Updated mapping for output reporting code
 * Changed Reply Arming Status Report Data to work as Area 1 only and to report current states
 *
 * 0.1.20
 * Added back some code for 'Reply Arming Status Report Data (AS)' to clean up logging
 *
 * 0.1.19
 * Removed some code for 'Reply Arming Status Report Data (AS)' to clean up logging
 *
 * 0.1.18
 * Add support for Occupancy Sensors - this will be a work in progress since not sure how to code it
 *
 * 0.1.17
 * Changed devices 'isComponent' to 'False' - this will allow the removal of devices and changing of drivers
 *
 * 0.1.16
 * Changed the one import to be not case sensitive
 *
 * 0.1.15
 * Added support for manual inclusion of Elk M1 outputs and tasks
 * Added support for importing Elk M1 outputs, tasks and thermostats
 * Added better support for child devices (all communication goes through Elk M1 device)
 * Cleaned up some descriptions and instructions
 *
 * 0.1.14
 * Added support for importing Elk M1 zones
 * Fixed erroneous error codes
 * Added actuator capability to allow custom commands to work in dashboard and rule machine
 * Added command to request temperature data
 * 0.1.13
 * Elk M1 Code - No longer requires a 6 digit code (Add leading zeroes to 4 digit codes)
 * Outputs and tasks can now be entered as a number
 * Code clean up - removed some unused code
 * 0.1.12
 * Added support for outputs
 * 0.1.11
 * Built seperate thermostat child driver should allow for multiple thermostats
 * 0.1.10
 * Ability to control thermostat 1
 * 0.1.9
 * Minor changes
 * 0.1.8
 * Ability to read thermostat data (haven't confirmed multiple thermostat support)
 * Added additional mappings for thermostat support
 * Additional code clean up
 * 0.1.7
 * Rewrite of the various commands
 * 0.1.6
 * Changed text description mapping to string characters
 * 0.1.5
 * Added zone types
 * Added zone definitions
 * 0.1.4
 * Added additional command requests
 * 0.1.3
 * Receive status messages and interpret data
 * 0.1.2
 * Minor changes to script nomenclature and formating
 * 0.1.1
 * Abiltiy to connect Elk M1 and see data
 * Ability to send commands to Elk M1
 * Changed code to input parameter
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 * I - Area is hard coded to 1
 * F - Import Zone data from Elk
 * F - Controls for thermostat 1 (mulitple)
 * F - Activate elk task by name (via dashboard button)
 * F - Lighting support (this is low priority for me since HE is handling my Zwave lights)
 * F - Thermostat setup page (currenty uses the zone map page)
 * I - Fan Circulate, emergency heat and set schedule not supported
 * F - Request text descriptions for zone setup, tasks and outputs
 * I - A device with the same device network ID exists (this is really not an issue)
 *
 ***********************************************************************************************************************/