/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hdds.scm.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.hadoop.hdds.HddsIdFactory;
import org.apache.hadoop.hdds.protocol.DatanodeDetails;
import org.apache.hadoop.hdds.protocol.MockDatanodeDetails;
import org.apache.hadoop.hdds.protocol.proto.StorageContainerDatanodeProtocolProtos.CommandStatus;
import org.apache.hadoop.hdds.protocol.proto.StorageContainerDatanodeProtocolProtos.CommandStatusReportsProto;
import org.apache.hadoop.hdds.protocol.proto.StorageContainerDatanodeProtocolProtos.SCMCommandProto.Type;
import org.apache.hadoop.hdds.scm.HddsTestUtils;
import org.apache.hadoop.hdds.scm.server.SCMDatanodeHeartbeatDispatcher;
import org.apache.hadoop.hdds.scm.server.SCMDatanodeHeartbeatDispatcher.CommandStatusReportFromDatanode;
import org.apache.hadoop.hdds.server.events.Event;
import org.apache.hadoop.hdds.server.events.EventPublisher;
import org.apache.ozone.test.GenericTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for command status report handler.
 */
public class TestCommandStatusReportHandler implements EventPublisher {

  private static final Logger LOG = LoggerFactory
      .getLogger(TestCommandStatusReportHandler.class);
  private CommandStatusReportHandler cmdStatusReportHandler;

  @BeforeEach
  public void setup() {
    cmdStatusReportHandler = new CommandStatusReportHandler();
  }

  @Test
  public void testCommandStatusReport() {
    GenericTestUtils.LogCapturer logCapturer = GenericTestUtils.LogCapturer.captureLogs(LOG);

    CommandStatusReportFromDatanode report = this.getStatusReport(Collections
        .emptyList());
    cmdStatusReportHandler.onMessage(report, this);
    assertThat(logCapturer.getOutput()).doesNotContain("Delete_Block_Status");
    assertThat(logCapturer.getOutput()).doesNotContain("Replicate_Command_Status");

    report = this.getStatusReport(this.getCommandStatusList());
    cmdStatusReportHandler.onMessage(report, this);
    assertThat(logCapturer.getOutput()).contains("firing event of type " +
        "Delete_Block_Status");
    assertThat(logCapturer.getOutput()).contains("type: " +
        "deleteBlocksCommand");

  }

  private CommandStatusReportFromDatanode getStatusReport(
      List<CommandStatus> reports) {
    CommandStatusReportsProto report = HddsTestUtils.createCommandStatusReport(
        reports);
    DatanodeDetails dn = MockDatanodeDetails.randomDatanodeDetails();
    return new SCMDatanodeHeartbeatDispatcher.CommandStatusReportFromDatanode(
        dn, report);
  }

  @Override
  public <PAYLOAD, EVENT_TYPE extends Event<PAYLOAD>> void
      fireEvent(EVENT_TYPE event, PAYLOAD payload) {
    LOG.info("firing event of type {}, payload {}", event.getName(), payload
        .toString());
  }

  private List<CommandStatus> getCommandStatusList() {
    List<CommandStatus> reports = new ArrayList<>(3);

    // Add status message for replication, close container and delete block
    // command.
    CommandStatus.Builder builder = CommandStatus.newBuilder();

    builder.setCmdId(HddsIdFactory.getLongId())
        .setStatus(CommandStatus.Status.EXECUTED)
        .setType(Type.deleteBlocksCommand);
    reports.add(builder.build());

    builder.setMsg("Not enough space")
        .setCmdId(HddsIdFactory.getLongId())
        .setStatus(CommandStatus.Status.FAILED)
        .setType(Type.replicateContainerCommand);
    reports.add(builder.build());
    return reports;
  }

}
