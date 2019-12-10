/***********************************************************************************************************************
 *
 *  A Hubitat App for managing Elk M1 Integration
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
 *  Name: Elk M1 Application
 *
 *  Special Thanks to Doug Beard for the framework of this application!
 *** See Release Notes at the bottom***
 ***********************************************************************************************************************/
//Adding thermostat support

public static String version() { return "v0.2.00" }

definition(
		name: "Elk M1 Application",
		namespace: "belk",
		singleInstance: true,
		author: "Mike Magrann",
		description: "Integrate your Elk M1 Alarm system",
		category: "My Apps",
		iconUrl: "",
		iconX2Url: "",
		iconX3Url: "",
)

preferences {
	page(name: "mainPage", nextPage: "zoneMapsPage")
	page(name: "zoneMapsPage", nextPage: "mainPage")
	page(name: "notificationPage", nextPage: "mainPage")
	page(name: "lockPage", nextPage: "mainPage")
	page(name: "hsmPage", nextPage: "mainPage")
	page(name: "defineZoneMap", nextPage: "zoneMapsPage")
	page(name: "defineLightMap", nextPage: "zoneMapsPage")
	page(name: "defineZoneMapImport", nextPage: "importZones")
	page(name: "importZones", nextPage: "zoneMapsPage")
	page(name: "editZoneMapPage", nextPage: "zoneMapsPage")
//	page(name: "aboutPage", nextPage: "mainPage")
}

//App Pages/Views
def mainPage() {
	app.removeSetting("elkM1Password")
	state.remove("ElkM1DeviceName")
	state.remove("ElkM1IP")
	state.remove("ElkM1Port")
	state.remove("ElkM1Code")
	state.remove("ElkM1Password")
	state.remove("allZones")
	state.remove("ElkM1Installed")
	state.remove("isDebug")
	state.creatingZone = false
	state.editedZoneDNI = null
	ifDebug("Showing mainPage")
	return dynamicPage(name: "mainPage", title: "", install: false, uninstall: true) {
		if (getChildDevices().size() == 0) {
			section("Define your Elk M1 device") {
				clearStateVariables()
				input "elkM1Name", "text", title: "Elk M1 Name", required: true, multiple: false, defaultValue: "Elk M1", submitOnChange: false
				input "elkM1IP", "text", title: "Elk M1 IP Address", required: true, multiple: false, defaultValue: "", submitOnChange: false
				input "elkM1Port", "text", title: "Elk M1 Port", required: true, multiple: false, defaultValue: "2101", submitOnChange: false
				input "elkM1Keypad", "text", title: "Elk M1 Keypad", required: true, multiple: false, defaultValue: "1", submitOnChange: false
				input "elkM1Code", "text", title: "Elk M1 Disarm Code", required: true, multiple: false, defaultValue: "", submitOnChange: false
			}
		} else {
			section("<h1>Device Mapping</h1>") {
				href(name: "zoneMapsPage", title: "Devices",
						description: "Create Virtual Devices and Map them to Existing Zone, Output, Task, Lighting, Thermostat, Keypad and/or Speech Devices in your Elk M1 setup",
						page: "zoneMapsPage")
			}

			section("<h1>Notifications</h1>") {
				href(name: "notificationPage", title: "Notifications",
						description: "Enable Push and TTS Messages",
						page: "notificationPage")
			}

			section("<h1>Integration</h1>") {
				href(name: "lockPage", title: "Locks",
						description: "Integrate Locks",
						page: "lockPage")
				href(name: "hsmPage", title: "HSM",
						description: "Integrate Hubitat Safety Monitor",
						page: "hsmPage")
			}

		}
//		section("<br/><br/>") {
//			href (name: "aboutPage", title: "About",
//				description: "Find out more about Elk M1 Application",
//				page: "aboutPage")
//		}
		section("") {
			input "isDebug", "bool", title: "Enable Debug Logging", required: false, multiple: false, defaultValue: false, submitOnChange: true
		}

	}
}

