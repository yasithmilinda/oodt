/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oodt.cas.pge;

//OODT static imports
import static org.apache.oodt.cas.pge.metadata.PgeTaskMetKeys.NAME;
import static org.apache.oodt.cas.pge.metadata.PgeTaskMetKeys.CONFIG_FILE_PATH;
import static org.apache.oodt.cas.pge.metadata.PgeTaskMetKeys.PGE_CONFIG_BUILDER;
import static org.apache.oodt.cas.pge.metadata.PgeTaskMetKeys.PROPERTY_ADDERS;
import static org.apache.oodt.cas.pge.metadata.PgeTaskMetKeys.REQUIRED_METADATA;
import static org.apache.oodt.cas.pge.metadata.PgeTaskMetKeys.WORKFLOW_MANAGER_URL;

//JDK imports
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

//Apache imports
import org.apache.commons.io.FileUtils;

//OODT imports
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.pge.PGETaskInstance;
import org.apache.oodt.cas.pge.PGETaskInstance.LoggerOuputStream;
import org.apache.oodt.cas.pge.config.DynamicConfigFile;
import org.apache.oodt.cas.pge.config.MockPgeConfigBuilder;
import org.apache.oodt.cas.pge.config.OutputDir;
import org.apache.oodt.cas.pge.config.PgeConfig;
import org.apache.oodt.cas.pge.metadata.PgeMetadata;
import org.apache.oodt.cas.pge.metadata.PgeTaskMetKeys;
import org.apache.oodt.cas.pge.metadata.PgeTaskStatus;
import org.apache.oodt.cas.pge.writers.MockSciPgeConfigFileWriter;
import org.apache.oodt.cas.workflow.metadata.CoreMetKeys;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.system.XmlRpcWorkflowManagerClient;

//Google imports
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

//JUnit imports
import junit.framework.TestCase;

/**
 * Test class for {@link PGETaskInstance}.
 * 
 * @author bfoster (Brian Foster)
 */
public class TestPGETaskInstance extends TestCase {

   private List<PGETaskInstance> pgeTasks = Lists.newArrayList();
   private List<File> tmpDirs = Lists.newArrayList();

   public void tearDown() throws Exception {
      for (PGETaskInstance pgeTask : pgeTasks) {
         pgeTask.closePgeLogger();
      }
      pgeTasks.clear();
      for (File tmpDir : tmpDirs) {
         FileUtils.forceDelete(tmpDir);
      }
      tmpDirs.clear();
   }

   public void testLoadPropertyAdders() throws Exception {
      PGETaskInstance pgeTask = createTestInstance();
      ConfigFilePropertyAdder propAdder = pgeTask
            .loadPropertyAdder(MockConfigFilePropertyAdder.class
                  .getCanonicalName());
      assertNotNull(propAdder);
      assertTrue(propAdder instanceof MockConfigFilePropertyAdder);
   }

   public void testRunPropertyAdders() throws Exception {
      PGETaskInstance pgeTask = createTestInstance();
      pgeTask.pgeMetadata.replaceMetadata(PROPERTY_ADDERS,
            MockConfigFilePropertyAdder.class.getCanonicalName());
      pgeTask.pgeConfig.setPropertyAdderCustomArgs(new Object[] { "key",
            "value" });
      pgeTask.runPropertyAdders();
      assertEquals("value", pgeTask.pgeMetadata.getMetadata("key"));

      pgeTask.pgeMetadata.replaceMetadata(
            MockConfigFilePropertyAdder.RUN_COUNTER, "0");
      pgeTask.pgeMetadata.replaceMetadata(PROPERTY_ADDERS, Lists.newArrayList(
            MockConfigFilePropertyAdder.class.getCanonicalName(),
            MockConfigFilePropertyAdder.class.getCanonicalName()));
      pgeTask.runPropertyAdders();
      assertEquals("value", pgeTask.pgeMetadata.getMetadata("key"));
      assertEquals("2",
            pgeTask.pgeMetadata
                  .getMetadata(MockConfigFilePropertyAdder.RUN_COUNTER));

      pgeTask.pgeMetadata.replaceMetadata(
            MockConfigFilePropertyAdder.RUN_COUNTER, "0");
      System.setProperty(PgeTaskMetKeys.USE_LEGACY_PROPERTY, "true");
      pgeTask.pgeMetadata.replaceMetadata(PROPERTY_ADDERS.getName(),
            MockConfigFilePropertyAdder.class.getCanonicalName());
      pgeTask.runPropertyAdders();
      assertEquals("value", pgeTask.pgeMetadata.getMetadata("key"));
      assertEquals("1",
            pgeTask.pgeMetadata
                  .getMetadata(MockConfigFilePropertyAdder.RUN_COUNTER));
   }

