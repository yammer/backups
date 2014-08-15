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
B.Model.Verification = Backbone.Model.extend({
    defaults: {
        "id"            : "N/A",
        "service"       : "N/A",
        "startedDate"   : "N/A",
        "completedDate" : "N/A",
        "state"         : "N/A",
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

B.Collection.Verifications = Backbone.Collection.extend({
  model: B.Model.Verification,

  url: "/api/verification",

  initialize: function() {
    this._indexByBackupId = {};
    this.listenTo(this, "sync", this.onSync);
  },

  onSync: function(collection, rsp, opts) {
    var that = this;

    // i had hoped that backbone/underscore would have bade indexBy (a) available to collections
    // and (b) if not, much easier to use ... but not so much so here we are
    collection.each(function(verification){
      var backupId = verification.get("backupId");
      if (that._indexByBackupId[backupId] === undefined) {
        that._indexByBackupId[backupId] = new B.Collection.VerificationsByBackup();
      }
      that._indexByBackupId[backupId].add(verification);
    });

    this.trigger("v:indexed");
  },

  getByBackupId: function(backupId) {
    var verifications = this._indexByBackupId[backupId];
    if (verifications === undefined) {
      return undefined;
    }

    return B.ViewUtil.getStateBasedVerification(verifications.models);
  }
});
