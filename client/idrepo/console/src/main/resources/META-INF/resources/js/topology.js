/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var def = {
  paintStyle: {
    lineWidth: 2,
    strokeStyle: "rgba(204,204,204, 1)",
    outlineColor: "#666",
    outlineWidth: 1
  },
  connectorPaintStyle: {
    lineWidth: 2
  },
  anchor: "AutoDefault",
  detachable: false,
  endpointStyle: {
    gradient: {
      stops: [
        [0, "rgba(204,204,204, 1)"], [1, "rgba(180, 180, 200, 1)"]
      ],
      offset: 5.5,
      innerRadius: 3.5
    },
    radius: 3.5
  }
};

var failedConnectorStyle = {
  lineWidth: 2,
  strokeStyle: "rgba(220, 220, 220, 1)",
  outlineColor: "#666",
  outlineWidth: 1
};

var failedConnectorHoverStyle = {
  strokeStyle: "#FFFFFF"
};

var failedEndpointStyle = {
  gradient: {
    stops: [
      [0, "rgba(220, 220, 220, 1)"], [1, "rgba(180, 180, 200, 1)"]
    ],
    offset: 5.5,
    innerRadius: 0
  },
  radius: 0
};

var disabledConnectorStyle = {
  lineWidth: 2,
  strokeStyle: "rgba(255, 69, 0, 1)",
  outlineColor: "#666",
  outlineWidth: 1
};

var disabledConnectorHoverStyle = {
  strokeStyle: "#FF8C00"
};

var disabledEndpointStyle = {
  gradient: {
    stops: [
      [0, "rgba(255, 69, 0, 1)"], [1, "rgba(180, 180, 200, 1)"]
    ],
    offset: 5.5,
    innerRadius: 1
  },
  radius: 1
};

var enabledConnectorStyle = {
  lineWidth: 2,
  strokeStyle: "rgba(65, 155, 30, 1)",
  outlineColor: "#666",
  outlineWidth: 1
};

var enabledConnectorHoverStyle = {
  strokeStyle: "#00FF00"
};

var enabledEndpointStyle = {
  gradient: {
    stops: [
      [0, "rgba(65, 155, 30, 0.1)"], [1, "rgba(180, 180, 200, 0.1)"]
    ],
    offset: 5.5,
    innerRadius: 2
  },
  radius: 2
};

window.disable = function (targetName) {
  jsPlumb.ready(function () {
    jsPlumb.select({target: targetName}).setPaintStyle(disabledConnectorStyle).setHoverPaintStyle(disabledConnectorHoverStyle);
    jsPlumb.selectEndpoints({element: [targetName]}).setPaintStyle(disabledEndpointStyle);
  });
}

window.enable = function (targetName) {
  jsPlumb.ready(function () {
    jsPlumb.select({target: targetName}).setPaintStyle(enabledConnectorStyle).setHoverPaintStyle(enabledConnectorHoverStyle);
    jsPlumb.selectEndpoints({element: [targetName]}).setPaintStyle(enabledEndpointStyle);
  });
}

window.failure = function (targetName) {
  jsPlumb.ready(function () {
    jsPlumb.select({target: targetName}).setPaintStyle(failedConnectorStyle).setHoverPaintStyle(failedConnectorHoverStyle);
    jsPlumb.selectEndpoints({element: [targetName]}).setPaintStyle(failedEndpointStyle);
  });
}

window.unknown = function (targetName) {
}

function getTopology() {
  var topology = $.cookie("topology");

  if (topology == null) {
    var val = {};
  } else {
    var val = JSON.parse(decodeURIComponent(topology));
  }

  return val;
}

window.refreshPosition = function (element) {
  var val = getTopology();

  var id = $(element).attr('id');
  var left = $(element).css('left');
  var top = $(element).css('top');

  if (val[id] == null) {
    val[id] = {'top': top, 'left': left};
  } else {
    val[id].top = top;
    val[id].left = left;
  }

  $.cookie("topology", JSON.stringify(val), {expires: 9999});
}

window.setPosition = function (id, x, y) {
  var val = getTopology();

  try {
    // We cannot use jQuery selector for id since the syntax of connector server id
    var element = $(document.getElementById(id));

    if (val[id] == null) {
      element.css("left", x + "px");
      element.css("top", y + "px");
    } else {
      element.css("left", val[id].left);
      element.css("top", val[id].top);
    }
  } catch (err) {
    console.log("Failure setting position for ", id);
  }
}