   public void testCreatePgeMetadata() throws Exception {
      final String PGE_NAME = "PGE_Test";
      final String PGE_REQUIRED_METADATA = "Filename, FileLocation ";
      final String PROP_ADDERS = "some.prop.adder.classpath,some.other.classpath";
      PGETaskInstance pgeTask = createTestInstance();
      Metadata dynMet = new Metadata();
      WorkflowTaskConfiguration config = new WorkflowTaskConfiguration();
      config.addConfigProperty(NAME.getName(), PGE_NAME);
      config.addConfigProperty(REQUIRED_METADATA.getName(),
            PGE_REQUIRED_METADATA);
      config.addConfigProperty(PROPERTY_ADDERS.getName(), PROP_ADDERS);

      PgeMetadata pgeMet = pgeTask.createPgeMetadata(dynMet, config);
      assertEquals(1, pgeMet.getAllMetadata(NAME).size());
      assertEquals(PGE_NAME, pgeMet.getAllMetadata(NAME).get(0));
      assertEquals(2, pgeMet.getAllMetadata(REQUIRED_METADATA).size());
      assertTrue(pgeMet.getAllMetadata(REQUIRED_METADATA).contains("Filename"));
      assertTrue(pgeMet.getAllMetadata(REQUIRED_METADATA).contains(
            "FileLocation"));
      assertEquals(2, pgeMet.getAllMetadata(PROPERTY_ADDERS).size());
      assertTrue(pgeMet.getAllMetadata(PROPERTY_ADDERS).contains(
            "some.prop.adder.classpath"));
      assertTrue(pgeMet.getAllMetadata(PROPERTY_ADDERS).contains(
            "some.other.classpath"));

      // Verify still works when only one property adder is specified.
      pgeTask = createTestInstance();
      config = new WorkflowTaskConfiguration();
      config.addConfigProperty(PgeTaskMetKeys.PROPERTY_ADDERS.getName(),
            "one.prop.adder.only");

      pgeMet = pgeTask.createPgeMetadata(dynMet, config);
      assertEquals(1, pgeMet.getAllMetadata(PROPERTY_ADDERS).size());
      assertEquals("one.prop.adder.only", pgeMet
            .getAllMetadata(PROPERTY_ADDERS).get(0));
   }

