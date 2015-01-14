package com.tysonista.jenkinsci.tizen.commitlog;

import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.ParameterDefinition;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.tysonista.jenkinsci.tizen.common.Constants;

public class TizenGitParameterDefinition extends ParameterDefinition {

    public final String gitUrl;
    public final String gitPort;
    public final String gitPath;
    public final String gitBranch;

    @DataBoundConstructor
    public TizenGitParameterDefinition(String name, String description, String gitUrl, String gitPort, String gitPath, String gitBranch) {
        super(name, description);
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

    @Override
    public ParameterValue createValue(StaplerRequest arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ParameterValue createValue(StaplerRequest arg0, JSONObject arg1) {
        // TODO Auto-generated method stub
        return null;
    }

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
//            return FormValidation.ok("gitUrl:"+url+
//                    ", gitPort:"+port+
//                    ", gitPath:"+path+
//                    ", gitBranch:"+branch);
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
                sb.append("[Commit Logs]")
                .append("=======================")
                .append(commitData)
                .append("=======================");

                return FormValidation.ok(sb.toString());
            }
        }
        private String getCommitData() throws IOException {
            FileInputStream fis = null;
            try {
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
}