def aboutPage() {
	ifDebug("Showing aboutPage")

	dynamicPage(name: "aboutPage", title: none) {
		section("<h1>Introducing Elk M1 Integration</h1>") {
			paragraph "Elk M1 module allows you to upgrade your existing security system with IP control ..." +
					" Elk M1 Integration connects to your M1XEP/C1M1 module via Telnet, using Hubitat."
			paragraph "Elk M1 Integration automates installation and configuration of the Elk M1 Driver" +
					" as well as Virtual Contacts representing the dry contact zones and Virtual Motion Detection configured in your Elk M1 Alarm system."
			paragraph "You must have the Hubitat Elk M1 driver already installed before making use of Elk M1 application "
			paragraph "Currently, Elk M1 application and driver only works with Elk M1XEP and C1M1 on the local network"
			paragraph "Special Thanks to Doug Beard."
		}
	}
}

def lockPage() {
	ifDebug("Showing lockPage")

	dynamicPage(name: "lockPage", title: none) {
		section("<h1>Locks</h1>") {
			paragraph "Enable Lock Integration, selected locks will lock when armed and/or unlock when disarmed"
			input "armLocks", "capability.lock", title: "Which locks to lock when armed?", required: false, multiple: true, submitOnChange: true
			input "disarmLocks", "capability.lock", title: "Which locks to unlock when disarmed?", required: false, multiple: true, submitOnChange: true
		}
	}
}

def hsmPage() {
	ifDebug("Showing hsmPage")
	dynamicPage(name: "hsmPage", title: none) {
		section("<h1>Hubitat Safety Monitor</h1>") {
			paragraph "Hubitat Safety Monitor Integration will tie your Elk M1 Arm Status to the status of HSM.  Enable both switches for full integration."
			paragraph "HSM Status will be set to Arm Away, Arm Home, Arm Night or Disarm when the Elk M1 Arm Status changes."
			input "enableElktoHSM", "bool", title: "Enable Elk M1 to HSM Integration", required: false, multiple: false, defaultValue: false, submitOnChange: true
			paragraph "Your Elk M1 will receive the Arm Away, Arm Stay, Arm Night or Disarm commands when the HSM Status changes."
			input "enableHSMtoElk", "bool", title: "Enable HSM to Elk M1 Integration", required: false, multiple: false, defaultValue: false, submitOnChange: true
		}
	}
}

def notificationPage() {
	dynamicPage(name: "notificationPage", title: none) {
		section("<h1>Notifications</h1>") {
			paragraph "Enable TTS and Notification integration will announcing arming and disarming over your supported audio and/or push enabled device"
			paragraph "<h3><b>Notification Text</b></h2>"

			input "armingStayBool", "bool", title: "Enable Arming Stay Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
			if (armingStayBool) {
				input "armingStayText", "text", title: "Notification for Arming Stay", required: false, multiple: false, defaultValue: "Arming Stay", submitOnChange: false, visible: armingStayBool
			}

			input "armingAwayBool", "bool", title: "Enable Arming Away Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
			if (armingAwayBool) {
				input "armingAwayText", "text", title: "Notification for Arming Away", required: false, multiple: false, defaultValue: "Arming Away", submitOnChange: false
			}
			input "armingVacationBool", "bool", title: "Enable Arming Vacation Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
			if (armingVacationBool) {
				input "armingVacationText", "text", title: "Notification for Arming Vacation", required: false, multiple: false, defaultValue: "Arming Vacation", submitOnChange: false
			}
			input "armingNightBool", "bool", title: "Enable Arming Night Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
			if (armingNightBool) {
				input "armingNightText", "text", title: "Notification for Arming Night", required: false, multiple: false, defaultValue: "Arming Night", submitOnChange: false
			}
			input "armedBool", "bool", title: "Enable Armed Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
			if (armedBool) {
				input "armedText", "text", title: "Notification for Armed", required: false, multiple: false, defaultValue: "Armed", submitOnChange: false
			}
			input "disarmedBool", "bool", title: "Enable Disarmed Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
			if (disarmedBool) {
				input "disarmedText", "text", title: "Notification for Disarmed", required: false, multiple: false, defaultValue: "Disarmed", submitOnChange: false
			}
			input "entryDelayAlarmBool", "bool", title: "Enable Entry Delay Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
			if (entryDelayAlarmBool) {
				input "entryDelayAlarmText", "text", title: "Notification for Entry Delay", required: false, multiple: false, defaultValue: "Entry Delay in Progress, Alarm eminent", submitOnChange: false
			}
			input "alarmBool", "bool", title: "Enable Alarm Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
			if (alarmBool) {
				input "alarmText", "text", title: "Notification for Alarm", required: false, multiple: false, defaultValue: "Alarm, Alarm, Alarm, Alarm, Alarm", submitOnChange: false
			}
			paragraph "<h3><b>Notification Devices</b></h2>"
			input "speechDevices", "capability.speechSynthesis", title: "Which speech devices?", required: false, multiple: true, submitOnChange: true
			input "notificationDevices", "capability.notification", title: "Which notification devices?", required: false, multiple: true, submitOnChange: true
		}
	}
}