   public void testLogger() throws Exception {
      File tmpFile = File.createTempFile("bogus", "bogus");
      File tmpDir = tmpFile.getParentFile();
      tmpFile.delete();
      File tmpDir3 = new File(tmpDir, UUID.randomUUID().toString());
      assertTrue(tmpDir3.mkdirs());

      PGETaskInstance pgeTask1 = createTestInstance();
      pgeTask1.log(Level.INFO, "pge1 message1");
      pgeTask1.log(Level.INFO, "pge1 message2");
      pgeTask1.log(Level.INFO, "pge1 message3");
      pgeTask1.closePgeLogger();
      List<String> messages = FileUtils.readLines(
            new File(pgeTask1.pgeConfig.getExeDir() + "/logs").listFiles()[0],
            "UTF-8");
      assertEquals("INFO: pge1 message1", messages.get(1));
      assertEquals("INFO: pge1 message2", messages.get(3));
      assertEquals("INFO: pge1 message3", messages.get(5));

      PGETaskInstance pgeTask2 = createTestInstance();
      pgeTask2.log(Level.SEVERE, "pge2 message1");
      pgeTask2.closePgeLogger();
      messages = FileUtils.readLines(new File(pgeTask2.pgeConfig.getExeDir()
            + "/logs").listFiles()[0], "UTF-8");
      assertEquals("SEVERE: pge2 message1", messages.get(1));

      PGETaskInstance pgeTask3 = new PGETaskInstance() {
         @Override
         protected LoggerOuputStream createStdOutLogger() {
            return new LoggerOuputStream(Level.INFO, 10);
         }
      };
      pgeTask3.workflowInstId = "1234";
      pgeTask3.pgeMetadata = new PgeMetadata();
      pgeTask3.pgeConfig = new PgeConfig();
      pgeTask3.pgeConfig.setExeDir(tmpDir3.getAbsolutePath());
      pgeTask3.initializePgeLogger();
      LoggerOuputStream los = pgeTask3.createStdOutLogger();
      los.write("This is a test write to a log file".getBytes());
      los.close();
      pgeTask3.closePgeLogger();
      messages = FileUtils.readLines(new File(tmpDir3, "logs").listFiles()[0],
            "UTF-8");
      assertEquals(8, messages.size());
      assertEquals("INFO: This is a ", messages.get(1));
      assertEquals("INFO: test write", messages.get(3));
      assertEquals("INFO:  to a log ", messages.get(5));
      assertEquals("INFO: file", messages.get(7));

      FileUtils.forceDelete(tmpDir3);
   }

   public void testUpdateStatus() throws Exception {
      final Map<String, String> args = Maps.newHashMap();
      PGETaskInstance pgeTask = createTestInstance();
      pgeTask.wm = new XmlRpcWorkflowManagerClient(null) {
         public boolean updateWorkflowInstanceStatus(String instanceId,
               String status) {
            args.put("InstanceId", instanceId);
            args.put("Status", status);
            return true;
         }
      };
      String instanceId = "Test ID";
      String status = PgeTaskStatus.CRAWLING.getWorkflowStatusName();
      pgeTask.workflowInstId = instanceId;
      pgeTask.updateStatus(status);
      assertEquals(instanceId, args.get("InstanceId"));
      assertEquals(status, args.get("Status"));
   }

   public void testCreatePgeConfig() throws Exception {
      final String KEY = "TestKey";
      final String VALUE = "TestValue";
      File pgeConfigFile = new File(createTmpDir("1234"), "pgeConfig.xml");
      FileUtils.writeLines(pgeConfigFile, "UTF-8", 
            Lists.newArrayList(
               "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
               "<pgeConfig>",
               "   <customMetadata>",
               "      <metadata key=\"" + KEY + "\" val=\"" + VALUE
               		+ "\"/>",
               "   </customMetadata>",
               "</pgeConfig>"));
      PGETaskInstance pgeTask = createTestInstance();
      pgeTask.pgeMetadata.replaceMetadata(CONFIG_FILE_PATH,
            pgeConfigFile.getAbsolutePath());
      PgeConfig pgeConfig = pgeTask.createPgeConfig();
      assertNotNull(pgeConfig);
      assertEquals(VALUE, pgeTask.pgeMetadata.getMetadata(KEY));

      pgeTask = createTestInstance();
      pgeTask.pgeMetadata.replaceMetadata(PGE_CONFIG_BUILDER,
            MockPgeConfigBuilder.class.getCanonicalName());
      pgeConfig = pgeTask.createPgeConfig();
      assertEquals(MockPgeConfigBuilder.MOCK_EXE_DIR, pgeConfig.getExeDir());
   }

