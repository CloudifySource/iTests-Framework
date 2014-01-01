# performing braking changes in iTests-Framework

- follow these steps:
  - increment maven module version (in iTests-Framework pom.xml)
  - commit the changes to the repository
  - deploy to s3 by running `mvn clean install s3client:deploy`
  - update all projects with the iTests-Framework dependecy (sg-test, Cloudify-iTests) to increment their pom.xml and commit these changes

## notes

- don't rely on s3 for the latest package if you don't increment the version number as it's caching packages with identical names.

## troubleshooting

- check the user and key are updated, under the s3 profile in .m2/settings.xml.


