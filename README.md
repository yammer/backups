# Backups service
[![Build Status](https://api.travis-ci.org/yammer/backups.png?branch=master)](https://travis-ci.org/yammer/backups?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.yammer.backups/backups-service/badge.png)](https://maven-badges.herokuapp.com/maven-central/com.yammer.backups/backups-service)

Once upon a time in a world full of custom scripts rsync'ing, compressing and encrypting data dumps 
here and there we decided to unify all the backup tools in a single, easy to use service.

The backup service provides a REST API for securely storing data store dumps into different backend
storage such as Azure or the local filesystem.

Additionally, the backups service provides easy auditing, monitoring, retention and verification.

For documentation, check out [the wiki](https://github.com/yammer/backups/wiki).
