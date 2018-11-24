/*
TP-Link/Kasa Hubitat Environment Manager, 2018 Version 3.5

	Copyright 2018 Dave Gutheinz

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License.

Discalimer: This Service Manager and the associated Device
Handlers are in no way sanctioned or supported by TP-Link. All
development is based upon open-source data on the TP-Link Kasa Devices;
primarily various users on GitHub.com.

===============================================================
11-11-18.	Update Manager to version 3.5 nomenclature.  This
			is a complete rework with new elements.  Added:
			a.  Manage (add, delete) both Hub and Cloud-based
				devices.
			b.	Ability to change Hub bridge IP as well as
				update Cloud Kasa Token.
			c.	Ability to remove selected devices.
			d.	Removal will remove all devices.
11-24-18	Added support for HS107 (tested) and HS300 plugs.
			Awaiting confirmation on HS300 from user.
=============================================================*/

//	====== Application Information ============================
	def appLabel() { return "Hubitat TP-Link/Kasa Manager" }
	def appVersion() { return "3.5" }
	def driverVersion() { return "3.5" }
//	===========================================================

definition (
	name: appLabel(),
	namespace: "davegut", 
	author: "Dave Gutheinz", 
	description: "Hubitat TP-Link/Kasa Service Manager for both cloud and hub connected devices.", 
	category: "Convenience", 
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
	singleInstance: true
)

preferences {
	page(name: "startPage")
	page(name: "welcomePage")
	page(name: "hubEnterIpPage")
	page(name: "kasaAuthenticationPage")
	page(name: "addDevicesPage")
	page(name: "removeDevicesPage")
	page(name: "updateData")
}

def setInitialStates() {
	if (!state.TpLinkToken) { state.TpLinkToken = null }
	if (!state.devices) {state.devices = [:]}
	if (!state.currentError) {state.currentError = null}
	if (!state.errorCount) {state.errorCount = 0}
    if (!state.installed) {state.installed = false}
    if (!state.updateType) {state.updateType = null}
}

//	===== Main Pages =====================
def startPage() {
	setInitialStates()
    if (state.installed == true) { return welcomePage() }
    def page1Text = ""
	def page2Text = ""
    def page3Text = ""
    def page4Text = ""
    def page5Text = ""
    def page6Text = ""
	page1Text += "Before the installation, the installation type must be entered.  There are two options:"
    page2Text += "Kasa Account:  This is the cloud based entry.  It requires that the user provide "
    page2Text += "enter their Kasa Account login and password to install the devices."
    page3Text += "Node Applet:  This installation requires several items. (1) An always on server (PC, Android, "
    page3Text += "or other device).  (2) The provided node.js applet up and running on the Server.  (3) Static "
    page3Text += "IP addresses for the server (bridge/hub).  It does not require login credentials."
    page4Text += "After selecting the mode, you will be sent a page to enter your Kasa Login Information "
    page4Text += "or enter the IP address of your Hub."
    page5Text += "Once you enter the connection information, the program will direct you to add devices. "
    page5Text += "The application will poll for your devices, allow you to select found devices, and "
    page5Text += "finally install the devices and application to Hubitat Environment."
    page6Text += "Once the application is already installed, you not see this page.  Instead you will be"
    page6Text += "directed to a page where you can select add devices, remove devices, and set device preferences."

    return dynamicPage (name: "startPage",
                        title: "Select Installation Type", 
                        install: false, 
                        uninstall: true) {
		section("") {
			if (state.currentError != null) {
				paragraph "ERROR:  ${state.currentError}! Correct before continuing."
			} else {
				paragraph "No detected program errors!"
            }
		}
        
		section("Page Instructions", hideable: true, hidden: true) {
            paragraph page1Text
            paragraph page2Text
            paragraph page3Text
            paragraph page4Text
            paragraph page5Text
            paragraph page6Text
		}
        
		section("") {
			input ("installType", "enum", 
                   title: "Select Installation Type", 
                   required: false,
                   multiple: false, 
            	   submitOnChange: true, 
                   options: ["Kasa Account", "Node Applet"])
 		}
		section("") {
            if (installType == "Kasa Account") {
				href "kasaAuthenticationPage", 
                    title: "Kasa Login and Token Update", 
                    description: "Go to Kasa Login Update"
            }	else if (installType == "Node Applet") {
				href "hubEnterIpPage", 
                    title: "Install Node Applet Devices", 
                    description: "Go to Enter Gateway IP"
			}
        }
        
        section("Copyright Dave Gutheinz and Anthony Rameriz", hideable: true, hidden: false) {
            paragraph "Application Version: ${appVersion()}"
            paragraph "Minimum Guaranteed Driver Version: ${driverVersion()}"
		}
	}
}

