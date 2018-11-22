/*
TP-Link Plug and Switch Device Handler, 2018, Version 3.5

	Copyright 2018 Dave Gutheinz and Anthony Ramirez

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this  file except in compliance with the
License. You may obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, 
software distributed under the License is distributed on an 
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
either express or implied. See the License for the specific 
language governing permissions and limitations under the 
License.

Discalimer:  This Service Manager and the associated Device 
Handlers are in no way sanctioned or supported by TP-Link.  
All  development is based upon open-source data on the 
TP-Link devices; primarily various users on GitHub.com.

===== History ================================================
11.11.18	Update to Version 3.5:
			a.  Single Drivers for manual Hub Installation, 
				automated Hub Installation, and for Kasa
				Account based installation.
           b.	Update to match the Hubitat Driver input - 
				output paradigm.
//	===== Device Type Identifier ===========================*/
	def driverVer() { return "3.5" }
	def deviceType() { return "Multi-Plug" }
//	==========================================================

metadata {
	definition (name: "TP-Link/Kasa ${deviceType()}", 
    			namespace: "davegut", 
                author: "Dave Gutheinz") {
		capability "Switch"
        capability "Actuator"
		capability "Refresh"
	}

if (getDataValue("installType") != "Kasa Account")  {
    preferences {
		input ("device_Id", "text", title: "The Device ID Manual Installation")
		input ("device_IP", "text", title: "Device IP (Hub Only, NNN.NNN.N.NNN)")
		input ("gateway_IP", "text", title: "Gateway IP (Hub Only, NNN.NNN.N.NNN)")
	}
}
}

//	===== Update when installed or setting changed =====
def installed() {
	log.info "Installing ${device.label}..."
	if(getDataValue("installType") == null) {
		updateDataValue("installType", "Node Applet")
	}
	update()
}

def ping() {
	refresh()
}

def update() {
    runIn(2, updated)
}

def updated() {
	log.info "Updating ${device.label}..."
	unschedule()
    runEvery5Minutes(refresh)
    if (device_Id) { setDeviceId(device_Id) }
    if (device_IP) { setDeviceIP(device_IP) }
    if (gateway_IP) { setGatewayIP(gateway_IP) }
	runIn(5, refresh)
}

void uninstalled() {
	try {
		def alias = device.label
		log.info "Removing device ${alias} with DNI = ${device.deviceNetworkId}"
		parent.removeChildDevice(alias, device.deviceNetworkId)
	} catch (ex) {
		log.info "${device.name} ${device.label}: Either the device was manually installed or there was an error"
        log.info "No Parent Application"
	}
}

//	===== Basic Plug Control/Status =====
def on() {
	sendCmdtoServer('{"system" :{"set_relay_state" :{"state" : 1}}}', "deviceCommand", "commandResponse")
}

def off() {
	sendCmdtoServer('{"system" :{"set_relay_state" :{"state" : 0}}}', "deviceCommand", "commandResponse")
}

def refresh(){
	sendCmdtoServer('{"system" :{"get_sysinfo" :{}}}', "deviceCommand", "refreshResponse")
}

def refreshResponse(cmdResponse){
//	Requires parse update to new response format for multi-switches.
	def children = cmdResponse.system.get_sysinfo.children
	def onOff
	def plugId = "${getDataValue("deviceId")}00"
log.error plugId
	children.each {
		if (it.id == plugId) {
			log.error it.id
			log.error it.state
			if (it.state == 1) {
				onOff = "on"
			} else {
				onOff = "off"
			}
		}
	}
	sendEvent(name: "switch", value: onOff)
	log.info "${device.name} ${device.label}: Power: ${onOff}"
}
//	===== Send the Command =====
private sendCmdtoServer(command, hubCommand, action) {
	try {
    	def installType = getDataValue("installType")
		if (installType == "Kasa Account") {
			sendCmdtoCloud(command, hubCommand, action)
		} else {
	    	def deviceIP = getDataValue("deviceIP")
	        def gatewayIP = getDataValue("gatewayIP")
			if (deviceIP =~ null && gatewayIP =~ null) {
				sendEvent(name: "switch", value: "unavailable", descriptionText: "Please input Device IP / Gateway IP")
				sendEvent(name: "deviceError", value: "No Hub Address Data")
			} else {
				sendCmdtoHub(command, hubCommand, action)
			}
		}
	} catch (ex) {
		log.error "Sending Command Exception: ", ex
	}
}

