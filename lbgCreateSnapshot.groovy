// ----------------------------------------------------------------------------
// Name:             Create LBG snapshot 
// Desc:             Generates a correctly formed and numbered snapshot name
// Author:           Bob Clarke (LBG)
// Date:             05/07/2016
// ----------------------------------------------------------------------------
 
// --------------------------------------------------
// Set up
// --------------------------------------------------
import com.urbancode.air.AirPluginTool
import com.urbancode.ud.client.ApplicationClient
import groovy.json.JsonSlurper

def apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties()

snapshotDescription = "Test"
applicationName = props['application']
environmentName = props['environment']
br_type = props['release_type']
year = props['release_year']
month = props['release_month']
def snapshot_prefix = "SNAPSHOT_${br_type}_${year}_${month}"

agent_home = System.getenv("AGENT_HOME")
udc = agent_home+'/opt/udclient/udclient.jar'
 
udUser = apTool.getAuthTokenUsername()
udPass = apTool.getAuthToken()
if (udUser.equals("PasswordIsAuthToken")){
	println 'it works'
	udUser = props['udUser']
	udPass = props['udPass']
}
weburl = System.getenv("AH_WEB_URL")
ApplicationClient client = new ApplicationClient(new URI(weburl), udUser, udPass)
 
// --------------------------------------------------
// Main
// --------------------------------------------------
def snapshot_names_array = getSnapshotNames()
//def new_snapshot_name = get_new_snapshot_name(snapshot_names_array, snapshot_prefix)
//println "Setting new snapshot name to "+new_snapshot_name
 
// --------------------------------------------------
// Subs
// --------------------------------------------------
 
ArrayList getSnapshotNames(){
        def sout = new StringBuilder()
        def serr = new StringBuilder()
        def cmd = ['java', '-jar', udc, '--weburl', weburl, '--username', udUser, '--password', udPass, 'getSnapshotsInApplication', '--application', applicationName]
        proc = cmd.execute()
        proc.waitForProcessOutput(sout, serr)
	println sout
	println serr
	
        def slurper = new JsonSlurper()
        def snapshot_names = slurper.parseText(sout.toString()).name
        return(snapshot_names)
}
 
String get_new_snapshot_name(snapshot_names_array, snapshot_prefix){
        def minor_versions = []
        def current_minor_version
        
        def names_array = snapshot_names_array.findAll{ it.contains(snapshot_prefix)}
               
        names_array.each{ name ->
                minor_version = name.split('_')[4]
                minor_versions.push(minor_version)
        }
 
        if(names_array.size() < 1){
                println "No exitisting snapshots matching this pattern. Generating from 0"
                current_minor_version = "0"
        }else{
                current_minor_version =  minor_versions.max()
        }
        new_minor_version = current_minor_version.toInteger()
        new_minor_version++
        def new_snapshot_name = snapshot_prefix+'_'+new_minor_version
        return(new_snapshot_name)
}