def welcomePage() {
	def page1Text = ""
	def page2Text = ""
	def page3Text = ""
	def page4Text = ""
    page1Text += "Install Kasa Devices:  Detects devices on your network that are not "
    page1Text += "installed and then installs them."
    page2Text += "Remove Installed Kasa Devices.  Allows removal of selected installed "
    page2Text += "Kasa Devices. Note: to remove all devices and the application, simply "
    page2Text += "delete the Application."
    if (insallType == "Kasa Account") {
    	page3Text += "Kasa Login and Token Update.  Allows for maintenance update of "
        page3Text += "the Kasa Account credentials."
        page4Text += "Note: For Cloud-based installation, the devices must be in remote "
	    page4Text += "control mode via the Kasa App."
    } else {
	    page3Text += "Update Gateway IP.  Ability to update the Gateway IP."
	    page4Text += "Update Device IPs.  Update  all device IPs device IPs."
    }
    return dynamicPage (name: "welcomePage", 
                        title: "Main Page", 
                        install: false, 
                        uninstall: true) {
		section("") {
			if (state.currentError != null) {
				paragraph "ERROR:  ${state.currentError}! Correct before continuing."
			} else {
				paragraph "No detected program errors!"
            }
		}
        
		section("Page Instructions", hideable: true, hidden: true) {
            paragraph page1Text
            paragraph page2Text
            paragraph page3Text
			paragraph page4Text
        }
           
		section("Available Device Management Functions", hideable: true, hidden: false) {
			href "addDevicesPage", 
                title: "Install Kasa Devices", 
                description: "Go to Install Devices"
			href "removeDevicesPage", 
                title: "Remove Installed Kasa Devices", 
                description: "Go to Remove Devices"
            if (installType == "Kasa Account") {
				href "kasaAuthenticationPage", 
                    title: "Kasa Login and Token Update", 
                    description: "Go to Kasa Login Update"
            }	else {
				href "hubEnterIpPage", 
                    title: "Update Gateway IP", 
                    description: "Update Gateway IP"
                state.updateType = "deviceIpUpdate"
                href "updateData", 
                    title: "Update Device IPs", 
                    description: "Update Node Device IPs"
			}
		}
        
        section("Copyright Dave Gutheinz and Anthony Rameriz", hideable: true, hidden: false) {
            paragraph "Application Version: ${appVersion()}"
            paragraph "Minimum Guaranteed Driver Version: ${driverVersion()}"
        }
	}
}

//	===== Generic Methods ================
def updateData() {
	switch(state.updateType) {
		case "hubIpUpdate" :
		    def devices = state.devices
			devices.each {
				def isChild = getChildDevice(it.value.deviceNetworkId)
				if (isChild) { isChild.setGatewayIP(bridgeIp) }
			}
			break
        case "deviceIpUpdate" :
        	hubGetDevices()
        	break
		case "KasaTokenUpdate" :
	        getToken()
			break
        case "addDevices" :
        	addDevices()
        	break
        case "removeDevices" :
        	removeDevices()
        	break
		default:
            break
	}
    welcomePage()
}

