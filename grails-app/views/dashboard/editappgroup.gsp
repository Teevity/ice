<%--

    Copyright 2013 Netflix, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Edit Application Group</title>
</head>
<body>
<div class="" style="margin: auto; width: 1024px; padding: 20px 30px" ng-controller="editCtrl">

  <div class="message" ng-show="message">{{message}}</div>

  <h1><span ng-show="isCreate">Create</span><span ng-show="!isCreate">Edit</span> Application Group <b>{{appgroup.name}}</b></h1>

  <div class="dialog">
    <table>
      <tr class="prop">
        <td class="name">Group Name:</td>
        <td class="name"><input ng-model="appgroup.name" class="required" type="text"/></td>
        <td></td>
      </tr>
      <tr class="prop">
        <td class="name">Email:</td>
        <td class="name"><input ng-model="appgroup.owner" class="required email" type="text"/></td>
        <td></td>
      </tr>
      <tr class="prop" ng-repeat="row in data">
        <td class="name" colspan="2">Selected {{row.displayName}}:<br>
          <select ng-model="left[row.name]" style="vertical-align: top; min-width: 200px" multiple="true" size="10" ng-options="c.name for c in appgroup.data[row.name] | filter:leftfilter[row.name]"></select>
          <input ng-model="leftfilter[row.name]" type="text" class="metaFilter" placeholder="filter">
        </td>
        <td class="name"><br><br><br><br><br>
            <a href="javascript:void(0)" ng-click="add(row)"><img class="icon" src="${resource(dir: '/')}images/tango/tango-icon-theme/32x32/actions/go-previous.png"></a><br>
            <a href="javascript:void(0)" ng-click="remove(row)"><img class="icon" src="${resource(dir: '/')}images/tango/tango-icon-theme/32x32/actions/go-next.png"></a>
        </td>
        <td class="name" colspan="2">Rest of {{row.displayName}}:<br>
            <select ng-model="right[row.name]" style="vertical-align: top; min-width: 200px" multiple="true" size="10" ng-options="c.name for c in row.data | filter:rightfilter[row.name]"></select>
          <input ng-model="rightfilter[row.name]" type="text" class="metaFilter" placeholder="filter">
        </td>
        <td></td>
      </tr>
    </table>
    <div class="buttons">
      <span ng-show="loading"><img src="${resource(dir: "/")}images/spinner.gif" border="0"></span>
      <button ng-show="!loading" ng-disabled="isDisabled()" type="submit" class="save" ng-click="save()"><div>Save</div></button>
      <button ng-show="!loading && !isCreate" type="submit" class="delete" ng-click="delete()"><div>Delete</div></button>
      <a ng-show="!loading" class="restore" href="${resource(dir: 'dashboard', file: 'breakdown')}#appgroup" style="background-image: url(${resource(dir: '/')}images/tango/24/actions/edit-undo.png)">Cancel</a>
    </div>
    </div>

</div>
</body>
</html>