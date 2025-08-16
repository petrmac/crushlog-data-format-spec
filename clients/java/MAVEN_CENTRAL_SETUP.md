# Maven Central Publishing Setup Guide

This guide provides detailed instructions for setting up Maven Central publishing for the CLDF Java project.

## Overview

Publishing to Maven Central requires:
1. Sonatype OSSRH account
2. Namespace ownership verification  
3. GPG signing setup
4. Gradle configuration
5. GitHub secrets configuration

## Step 1: Sonatype OSSRH Account

### Create Account

1. Go to https://issues.sonatype.org/secure/Signup!default.jspa
2. Fill in the registration form
3. Verify your email address

### Request Namespace

1. Create a new JIRA ticket:
   - Project: `Community Support - Open Source Project Repository Hosting (OSSRH)`
   - Issue Type: `New Project`
   - Summary: `Request for app.crushlog.cldf namespace`
   - Group Id: `app.crushlog.cldf`
   - Project URL: `https://github.com/your-org/crushlog-data-format-spec`
   - SCM URL: `https://github.com/your-org/crushlog-data-format-spec.git`

2. Example Description:
   ```
   We are requesting the app.crushlog.cldf namespace for the CrushLog Data Format project.
   This is an open-source data format specification for climbing and training data.
   
   We own the cldf.io domain and can verify ownership.
   ```

3. Domain Verification:
   - Add TXT record to cldf.io DNS: `OSSRH-XXXXX` (ticket number)
   - Or setup redirect from cldf.io to GitHub project

4. Wait for approval (typically 1-2 business days)

## Step 2: GPG Key Setup

### Generate GPG Key

```bash
# Generate a new GPG key
gpg --full-generate-key

# Select:
# - RSA and RSA (default)
# - 4096 bits
# - Key does not expire (or set expiration)
# - Real name: Your Name
# - Email: your-email@example.com
# - Comment: CLDF Release Signing Key
```

### Find Your Key ID

```bash
# List secret keys
gpg --list-secret-keys --keyid-format=long

# Output will look like:
# sec   rsa4096/ABCDEF1234567890 2024-01-01 [SC]
#       1234567890ABCDEF1234567890ABCDEF12345678
# uid   [ultimate] Your Name <your-email@example.com>

# Your key ID is: ABCDEF1234567890
```

### Publish Public Key

```bash
# Publish to Ubuntu keyserver
gpg --keyserver hkp://keyserver.ubuntu.com --send-keys ABCDEF1234567890

# Also publish to other keyservers
gpg --keyserver hkp://keys.openpgp.org --send-keys ABCDEF1234567890
gpg --keyserver hkp://pgp.mit.edu --send-keys ABCDEF1234567890
```

### Export Private Key

```bash
# Export private key for GitHub secrets
gpg --export-secret-keys --armor ABCDEF1234567890 > cldf-signing-key.asc

# View the key (to copy for GitHub secret)
cat cldf-signing-key.asc
```

## Step 3: Gradle Configuration

### Update gradle.properties

Create or update `~/.gradle/gradle.properties`:

```properties
# Sonatype OSSRH credentials
ossrhUsername=your-sonatype-username
ossrhPassword=your-sonatype-password

# GPG signing
signing.keyId=ABCDEF1234567890
signing.password=your-gpg-passphrase
signing.secretKeyRingFile=/Users/you/.gnupg/secring.gpg
```

### Configure build.gradle

The project already has the necessary configuration:

```gradle
publishing {
    publications {
        maven(MavenPublication) {
            from components.java
            
            pom {
                name = 'CLDF Java Client'
                description = 'Java client library for CrushLog Data Format (CLDF)'
                url = 'https://github.com/cldf/cldf-java'
                
                licenses {
                    license {
                        name = 'MIT License'
                        url = 'https://opensource.org/licenses/MIT'
                    }
                }
                
                developers {
                    developer {
                        name = 'CLDF Team'
                        email = 'info@crushlog.app'
                        organization = 'CLDF'
                        organizationUrl = 'https://cldf.io'
                    }
                }
                
                scm {
                    connection = 'scm:git:git://github.com/cldf/cldf-java.git'
                    developerConnection = 'scm:git:ssh://github.com:cldf/cldf-java.git'
                    url = 'https://github.com/cldf/cldf-java'
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}
```

