/*
* Licensed Materials - Property of IBM Corp.
* IBM UrbanCode Deploy
* (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
*
* U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
* GSA ADP Schedule Contract with IBM Corp.
*/
package com.urbancode.air.plugin.udeploy.applications

import com.urbancode.air.AirPluginTool
import com.urbancode.ud.client.ApplicationClient

import java.rmi.RemoteException;
import java.util.UUID

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject

public class ApplicationHelper {
    def apTool
    def props = []
    def udUser
    def udPass
    def weburl
    ApplicationClient client

    public ApplicationHelper(def apToolIn) {
        apTool = apToolIn
        props = apTool.getStepProperties()
        udUser = apTool.getAuthTokenUsername()
        udPass = apTool.getAuthToken()
        weburl = System.getenv("AH_WEB_URL")
        client = new ApplicationClient(new URI(weburl), udUser, udPass)

        com.urbancode.air.XTrustProvider.install()
    }

    public def createApplication() {
        def appName = props['application']
        def description = props['description']
        def notificationScheme = props['notificationScheme']
        def enforceCompleteSnapshots = Boolean.valueOf(props['enforceCompleteSnapshots'])

        if (!appName) {
            throw new IllegalArgumentException("no application was specified")
        }
        if (!description) {
            description = ""
        }
        if (!notificationScheme) {
            notificationScheme = ""
        }

        UUID appUUID = client.createApplication(appName, description, notificationScheme, enforceCompleteSnapshots)
        println "created application with name : " + appName
        apTool.setOutputProperty("application.id", appUUID.toString())
        apTool.setOutputProperties()
    }

    public def deleteApplication() {
        def appName = props['application']

        if (!appName) {
            throw new IllegalArgumentException("no application was specified")
        }

        UUID appUUID = client.deleteApplication(appName)
        println "Deleted application: " + appName
        apTool.setOutputProperty("application.id", appUUID.toString())
        apTool.setOutputProperties()
    }

    public def createApplicationProcess() {
        def jsonIn = props['json']

        if (!jsonIn) {
            throw new IllegalArgumentException("no JSON input was supplied")
        }

        String uuid = client.createApplicationProcess(jsonIn).toString()
        println "created application process with ID: " + uuid
        apTool.setOutputProperty("application.process.id", uuid)
        apTool.setOutputProperties()
    }

    public def setApplicationProperty() {
        def appName = props['application']
        def propName = props['name']
        def propValue = props['value']
        def isSecure = Boolean.valueOf(props['isSecure'])

        if (!propName) {
            throw new IllegalArgumentException("no property name was specified")
        }
        if (!propValue) {
            propValue = ""
        }
        if (!appName) {
            throw new IllegalArgumentException("no application was specified")
        }
        client.setApplicationProperty(appName, propName, propValue, isSecure)
    }

    public def addComponentToApplication() {
        def appName = props['application']
        def compName = props['component']

        if (!appName) {
            throw new IllegalArgumentException("no application was specified")
        }
        if (!compName) {
            throw new IllegalArgumentException("no component was specified")
        }

        def componentsInApp = client.addComponentToApplication(appName, compName);
    }

    public def addTagToApplication() {
        def appName = props['application']
        def tagName = props['tag']

        if (!appName) {
            throw new IllegalArgumentException("no application was specified")
        }
        if (!tagName) {
            throw new IllegalArgumentException("no tag was specified")
        }

        client.addTagToApplication(appName, tagName)
        println("Added tag: ${tagName} to application: ${appName}")
    }

    public def removeComponentFromApplication() {
        def appName = props['application']
        def compNames = props['component']

        if (!appName) {
            throw new IllegalArgumentException("no application was specified")
        }
        if (!compNames) {
            throw new IllegalArgumentException("no component was specified")
        }

        def components = compNames.split('\n')
        client.removeComponentFromApplication(components as String[], appName);
        components.each { comp -> println "Removed ${comp} from ${appName}" }
    }

    public def removeTagFromApplication() {
        def appName = props['application']
        def tagName = props['tag']

        if (!appName) {
            throw new IllegalArgumentException("no application was specified")
        }
        if (!tagName) {
            throw new IllegalArgumentException("no tag was specified")
        }

        client.removeTagFromApplication(appName, tagName)
        println("Removed tag: ${tagName} from application: ${appName}")
    }

