package com.tysonista.jenkinsci.tizen.versionup;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.util.Set;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.tysonista.jenkinsci.tizen.common.Constants;

/**
 * Tizen package version up {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link TizenPackageVersionUp} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class TizenPackageVersionUp extends Builder {


    private String packageVersionRule;
    private String gitUrl;
    private String gitPort;
    private String gitPath;
    private String gitBranch;
    private String changeLog;
    private String singleId;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public TizenPackageVersionUp(String gitUrl, String gitPort, String gitPath, String gitBranch, String changeLog, String packageVersionRule, String singleId) {
        this.gitUrl = gitUrl;
        this.gitPort = gitPort;
        this.gitPath = gitPath;
        this.gitBranch = gitBranch;
        this.changeLog = changeLog;
        this.packageVersionRule = packageVersionRule;
        this.singleId = singleId;
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        // This is where you 'build' the project.
        return antCall(gitUrl, gitPort, gitPath, gitBranch, changeLog, packageVersionRule, singleId, build, launcher, listener);
    }

    private boolean antCall(String gitUrl,
                            String gitPort,
                            String gitPath,
                            String gitBranch,
                            String changeLog,
                            String packageVersionRule,
                            String singleId,
                            AbstractBuild<?, ?> build,
                            Launcher launcher,
                            BuildListener listener) throws IOException, InterruptedException {

        String buildXmlFilePath = Constants.pluginPath+"ant-version.xml";
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add("ant");
        if (buildXmlFilePath!=null) {
            args.add("-file", buildXmlFilePath);
        }

        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());

        VariableResolver<String> vr = new VariableResolver.ByMap<String>(env);
        Set<String> sensitiveVars = build.getSensitiveBuildVariables();
        StringBuilder properties = new StringBuilder();
        properties.append("GIT_SERVER_URL=").append(gitUrl).append("\n")
                    .append("GIT_SERVER_PORT=").append(gitPort).append("\n")
                    .append("GIT_PROJECT_NAME=").append(gitPath).append("\n")
                    .append("GIT_BRANCH_NAME=").append(gitBranch).append("\n")
                    .append("PACKAGE_VERSION_RULE=").append(packageVersionRule).append("\n")
                    .append("SINGLE_ID=").append(singleId).append("\n")
                    .append("WORKSPACE=").append("${WORKSPACE}");
                    
        args.addKeyValuePairsFromPropertyString("-D", properties.toString(), vr, sensitiveVars);
        args.add("-DCHANGE_DATA="+env.get("ChangeLog"));
//        if(!launcher.isUnix()) {
//            args = args.toWindowsCommand();
//            // For some reason, ant on windows rejects empty parameters but unix does not.
//            // Add quotes for any empty parameter values:
//            List<String> newArgs = new ArrayList<String>(args.toList());
//            newArgs.set(newArgs.size() - 1, newArgs.get(newArgs.size() - 1).replaceAll(
//                    "(?<= )(-D[^\" ]+)= ", "$1=\"\" "));
//            args = new ArgumentListBuilder(newArgs.toArray(new String[newArgs.size()]));
//        }
        try {
            int r = launcher.launch().cmds(args).stdout(listener.getLogger()).envs(env).pwd(Constants.pluginPath).join();
            return r==0;
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            e.printStackTrace();
            return false;
        }
    }

    public String getSingleId() {
        return singleId;
    }

    public void setSingleId(String singleId) {
        this.singleId = singleId;
    }

    public String getPackageVersionRule() {
        return packageVersionRule;
    }

    public void setPackageVersionRule(String packageVersionRule) {
        this.packageVersionRule = packageVersionRule;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getGitPort() {
        return gitPort;
    }

    public void setGitPort(String gitPort) {
        this.gitPort = gitPort;
    }

    public String getGitPath() {
        return gitPath;
    }

    public void setGitPath(String gitPath) {
        this.gitPath = gitPath;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link TizenPackageVersionUp}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Tizen Package Version Up";
        }
    }
}