private sendCmdtoCloud(command, hubCommand, action){
	def appServerUrl = getDataValue("appServerUrl")
	def deviceId = getDataValue("deviceId")
	def cmdResponse = parent.sendDeviceCmd(appServerUrl, deviceId, command)
	String cmdResp = cmdResponse.toString()
	if (cmdResp.substring(0,5) == "ERROR"){
		def errMsg = cmdResp.substring(7,cmdResp.length())
		log.error "${device.name} ${device.label}: ${errMsg}"
		sendEvent(name: "switch", value: "commsError", descriptionText: errMsg)
		sendEvent(name: "deviceError", value: errMsg)
		action = ""
	} else {
		sendEvent(name: "deviceError", value: "OK")
	}
	actionDirector(action, cmdResponse)
}

private sendCmdtoHub(command, hubCommand, action){
    def gatewayIP = getDataValue("gatewayIP")
    def deviceIP = getDataValue("deviceIP")
	def headers = [:] 
	headers.put("HOST", "$gatewayIP:8082")	//	Same as on Hub.
	headers.put("tplink-iot-ip", deviceIP)
	headers.put("tplink-command", command)
	headers.put("action", action)
	headers.put("command", hubCommand)
	sendHubCommand(new hubitat.device.HubAction([headers: headers], null, [callback: hubResponseParse]))
}

def hubResponseParse(response) {
	def action = response.headers["action"]
	def cmdResponse = parseJson(response.headers["cmd-response"])
	if (cmdResponse == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR - OffLine in hubResponseParse")
		sendEvent(name: "deviceError", value: "TCP Timeout in Hub")
	} else {
		sendEvent(name: "deviceError", value: "OK")
		actionDirector(action, cmdResponse)
	}
}

def actionDirector(action, cmdResponse) {
	switch(action) {
		case "commandResponse" :
			refresh()
			break
		case "refreshResponse" :
			refreshResponse(cmdResponse)
			break
		default:
			log.info "Interface Error. See SmartApp and Device error message."
	}
}

//	===== Child / Parent Interchange =====
def setAppServerUrl(newAppServerUrl) {
	updateDataValue("appServerUrl", newAppServerUrl)
	log.info "Updated appServerUrl for ${device.name} ${device.label}"
}

def setDeviceId(deviceId) { 
	updateDataValue("deviceId", deviceId) 	//	device Id for manual installation
	log.info "${device.name} ${device.label} device Id set to ${deviceId}"
}

def setDeviceIP(deviceIP) { 
	updateDataValue("deviceIP", deviceIP) 	//	gatewayIP must be in form NNN.NNN.N.NNN
	log.info "${device.name} ${device.label} device IP set to ${deviceIP}"
}

def setGatewayIP(gatewayIP) { 
	updateDataValue("gatewayIP", gatewayIP) 	//	gatewayIP must be in form NNN.NNN.N.NNN
	log.info "${device.name} ${device.label} gateway IP set to ${gatewayIP}"
}

def setPlugID(plugID) { 
	updateDataValue("plugID", plugID) 	//	plugID (deviceID plus 2 digit plug number)
	log.info "${device.name} ${device.label} plug ID set to ${plugID}"
}

def setInstallType(installType) {
	updateDataValue("installType", installType)
	log.info "${device.name} ${device.label} Installation Type set to ${installType}"
}
//end-of-file