    public def runApplicationProcess() {
        def appName = props['application']
        def processName = props['process']
        def envName = props['environment']
        def snapshot = props['snapshot']
        def description = props['description']
        def onlyChanged = Boolean.valueOf(props['onlyChanged'])
        def waitForProcess = Boolean.valueOf(props['waitForProcess'])
        String unparsedComponentVersions = props['componentVersions']
        Map<String, String> componentVersions = new HashMap<String, String>()

        if (!appName) {
            throw new IllegalArgumentException("no application was specified")
        }
        if (!processName) {
            throw new IllegalArgumentException("no process was specified")
        }
        if (!envName) {
            throw new IllegalArgumentException("no environment was specified")
        }
        if (!description) {
            description = ""
        }
        if (!snapshot) {
            snapshot = ""
        }
        if (!unparsedComponentVersions) {
            unparsedComponentVersions = ""
        }

        String[] cvLines = unparsedComponentVersions.split("\n")
        for (String cvLine : cvLines) {
            if(cvLine) {
                int delim = cvLine.indexOf(':')
                if (delim <= 0) {
                    throw new IllegalArgumentException("Component/version pairs must be of the form {Component}:{Version #}")
                }
                String component = cvLine.substring(0, delim).trim()

                def versionList = componentVersions.get(component)
                if (versionList == null) {
                    versionList = new ArrayList<String>()
                    componentVersions.put(component, versionList)
                }

                String version = cvLine.substring(delim+1).trim()
                versionList.add(version)
            }
        }

        def outputStatus

        def processId = client.requestApplicationProcess(appName, processName, description,
            envName, snapshot, onlyChanged, componentVersions)
        println "Running process with request ID " + processId.toString()
        if (waitForProcess) {
            def processStatus = ""
            while (!(processStatus.equalsIgnoreCase("succeeded") || processStatus.equalsIgnoreCase("faulted"))) {
                Thread.sleep(3000);
                processStatus = client.getApplicationProcessStatus(processId.toString())

                if (processStatus.equalsIgnoreCase("FAILED TO START")) {
                    throw new RuntimeException("The process failed to start, please see the process history for more details.")
                }
            }

            if (processStatus.equalsIgnoreCase("succeeded")) {
                outputStatus = "Success"
            }
            else if (processStatus.equalsIgnoreCase("faulted")) {
                println "The application process failed. See the app process log for details."
                outputStatus = "Failure"
            }
        }
        else {
            outputStatus = "Did Not Wait"
        }

        apTool.setOutputProperty("detailsLink", "#applicationProcessRequest/"+processId);
        apTool.setOutputProperty("process status", outputStatus)
        apTool.setOutputProperties()
    }

    public def applicationExists() {
        def appName = props['application']

        if (!appName) {
            throw new IllegalArgumentException("no application was specified")
        }

        try {
            JSONObject appJSON = client.getApplication(appName);
            println "Application with name ${appName} was found."
            apTool.setOutputProperty("exists", "true");
        }
        catch(IOException e) {
            if(e.getMessage().contains("404")) {
                println "Request was successful but no application with name ${appName} was found."
                apTool.setOutputProperty("exists", "false");
            }
            else {
                println "An error occurred during your request."
                throw new IOException(e);
            }
        }
        apTool.setOutputProperties();
    }

    public def addApplicationToTeam() {
        def appName = props['application']
        def teamName = props['team']
        def typeName = props['type']

        if (!appName) {
            throw new IllegalArgumentException("no application was specified")
        }
        if (!teamName) {
            throw new IllegalArgumentException("no team was specified")
        }

        client.addApplicationToTeam(appName, teamName, typeName)
        println "Application was added to team for the given type classification."
    }

    public def createSnapshot() {
        def snapshotName = props['name']
        def snapshotDescription = props['description']
        def applicationName = props['application']
        def rawVersions = props['versions']
        def versionMap = new HashMap<String, List<String, String>>();

        rawVersions.eachLine { line ->
            if (!StringUtils.isEmpty(line.trim())) {
                def equalsIndex = line.indexOf("=");
                if (equalsIndex > 0 && equalsIndex < line.length()) {
                    def namePart = line.substring(0, equalsIndex).trim();
                    def versionPart = line.substring(equalsIndex+1).trim();

                    if (StringUtils.isEmpty(namePart) || StringUtils.isEmpty(versionPart)) {
                        throw new IllegalArgumentException("Invalid version line: "+line)
                    }

                    def componentVersionList = versionMap.get(namePart);
                    if (componentVersionList == null) {
                        componentVersionList = new ArrayList<String>();
                        versionMap.put(namePart, componentVersionList);
                    }

                    if (!componentVersionList.contains(versionPart)) {
                        componentVersionList.add(versionPart);
                        println "Using version "+versionPart+" of component "+namePart
                    }
                }
                else {
                    throw new IllegalArgumentException("Invalid version line: "+line)
                }
            }
        }

        def snapshotId = client.createSnapshot(snapshotName, snapshotDescription, applicationName,
                versionMap);


        apTool.setOutputProperty("snapshotId", snapshotId.toString())
        apTool.setOutputProperties()
        println "\nSnapshot "+snapshotName+" created."
    }

