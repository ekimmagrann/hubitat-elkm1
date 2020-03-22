/***********************************************************************************************************************
*
*  A Hubitat Driver using Telnet to connect to the Elk M1 via the M1XEP.
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
*  Name: Elk M1 TEST Driver
*
*  A Special Thanks to Doug Beard for the framework of this driver!
*
*  I am not a programmer so alot of this work is through trial and error. I also spent a good amount of time looking 
*  at other integrations on various platforms. I know someone else was working on an Elk driver that involved an ESP
*  setup. This is a more direct route using equipment I already owned.
*
***See Release Notes at the bottom***
***********************************************************************************************************************/

public static String version()      {  return "v0.1.1"  }
public static boolean isDebug() { return true }
import groovy.transform.Field
import java.util.regex.Matcher
import java.util.regex.Pattern;
    

metadata {
	definition (name: "Elk TEST Driver", namespace: "telk", author: "Mike Magrann") {
		capability "Initialize"
		capability "Telnet"
        command "ImportZones"

	}
	preferences {
		input("ip", "text", title: "IP Address", description: "ip", required: true)
		input("port", "text", title: "Port", description: "port", required: true)
        input("passwd", "text", title: "Password", description: "password", required: true)
        input("code", "text", title: "Code", description: "code", required: true)
	}
}

//general handlers
//def installed() {
//	log.warn "installed..."
//    initialize()
//   }
//def updated() {
//	ifDebug("updated...")
//  ifDebug("Configuring IP: ${ip}, Port${port}, Code: ${code}, Password: ${passwd}")
//	initialize()
//}
def initialize() {
    telnetClose() 
	try {
		//open telnet connection
		telnetConnect([termChars:[13,10]], ip, 2101, null, null)
		//give it a chance to start
		pauseExecution(1000)
//		ifDebug("Telnet connection to Elk M1 established")
        //poll()
	} catch(e) {
		log.warn "initialize error: ${e.message}"
	}
}
def uninstalled() {
    telnetClose() 
	removeChildDevices(getChildDevices())
}


//This for loop now works properly
def ImportZones(){
// 	ifDebug("request Text Descriptions()")
	def cmd = "sd"
	def type = "00";
	def future = "00";
	  for (i = 1; i <= 208; i++) {
	number = (i.toString());
		  		number = (number.padLeft(3,'0'));		  
	def msg = cmd + type + number + future;
	def len  = (msg.length()+2);
	String msgStr = Integer.toHexString(len);
	msgStr = (msgStr.toUpperCase());
    msgStr = (msgStr.padLeft(2,'0')); 
	msg = msgStr + msg
	def cc = generateChksum(msg);
		msg = msg + cc
sendHubCommand(new hubitat.device.HubAction(msg, hubitat.device.Protocol.TELNET))
	pauseExecution(50)
	  }
}

private generateChksum(String msg){
		def msgArray = msg.toCharArray()
        def msgSum = 0
        msgArray.each { (msgSum += (int)it) }
		msgSum = msgSum  % 256;
		msgSum = 256 - msgSum;
	 	String  chkSumStr = Integer.toHexString(msgSum);
		chkSumStr = (chkSumStr.toUpperCase());
	    chkSumStr = (chkSumStr.padLeft(2,'0')); 
}


private parse(String message) {
//    ifDebug("Parsing Incoming message: " + message)
		def commandCode = message.substring(2, 4);
		if (commandCode.matches("ZC")){
			def zoneNumber = message.substring(4, 7)
			def zoneStatus = message.substring(7, 8)
  if (zoneStatus == '9') {
//	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - "  + ViolatedOpen);
	  				zoneOpen(message)
//	              break;
  }  else {
  if (zoneStatus == 'B') {
//	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - "  + ViolatedShort);
//This should actually be zoneClosed but in order to show active status we are using zoneOpen.
	  				zoneOpen(message)
//	              break;
  } else {
  if (zoneStatus == '1') {
//	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - " + NormalOpen);
//This should actually be zoneOpen but in order to show inactive status we are using zoneClosed.
	  				zoneClosed(message)
//	              break;
  }  else {
  if (zoneStatus == '3') {
//	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - "  + NormalShort);
	  				zoneClosed(message)
//	              break;
  }  
	 else {
    zoneStatus = 'Unknown zone status message';
  }
//		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus);
  }
  }}}
		if (commandCode.matches("SD")){
			def type = message.substring(4, 6);
//			type = elkTextDescriptionsTypes[type];
			def name = message.substring(6, 9)
			def text = message.substring(9, 25)
			def zoneType;
		if (type.matches("04")){
			zoneType = "Output"
		}            
		else
		if (type.matches("05")){
			zoneType = "Task"
		} 		else
		if (type.matches("11")){
			zoneType = "Thermostat"
		}
 		else
		if (type.matches("02")){
			zoneType = "Thermostat"
		}
 		else
		if (type.matches("00")){
//			zoneType = "Contact"
		if (text.matches("(.*)Door(.*)")){
			zoneType = "Contact"
		} else 
		if (text.matches("(.*)door(.*)")){
			zoneType = "Contact"
		} else 
		if (text.matches("(.*)indow(.*)")){
			zoneType = "Contact"
		} else
			if (text.matches("(.*)Motion(.*)")){
			zoneType = "Motion"
			}
		}
			else
			zoneType = "Alert";
		if(name > '000'){
//			ifDebug("Zones: " + name + text + zoneType);
			createZones(name, text, zoneType)
		}
//            break;  		
		}
		 else {
//            ifDebug("The event is unknown"); 
//            break;
  }
}	

