package org.jenkinsci.plugins.envinject;

import hudson.Util;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;

import java.util.HashMap;
import java.util.Map;

import jenkins.model.Jenkins;
import junit.framework.Assert;

import org.jenkinsci.lib.envinject.EnvInjectAction;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.JenkinsRule;
import org.apache.commons.io.FileUtils;
import hudson.model.*;
import hudson.tasks.Shell;
import org.junit.Test;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectBuildWrapperTest extends HudsonTestCase {
    @Rule public JenkinsRule j = new JenkinsRule();

    @Test
    public void testPropertiesContentCustomWorkspace() throws Exception {

        String customWorkspaceValue = Hudson.getInstance().getRootPath().getRemote() + "/customWorkspace";
        String customEnvVarName = "materialize_workspace_path";
        String customEnvVarValue = "${WORKSPACE}/materialize_workspace";
        
        /**
         * Testing
         */
        String customWorkspaceValue2 = Jenkins.getInstance().getRootPath().getRemote() + "/customWorkspace";
        System.out.println("hudson: " + customWorkspaceValue);
        System.out.println("Jenkins: " + customWorkspaceValue2);
        System.out.println("Jenkins root path: " + Jenkins.getInstance().getRootPath());
        System.out.println("getRootDir: " + Jenkins.getInstance().getRootDir());
        System.out.println("getRootUrl: " + Jenkins.getInstance().getRootUrl());

        FreeStyleProject project = createFreeStyleProject();
        project.setCustomWorkspace(customWorkspaceValue);

        String propertiesContent = customEnvVarName + "=" + customEnvVarValue;

        EnvInjectJobPropertyInfo jobPropertyInfo = new EnvInjectJobPropertyInfo(null, propertiesContent, null, null, null, false);
        EnvInjectBuildWrapper envInjectBuildWrapper = new EnvInjectBuildWrapper();
        envInjectBuildWrapper.setInfo(jobPropertyInfo);
        project.getBuildWrappersList().add(envInjectBuildWrapper);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());

        //1-- Compute expected injected var value
        //Retrieve build workspace
        String buildWorkspaceValue = build.getWorkspace().getRemote();
        Assert.assertEquals(customWorkspaceValue, buildWorkspaceValue);
        //Compute value with workspace
        Map<String, String> mapEnvVars = new HashMap<String, String>();
        mapEnvVars.put("WORKSPACE", buildWorkspaceValue);
        String expectedCustomEnvVarValue = resolveVars(customEnvVarValue, mapEnvVars);
        //2-- Get injected value for the specific variable
        EnvInjectAction envInjectAction = build.getAction(EnvInjectAction.class);
        Assert.assertNotNull(envInjectAction);
        Map<String, String> envVars = envInjectAction.getEnvMap();
        Assert.assertNotNull(envVars);
        String resolvedValue = envVars.get(customEnvVarName);
        Assert.assertNotNull(resolvedValue);
        
        /**
         * Testing
         */
        System.out.println("build get url: " + build.getUrl());
        System.out.println("build get work space: " + build.getWorkspace());
        System.out.println("build get root dir: " + build.getRootDir().getPath());
        System.out.println("build work space value: " + buildWorkspaceValue);
        System.out.println("expected: " + expectedCustomEnvVarValue);
        //resolvedValue = envVars.get("JENKINS_HOME") + "/customWorkspace/materialize_workspace"; 
        System.out.println("JENKINS_HOME: " + envVars.get("JENKINS_HOME"));
        System.out.println("HUDSON_HOME: " + envVars.get("HUDSON_HOME"));
        System.out.println("materialize_workspace_path: " + envVars.get("materialize_workspace_path"));
        System.out.println("WORKSPACE: " + envVars.get("WORKSPACE"));
        
        //3-- Test equals
        Assert.assertEquals(expectedCustomEnvVarValue, resolvedValue);
    }

    private String resolveVars(String value, Map<String, String> map) {
        return Util.replaceMacro(value, map);
    }

}
