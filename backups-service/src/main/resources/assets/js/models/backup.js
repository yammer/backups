/*
 * #%L
 * Backups
 * %%
 * Copyright (C) 2013 - 2014 Microsoft Corporation
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
B.Model.Backup = Backbone.Model.extend({
  defaults: {
    "id"            : "N/A",
    "service"       : "N/A",
    "startedDate"   : "N/A",
    "completedDate" : "N/A",
    "state"         : "N/A",
    "size"          : "N/A",
    "duration"      : "N/A"
  },

  parse: function(resp, opts){

    // create a short id
    resp['shortId'] = resp['id'].substring(0, 8);

    // create durations, if the backup isn't done yet, fake a duration
    if (resp['completedDate']) {
      resp['duration']      = (resp['completedDate'] - resp['startedDate']) / 1000;
      resp['completedDate'] = new Date(resp['completedDate']);
    }
    else {
      resp['duration']      = (new Date().getTime() - resp['startedDate']) / 1000;
      resp['completedDate'] = "N/A";
    }
    resp['startedDate'] = new Date(resp['startedDate']);

    return resp;
  }
});

B.Collection.Backups = Backbone.Collection.extend({
  model: B.Model.Backup,
  url  : "/api/backup",
  initialize: function() {
    this._indexByService = {};
    this.listenTo(this, "sync", this.onSync);
  },

  onSync: function(collection, rsp, opts) {
    var that = this;

    // i had hoped that backbone/underscore would have bade indexBy (a) available to collections
    // and (b) if not, much easier to use ... but not so much so here we are
    collection.each(function(backup){
      var serviceName = backup.get("service");
      if (that._indexByService[serviceName] === undefined) {
        that._indexByService[serviceName] = new B.Collection.BackupsByService();
      }
      that._indexByService[serviceName].add(backup);
    });

    this.trigger("b:indexed");
  },

  getByService: function () {
    return this._indexByService;
  },

  // slot the backups in to buckets based on their age
  getByServiceAndDateBucket: function (now) {
      var buckets = {};

      // TODO: This isn't very backboney, maybe we should index them like this?
      _.forEach(this.getByService(), function(backups, service) {
          backups.forEach(function(backup) {
              var date = backup.get('startedDate');
              var days = Math.floor((now.getTime() - date.getTime()) / DAY_IN_MILLISECONDS);

              if (!(service in buckets)) {
                  buckets[service] = {};
              }

              if (!(days in buckets[service])) {
                  buckets[service][days] = [];
              }

              buckets[service][days].push(backup);
          });
      });

      return buckets;
  },

  getByServiceAndId: function (service, id) {
    var serviceBackups = this.getByService()[service],
        theBackup = null;
    serviceBackups.forEach(function(backup) {
      if (backup.get("id") === id) {
        theBackup = backup;
      }
    });
    return theBackup;
  }
});