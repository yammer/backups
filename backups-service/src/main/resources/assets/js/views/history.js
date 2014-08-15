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
B.View.HistoryView = Backbone.View.extend({

  initialize: function(options) {
    this.verifications = options.verifications;
    this.listenTo(this.model, "b:indexed", this.render);
    this.listenTo(this.verifications, "v:indexed", function() {
      B.ViewUtil.updateVerificationStatuses(this.$(".i-backup-status"), this.verifications);
    });
  },

  render: function() {
    var that = this;

    _.forEach(this.model.getByService(), function(backups, service){
      var viewBackupRows = [];

      backups.sortBy(function(b) { return b.get('startedDate') }).reverse().forEach(function(backup) {
        var viewRowData = backup.attributes;

        viewRowData['niceStartedDate']   = B.ViewUtil.niceifyDate(viewRowData['startedDate']);
        viewRowData['niceCompletedDate'] = B.ViewUtil.niceifyDate(viewRowData['completedDate']);
        viewRowData['niceOriginalSize'] = B.ViewUtil.niceifySize(viewRowData['originalSize']);
        viewRowData['niceSize'] = B.ViewUtil.niceifySize(viewRowData['size']);
        viewRowData['niceDuration'] = B.ViewUtil.niceifyDuration(viewRowData['duration']);
        viewRowData['stateSymbol'] = B.ViewUtil.getGlyphForBackupState(viewRowData['state']);

        viewBackupRows.push(viewRowData);
      });

      var m = Mustache.render(that.panelTemplate(), { service: service, backups: viewBackupRows });
      that.$el.append(m);
    });
  },

  panelTemplate: function() {
    var t = "<div class='panel panel-default'>";
    t    += "<div class='panel-heading'>";
    t    += "<h3 class='panel-title'><a name='{{service}}'>{{service}}</a></h3>";
    t    += "</div>";
    t    += "<div class='panel-body'>";
    t    += "<table class='table table-condensed'>";
    t    += "<tr><th>&nbsp;</th><th>id</th><th>start</th><th>size</th><th>duration</th><th>source</th><th>node</th></tr>";
    t    += "{{#backups}}";

    t    += "<tr><td><span class='{{stateSymbol}} i-backup-status' title='Backup status: {{state}}' data-backup-id='{{id}}'></span></td>";
    t    += "<td class='field-id' title='{{id}}'><a href='/detail/{{service}}/{{id}}'>{{id}}</a></td><td>{{{niceStartedDate}}}</td>";
    t    += "<td title='Originally {{& niceOriginalSize}}'>{{& niceSize}}</td><td>{{niceDuration}}</td><td>{{sourceAddress}}</td><td>{{nodeName}}</td></tr>";

    t    += "{{/backups}}";
    t    += "</table>";
    t    += "</div>";
    t    += "</div>";
    return t
  }

});
