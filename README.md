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
