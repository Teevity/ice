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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class BasicAccountService implements AccountService {

    Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, Account> accountsById = Maps.newConcurrentMap();
    private Map<String, Account> accountsByName = Maps.newConcurrentMap();
    private Map<Account, List<Account>> reservationAccounts = Maps.newHashMap();
    private Map<Account, String> reservationAccessRoles = Maps.newHashMap();
    private Map<Account, String> reservationAccessExternalIds = Maps.newHashMap();

    public BasicAccountService(List<Account> accounts, Map<Account, List<Account>> reservationAccounts,
                               Map<Account, String> reservationAccessRoles, Map<Account, String> reservationAccessExternalIds) {
        this.reservationAccounts = reservationAccounts;
        this.reservationAccessRoles = reservationAccessRoles;
        this.reservationAccessExternalIds = reservationAccessExternalIds;
        for (Account account: accounts) {
            accountsByName.put(account.name, account);
            accountsById.put(account.id, account);
        }
    }

    public Account getAccountById(String accountId) {
        Account account = accountsById.get(accountId);
        if (account == null) {
            account = new Account(accountId, accountId);
            accountsByName.put(account.name, account);
            accountsById.put(account.id, account);
            logger.info("created account " + accountId + ".");
        }
        return account;
    }

    public Account getAccountByName(String accountName) {
        Account account = accountsByName.get(accountName);
        // for accounts that were not mapped to names in ice.properties (ice.account.xxx), this check will make sure that
        // data/tags are updated properly once the mapping is established in ice.properties
        if (account == null) {
            account = accountsById.get(accountName);
        }
        if (account == null) {
            account = new Account(accountName, accountName);
            accountsByName.put(account.name, account);
            accountsById.put(account.id, account);
        }
        return account;
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

    public Map<Account, String> getReservationAccessRoles() {
        return reservationAccessRoles;
    }


    public Map<Account, String> getReservationAccessExternalIds() {
        return reservationAccessExternalIds;
    }

    public boolean externalMappingExist(Account account, Zone zone) {
        return true;
    }
}
