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

 *  A Special Thanks to Doug Beard for the framework of this driver!
 *
 *  I am not a programmer so a lot of this work is through trial and error. I also spent a good amount of time looking
 *  at other integrations on various platforms. I know someone else was working on an Elk driver that involved an ESP
 *  setup. This is a more direct route using equipment I already owned.
 *
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/

public static String version() { return "v0.2.4" }

import groovy.transform.Field

metadata {
	definition(name: "Elk M1 Driver", namespace: "belk", author: "Mike Magrann") {
		capability "Actuator"
		capability "Switch"
		capability "Initialize"
		capability "Telnet"
		capability "ContactSensor"
		capability "PushableButton"
		capability "TemperatureMeasurement"
		command "armAway"
		command "armStay"
		command "armStayInstant"
		command "armNight"
		command "armNightInstant"
		command "armVacation"
		command "chime"
		command "disarm"
		command "push", [[name: "button*", description: "1 - 6", type: "NUMBER"]]
		command "refreshArmStatus"
		command "refreshCounterValues"
		command "refreshCustomValues"
//		command "refreshKeypadArea"
		command "refreshLightingStatus"
		command "refreshOutputStatus"
		command "refreshTemperatureStatus"
		command "refreshTroubleStatus"
		command "refreshZoneStatus"
//This is used to run the zone import script
//		command "RequestTextDescriptions"
		command "requestZoneDefinitions"
		command "requestZoneVoltage", [[name: "zone*", description: "1 - 208", type: "NUMBER"]]
		command "sendMsg"
		command "showTextOnKeypads", [[name: "line1", description: "Max 16 characters", type: "STRING"],
									  [name: "line2", description: "Max 16 characters", type: "STRING"],
									  [name: "timeout*", description: "0 - 65535", type: "NUMBER"],
									  [name: "beep", type: "ENUM", constraints: ["no", "yes"]]]
		command "speakPhrase", [[name: "phraseNumber*", description: "1 - 319", type: "NUMBER"]]
		command "speakWord", [[name: "wordNumber*", description: "1 - 473", type: "NUMBER"]]
		command "zoneBypass", [[name: "zone*", description: "1 - 208", type: "NUMBER"]]
		command "zoneTrigger", [[name: "zone*", description: "1 - 208", type: "NUMBER"]]
		attribute "alarm", "string"
		attribute "alarmState", "string"
		attribute "armMode", "string"
		attribute "armState", "string"
		attribute "armStatus", "string"
		attribute "beep", "string"
		attribute "button1", "string"
		attribute "button2", "string"
		attribute "button3", "string"
		attribute "button4", "string"
		attribute "button5", "string"
		attribute "button6", "string"
		attribute "chime", "string"
		attribute "chimeMode", "string"
		attribute "f1LED", "string"
		attribute "f2LED", "string"
		attribute "f3LED", "string"
		attribute "f4LED", "string"
		attribute "f5LED", "string"
		attribute "f6LED", "string"
		attribute "lastUser", "string"
		attribute "trouble", "string"
	}
	preferences {
		input name: "ip", type: "text", title: "IP Address", required: true
		input name: "port", type: "number", title: "Port", range: 1..65535, required: true, defaultValue: 2101
		input name: "keypad", type: "number", title: "Keypad", range: 1..16, required: true, defaultValue: 1
		input name: "code", type: "text", title: "User code"
		input name: "timeout", type: "number", title: "Timeout in minutes", range: 0..1999, defaultValue: 0
		input name: "tempCelsius", type: "bool", title: "Temperatures in ˚C", defaultValue: false
		input name: "locationSet", type: "bool", title: "Set location mode", defaultValue: true
		input name: "dbgEnable", type: "bool", title: "Enable debug logging", defaultValue: false
		input name: "txtEnable", type: "enum", title: "Enable event logging for system +",
				options: ["none", "all", "keypad", "area"], defaultValue: "all", required: true
		input name: "switchArm", type: "enum", title: "Switch to arm mode",
				options: ["none", "away", "stay", "stayInstant", "night", "nightInstant", "vacation",
						  "nextAway", "nextStay", "forceAway", "forceStay"], defaultValue: "away", required: true
		input name: "switchDisarm", type: "bool", title: "Allow switch to disarm", defaultValue: true
		input name: "switchFully", type: "bool", title: "Switch on only when fully armed", defaultValue: true
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
		log.debug "${device.label}: Configuring IP: ${ip}, Port ${port}, Keypad ${keypad}, Code: ${code != ""}, Timeout: ${timeout}"
	sendEvent(name: "numberOfButtons", value: 6, type: "keypad")
	sendEvent(name: "button1", value: "F1", type: "keypad")
	sendEvent(name: "button2", value: "F2", type: "keypad")
	sendEvent(name: "button3", value: "F3", type: "keypad")
	sendEvent(name: "button4", value: "F4", type: "keypad")
	sendEvent(name: "button5", value: "F5", type: "keypad")
	sendEvent(name: "button6", value: "F6", type: "keypad")
	initialize()
}

def initialize() {
	if (state.alarmState != null) state.remove("alarmState")
	if (state.armState != null) state.remove("armState")
	if (state.armStatus != null) state.remove("armStatus")
	if (port == null)
		device.updateSetting("port", [type: "number", value: 2101])
	if (keypad == null)
		device.updateSetting("keypad", [type: "number", value: 1])
	if (timeout == null)
		device.updateSetting("timeout", [type: "number", value: 0])
	if (tempCelsius == null)
		device.updateSetting("tempCelsius", [type: "bool", value: "false"])
	if (locationSet == null)
		device.updateSetting("locationSet", [type: "bool", value: "true"])
	if (dbgEnable == null)
		device.updateSetting("dbgEnable", [type: "bool", value: "false"])
	if (txtEnable == null)
		device.updateSetting("txtEnable", [type: "text", value: "all"])
	if (switchArm == null)
		device.updateSetting("switchArm", [type: "text", value: "away"])
	if (switchDisarm == null)
		device.updateSetting("switchDisarm", [type: "bool", value: "true"])
	if (switchFully == null)
		device.updateSetting("switchFully", [type: "bool", value: "true"])
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
		heartbeat() // Start checking for telnet timeout
		refresh()
	}
}

def refresh() {
	List<hubitat.device.HubAction> cmds = []
	cmds.add(refreshKeypadArea())
	cmds.add(refreshVersionNumber())
	cmds.add(refreshTemperatureStatus())
	cmds.add(refreshArmStatus())
	cmds.add(refreshOutputStatus())
	cmds.add(refreshZoneStatus())
	cmds.add(refreshLightingStatus())
	cmds.add(refreshTroubleStatus())
	cmds.add(refreshAlarmStatus())
	cmds.add(requestKeypadStatus())
	cmds.add(requestKeypadPress())
	cmds.add(RequestTextDescriptions("12", keypad.toInteger()))
	cmds.add(RequestTextDescriptions("13", keypad.toInteger()))
	cmds.add(RequestTextDescriptions("14", keypad.toInteger()))
	cmds.add(RequestTextDescriptions("15", keypad.toInteger()))
	cmds.add(RequestTextDescriptions("16", keypad.toInteger()))
	cmds.add(RequestTextDescriptions("17", keypad.toInteger()))
	return delayBetween(cmds, 1000)
}

def uninstalled() {
	telnetClose()
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each { deleteChildDevice(it.deviceNetworkId) }
}

//Elk M1 Command Line Request - Start of
hubitat.device.HubAction off() {
	if (switchDisarm)
		disarm()
}

hubitat.device.HubAction on() {
	switch (switchArm) {
		case "away":
			armAway();
			break;
		case "stay":
			armStay();
			break;
		case "stayInstant":
			armStayInstant();
			break;
		case "night":
			armNight();
			break;
		case "nightInstant":
			armNightInstant();
			break;
		case "vacation":
			armVacation();
			break;
		case "nextAway":
			ArmNextAway();
			break;
		case "nextStay":
			ArmNextStay();
			break;
		case "forceAway":
			ArmForceAway();
			break;
		case "forceStay":
			ArmForceStay();
			break;
	}
}

