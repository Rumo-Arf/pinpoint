/*
 * Copyright 2017 NAVER Corp.
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

package com.navercorp.pinpoint.profiler.monitor.datasource;

import com.codahale.metrics.Metric;
import com.navercorp.pinpoint.bootstrap.plugin.monitor.DataSourceMonitor;
import com.navercorp.pinpoint.bootstrap.plugin.monitor.DataSourceMonitorList;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.profiler.monitor.codahale.datasource.DefaultDataSourceCollector;
import com.navercorp.pinpoint.profiler.monitor.codahale.datasource.metric.DataSourceGauge;
import com.navercorp.pinpoint.profiler.monitor.codahale.datasource.metric.DataSourceMetricSet;
import com.navercorp.pinpoint.thrift.dto.TDataSource;
import com.navercorp.pinpoint.thrift.dto.TDataSourceList;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author Taejin Koo
 */
public class DefaultDataSourceCollectorTest {


    @Test
    public void collectTest() throws Exception {
        int createMockObjectSize = 10;

        DataSourceMonitorList dataSourceMonitorList = new DataSourceMonitorList(createMockObjectSize);
        MockDataSourceMonitor[] mockDataSourceMonitors = createMockDataSourceMonitor(dataSourceMonitorList, dataSourceMonitorList.getRemainingIdNumber());

        DataSourceMetricSet metricSet = new DataSourceMetricSet(dataSourceMonitorList);
        DefaultDataSourceCollector dataSourceCollector = new DefaultDataSourceCollector(metricSet);
        TDataSourceList collect = dataSourceCollector.collect();
        assertIdIsUnique(collect.getDataSourceList());

        for (MockDataSourceMonitor dataSourceMonitor : mockDataSourceMonitors) {
            assertContainsAndEquals(dataSourceMonitor, collect.getDataSourceList());
        }
    }

    private MockDataSourceMonitor[] createMockDataSourceMonitor(DataSourceMonitorList dataSourceMonitorRegistry, int remainingCapacity) {
        MockDataSourceMonitor[] mockDataSourceMonitors = new MockDataSourceMonitor[remainingCapacity];
        for (int i = 0; i < remainingCapacity; i++) {
            MockDataSourceMonitor mock = new MockDataSourceMonitor(i);
            boolean register = dataSourceMonitorRegistry.register(mock);
            Assert.assertTrue(register);
            mockDataSourceMonitors[i] = mock;
        }
        return mockDataSourceMonitors;
    }

    private List<TDataSource> extractTDataSourceList(Map<String, Metric> metrics) {
        List<TDataSource> dataSourceList = new ArrayList<TDataSource>();
        Collection<Metric> values = metrics.values();
        for (Metric value : values) {
            if (value instanceof DataSourceGauge) {
                TDataSource dataSource = ((DataSourceGauge) value).getValue();
                dataSourceList.add(dataSource);
            }
        }
        return dataSourceList;
    }

    private void assertIdIsUnique(List<TDataSource> dataSourceList) {
        Set<Integer> idSet = new HashSet<Integer>();

        for (TDataSource dataSource : dataSourceList) {
            idSet.add(dataSource.getId());
        }

        Assert.assertEquals(dataSourceList.size(), idSet.size());
    }

    private void assertContainsAndEquals(DataSourceMonitor dataSourceMonitor, List<TDataSource> dataSourceList) {
        for (TDataSource dataSource : dataSourceList) {
            String name = dataSourceMonitor.getName();

            if (name.equals(dataSource.getName())) {
                Assert.assertEquals(dataSourceMonitor.getUrl(), dataSource.getUrl());
                Assert.assertEquals(dataSourceMonitor.getActiveConnectionSize(), dataSource.getActiveConnectionSize());
                Assert.assertEquals(dataSourceMonitor.getMaxConnectionSize(), dataSource.getMaxConnectionSize());
                Assert.assertEquals(dataSourceMonitor.getServiceType().getCode(), dataSource.getServiceTypeCode());
                return;
            }

        }
        Assert.fail();
    }

    private static class MockDataSourceMonitor implements DataSourceMonitor {

        private static final Random RANDOM = new Random(System.currentTimeMillis());
        private static final int MIN_VALUE_OF_MAX_CONNECTION_SIZE = 20;
        private static final ServiceType[] SERVICE_TYPE_LIST = {ServiceType.UNKNOWN, ServiceType.UNDEFINED, ServiceType.TEST};

        private final int id;
        private final ServiceType serviceType;
        private final int activeConnectionSize;
        private final int maxConnectionSize;

        private boolean closed = false;

        public MockDataSourceMonitor(int index) {
            this.id = index;
            this.serviceType = SERVICE_TYPE_LIST[RANDOM.nextInt(SERVICE_TYPE_LIST.length)];
            this.maxConnectionSize = MIN_VALUE_OF_MAX_CONNECTION_SIZE + RANDOM.nextInt(MIN_VALUE_OF_MAX_CONNECTION_SIZE);
            this.activeConnectionSize = RANDOM.nextInt(maxConnectionSize);
        }

        @Override
        public String getName() {
            return "name" + id;
        }

        @Override
        public String getUrl() {
            return "url" + id;
        }

        @Override
        public int getActiveConnectionSize() {
            return activeConnectionSize;
        }

        @Override
        public int getMaxConnectionSize() {
            return maxConnectionSize;
        }

        @Override
        public ServiceType getServiceType() {
            return serviceType;
        }

        @Override
        public boolean isDisabled() {
            return closed;
        }

        public void close() {
            closed = true;
        }

    }

}