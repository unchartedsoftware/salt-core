# Contributing to Salt

## Building

TODO

## Testing

TODO

## Deploying to Sonatype Central Repository

Staging and deployment to the Sonatype Central Repository is restricted to core contributors.

You will need the Salt signing key in your GPG keyring. Then, create a gradle.properties file in the root project directory (.gitignored for security reasons), which contains the following contents:

```ini
signing.keyId=[Salt signing key ID]
signing.password=[Salt signing key password]
signing.secretKeyRingFile=[/path/to/your/.gnupg/secring.gpg]
```

Finall, building a Nexus-compatible bundle JAR for [manual staging/deployment](http://central.sonatype.org/pages/manual-staging-bundle-creation-and-deployment.html) can be achieved as follows:

```bash
$ ./gradlew nexus
```
