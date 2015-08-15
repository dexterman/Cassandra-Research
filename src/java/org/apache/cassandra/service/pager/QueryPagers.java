/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.service.pager;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.filter.*;
import org.apache.cassandra.db.partitions.*;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.service.ClientState;

/**
 * Static utility methods for paging.
 */
public class QueryPagers
{
    private QueryPagers() {};

    /**
     * Convenience method that count (live) cells/rows for a given slice of a row, but page underneath.
     */
    //只在org.apache.cassandra.thrift.CassandraServer.get_count调用
    public static int countPaged(CFMetaData metadata,
                                 DecoratedKey key,
                                 ColumnFilter columnFilter,
                                 ClusteringIndexFilter filter,
                                 DataLimits limits,
                                 ConsistencyLevel consistencyLevel,
                                 ClientState state,
                                 final int pageSize,
                                 int nowInSec,
                                 boolean isForThrift) throws RequestValidationException, RequestExecutionException
    {
        SinglePartitionReadCommand command = SinglePartitionReadCommand.create(isForThrift, metadata, nowInSec, columnFilter, RowFilter.NONE, limits, key, filter);
        final SinglePartitionPager pager = new SinglePartitionPager(command, null);

        int count = 0;
        while (!pager.isExhausted())
        {
            try (CountingPartitionIterator iter = new CountingPartitionIterator(pager.fetchPage(pageSize, consistencyLevel, state), limits, nowInSec))
            {
                PartitionIterators.consume(iter);
                count += iter.counter().counted();
            }
        }
        return count;
    }
}