def zoneMapsPage() {
	ifDebug("Showing zoneMapsPage")
	if (getChildDevices().size() == 0) {
		createElkM1ParentDevice()
	}

	if (state.creatingZone) {
		createZone()
	}

	dynamicPage(name: "zoneMapsPage", title: "", install: true, uninstall: false) {

		section("<h1>Device Maps</h1>") {
			paragraph "The partition of your Elk M1 Installation may consist of Zone, Output, Task, Lighting, Thermostat, Keypad and Speech Devices.  You can choose to map the devices manually or use the import method. "
			paragraph "You'll want to determine the device number as it is defined in your Elk M1 setup. " +
					" Define a new device in Elk M1 application and the application will then create either a Virtual sensor component device or an Elk Child device, which will report the state of the Elk M1 device to which it is mapped. " +
					" The devices can be used in Rule Machine or any other application that is capable of leveraging the devices capability.  Elk M1 is capable of 208 zones, your zone map should correspond to the numeric representation of that zone."
		}
		section("<h2>Create New Devices</h2>") {
			href(name: "createZoneImportPage", title: "Import Elk Devices",
					description: "Click to import Elk devices",
					page: "defineZoneMapImport")
		}
		section("") {
			href(name: "createZoneMapPage", title: "Create a Device Map",
					description: "Create a Virtual Device Manually",
					page: "defineZoneMap")
		}
		section("") {
			href(name: "createLightMapPage", title: "Create a Lighting Device Map",
					description: "Create a Virtual Lighting Device Manually",
					page: "defineLightMap")
		}

		section("<h2>Existing Devices</h2>") {
			getChildDevice(state.ElkM1DNI).getChildDevices().sort { it.deviceNetworkId }.each {
				href(name: "editZoneMapPage", title: "${it.label}",
						description: "Device Details",
						params: [deviceNetworkId: it.deviceNetworkId],
						page: "editZoneMapPage")
			}
		}
	}
}

def defineZoneMap() {
	ifDebug("Showing defineZoneMap")
	state.creatingZone = true;
	dynamicPage(name: "defineZoneMap", title: "") {
		section("<h1>Create a Device Map</h1>") {
			paragraph "Create a Map for a device in Elk M1"
			input "zoneName", "text", title: "Device Name", required: true, multiple: false, defaultValue: "Zone x", submitOnChange: false
			input "zoneNumber", "number", title: "Which Device 1 - 208", required: true, multiple: false, defaultValue: 1, range: "1..208", submitOnChange: false
			input "zoneType", "enum", title: "Zone, Output, Task, Thermostat, Keypad or Speech Device?", required: true, multiple: false,
					options: [['00': "Zone (1 - 208)"], ['04': "Output (1 - 208)"], ['05': "Task (1 - 32)"], ['11': "Thermostat (1 - 16)"],
							  ['03': "Keypad (1 - 16)"], ['SP': "Speech (1)"]]
		}
	}
}

