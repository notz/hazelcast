/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.client.proxy;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.proxy.ProxyHelper;
import com.hazelcast.core.Transaction;
import com.hazelcast.nio.protocol.Command;

public class TransactionClientProxy implements Transaction {
    final ProxyHelper proxyHelper;

    public TransactionClientProxy(String name, HazelcastClient client) {
        proxyHelper = new ProxyHelper(client.getSerializationService(), client.getConnectionPool());
    }

    public void begin() throws IllegalStateException {
        proxyHelper.doCommand(Command.TRXBEGIN, new String[]{}, null);
    }

    public void commit() throws IllegalStateException {
        proxyHelper.doCommand(Command.TRXCOMMIT, new String[]{}, null);
//        ClientThreadContext threadContext = ClientThreadContext.get();
//        threadContext.removeTransaction();
    }

    public int getStatus() {
        return 0;
    }

    public void rollback() throws IllegalStateException {
        proxyHelper.doCommand(Command.TRXROLLBACK, new String[]{}, null);
//        ClientThreadContext threadContext = ClientThreadContext.get();
//        threadContext.removeTransaction();
    }
}