## Step 4: GitHub Secrets Setup

### Add Repository Secrets

1. Go to repository Settings → Secrets and variables → Actions
2. Add the following secrets:

#### MAVEN_USERNAME
- Value: Your Sonatype OSSRH username

#### MAVEN_PASSWORD  
- Value: Your Sonatype OSSRH password

#### SIGNING_KEY
- Value: Complete contents of `cldf-signing-key.asc` file
- Include the headers:
  ```
  -----BEGIN PGP PRIVATE KEY BLOCK-----
  
  [key content]
  
  -----END PGP PRIVATE KEY BLOCK-----
  ```

#### SIGNING_PASSWORD
- Value: Your GPG key passphrase

## Step 5: Testing the Setup

### Local Testing

```bash
# Test signing
./gradlew :cldf-java:signMavenPublication

# Test publishing to local repository
./gradlew :cldf-java:publishToMavenLocal

# Verify artifacts
ls ~/.m2/repository/io/cldf/cldf-java/1.0.0/
```

### Staging Repository Test

```bash
# Publish to staging
./gradlew :cldf-java:publishToSonatype

# Check staging repository
# Login to: https://s01.oss.sonatype.org
# Go to: Staging Repositories
# Find your repository (app.crushlog.cldf-XXXX)
```

## Step 6: First Release

### Manual Process

1. **Update Version**:
   ```gradle
   version = '1.0.0'  // Remove -SNAPSHOT
   ```

2. **Publish to Staging**:
   ```bash
   ./gradlew :cldf-java:publishToSonatype
   ```

3. **Verify Staging Repository**:
   - Login to https://s01.oss.sonatype.org
   - Go to Staging Repositories
   - Find repository named `iocldf-XXXX`
   - Click "Content" tab to verify artifacts

4. **Release from Staging**:
   ```bash
   ./gradlew closeAndReleaseSonatypeStagingRepository
   ```

### Automated Process

Simply push a tag:
```bash
git tag java-v1.0.0
git push origin java-v1.0.0
```

GitHub Actions will handle the rest!

## Verification

### Check Maven Central

After 10-30 minutes:
- Direct: https://repo1.maven.org/maven2/io/cldf/cldf-java/
- Search: https://search.maven.org/search?q=g:app.crushlog.cldf

### Test Dependency

```gradle
dependencies {
    implementation 'app.crushlog.cldf:cldf-java:1.0.0'
}
```

```xml
<dependency>
    <groupId>app.crushlog.cldf</groupId>
    <artifactId>cldf-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Troubleshooting

### Common Issues

1. **Invalid POM**:
   - Ensure all required fields are present
   - Check for valid URLs
   - Verify license information

2. **Signature Verification Failed**:
   - Ensure public key is published to keyservers
   - Wait 10-15 minutes for key propagation
   - Try different keyserver

3. **Staging Repository Not Closing**:
   - Check validation errors in Nexus UI
   - Common: missing javadoc, sources, or signatures
   - Drop repository and retry

4. **403 Forbidden**:
   - Verify namespace ownership approved
   - Check credentials are correct
   - Ensure not trying to overwrite existing version

### Debug Commands

```bash
# Test credentials
curl -u "username:password" https://s01.oss.sonatype.org/service/local/staging/profile_repositories

# Check GPG
gpg --list-keys
gpg --list-secret-keys

# Verify signatures locally
gpg --verify cldf-java-1.0.0.jar.asc cldf-java-1.0.0.jar
```

## Security Best Practices

1. **GPG Key Security**:
   - Use strong passphrase
   - Backup private key securely
   - Consider using subkeys for signing
   - Set expiration date

2. **Credential Management**:
   - Never commit credentials
   - Use GitHub secrets for CI/CD
   - Rotate passwords regularly
   - Use 2FA on Sonatype account

3. **Release Verification**:
   - Always verify staging before release
   - Test artifacts before publishing
   - Monitor for unauthorized releases

## Resources

- [OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Gradle Publishing](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [GPG Best Practices](https://riseup.net/en/security/message-security/openpgp/best-practices)
- [Sonatype JIRA](https://issues.sonatype.org)