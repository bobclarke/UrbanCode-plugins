import com.urbancode.air.AirPluginTool
import com.urbancode.ud.client.ApplicationClient

def apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties()

def snapshotDescription = "Test" 
def snapshotName = props['snapshot_name']
def applicationName = props['target_application']
def environmentName = props['source_environment']

def udUser = apTool.getAuthTokenUsername()
def udPass = apTool.getAuthToken()
def weburl = System.getenv("AH_WEB_URL")
ApplicationClient client = new ApplicationClient(new URI(weburl), udUser, udPass)

def snapshotId = client.createSnapshotOfEnvironment(environmentName, applicationName, snapshotName, snapshotDescription);
apTool.setOutputProperty("snapshotId", snapshotId.toString())
apTool.setOutputProperties()
println "\nSnapshot "+snapshotName+" created."

