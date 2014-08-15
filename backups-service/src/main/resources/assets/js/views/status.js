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
B.View.StatusGrid = Backbone.View.extend({

  initialize: function(options) {
    this.verifications = options.verifications;
    this.services = options.services;
    this.listenTo(this.model, "b:indexed", this.render);
    this.listenTo(this.services, "b:servicesLoaded", this.render);
    this.listenTo(this.verifications, "v:indexed", function() {
      B.ViewUtil.updateVerificationStatuses(this.$(".i-backup-status"), this.verifications);
    });
  },

  _renderHead: function() {
    var now = new Date();
    var ages = _.range(0, 14).reverse(); // TODO: Pull this from the server?

    var viewDates = _.map(ages, function(age) {
      var endDate = new Date(now.getTime() - (DAY_IN_MILLISECONDS * age));
      var startDate = new Date(endDate.getTime() - DAY_IN_MILLISECONDS);

      return {
          age: B.ViewUtil.getLabelForAge(age),
          endDate: B.ViewUtil.formatDate(endDate),
          startDate: B.ViewUtil.formatDate(startDate)
      };
    });

    var m = Mustache.render(this.headTemplate(), { dates: viewDates });
    this.$el.append(m);
  },

  _renderRows: function() {
    var that = this;
    var ages = _.range(0, 14).reverse(); // TODO: Pull this from the server?

    _.forEach(that.model.getByServiceAndDateBucket(new Date()), function(backups, service) {
      var viewBackups = _.map(ages, function(age) {
          var dayBackups = (age in backups) ? backups[age] : [],
              bestBackup = B.ViewUtil.getStateBasedBackup(dayBackups),
              bestState = bestBackup ? bestBackup.get("state") : undefined;

          return {
              class: B.ViewUtil.getGlyphForBackupState(bestState),
              status: bestState ? bestState : "Unknown",
              date: bestBackup ? B.ViewUtil.formatDate(bestBackup.get("startedDate")) : "Unknown",
              service: bestBackup ? bestBackup.get("service") : undefined,
              id: bestBackup ? bestBackup.get("id") : undefined,
              'exists?': bestBackup ? true : false
          };
      });

      // highlight the right-most row
      viewBackups[viewBackups.length - 1]["active"] = "active";

      var s = that.services.get(service);
      var isDisabled = s && s.get('disableHealthcheck') === true;
      var serviceLabel = isDisabled ? service + ' (disabled)' : service;
      var serviceStatus = isDisabled ? 'warning' : '';

      var m = Mustache.render(that.rowTemplate(), {
          service: serviceLabel,
          serviceStatus: serviceStatus,
          backups: viewBackups
      });
      that.$el.append(m);
    });
  },

  render: function() {
    this.$el.empty();
    this._renderHead();
    this._renderRows();
  },

  headTemplate: function() {
    var m = "";
      m += "<tr><th>Service / Age (days)</th>";
      m += "{{#dates}}";
      m += "<th title='Backups from {{startDate}} to {{endDate}}'>{{age}}</th>";
      m += "{{/dates}}";
      m += "</tr>";
      return m;
  },

  rowTemplate: function() {
    var m = "";
    m += "<tr class='{{serviceStatus}}'><td>{{service}}</td>";
    m += "{{#backups}}";
    m += "<td class='{{active}}'>";
    m += "{{#exists?}}";
    m += "<a href='/detail/{{service}}/{{id}}'>";
    m += "{{/exists?}}";
    m += "<span class='{{class}} i-backup-status' title='Backup: {{status}} (started: {{date}})' data-backup-id='{{id}}'></span>";
    m += "{{#exists?}}";
    m += "</a>";
    m += "{{/exists?}}";
    m += "</td>";
    m += "{{/backups}}";
    m += "</tr>";
    return m
  }
});