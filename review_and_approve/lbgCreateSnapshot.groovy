 
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

// Check if we're being invoked from UrbanCode or via a command line test rig
if (udUser.equals("PasswordIsAuthToken")){
	udUser = props['udUser']
	udPass = props['udPass']
}

 
// --------------------------------------------------
// Main
// --------------------------------------------------
LBGUCRestClient restClient = new LBGUCRestClient(new URI(weburl), udUser, udPass)
def snapshotNames = restClient.getSnapshotNames(applicationName)
def newSnapshotName = getNewSnapshotName(snapshotNames, snapshotPrefix)
println "Setting new snapshot name to "+newSnapshotName
 
// --------------------------------------------------
// Classes and Functions
// --------------------------------------------------
 
public class LBGUCRestClient extends UDRestClient {
    	public LBGUCRestClient(URI url, String clientUser, String clientPassword) {
		super(url, clientUser, clientPassword)
    	}

	public getSnapshotNames(String applicationName){
		String url = "${url}/cli/application/snapshotsInApplication?application=${applicationName}"
		HttpGet method = new HttpGet(url)
		HttpResponse response = invokeMethod(method)
		String body = getBody(response)
		def slurper = new JsonSlurper()
		def json = slurper.parseText(body.toString())
		return(json.name)
	}
		
}

String getNewSnapshotName(snapshotNames, snapshotPrefix){
        def ninorVersions = []
        def currentMinorVrsion
        
        def namesArray = snapshotNames.findAll{ it.contains(snapshotPrefix)}
               
        namesArray.each{ name ->
                minorVersion = name.split('_')[4]
                minorVersions.push(minorVersion)
        }
 
        if(namesArray.size() < 1){
                println "No exitisting snapshots matching this pattern. Generating from 0"
                currentMinorVersion = "0"
        }else{
                currentMinorVersion =  minorVersions.max()
        }
        newMinorVersion = currentMinorVersion.toInteger()
        newMinorVersion++
        def newSnapshotName = snapshotPrefix+'_'+newMinorVersion
        return(newSnapshotName)
}