def addDevicesPage() {
    state.updateType = "addDevices"
    def page1Text = ""
    def page2Text = ""
    def page3Text = ""
    def page4Text = ""
    def page5Text = ""
    def page6Text = ""
    if (installType == "Node Applet") {
		hubGetDevices()
		page1Text += "This page installs the devices through the running a Node "
	    page1Text += "Applet. On initial installation, an error will be displayed "
	    page1Text += "until the Applet returns the device data."
	    page2Text += "1.  Assure that the node applet is running."
    } else {
		getToken()
		kasaGetDevices()
	 	page1Text += "This page installs the devices through the your Kasa Account. "
	    page1Text += "On initial installation, an error may be displayed until the "
	    page1Text += "Kasa Cloud returns the device data."
	    page2Text += "1.  Devices that have not been previously installed and are "
	    page2Text += "not in 'Local WiFi control only' will appear."
    }
    page3Text += "2.  Wait for a device count equal to your devices to appear in "
    page3Text += "'Selet Devices to Add' string."
    page4Text += "3.  Select the devices you want to install."
    page5Text += "4.  Select 'DONE' to install the devices "
    page5Text += "and install the Application."
    page6Text += "5.  To cancel, select 'Don't Add Devices' at the bottom."

    def devices = state.devices
	def errorMsgDev = null
	def newDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceNetworkId)
		if (!isChild) {
			newDevices["${it.value.deviceNetworkId}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
	}
	if (devices == [:]) {
		errorMsgDev = "Looking for devices.  If this message persists, we have been unable to find " +
        "TP-Link devices on your wifi.  Check: 1) Hubitat Environment logs, 2) node.js logfile for "
	} else if (newDevices == [:]) {
		errorMsgDev = "No new devices to add. Check: 1) Device installed to Kasa properly, " +
        "2) The Hubitat Devices Tab (in case already installed)."
	}
	return dynamicPage(name:"addDevicesPage",
		title:"Add Kasa Devices",
		nextPage:"",
		refreshInterval: 5,
        multiple: true,
		install: true) {
		section("") {
			if (state.currentError != null) {
				paragraph "ERROR:  ${state.currentError}! Correct before continuing."
            } else if (errorMsgDev != null) {
				paragraph "ERROR:  ${errorMsgDev}"
			} else {
				paragraph "No detected program errors!"
            }
		}
        
		section("Page Instructions", hideable: true, hidden: true) {
            paragraph page1Text
            paragraph page2Text
            paragraph page3Text
            paragraph page4Text
            paragraph page5Text
            paragraph page6Text
		}
        
 		section("Select Devices to Add (${newDevices.size() ?: 0} found)", hideable: true, hidden: false) {
			input ("selectedDevices", "enum", 
                   required: false, 
                   multiple: true, 
		  		   refreshInterval: 5,
                   submitOnChange: true,
                   title: null,
                   description: "Add Devices",
                   options: newDevices)
	        if (state.installed == true) {
				href "welcomePage", 
                    title: "Return to Main Page without adding devices", 
                    description: "Don't Add Devices"
	        }
		}
	}
}

def addDevices() {
	def tpLinkModel = [:]
	//	Plug-Switch Devices (no energy monitor capability)
	tpLinkModel << ["HS100" : "TP-Link/Kasa Plug-Switch"]				//	HS100
	tpLinkModel << ["HS103" : "TP-Link/Kasa Plug-Switch"]				//	HS103
	tpLinkModel << ["HS105" : "TP-Link/Kasa Plug-Switch"]				//	HS105
	tpLinkModel << ["HS200" : "TP-Link/Kasa Plug-Switch"]				//	HS200
	tpLinkModel << ["HS210" : "TP-Link/Kasa Plug-Switch"]				//	HS210
	tpLinkModel << ["KP100" : "TP-Link/Kasa Plug-Switch"]				//	KP100
	//	Miltiple Outlet Plug
	tpLinkModel << ["HS107" : "TP-Link/Kasa Multi-Plug"]				//	HS107
	tpLinkModel << ["HS300" : "TP-Link/Kasa Multi-Plug"]				//	HS300
	//	Dimming Switch Devices
	tpLinkModel << ["HS220" : "TP-Link/Kasa Dimming Switch"]			//	HS220
	//	Energy Monitor Plugs
	tpLinkModel << ["HS110" : "TP-Link/Kasa Plug-Switch"]				//	HS110
	tpLinkModel << ["HS115" : "TP-Link/Kasa Plug-Switch"]				//	HS110
	//	Soft White Bulbs
	tpLinkModel << ["KB100" : "TP-Link/Kasa Soft White Bulb"]			//	KB100
	tpLinkModel << ["LB100" : "TP-Link/Kasa Soft White Bulb"]			//	LB100
	tpLinkModel << ["LB110" : "TP-Link/Kasa Soft White Bulb"]			//	LB110
	tpLinkModel << ["KL110" : "TP-Link/Kasa Soft White Bulb"]			//	KL110
	tpLinkModel << ["LB200" : "TP-Link/Kasa Soft White Bulb"]			//	LB200
	//	Tunable White Bulbs
	tpLinkModel << ["LB120" : "TP-Link/Kasa Tunable White Bulb"]		//	LB120
	tpLinkModel << ["KL120" : "TP-Link/Kasa Tunable White Bulb"]		//	KL120
	//	Color Bulbs
	tpLinkModel << ["KB130" : "TP-Link/Kasa Color Bulb"]				//	KB130
	tpLinkModel << ["LB130" : "TP-Link/Kasa Color Bulb"]				//	LB130
	tpLinkModel << ["KL130" : "TP-Link/Kasa Color Bulb"]				//	KL130
	tpLinkModel << ["LB230" : "TP-Link/Kasa Color Bulb"]				//	LB230

	def hub = location.hubs[0]
	def hubId = hub.id
	selectedDevices.each { dni ->
		try {
			def isChild = getChildDevice(dni)
			if (!isChild) {
				def device = state.devices.find { it.value.deviceNetworkId == dni }
				def deviceModel = device.value.deviceModel
				addChildDevice(
                	"davegut", 
                	tpLinkModel["${deviceModel}"],
                    device.value.deviceNetworkId,hubId, [
                    	"label" : device.value.alias,
                    	"name" : deviceModel,
                    	"data" : [
                            "installType" : installType,
                        	"deviceId" : device.value.deviceId,
							"plugId": device.value.plugId,
                            "appServerUrl" : device.value.appServerUrl,
                            "deviceIP" : device.value.deviceIP,
                            "gatewayIP" : bridgeIp
                        ]
                    ]
                )
			log.info "Installed TP-Link $deviceModel with alias ${device.value.alias}"
			}
		} catch (e) {
			log.debug "Error Adding ${deviceModel}: ${e}"
		}
	}
    state.installed = true
}

