# manifold-bootstrap-plugin
Bootstrap for core manifold

## Release instructions
Deploying snapshots OR releases requires environment variables `MANIFOLD_BINTRAY_USER` and `MANIFOLD_BINTRAY_API_KEY`.

**Deploy a snapshot:** 
1. `./mvnw deploy -s settings.xml`

**Perform a release:**
1. `./mvnw release:prepare -B -DreleaseVersion=<desired version>`
2. `./mvnw release:perform -B -s settings.xml`