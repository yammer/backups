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
var B           = {};
B['Model']      = {};
B['View']       = {};
B['ViewUtil']   = {};
B['Collection'] = {};



B.ViewUtil.getGlyphForBackupState = function(state) {
  switch (state) {

  case "FAILED":
    return "glyphicon glyphicon-fire text-danger";

  case "FINISHED": 
    return "glyphicon glyphicon-ok-sign text-success";

  case "TIMEDOUT":
    return "glyphicon glyphicon-time text-warning";

  case "UPLOADING": 
    return "glyphicon glyphicon-cloud-upload text-primary";

  case "RECEIVING":
  case "WAITING":
    return "glyphicon glyphicon-download-alt text-primary";

  default:
    return "glyphicon glyphicon-minus text-muted";
  }
};

var DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;

B.ViewUtil.getStateBasedEntity = function(entities, states) {
    entities = _.sortBy(entities, function(entity) {
        return entity.get("startedDate").getTime() * -1;
    });

    entities = _.groupBy(entities, function(entity) {
        return entity.get("state");
    });

    states = _.filter(states, function(state) {
        return state in entities;
    });

    if (_.isEmpty(states)) {
        return undefined;
    }

    // Take the first entity with the first (best) state
    return entities[states[0]][0];
};

B.ViewUtil.getStateBasedBackup = function (backups) {
  return B.ViewUtil.getStateBasedEntity(backups, ['FINISHED', 'UPLOADING', 'RECEIVING', 'WAITING', 'FAILED', 'TIMEDOUT']);
};

B.ViewUtil.getStateBasedVerification = function (verifications) {
  return B.ViewUtil.getStateBasedEntity(verifications, ['FINISHED', 'STARTED', 'FAILED', 'TIMEDOUT']);
};

B.ViewUtil.extendElementTitleWithVerificationStatus = function($el, verification) {
  if (verification === undefined) {
    return $el;
  }

  var title = $el.prop("title");
  var status = verification.get("state");
  var startedDate = B.ViewUtil.formatDate(verification.get("startedDate"));

  $el.prop("title", [title, "\nVerification:", status, "(started: " + startedDate + ")"].join(" "));
  return $el;
};

B.ViewUtil.getLabelForAge = function(age) {
    return age.toString();
};


B.ViewUtil.formatDate = function(date) {
    return moment(date).format('Do MMM, hh:mm [(]Z[)]');
};


B.ViewUtil.niceifyDate = function(date) {
  return moment(date).format('Do MMM, hh:mm [<span class="text-muted">(]Z[)</span>]');
};


B.ViewUtil.niceifySize = function(bytes) {
  var units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

  var i = 0;
  while (bytes >= 1024) {
    bytes /= 1024;
    i++;
  }

  return bytes.toFixed(1) + '&nbsp;' + units[i];
};


B.ViewUtil.niceifyDuration = function(seconds) {
  var hours = Math.floor(seconds / 3600);
  var minutes = Math.floor((seconds - (hours * 3600)) / 60);
  seconds = Math.floor(seconds - ((hours * 3600) + (minutes * 60)));

  var duration = "";
  if (hours > 0) {
    duration += hours + "h ";
  }
  if (minutes > 0) {
    duration += minutes + "m ";
  }
  if (seconds > 0) {
      duration += seconds + "s ";
  }

  return duration.trim();
};

B.ViewUtil.updateVerificationStatuses = function($backupStatusEls, verifications) {
  $backupStatusEls.each(function() {
    var backupId = $(this).data("backup-id"),
        verification;

    if (backupId) {
      verification = verifications.getByBackupId(backupId);
      if (verification && verification.get("state") === "FINISHED") {
        $(this).addClass("verification-finished");
      } else if (verification && verification.get("state") === "FAILED") {
        $(this).addClass("verification-failed");
      } else if (verification && verification.get("state") === "TIMEDOUT") {
          $(this).addClass("verification-timedout");
      } else if (verification && verification.get("state") === "STARTED") {
        $(this).addClass("verification-started");
      } else {
        $(this).addClass("verification-unknown");
      }
      B.ViewUtil.extendElementTitleWithVerificationStatus($(this), verification);
    }
  });
};

