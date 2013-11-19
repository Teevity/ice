/*
 *
 *  Copyright 2013 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.ice.common;

import com.netflix.ice.tag.Account;
import com.netflix.ice.tag.Zone;

import java.util.List;
import java.util.Map;

public interface AccountService {
    /**
     * Get account by AWS id. The AWS id is usually an un-readable 12 digit string.
     * @param accountId
     * @return Account object associated with the account id
     */
    Account getAccountById(String accountId);

    /**
     * Get account by account name. The account name is a user defined readable string.
     * @param accountName
     * @return Account object associated with the account name
     */
    Account getAccountByName(String accountName);

    /**
     * Get a list of accounts from given account names.
     * @param accountNames
     * @return List of accounts
     */
    List<Account> getAccounts(List<String> accountNames);

    /**
     * If you don't have reserved instances, you can return an empty map.
     * @return Map of accounts. The keys are owner accounts, the values are list of borrowing accounts.
     */
    Map<Account, List<Account>> getReservationAccounts();

    /**
     * If you don't need to poll reservation capacity through ec2 API for other accounts, you can return an empty map.
     * @return Map of account access roles. The keys are reservation owner accounts,
     * the values are assumed roles to call ec2 describeReservedInstances on each reservation owner account.
     */
    Map<Account, String> getReservationAccessRoles();

    /**
     * If you don't need to poll reservation capacity through ec2 API for other accounts, ir if you don't use external ids,
     * you can return an empty map.
     * @return Map of account access external ids. The keys are reservation owner accounts,
     * the values are external ids to call ec2 describeReservedInstances on each reservation owner account.
     */
    Map<Account, String> getReservationAccessExternalIds();

    /**
     * @param account
     * @param zone
     * @return Whether or not external mappings are not available in specified account.
     */
    boolean externalMappingExist(Account account, Zone zone);
}
