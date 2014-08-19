# Backups as a Service
[![Build Status](https://api.travis-ci.org/yammer/backups.png?branch=master)](https://travis-ci.org/yammer/backups?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.yammer.backups/backups-service/badge.png)](https://maven-badges.herokuapp.com/maven-central/com.yammer.backups/backups-service)

Once upon a time in a world full of custom scripts rsync'ing, compressing and encrypting data dumps 
here and there we decided to unify all the backup tools in a single, easy to use service.

The backup service provides a REST API for securely storing data store dumps into different backend
storage such as Azure or the local filesystem.

Additionally, the backups service provides easy auditing, monitoring, retention and verification.

----
![Main](https://raw.githubusercontent.com/yammer/backups/master/backups-service/screenshots/main.png)
----
![History](https://raw.githubusercontent.com/yammer/backups/master/backups-service/screenshots/history.png)
----
![Details](https://raw.githubusercontent.com/yammer/backups/master/backups-service/screenshots/details.png)
----

For documentation, check out [the wiki](https://github.com/yammer/backups/wiki).

## Runtime Dependencies

* [Azure Table Storage](http://azure.microsoft.com/en-us/documentation/articles/storage-introduction/) for metadata storage.
* [Azure Blob Storage](http://azure.microsoft.com/en-us/documentation/articles/storage-introduction/) for offsite backup storage.
* An LDAP Server for user authentication.

## Getting Started

1. Copy and fill in the sample configuration file, found at `backups-service/conf/backups.yml.template`. Documentation on the Backups specific configuration options can be found in [the wiki](https://github.com/yammer/backups/wiki/Configuration), and the inherited Dropwizard configuration options at [dropwizard.io](http://dropwizard.io/manual/configuration.html).
2. Build the project: `mvn clean package`
3. Run the service: `java -jar backups-service/target/backups-service-<version>.jar server <configuration file path>``
4. The UI and API will be available over HTTPS on port 8443 (or as defined in the configuration file).

## Next Steps

* Set up client scripts to perform data export. Usually these will be configured to run using a crontab. An example can be found in the [`clients/example`](https://github.com/yammer/backups/tree/master/clients/example) directory.
* Set up alerting for backup failures. Alerting is handled using Dropwizard health checks, which are exposed on the configured admin connector (by default, via HTTP on port 8081).

## Contributors

* [Jamie Furness](https://github.com/reines)
* [Matias Surdi](https://github.com/msurdi)
* [Matt Knopp](https://github.com/mhat)
* [Maxim Kazantsev](https://github.com/mkazantsev)
* [Ryan Kennedy](https://github.com/ryankennedy)
* [Will Whitaker](https://github.com/willwhitaker)

## License

Released under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).

_Microsoft LCA request 9424_
