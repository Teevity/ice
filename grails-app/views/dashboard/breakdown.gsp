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
  <title>Aws Usage Breakdown</title>
</head>
<body>
<div class="" style="margin: 20px 30px;" ng-controller="breakdownCtrl">
  <div class="message" ng-show="message">{{message}}</div>

  <table>
    <tr>
      <td></td>
      <td>Show</td>
      <td>Account</td>
      <td>Region</td>
      <td>Product</td>
      <td>Operation</td>
      <td>UsageType</td>
    </tr>
    <tr>
      <td nowrap="">
        <div style="padding-top: 00px">End: <input class="required" type="text" name="end" id="end" size="15"/></div>
        <div style="padding-top: 10px"># of Spans: <input class="required" type="text" name="spans" id="spans" size="8" ng-model="spans"/></div>
        <div style="padding-top: 10px">Aggregate: <select ng-model="consolidate">
            <option>hourly</option>
            <option>daily</option>
            <option>weekly</option>
            <option>monthly</option>
          </select></div>
      </td>
      <td>
        <input type="radio" ng-model="usage_cost" value="cost"> <label>Cost</label><br>
        <input type="radio" ng-model="usage_cost" value="usage"> <label>Usage</label>
      </td>
      <td>
        <select ng-model="selected_accounts" ng-options="a.name for a in accounts | filter:filter_accounts" ng-change="accountsChanged()" multiple="multiple" class="metaAccounts metaSelect"></select>
        <br><input ng-model="filter_accounts" type="text" class="metaFilter" placeholder="filter">
      </td>
      <td>
        <select ng-model="selected_regions" ng-options="a.name for a in regions | filter:filter_regions" ng-change="regionsChanged()" multiple="multiple" class="metaRegions metaSelect"></select>
        <br><input ng-model="filter_regions" type="text" class="metaFilter" placeholder="filter">
      </td>
      <td>
        <select ng-model="selected_products" ng-options="a.name for a in products | filter:filter_products" ng-change="productsChanged()" multiple="multiple" class="metaProducts metaSelect"></select>
        <br><input ng-model="filter_products" type="text" class="metaFilter" placeholder="filter">
      </td>
      <td>
        <select ng-model="selected_operations" ng-options="a.name for a in operations | filter:filter_operations" ng-change="operationsChanged()" multiple="multiple" class="metaOperations metaSelect"></select>
        <br><input ng-model="filter_operations" type="text" class="metaFilter" placeholder="filter">
      </td>
      <td>
        <select ng-model="selected_usageTypes" ng-options="a.name for a in usageTypes | filter:filter_usageTypes" multiple="multiple" class="metaUsageTypes metaSelect"></select>
        <br><input ng-model="filter_usageTypes" type="text" class="metaFilter" placeholder="filter">
      </td>

    </tr>
  </table>

  <div class="buttons">
    <img src="${resource(dir: '/')}images/spinner.gif" ng-show="loading">
    <a href="javascript:void(0)" class="monitor" style="background-image: url(${resource(dir: '/')}images/tango/16/apps/utilities-system-monitor.png)"
       ng-click="updateUrl(); getData()" ng-show="!loading"
       ng-disabled="selected_accounts.length == 0 || selected_regions.length == 0 || selected_products.length == 0 || selected_operations.length == 0 || selected_usageTypes.length == 0">Submit</a>
  </div>

  <div class="list">
    <div>
      <input ng-model="filter_legend" type="text" class="metaFilter" placeHolder="filter" style="float: right; margin-right: 0">
    </div>
    <table>
      <thead>
      <tr>
        <th rowspan="2" ng-show="groupBy.name == 'ApplicationGroup'" style="text-align: center; width: 35px">Edit</th>
        <th rowspan="2" ng-show="groupBy.name == 'ApplicationGroup'" style="text-align: center; width: 40px">Delete</th>
        <th rowspan="2" ng-click="order('name')">{{groupBy.name}}</th>
        <th colspan="2" ng-repeat="period in periods" style="text-align: center;">{{dayFormat(period)}} {{getConsolidateName(data_consolidate)}}</th>
      </tr>
      <tr>
        <th ng-repeat="header in headers" ng-click="order(header.index)">{{header.name}}</th>
      </tr>
      </thead>
      <tbody>
      <tr ng-repeat="row in data | filter:filter_legend" class="{{getTrClass($index)}}">
        <td ng-show="groupBy.name == 'ApplicationGroup'" style="text-align: center;">
          <a ng-href="editappgroup#{{row.name}}"><img src="${resource(dir: '/')}images/tango/16/tools/draw-freehand.png" alt="Edit"></a>
        </td>
        <td ng-show="groupBy.name == 'ApplicationGroup'" style="text-align: center;">
          <a href="javascript:void(0)" ng-click="deleteAppGroup(row.name);"><img src="${resource(dir: '/')}images/tango/16/places/user-trash.png" alt="Delete"></a>
        </td>
        <td ng-switch on="groupBy.name">
          <a ng-switch-when="ResourceGroup" ng-href="detail#showResourceGroups=true&groupBy=UsageType&start={{dataStart}}&end={{dataEnd}}&usage_cost={{usage_cost}}&resourceGroup={{row.name}}">{{row.name}}</a>
          <a ng-switch-default ng-href="appgroup#appgroup={{row.name}}&start={{dataStart}}&end={{dataEnd}}&usage_cost={{usage_cost}}">{{row.name}}</a>
        </td>
        <td ng-repeat="header in headers">
          <span ng-show="legend_usage_cost == 'cost'">{{currencySign}} </span>{{row[header.index] | number:2}}
        </td>
      </tr>
      </tbody>
    </table>
  </div>

</div>
</body>
</html>