def defineLightMap() {
	ifDebug("Showing defineLightMap")
	state.creatingZone = true;
	dynamicPage(name: "defineLightMap", title: "") {
		section("<h1>Create a Device Map</h1>") {
			paragraph "Create a Map for a lighting device in Elk M1"
			input "zoneName", "text", title: "Device Name", required: true, multiple: false, defaultValue: "Zone x", submitOnChange: false
			input "zoneType", "enum", title: "Which Lighting Group?", required: true, multiple: false,
					options: [['A': "A"], ['B': "B"], ['C': "C"], ['D': "D"], ['E': "E"], ['F': "F"], ['G': "G"], ['H': "H"],
							  ['I': "I"], ['J': "J"], ['K': "K"], ['L': "L"], ['M': "M"], ['N': "N"], ['O': "O"], ['P': "P"]]
			input "zoneNumber", "number", title: "Which Device 1 - 16", required: true, multiple: false, defaultValue: 1,
					range: "1..16", submitOnChange: false
		}
	}
}

def defineZoneMapImport() {
	ifDebug("Showing defineZoneMapImport")
	getChildDevice(state.ElkM1DNI).initialize()
	state.creatingZone = true;
	dynamicPage(name: "defineZoneMapImport", title: "") {
		section("<h1>Import Elk Zones</h1>") {
			paragraph "Create a Map for a zone in Elk M1"
			input "deviceType", "enum", title: "Select Device Type", required: true, multiple: false,
					options: [['00': "Zones"], ['04': "Output"], ['05': "Task"], ['07': "Lighting"], ['11': "Thermostat"], ['03': "Keypad"]]
		}
	}
}

def editZoneMapPage(message) {
	ifDebug("Showing editZoneMapPage")
	ifDebug("editing ${message.deviceNetworkId}")
	def zoneDevice = getChildDevice(state.ElkM1DNI).getChildDevice(message.deviceNetworkId)
	def paragraphText = ""
	state.editedZoneDNI = message.deviceNetworkId;
	zoneDevice.capabilities.each {
		if (it.name != "Actuator")
			paragraphText = paragraphText + it.name + "\n"
	}
	dynamicPage(name: "editZoneMapPage", title: "") {
		section("<h1>${zoneDevice.label}</h1>") {
			paragraph paragraphText
		}
	}
}

//End New Code Temp
def clearStateVariables() {
	ifDebug("Clearing State Variables just in case.")
}

def createElkM1ParentDevice() {
	ifDebug("Creating Parent ElkM1 Device")
	if (getChildDevice(state.ElkM1DNI) == null) {
		state.ElkM1DNI = UUID.randomUUID().toString()
		ifDebug("Setting state.ElkM1DNI ${state.ElkM1DNI}")
		addChildDevice("belk", "Elk M1 Driver", state.ElkM1DNI, null, [name: elkM1Name, isComponent: true, label: elkM1Name])
		getChildDevice(state.ElkM1DNI).updateSetting("ip", [type: "text", value: elkM1IP])
		getChildDevice(state.ElkM1DNI).updateSetting("port", [type: "text", value: elkM1Port])
		getChildDevice(state.ElkM1DNI).updateSetting("keypad", [type: "text", value: elkM1Keypad])
		getChildDevice(state.ElkM1DNI).updateSetting("code", [type: "text", value: elkM1Code])
		if (getChildDevice(state.ElkM1DNI)) {
			ifDebug("Found a Child Elk M1 ${getChildDevice(state.ElkM1DNI).label}")
		} else {
			ifDebug("Did not find a Parent Elk M1")
		}
	}
}

def importZones() {
	dynamicPage(name: "defineZoneMapImport", title: "") {
		section("<h1>Import Elk Zones</h1>") {
			paragraph "Finished importing zones for Elk M1 - Click Next to continue"
			getChildDevice(state.ElkM1DNI).RequestTextDescriptions(deviceType, 1)
			state.creatingZone = false;
		}
	}
}