    public def createSnapshotOfEnvironment() {
        def snapshotName = props['name']
        def snapshotDescription = props['description']
        def applicationName = props['application']
        def environmentName = props['environment']

        def snapshotId = client.createSnapshotOfEnvironment(environmentName, applicationName,
                snapshotName, snapshotDescription);

        apTool.setOutputProperty("snapshotId", snapshotId.toString())
        apTool.setOutputProperties()
        println "\nSnapshot "+snapshotName+" created."
    }

    public def getComponentsInApplication() {
        def applicationName = props['application']

        def componentsJson = client.getApplicationComponents(applicationName);

        String componentNames = "";
        String componentIds = "";
        String componentCount = 0;
        for (int i = 0; i < componentsJson.length(); i++) {
            componentCount++;

            if (componentIds.length() > 0) {
                componentIds += ",";
            }
            if (componentNames.length() > 0) {
                componentNames += ",";
            }

            def componentJson = componentsJson.getJSONObject(i);
            def componentId = componentJson.getString("id");
            def componentName = componentJson.getString("name");

            println "Found component \""+componentName+"\" with ID "+componentId

            componentIds += componentId;
            componentNames += componentName;
        }

        if (componentCount == 0) {
            println "No components found."
        }

        apTool.setOutputProperty("componentIds", componentIds)
        apTool.setOutputProperty("componentNames", componentNames)
        apTool.setOutputProperty("componentCount", componentCount.toString())
        apTool.setOutputProperties()
    }

    public def getApplicationInfo() {
        def applicationName = props['application']
        if (!applicationName) {
            throw new IllegalArgumentException("no application was specified")
        }

        def applicationJson = client.getApplication(applicationName)
        def applicationInfoMap = client.getJSONAsProperties(applicationJson)
        for (String key : applicationInfoMap.keySet()) {
            apTool.setOutputProperty(key, applicationInfoMap.get(key))
        }
        apTool.setOutputProperties()

        println applicationJson
    }

    public def applicationProcessExists() {
        def appName = props['application']
        def appProcName = props['appProcess']

        if (!appName) {
            throw new IllegalArgumentException("no application was specified")
        }
        if (!appProcName) {
            throw new IllegalArgumentException("no application process was specified")
        }

        try {
            JSONObject appJSON = client.getApplicationProcess(appName, appProcName);
            println "Process with name ${appProcName} for application ${appName} was found."
            apTool.setOutputProperty("exists", "true");
        }
        catch(IOException e) {
            if(e.getMessage().contains("404")) {
                println "Request was successful but process with name ${appProcName} for application ${appName} was not found."
                apTool.setOutputProperty("exists", "false");
            }
            else {
                println "An error occurred during your request."
                throw new IOException(e);
            }
        }
        apTool.setOutputProperties();
    }

    public def getEnvironmentsInApplication() {
        def applicationName = props['application']
        def fetchActive = props['fetchActive']
        def fetchInactive = props['fetchInactive']

        def envJson = client.getApplicationEnvironments(applicationName, fetchActive, fetchInactive);

        String envNames = "";
        String envIds = "";
        String envCount = 0;
        for (int i = 0; i < envJson.length(); i++) {
            envCount++;

            if (envIds.length() > 0) {
                envIds += ",";
            }
            if (envNames.length() > 0) {
                envNames += ",";
            }

            def envJsonEntry = envJson.getJSONObject(i);
            def envId = envJsonEntry.getString("id");
            def envName = envJsonEntry.getString("name");

            println "Found environment \"" + envName + "\" with ID "+ envId

            envIds += envId;
            envNames += envName;
        }

        if (envCount == 0) {
            println "No environments found."
        }

        apTool.setOutputProperty("environmentIds", envIds)
        apTool.setOutputProperty("environmentNames", envNames)
        apTool.setOutputProperty("environmentCount", envCount.toString())
        apTool.setOutputProperties()
    }

}
