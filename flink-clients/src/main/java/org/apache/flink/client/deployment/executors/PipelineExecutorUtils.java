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

package org.apache.flink.client.deployment.executors;

import org.apache.flink.api.common.JobID;
import org.apache.flink.api.dag.Pipeline;
import org.apache.flink.client.FlinkPipelineTranslationUtil;
import org.apache.flink.client.cli.ClientOptions;
import org.apache.flink.client.cli.ExecutionConfigAccessor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.PipelineOptionsInternal;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.streaming.api.graph.StreamGraph;

import javax.annotation.Nonnull;

import java.net.MalformedURLException;

import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/** Utility class with method related to job execution. */
public class PipelineExecutorUtils {

    /**
     * Creates the {@link JobGraph} corresponding to the provided {@link Pipeline}.
     *
     * @param pipeline the pipeline whose job graph we are computing.
     * @param configuration the configuration with the necessary information such as jars and
     *     classpaths to be included, the parallelism of the job and potential savepoint settings
     *     used to bootstrap its state.
     * @param userClassloader the classloader which can load user classes.
     * @return the corresponding {@link JobGraph}.
     */
    public static JobGraph getJobGraph(
            @Nonnull final Pipeline pipeline,
            @Nonnull final Configuration configuration,
            @Nonnull ClassLoader userClassloader)
            throws MalformedURLException {
        checkNotNull(pipeline);
        checkNotNull(configuration);

        final ExecutionConfigAccessor executionConfigAccessor =
                ExecutionConfigAccessor.fromConfiguration(configuration);
        final JobGraph jobGraph =
                FlinkPipelineTranslationUtil.getJobGraph(
                        userClassloader,
                        pipeline,
                        configuration,
                        executionConfigAccessor.getParallelism());

        configuration
                .getOptional(PipelineOptionsInternal.PIPELINE_FIXED_JOB_ID)
                .ifPresent(strJobID -> jobGraph.setJobID(JobID.fromHexString(strJobID)));

        if (configuration.get(DeploymentOptions.ATTACHED)
                && configuration.get(DeploymentOptions.SHUTDOWN_IF_ATTACHED)) {
            jobGraph.setInitialClientHeartbeatTimeout(
                    configuration.get(ClientOptions.CLIENT_HEARTBEAT_TIMEOUT).toMillis());
        }

        jobGraph.addJars(executionConfigAccessor.getJars());
        jobGraph.setClasspaths(executionConfigAccessor.getClasspaths());
        jobGraph.setSavepointRestoreSettings(executionConfigAccessor.getSavepointRestoreSettings());

        return jobGraph;
    }

    public static StreamGraph getStreamGraph(
            @Nonnull final Pipeline pipeline, @Nonnull final Configuration configuration)
            throws MalformedURLException {
        checkNotNull(pipeline);
        checkNotNull(configuration);
        checkState(pipeline instanceof StreamGraph);

        StreamGraph streamGraph = (StreamGraph) pipeline;

        final ExecutionConfigAccessor executionConfigAccessor =
                ExecutionConfigAccessor.fromConfiguration(configuration);

        configuration
                .getOptional(PipelineOptionsInternal.PIPELINE_FIXED_JOB_ID)
                .ifPresent(strJobID -> streamGraph.setJobId(JobID.fromHexString(strJobID)));

        if (configuration.get(DeploymentOptions.ATTACHED)
                && configuration.get(DeploymentOptions.SHUTDOWN_IF_ATTACHED)) {
            streamGraph.setInitialClientHeartbeatTimeout(
                    configuration.get(ClientOptions.CLIENT_HEARTBEAT_TIMEOUT).toMillis());
        }

        streamGraph.addJars(executionConfigAccessor.getJars());
        streamGraph.setClasspaths(executionConfigAccessor.getClasspaths());
        streamGraph.setSavepointRestoreSettings(
                executionConfigAccessor.getSavepointRestoreSettings());

        return streamGraph;
    }
}