def removeDevicesPage() {
    state.updateType = "removeDevices"
	def page1Text = ""
	def page2Text = ""
	def page3Text = ""
	def page4Text = ""
	def page5Text = ""
	page1Text += "Devices that have been installed will appear below,"
	page2Text += "1.  Tap below to see the list of istalled Kasa Devices."
	page3Text += "2.  Select the devices you want to connect to remove."
	page4Text += "3.  Press Save to remove the devices to your the Environment."
    page5Text += "4.  To cancel, select 'Don't Remove Devices' at the bottom."

    def devices = state.devices
	def errorMsgDev = null
	def oldDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceNetworkId)
		if (isChild) {
			oldDevices["${it.value.deviceNetworkId}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
	}
	if (devices == [:]) {
		errorMsgDev = "Devices database was cleared in-error.  Run Device Installer Page to correct " +
        "then try again.  You can also remove devices using the Environment app."
	} else if (oldDevices == [:]) {
		errorMsgDev = "There are no devices to remove from the Environment at this time.  This " +
        "implies no devices are installed."
	}
	return dynamicPage (name: "removeDevicesPage", 
                        title: "Device Uninstaller Page", 
                        install: true) {
		section("") {
			if (state.currentError != null) {
				paragraph "ERROR:  ${state.currentError}! Correct before continuing."
            } else if (errorMsgDev != null) {
				paragraph "ERROR:  ${errorMsgDev}"
			} else {
				paragraph "No detected program errors!"
            }
		}
        
		section("Page Instructions", hideable: true, hidden: true) {
            paragraph page1Text
            paragraph page2Text
            paragraph page3Text
            paragraph page4Text
            paragraph page5Text
		}
        
		section("Select Devices to Remove (${oldDevices.size() ?: 0} found)", hideable: true, hidden: false) {
			input ("selectedDevices", "enum", 
                   required: false, 
                   multiple: true, 
            	   submitOnChange: false, 
                   title: null,
                   description: "Remove Devices",
                   options: oldDevices)
				href "welcomePage", 
                    title: "Return to Main Page without removing devices", 
                    description: "Don't Remove Devices"
		}
	}
}

def removeDevices() {
	selectedDevices.each { dni ->
		try{
			def isChild = getChildDevice(dni)
			if (isChild) {
				def delete = isChild
				delete.each { deleteChildDevice(it.deviceNetworkId) }
			}
		} catch (e) {
			log.debug "Error deleting: ${e}"
		}
	}
}