def createZone() {
	String zoneNumberFormatted
	if (zoneType.length() == 1 && zoneType >= 'A' && zoneType <= 'P') {
		zoneNumber = "ABCDEFGHIJKLMNOP".indexOf(zoneType) * 16 + zoneNumber.toInteger()
		zoneType = '07'
	}
	zoneNumberFormatted = String.format("%03d", zoneNumber)
	def zoneText
	ifDebug("Starting validation of ${zoneName} ZoneType: ${zoneType} ZoneNumber: ${zoneNumber}")
	getChildDevice(state.ElkM1DNI).createZone([zoneNumber: zoneNumberFormatted, zoneName: zoneName, zoneType: zoneType, zoneText: zoneText])
	state.creatingZone = false;
}

def editZone() {
	def childZone = getChildDevice(state.ElkM1DNI).getChildDevice(state.editedZoneDNI);
	ifDebug("Starting validation of ${childZone.label}")
	ifDebug("Attempting rename of zone to ${newZoneName}")
	childZone.updateSetting("label", [type: "text", value: newZoneName])
	newZoneName = null;
	state.editedZoneDNI = null;
}

private ifDebug(msg) {
	if (msg && isDebug) log.debug 'Elk M1 Module: ' + msg
}

//General App Events
def installed() {
	initialize()
}

def updated() {
	log.info "updated"
	initialize()
}

