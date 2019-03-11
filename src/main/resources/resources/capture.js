/*
 * <Alice LiveMan>
 * Copyright (C) <2018>  <NekoSunflower>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

var page = require('webpage').create();
system = require('system');
var address;
if (system.args.length < 3) {
    phantom.exit();
} else {
    page.viewportSize = {width: system.args[2], height: system.args[3]};
    console.log(system.args);
    page.open(system.args[1], function (status) {
        if (status != "success") {
            phantom.exit();
        }
        window.setInterval(function () {
            page.evaluate(function (width, height) {
                var app = document.getElementById("app");
                app.style.width = width + 'px';
                app.style.height = height + 'px';
                app.style.display = "block";
                app.style.overflow = "hidden";
                app.scrollTop = 100000;
            }, system.args[2], system.args[3]);
            var base64 = page.renderBase64('PNG');
            console.log("capture:" + base64);
        }, 1000);
    });
}