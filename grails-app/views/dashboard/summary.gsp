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
  <title>AWS Usage Summary</title>
</head>
<body>
<div class="" style="margin: auto; width: 1200px; padding: 20px 30px" ng-controller="summaryCtrl">

  <table>
    <tr>
      <td>Group By</td>
      <td>Account</td>
      <td>Region</td>
      <td>Product</td>
      <td>Operation</td>
      <td>UsageType</td>
    </tr>
    <tr>
      <td class="metaTd">
        <select ng-model="groupBy" ng-options="a.name for a in groupBys" class="metaInput"></select>
      </td>
      <td class="metaTd">
        <select ng-model="selected_accounts" ng-options="a.name for a in accounts | filter:filter_accounts" ng-change="accountsChanged()" multiple="multiple" class="metaAccounts metaSelect"></select>
        <br><input ng-model="filter_accounts" type="text" class="metaFilter" placeholder="filter">
      </td>
      <td class="metaTd">
        <select ng-model="selected_regions" ng-options="a.name for a in regions | filter:filter_regions" ng-change="regionsChanged()" multiple="multiple" class="metaRegions metaSelect"></select>
        <br><input ng-model="filter_regions" type="text" class="metaFilter" placeholder="filter">
      </td>
      <td class="metaTd">
        <select ng-model="selected_products" ng-options="a.name for a in products | filter:filter_products" ng-change="productsChanged()" multiple="multiple" class="metaProducts metaSelect"></select>
        <br><input ng-model="filter_products" type="text" class="metaFilter" placeholder="filter">
      </td>
      <td class="metaTd">
        <select ng-model="selected_operations" ng-options="a.name for a in operations | filter:filter_operations" ng-change="operationsChanged()" multiple="multiple" class="metaOperations metaSelect"></select>
        <br><input ng-model="filter_operations" type="text" class="metaFilter" placeholder="filter">
      </td>
      <td class="metaTd">
        <select ng-model="selected_usageTypes" ng-options="a.name for a in usageTypes | filter:filter_usageTypes" multiple="multiple" class="metaUsageTypes metaSelect"></select>
        <br><input ng-model="filter_usageTypes" type="text" class="metaFilter" placeholder="filter">
      </td>
    </tr>
  </table>
  <div class="buttons">
    <img src="${resource(dir: '/')}images/spinner.gif" ng-show="loading">
    <a href="javascript:void(0)" class="monitor" style="background-image: url(${resource(dir: '/')}images/tango/16/apps/utilities-system-monitor.png)"
       ng-click="updateUrl(); getData()" ng-show="!loading"
       ng-disabled="selected_accounts.length == 0|| selected_regions.length == 0 || selected_products.length == 0 || selected_operations.length == 0 || selected_usageTypes.length == 0">Submit</a>
  </div>

  <div id="highchart_container" style="width: 100%; height: 400px;">
  </div>
  <div class="list" ng-show="legends && legends.length > 0">
    <div>
      <a href="javascript:void(0)" class="legendControls" ng-click="showall()">SHOW ALL</a>
      <a href="javascript:void(0)" class="legendControls" ng-click="hideall()">HIDE ALL</a>
      <input ng-model="filter_legend" type="text" class="metaFilter" placeHolder="filter" style="float: right; margin-right: 0">
    </div>
    <table>
      <thead>
      <tr>
        <th rowspan="2" ng-click="order('name')">{{legendName}}</th>
        <th colspan="2" ng-repeat="month in monthes" style="text-align: center;">{{monthFormat(month)}}</th>
      </tr>
      <tr>
        <th ng-repeat="header in headers" ng-click="order(header.index)">{{header.name}}</th>
      </tr>
      </thead>
      <tbody>
      <tr ng-repeat="legend in legends | filter:filter_legend" class="{{getTrClass($index)}}"  style="{{legend.style}};">
        <td><span style="cursor: pointer; font-weight: bold;" ng-click="clickitem(legend)"><div class="legendIcon" style="{{legend.iconStyle}}"></div>{{legend.name}}</span></td>
        <td ng-repeat="header in headers" ng-switch on="legend.name">
          <a ng-switch-when="aggregated" ng-href="detail#groupBy={{legendName}}&start={{header.start}}&end={{header.end}}">{{currencySign}} {{data[legend.name][header.index] | number:2}}</a>
          <a ng-switch-default ng-href="detail#{{legendName.toLowerCase()}}={{legend.name}}&groupBy={{nextGroupBy(legendName)}}&start={{header.start}}&end={{header.end}}">{{currencySign}} {{data[legend.name][header.index] | number:2}}</a>
        </td>
      </tr>
      </tbody>
    </table>
  </div>

</div>
</body>
</html>