//	===== Kasa Account Methods ===========
def kasaAuthenticationPage() {
	def page1Text = ""
	def page2Text = ""
	def page3Text = ""
	page1Text += "If possible, open the Envoroment and seletc Logs. Then, "
	page1Text += "enter your Username and Password for the Kasa Application."
	page2Text += "After entering all credentials, select 'Install Devices to Continue'.  "
	page2Text += "This will call the Add Devices page."
	page3Text += "You must select and add a device to install the application!"
	return dynamicPage (name: "kasaAuthenticationPage",
                        nextPage: "",
                        install: false,
                        uninstall: true) {
		section("") {
			if (state.currentError != null) {
				paragraph "ERROR:  ${state.currentError}! Correct before continuing."
			} else {
				paragraph "No detected program errors!"
            }
		}
        
		section("Page Instructions", hideable: true, hidden: true) {
            paragraph page1Text
            paragraph page2Text
            paragraph page3Text
		}
            
		section("Enter Kasa Account Credentials: ") {
			input ("userName", "email", 
                   title: "TP-Link Kasa Email Address", 
                   required: true, 
                   submitOnChange: false)
			input ("userPassword", "password", 
                   title: "Kasa Account Password", 
                   required: true, 
                   submitOnChange: true)
			if (state.currentError != null) {
				paragraph "Error! Exit program and try again after resolving problem. ${state.currentError}!"
			} 
            if (state.installed == false) {
 				href "addDevicesPage", 
                    title: "Install Devices to Continue!", 
                    description: "Go to Install Devices"
            } else if (state.installed == "true") {
                state.updateType = "KasaTokenUpdate"
                href "updateData", 
                    title: "Select to update Kasa Token.", 
                    description: "Update Kasa Token"
            }
		}
	}
}

def getToken() {
	def hub = location.hubs[0]
	def cmdBody = [
		method: "login",
		params: [
			appType: "Kasa_Android",
			cloudUserName: "${userName}",
			cloudPassword: "${userPassword}",
			terminalUUID: "${hub.id}"
		]
	]
	def getTokenParams = [
		uri: "https://wap.tplinkcloud.com",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(getTokenParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			state.TpLinkToken = resp.data.result.token
			log.info "TpLinkToken updated to ${state.TpLinkToken}"
			sendEvent(name: "TokenUpdate", value: "tokenUpdate Successful.")
			if (state.currentError != null) {
				state.currentError = null
			}
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getToken: ${state.currentError}"
			sendEvent(name: "TokenUpdate", value: state.currentError)
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getToken: ${state.currentError}"
			sendEvent(name: "TokenUpdate", value: state.currentError)
		}
	}
}

def kasaGetDevices() {
	def currentDevices = ""
	def cmdBody = [method: "getDeviceList"]
	def getDevicesParams = [
		uri: "https://wap.tplinkcloud.com?token=${state.TpLinkToken}",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(getDevicesParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			currentDevices = resp.data.result.deviceList
			if (state.currentError != null) {
				state.currentError = null
			}
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getDeviceData: ${state.currentError}"
            return
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getDeviceData: ${state.currentError}"
            return
		}
	}
	state.devices = [:]
	def devices = state.devices
	currentDevices.each {
		def deviceModel = it.deviceModel.substring(0,5)
		if (deviceModel == "HS107" || deviceModel == "HS300") {
			def totalPlugs = 2
			if (deviceModel == "HS300") {
				totalPlugs = 6
			}
			for (int i = 0; i < totalPlugs; i++) {
				def deviceNetworkId = "${it.deviceMac}_0${i}"
				plugId = "${it.deviceId}0${i}"
				def sysinfo = sendDeviceCmd(it.appServerUrl, it.deviceId, '{"system" :{"get_sysinfo" :{}}}')
				def children = sysinfo.system.get_sysinfo.children
				def alias
				children.each {
					if (it.id == plugId) {
						alias = it.alias
					}
				}
				def device = [:]
				device["deviceNetworkId"] = deviceNetworkId
				device["alias"] = alias
				device["deviceModel"] = deviceModel
				device["plugId"] = plugId
				device["deviceId"] = it.deviceId
				device["appServerUrl"] = it.appServerUrl
				devices << ["${deviceNetworkId}" : device]
				def isChild = getChildDevice(it.deviceMac)
				if (isChild) {
					isChild.setAppServerUrl(it.appServerUrl)
				}
				log.info "Device ${it.alias} added to devices array"
			}
		} else {
			def device = [:]
			device["deviceNetworkId"] = it.deviceMac
			device["alias"] = it.alias
			device["deviceModel"] = deviceModel
			device["deviceId"] = it.deviceId
			device["plugId"] = ""
			device["appServerUrl"] = it.appServerUrl
			devices << ["${it.deviceMac}" : device]
			def isChild = getChildDevice(it.deviceMac)
			if (isChild) {
				isChild.setAppServerUrl(it.appServerUrl)
			}
			log.info "Device ${it.alias} added to devices array"
		}
	}
}

