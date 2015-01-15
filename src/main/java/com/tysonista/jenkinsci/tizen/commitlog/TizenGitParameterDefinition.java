package com.tysonista.jenkinsci.tizen.commitlog;

import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.tysonista.jenkinsci.tizen.common.Constants;

public class TizenGitParameterDefinition extends StringParameterDefinition {

    public final String gitUrl;
    public final String gitPort;
    public final String gitPath;
    public final String gitBranch;

    @DataBoundConstructor
    public TizenGitParameterDefinition(String gitUrl, String gitPort, String gitPath, String gitBranch) {
        super("Tizen Commit Logs", "");
        this.gitUrl=gitUrl;
        this.gitPort=gitPort;
        this.gitPath=gitPath;
        this.gitBranch=gitBranch;
    }

    public final List<Object> getGitUrls() {
        ArrayList<Object> list = new ArrayList<Object>();
        list.addAll(Arrays.asList(this.gitUrl.split("\n")));
        return list;
    }

    public final List<Object> getGitPorts() {
        ArrayList<Object> list = new ArrayList<Object>();
        list.addAll(Arrays.asList(this.gitPort.split("\n")));
        return list;
    }

    public final List<Object> getGitPaths() {
        ArrayList<Object> list = new ArrayList<Object>();
        list.addAll(Arrays.asList(this.gitPath.split("\n")));
        return list;
    }

    public final List<Object> getGitBranchs() {
        ArrayList<Object> list = new ArrayList<Object>();
        list.addAll(Arrays.asList(this.gitBranch.split("\n")));
        return list;
    }

    /**
     * 
     */
    private static final long serialVersionUID = -730014143015177976L;

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "Tizen Commit Logs";
        }
        public FormValidation doTest(
                @QueryParameter("url") String url,
                @QueryParameter("port") String port,
                @QueryParameter("path") String path,
                @QueryParameter("branch") String branch) {
            try {
                return antCall(url, port, path, branch);
            } catch (IOException e) {
                return FormValidation.error(e, e.getLocalizedMessage());
            } catch (InterruptedException e) {
                return FormValidation.error(e, e.getLocalizedMessage());
            }
        }
        public FormValidation antCall(String url, String port, String path, String branch) throws IOException, InterruptedException {
            // This also shows how you can consult the global configuration of the builder
            String buildXmlFilePath = Constants.pluginPath+"ant-version.xml";

            ArgumentListBuilder args = new ArgumentListBuilder();
            args.add("ant");
            if (buildXmlFilePath!=null) {
                args.add("-file", buildXmlFilePath);
            }

            StringBuilder properties = new StringBuilder();
            properties.append("GIT_SERVER_URL=").append(url).append("\n")
                                                .append("GIT_SERVER_PORT=").append(port).append("\n")
                                                .append("GIT_PROJECT_NAME=").append(path).append("\n")
                                                .append("GIT_BRANCH_NAME=").append(branch).append("\n");
            args.addKeyValuePairsFromPropertyString("-D", properties.toString(), null, null);

            List<String> newArgs = new ArrayList<String>(args.toList());
            newArgs.set(newArgs.size() - 1, newArgs.get(newArgs.size() - 1).replaceAll("(?<= )(-D[^\" ]+)= ", "$1=\"\" "));
            args = new ArgumentListBuilder(newArgs.toArray(new String[newArgs.size()]));
            args.add("commit");

            Process process;
            process = Runtime.getRuntime().exec(args.toList().toArray(new String[0]));
            int exitValue = process.waitFor();
            String logs = getString(process.getInputStream());
            logs = "command:"+args.toString()+"\n"+logs;
            if (exitValue != 0) {
                String errs = getString(process.getErrorStream());
                return FormValidation.error("ant build failed("+ exitValue+")\n"+logs+"\n"+errs);
            } else {
                String commitData = getCommitData();
                StringBuilder sb = new StringBuilder();
                sb.append("[Commit Logs]\n")
                .append(commitData)
                .append("\n[END]\n"+logs);

                return FormValidation.ok(sb.toString());
            }
        }
        private String getCommitData() throws IOException {
            FileInputStream fis = null;
            try {
                //TODO: change to workspace
                fis = new FileInputStream(Constants.pluginPath+"commit-data");
                String commitData = getString(fis);
                return commitData;
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        }
        private String getString(InputStream is) throws IOException {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(is));
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = br.readLine())!= null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } finally {
                if (br != null) {
                    br.close();
                }
            }
        }
    }

    @Override
    public StringParameterValue getDefaultParameterValue() {
        // TODO Auto-generated method stub
        return super.getDefaultParameterValue();
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        String url = req.getParameter("gitUrl");
        String port = req.getParameter("gitPort");
        String path = req.getParameter("gitPath");
        String branch = req.getParameter("gitBranch");
        return new TizenGitParameterValue(getName(), getDefaultValue(), url, port, path, branch);
    }

    @Override
    public ParameterValue createValue(String value) {
        // TODO Auto-generated method stub
        return super.createValue(value);
    }
}
