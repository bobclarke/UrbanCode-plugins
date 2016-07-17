export AH_WEB_URL=https://localhost:8443
export AGENT_HOME=/Users/clarkeb/urbancode/agent
groovy -cp classes:lib/uDeployRestClient.jar:lib/jettison-1.1.jar lbgCreateSnapshot.groovy in.props out.props