//	===== Node Applet Methods ============
def hubEnterIpPage() {
	def page1Text = ""
	def page2Text = ""
	def page3Text = ""
    if (state.installed == false) {
		page1Text += "If possible, open the Envoroment and seletc Logs. Then, "
		page1Text += "enter the static IP address for your Node Applet gateway."
		page2Text += "After entering all the IP, select 'Install Devices to Continue'.  "
		page2Text += "This will call the Add Devices page."
		page3Text += "You must select and add a device to install the application!"
    } else {
		page1Text += "You can upate the Node Applet gateway IP using this page."
		page2Text += "1.  Enter the new IP address in the box below.  "
		page3Text += "2.  Select 'Update Node Applet Gateway IP' area.  This will "
		page3Text += "update the gateway IP in all installed Kasa devices and ."
		page3Text += "return to the Main Page."
    }

    return dynamicPage (name: "hubEnterIpPage", 
                        title: "Set Gateway IP",
                        nextPage: "",
                        install: false,
                        uninstall: true) {
		section("") {
			if (state.currentError != null) {
				paragraph "ERROR:  ${state.currentError}! Correct before continuing."
			} else {
				paragraph "No detected program errors!"
            }
		}
        
		section("Page Instructions", hideable: true, hidden: true) {
            paragraph page1Text
            paragraph page2Text
            paragraph page3Text
		}
            
		section("Enter Node Applet Gateway IP", hideable: true, hidden: false) {
			input ("bridgeIp",
                   "text",
                   title: null,
                   required: true,
                   multiple: false,
                   submitOnChange: true)
            if (state.installed == false) {
				href "addDevicesPage", 
                    title: "Install Devices to Continue!", 
                    description: "Go to Install Devices"
            } else if (state.installed == true) {
                state.updateType = "hubIpUpdate"
                href "updateData", 
                    title: "Select to update Gateway IP.", 
                    description: "Update Node Applet Gateway IP"
            }
		}
	}
}

def hubGetDevices() {
	runIn(10, createBridgeError)
	def headers = [:]
	headers.put("HOST", "${bridgeIp}:8082")	//	Same as on Hub.
	headers.put("command", "pollForDevices")
	sendHubCommand(new hubitat.device.HubAction([headers: headers], null, [callback: hubExtractDeviceData]))
}

def hubExtractDeviceData(response) {
	def currentDevices =  parseJson(response.headers["cmd-response"])
	if (currentDevices == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "currentError", value: "TCP Timeout in Hub")
        return
	}
    log.info "Node Applet Bridge Status: OK"
    unschedule(createBridgeError)
	state.currentError = null
	sendEvent(name: "currentError", value: null)
    if (currentDevices == []) {
    	return
    }
	state.devices = [:]
	def devices = state.devices
	currentDevices.each {
		def deviceModel = it.deviceModel.substring(0,5)
		if (deviceModel == "HS107" || deviceModel == "HS300") {
			def totalPlugs = 2
			if (deviceModel == "HS300") {
				totalPlugs = 6
			}
			for (int i = 0; i < totalPlugs; i++) {
				def deviceNetworkId = "${it.deviceMac}_0${i}"
				def alias = "temp_0${i}"		//	Temporary Device Alias
				plugId = "${it.deviceId}0${i}"
				def device = [:]
				device["deviceNetworkId"] = deviceNetworkId
				device["alias"] = alias
				device["deviceModel"] = deviceModel
				device["deviceId"] = it.deviceId
				device["plugId"] = plugId
		        device["deviceIP"] = it.deviceIP
				devices << ["${deviceNetworkId}" : device]
				def isChild = getChildDevice(it.deviceMac)
				if (isChild) {
		            isChild.setDeviceIP(it.deviceIP)
		            isChild.setGatewayIP(bridgeIp)
				}
				log.info "Device ${it.alias} added to devices array"
			}
		} else {
			def device = [:]
			device["deviceNetworkId"] = it.deviceMac
			device["alias"] = it.alias
			device["deviceModel"] = deviceModel
			device["deviceId"] = it.deviceId
			device["plugId"] = ""
		    device["deviceIP"] = it.deviceIP
			devices << ["${it.deviceMac}" : device]
			def isChild = getChildDevice(it.deviceMac)
				if (isChild) {
		            isChild.setDeviceIP(it.deviceIP)
		            isChild.setGatewayIP(bridgeIp)
				}
			log.info "Device ${it.alias} added to devices array"
		}
	}
}