hubitat.device.HubAction disarm(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} disarm"
	String cmd = elkCommands["Disarm"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armAway(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armAway"
	String cmd = elkCommands["ArmAway"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armStay(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armStay"
	def cmd = elkCommands["ArmStay"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armStayInstant(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armStayInstant"
	String cmd = elkCommands["ArmStayInstant"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armNight(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armNight"
	String cmd = elkCommands["ArmNight"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armNightInstant(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armNightInstant"
	String cmd = elkCommands["ArmNightInstant"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction armVacation(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} armVacation"
	String cmd = elkCommands["ArmVacation"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction ArmNextAway(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} ArmNextAway"
	String cmd = elkCommands["ArmNextAway"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction ArmNextStay(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} ArmNextStay"
	String cmd = elkCommands["ArmNextStay"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction ArmForceAway(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} ArmForceAway"
	String cmd = elkCommands["ArmForceAway"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction ArmForceStay(int sysArea = state.area, String sysCode = code) {
	if (dbgEnable)
		log.debug "${device.label} area ${sysArea} ArmForceStay"
	String cmd = elkCommands["ArmForceStay"]
	sendMsg(cmd, sysArea, sysCode)
}

hubitat.device.HubAction refreshVersionNumber() {
	if (dbgEnable)
		log.debug "${device.label} refreshVersionNumber"
	String cmd = elkCommands["RequestVersionNumber"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshArmStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshArmStatus"
	String cmd = elkCommands["RequestArmStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshTroubleStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshTroubleStatus"
	String cmd = elkCommands["RequestTroubleStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshAlarmStatus() {
	if (dbgEnable)
		log.debug "${device.label} requestAlarmStatus"
	String cmd = elkCommands["RequestAlarmStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshTemperatureStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshTemperatureStatus"
	String cmd = elkCommands["RequestTemperatureData"]
	sendMsg(cmd)
}

def RequestTextDescriptions(String deviceType, int startDev) {
	if (dbgEnable)
		log.debug "${device.label} RequestTextDescriptions Type: ${deviceType} Zone: ${startDev}"
	if (deviceType < "12" || deviceType > "17") {
		if (startDev == 1)
			state.creatingZone = true
		runIn(10, "stopCreatingZone")
	}
	String cmd = elkCommands["RequestTextDescriptions"] + deviceType + String.format("%03d", startDev) + "00"
	cmd = addChksum(Integer.toHexString(cmd.length() + 2).toUpperCase().padLeft(2, '0') + cmd)
	if (dbgEnable)
		log.debug "${device.label}: sending ${cmd}"
	sendHubCommand(new hubitat.device.HubAction(cmd, hubitat.device.Protocol.TELNET))
}

def stopCreatingZone() {
	state.creatingZone = false
}

hubitat.device.HubAction ControlOutputOn(BigDecimal output = 0, String time = "0") {
	if (dbgEnable)
		log.debug "${device.label} ControlOutputOn output ${output}"
	String cmd = elkCommands["ControlOutputOn"]
	cmd = cmd + String.format("%03d", output.intValue()) + time.padLeft(5, '0')
	sendMsg(cmd)
}

hubitat.device.HubAction ControlOutputOff(BigDecimal output = 0) {
	if (dbgEnable)
		log.debug "${device.label} ControlOutputOff output ${output}"
	String cmd = elkCommands["ControlOutputOff"]
	cmd = cmd + String.format("%03d", output.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction ControlOutputToggle(BigDecimal output = 0) {
	if (dbgEnable)
		log.debug "${device.label} ControlOutputToggle output ${output}"
	String cmd = elkCommands["ControlOutputToggle"]
	cmd = cmd + String.format("%03d", output.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction TaskActivation(BigDecimal task = 0) {
	if (dbgEnable)
		log.debug "${device.label} TaskActivation task: ${task}"
	String cmd = elkCommands["TaskActivation"]
	cmd = cmd + String.format("%03d", task.intValue())
	sendMsg(cmd)
}

hubitat.device.HubAction speakPhrase(BigDecimal phraseNumber = 0) {
	if (phraseNumber > 0 && phraseNumber < 320) {
		if (dbgEnable)
			log.debug "${device.label} speakPhrase ${phraseNumber}"
		String cmd = elkCommands["SpeakPhrase"] + String.format("%03d", phraseNumber.toInteger())
		sendMsg(cmd)
	}
}

hubitat.device.HubAction speakWord(BigDecimal wordNumber = 0) {
	if (wordNumber > 0 && wordNumber < 474) {
		if (dbgEnable)
			log.debug "${device.label} speakWord ${wordNumber}"
		String cmd = elkCommands["SpeakWord"] + String.format("%03d", wordNumber.toInteger())
		sendMsg(cmd)
	}
}

hubitat.device.HubAction refreshThermostatStatus(String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} refreshThermostatStatus tstat: ${thermostat}"
	String cmd = elkCommands["RequestThermostatData"]
	cmd = cmd + thermostat
	sendMsg(cmd)
}

hubitat.device.HubAction setThermostatMode(String thermostatmode, String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} setThermostatMode tstat: ${thermostat} mode ${thermostatmode}"
	String cmd = elkCommands["SetThermostatData"]
	String value = elkThermostatModeSet[thermostatmode].padLeft(2, '0')
	String element = "0"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setThermostatFanMode(String fanmode, String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} setThermostatFanMode tstat: ${thermostat} fanmode ${fanmode}"
	String cmd = elkCommands["SetThermostatData"]
	String value = elkThermostatFanModeSet[fanmode].padLeft(2, '0')
	String element = "2"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setThermostatHoldMode(String holdmode, String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} setThermostatHoldMode tstat: ${thermostat} hold ${holdmode}"
	String cmd = elkCommands["SetThermostatData"]
	String value = elkThermostatHoldModeSet[holdmode]
	String element = "1"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setThermostatTemperature(BigDecimal degrees, String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} setThermostatTemperature tstat: ${thermostat} hold ${degrees}"
	String cmd = elkCommands["SetThermostatData"]
	String value = String.format("%02d", degrees.intValue())
	String element = "3"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setCoolingSetpoint(BigDecimal degrees, String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} setCoolingSetpoint tstat: ${thermostat} temperature ${degrees}"
	String cmd = elkCommands["SetThermostatData"]
	String value = String.format("%02d", degrees.intValue())
	String element = "4"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setHeatingSetpoint(BigDecimal degrees, String thermostat) {
	if (dbgEnable)
		log.debug "${device.label} setHeatingSetpoint tstat: ${thermostat} temperature ${degrees}"
	String cmd = elkCommands["SetThermostatData"]
	String value = String.format("%02d", degrees.intValue())
	String element = "5"
	cmd = cmd + thermostat + value + element
	sendMsg(cmd)
}

hubitat.device.HubAction setCounterValue(BigDecimal counter, BigDecimal value) {
	if (counter >= 1 && counter <= 64 && value >= 0 && value <= 65535) {
		if (dbgEnable)
			log.debug "${device.label} setCounterValue counter: ${counter} value ${value}"
		String cmd = elkCommands["SetCounterValue"] + String.format("%02d", counter.intValue()) + String.format("%05d", value.intValue())
		sendMsg(cmd)
	}
}

hubitat.device.HubAction setCustomTime(BigDecimal custom, BigDecimal hours, BigDecimal minutes) {
	BigDecimal value = hours * 256 + minutes
	setCustomValue(custom, value)
}

hubitat.device.HubAction setCustomValue(BigDecimal custom, BigDecimal value) {
	if (custom >= 1 && custom <= 20 && value >= 0 && value <= 65535) {
		if (dbgEnable)
			log.debug "${device.label} setCustomValue custom: ${custom} value ${value}"
		String cmd = elkCommands["SetCustomValue"] + String.format("%02d", custom.intValue()) + String.format("%05d", value.intValue())
		sendMsg(cmd)
	}
}

hubitat.device.HubAction requestZoneDefinitions() {
	if (dbgEnable)
		log.debug "${device.label} requestZoneDefinitions"
	String cmd = elkCommands["RequestZoneDefinitions"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshZoneStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshZoneStatus"
	String cmd = elkCommands["RequestZoneStatus"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshOutputStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshOutputStatus"
	String cmd = elkCommands["RequestOutputStatus"]
	sendMsg(cmd)
}

def refreshCounterValues() {
	List<hubitat.device.HubAction> cmds = []
	int i
	for (i = 1; i <= 64; i += 1) {
		if (getChildDevice(device.deviceNetworkId + "_X_" + String.format("%02d", i)) != null)
			cmds.add(refreshCounterValue(i))
	}
	return delayBetween(cmds, 500)
}

hubitat.device.HubAction refreshCounterValue(BigDecimal counter) {
	if (dbgEnable)
		log.debug "${device.label} refreshCounterValue(${counter})"
	String cmd = elkCommands["RequestCounterValue"] + String.format("%02d", counter.toInteger())
	sendMsg(cmd)
}

hubitat.device.HubAction refreshCustomValues() {
	if (dbgEnable)
		log.debug "${device.label} refreshCustomValues"
	String cmd = elkCommands["RequestAllCustomValues"]
	sendMsg(cmd)
}

hubitat.device.HubAction refreshCustomValue(BigDecimal custom) {
	if (dbgEnable)
		log.debug "${device.label} refreshCustomValue(${custom})"
	String cmd = elkCommands["RequestCustomValue"] + String.format("%02d", custom.toInteger())
	sendMsg(cmd)
}

hubitat.device.HubAction zoneBypass(BigDecimal zoneNumber = 0, String sysCode = code) {
	if (zoneNumber > 0 && zoneNumber < 209) {
		String zoneNbr = String.format("%03d", zoneNumber.toInteger())
		if (dbgEnable)
			log.debug "${device.label} zone ${zoneNbr} zoneBypass"
		String cmd = elkCommands["ZoneBypass"]
		sendMsg(cmd, zoneNbr, sysCode)
	}
}

hubitat.device.HubAction zoneTrigger(BigDecimal zoneNumber = 0) {
	if (zoneNumber > 0 && zoneNumber < 209) {
		String zoneNbr = String.format("%03d", zoneNumber.toInteger())
		if (dbgEnable)
			log.debug "${device.label} zone ${zoneNbr} zoneTrigger"
		String cmd = elkCommands["ZoneTrigger"] + zoneNbr
		sendMsg(cmd)
	}
}

hubitat.device.HubAction requestZoneVoltage(BigDecimal zoneNumber = 0) {
	if (zoneNumber > 0 && zoneNumber < 209) {
		String zoneNbr = String.format("%03d", zoneNumber.toInteger())
		if (dbgEnable)
			log.debug "${device.label} zone ${zoneNbr} zoneVoltage"
		String cmd = elkCommands["ZoneVoltage"] + zoneNbr
		sendMsg(cmd)
	}
}

hubitat.device.HubAction controlLightingMode(String unitCode, String mode, String setting, String time) {
	if (dbgEnable)
		log.debug "${device.label} controlLightingMode ${unitCode} mode: ${mode} setting: ${setting} time: ${time}"
	String cmd = elkCommands["ControlLightingMode"] +
			unitCode + mode.padLeft(2, '0').take(2) + setting.padLeft(2, '0').take(2) + time.padLeft(4, '0').take(4)
	sendMsg(cmd)
}

hubitat.device.HubAction controlLightingOn(String unitCode = "A03") {
	if (dbgEnable)
		log.debug "${device.label} controlLightingOn: ${unitCode}"
	String cmd = elkCommands["ControlLightingOn"] + unitCode
	sendMsg(cmd)
}

hubitat.device.HubAction controlLightingOff(String unitCode) {
	if (dbgEnable)
		log.debug "${device.label} controlLightingOff: ${unitCode}"
	String cmd = elkCommands["ControlLightingOff"] + unitCode
	sendMsg(cmd)
}

hubitat.device.HubAction controlLightingToggle(String unitCode) {
	if (dbgEnable)
		log.debug "${device.label} controlLightingToggle: ${unitCode}"
	String cmd = elkCommands["ControlLightingToggle"] + unitCode
	sendMsg(cmd)
}

hubitat.device.HubAction chime(int keypadNumber = keypad) {
	push(7, keypadNumber)
}

hubitat.device.HubAction push(BigDecimal button, int keypadNumber = keypad) {
	String buttonCode = button.toString()
	if (button == 7)
		buttonCode = "C"
	if (button == 8)
		buttonCode = "*"
	if (button >= 1 && button <= 8) {
		requestKeypadPress(buttonCode, keypadNumber)
	}
}

hubitat.device.HubAction refreshLightingStatus() {
	if (dbgEnable)
		log.debug "${device.label} refreshLightingStatus"
	runIn(1, "refreshLightingStatus", [data: "0"])
	pauseExecution(1100)
	runIn(1, "refreshLightingStatus", [data: "1"])
	pauseExecution(1100)
	runIn(1, "refreshLightingStatus", [data: "2"])
	pauseExecution(1100)
	runIn(1, "refreshLightingStatus", [data: "3"])
}

hubitat.device.HubAction refreshLightingStatus(String unitCode) {
	String bank = unitCode.take(1)
	if (bank >= "A" && bank <= "D")
		bank = "0"
	else if (bank >= "E" && bank <= "H")
		bank = "1"
	else if (bank >= "I" && bank <= "L")
		bank = "2"
	else if (bank >= "M" && bank <= "P")
		bank = "3"

	if (dbgEnable)
		log.debug "${device.label} refreshLightingStatus: ${bank}"
	String cmd = elkCommands["RequestLightingStatus"] + bank
	sendMsg(cmd)
}

hubitat.device.HubAction refreshKeypadArea() {
	if (dbgEnable)
		log.debug "${device.label} refreshKeypadArea"
	String cmd = elkCommands["RequestKeypadArea"]
	sendMsg(cmd)
}

hubitat.device.HubAction requestKeypadPress(String key = "0", int keypadNumber = keypad) {
	if (dbgEnable)
		log.debug "${device.label} requestKeypadPress"
	String cmd = elkCommands["RequestKeypadPress"] + String.format("%02d", keypadNumber) + key
	sendMsg(cmd)
}

hubitat.device.HubAction requestKeypadStatus(String keypadNumber) {
	if (dbgEnable)
		log.debug "${device.label} requestKeypadStatus"
	String cmd = elkCommands["RequestKeypadStatus"] + keypadNumber
	sendMsg(cmd)
}

hubitat.device.HubAction showTextOnKeypads(BigDecimal time, String beep, int sysArea = state.area) {
	return showTextOnKeypads("", "", time, beep, sysArea)
}

hubitat.device.HubAction showTextOnKeypads(String line1, BigDecimal time, String beep, int sysArea = state.area) {
	return showTextOnKeypads(line1, "", time, beep, sysArea)
}

hubitat.device.HubAction showTextOnKeypads(String line1 = "", String line2 = "", BigDecimal time = 0, String beep = "no", int sysArea = state.area) {
	log.debug received
	if (dbgEnable)
		log.debug "${device.label} showTextOnKeypads: line1 ${line1}, line2 ${line2}, time ${time}, beep ${beep}, area ${sysArea}"
	if (line1.length() == 0) {
		line1 = line2
		line2 = ""
	}
	String clear = (time == 0 ? "1" : "2")
	String hasBeep = (beep == "yes" ? "1" : "0")
	int duration = (time < 1 ? 0 : time > 65535 ? 65535 : time.intValue())
	String first = ""
	String second = ""
	if (line1.length() > 16)
		first = line1.substring(0, 16)
	else if (line1.length() == 0)
		clear = "0"
	else if (line1.length() < 16)
		first = line1 + "^"
	if (line2.length() > 16)
		second = line2.substring(0, 16)
	else if (line2.length() > 0 && line2.length() < 16)
		second = line2 + "^"
	String cmd = elkCommands["ShowTextOnKeypads"] + sysArea.toString().padLeft(1, '0') + clear + hasBeep + String.format("%05d", duration) +
			first.padRight(16, ' ') + second.padRight(16, ' ')
	sendMsg(cmd)
}
//Elk M1 Command Line Request - End of


//Elk M1 Message Send Lines - Start of
hubitat.device.HubAction sendMsg(String cmd, int sysArea, String sysCode = null) {
	if (sysArea > 0 && sysArea < 9)
		sendMsg(cmd, sysArea.toString().take(1), sysCode)
	else
		sendMsg(cmd)
}

hubitat.device.HubAction sendMsg(String cmd, String zoneNumber = null, String sysCode = null) {
	String msg
	if (zoneNumber == null || sysCode == null)
		msg = cmd + "00"
	else
		msg = cmd + zoneNumber + sysCode.padLeft(6, '0').take(6) + "00"
	String msgStr = addChksum(Integer.toHexString(msg.length() + 2).toUpperCase().padLeft(2, '0') + msg)
	if (dbgEnable)
		log.debug "${device.label} sendMsg: $msgStr"
	return new hubitat.device.HubAction(msgStr, hubitat.device.Protocol.TELNET)
}

hubitat.device.HubAction sendMsg(hubitat.device.HubAction action = null) {
	return action
}

String addChksum(String msg) {
	char[] msgArray = msg.toCharArray()
	int msgSum = 0
	msgArray.each { (msgSum += (int) it) }
	String chkSumStr = Integer.toHexString(256 - (msgSum % 256)).toUpperCase().padLeft(2, '0')
	if (chkSumStr.length() == 2)
		return msg + chkSumStr
	else
		return msg + chkSumStr.substring(1)
}

//Elk M1 Message Send Lines - End of


//Elk M1 Event Receipt Lines
private List<Map> parse(String message) {
	List<Map> statusList = null
	if (dbgEnable)
		log.debug "${device.label} Parsing Incoming message: " + message

	switch (message.substring(2, 4)) {
		case "ZC":
			zoneChange(message);
			break;
		case "XK":
			heartbeat();
			break;
		case "CC":
			outputChange(message);
			break;
		case "TC":
			taskChange(message);
			break;
		case "EE":
			statusList = entryExitChange(message);
			break;
		case "AS":
			statusList = armStatusReport(message);
			break;
		case "ZS":
			zoneStatusReport(message);
			break;
		case "CS":
			outputStatus(message);
			break;
		case "DS":
			lightingDeviceStatus(message);
			break;
		case "PC":
			lightingDeviceChange(message);
			break;
		case "LW":
			statusList = temperatureData(message);
			break;
		case "ST":
			statusList = statusTemperature(message);
			break;
		case "TR":
			thermostatData(message);
			break;
		case "IC":
			statusList = userCodeEntered(message);
			break;
		case "EM":
			statusList = sendEmail(message);
			break;
		case "AR":
			alarmReporting(message);
			break;
		case "PS":
			lightingBankStatus(message);
			break;
		case "LD":
			logData(message);
			break;
		case "SD":
			statusList = stringDescription(message);
			break;
		case "AM":
			statusList = updateAlarmAreas(message);
			break;
		case "AZ":
			updateAlarmZones(message);
			break;
		case "KA":
			keypadAreaAssignments(message);
			break;
		case "KC":
			statusList = keypadKeyChangeUpdate(message);
			break;
		case "KF":
			statusList = keypadFunctionKeyUpdate(message);
			break;
		case "VN":
			versionNumberReport(message);
			break;
		case "ZD":
			zoneDefinitionReport(message);
			break;
		case "IE":
			refresh();
			break;
		case "RP":
			connectionStatus(message);
			break;
		case "SS":
			statusList = updateSystemTrouble(message);
			break;
		case "AP":
			receiveTextString(message);
			break;
		case "CR":
			updateCustom(message);
			break;
		case "CV":
			updateCounter(message);
			break;
		case "ZV":
			zoneVoltage(message);
			break;
		case "UA":
		case "RR":
			break;
		default:
			if (txtEnable) log.info "${device.label}: The ${message.substring(2, 4)} command is unknown";
			break;
	}
	if (statusList != null && statusList.size() > 0) {
		//log.debug "Trying: ${statusList}"
		List<Map> rtn = []
		statusList.each {
			if (device.currentState(it.name)?.value == null || device.currentState(it.name).value != it.value) {
				if (it.descriptionText == null)
					it.descriptionText = device.label + " " + it.name + " was " + it.value
				else
					it.descriptionText == device.label + " " + it.descriptionText
				rtn << createEvent(it)
				if ((txtEnable == "all" || it?.type == "system" || txtEnable == it?.type) && it.name != "armState" && it.name != "contact" &&
						it.name != "switch" && it.name != "trouble" && it.name != "temperature" && (it.isStateChange == null || it.isStateChange))
					log.info it.descriptionText
			}
		}
		statusList = rtn
	}
	return statusList
}

def zoneChange(String message) {
	String zoneNumber = message.substring(4, 7)
	String zoneStatusCode = message.substring(7, 8)
	String zoneStatus = elkZoneStatuses[zoneStatusCode]
	switch (zoneStatusCode) {
		case "1":
		case "2":
		case "3":
			zoneNormal(zoneNumber, zoneStatus);
			break;
		case "9":
		case "A":
		case "B":
			zoneViolated(zoneNumber, zoneStatus);
			break;
		case "5":
		case "6":
		case "7":
		case "C":
		case "D":
		case "E":
		case "F":
			zoneTrouble(zoneNumber, zoneStatus);
			break;
		default:
			if (dbgEnable) log.debug "${device.label} Unknown zone status: zone ${zoneStatus} - ${zoneStatus}";
			break;
	}
}

def zoneVoltage(String message) {
	String zoneNumber = message.substring(4, 7)
	BigDecimal zoneVoltageNumber = new BigDecimal(message.substring(7, 10)) / 10
	def zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}")
	if (zoneDevice != null) {
		String description = "${zoneDevice.label} voltage was ${zoneVoltageNumber}"
		zoneDevice.sendEvent(name: "voltage", value: zoneVoltageNumber, descriptionText: description, unit: "V")
		if (txtEnable != "none")
			log.info description
	} else if (dbgEnable) {
		log.debug "${device.label} zone ${zoneNumber} voltage was ${zoneVoltageNumber}";
	}
}

List<Map> entryExitChange(String message) {
	List<Map> statusList = null
	int sysArea = message.substring(4, 5).toInteger()
	boolean isEntry = (message.substring(5, 6) != "0")
	String exitTime = message.substring(6, 12)
	String armStatus
	if (exitTime == "000000" || isEntry) {
		armStatus = elkArmStatuses[message.substring(12, 13)]
	} else {
		armStatus = elkArmingStatuses[message.substring(12, 13)]
	}
	if (dbgEnable)
		log.debug "${device.label} Area: $sysArea, Time: $exitTime, Entry: $isEntry, armStatus: $armStatus"
	if (state.keypadAreas != null && state.keypadAreas[sysArea.toString()] != null) {
		statusList = updateAreaStatus(sysArea, setStatus(sysArea, armStatus))
		if (exitTime != "000000" && isEntry && armStatus != Disarmed && sysArea == state.area) {
			parent.speakEntryDelay()
		}
	}
	return statusList
}

List<Map> armStatusReport(String message) {
	List<Map> statusList = null
	Integer i
	String armStatus
	String armUpState
	String alarmState
	Map armEvent
	Map alarmEvent
	Map contactEvent
	for (i = 1; i <= 8; i += 1) {
		if (state.keypadAreas != null && state.keypadAreas[i.toString()] != null) {
			armStatus = elkArmStatuses[message.substring(3 + i, 4 + i)]
			armUpState = elkArmUpStates[message.substring(11 + i, 12 + i)]
			alarmState = elkAlarmStates[message.substring(19 + i, 20 + i)]
			if (dbgEnable && i == state.area) {
				log.debug "${device.label} Area ${i} armStatus: ${armStatus} armState: ${armUpState} alarmState: ${alarmState}"
			}

			armEvent = [name: "armState", value: armUpState, type: "area"]
			alarmEvent = [name: "alarmState", value: alarmState, type: "area"]
			if (alarmState == PoliceAlarm || alarmState == BurgularAlarm) {
				contactEvent = [name: "contact", value: "open", type: "area"]
				//if (sysArea == state.area) {
				parent.speakAlarm()
				//}
			} else {
				contactEvent = [name: "contact", value: "closed", type: "area"]
			}

			List<Map> statuses = [armEvent, alarmEvent, contactEvent]
			if (armStatus == Disarmed)
				statuses += setStatus(i, armStatus)
			statusList = updateAreaStatus(i, statuses)
		}
	}
	return statusList
}

List<Map> setStatus(int sysArea, String armStatus) {
	String armMode = "Home"
	String hsmSetArm = "disarm"
	Map statusEvent = [name: "armStatus", value: armStatus, type: "area"]
	Map switchEvent
	boolean armChange = false
	if (sysArea == state.area && (device.currentState("armStatus")?.value == null || device.currentState("armStatus").value != armStatus)) {
		armChange = true
	}
	switch (armStatus) {
		case Disarmed:
			switchEvent = [name: "switch", value: "off", type: "area"]
			break
		case ArmedAway:
			armMode = "Away"
			hsmSetArm = "armAway"
			if (switchFully)
				switchEvent = [name: "switch", value: "on", type: "area"]
			break
		case ArmedStay:
		case ArmedStayInstant:
			armMode = "Stay"
			hsmSetArm = "armHome"
			if (switchFully)
				switchEvent = [name: "switch", value: "on", type: "area"]
			break
		case ArmedNight:
		case ArmedNightInstant:
			armMode = "Night"
			hsmSetArm = "armNight"
			if (switchFully)
				switchEvent = [name: "switch", value: "on", type: "area"]
			break
		case ArmedVacation:
			armMode = "Vacation"
			hsmSetArm = "armAway"
			if (switchFully)
				switchEvent = [name: "switch", value: "on", type: "area"]
			break
		case ArmingAway:
			if (armChange)
				parent.speakArmingAway()
			if (!switchFully)
				switchEvent = [name: "switch", value: "on", type: "area"]
			break
		case ArmingStay:
		case ArmingStayInstant:
			if (armChange)
				parent.speakArmingStay()
			if (!switchFully)
				switchEvent = [name: "switch", value: "on", type: "area"]
			break
		case ArmingNight:
		case ArmingNightInstant:
			if (armChange)
				parent.speakArmingNight()
			if (!switchFully)
				switchEvent = [name: "switch", value: "on", type: "area"]
			break
		case ArmingVacation:
			if (armChange)
				parent.speakArmingVacation()
			if (!switchFully)
				switchEvent = [name: "switch", value: "on", type: "area"]
			break
	}
	Map modeEvent = [name: "armMode", value: armMode, type: "area"]
	if (sysArea == state.area && (device.currentState("armMode")?.value == null || device.currentState("armMode").value != armMode)) {
		if (locationSet) {
			def allmodes = location.getModes()
			int idx = allmodes.findIndexOf { it.name == armMode }
			if (idx == -1 && armMode == "Vacation") {
				idx = allmodes.findIndexOf { it.name == "Away" }
			} else if (idx == -1 && armMode == "Stay") {
				idx = allmodes.findIndexOf { it.name == "Home" }
			}
			if (idx != -1) {
				String curmode = location.currentMode.name
				String newmode = allmodes[idx].name
				location.setMode(newmode)
				if (dbgEnable)
					log.debug "${device.label}: Location Mode changed from $curmode to $newmode"
			}
		}
		parent.setHSMArm(hsmSetArm, device.label + " was armed " + armMode)
	}
	if (switchEvent) {
		return [statusEvent, switchEvent, modeEvent]
	} else {
		return [statusEvent, modeEvent]
	}
}

List<Map> updateAreaStatus(int sysArea, List<Map> areaStatus) {
	List<Map> statusList = []
	if (state.keypadAreas != null) {
		List<String> keypads = state.keypadAreas[sysArea.toString()]
		if (keypads != null) {
			keypads.each {
				if (it == "00") {
					statusList = areaStatus
				} else {
					def keypadDevice = getChildDevice(device.deviceNetworkId + "_P_0" + it)
					if (keypadDevice != null && keypadDevice.hasCapability("Actuator"))
						keypadDevice.parse(areaStatus)
				}
			}
		}
	}
	return statusList
}

List<Map> updateKeypadStatus(String keypadNumber, List<Map> keypadStatus) {
	List<Map> statusList = []
	if (keypadNumber.toInteger() == keypad) {
		statusList = keypadStatus
	}
	def keypadDevice = getChildDevice("${device.deviceNetworkId}_P_0${keypadNumber}")
	if (keypadDevice != null) {
		if (keypadDevice.hasCapability("Actuator")) {
			keypadDevice.parse(keypadStatus)
		} else if (keypadStatus.first().name == "temperature") {
			Map eventMap = keypadStatus.first()
			if (eventMap.descriptionText == null)
				eventMap.descriptionText = keypadDevice.label + " " + eventMap.name + " was " + eventMap.value
			else
				eventMap.descriptionText == keypadDevice.label + " " + eventMap.descriptionText
			keypadDevice.sendEvent(eventMap)
		}
	}
	return statusList
}

List<Map> userCodeEntered(String message) {
	List<Map> statusList = null
	String userCode = message.substring(4, 16)
	String userNumber = message.substring(16, 19)
	String keypadNumber = message.substring(19, 21)
	if (userCode == "000000000000") {
		if (txtEnable != "none")
			log.info "${device.label} lastUser was: ${userNumber} on keypad ${keypadNumber}"
		Map userEvent = [name: "lastUser", value: userNumber, type: "keypad"]
		statusList = [userEvent]
		def keypadDevice = getChildDevice(device.deviceNetworkId + "_P_0" + keypadNumber)
		if (keypadDevice != null && keypadDevice.hasCapability("Actuator"))
			keypadDevice.parse([userEvent])
	} else {
		statusList = updateKeypadStatus(keypadNumber, [[name: "invalidUser", value: userCode, type: "keypad"]])
	}
	return statusList
}

List<Map> sendEmail(String message) {
	int emailNumber = message.substring(4, 7).toInteger()
	return [[name: "sendEmail", value: emailNumber, type: "system", descriptionText: "${device.label} sent email # ${emailNumber}"]]
}

def alarmReporting(String message) {
	String accountNumber = message.substring(4, 10)
	String alarmCode = message.substring(10, 14)
	int alarmArea = message.substring(14, 16).toInteger()
	String zone = message.substring(16, 19)
	String telIp = message.substring(19, 20)
	log.warn "${device.label} AlarmReporting account: ${accountNumber}, code: ${alarmCode}, area ${alarmArea}, zone ${zone}, device ${telIp}"
}

def outputChange(String message) {
	String outputNumber = message.substring(4, 7)
	String outputState = elkStates[message.substring(7, 8)]
	def zoneDevice = getChildDevice("${device.deviceNetworkId}_O_${outputNumber}")
	if (zoneDevice != null) {
		if (dbgEnable)
			log.debug "${device.label} outputChange: ${outputNumber} - ${outputState}"
		zoneDevice.parse(outputState)
		if (state.outputReport != null) {
			state.outputReport = sendReport(state.outputReport, zoneDevice, outputNumber, outputState == "on")
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
		//	log.debug "${device.label}: OutputStatus: Output " + i + " - " + elkStates[outputState]
		outputChange("    " + String.format("%03d", i) + outputState)
	}
}

def lightingDeviceStatus(String message) {
	int deviceNumber = message.substring(4, 7).toInteger()
	String level = message.substring(7, 9)
	int ndx = (deviceNumber - 1) / 16
	int unitNumber = deviceNumber - ndx * 16
	lightingDeviceChange("    " + "ABCDEFGHIJKLMNOP".substring(ndx, ndx + 1) + String.format("%02d", unitNumber) + level)
}

def lightingBankStatus(String message) {
	int bank = message.substring(4, 5).toInteger()
	char[] statusString = message.substring(5, 69).toCharArray()
	String groups = "ABCDEFGHIJKLMNOP".substring(bank * 4, bank * 4 + 4)
	String search = device.deviceNetworkId + "_L_"
	int len = search.length()
	String childDeviceDNID
	int ndx
	int level
	getChildDevices().each {
		childDeviceDNID = it.deviceNetworkId
		if (childDeviceDNID.substring(0, len) == search) {
			ndx = groups.indexOf(childDeviceDNID.substring(len, len + 1))
			if (ndx != -1) {
				level = ((int) statusString[childDeviceDNID.substring(len + 1, len + 3).toInteger() + ndx * 16 - 1]) - 48
				lightingDeviceChange("    " + childDeviceDNID.substring(len, len + 3) + String.format("%02d", level))
			}
		}
	}
}

def lightingDeviceChange(String message) {
	String unitCode = message.substring(4, 7)
	String level = message.substring(7, 9)
	if (dbgEnable)
		log.debug "${device.label} Light: ${unitCode} Level: ${level}"
	getChildDevice("${device.deviceNetworkId}_L_${unitCode}")?.parse(level)
}

def logData(String message) {
	if (message.substring(20, 23) == "000") {
		String eventDesc = elkLogData[message.substring(4, 8)]
		if (eventDesc != null) {
			String sysArea = message.substring(11, 12)
			if (sysArea == "0")
				sysArea = ""
			else
				sysArea = " Area ${sysArea}"
			String sysNumber = message.substring(8, 11)
			if (sysNumber == "000")
				sysNumber = ""
			else if (sysNumber.substring(0, 2) == "00")
				sysNumber = sysNumber.substring(2)
			else if (sysNumber.substring(0, 1) == "0")
				sysNumber = sysNumber.substring(1)
			log.warn "${device.label}${sysArea}: ${eventDesc} ${sysNumber}"
		}
	}
}

List<Map> stringDescription(String message) {
	List<Map> statusList = null
	String zoneNumber = message.substring(6, 9)
	if (zoneNumber != "000") {
		String zoneName
		String zoneType = message.substring(4, 6)
		byte firstText = message.substring(9, 10)
		String zoneText = (String) ((char) (firstText & 0b01111111)) + message.substring(10, 25).trim()
		// Mask high order "Show On Keypad" bit in first letter
		if (zoneText != "") {
			if (zoneType >= "12" && zoneType <= "17")
				statusList = updateFKeyName(zoneNumber.substring(1), zoneType, zoneText.trim())
			else if (state.creatingZone)
				createZone([zoneNumber: zoneNumber, zoneName: zoneName, zoneType: zoneType, zoneText: zoneText])
		}
		if (state.creatingZone) {
			int i = zoneNumber.toInteger() // Request next zone description
			if (i < 208) {
				RequestTextDescriptions(zoneType, i + 1)
			}
		}
	}
	return statusList
}

List<Map> temperatureData(String message) {
	List<Map> statusList = null
	String temp
	int i
	int zoneNumber
	for (i = 4; i <= 50; i += 3) {
		temp = message.substring(i, i + 3)
		if (temp != "000") {
			zoneNumber = (i - 1) / 3
			statusList = statusTemperature("    1" + String.format("%02d", zoneNumber) + temp + "    ")
		}
	}

	for (i = 52; i <= 98; i += 3) {
		temp = message.substring(i, i + 3)
		if (temp != "000") {
			zoneNumber = (i - 49) / 3
			statusTemperature("    0" + String.format("%02d", zoneNumber) + temp + "    ")
		}
	}
	return statusList
}

List<Map> statusTemperature(String message) {
	List<Map> statusList = null
	String group = elkTempTypes[message.substring(4, 5).toInteger()]
	String zoneNumber = message.substring(5, 7)
	int temp = message.substring(7, 10).toInteger()
	String uom = tempCelsius != null && tempCelsius == true ? "˚C" : "˚F"
	def zoneDevice
	if (group == TemperatureProbe) {
		temp = temp - 60
		if (dbgEnable)
			log.debug "${device.label} Zone ${zoneNumber} temperature was ${temp} ${uom}"
		zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_0${zoneNumber}")
		if (zoneDevice?.hasCapability("TemperatureMeasurement")) {
			zoneDevice.sendEvent(name: "temperature", value: temp, unit: uom, descriptionText: "${device.label} temperature was ${temp} ${uom}")
		}
	} else if (group == Keypads) {
		temp = temp - 40
		if (dbgEnable)
			log.debug "${device.label} Keypad ${zoneNumber} temperature is ${temp} ${uom}"
		statusList = updateKeypadStatus(zoneNumber, [[name: "temperature", value: temp, unit: uom, type: "keypad"]])
	} else if (group == Thermostats) {
		if (dbgEnable)
			log.debug "${device.label} Thermostat ${zoneNumber} temperature is ${temp} ${uom}"
		zoneDevice = getChildDevice("${device.deviceNetworkId}_T_0${zoneNumber}")
		if (zoneDevice?.hasCapability("Thermostat")) {
			zoneDevice.sendEvent(name: "temperature", value: temp, unit: uom, descriptionText: "${device.label} temperature was ${temp} ${uom}")
		}
	}
	return statusList
}

def taskChange(String message) {
	String taskNumber = message.substring(4, 7)
	def zoneDevice = getChildDevice(device.deviceNetworkId + "_K_" + taskNumber)
	if (zoneDevice != null) {
		if (dbgEnable)
			log.debug "${device.label} Task Change Update: ${taskNumber}"
		zoneDevice.parse()
		if (state.taskReport != null) {
			state.taskReport = sendReport(state.taskReport, zoneDevice, taskNumber, true)
		}
	}
}

def thermostatData(String message) {
	String thermNumber = message.substring(4, 6).padLeft(3, '0')
	def zoneDevice = getChildDevice(device.deviceNetworkId + "_T_" + thermNumber)
	if (zoneDevice != null) {
		if (dbgEnable)
			log.debug "${device.label} thermostatData: ${thermNumber} - ${message.substring(6, 17)}"
		String uom = tempCelsius != null && tempCelsius == true ? "˚C" : "˚F"
		zoneDevice.parse(uom + message.substring(2))
	}
}

def heartbeat() {
	if (timeout != null && timeout.toString().isInteger() && timeout >= 1)
		runIn(timeout * 60, "telnetTimeout")
}

def keypadAreaAssignments(String message) {
	int myArea = 0
	int sysArea
	int i
	Map areas = [:]
	for (i = 1; i <= 16; i += 1) {
		sysArea = message.substring(i + 3, i + 4).toInteger()
		if (sysArea != 0) {
			if (i == keypad)
				myArea = sysArea
			String keypadNumber = String.format("%02d", i)
			def keypadDevice = getChildDevice("${device.deviceNetworkId}_P_0${keypadNumber}")
			if (keypadDevice != null && keypadDevice.hasCapability("Actuator")) {
				if (areas[sysArea] == null)
					areas[sysArea] = []
				areas[sysArea] << keypadNumber
				keypadDevice.parse([[name: "area", value: sysArea, type: "keypad"]])
			}
		}
	}
	if (myArea == 0) {
		log.warn "${device.label} warning: keypad not found"
	} else {
		if (areas[myArea] == null)
			areas[myArea] = ["00"]
		else
			areas[myArea] = ["00"] + areas[myArea]
		state.area = myArea
		state.keypadAreas = areas
	}
	if (dbgEnable)
		log.debug "Keypad Area Assignments ${areas}"
}

List<Map> keypadKeyChangeUpdate(String message) {
	String keypadNumber = message.substring(4, 6)
	String key = elkKeys[message.substring(6, 8)]
	List<Map> statusList = []
	List<Map> statuses = []
	int i
	if (key != NOTHING) {
		statuses << [name: "pushed", value: key, type: "keypad", descriptionText: "pushed ${key}"]
	}
	for (i = 1; i <= 6; i++) {
		statuses << [name: "f${i.toString()}LED", value: elkStates[message.substring(i + 7, i + 8)], type: "keypad"]
	}
	if (statuses.size() > 0)
		statusList += updateKeypadStatus(keypadNumber, statuses)

	byte[] chimes = message.substring(14, 23).getBytes()
	for (i = 1; i <= 8; i++) {
		if (state.keypadAreas != null && state.keypadAreas[i.toString()] != null) {
			statuses = []
			if (chimes[i] == 0x30) {
				statuses << [name: "beep", value: "off", type: "area"]
				statuses << [name: "chime", value: "off", type: "area"]
			} else {
				if (chimes[i] & 2)
					statuses << [name: "beep", value: "beeping", type: "area"]
				else if (chimes[i] & 1)
					statuses << [name: "beep", value: "beeped", type: "area"]
				if (chimes[i] & 4)
					statuses << [name: "chime", value: "chimed", type: "area"]
			}
			if (statuses.size() > 0)
				statusList += updateAreaStatus(i, statuses)
		}
	}
	return statusList
}

List<Map> keypadFunctionKeyUpdate(String message) {
	String keypadNumber = message.substring(4, 6)
	String key = elkFKeys[message.substring(6, 7)]
	List<Map> statusList = []
	if (key != NOTHING) {
		statusList += updateKeypadStatus(keypadNumber, [[name: "pushed", value: key, type: "keypad", descriptionText: "pushed ${key}"]])
	}
	int i
	for (i = 1; i <= 8; i++) {
		if (state.keypadAreas != null && state.keypadAreas[i.toString()] != null) {
			statusList += updateAreaStatus(i, [[name: "chimeMode", value: elkChimeStates[message.substring(i + 6, i + 7)], type: "area"]])
		}
	}
	return statusList
}

def versionNumberReport(String message) {
	if (dbgEnable)
		log.debug "versionNumberReport"
	BigInteger m1Version = new BigInteger(message.substring(4, 10), 16)
	BigInteger xepVersion = new BigInteger(message.substring(10, 16), 16)
	int m1U = m1Version / 65536
	int m1M = (m1Version - m1U * 65536) / 256
	int m1L = m1Version - m1U * 65536 - m1M * 256
	int xepU = xepVersion / 65536
	int xepM = (xepVersion - xepU * 65536) / 256
	int xepL = xepVersion - xepU * 65536 - xepM * 256
	state.m1Version = String.format("%3d.%3d.%3d", m1U, m1M, m1L).replace(" ", "")
	state.xepVersion = String.format("%3d.%3d.%3d", xepU, xepM, xepL).replace(" ", "")
	if (txtEnable != "none")
		log.info "${device.label} panel version ${state.m1Version}, XEP Version ${state.xepVersion} found"
}

def connectionStatus(String message) {
	switch (message.substring(4, 6)) {
		case "00":
			log.warn "${device.label} ELKRP disconnected";
			refresh();
			break;
		case "01":
			log.warn "${device.label} ELKRP is connected";
			break;
		case "02":
			log.warn "${device.label} M1XEP is initializing";
			break;
	}
}

List<Map> updateSystemTrouble(String message) {
	List<Map> statusList = null
	byte[] statuses = message.substring(4, 38).getBytes()
	if (dbgEnable)
		log.debug "${device.label} updateSystemTrouble"
	int troubleCode
	String troubleMessage
	List<Integer> activeTrouble = []
	int i
	for (i = 0; i <= 33; i++) {
		troubleCode = statuses[i] - 48
		if (troubleCode || (state.trouble != null && state.trouble.findIndexOf { it == i } != -1)) {
			troubleMessage = elkTroubleCodes[i.toString()]
			if (troubleMessage != null) {
				if (troubleMessage == BoxTamperTrouble || troubleMessage == TransmitterLowBatteryTrouble || troubleMessage == SecurityAlert ||
						troubleMessage == LostTransmitterTrouble || troubleMessage == FireTrouble)
					troubleMessage += " zone " + troubleCode.toString()
				if (troubleCode) {
					troubleMessage += ": trouble"
					activeTrouble << i
				} else {
					troubleMessage += ": normal"
				}
				log.warn "${device.label} ${troubleMessage}"
			}
		}
	}
	if (activeTrouble.size() > 0) {
		state.trouble = activeTrouble
		if (device.currentState("trouble")?.value == null || device.currentState("trouble").value != true)
			statusList = [[name: "trouble", value: true, type: "system"]]
	} else {
		state.remove("trouble")
		if (device.currentState("trouble")?.value == null || device.currentState("trouble").value != false)
			statusList = [[name: "trouble", value: false, type: "system"]]
	}
	return statusList
}

List<Map> updateAlarmAreas(String message) {
	List<Map> statusList = null
	if (dbgEnable)
		log.debug "${device.label} updateAlarmAreas"
	String isAlarm
	for (i = 1; i <= 8; i++) {
		isAlarm = (message.substring(3 + i, 4 + i) == "1" ? "true" : "false")
		statusList = updateAreaStatus(i, [[name: "alarm", value: isAlarm, type: "area"]])
	}
	return statusList
}

def updateAlarmZones(String message) {
	if (dbgEnable)
		log.debug "${device.label} updateAlarmZones"
	String zoneType
	int i
	for (i = 1; i <= 208; i++) {
		zoneType = elkZoneDefinitions[message.substring(i + 3, i + 4)]
		if (zoneType != Disabled) {
			log.warn "${device.label} zone ${i}, type ${zoneType} is in alarm"
		}
	}
}

def receiveTextString(String message) {
	String textString = message.substring(4)
	if (txtEnable != "none")
		log.info "${device.label} receiveTextString: ${textString}"
}

def updateCustom(String message) {
	String custom = message.substring(4, 6)
	if (custom == "00") {
		int offset
		int i
		for (i = 1; i <= 20; i++) {
			offset = i * 6
			updateCustom(String.format("%02d", i), message.substring(offset, offset + 5).toInteger(), message.substring(offset + 5, offset + 6))
		}
	} else {
		updateCustom(custom, message.substring(6, 11).toInteger(), message.substring(11, 12))
	}
}

def updateCustom(String custom, int value, String format) {
	if (dbgEnable)
		log.debug "${device.label} updateCustom ${custom} = ${value}, format ${format}"
	def customDevice = getChildDevice(device.deviceNetworkId + "_Y_" + custom)
	if (customDevice != null) {
		customDevice.parse([value: value, format: format])
	}
}

def updateCounter(String message) {
	String counter = message.substring(4, 6)
	int value = message.substring(6, 11).toInteger()
	if (dbgEnable)
		log.debug "${device.label} updateCounter ${counter} = ${value}"
	def counterDevice = getChildDevice(device.deviceNetworkId + "_X_" + counter)
	if (counterDevice != null) {
		counterDevice.parse(value.toString())
	}
}

def zoneDefinitionReport(String message) {
	String zoneString
	String zoneDefinitions = message.substring(4, 212)
	int i
	for (i = 1; i <= 208; i++) {
		zoneString = elkZoneDefinitions[zoneDefinitions.substring(i - 1, i)]
		if (zoneString != null && zoneString != Disabled && txtEnable != "none") {
			log.info "${device.label} zoneDefinitionReport: Zone " + i + " - " + zoneString
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
			//	log.debug "${device.label} ZoneStatus: Zone " + i + " - " + elkZoneStatuses[zoneStatus]
			zoneChange("    " + String.format("%03d", i) + zoneStatus + "    ")
		}
	}
}

// Zone Status
def zoneViolated(String zoneNumber, String zoneStatus) {
	if (dbgEnable)
		log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus}";
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
			if (zoneDevice.currentState("contact")?.value == null || zoneDevice.currentState("contact").value != "open") {
				zoneDevice.open()
			}
		} else if (cmdList.find { it.name == "active" } != null) {
			if (zoneDevice.currentState("motion")?.value == null || zoneDevice.currentState("motion").value != "active") {
				zoneDevice.active()
			}
		} else if (cmdList.find { it.name == "on" } != null) {
			if (zoneDevice.currentState("switch")?.value == null || zoneDevice.currentState("switch").value != "on") {
				zoneDevice.on()
			}
		} else if (cmdList.find { it.name == "detected" } != null) {
			if ((zoneDevice.currentState("smoke")?.value == null || zoneDevice.currentState("smoke").value != "detected") &&
					(zoneDevice.currentState("carbonMonoxide")?.value == null || zoneDevice.currentState("carbonMonoxide").value != "detected")) {
				zoneDevice.detected()
			}
		} else if (cmdList.find { it.name == "wet" } != null) {
			if (zoneDevice.currentState("water")?.value == null || zoneDevice.currentState("water").value != "wet") {
				zoneDevice.wet()
			}
		} else if (cmdList.find { it.name == "arrived" } != null) {
			zoneDevice.arrived()
		} else {
			String capFound = zoneDevice.capabilities.find { capabilitiesViolated[it.name] != null }?.name
			if (capFound != null) {
				Map zoneEvent = capabilitiesViolated[capFound]
				if ((zoneEvent.isStateChange != null && zoneEvent.isStateChange == true) ||
						zoneDevice.currentState(zoneEvent.name)?.value == null || zoneDevice.currentState(zoneEvent.name).value != zoneEvent.value) {
					if (zoneEvent.value == "open")
						zoneEvent.descriptionText = zoneDevice.label + " was opened"
					else
						zoneEvent.descriptionText = zoneDevice.label + " " + zoneEvent.name + " was " + zoneEvent.value
					zoneDevice.sendEvent(zoneEvent)
					if (txtEnable != "none")
						log.info zoneEvent.descriptionText
				}
			}
		}
	}
}

def zoneNormal(String zoneNumber, String zoneStatus) {
	if (dbgEnable)
		log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus}";
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
			if (zoneDevice.currentState("contact")?.value == null || zoneDevice.currentState("contact").value != "closed") {
				zoneDevice.close()
			}
		} else if (cmdList.find { it.name == "inactive" } != null) {
			if (zoneDevice.currentState("motion")?.value == null || zoneDevice.currentState("motion").value != "inactive") {
				zoneDevice.inactive()
			}
		} else if (cmdList.find { it.name == "off" } != null) {
			if (zoneDevice.currentState("switch")?.value == null || zoneDevice.currentState("switch").value != "off") {
				zoneDevice.off()
			}
		} else if (cmdList.find { it.name == "clear" } != null) {
			if ((zoneDevice.currentState("smoke")?.value == null || zoneDevice.currentState("smoke").value != "clear") &&
					(zoneDevice.currentState("carbonMonoxide")?.value == null || zoneDevice.currentState("carbonMonoxide").value != "clear")) {
				zoneDevice.clear()
			}
		} else if (cmdList.find { it.name == "dry" } != null) {
			if (zoneDevice.currentState("water")?.value == null || zoneDevice.currentState("water").value != "dry") {
				zoneDevice.dry()
			}
		} else if (cmdList.find { it.name == "departed" } != null) {
			zoneDevice.departed()
		} else {
			String capFound = zoneDevice.capabilities.find { capabilitiesNormal[it.name] != null }?.name
			if (capFound != null) {
				Map zoneEvent = capabilitiesNormal[capFound]
				if ((zoneEvent.isStateChange != null && zoneEvent.isStateChange == true) ||
						zoneDevice.currentState(zoneEvent.name)?.value == null || zoneDevice.currentState(zoneEvent.name).value != zoneEvent.value) {
					if (zoneEvent.value == "closed")
						zoneEvent.descriptionText = zoneDevice.label + " was closed"
					else
						zoneEvent.descriptionText = zoneDevice.label + " " + zoneEvent.name + " was " + zoneEvent.value
					zoneDevice.sendEvent(zoneEvent)
					if (txtEnable != "none")
						log.info zoneEvent.descriptionText
				}
			}
		}
	}
}

def zoneTrouble(String zoneNumber, String zoneStatus) {
	def zoneDevice = getChildDevice("${device.deviceNetworkId}_Z_${zoneNumber}")
	if (zoneDevice != null) {
		zoneDevice.sendEvent(name: "status", value: zoneStatus, descriptionText: "${zoneDevice.label} status ${zoneStatus}")
		if (txtEnable != "none")
			log.info "${zoneDevice.label} status ${zoneStatus}"
	} else if (dbgEnable) {
		log.debug "${device.label} ZoneChange: ${zoneNumber} - ${zoneStatus}";
	}
}

//Manage Zones
List<Map> updateFKeyName(String keypadNumber, String keyType, String keyText) {
	List<Map> statusList = null
	if (!(keyText ==~ /(?i).*Not Defined.*/)) {
		String keyName = elkTextDescriptionsTypes[keyType]
		if (dbgEnable)
			log.debug "${device.label}: keypad: ${keypadNumber}, key: ${keyName}, name: ${keyText}"
		statusList = updateKeypadStatus(keypadNumber, [[name: keyName, value: keyText, type: "keypad"]])
	}
	return statusList
}

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
				if (txtEnable != "none")
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Motion"
				addChildDevice("hubitat", "Virtual Motion Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
				def newDevice = getChildDevice(deviceNetworkId)
				newDevice.updateSetting("autoInactive", [type: "enum", value: 0])
			} else if (zoneName ==~ /(?i).*temperature.*/) {
				if (txtEnable != "none")
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Temperature"
				addChildDevice("hubitat", "Virtual Temperature Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
				def newDevice = getChildDevice(deviceNetworkId)
			} else {
				if (txtEnable != "none")
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Contact"
				addChildDevice("hubitat", "Virtual Contact Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
				def newDevice = getChildDevice(deviceNetworkId)
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
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Keypad"
			try {
				addChildDevice("captncode", "Elk M1 Driver Keypad", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
			} catch (e) {
				addChildDevice("hubitat", "Virtual Temperature Sensor", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
				def newDevice = getChildDevice(deviceNetworkId)
				newDevice.updateSetting("txtEnable", [value: "false", type: "bool"])
			}
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "04") {
		if (zoneName == null) {
			zoneName = "Output ${zoneNumber} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_O_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
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
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Task"
			addChildDevice("belk", "Elk M1 Driver Tasks", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "07") {
		int deviceNumber = zoneNumber.toInteger()
		int ndx = (deviceNumber - 1) / 16
		int unitNumber = deviceNumber - ndx * 16
		zoneNumber = "ABCDEFGHIJKLMNOP".substring(ndx, ndx + 1) + String.format("%02d", unitNumber)
		if (zoneName == null) {
			zoneName = "Lighting ${zoneNumber} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_L_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (zoneName ==~ /(?i).*dim.*/) {
				if (txtEnable != "none")
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Lighting Dimmer"
				addChildDevice("captncode", "Elk M1 Driver Lighting Dimmer", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
			} else {
				if (txtEnable != "none")
					log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Lighting Switch"
				addChildDevice("captncode", "Elk M1 Driver Lighting Switch", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
			}
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "09") {
		zoneNumber = zoneNumber.substring(1, 3)
		if (zoneName == null) {
			zoneName = "Custom ${zoneNumber} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_Y_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Custom"
			addChildDevice("captncode", "Elk M1 Driver Custom", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "10") {
		zoneNumber = zoneNumber.substring(1, 3)
		if (zoneName == null) {
			zoneName = "Counter ${zoneNumber} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_X_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Counter"
			addChildDevice("captncode", "Elk M1 Driver Counter", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "11") {
		if (zoneName == null) {
			zoneName = "Thermostat ${zoneNumber} - ${zoneInfo.zoneText}"
		}
		deviceNetworkId = "${device.deviceNetworkId}_T_${zoneNumber}"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: Thermostat"
			addChildDevice("belk", "Elk M1 Driver Thermostat", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	} else if (zoneInfo.zoneType == "SP") {
		if (zoneName == null) {
			zoneName = zoneInfo.zoneText
		}
		deviceNetworkId = "${device.deviceNetworkId}_S_0"
		if (getChildDevice(deviceNetworkId) == null) {
			if (txtEnable != "none")
				log.info "${device.label}: Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: SpeechSynthesis"
			addChildDevice("captncode", "Elk M1 Driver Text To Speech", deviceNetworkId, [name: zoneName, isComponent: false, label: zoneName])
		} else if (dbgEnable) {
			log.debug "${device.label}: deviceNetworkId = ${deviceNetworkId} already exists"
		}
	}
}

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
def telnetTimeout() {
	telnetStatus("timeout")
}

int getReTry(boolean inc) {
	int reTry = (state.reTryCount ?: 0).toInteger()
	if (inc) reTry++
	state.reTryCount = reTry
	return reTry
}

def telnetStatus(String status) {
	log.warn "${device.label} telnetStatus error: ${status}"
	if (status == "receive error: Stream is closed" || status == "send error: Broken pipe (Write failed)" || status == "timeout") {
		getReTry(true)
		log.error "Telnet connection dropped..."
		log.warn "${device.label} Telnet is restarting..."
		initialize()
	}
}

////REFERENCES AND MAPPINGS////
// Key Mapping Readable Text
@Field static final String Off = "off"
@Field static final String On = "on"
@Field static final String Blinking = "blinking"

@Field final Map elkStates = [
		"0": Off,
		"1": On,
		"2": Blinking
]

@Field static final String Tone = "tone"
@Field static final String Voice = "voice"
@Field static final String ToneVoice = "tone/voice"
@Field final Map elkChimeStates = [
		"0": Off,
		"1": Tone,
		"2": Voice,
		"3": ToneVoice
]

@Field static final String NOTHING = ""
@Field static final String STARKEY = "*"
@Field static final String POUNDKEY = "#"
@Field static final String F1KEY = "1"
@Field static final String F2KEY = "2"
@Field static final String F3KEY = "3"
@Field static final String F4KEY = "4"
@Field static final String STAYKEY = "Stay"
@Field static final String EXITKEY = "Exit"
@Field static final String CHIMEKEY = "Chime"
@Field static final String BYPASSKEY = "Bypass"
@Field static final String MENUKEY = "Menu"
@Field static final String DOWNKEY = "Down"
@Field static final String UPKEY = "Up"
@Field static final String RIGHTKEY = "Right"
@Field static final String LEFTKEY = "Left"
@Field static final String F6KEY = "6"
@Field static final String F5KEY = "5"
@Field static final String DATAKEYMODE = "Data"

// Event Mapping
@Field final Map elkKeys = [
		'00': NOTHING,
		'11': STARKEY,
		'12': POUNDKEY,
		'13': F1KEY,
		'14': F2KEY,
		'15': F3KEY,
		'16': F4KEY,
		'17': STAYKEY,
		'18': EXITKEY,
		'19': CHIMEKEY,
		'20': BYPASSKEY,
		'21': MENUKEY,
		'22': DOWNKEY,
		'23': UPKEY,
		'24': RIGHTKEY,
		'25': LEFTKEY,
		'26': F6KEY,
		'27': F5KEY,
		'28': DATAKEYMODE
]

@Field final Map elkFKeys = [
		'*': STARKEY,
		'0': NOTHING,
		'1': F1KEY,
		'2': F2KEY,
		'3': F3KEY,
		'4': F4KEY,
		'5': F5KEY,
		'6': F6KEY,
		'C': CHIMEKEY
]
@Field static final String Disarmed = "Disarmed"
@Field static final String ArmedAway = "Armed Away"
@Field static final String ArmedStay = "Armed Stay"
@Field static final String ArmedStayInstant = "Armed Stay Instant"
@Field static final String ArmedNight = "Armed Night"
@Field static final String ArmedNightInstant = "Armed Night Instant"
@Field static final String ArmedVacation = "Armed Vacation"
@Field static final String ArmingAway = "Arming Away"
@Field static final String ArmingStay = "Arming Stay"
@Field static final String ArmingStayInstant = "Arming Stay Instant"
@Field static final String ArmingNight = "Arming Night"
@Field static final String ArmingNightInstant = "Arming Night Instant"
@Field static final String ArmingVacation = "Arming Vacation"

@Field final Map elkArmStatuses = [
		'0': Disarmed,
		'1': ArmedAway,
		'2': ArmedStay,
		'3': ArmedStayInstant,
		'4': ArmedNight,
		'5': ArmedNightInstant,
		'6': ArmedVacation
]

@Field final Map elkArmingStatuses = [
		'0': Disarmed,
		'1': ArmingAway,
		'2': ArmingStay,
		'3': ArmingStayInstant,
		'4': ArmingNight,
		'5': ArmingNightInstant,
		'6': ArmingVacation
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
		'B': FireVerified,
		'D': BurglarBoxTamper
]

// Zone Status Mapping Readable Text
@Field static final String NormalUnconfigured = "Normal: Unconfigured"
@Field static final String NormalOpen = "Normal: Open"
@Field static final String NormalEOL = "Normal: EOL"
@Field static final String NormalShort = "Normal: Short"
@Field static final String TroubleOpen = "Trouble: Open"
@Field static final String TroubleEOL = "Trouble: EOL"
@Field static final String TroubleShort = "Trouble: Short"
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
		'9': ViolatedOpen,
		'A': ViolatedEOL,
		'B': ViolatedShort,
		'C': SoftBypassed,
		'D': BypassedOpen,
		'E': BypassedEOL,
		'F': BypassedShort

]

@Field final Map capabilitiesViolated = [
		"AccelerationSensor"    : [name: "acceleration", value: "active"],
		"Beacon"                : [name: "presence", value: "present"],
		"CarbonMonoxideDetector": [name: "carbonMonoxide", value: "detected"],
		"ContactSensor"         : [name: "contact", value: "open"],
		"DoorControl"           : [name: "door", value: "open"],
		"GarageDoorControl"     : [name: "door", value: "open"],
		"MotionSensor"          : [name: "motion", value: "active"],
		"PresenceSensor"        : [name: "presence", value: "present"],
		"RelaySwitch"           : [name: "switch", value: "on"],
		"ShockSensor"           : [name: "shock", value: "detected"],
		"SleepSensor"           : [name: "sleeping", value: "sleeping"],
		"SmokeDetector"         : [name: "smoke", value: "detected"],
		"SoundSensor"           : [name: "sound", value: "detected"],
		"Switch"                : [name: "switch", value: "on"],
		"TamperAlert"           : [name: "tamper", value: "detected"],
		"TouchSensor"           : [name: "touch", value: "touched", isStateChange: true],
		"Valve"                 : [name: "valve", value: "open"],
		"WaterSensor"           : [name: "water", value: "wet"]
]

@Field final Map capabilitiesNormal = [
		"AccelerationSensor"    : [name: "acceleration", value: "inactive"],
		"Beacon"                : [name: "presence", value: "not present"],
		"CarbonMonoxideDetector": [name: "carbonMonoxide", value: "clear"],
		"ContactSensor"         : [name: "contact", value: "closed"],
		"DoorControl"           : [name: "door", value: "closed"],
		"GarageDoorControl"     : [name: "door", value: "closed"],
		"MotionSensor"          : [name: "motion", value: "inactive"],
		"PresenceSensor"        : [name: "presence", value: "not present"],
		"RelaySwitch"           : [name: "switch", value: "off"],
		"ShockSensor"           : [name: "shock", value: "clear"],
		"SleepSensor"           : [name: "sleeping", value: "not sleeping"],
		"SmokeDetector"         : [name: "smoke", value: "clear"],
		"SoundSensor"           : [name: "sound", value: "not detected"],
		"Switch"                : [name: "switch", value: "off"],
		"TamperAlert"           : [name: "tamper", value: "clear"],
		"TouchSensor"           : [name: "touch", value: null, isStateChange: true],
		"Valve"                 : [name: "valve", value: "closed"],
		"WaterSensor"           : [name: "water", value: "dry"]
]

@Field static final String Fahrenheit = "Fahrenheit"
@Field static final String Celcius = "Celcius"

@Field final Map elkTemperatureModes = [
		F: Fahrenheit,
		C: Celcius
]

@Field static final String User = "User"

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
@Field static final String FunctionKey1Name = "button1"
@Field static final String FunctionKey2Name = "button2"
@Field static final String FunctionKey3Name = "button3"
@Field static final String FunctionKey4Name = "button4"
@Field static final String FunctionKey5Name = "button5"
@Field static final String FunctionKey6Name = "button6"


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
@Field static final String on = "on"
@Field static final String circulate = "circulate"

@Field final Map elkThermostatModeSet = [off: '0', heat: '1', cool: '2', auto: '3', 'emergency heat': '4']
@Field final Map elkThermostatFanModeSet = [auto: '0', on: '1', circulate: '0']
@Field final Map elkThermostatHoldModeSet = [off: '0', on: '1']

@Field final Map elkCommands = [
		Disarm                 : "a0",
		ArmAway                : "a1",
		ArmStay                : "a2",
		ArmStayInstant         : "a3",
		ArmNight               : "a4",
		ArmNightInstant        : "a5",
		ArmVacation            : "a6",
		ArmNextAway            : "a7",
		ArmNextStay            : "a8",
		ArmForceAway           : "a9",
		ArmForceStay           : "a:",
		RequestArmStatus       : "as",
		RequestTemperatureData : "lw",
		RequestTextDescriptions: "sd",
		RequestTroubleStatus   : "ss",
		RequestAlarmStatus     : "az",
		RequestThermostatData  : "tr",
		RequestVersionNumber   : "vn",
		RequestZoneDefinitions : "zd",
		RequestZoneStatus      : "zs",
		RequestLightingStatus  : "ps",
		RequestKeypadArea      : "ka",
		RequestKeypadPress     : "kf",
		RequestKeypadStatus    : "kc",
		RequestOutputStatus    : "cs",
		RequestAllCustomValues : "cp",
		RequestCustomValue     : "cr",
		RequestCounterValue    : "cv",
		TaskActivation         : "tn",
		SetThermostatData      : "ts",
		SetCounterValue        : "cx",
		SetCustomValue         : "cw",
		ZoneBypass             : "zb",
		ZoneTrigger            : "zt",
		ZoneVoltage            : "zv",
		ControlOutputOn        : "cn",
		ControlOutputOff       : "cf",
		ControlOutputToggle    : "ct",
		ControlLightingMode    : "pc",
		ControlLightingOn      : "pn",
		ControlLightingOff     : "pf",
		ControlLightingToggle  : "pt",
		SpeakWord              : "sw",
		SpeakPhrase            : "sp",
		ShowTextOnKeypads      : "dm"
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

@Field static final String ACFailTrouble = "AC Fail Trouble"
@Field static final String BoxTamperTrouble = "Box Tamper Trouble"
@Field static final String FailToCommunicateTrouble = "Fail To Communicate Trouble"
@Field static final String EEPromMemoryErrorTrouble = "EEProm Memory Error Trouble"
@Field static final String LowBatteryControlTrouble = "Low Battery Control Trouble"
@Field static final String TransmitterLowBatteryTrouble = "Transmitter Low Battery Trouble"
@Field static final String OverCurrentTrouble = "Over Current Trouble"
@Field static final String TelephoneFaultTrouble = "Telephone Fault Trouble"
@Field static final String Output2Trouble = "Output 2 Trouble"
@Field static final String MissingKeypadTrouble = "Missing Keypad Trouble"
@Field static final String ZoneExpanderTrouble = "Zone Expander Trouble"
@Field static final String OutputExpanderTrouble = "Output Expander Trouble"
@Field static final String ELKRPRemoteAccess = "ELKRP Remote Access"
@Field static final String CommonAreaNotArmed = "Common Area Not Armed"
@Field static final String FlashMemoryErrorTrouble = "Flash Memory Error Trouble"
@Field static final String SecurityAlert = "Security Alert"
@Field static final String SerialPortExpanderTrouble = "Serial Port Expander Trouble"
@Field static final String LostTransmitterTrouble = "Lost Transmitter Trouble"
@Field static final String SmokeCleanMeTrouble = "Smoke Clean Me Trouble"
@Field static final String EthernetTrouble = "Ethernet Trouble"
@Field static final String DisplayMessageInKeypadLine1 = "Display Message In Keypad Line 1"
@Field static final String DisplayMessageInKeypadLine2 = "Display Message In Keypad Line 2"
@Field static final String FireTrouble = "Fire Trouble"


@Field final Map elkTroubleCodes = [
		0 : ACFailTrouble,
		1 : BoxTamperTrouble,
		2 : FailToCommunicateTrouble,
		3 : EEPromMemoryErrorTrouble,
		4 : LowBatteryControlTrouble,
		5 : TransmitterLowBatteryTrouble,
		6 : OverCurrentTrouble,
		7 : TelephoneFaultTrouble,
		9 : Output2Trouble,
		10: MissingKeypadTrouble,
		11: ZoneExpanderTrouble,
		12: OutputExpanderTrouble,
		14: ELKRPRemoteAccess,
		16: CommonAreaNotArmed,
		17: FlashMemoryErrorTrouble,
		18: SecurityAlert,
		19: SerialPortExpanderTrouble,
		20: LostTransmitterTrouble,
		21: SmokeCleanMeTrouble,
		22: EthernetTrouble,
		31: DisplayMessageInKeypadLine1,
		32: DisplayMessageInKeypadLine2,
		33: FireTrouble
]

@Field final Map elkLogData = [
		1111: "Code Lockout, Any Keypad",
		1112: "Keypad 01 Code-Lockout",
		1113: "Keypad 02 Code-Lockout",
		1114: "Keypad 03 Code-Lockout",
		1115: "Keypad 04 Code-Lockout",
		1116: "Keypad 05 Code-Lockout",
		1117: "Keypad 06 Code-Lockout",
		1118: "Keypad 07 Code-Lockout",
		1119: "Keypad 08 Code-Lockout",
		1120: "Keypad 09 Code-Lockout",
		1121: "Keypad 10 Code-Lockout",
		1122: "Keypad 11 Code-Lockout",
		1123: "Keypad 12 Code-Lockout",
		1124: "Keypad 13 Code-Lockout",
		1125: "Keypad 14 Code-Lockout",
		1126: "Keypad 15 Code-Lockout",
		1127: "Keypad 16 Code-Lockout",
		1131: "RF Sensor Low Battery",
		1132: "Lost Anc Module Trouble",
		1141: "Expansion Module Trouble",
		1161: "Expansion Module Restore",
		1293: "Automatic Closing",
		1294: "Early Closing",
		1295: "Closing Time Extended",
		1296: "Fail To Close",
		1297: "Late To Close",
		1298: "Keyswitch Closing",
		1299: "Duress",
		1300: "Exception Opening",
		1301: "Early Opening",
		1302: "Fail To Open",
		1303: "Late To Open",
		1304: "Keyswitch Opening",
		1313: "Access Keypad 01",
		1314: "Access Keypad 02",
		1315: "Access Keypad 03",
		1316: "Access Keypad 04",
		1317: "Access Keypad 05",
		1318: "Access Keypad 06",
		1319: "Access Keypad 07",
		1321: "Access Keypad 09",
		1322: "Access Keypad 10",
		1323: "Access Keypad 11",
		1324: "Access Keypad 12",
		1325: "Access Keypad 13",
		1326: "Access Keypad 14",
		1327: "Access Keypad 15",
		1328: "Access Keypad 16",
		1329: "Access Any Keypad",
		1338: "Area 1 Exit Error",
		1339: "Area 2 Exit Error",
		1340: "Area 3 Exit Error",
		1341: "Area 4 Exit Error",
		1342: "Area 5 Exit Error",
		1343: "Area 6 Exit Error",
		1344: "Area 7 Exit Error",
		1345: "Area 8 Exit Error",
		1351: "Dialer Abort",
		1352: "Dialer Cancel",
		1353: "Dialer Auto Test",
		1356: "Keyswitch Zone Tamper Alert",
		1358: "Telephone Line Is Ringing",
		1359: "Telephone Line Seize",
		1360: "Telephone Line Off/On Hook",
		1361: "Telephone Local Access",
		1362: "Telephone Remote Access",
		1365: "AC Fail Trouble Zone",
		1366: "Low Battery Trouble Zone",
		1367: "System Start Up",
		1368: "Control Low Voltage Shutdown",
		1369: "RF Keyfob Button 1",
		1370: "RF Keyfob Button 2",
		1371: "RF Keyfob Button 3",
		1372: "RF Keyfob Button 4",
		1373: "RF Keyfob Button 5",
		1374: "RF Keyfob Button 6",
		1375: "RF Keyfob Button 7",
		1376: "RF Keyfob Button 8",
		1378: "Rule Triggered Voice Telephone Dial",
		1379: "Dialer Report Cleared",
		1380: "Central Station Kissoff",
		1381: "Transmitter Supervision Loss",
		1385: "Restore AC Power Zone",
		1386: "Restore Battery Zone"
]

/***********************************************************************************************************************
 *
 * Release Notes (see Known Issues Below)
 *
 * 0.2.4
 * Fixed an issue with a keypad device that is a Virtual Temperature Probe.
 *
 * 0.2.3
 * Added Counter and Custom value device types.
 * Fixed long standing issue with checksums on Elk commands.  1% of the checksums were generating wrong causing
 *       those commands to quietly fail.
 * Fixed incorrectly named command refreshSystemTroubleStatus.  Renamed it to refreshTroubleStatus.
 * Fixed errors with a keypad device that is a Virtual Temperature Probe.
 *
 * 0.2.2
 * Added setting to control if switch is turned on when arming or when fully armed.
 *
 * 0.2.1
 * Added setThermostatTemperature to thermostat.
 * Fixed bug with trouble code.
 * Changed thermostatHoldMode from true and false to yes and no.
 *
 * 0.2.0
 * Added keypad functionality such as chime and temperature measurement by assigning a keypad number
 *       to this device.  Device area is derived from the area the keypad is assigned to.
 * Added Function 1-6 keys as pushable buttons and retrieving of their description.
 * Added Function key LED status.
 * Added keypad beep, chime and chime mode status.
 * Added alarm and system trouble status.
 * Added request of zone voltage to be written to info log.
 * Added speak word/phrase using the Elk word or phrase number.
 * Added showTextOnKeypads to display a message on all keypads.
 * Added zone bypass and zone trigger commands.
 * Added setting if temperatures from the panel is in C or F.
 * Added setting to control if you want the hub's location set when the panel is armed/disarmed.
 * Added settings to control what arming or disarming happens when the switch assigned to this
 *       device is turned on or off.  Primarily for use by the dashboard or Google/Alexa.
 * Enhanced logging setting to allow control of whether Area or Keypad events get logged.
 * Added sendEmail event when an email is sent by the panel.
 * Added warning log when certain data is written to the panel log.
 * Added warning log on zone devices when a zone is in alarm.
 * Added status event on zone devices when a zone has a trouble code.
 * Added pushed status when certain keys are pressed a the keypad.
 * Added invalidUser status when an invalid user code is entered on a keypad.  This could be used
 *       to trigger rules based on a specific invalid user code being entered.
 * Added refresh if Elk RP programming occurs and warning log of Elk RP connections.
 * Added descriptions to events for clarity.
 * Added "arming" armStatus event when panel is armed and the exit delay has not yet expired.
 * Added calls to Elk application when arm/disarm and exit delay events occur for HSM, TTS and notifications.
 * Added warning log when alarm reporting occurs.
 * Added hooks for separate custom Elk Keypad and Speech drivers.
 * Changed task to be a momentary pushable button instead of a switch.
 *
 * 0.1.34
 * Added a telnet timeout feature to re-initialize device if the panel's IP Test message is not received when expected
 * Added armMode attribute
 * Rename armHome command to armStay
 * Removed processing of any Log Data Update report due to not being reliable for real time results
 *
 * 0.1.33
 * Added import of lighting devices for use with new drivers.  If lighting text has "dim" in it, it will be assigned the dimmer
 *   driver.  Otherwise, it will be assigned the switch driver.
 * Fixed issue with M1 Touch Pro App sync conflict that caused this device to crash
 * Fixed issue with thermostats not working
 * Removed redundant output and task command buttons.  The individual devices now have them.
 * Renamed the Request xxx Status command buttons to Refresh xxx Status for accuracy
 * Stopped zone statuses from writing info log when the status didn't change
 * Added retrieval of Elk M1 Version after initialization
 *
 * 0.1.32
 * Fixed issue with temperature readings.
 *
 * 0.1.31
 * Added handling of Temperature Data automatically sent starting with M1 Ver. 4.2.8
 * Added info logging only of Zone Definitions
 * Simplified thermostat code
 * Simplified Output and Task logging and events
 * Removed unneeded thermostat capability from this device since the child thermostat device has it
 *
 * 0.1.30
 * Added polling of device status once connected or reconnected to the panel.
 * Added Enable debug logging and Enable descriptionText logging to the main device.  Debug is no longer set for the
 *   device from within the application.  Info logging can now be turned on or off for the main device.
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
 *   Data button on the main device or by setting up a rule to periodically execute the refreshTemperatureStatus command.
 * Added "ContactSensor" as a capability for HSM monitoring.  The contact will open if a Burglar Alarm or Police Alarm is triggered.
 * Changed the method of importing devices to greatly improve performance and reduce panel communication.
 * Fixed an issue not deleting child devices when the main device is uninstalled.
 * Fixed an issue with the name when importing a device with the "Show On Keypad" flag set.
 *
 * 0.1.27
 * You can now change the port on the main device page.  You must click "Initialize" after you save preferences to take effect
 * Changed info logging to only happen if Enable descriptionText logging is turned on the device
 * Re-enabled Request Zone Definition and Request Zone Status
 * Added Request Output Status
 *
 * 0.1.26
 * Added info logging when output or task status changes or Arm Mode changes
 * Added switch capability to main Elk M1 device
 * Improved armStatus and alarmState events to fire only when changed
 * Adding missing alarmStates
 * Small tweaks to improve performance
 *
 * 0.1.25
 * Added sync of Elk M1 modes to Location Modes: Disarmed, Armed Away, Night, Stay, Vacation synced to modes Home, Away, Night,
 *    Stay (if available and Home if not), Vacation (if available and Away if not), respectively.
 * Added sync of hsmSetArm modes of disarm, armAway, armNight, ArmStay
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
 * Added setting of lastUser event which contains the user number who last triggered armed or disarmed
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
 * Fixed code to show operating state and thermostat set point on dashboard tile
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
 *
 * 0.1.13
 * Elk M1 Code - No longer requires a 6 digit code (Add leading zeroes to 4 digit codes)
 * Outputs and tasks can now be entered as a number
 * Code clean up - removed some unused code
 *
 * 0.1.12
 * Added support for outputs
 *
 * 0.1.11
 * Built separate thermostat child driver should allow for multiple thermostats
 *
 * 0.1.10
 * Ability to control thermostat 1
 *
 * 0.1.9
 * Minor changes
 *
 * 0.1.8
 * Ability to read thermostat data (haven't confirmed multiple thermostat support)
 * Added additional mappings for thermostat support
 * Additional code clean up
 *
 * 0.1.7
 * Rewrite of the various commands
 *
 * 0.1.6
 * Changed text description mapping to string characters
 *
 * 0.1.5
 * Added zone types
 * Added zone definitions
 *
 * 0.1.4
 * Added additional command requests
 *
 * 0.1.3
 * Receive status messages and interpret data
 *
 * 0.1.2
 * Minor changes to script nomenclature and formatting
 *
 * 0.1.1
 * Ability to connect Elk M1 and see data
 * Ability to send commands to Elk M1
 * Changed code to input parameter
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 * I - Unable to use the secure port on the Elk M1XEP.  A non-secure port is required.
 * I - Fan Circulate and set schedule not supported
 * F - Request text descriptions for zone setup, tasks and outputs (currently this must be done via the app)
 * I - A device with the same device network ID exists (this is really not an issue)
 * I - Zone, output and task reporting is limited to one report per child device
 *
 ***********************************************************************************************************************/