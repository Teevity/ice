/*
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.ice.basic;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.ice.common.AccountService;
import com.netflix.ice.tag.Account;
import com.netflix.ice.tag.Zone;

import java.util.List;
import java.util.Map;

public class BasicAccountService implements AccountService {

    private Map<String, Account> accountsById = Maps.newHashMap();
    private Map<String, Account> accountsByName = Maps.newHashMap();
    private Map<Account, List<Account>> reservationAccounts = Maps.newHashMap();

    public BasicAccountService(List<Account> accounts, Map<Account, List<Account>> reservationAccounts) {
        this.reservationAccounts = reservationAccounts;
        for (Account account: accounts) {
            accountsByName.put(account.name, account);
            accountsById.put(account.id, account);
        }
    }

    public Account getAccountById(String accountId) {
        Account account = accountsById.get(accountId);
        return account;
    }

    public Account getAccountByName(String accountName) {
        return accountsByName.get(accountName);
    }

    public List<Account> getAccounts(List<String> accountNames) {
        List<Account> result = Lists.newArrayList();
        for (String name: accountNames)
            result.add(accountsByName.get(name));
        return result;
    }

    public Map<Account, List<Account>> getReservationAccounts() {
        return reservationAccounts;
    }

    public Zone getAccountMappedZone(Account mapAccount, Account account, Zone zone) {
        return zone;
    }
}
