/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

if (typeof String.prototype.startsWith === "undefined") {
    String.prototype.startsWith = function(searchString, position) {
        position = position || 0;
        return this.lastIndexOf(searchString, position) === position;
    };
}
if (typeof String.prototype.endsWith === "undefined") {
    String.prototype.endsWith = function(searchString, position) {
        var subjectString = this.toString();
        if (position === undefined || position > subjectString.length) {
            position = subjectString.length;
        }
        position -= searchString.length;
        var lastIndex = subjectString.indexOf(searchString, position);
        return lastIndex !== -1 && lastIndex === position;
    };
}
// ES6 Math polyfills
if (typeof Math.sign === "undefined") {
    Math.sign = function(x) {
        x = +x; // convert to a number
        if (x === 0 || isNaN(x)) {
            return Number(x);
        }
        return x > 0 ? 1 : -1;
    };
}
if (typeof Math.trunc === "undefined") {
    Math.trunc = function(x) {
        if (isNaN(x)) {
            return NaN;
        }
        if (x > 0) {
            return Math.floor(x);
        }
        return Math.ceil(x);
    };
}
if (typeof Math.sinh === "undefined") {
    Math.sinh = function(x) {
        var y = Math.exp(x);
        return (y - 1 / y) / 2;
    };
}
if (typeof Math.cosh === "undefined") {
    Math.cosh = function(x) {
        var y = Math.exp(x);
        return (y + 1 / y) / 2;
    };
}
if (typeof Math.tanh === "undefined") {
    Math.tanh = function(x){
        var a = Math.exp(+x), b = Math.exp(-x);
        return a == Infinity ? 1 : b == Infinity ? -1 : (a - b) / (a + b);
    };
}
if (typeof Math.hypot === "undefined") {
    Math.hypot = function() {
        var y = 0;
        var length = arguments.length;

        for (var i = 0; i < length; i++) {
            if (arguments[i] === Infinity || arguments[i] === -Infinity) {
                return Infinity;
            }
            y += arguments[i] * arguments[i];
        }
        return Math.sqrt(y);
    };
}
if (typeof Math.log10 === "undefined") {
    Math.log10 = function(x) {
        return Math.log(x) * Math.LOG10E;
    };
}
if (typeof Math.log2 === "undefined") {
    Math.log2 = function(x) {
        return Math.log(x) * Math.LOG2E;
    };
}
if (typeof Math.log1p === "undefined") {
    Math.log1p = function(x) {
        return Math.log(x + 1);
    };
}
if (typeof Math.expm1 === "undefined") {
    Math.expm1 = function(x) {
        return Math.exp(x) - 1;
    };
}
// For HtmlUnit and PhantomJs
if (typeof ArrayBuffer.isView === "undefined") {
    ArrayBuffer.isView = function(a) {
        return a != null && a.__proto__ != null && a.__proto__.__proto__ === Int8Array.prototype.__proto__;
    };
}