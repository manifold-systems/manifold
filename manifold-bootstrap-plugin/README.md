# manifold-bootstrap-plugin
Bootstrap for core manifold

## Release instructions
Deploying snapshots OR releases requires environment variable `MAN_PASS_PHRASE`.

**Deploy a snapshot:** 
1. `./mvnw deploy -s settings.xml`

**Perform a release:**
1. `./mvnw release:prepare -B -DreleaseVersion=<desired version>`
2. `./mvnw release:perform -B -s settings.xml`