def initialize() {
	log.info "initialize"
	unsubscribe()
	state.creatingZone = false;
	subscribe(location, "hsmStatus", statusHandler)
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

def setHSMArm(String hsmSetArm, String description) {
	if (hsmSetArm == "disarm") {
		unlockIt()
		speakDisarmed()
	} else {
		lockIt()
		speakArmed()
	}

	String hsmStatus = location.hsmStatus
	if (isDebug && enableElktoHSM)
		log.debug "HSM arm changing from ${hsmStatus} to ${hsmSetArm}"
	if (enableElktoHSM && ((hsmSetArm == "armAway" && hsmStatus != "armedAway" && hsmStatus != "armingAway") ||
			(hsmSetArm == "armHome" && hsmStatus != "armedHome" && hsmStatus != "armingHome") ||
			(hsmSetArm == "armNight" && hsmStatus != "armedNight" && hsmStatus != "armingNight") ||
			(hsmSetArm == "disarm" && hsmStatus != "disarmed" && hsmStatus != "allDisarmed")))
		sendLocationEvent(name: "hsmSetArm", value: hsmSetArm, descriptionText: description)
}

def statusHandler(evt) {
	def lock
	if (enableHSMtoElk && !lock && evt?.value && evt.value != state.hsmStatus) {
		lock = true
		log.info "HSM Alert: $evt.value"
		ifDebug("HSM is enabled")
		switch (evt.value) {
			case "armedAway":
			case "armingAway":
				ifDebug("Sending Arm Away")
				if (getChildDevice(state.ElkM1DNI).currentValue("armState") == "Ready to Arm") {
					getChildDevice(state.ElkM1DNI).armAway()
				}
				break
			case "armedHome":
			case "armingHome":
				ifDebug("Sending Arm Home")
				if (getChildDevice(state.ElkM1DNI).currentValue("armState") == "Ready to Arm") {
					getChildDevice(state.ElkM1DNI).armStay()
				}
				break
			case "armedNight":
			case "armingNight":
				ifDebug("Sending Arm Night")
				if (getChildDevice(state.ElkM1DNI).currentValue("armState") == "Ready to Arm") {
					getChildDevice(state.ElkM1DNI).armNight()
				}
				break
			case "disarmed":
			case "allDisarmed":
				ifDebug("Sending Disarm")
				if (getChildDevice(state.ElkM1DNI).currentValue("armStatus") != "Disarmed") {
					getChildDevice(state.ElkM1DNI).disarm()
				}
				break
		}
		lock = false;
	}
	if (evt?.value)
		state.hsmStatus = evt.value
}

def speakArmed() {
	if (!armedBool) return
	if (armedText != "") {
		speakIt(armedText)
	}
}

def speakArmingAway() {
	if (!armingAwayBool) return
	if (armingAwayText) {
		speakIt(armingAwayText)
	} else {
		speakIt("Arming Away")
	}
}

def speakArmingVacation() {
	if (!armingVacationBool) return
	if (armingVacationText) {
		speakIt(armingVacationText)
	} else {
		speakIt("Arming Vacation")
	}
}

def speakArmingStay() {
	if (!armingStayBool) return
	if (armingStayText != "") {
		speakIt(armingStayText)
	}
}

def speakArmingNight() {
	if (!armingNightBool) return
	if (armingNightText != "") {
		speakIt(armingNightText)
	}
}

def speakDisarmed() {
	if (!disarmedBool) return
	if (disarmedText != "") {
		speakIt(disarmedText)
	}
}

def speakEntryDelay() {
	if (!entryDelayAlarmBool) return
	if (entryDelayAlarmText != "") {
		speakIt(entryDelayAlarmText)
	}
}

def speakAlarm() {
	if (!alarmBool) return
	if (alarmText != "") {
		speakIt(alarmText)
	}
}

private speakIt(str) {
	ifDebug("TTS: $str")
	if (state.speaking) {
		ifDebug("Already Speaking")
		runOnce(new Date(now() + 10000), speakRetry, [overwrite: false, data: [str: str]])
		return
	}

	if (!speechDevices) return;
	ifDebug("Found Speech Devices")

	state.speaking = true
	speechDevices.speak(str)

	if (notificationDevices) {
		ifDebug("Found Notification Devices")
		notificationDevices.deviceNotification(str)
	}
	state.speaking = false
}

private lockIt() {
	ifDebug("Lock")
	if (!armLocks) return
	ifDebug("Found Lock")
	armLocks.lock()
}

private unlockIt() {
	ifDebug("Unlock")
	if (!disarmLocks) return
	ifDebug("Found Lock")
	disarmLocks.unlock()
}


def speakRetry(data) {
	if (data.str) speakIt(data.str);
}

private removeChildDevices(delete) {
	delete.each { deleteChildDevice(it.deviceNetworkId) }
}

/***********************************************************************************************************************
 *
 * Release Notes
 *
 * Version: 0.2.0
 * Added import of Speech device.  Also finished TTS, Notification and HSM Integration code.
 *
 * Version: 0.1.11
 * Removed unused password setting.
 *
 * Version: 0.1.10
 * Added import of Lighting devices.
 *
 * Version: 0.1.9
 * Added Thermostat to list of automatic import devices.
 *
 * Version: 0.1.8
 * Added Keypad device type to import for their built in temperature sensor.
 *
 * Version: 0.1.7
 * Combined Zone Type creation "Contact" and "Motion" and made "Zone" to fix a zone creation bug.
 *
 * Version: 0.1.6
 * Moved some zone creation code to Elk Driver
 *
 * Version: 0.1.5
 * Added support for manual inclusion of Elk M1 outputs and tasks
 * Added support for importing Elk M1 outputs, tasks and thermostats
 * Cleaned up app and related code
 *
 * Version: 0.1.4
 * Added support for importing Elk M1 zones
 *
 * Version: 0.1.3
 * Removed any unsupported features for now
 *
 * Version: 0.1.2
 * Added thermostat support (data receipt only)
 *
 * Version: 0.1.1
 * Ported code from Doug Beard Envisalink Integration
 * HSM integration not currently supported
 * All functionality not fully tested
 *
 ***********************************************************************************************************************/
/***********************************************************************************************************************
 *
 * Feature Request & Known Issues
 *
 * I - Must initialize the Elk M1 Device prior to using the Elk M1 App Import Zone functions
 *
 */