def createBridgeError() {
    log.info "Node Applet Bridge Status: Not Accessible"
	state.currentError = "Node Applet not acessible"
	sendEvent(name: "currentError", value: "Node Applet Not Accessible")
}

//	===== Kasa Account UtilityMethods ====
def sendDeviceCmd(appServerUrl, deviceId, command) {
	def cmdResponse = ""
	def cmdBody = [
		method: "passthrough",
		params: [
			deviceId: deviceId,
			requestData: "${command}"
		]
	]
	def sendCmdParams = [
		uri: "${appServerUrl}/?token=${state.TpLinkToken}",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(sendCmdParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			def jsonSlurper = new groovy.json.JsonSlurper()
			cmdResponse = jsonSlurper.parseText(resp.data.result.responseData)
			if (state.errorCount != 0) {
				state.errorCount = 0
			}
			if (state.currentError != null) {
				state.currentError = null
				sendEvent(name: "currentError", value: null)
				log.debug "state.errorCount = ${state.errorCount}	//	state.currentError = ${state.currentError}"
			}
		//log.debug "state.errorCount = ${state.errorCount}		//	state.currentError = ${state.currentError}"
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			cmdResponse = "ERROR: ${resp.statusLine}"
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in sendDeviceCmd: ${state.currentError}"
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			cmdResponse = "ERROR: ${resp.data.msg}"
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in sendDeviceCmd: ${state.currentError}"
		}
	}
	return cmdResponse
}

def checkError() {
	if (state.currentError == null || state.currentError == "none") {
		log.info "${appLabel()} did not find any errors."
		if (state.currentError == "none") {
			state.currentError = null
		}
		return
	}
	def errMsg = state.currentError.msg
	log.info "Attempting to solve error: ${errMsg}"
	state.errorCount = state.errorCount +1
	if (errMsg == "Token expired" && state.errorCount < 6) {
		sendEvent (name: "ErrHandling", value: "Handle comms error attempt ${state.errorCount}")
		getDevices()
		if (state.currentError == null) {
			log.info "getDevices successful. apiServerUrl updated and token is good."
			return
		}
		log.error "${errMsg} error while attempting getDevices. Will attempt getToken"
		getToken()
		if (state.currentError == null) {
			log.info "getToken successful. Token has been updated."
			getDevices()
			return
		}
	} else {
		log.error "checkError: No auto-correctable errors or exceeded Token request count."
	}
	log.error "checkError residual: ${state.currentError}"
}

//	===== Generic Utility Methods ========
def installed() {
	initialize()
}

def updated() {
	initialize()
}

def initialize() {
	unsubscribe()
	unschedule()
	if (installType == "Kasa Account"){
		schedule("0 30 2 ? * WED", getToken)
		runEvery5Minutes(checkError)
    }
    if (installType == "Node Applet") { runEvery5Minutes(hubGetDevices) }
	if (selectedDevices) { updateData() }
}

def uninstalled() {
    	getAllChildDevices().each { 
        deleteChildDevice(it.deviceNetworkId)
    }
}

def removeChildDevice(alias, deviceNetworkId) {
	try {
		deleteChildDevice(it.deviceNetworkId)
		sendEvent(name: "DeviceDelete", value: "${alias} deleted")
	} catch (Exception e) {
		sendEvent(name: "DeviceDelete", value: "Failed to delete ${alias}")
	}
}
//end-of-file