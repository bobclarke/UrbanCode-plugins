// --------------------------------------------------
// lbgApproveSnapshot.groovy
// Author: Bob Clarke
// Date: 07/07/2016
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
snapshotName = props['snapshot']
weburl = System.getenv("AH_WEB_URL")
user = apTool.getAuthTokenUsername()
pass = apTool.getAuthToken()
pluginDev = System.getenv("PLUGIN_DEV")

// Check if we're being invoked from UrbanCode or via a command line test rig
if (pluginDev.equals("yes")){
        user = props['udUser']
        pass = props['udPass']
} 


// --------------------------------------------------
// Main
// --------------------------------------------------

LBGUCRestClient restClient = new LBGUCRestClient(new URI(weburl), user, pass)
def versions = restClient.getSnapshotVersions(applicationName, snapshotName)
def targetEnvironmentType = getEnvType(environmentName)
restClient.setVersionStatuses(targetEnvironmentType, versions)
restClient.setSnapshotStatus(targetEnvironmentType, applicationName, snapshotName)


// --------------------------------------------------
// Classes and Functions
// --------------------------------------------------
 
public class LBGUCRestClient extends UDRestClient {
    	public LBGUCRestClient(URI url, String clientUser, String clientPassword) {
		super(url, clientUser, clientPassword)
    	}

	public getSnapshotVersions(String applicationName, String snapshotName){
		String url = "${url}/cli/snapshot/getSnapshotVersions?application=${applicationName}&snapshot=${snapshotName}"
		HttpGet method = new HttpGet(url)
		HttpResponse response = invokeMethod(method)
		String body = getBody(response)
		def slurper = new JsonSlurper()
		def versions = slurper.parseText(body)
		return(versions.desiredVersions)
	}

	void setVersionStatuses(target_environment_type, version_array){
		version_array.each{ version ->
			def version_name = version.name[0]
			def component_name = version.component.name[0]
			def status = 'Approved for '+target_environment_type
			println "Adding status ${status} on ${version_name} for comp ${component_name}"
			String encodedSection = java.net.URLEncoder.encode(status, "UTF-8");
			String url = "${url}/cli/version/addStatus?component=${component_name}&version=${version_name}&status=${encodedSection}"
			HttpPut method = new HttpPut(url)
			HttpResponse response = invokeMethod(method)
			String body = getBody(response)
		}
	}

	void setSnapshotStatus(target_environment_type, application, snapshot){
		def status = 'Approved for '+target_environment_type
		println "Adding status ${status} to snapshot ${snapshot}"
		String encodedSection = java.net.URLEncoder.encode(status, "UTF-8");
		String url = "${url}/cli/snapshot/addStatusToSnapshot?application=${application}&snapshot=${snapshot}&statusName=${encodedSection}"
		HttpPut method = new HttpPut(url)
		HttpResponse response = invokeMethod(method)
		String body = getBody(response)
	}
}


void setSnapshotStatus(target_environment_type, snapshot, action){
        println "Performing action ${action} on ${snapshot} for ${target_environment_type}"

        def sout = new StringBuilder()
        def serr = new StringBuilder()

        def cmd = ['java', '-jar', udc, '--weburl', url, '--authtoken', auth_token, action, '--snapshot', snapshot, '--application', application, '--statusName', 'Approved for '+target_environment_type]

        proc = cmd.execute()
        proc.waitForProcessOutput(sout, serr)
        println sout
        println serr
}

String getEnvType(target_environment){
	def env_type
        def env_numbers = target_environment.find(/\d+/)
	if(env_numbers){
        	env_type = target_environment.replaceAll(env_numbers,'')
	}else{
		env_type = target_environment
	}
        return(env_type)
}

