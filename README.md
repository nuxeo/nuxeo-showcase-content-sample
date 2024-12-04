# Nuxeo Showcase Content Sample

This addon import a set of showcase content containing office documents, images, videos, ...

## Modules

- nuxeo-showcase-content-importer: Showcase content Importer bundle

## Building all modules

    mvn clean install

## How to Create a New Data Zip File

- Import latest released Version
- Remove old or deprecated documents; without forgetting to clean the trash
- Import new documents
- To lighter the zip export file, cleanup all picture views, transcoded videos, etc. Execute this SQL query using your prefered SQL client:
```
delete from hierarchy where primarytype  = 'view' or primarytype = 'storyboarditem' or primarytype = 'transcodedVideoItem';
```
- Cleanup CoreSession cache
- Make a `Zip Tree Export` from `Workspaces`
- Upload it on Nexus, GAV: `org.nuxeo.ecm.platform:nuxeo-showcase-content-sample-data:NEXT_VERSION`
- Update Marketplace `nuxeo/marketplace-showcase-content-sample` project to use the correct data version.

## Release the project

Make sure the project builds and its tests pass.

Then create a temporary branch to perform the release:

```bash
git checkout -b tmp-release
```

Then update the project version to final, for instance `2025.0.2`:

```bash
mvn versions:set -DnewVersion=2025.0.2 -DgenerateBackupPoms=false
```

Then commit and tag the release:

```bash
git commit -a -m "Release 2025.0.2"
git tag -a -m "release-2025.0.2" release-2025.0.2
```

Then build the Marketplace Package:

```bash
mvn clean install -DskipTests
```

Then upload the marketplace to Connect and NOS Preprod:

```
curl --fail -su USERNAME -F package=@nuxeo-showcase-content-package/target/nuxeo-showcase-content-package-2025.0.2.zip 'https://connect.nuxeo.com/nuxeo/site/marketplace/upload?batch=true'
```
```
curl --fail -su USERNAME -F package=@nuxeo-showcase-content-package/target/nuxeo-showcase-content-package-2025.0.2.zip 'https://nos-preprod-connect.nuxeocloud.com/nuxeo/site/marketplace/upload?batch=true'
```

> [!IMPORTANT]
> You should replace the `USERNAME`, curl will prompt you for the password.

Then push the tag:

```bash
git push --tags
```

Then cleanup your branch and prepare the next development iteration:

```bash
git checkout 2025
git branch -D tmp-release
mvn versions:set -DnewVersion=2025.0.3-SNAPSHOT -DgenerateBackupPoms=false
git commit -a -m "Post release 2025.0.2"
git push
```

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
