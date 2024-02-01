/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.quarkus.tests;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.HeavyWeightWindowFixture;
import com.intellij.remoterobot.fixtures.JLabelFixture;
import com.intellij.remoterobot.fixtures.JListFixture;
import com.intellij.remoterobot.fixtures.JTreeFixture;
import com.redhat.devtools.intellij.commonuitest.UITestRunner;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.FlatWelcomeFrame;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.information.TipDialog;
import com.redhat.devtools.intellij.commonuitest.utils.constants.XPathDefinitions;
import com.redhat.devtools.intellij.commonuitest.utils.internalerror.IdeInternalErrorUtils;
import com.redhat.devtools.intellij.commonuitest.utils.runner.IntelliJVersion;
import com.redhat.devtools.intellij.commonuitest.utils.testextension.ScreenshotAfterTestFailExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * Abstract test class
 *
 * @author zcervink@redhat.com
 */
//@ExtendWith(ScreenshotAfterTestFailExtension.class)
abstract public class AbstractQuarkusTest {
    protected static RemoteRobot remoteRobot;
    private static boolean intelliJHasStarted = false;

    @BeforeAll
    protected static void startIntelliJ() {
        if (!intelliJHasStarted) {
            remoteRobot = UITestRunner.runIde(IntelliJVersion.COMMUNITY_V_2022_1, 8580);
            intelliJHasStarted = true;
            Runtime.getRuntime().addShutdownHook(new CloseIntelliJBeforeQuit());

            FlatWelcomeFrame flatWelcomeFrame = remoteRobot.find(FlatWelcomeFrame.class, Duration.ofSeconds(10));
            flatWelcomeFrame.disableNotifications();
//            flatWelcomeFrame.preventTipDialogFromOpening();
            preventTipDialogFromOpening();
            flatWelcomeFrame.switchToProjectsPage();
        }
    }

    public static void preventTipDialogFromOpening() {
        IdeInternalErrorUtils.clearWindowsErrorsIfTheyAppear(remoteRobot);
        JTreeFixture jTreeFixture = remoteRobot.find(JTreeFixture.class, byXpath(XPathDefinitions.TREE));
        jTreeFixture.findText("Learn").click();
        FlatWelcomeFrame flatWelcomeFrame = remoteRobot.find(FlatWelcomeFrame.class);
        flatWelcomeFrame.findText("Tip of the Day").click();

        TipDialog tipDialog = remoteRobot.find(TipDialog.class, Duration.ofSeconds(10));
        tipDialog.dontShowTipsCheckBox().setValue(true);
        tipDialog.close();
    }

//    public TipDialog MyopenTipDialog() {
//        IdeInternalErrorUtils.clearWindowsErrorsIfTheyAppear(remoteRobot);
//        JTreeFixture jTreeFixture = remoteRobot.find(JTreeFixture.class, byXpath(XPathDefinitions.TREE));
//        jTreeFixture.findText("Learn").click();
//        FlatWelcomeFrame flatWelcomeFrame = remoteRobot.find(FlatWelcomeFrame.class);
//        flatWelcomeFrame.findText("Tip of the Day").click();
//
//        return remoteRobot.find(TipDialog.class, Duration.ofSeconds(10));
//    }

    private static class CloseIntelliJBeforeQuit extends Thread {
        @Override
        public void run() {
            UITestRunner.closeIde();
        }
    }
}