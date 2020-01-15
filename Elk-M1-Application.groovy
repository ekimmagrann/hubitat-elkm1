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

public static String version() { return "v0.2.2" }

import groovy.transform.Field

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
	if (getChildDevices().size() == 0) {
		subscriptionCleanup()
	}
	app.removeSetting("elkM1Password")
	app.removeSetting("isDebug")
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
	if (dbgEnable)
		log.debug app.name + ": Showing mainPage"
	return dynamicPage(name: "mainPage", title: "", install: false, uninstall: true) {
		if (getChildDevices().size() == 0) {
			section("Define your Elk M1 device") {
				input "elkM1Name", "text", title: "Elk M1 Name", required: true, multiple: false, defaultValue: "Elk M1", submitOnChange: false
				input "elkM1IP", "text", title: "Elk M1 IP Address", required: true, multiple: false, defaultValue: "", submitOnChange: false
				input "elkM1Port", "text", title: "Elk M1 Port", required: true, multiple: false, defaultValue: "2101", submitOnChange: false
				input "elkM1Keypad", "text", title: "Elk M1 Keypad", required: true, multiple: false, defaultValue: "1", submitOnChange: false
				input "elkM1Code", "text", title: "Elk M1 User Code", required: true, multiple: false, defaultValue: "", submitOnChange: false
			}
		} else {
			section("<h1>Device Mapping and Integration</h1>") {
				href(name: "zoneMapsPage", title: "Devices",
						description: "Create Virtual Devices and Map them to Existing Zone, Output, Task, Lighting, Thermostat, Keypad, " +
								"Custom, Counter and/or Speech Devices in your Elk M1 setup.\nIntegrate virtual Elk M1 devices with physical devices.",
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
			input "dbgEnable", "bool", title: "Enable Debug Logging", required: false, multiple: false, defaultValue: false, submitOnChange: true
		}

	}
}

def aboutPage() {
	if (dbgEnable)
		log.debug app.name + ": Showing aboutPage"

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
	if (dbgEnable)
		log.debug app.name + ": Showing lockPage"

	dynamicPage(name: "lockPage", title: none) {
		section("<h1>Locks</h1>") {
			paragraph "Enable Lock Integration, selected locks will lock when armed and/or unlock when disarmed"
			input "armLocks", "capability.lock", title: "Which locks to lock when armed?", required: false, multiple: true, submitOnChange: true
			input "disarmLocks", "capability.lock", title: "Which locks to unlock when disarmed?", required: false, multiple: true, submitOnChange: true
		}
	}
}

def hsmPage() {
	if (dbgEnable)
		log.debug app.name + ": Showing hsmPage"
	if (enableHSMtoElk)
		subscribe(location, "hsmStatus", statusHandler)
	else
		unsubscribe(location, "hsmStatus")
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
	if (dbgEnable)
		log.debug app.name + ": Showing zoneMapsPage"
	if (getChildDevices().size() == 0) {
		createElkM1ParentDevice()
	}

	if (state.creatingZone) {
		createZone()
	}
	state.editedZoneDNI = null
	Map integrationMap = [:]
	settings.findAll { it.key.startsWith("integrate:") }.each {
		String[] integrations = it.key.split(':')
		if (integrations.size() > 2) {
			if (integrationMap[integrations[1]] == null) {
				integrationMap[integrations[1]] = [integrations[2]]
			} else {
				integrationMap[integrations[1]] << integrations[2]
			}
		}
	}
	dynamicPage(name: "zoneMapsPage", title: "", install: true, uninstall: false) {

		section("<h1>Device Maps</h1>") {
			paragraph "The partition of your Elk M1 Installation may consist of Zone, Output, Task, Lighting, Thermostat, Keypad, " +
					"Custom, Counter and Speech Devices.  You can choose to map the devices manually or use the import method. "
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

		String description = ""
		def deviceInfo = getChildDevice(state.ElkM1DNI)
		Boolean hasCapabilities
		section("<h2>Existing Devices</h2>") {
			href(name: "editParent", title: "${deviceInfo.label}",
					description: "Click to edit parent device",
					url: "/device/edit/${deviceInfo.id}")
			paragraph "View or change the integration of virtual Elk M1 child devices with physical devices."
			getChildDevice(state.ElkM1DNI).getChildDevices().sort { it.deviceNetworkId }.each {
				hasCapabilities = false
				it.getCapabilities().each {
					if (capabilityMap[it.name] != null)
						hasCapabilities = true
				}
				if (integrationMap[it.deviceNetworkId] != null) {
					description = "Integrated using " + integrationMap[it.deviceNetworkId].join(', ')
				} else if (hasCapabilities) {
					description = "Add Integrations"
				} else {
					description = "Device Details"
				}
				href(name: "editZoneMapPage", title: "${it.label}",
						description: description,
						params: [deviceNetworkId: it.deviceNetworkId],
						page: "editZoneMapPage")
			}
		}
	}
}

def defineZoneMap() {
	if (dbgEnable)
		log.debug app.name + ": Showing defineZoneMap"
	state.creatingZone = true;
	dynamicPage(name: "defineZoneMap", title: "") {
		section("<h1>Create a Device Map</h1>") {
			paragraph "Create a Map for a device in Elk M1"
			input "zoneName", "text", title: "Device Name", required: true, multiple: false, defaultValue: "Zone x", submitOnChange: false
			input "zoneNumber", "number", title: "Which Device 1 - 208", required: true, multiple: false, defaultValue: 1, range: "1..208", submitOnChange: false
			input "zoneType", "enum", title: "Zone, Output, Task, Thermostat, Keypad, Custom, Counter or Speech Device?", required: true, multiple: false,
					options: [['00': "Zone (1 - 208)"], ['04': "Output (1 - 208)"], ['05': "Task (1 - 32)"], ['11': "Thermostat (1 - 16)"],
							  ['03': "Keypad (1 - 16)"], ['09': "Custom (1 - 20)"], ['10': "Counter (1 - 64)"], ['SP': "Speech (1)"]]
		}
	}
}

def defineLightMap() {
	if (dbgEnable)
		log.debug app.name + ": Showing defineLightMap"
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
	if (dbgEnable)
		log.debug app.name + ": Showing defineZoneMapImport"
	getChildDevice(state.ElkM1DNI).initialize()
	state.creatingZone = true;
	dynamicPage(name: "defineZoneMapImport", title: "") {
		section("<h1>Import Elk Zones</h1>") {
			paragraph "Create a Map for a zone in Elk M1"
			input "deviceType", "enum", title: "Select Device Type", required: true, multiple: false,
					options: [['00': "Zones"], ['04': "Output"], ['05': "Task"], ['07': "Lighting"], ['11': "Thermostat"], ['03': "Keypad"],
							  ['09': "Custom"], ['10': "Counter"]]
		}
	}
}

def editZoneMapPage(message) {
	if (message != null) {
		state.editedZoneDNI = message.deviceNetworkId
	}
	if (dbgEnable) {
		log.debug app.name + ": Showing editZoneMapPage"
		log.debug app.name + ": editing ${state.editedZoneDNI}"
	}
	def zoneDevice = getChildDevice(state.ElkM1DNI).getChildDevice(state.editedZoneDNI)
	String paragraphText = "No device capabilities exist that can be integrated.  Current capabilities are:\n"
	List<String> capList = subscribeDevice(zoneDevice)
	if (capList.size() == 0) {
		zoneDevice.getCapabilities().each {
			if (it.name != "Actuator")
				paragraphText = paragraphText + it.name + "\n"
		}
	} else {
		paragraphText = "Choose any devices you want to integrate with ${zoneDevice.label}"
	}

	dynamicPage(name: "editZoneMapPage", title: "") {
		section("<h1>${zoneDevice.label}</h1>") {
			href(name: "editChild", title: "Edit Device",
					description: "Click to edit this device",
					url: "/device/edit/${zoneDevice.id}")
			paragraph paragraphText
			capList.each { cap ->
				input "integrate:$state.editedZoneDNI:$cap", "${capabilityMap[cap]}", title: "$cap devices to integrate", required: false, multiple: true, submitOnChange: true
			}
		}
	}
}

List<String> subscribeDevice(def zoneDevice) {
	String targetDNID = zoneDevice.deviceNetworkId
	String targetDevID = zoneDevice.id
	String attribute
	String triggerValue
	String attributeValue
	List<String> capList = []
	zoneDevice.getCapabilities().each {
		if (capabilityMap[it.name] != null)
			capList << it.name
	}
	Map mySubscription = [:]
	atomicState.subscriptionMap.each { k, v ->
		List subscriptions = []
		v.findAll { !it.startsWith(targetDNID + ":") }.each {
			subscriptions << it
		}
		if (subscriptions.size() > 0)
			mySubscription[k] = subscriptions
	}
	Map myRevSubscription = atomicState.RevSubscriptionMap
	myRevSubscription.remove(targetDevID)
	Boolean stateSaved = false
	capList.each { capability ->
		com.hubitat.app.DeviceWrapperList deviceList = settings["integrate:$targetDNID:$capability"]
		deviceList.each {
			if (it?.deviceNetworkId != targetDNID) {
				//it?.properties?.each { item -> log.debug "$item.key = $item.value" }
				if (dbgEnable)
					log.debug app.name + ": Subscribing ${it.label} capability ${capability} to target ${zoneDevice.label}"
				if (mySubscription[it.deviceId] == null) {
					mySubscription[it.deviceId] = [targetDNID + ":" + capability]
				} else {
					mySubscription[it.deviceId] << (targetDNID + ":" + capability)
					mySubscription[it.deviceId] = mySubscription[it.deviceId].unique()
				}
				if (myRevSubscription[targetDevID] == null) {
					myRevSubscription[targetDevID] = [targetDNID + ":" + capability]
				} else {
					myRevSubscription[targetDevID] << (targetDNID + ":" + capability)
					myRevSubscription[targetDevID] = myRevSubscription[targetDevID].unique()
				}
				atomicState.subscriptionMap = mySubscription
				atomicState.RevSubscriptionMap = myRevSubscription
				stateSaved = true
				attributeMap.each { key, value ->
					if (key.startsWith(capability + ":")) {
						attribute = key.substring(capability.length() + 1)
						subscribe(it, attribute, integrationHandler)
						subscribe(zoneDevice, attribute, revIntegrationHandler)
						int ndx = attribute.indexOf('.')
						if (ndx < 1) {
							triggerValue = ""
						} else {
							triggerValue = attribute.substring(ndx + 1)
							attribute = attribute.substring(0, ndx)
						}
						attributeValue = it.currentState(attribute)?.value
						if (attributeValue != null && (triggerValue == "" || triggerValue == attributeValue))
							it.sendEvent(name: attribute, value: attributeValue, isStateChange: true)
					}
				}
			}
		}
	}
	if (!stateSaved) {
		atomicState.subscriptionMap = mySubscription
		atomicState.RevSubscriptionMap = myRevSubscription
	}
	return capList
}

def subscriptionCleanup() {
	Boolean fixSubscriptions = false
	def zoneDevice
	settings.findAll { it.key.startsWith("integrate:") }.each {
		String[] integrations = it.key.split(':')
		if (integrations.size() < 3) {
			log.trace app.name + ": Removing malformed integration setting $it.key"
			app.removeSetting(it.key)
			fixSubscriptions = true
		} else {
			zoneDevice = getChildDevice(state.ElkM1DNI).getChildDevice(integrations[1])
			if (zoneDevice == null) {
				log.trace app.name + ": Removing integration setting for missing child device $it.key"
				app.removeSetting(it.key)
				fixSubscriptions = true
			} else if (!zoneDevice.hasCapability(integrations[2])) {
				log.trace app.name + ": Removing integration setting for child device no longer supporting capability $it.key"
				app.removeSetting(it.key)
				fixSubscriptions = true
			} else {
				it.value.each { otherDevice ->
					if (!otherDevice.hasCapability(integrations[2])) {
						log.trace app.name + ": ${otherDevice.label} no longer supports capability ${integrations[2]}"
						fixSubscriptions = true
					}
				}
			}
		}
	}
	if (fixSubscriptions) {
		resubscribe()
	} else {
		if (atomicState.subscriptionMap == null)
			atomicState.subscriptionMap = [:]
		if (atomicState.RevSubscriptionMap == null)
			atomicState.RevSubscriptionMap = [:]
	}
}

int resubscribe() {
	int cnt = 0
	String lastDNID = ""
	log.trace app.name + ": Rebuilding integration subscriptions"
	unsubscribe()
	if (enableHSMtoElk)
		subscribe(location, "hsmStatus", statusHandler)
	atomicState.subscriptionMap = [:]
	atomicState.RevSubscriptionMap = [:]
	settings.findAll { it.key.startsWith("integrate:") }.each {
		String[] integrations = it.key.split(':')
		if (integrations[1] != lastDNID) {
			zoneDevice = getChildDevice(state.ElkM1DNI).getChildDevice(integrations[1])
			if (zoneDevice != null) {
				subscribeDevice(zoneDevice)
				cnt++
			}
			lastDNID = integrations[1]
		}
	}
	return cnt
}

def createElkM1ParentDevice() {
	if (dbgEnable)
		log.debug app.name + ": Creating Parent ElkM1 Device"
	if (getChildDevice(state.ElkM1DNI) == null) {
		state.ElkM1DNI = UUID.randomUUID().toString()
		if (dbgEnable)
			log.debug app.name + ": Setting state.ElkM1DNI ${state.ElkM1DNI}"
		addChildDevice("belk", "Elk M1 Driver", state.ElkM1DNI, null, [name: elkM1Name, isComponent: true, label: elkM1Name])
		getChildDevice(state.ElkM1DNI).updateSetting("ip", [type: "text", value: elkM1IP])
		getChildDevice(state.ElkM1DNI).updateSetting("port", [type: "text", value: elkM1Port])
		getChildDevice(state.ElkM1DNI).updateSetting("keypad", [type: "text", value: elkM1Keypad])
		getChildDevice(state.ElkM1DNI).updateSetting("code", [type: "text", value: elkM1Code])
		if (dbgEnable) {
			if (getChildDevice(state.ElkM1DNI))
				log.debug app.name + ": Found a Child Elk M1 ${getChildDevice(state.ElkM1DNI).label}"
			else
				log.debug app.name + ": Did not find a Parent Elk M1"
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
	if (dbgEnable)
		log.debug app.name + ": Starting validation of ${zoneName} ZoneType: ${zoneType} ZoneNumber: ${zoneNumber}"
	getChildDevice(state.ElkM1DNI).createZone([zoneNumber: zoneNumberFormatted, zoneName: zoneName, zoneType: zoneType, zoneText: zoneText])
	state.creatingZone = false;
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
	state.creatingZone = false;
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
	if (dbgEnable && enableElktoHSM)
		log.debug "HSM arm changing from ${hsmStatus} to ${hsmSetArm}"
	if (enableElktoHSM && ((hsmSetArm == "armAway" && hsmStatus != "armedAway" && hsmStatus != "armingAway") ||
			(hsmSetArm == "armHome" && hsmStatus != "armedHome" && hsmStatus != "armingHome") ||
			(hsmSetArm == "armNight" && hsmStatus != "armedNight" && hsmStatus != "armingNight") ||
			(hsmSetArm == "disarm" && hsmStatus != "disarmed" && hsmStatus != "allDisarmed")))
		sendLocationEvent(name: "hsmSetArm", value: hsmSetArm, descriptionText: description)
}

def integrationHandler(com.hubitat.hub.domain.Event evt) {
	//log.debug "Unhandled integration event:"
	//evt?.properties?.each { item -> log.debug "$item.key = $item.value" }
	//log.debug "Device $evt.displayName, attribute $evt.name = $evt.value"
	int triggerCnt = 0
	def zoneDevice
	String cmd
	List targetList = atomicState.subscriptionMap[evt.deviceId.toString()]
	if (targetList != null) {
		targetList.each {
			String[] targetArr = it.split(':')
			if (targetArr.size() > 1) {
				cmd = attributeMap[targetArr[1] + ":" + evt.name + "." + evt.value]
				if (cmd == null)
					cmd = attributeMap[targetArr[1] + ":" + evt.name]
				if (cmd != null) {
					zoneDevice = getChildDevice(state.ElkM1DNI).getChildDevice(targetArr[0])
					if (zoneDevice != null && zoneDevice.hasCapability(targetArr[1])) {
						triggerCnt += updateDevice(evt, zoneDevice, cmd)
					}
				}
			}
		}
	}
	if (triggerCnt == 0) { // This subscription is no longer needed
		unsubscribe(evt.getDevice(), evt.name, integrationHandler)
		unsubscribe(evt.getDevice(), evt.name + "." + evt.value, integrationHandler)
	}
}

def revIntegrationHandler(com.hubitat.hub.domain.Event evt) {
	//log.debug "Device $evt.displayName, attribute $evt.name = $evt.value"
	int triggerCnt = 0
	String cmd
	List targetList = atomicState.RevSubscriptionMap[evt.deviceId.toString()]
	if (targetList != null) {
		targetList.each {
			String[] targetArr = it.split(':')
			if (targetArr.size() > 1) {
				cmd = attributeMap[targetArr[1] + ":" + evt.name + "." + evt.value]
				if (cmd == null)
					cmd = attributeMap[targetArr[1] + ":" + evt.name]
				if (cmd != null) {
					settings["integrate:${targetArr[0]}:${targetArr[1]}"]?.each { zoneDevice ->
						if (zoneDevice?.hasCapability(targetArr[1])) {
							triggerCnt += updateDevice(evt, zoneDevice, cmd)
						}
					}
				}
			}
		}
	}
	if (triggerCnt == 0) { // This subscription is no longer needed
		unsubscribe(evt.getDevice(), evt.name, revIntegrationHandler)
		unsubscribe(evt.getDevice(), evt.name + "." + evt.value, revIntegrationHandler)
	}
}

int updateDevice(com.hubitat.hub.domain.Event evt, zoneDevice, String cmd) {
	//log.debug "Device $evt.displayName, attribute $evt.name = $evt.value, todevice $zoneDevice.label, command: $cmd"
	int triggerCnt = 0
	if (cmd != "attributeonly" && zoneDevice.hasCommand(cmd)) {
		triggerCnt = 1
		if (zoneDevice.currentState(evt.name)?.value == null || zoneDevice.currentState(evt.name).value != evt.value) {
			if (dbgEnable)
				log.debug app.name + ": ${evt.displayName} triggering command ${cmd}(${evt.value}) on ${zoneDevice.label}"
			switch (cmd) {
				case "off":
					zoneDevice.off()
					break
				case "open":
					zoneDevice.open()
					break
				case "close":
					zoneDevice.close()
					break
				case "on":
					zoneDevice.on()
					break
				case "push":
					zoneDevice.push(evt.integerValue)
					break
				case "setLevel":
					zoneDevice.setLevel(evt.integerValue)
					break
				case "setCoolingSetpoint":
					zoneDevice.setCoolingSetpoint(evt.integerValue)
					break
				case "setHeatingSetpoint":
					zoneDevice.setHeatingSetpoint(evt.integerValue)
					break
				case "setThermostatFanMode":
					zoneDevice.setThermostatFanMode(evt.value)
					break
				case "setThermostatMode":
					zoneDevice.setThermostatMode(evt.value)
					break
				case "setThermostatTemperature":
					zoneDevice.setThermostatTemperature(evt.integerValue)
					break
			}
		}
	} else if (zoneDevice.hasAttribute(evt.name)) {
		triggerCnt = 1
		if (zoneDevice.currentState(evt.name)?.value == null || zoneDevice.currentState(evt.name).value != evt.value) {
			if (dbgEnable)
				log.debug app.name + ": ${evt.displayName} setting attribute ${evt.name} = ${evt.value} on ${zoneDevice.label}"
			zoneDevice.sendEvent(name: evt.name, value: evt.value)
		}
	}
	return triggerCnt
}

def statusHandler(com.hubitat.hub.domain.Event evt) {
	if (evt?.source == "LOCATION" && evt?.name == "hsmStatus" && evt?.value != null) {
		def lock
		if (enableHSMtoElk && !lock && evt?.value && evt.value != state.hsmStatus) {
			lock = true
			log.info "HSM Alert: $evt.value"
			if (dbgEnable)
				log.debug app.name + ": HSM is enabled"
			switch (evt.value) {
				case "armedAway":
				case "armingAway":
					if (dbgEnable)
						log.debug app.name + ": Sending Arm Away"
					if (getChildDevice(state.ElkM1DNI).currentValue("armState") == "Ready to Arm") {
						getChildDevice(state.ElkM1DNI).armAway()
					}
					break
				case "armedHome":
				case "armingHome":
					if (dbgEnable)
						log.debug app.name + ": Sending Arm Home"
					if (getChildDevice(state.ElkM1DNI).currentValue("armState") == "Ready to Arm") {
						getChildDevice(state.ElkM1DNI).armStay()
					}
					break
				case "armedNight":
				case "armingNight":
					if (dbgEnable)
						log.debug app.name + ": Sending Arm Night"
					if (getChildDevice(state.ElkM1DNI).currentValue("armState") == "Ready to Arm") {
						getChildDevice(state.ElkM1DNI).armNight()
					}
					break
				case "disarmed":
				case "allDisarmed":
					if (dbgEnable)
						log.debug app.name + ": Sending Disarm"
					if (getChildDevice(state.ElkM1DNI).currentValue("armStatus") != "Disarmed") {
						getChildDevice(state.ElkM1DNI).disarm()
					}
					break
			}
			lock = false;
		}
		state.hsmStatus = evt.value
	} else {
		log.debug "Unhandled event:"
		evt?.properties?.each { item -> log.debug "$item.key = $item.value" }
	}
}

def speakArmed() {
	if (!armedText)
		armedText = "Armed"
	speakIt(armedText, armedBool)
}

def speakArmingAway() {
	if (!armingAwayText)
		armingAwayText = "Arming Away"
	speakIt(armingAwayText, armingAwayBool)
}

def speakArmingVacation() {
	if (!armingVacationText)
		armingVacationText = "Arming Vacation"
	speakIt(armingVacationText, armingVacationBool)
}

def speakArmingStay() {
	if (!armingStayText)
		armingStayText = "Arming Stay"
	speakIt(armingStayText, armingStayBool)
}

def speakArmingNight() {
	if (!armingNightText)
		armingNightText = "Arming Night"
	speakIt(armingNightText, armingNightBool)
}

def speakDisarmed() {
	if (!disarmedText)
		disarmedText = "Disarmed"
	speakIt(disarmedText, disarmedBool)
}

def speakEntryDelay() {
	if (!entryDelayAlarmText)
		entryDelayAlarmText = "Entry Delay in Progress, Alarm eminent"
	speakIt(entryDelayAlarmText, entryDelayAlarmBool)
}

def speakAlarm() {
	if (!alarmText)
		alarmText = "Alarm, Alarm, Alarm, Alarm, Alarm"
	speakIt(alarmText, alarmBool)
}

private speakIt(String str, boolean isEnabled = true) {
	if (dbgEnable)
		log.debug app.name + ": TTS: $str"
	if (isEnabled && speechDevices) {
		if (atomicState.speaking) {
			if (dbgEnable)
				log.debug app.name + ": Already Speaking"
			runOnce(new Date(now() + 10000), speakRetry, [overwrite: false, data: [str: str]])
		} else {
			if (dbgEnable)
				log.debug app.name + ": Found Speech Devices"

			atomicState.speaking = true
			speechDevices.speak(str)
			atomicState.speaking = false

			if (notificationDevices) {
				if (dbgEnable)
					log.debug app.name + ": Found Notification Devices"
				notificationDevices.deviceNotification(str)
			}
		}
	}
}

private lockIt() {
	if (dbgEnable)
		log.debug app.name + ": Lock"
	if (armLocks) {
		if (dbgEnable)
			log.debug app.name + ": Found Lock"
		armLocks.lock()
	}
}

private unlockIt() {
	if (dbgEnable)
		log.debug app.name + ": Unlock"
	if (disarmLocks) {
		if (dbgEnable)
			log.debug app.name + ": Found Lock"
		disarmLocks.unlock()
	}
}

def speakRetry(data) {
	if (data.str) speakIt(data.str);
}

private removeChildDevices(delete) {
	delete.each { deleteChildDevice(it.deviceNetworkId) }
}

@Field final Map capabilityMap = [
		"PushableButton"             : "capability.pushableButton",
		"RelativeHumidityMeasurement": "capability.relativeHumidityMeasurement",
		"Switch"                     : "capability.switch",
		"SwitchLevel"                : "capability.switchLevel",
		"Thermostat"                 : "capability.thermostat"
]

@Field final Map attributeMap = [
		"PushableButton:pushed"                 : "push",
		"RelativeHumidityMeasurement:humidity"  : "attributeonly",
		"Switch:switch.on"                      : "on",
		"Switch:switch.off"                     : "off",
		"SwitchLevel:level"                     : "setLevel",
		"Thermostat:coolingSetpoint"            : "setCoolingSetpoint",
		"Thermostat:heatingSetpoint"            : "setHeatingSetpoint",
		"Thermostat:supportedThermostatFanModes": "attributeonly",
		"Thermostat:supportedThermostatModes"   : "attributeonly",
		"Thermostat:temperature"                : "setThermostatTemperature",
		"Thermostat:thermostatFanMode"          : "setThermostatFanMode",
		"Thermostat:thermostatMode"             : "setThermostatMode",
		"Thermostat:thermostatOperatingState"   : "attributeonly",
		"Thermostat:thermostatSetpoint"         : "attributeonly"
]

/***********************************************************************************************************************
 *
 * Release Notes
 *
 * Version: 0.2.2
 * Added import of Custom and Counter value devices.
 *
 * Version: 0.2.1
 * Added integration of virtual child devices with physical HE devices.
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