   public void testCreateWorkflowManagerClient() throws Exception {
      PGETaskInstance pgeTask = createTestInstance();
      pgeTask.pgeMetadata.replaceMetadata(WORKFLOW_MANAGER_URL,
            "http://localhost:8888");
      XmlRpcWorkflowManagerClient wmClient =
         pgeTask.createWorkflowManagerClient();
      assertNotNull(wmClient);
   }

   public void testGetWorkflowInstanceId() throws Exception {
      String workflowInstId = "12345";
      PGETaskInstance pgeTask = createTestInstance();
      pgeTask.pgeMetadata.replaceMetadata(CoreMetKeys.WORKFLOW_INST_ID,
            workflowInstId);
      assertEquals(workflowInstId, pgeTask.getWorkflowInstanceId());
   }

   public void testCreateExeDir() throws Exception {
      PGETaskInstance pgeTask = createTestInstance();
      File exeDir = new File(pgeTask.pgeConfig.getExeDir());
      FileUtils.deleteDirectory(exeDir);
      assertFalse(exeDir.exists());
      pgeTask.createExeDir();
      assertTrue(exeDir.exists());
   }

   public void testCreateOuputDirsIfRequested() throws Exception {
      PGETaskInstance pgeTask = createTestInstance();
      File outputDir1 = createTmpDir("outputDir1");
      FileUtils.forceDelete(outputDir1);
      File outputDir2 = createTmpDir("outputDir2");
      FileUtils.forceDelete(outputDir2);
      File outputDir3 = new File("/some/file/path");
      assertFalse(outputDir1.exists());
      assertFalse(outputDir2.exists());
      assertFalse(outputDir3.exists());
      pgeTask.pgeConfig.addOuputDirAndExpressions(new OutputDir(outputDir1
            .getAbsolutePath(), true));
      pgeTask.pgeConfig.addOuputDirAndExpressions(new OutputDir(outputDir2
            .getAbsolutePath(), true));
      pgeTask.pgeConfig.addOuputDirAndExpressions(new OutputDir(outputDir3
            .getAbsolutePath(), false));
      pgeTask.createOuputDirsIfRequested();
      assertTrue(outputDir1.exists());
      assertTrue(outputDir2.exists());
      assertFalse(outputDir3.exists());
   }

   public void testCreateSciPgeConfigFile() throws Exception {
      File tmpDir = createTmpDir("sciPgeDir");
      FileUtils.forceDelete(tmpDir);
      assertFalse(tmpDir.exists());
      PGETaskInstance pgeTask = createTestInstance();
      File sciPgeConfigFile = new File(tmpDir, "SciPgeConfig.xml");
      assertFalse(sciPgeConfigFile.exists());
      pgeTask.createSciPgeConfigFile(new DynamicConfigFile(sciPgeConfigFile.getAbsolutePath(),
            MockSciPgeConfigFileWriter.class.getCanonicalName(),
            new Object[] {}));
      assertTrue(sciPgeConfigFile.exists());
   }

   private PGETaskInstance createTestInstance() throws Exception {
      return createTestInstance(UUID.randomUUID().toString());
   }

   private PGETaskInstance createTestInstance(String workflowInstId)
         throws Exception {
      PGETaskInstance pgeTask = new PGETaskInstance();
      pgeTask.workflowInstId = workflowInstId;
      pgeTask.pgeMetadata = new PgeMetadata();
      pgeTask.pgeMetadata.replaceMetadata(NAME, "TestPGE");
      pgeTask.pgeConfig = new PgeConfig();
      File exeDir = createTmpDir(workflowInstId);
      pgeTask.pgeConfig.setExeDir(exeDir.getAbsolutePath());
      pgeTask.initializePgeLogger();
      pgeTasks.add(pgeTask);
      return pgeTask;
   }

   private File createTmpDir(String workflowInstId) throws Exception {
      File tmpFile = File.createTempFile("bogus", "bogus");
      File tmpDir = new File(tmpFile.getParentFile(), workflowInstId);
      tmpFile.delete();
      tmpDir.mkdirs();
      tmpDirs.add(tmpDir);
      return tmpDir;
   }
}