// Zone Status
def zoneOpen(message){
    def zoneDevice
    def substringCount = (message.length()-8);
     zoneDevice = getChildDevice("${device.deviceNetworkId}_C_${message.substring(substringCount).take(3)}")
    if (zoneDevice == null){
        zoneDevice = getChildDevice("${device.deviceNetworkId}_M_${message.substring(substringCount).take(3)}")
//		ifDebug("Motion Active")
		zoneDevice.active()
    }
		else
		{
//		ifDebug("Contact Open")
		zoneDevice.open()
    }

}
def zoneClosed(message){
    def zoneDevice
    def substringCount = (message.length()-8);
    zoneDevice = getChildDevice("${device.deviceNetworkId}_C_${message.substring(substringCount).take(3)}")
    if (zoneDevice == null){
        zoneDevice = getChildDevice("${device.deviceNetworkId}_M_${message.substring(substringCount).take(3)}")
//            ifDebug("Motion Inactive")
            zoneDevice.inactive()
    }
			else {
//            ifDebug("Contact Closed")
            zoneDevice.close()
        }
}

//NEW CODE
//Manage Zones
def createZones(name, text, zoneType){
//    log.info "Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: ${zoneType}"
   String deviceNetworkId
    def newDevice
	def ElkM1DNI = device.deviceNetworkId
//            ifDebug("DNI: " + ElkM1DNI); 
	def textLabel = "ZoneT " + name + " - " + text
	def taskLabel = "Task " + name + " - " + text
	def outputLabel = "Output " + name + " - " + text
	def tstatLabel = "Thermostat " + name + " - " + text
    if (zoneType == "Contact")
    {
	deviceNetworkId = ElkM1DNI + "_C_" + name
    	addChildDevice("hubitat", "Virtual Contact Sensor", deviceNetworkId, [name: textLabel, isComponent: false, label: textLabel])
	}
	else if (zoneType == "Motion")
{
	deviceNetworkId = ElkM1DNI + "_M_" + name
    	addChildDevice("hubitat", "Virtual Motion Sensor", deviceNetworkId, [name: textLabel, isComponent: false, label: textLabel])
        newDevice = getChildDevice(deviceNetworkId)
        newDevice.updateSetting("autoInactive",[type:"string", value:'disabled'])
}
	else if (zoneType == "Thermostat")
{
	deviceNetworkId = ElkM1DNI + "_T_" + name
    	addChildDevice("telk", "Elk M1 Driver Thermostat", deviceNetworkId, [name: tstatLabel, isComponent: false, label: tstatLabel])
        newDevice = getChildDevice(deviceNetworkId)
}	else if (zoneType == "Output")
{
	deviceNetworkId = ElkM1DNI + "_O_" + name
    	addChildDevice("telk", "Elk M1 Driver Outputs", deviceNetworkId, [name: outputLabel, isComponent: false, label: outputLabel])
        newDevice = getChildDevice(deviceNetworkId)
}
	else if (zoneType == "Task")
{
	deviceNetworkId = ElkM1DNI + "_K_" + name
    	addChildDevice("telk", "Elk M1 Driver Tasks", deviceNetworkId, [name: taskLabel, isComponent: false, label: taskLabel])
        newDevice = getChildDevice(deviceNetworkId)
}
	else {
        deviceNetworkId = ElkM1DNI + "_M_" + name
     	addChildDevice("hubitat", "Virtual Motion Sensor", deviceNetworkId, [name: textLabel, isComponent: false, label: textLabel])   
        newDevice = getChildDevice(deviceNetworkId)
        newDevice.updateSetting("autoInactive",[type:"string", value:'disabled'])
    }
}


def removeZone(zoneInfo){
    log.info "Removing ${zoneInfo.zoneName} with deviceNetworkId = ${zoneInfo.deviceNetworkId}"
    deleteChildDevice(zoneInfo.deviceNetworkId)
}

//Telnet
def getReTry(Boolean inc){
	def reTry = (state.reTryCount ?: 0).toInteger()
	if (inc) reTry++
	state.reTryCount = reTry
	return reTry
}

def telnetStatus(String status){
	log.warn "telnetStatus- error: ${status}"
	if (status != "receive error: Stream is closed"){
		getReTry(true)
		log.error "Telnet connection dropped..."
		initialize()
	} else {
		log.warn "Telnet is restarting..."
	}
}

//private ifDebug(msg)     
//{  
//	ifDebug('Connection Driver: ' + msg)
//}
	
/***********************************************************************************************************************
*
* Release Notes (see Known Issues Below)
*
* 0.1.1
* Intitial test to diagnose delays with motion lighting
*
***********************************************************************************************************************/
