// --------------------------------------------------
// lbgCreateSnapshot.groovy
// Author: Bob Clarke
// Date: 05/07/2016
// --------------------------------------------------

import com.urbancode.air.AirPluginTool
import com.urbancode.ud.client.*
import org.apache.http.HttpResponse
import org.apache.http.client.methods.*
import groovy.json.JsonSlurper

def apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties()
applicationName = props['application']
environmentName = props['environment']
br_type = props['release_type']
year = props['release_year']
month = props['release_month']
snapshotPrefix = "SNAPSHOT_${br_type}_${year}_${month}"
snapshotDescription = "Test"
weburl = System.getenv("AH_WEB_URL")
udUser = apTool.getAuthTokenUsername()
udPass = apTool.getAuthToken()
pluginDev = System.getenv("PLUGIN_DEV")

// Check if we're being invoked from UrbanCode or via a command line test rig
if (pluginDev.equals("yes")){
	udUser = props['udUser']
	udPass = props['udPass']
}

 
// --------------------------------------------------
// Main
// --------------------------------------------------
LBGUCRestClient restClient = new LBGUCRestClient(new URI(weburl), udUser, udPass)
def applicationId = restClient.getApplicationId(applicationName)
def snapshotNames = restClient.getSnapshotNames(applicationId)
def newSnapshotName = getNewSnapshotName(snapshotNames, snapshotPrefix)
String newSnapshotId = restClient.createIbSnapshot(applicationName, environmentName, newSnapshotName)
restClient.lockSnapshotVersions(applicationName, newSnapshotName)
restClient.softLockSnaphotConfig(newSnapshotId)
 
// --------------------------------------------------
// Classes and Functions
// --------------------------------------------------
 
public class LBGUCRestClient extends UDRestClient {
    	public LBGUCRestClient(URI url, String clientUser, String clientPassword) {
		super(url, clientUser, clientPassword)
    	}

	public  getApplicationId(String applicationName){
		String endpoint = "${url}/cli/application/info?application=${applicationName}"
		println "\nGetting ID of application: ${applicationName}"
		println "REST call is: ${endpoint}"
		HttpGet method = new HttpGet(endpoint)
		HttpResponse response = invokeMethod(method)
		String body = getBody(response)
		def slurper = new JsonSlurper()
		def json = slurper.parseText(body.toString())
		println "ID is ${json.id}"
		return(json.id)
	}

	public getSnapshotNames(String applicationId){
		//String endpoint = "${url}/cli/application/snapshotsInApplication?application=${applicationName}"
		println "Getting existing Snapshot names"
		String endpoint = "${url}/rest/deploy/application/${applicationId}/snapshots/false"
		println "REST call is: ${endpoint}"
		HttpGet method = new HttpGet(endpoint)
		HttpResponse response = invokeMethod(method)
		String body = getBody(response)
		def slurper = new JsonSlurper()
		def json = slurper.parseText(body.toString())
		return(json.name)
	}
		
	public createIbSnapshot(String applicationName, environmentName, snapshotName){
		String endpoint = "${url}/cli/snapshot/createSnapshotOfEnvironment?application=${applicationName}&environment=${environmentName}&name=${snapshotName}"
		println "Creating Snapshot"
		println "REST call is: ${endpoint}"
		HttpPut method = new HttpPut(endpoint)
		HttpResponse response = invokeMethod(method)
		String body = getBody(response)
		def slurper = new JsonSlurper()
		def json = slurper.parseText(body.toString())
		def snapshotId = json.id
		return(snapshotId)
	}
		
	public lockSnapshotVersions(String applicationName, String snapshotName){
		println "\nLocking snapshot versions"
		String endpoint = "${url}/cli/snapshot/lockSnapshotVersions?application=${applicationName}&snapshot=${snapshotName}"
		println "REST call is: ${endpoint}"
		HttpPut method = new HttpPut(endpoint)
		HttpResponse response = invokeMethod(method)
	}

	public softLockSnaphotConfig(String snapshotId){
		println "\nApplying soft lock to snapshot configuration. ID is ${snapshotId}"
		String endpoint = "${url}/rest/deploy/snapshot/${snapshotId}/configuration/lockAllToCurrent"
		println "REST call is: ${endpoint}"
		HttpPut method = new HttpPut(endpoint)
		HttpResponse response = invokeMethod(method)
	}
		
}

String getNewSnapshotName(snapshotNames, snapshotPrefix){
	println "\nGenerating new snapshot name"
        def minorVersions = []
        def currentMinorVersion
        
        def namesArray = snapshotNames.findAll{ it.contains(snapshotPrefix)}
               
        namesArray.each{ name ->
                minorVersion = name.split('_')[4].toInteger()
                minorVersions.push(minorVersion)
        }
 
        if(namesArray.size() < 1){
                println "No exitisting snapshots matching this pattern. Generating from 0"
                currentMinorVersion = "0"
        }else{
                currentMinorVersion =  minorVersions.max()
        }
	println "Current minor version is ${currentMinorVersion}"
	
        newMinorVersion = currentMinorVersion.toInteger()
        newMinorVersion++
        def newSnapshotName = snapshotPrefix+'_'+newMinorVersion
	println "New snapshot name is ${newSnapshotName}"
        return(newSnapshotName)
}