window.setZoom = function (el, zoom, instance, transformOrigin) {
  transformOrigin = transformOrigin || [0.5, 0.5];
  instance = instance || jsPlumb;
  el = el || instance.getContainer();

  var p = ["webkit", "moz", "ms", "o"],
          s = "scale(" + zoom + ")",
          oString = (transformOrigin[0] * 100) + "% " + (transformOrigin[1] * 100) + "%";

  for (var i = 0; i < p.length; i++) {
    el.style[p[i] + "Transform"] = s;
    el.style[p[i] + "TransformOrigin"] = oString;
  }

  el.style["transform"] = s;
  el.style["transformOrigin"] = oString;

  instance.setZoom(zoom);
};

window.zoomIn = function (el, instance, transformOrigin) {
  var val = getTopology();
  if (val.__zoom__ == null) {
    var zoom = 0.69;
  } else {
    var zoom = val.__zoom__ + 0.01;
  }

  setZoom(el, zoom, instance, transformOrigin);

  val['__zoom__'] = zoom;
  $.cookie("topology", JSON.stringify(val), {expires: 9999});
};

window.zoomOut = function (el, instance, transformOrigin) {
  var val = getTopology();
  if (val.__zoom__ == null) {
    var zoom = 0.67;
  } else {
    var zoom = val.__zoom__ - 0.01;
  }

  setZoom(el, zoom, instance, transformOrigin);

  val['__zoom__'] = zoom;
  $.cookie("topology", JSON.stringify(val), {expires: 9999});
};

window.connect = function (source, target, scope) {
  jsPlumb.ready(function () {
    if (jsPlumb.select({source: source, target: target, scope: scope}) != null) {
      jsPlumb.connect({source: source, target: target, scope: scope}, def);
    }
  });
}

window.activate = function (zoom) {
  jsPlumb.ready(function () {
    jsPlumb.draggable(jsPlumb.getSelector(".window"));
    jsPlumb.setContainer("drawing");

    jsPlumb.Defaults.MaxConnections = 1000;

    $("#drawing").draggable({
      containment: 'topology',
      cursor: 'move'
    });

    var val = getTopology();
    if (val.__zoom__ == null) {
      setZoom($("#drawing")[0], zoom);
    } else {
      setZoom($("#drawing")[0], val.__zoom__);
    }
  });
}

window.checkConnection = function () {
  jsPlumb.ready(function () {
    jsPlumb.select({scope: "CONNECTOR"}).each(function (connection) {
      Wicket.WebSocket.send("{ \"kind\":\"CHECK_CONNECTOR\", \"target\":\"" + connection.target.id + "\" }");
    });
    jsPlumb.select({scope: "RESOURCE"}).each(function (connection) {
      Wicket.WebSocket.send("{ \"kind\":\"CHECK_RESOURCE\", \"target\":\"" + connection.target.id + "\" }");
    });
  });
}

window.addEndpoint = function (source, target, scope) {
  var sourceElement = $(document.getElementById(source));

  var top = parseFloat(sourceElement.css("top")) + 10;
  var left = parseFloat(sourceElement.css("left")) - 150;

  setPosition(target, left, top);
  jsPlumb.ready(function () {
    jsPlumb.draggable(jsPlumb.getSelector(document.getElementById(target)));
    jsPlumb.connect({source: source, target: target, scope: scope}, def);
  });
}

jsPlumb.importDefaults({
  Connector: ["Straight"],
  DragOptions: {
    cursor: "pointer",
    zIndex: 2000
  },
  HoverClass: "connector-hover"
});

jQuery(function ($) {
  Wicket.Event.subscribe("/websocket/message", function (jqEvent, message) {
    var val = JSON.parse(decodeURIComponent(message));
    switch (val.status) {
      case 'UNKNOWN':
        unknown(val.target);
        break;
      case 'REACHABLE':
        enable(val.target);
        break;
      case 'UNREACHABLE':
        disable(val.target);
        break;
      case 'FAILURE':
        failure(val.target);
        break;
      default:
        break;
    }
  });
});
