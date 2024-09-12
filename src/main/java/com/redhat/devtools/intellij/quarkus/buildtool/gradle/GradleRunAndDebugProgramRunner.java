// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.redhat.devtools.intellij.quarkus.buildtool.gradle;

import com.intellij.build.BuildView;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.testframework.HistoryTestRunnableState;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState;
import com.redhat.devtools.intellij.quarkus.buildtool.BuildToolDelegate;
import com.redhat.devtools.intellij.quarkus.buildtool.maven.MavenToolDelegate;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import org.jetbrains.concurrency.Promises;

/**
 * Program runner to run/debug a Gradle configuration.
 * <p>
 * This class is a copy/paste from the Intellij
 * <a href="https://github.com/JetBrains/intellij-community/blob/master/platform/external-system-impl/src/com/intellij/openapi/externalSystem/service/execution/ExternalSystemTaskRunner.kt">ExternalSystemTaskRunner</a>
 * since this class cannot be extended.
 */
public class GradleRunAndDebugProgramRunner implements ProgramRunner<RunnerSettings> {

    private static final String RUNNER_ID = "GradleRunAndDebugProgramRunner";

    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(String executorId, RunProfile profile) {
        // For running / debugging Gradle 'quarkusDev', the program runner must be executed
        // with the standard IJ 'ExternalSystemTaskRunner' which works only if
        // the profile is an 'ExternalSystemRunConfiguration'. As QuarkusRunConfiguration
        // wraps the profile, this condition is not matched.
        // GradleRunAndDebugProgramRunner should extend ExternalSystemTaskRunner but as this class
        // is final, GradleRunAndDebugProgramRunner is a copy/paste of ExternalSystemTaskRunner
        // and the profile check is done by checking if QuarkusRunConfiguration wraps a Gradle run/debug configuration.
        if (profile instanceof QuarkusRunConfiguration quarkusRunConfiguration) {
            // returns true if the profile is a QuarkusRunConfiguration which wraps a Gradle configuration
            BuildToolDelegate delegate = BuildToolDelegate.getDelegate(quarkusRunConfiguration.getModule());
            return !(delegate instanceof MavenToolDelegate);
        }
        return false;
    }

    @Override
    public void execute(ExecutionEnvironment environment) throws ExecutionException {
        RunProfileState state = environment.getState();
        if (state == null) {
            return;
        }
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, () -> {
            try {
                return Promises.resolvedPromise(doExecute(state, environment));
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private RunContentDescriptor doExecute(RunProfileState state, ExecutionEnvironment environment) throws ExecutionException {
        if (!(state instanceof ExternalSystemRunnableState) && !(state instanceof HistoryTestRunnableState)) {
            return null;
        }

        RunContentDescriptor runContentDescriptor;
        ExecutionResult executionResult = state.execute(environment.getExecutor(), this);
        if (executionResult == null) {
            return null;
        }
        runContentDescriptor = new RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
        if (runContentDescriptor == null) {
            return null;
        }

        if (state instanceof HistoryTestRunnableState) {
            return runContentDescriptor;
        }

        ((ExternalSystemRunnableState) state).setContentDescriptor(runContentDescriptor);

        if (executionResult.getExecutionConsole() instanceof BuildView) {
            return runContentDescriptor;
        }

        RunContentDescriptor descriptor = new RunContentDescriptor(
                runContentDescriptor.getExecutionConsole(),
                runContentDescriptor.getProcessHandler(),
                runContentDescriptor.getComponent(),
                runContentDescriptor.getDisplayName(),
                runContentDescriptor.getIcon(),
                null,
                runContentDescriptor.getRestartActions()
        ) {
            @Override
            public boolean isHiddenContent() {
                return true;
            }
        };
        descriptor.setRunnerLayoutUi(runContentDescriptor.getRunnerLayoutUi());
        return descriptor;
    }
}
