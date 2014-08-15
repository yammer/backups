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
B.View.DetailView = Backbone.View.extend({

  template: '\
    <div class="page-header">\
      <h1>\
        <span class="{{status}} i-backup-status" title="Backup status: {{state}}" data-backup-id="{{id}}"></span>\
        {{service}} <small>{{id}}</small>\
      </h1>\
    </div>\
    <p title="Locations">\
      {{#locations}}\
        <span class="label label-success">{{.}}</span>\
      {{/locations}}\
    </p>\
    <div class="row b-details">\
      <div class="col-lg-3 col-md-3 col-sm-6 col-xs-6 col-xxs-12 b-detail-param-box">\
        <span class="i-detail-param-title">Original size</span>\
        <span class="i-detail-param-value" title="{{originalSize}}">{{& htmlOriginalSize}}</span>\
      </div>\
      <div class="col-lg-3 col-md-3 col-sm-6 col-xs-6 col-xxs-12 b-detail-param-box">\
        <span class="i-detail-param-title">Size</span>\
        <span class="i-detail-param-value" title="{{size}}">{{& htmlSize}}</span>\
      </div>\
      <div class="col-lg-3 col-md-3 col-sm-6 col-xs-6 col-xxs-12 b-detail-param-box">\
        <span class="i-detail-param-title">Node</span>\
        <span class="i-detail-param-value">{{nodeName}}</span>\
      </div>\
      <div class="col-lg-3 col-md-3 col-sm-6 col-xs-6 col-xxs-12 b-detail-param-box">\
        <span class="i-detail-param-title">Started</span>\
        <span class="i-detail-param-value" title="{{startedDate}}">{{startedDate}}</span>\
      </div>\
      <div class="col-lg-3 col-md-3 col-sm-6 col-xs-6 col-xxs-12 b-detail-param-box">\
        <span class="i-detail-param-title">Completed</span>\
        <span class="i-detail-param-value" title="{{completedDate}}">{{completedDate}}</span>\
      </div>\
      <div class="col-lg-3 col-md-3 col-sm-6 col-xs-6 col-xxs-12 b-detail-param-box">\
        <span class="i-detail-param-title">Source</span>\
        <span class="i-detail-param-value">{{sourceAddress}}</span>\
      </div>\
      <div class="col-lg-3 col-md-3 col-sm-6 col-xs-6 col-xxs-12 b-detail-param-box">\
        <span class="i-detail-param-link"><a href="/api/backup/logs/{{service}}/{{id}}">View Backup Log <span class="chevron glyphicon glyphicon-chevron-right"></span></a></span>\
      </div>\
    </div>\
    {{^chunks?}}\
    <p class="b-info-text">No files</p>\
    {{/chunks?}}\
    {{#chunks?}}\
    <div class="panel-group b-files-group">\
    {{/chunks?}}\
    {{#chunks}}\
      <div class="panel-collapse-control">\
        <span class="glyphicon glyphicon-arrow-down panel-collapse-control-expand" title="Expand All"></span>\
        <span class="glyphicon glyphicon-arrow-up panel-collapse-control-collapse" title="Collapse All"></span>\
      </div>\
      <div class="panel panel-default">\
        <div class="panel-heading">\
          <h4 class="panel-title">\
            <a data-toggle="collapse" href="#{{filename}}">\
              {{filename}}\
            </a>\
            <a class="download-link" href="/download/{{service}}/{{id}}/{{filename}}">\
              <span class="glyphicon glyphicon-save"></span>\
            </a>\
          </h4>\
        </div>\
        <div data-filename="{{filename}}" class="panel-collapse collapse">\
          <div class="panel-body">\
            <table class="table table-condensed">\
              <thead>\
                <tr>\
                  <th>Path</th>\
                  <th>Original Size</th>\
                  <th>Stored Size</th>\
                  <th>Hash</th>\
                  <th>Stored</th>\
                </tr>\
              </thead>\
              <tbody>\
              {{#chunks}}\
                <tr>\
                  <td>{{path}}</td>\
                  <td title="{{originalSize}}">{{& htmlOriginalSize}}</td>\
                  <td title="{{size}}">{{& htmlSize}}</td>\
                  <td>{{hash}}</td>\
                  <td title="{{storedDate}}"><span class="i-chunk-stored">{{storedDate}}</span></td>\
                </tr>\
              {{/chunks}}\
              </tbody>\
            </table>\
          </div>\
        </div>\
      </div>\
    {{/chunks}}\
    {{#chunks?}}\
    </div>\
    {{/chunks?}}\
  ',

  events: {
    "click .b-files-group a[data-toggle=collapse]": "_collapseFile",
    "click .b-files-group .panel-collapse-control .panel-collapse-control-expand": "_expandAllFiles",
    "click .b-files-group .panel-collapse-control .panel-collapse-control-collapse": "_collapseAllFiles"
  },

  initialize: function(options) {
    this.options = options;
    this.listenTo(this.model, "b:indexed", this.render);
    this.listenTo(this.options.verifications, "v:indexed", function() {
      B.ViewUtil.updateVerificationStatuses(this.$(".i-backup-status"), this.options.verifications);
      this.updateVerificationView(this.options.verifications.getByBackupId(this.options.id));
    });
  },

  updateVerificationView: function(verification) {
    if (verification === undefined) {
      return;
    }

    var m = Mustache.render('<div class="col-lg-3 col-md-3 col-sm-6 col-xs-6 col-xxs-12 b-detail-param-box"><span class="i-detail-param-link"><a href="/api/verification/logs/{{service}}/{{id}}">View Verification Log <span class="chevron glyphicon glyphicon-chevron-right"></span></a></span></div>', verification.attributes);
    this.$(".b-details").append(m);
  },

  prepareModelData: function(service, id) {
    var backupData = this.model.getByServiceAndId(service, id),
        viewChunks = [];

     // Moustachify chunks object
    _.each(_.pairs(backupData.get("chunks")), function(fileChunks) {
      var filename = fileChunks[0],
          chunks = fileChunks[1];
      _.each(chunks, function (chunk) {
        chunk.htmlSize = B.ViewUtil.niceifySize(chunk.size);
        chunk.htmlOriginalSize = B.ViewUtil.niceifySize(chunk.originalSize);
        chunk.storedDate = (new Date(chunk.storedDate)).toString();
      });
      viewChunks.push({ chunks: chunks, filename: filename });
    });
    backupData.set("chunks", viewChunks);
    backupData.set("chunks?", viewChunks.length > 0);

    // Translate state to glyphicon class and decription
    backupData.set("status", B.ViewUtil.getGlyphForBackupState(backupData.get("state")));

    // Niceify file sizes
    backupData.set("htmlSize", B.ViewUtil.niceifySize(backupData.get("size")));
    backupData.set("htmlOriginalSize", B.ViewUtil.niceifySize(backupData.get("originalSize")));

    return backupData.attributes;
  },

  render: function() {
    var m = Mustache.render(this.template, this.prepareModelData(this.options.service, this.options.id));
    this.$el.append(m);
    return this;
  },

  _collapseFile: function(e) {
    var filename;
    e.preventDefault();
    filename = $(e.currentTarget).attr("href").split("#")[1];
    this.$el.find("[data-filename='" + filename + "']").collapse("toggle");
  },

  _collapseAllFiles: function(e) {
    this.$el.find(".b-files-group .panel-collapse").collapse("hide");
  },

  _expandAllFiles: function(e) {
    this.$el.find(".b-files-group .panel-collapse").collapse("show");
  }
});
